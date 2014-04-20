package hu.bme.mit.inf.lookaheadmatcher.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eclipse.incquery.runtime.api.IncQueryEngine;
import org.eclipse.incquery.runtime.matchers.psystem.PQuery;
import org.eclipse.incquery.runtime.matchers.psystem.PVariable;

import com.google.common.collect.Multiset;

import hu.bme.mit.inf.lookaheadmatcher.IPartialPatternCacher;
import hu.bme.mit.inf.lookaheadmatcher.LookaheadMatcherInterface;

public class SimplePatternCacher implements IPartialPatternCacher {

	private IncQueryEngine engine;
	
	public SimplePatternCacher(IncQueryEngine engin)
	{
		this.engine = engin;
	}
	
	@Override
	public int GetMatchCountFromPartial(PQuery resolvingQuery,
			HashMap<PVariable, Object> partialMatchingWithNullsAndEverything,
			List<PVariable> variablesInOrder, boolean forceMakeIndexIfNotIndexed)
	{
		// we DO NOT WANT to use this as tengely, ALAP method: use as last
		if (forceMakeIndexIfNotIndexed) // NAC! needing info
			return (GetMatchingsFromPartial(resolvingQuery, partialMatchingWithNullsAndEverything, variablesInOrder, true)).size();
		return Integer.MAX_VALUE;
	}

	@Override
	public Multiset<LookaheadMatching> GetMatchingsFromPartial(
			PQuery resolvingQuery,
			HashMap<PVariable, Object> partialMatchingWithNullsAndEverything,
			List<PVariable> variablesInOrder, boolean forceMakeIndexIfNotIndexed)
	{
		// strategy: match all but with known values (should be almost a check)
		ArrayList<Object> knownValues = new ArrayList<Object>();
		for(PVariable var : variablesInOrder)
			knownValues.add(partialMatchingWithNullsAndEverything.get(var));
		return (new LookaheadMatcherInterface()).matchAll(engine, null, resolvingQuery, knownValues, null);
	}

}
