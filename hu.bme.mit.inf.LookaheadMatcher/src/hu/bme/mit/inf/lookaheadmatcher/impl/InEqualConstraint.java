package hu.bme.mit.inf.lookaheadmatcher.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eclipse.incquery.runtime.matchers.psystem.PVariable;
import org.eclipse.incquery.runtime.matchers.psystem.basicdeferred.Equality;
import org.eclipse.incquery.runtime.matchers.psystem.basicdeferred.Inequality;

public class InEqualConstraint extends CheckableConstraint implements IConstraint
{
	private Inequality innerInequalityConstraint;

	private List<PVariable> affectedVariables;
	
	public InEqualConstraint(Inequality innerEq) 
	{
		this.innerInequalityConstraint = innerEq;
		this.affectedVariables = new ArrayList<PVariable>();
		for (PVariable inner : this.innerInequalityConstraint.getAffectedVariables())
		{
			this.affectedVariables.add(inner);
		}
	}

	@Override
	public boolean Evaluate(HashMap<PVariable, Object> matchingVariables)
	{
		if (!this.CanBeEvaluated(matchingVariables))
			return false; // ejjnye!
		
		// bubble :(
		for (int i = 0; i < this.affectedVariables.size(); i++)
		{
			for (int j = i + 1; j < this.affectedVariables.size(); j++)
			{
				if (matchingVariables.get(this.affectedVariables.get(i)).equals(matchingVariables.get(this.affectedVariables.get(j))))
					return false; // if some of them equal, escape!
			}
		}
		return true;
	}

	@Override
	public boolean CanBeEvaluated(HashMap<PVariable, Object> matchingVariables)
	{
		for (PVariable affected : this.affectedVariables)
		{
			if (matchingVariables.get(affected) == null)
				return false;
		}
		return true;
	}
}
