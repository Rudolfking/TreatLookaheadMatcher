package hu.bme.mit.inf.lookaheadmatcher.impl;

import hu.bme.mit.inf.lookaheadmatcher.IPartialPatternCacher;
import hu.bme.mit.inf.lookaheadmatcher.LookaheadMatcherInterface;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Set;

import org.eclipse.emf.common.util.EList;
import org.eclipse.incquery.patternlanguage.patternLanguage.IntValue;
import org.eclipse.incquery.patternlanguage.patternLanguage.LiteralValueReference;
import org.eclipse.incquery.patternlanguage.patternLanguage.ParameterRef;
import org.eclipse.incquery.patternlanguage.patternLanguage.Pattern;
import org.eclipse.incquery.patternlanguage.patternLanguage.PatternCall;
import org.eclipse.incquery.patternlanguage.patternLanguage.StringValue;
import org.eclipse.incquery.patternlanguage.patternLanguage.ValueReference;
import org.eclipse.incquery.patternlanguage.patternLanguage.Variable;
import org.eclipse.incquery.patternlanguage.patternLanguage.VariableValue;
import org.eclipse.incquery.runtime.api.IQuerySpecification;
import org.eclipse.incquery.runtime.api.IncQueryEngine;
import org.eclipse.incquery.runtime.matchers.planning.SubPlan;
import org.eclipse.incquery.runtime.matchers.psystem.PVariable;
//import org.eclipse.viatra2.emf.incquery.runtime.extensibility.MatcherFactoryRegistry;
import org.eclipse.incquery.runtime.matchers.psystem.basicdeferred.NegativePatternCall;
import org.eclipse.incquery.runtime.matchers.tuple.FlatTuple;
import org.eclipse.incquery.runtime.matchers.tuple.Tuple;

public class NACConstraint extends CheckableConstraint implements IConstraint
{
	private ArrayList<PVariable> affectedVariables;
	private IncQueryEngine engineRef;
	private NegativePatternCall innerNegativeCallConstraint;
	private IPartialPatternCacher treatPartialCacher;
	
	@Override
	public boolean Evaluate(HashMap<PVariable, Object> matchingVariables)
	{
		if (!CanBeEvaluated(matchingVariables))
			return false; // heh?
		
		ArrayList<Object> knownValues = new ArrayList<Object>();
		
		Set<PVariable> parameters = this.innerNegativeCallConstraint.getAffectedVariables();
		for (PVariable pVar : parameters)
		{
			knownValues.add(matchingVariables.get(pVar));
		}
		
		// use interface to check caches (maybe full, but can be partial):
		int result = this.treatPartialCacher.GetMatchCountFromPartial(this.innerNegativeCallConstraint.getReferredQuery(), matchingVariables,
				affectedVariables, true);
		if (result != -1)
			return result == 0; // no matches means good evaluation (NAC)
		
		// use my matcher from scratch (no caching):
		LookaheadMatcherInterface lmi = new LookaheadMatcherInterface();
		boolean tried = lmi.tryMatch(engineRef, treatPartialCacher, innerNegativeCallConstraint.getReferredQuery(), knownValues);
		System.out.println("My (Lookahead Pattern Matches) NAC call returned: "+Boolean.toString(!tried));
		if (tried)
			return false; // can be matched, this is bad
		return true;
	}

	@Override
	public boolean CanBeEvaluated(HashMap<PVariable, Object> matchingVariables)
	{
		// mhm, can be evaluated when not all variables bound? no
		for (PVariable one : this.affectedVariables)
		{
			if (Utils.isRunning(one) == false && one.isVirtual() == false && matchingVariables.get(one) == null)
				return false;
		}
		return true;
	}

	@Override
	public String toString()
	{
		return this.innerNegativeCallConstraint.getReferredQuery().getFullyQualifiedName()+"("+writeArray(this.affectedVariables)+")";//.getPatternRef().getName()+"("+writeArray(this.affectedVariables)+")";
	}
	
	private String writeArray(ArrayList<PVariable> vv)
	{
		String ret="";
		for(PVariable v:vv)
		{
			ret+=v.getName()+",";
		}
		ret = ret.substring(0, ret.length()-1);
		return ret;
	}
	
	
	public NACConstraint(NegativePatternCall NACPatternCall, IPartialPatternCacher partialCacher, IncQueryEngine engine)
	{
		Tuple tup = NACPatternCall.getActualParametersTuple();
		this.affectedVariables = new ArrayList<PVariable>();
		for(int i=0;i<tup.getSize();i++)
		{
			this.affectedVariables.add((PVariable) tup.get(i));
		}
		this.innerNegativeCallConstraint = NACPatternCall;
		this.engineRef = engine;
		this.treatPartialCacher = partialCacher;
	}

	public NACConstraint(NegativePatternCall negCons)
	{
		this.setInnerNegativeCallConstraint(negCons);
	}

	public NegativePatternCall getInnerNegativeCallConstraint() {
		return innerNegativeCallConstraint;
	}

	public void setInnerNegativeCallConstraint(
			NegativePatternCall innerNegativeCallConstraint) {
		this.innerNegativeCallConstraint = innerNegativeCallConstraint;
	}
}
