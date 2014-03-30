package hu.bme.mit.inf.lookaheadmatcher.impl;

import hu.bme.mit.inf.lookaheadmatcher.IPartialPatternCacher;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.incquery.runtime.api.IncQueryEngine;
import org.eclipse.incquery.runtime.matchers.psystem.DeferredPConstraint;
import org.eclipse.incquery.runtime.matchers.psystem.EnumerablePConstraint;
import org.eclipse.incquery.runtime.matchers.psystem.PBody;
import org.eclipse.incquery.runtime.matchers.psystem.PConstraint;
import org.eclipse.incquery.runtime.matchers.psystem.PParameter;
import org.eclipse.incquery.runtime.matchers.psystem.PQuery;
import org.eclipse.incquery.runtime.matchers.psystem.PVariable;
import org.eclipse.incquery.runtime.matchers.psystem.basicdeferred.Equality;
import org.eclipse.incquery.runtime.matchers.psystem.basicdeferred.ExportedParameter;
import org.eclipse.incquery.runtime.matchers.psystem.basicdeferred.ExpressionEvaluation;
import org.eclipse.incquery.runtime.matchers.psystem.basicdeferred.Inequality;
import org.eclipse.incquery.runtime.matchers.psystem.basicdeferred.NegativePatternCall;
import org.eclipse.incquery.runtime.matchers.psystem.basicenumerables.ConstantValue;
import org.eclipse.incquery.runtime.matchers.psystem.basicenumerables.PositivePatternCall;
import org.eclipse.incquery.runtime.matchers.psystem.basicenumerables.TypeBinary;
import org.eclipse.incquery.runtime.matchers.psystem.basicenumerables.TypeTernary;
import org.eclipse.incquery.runtime.matchers.psystem.basicenumerables.TypeUnary;

public class AheadStructure implements Cloneable
{
	// fix symbolic variables
	public PVariable[] FixSymbolicVariables;
	// currently searched restrictions
	public ArrayList<AxisConstraint> SearchedConstraints = new ArrayList<AxisConstraint>();
	// found constraints (SearchedRest+FoundRest=fullRest)
	public ArrayList<AxisConstraint> FoundConstraints = new ArrayList<AxisConstraint>();
	// found constraints (SearchedRest+FoundRest=fullRest)
	public ArrayList<CheckableConstraint> CheckConstraints = new ArrayList<CheckableConstraint>();
	// the Variable - modelelement map (matches assigned to a variable) - object cannot be meguszni
	public HashMap<PVariable, Object> MatchingVariables = new HashMap<PVariable, Object>();

	// here is the equality hashtable: the key is the variable, and the value is an array, containing the variables equal to the key variable
	// value cannot be null and arrayList cannot be empty (we add this condition when we find one)
	// public Hashtable<LookVariable, ArrayList<LookVariable>> EqualVariables = new Hashtable<LookVariable, ArrayList<LookVariable>>();

	// stores the prefix - variables hashtable, so it is based on the patterns' own local+symbolic variables
	//public Hashtable<String, ArrayList<LookVariable>> variableReferenceGroups = new Hashtable<String, ArrayList<LookVariable>>();
	// stores, which prefix means distinct or shareable (isDistrinctMatching()) true - distinct, false - shareable
	//public Hashtable<String, Boolean> prefixDistinct = new Hashtable<String, Boolean>();
	
	private AheadStructure()
	{
		
	}
	
	public AheadStructure(PVariable[] fixSymbolicVariables)
	{
		this.FixSymbolicVariables = new PVariable[fixSymbolicVariables.length];
		for(int i=0;i<fixSymbolicVariables.length;i++)
			this.FixSymbolicVariables[i]=fixSymbolicVariables[i];
	}

	/*public boolean isEqualVariables(LookVariable le, LookVariable ri)
	{
		return isEqualVariables(le, ri, new ArrayList<LookVariable>());
	}*/

	// checks whether a recursive algorithm can find a way from the initial 'vizsg' to the 'cel' - if it can: they should be equal
	/*private boolean isEqualVariables(LookVariable vizsg, LookVariable cel, ArrayList<LookVariable> examined)
	{
		examined.add(vizsg); // we checked the vizsg, DO NOT check it again!
		if (EqualVariables.containsKey(vizsg) && EqualVariables.get(vizsg).contains(cel))
		{
				return true;
		}
		else if (EqualVariables.containsKey(vizsg)) // if cel is not in the list, but there IS a list
		{
			boolean isAnyPath = false; // if ANY path can be found: return true
			for (LookVariable lVar : EqualVariables.get(vizsg)) // iterate through all vizsg-s equal variables
			{
				if (!examined.contains(lVar))
				{
					if (isEqualVariables(lVar, cel, examined))
						isAnyPath = true; // let vizsg be the element from the list, cel remains unchanged
				}
				// the first step of the function is to disable lVar (so we will check lVar once)
			}
			if (isAnyPath)
				return true;
			return false; // if no path to goal: maybe it is really false
		}
		return false; // it can happen...
	}*/


	// get distinct matching by groups: only same-group distinct counts
	/*public boolean isDistinctMatchingByGroups(LookVariable left, LookVariable right)
	{
		if (!prefixDistinct.get(left.getPrefix()) && !prefixDistinct.get(right.getPrefix()))
			return false; // both shareable, can be equal
		if (left.getPrefix() == right.getPrefix())
			return true; // if they come from same group and 
		return false; // one of them is from distinctMatching and they're not from the same group - can be equal (think of Timi_A and A)
	}

	// returns true, if the desired two variables are locally (within a prefix group) equal -> no recursion here
	public boolean isLocallyNotEqualVariablesInDistinct(LookVariable le, LookVariable ri)
	{
		if ((!EqualVariables.containsKey(le) || (EqualVariables.containsKey(le) && !EqualVariables.get(le).contains(ri)))
				&& isDistinctMatchingByGroups(le, ri))
		{
			//it is OKAY, because: they are zero-depth equal, AND from the same group, AND distinct
			return true;
		}
		return false;
	}*/
	
	public AheadStructure(PBody pBody, List<String> params, IPartialPatternCacher partialCacher, IncQueryEngine engine)
	{
		this.setInnerPBody(pBody);
		
		// get all fix symbolic (parameter) vars
		int sz = 0;
		this.FixSymbolicVariables = new PVariable[pBody.getSymbolicParameters().size()];
		for (PVariable pv : pBody.getSymbolicParameters())
			this.FixSymbolicVariables[sz++] = pv;
		
		//get all other vars
		Set<PVariable> variables = pBody.getAllVariables();
		for (PVariable pv : variables)
			this.MatchingVariables.put(pv, null); // init to zero
		
		// convert (wrap) constraints
		ArrayList<IConstraint> constraints = new ArrayList<IConstraint>();
		for (PConstraint constraint : pBody.getConstraints())
		{
			if (constraint instanceof EnumerablePConstraint)
			{
				if (constraint instanceof TypeUnary)
				{
					// typeconstraint
					constraints.add(new TypeConstraint((TypeUnary)constraint));
				}
				else if (constraint instanceof TypeBinary)
				{
					// relationconstraint
					constraints.add(new RelationConstraint((TypeBinary)constraint));
				}
				else if (constraint instanceof PositivePatternCall)
				{
					// findconstraint
					constraints.add(new FindConstraint((PositivePatternCall)constraint, partialCacher));
				}
				else if (constraint instanceof ConstantValue)
				{
					// easyconstraint
					constraints.add(new EasyConstraint((ConstantValue)constraint));
				}
				else if (!(constraint instanceof ExportedParameter))
					throw new AssertionError("Unknown constraint! " + constraint.toString());
			}
			else if (constraint instanceof DeferredPConstraint)
			{
				if (constraint instanceof ExpressionEvaluation)
				{
					// checkconstraint - and eval!!! should implement eval
					constraints.add(new CheckEvalExpressionConstraint((ExpressionEvaluation)constraint));
				}
				else if (constraint instanceof NegativePatternCall)
				{
					// NACconstraint
					constraints.add(new NACConstraint((NegativePatternCall)constraint, partialCacher, engine));
				}
				else if (constraint instanceof Equality)
				{
					constraints.add(new EqualityConstraint((Equality)constraint));
				}
				else if (constraint instanceof Inequality)
				{
					constraints.add(new InEqualConstraint((Inequality)constraint));
				}
				else if (!(constraint instanceof ExportedParameter))
					throw new AssertionError("Unknown constraint! " + constraint.toString());
			}
		}
		for (IConstraint cons : constraints)
		{
			if (cons instanceof AxisConstraint)
				SearchedConstraints.add((AxisConstraint) cons);
			else if (cons instanceof CheckableConstraint)
				CheckConstraints.add((CheckableConstraint) cons);
			else
				System.out.println("Unrecognized constraint type");
		}
		// well, it is initalized and ready, good job guys, good job guys
	}
	
	private PBody innerPBody;

	@Override
	public AheadStructure clone()
	{
		AheadStructure ast = new AheadStructure();
		ast.FixSymbolicVariables = this.FixSymbolicVariables.clone();
		ast.SearchedConstraints.addAll(this.SearchedConstraints);
		ast.FoundConstraints.addAll(this.FoundConstraints);
		ast.CheckConstraints.addAll(this.CheckConstraints);
		ast.MatchingVariables.putAll(this.MatchingVariables);
		
		// equalVariable contains an ArrayList, so cloning is a bit more complicated
		/*for (Entry<LookVariable, ArrayList<LookVariable>> a : this.EqualVariables.entrySet())
		{
			ArrayList<LookVariable> newEq = new ArrayList<LookVariable>();
			newEq.addAll(a.getValue());
			ast.EqualVariables.put(a.getKey(), newEq);
		}*/

		// prefixDistinct contains an ArrayList, too
		/*ast.prefixDistinct.putAll(this.prefixDistinct);
		for (Entry<String, ArrayList<LookVariable>> a : this.variableReferenceGroups.entrySet())
		{
			ArrayList<LookVariable> newDis = new ArrayList<LookVariable>();
			newDis.addAll(a.getValue());
			ast.variableReferenceGroups.put(a.getKey(), newDis);
		}*/
		
		return ast;
	}

	public PBody getInnerPBody() {
		return innerPBody;
	}

	public void setInnerPBody(PBody innerPBody) {
		this.innerPBody = innerPBody;
	}
}
