package hu.bme.mit.inf.lookaheadmatcher.impl;

import java.util.HashMap;

import org.eclipse.incquery.runtime.matchers.psystem.PVariable;

public abstract class CheckableConstraint implements IConstraint
{
	public boolean isAxis()
	{
		return false;
	}
	public boolean isCheckable()
	{
		return true;
	}
	
	public abstract boolean Evaluate(HashMap<PVariable, Object> matchingVariables);
	
	public abstract boolean CanBeEvaluated(HashMap<PVariable, Object> matchingVariables);
}
