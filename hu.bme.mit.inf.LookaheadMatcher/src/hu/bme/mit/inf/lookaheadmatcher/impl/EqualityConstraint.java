package hu.bme.mit.inf.lookaheadmatcher.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eclipse.incquery.runtime.matchers.psystem.PVariable;
import org.eclipse.incquery.runtime.matchers.psystem.basicdeferred.Equality;

public class EqualityConstraint extends CheckableConstraint
{
	private Equality innerEqualityConstraint;
	private List<PVariable> affectedVariables;
	
	public EqualityConstraint(Equality innerEq) 
	{
		this.innerEqualityConstraint = innerEq;
		this.affectedVariables = new ArrayList<PVariable>();
		for (PVariable inner : this.innerEqualityConstraint.getAffectedVariables())
		{
			this.affectedVariables.add(inner);
		}
	}

	@Override
	public boolean Evaluate(HashMap<PVariable, Object> matchingVariables)
	{
		if (!this.CanBeEvaluated(matchingVariables))
			return false; // ejjnye!
		
		Object value = null;
		for (PVariable affected : this.affectedVariables)
		{
			if (value == null)
			{
				value = matchingVariables.get(affected);
				continue;
			}
			if (matchingVariables.get(affected).equals(value) == false)
				return false; // inequal!
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
