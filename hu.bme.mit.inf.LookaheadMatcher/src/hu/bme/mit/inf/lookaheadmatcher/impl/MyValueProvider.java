package hu.bme.mit.inf.lookaheadmatcher.impl;

import java.util.HashMap;

import org.eclipse.incquery.runtime.matchers.psystem.IValueProvider;
import org.eclipse.incquery.runtime.matchers.psystem.PVariable;

public class MyValueProvider implements IValueProvider
{
	private HashMap<PVariable, Object> matchings;

	@SuppressWarnings("unused")
	private MyValueProvider(){}
	
	public MyValueProvider(HashMap<PVariable, Object> matchingVariables)
	{
		addKnownMatchings(matchingVariables);
	}

	@Override
	public Object getValue(String variableName) throws IllegalArgumentException
	{
		for (PVariable item : matchings.keySet())
		{
			// isUnique => not unified
			if (item.isUnique() && item.getName().equals(variableName))
				return matchings.get(item);
		}
		throw new IllegalArgumentException("Given variable for variable name was not found!");
	}

	@SuppressWarnings("unchecked")
	private void addKnownMatchings(HashMap<PVariable, Object> matchingVariables)
	{
		this.matchings = (HashMap<PVariable, Object>) matchingVariables.clone();
	}
}
