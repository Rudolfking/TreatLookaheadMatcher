package hu.bme.mit.inf.lookaheadmatcher;

import hu.bme.mit.inf.lookaheadmatcher.impl.LookaheadMatching;

import java.util.HashMap;
import java.util.List;

import org.eclipse.incquery.runtime.matchers.psystem.PQuery;
import org.eclipse.incquery.runtime.matchers.psystem.PVariable;

import com.google.common.collect.Multiset;

public interface IPartialPatternCacher
{
	// Get the match count for a partial matching
	int GetMatchCountFromPartial(PQuery resolvingQuery, HashMap<PVariable, Object> partialMatchingWithNullsAndEverything, List<PVariable> variablesInOrder, boolean forceMakeIndexIfNotIndexed);
	
	// Get the exact matches for a partial matching (eg. when using this constraint as the next step)
	Multiset<LookaheadMatching> GetMatchingsFromPartial(PQuery resolvingQuery, HashMap<PVariable, Object> partialMatchingWithNullsAndEverything, List<PVariable> variablesInOrder, boolean forceMakeIndexIfNotIndexed);
}
