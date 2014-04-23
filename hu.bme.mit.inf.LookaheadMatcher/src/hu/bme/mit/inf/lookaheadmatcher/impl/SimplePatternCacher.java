package hu.bme.mit.inf.lookaheadmatcher.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eclipse.incquery.runtime.api.IncQueryEngine;
import org.eclipse.incquery.runtime.base.api.NavigationHelper;
import org.eclipse.incquery.runtime.matchers.psystem.PQuery;
import org.eclipse.incquery.runtime.matchers.psystem.PVariable;

import com.google.common.collect.Multiset;

import hu.bme.mit.inf.lookaheadmatcher.IPartialPatternCacher;
import hu.bme.mit.inf.lookaheadmatcher.LookaheadMatcherInterface;

public class SimplePatternCacher implements IPartialPatternCacher {

	private IncQueryEngine engine;
	private NavigationHelper navigationHelper;
	
	public SimplePatternCacher(IncQueryEngine engin, NavigationHelper navHelp)
	{
		this.engine = engin;
		this.navigationHelper = navHelp;
	}
	
	// private HashMap<PQuery, Multiset<LookaheadMatching>> smallGodSet = new HashMap<PQuery, Multiset<LookaheadMatching>>();
	// private HashMap<PQuery, Boolean> smallGodSetNeg = new HashMap<PQuery, Boolean>();
	
	@Override
	public int GetMatchCountFromPartial(PQuery resolvingQuery,
			HashMap<PVariable, Object> partialMatchingWithNullsAndEverything,
			List<PVariable> variablesInOrder, boolean forceMakeIndexIfNotIndexed)
	{
		// we DO NOT WANT to use this as tengely, ALAP method: use as last
		if (forceMakeIndexIfNotIndexed) // NAC! needing info
		{
			ArrayList<Object> knownValues = new ArrayList<Object>();
			for(PVariable var : variablesInOrder)
				knownValues.add(partialMatchingWithNullsAndEverything.get(var));
			return (new LookaheadMatcherInterface(this.navigationHelper)).tryMatch(engine, null, resolvingQuery, knownValues, null) ? 1 : 0;
		}
		return Integer.MAX_VALUE - 10; // because mayvalue is the cost default :):):)
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
		return (new LookaheadMatcherInterface(this.navigationHelper)).matchAll(engine, null, resolvingQuery, knownValues, null);
	}

}
