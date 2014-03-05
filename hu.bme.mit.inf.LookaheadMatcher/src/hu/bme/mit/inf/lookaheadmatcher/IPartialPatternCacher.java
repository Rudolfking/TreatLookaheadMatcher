package hu.bme.mit.inf.lookaheadmatcher;

import hu.bme.mit.inf.lookaheadmatcher.impl.LookaheadMatching;
import hu.bme.mit.inf.lookaheadmatcher.impl.MultiSet;

import java.util.HashMap;
import java.util.List;

import org.eclipse.incquery.runtime.matchers.psystem.PQuery;
import org.eclipse.incquery.runtime.matchers.psystem.PVariable;

public interface IPartialPatternCacher
{
	// Get the match count for a partial matching
	int GetMatchCountFromPartial(PQuery resolvingQuery, HashMap<PVariable, Object> partialMatchingWithNullsAndEverything, List<PVariable> variablesInOrder, boolean forceMakeIndex);
	
	// Get the exact matches for a partial matching (eg. when using this constraint as the next step)
	MultiSet<LookaheadMatching> GetMatchingsFromPartial(PQuery resolvingQuery, HashMap<PVariable, Object> partialMatchingWithNullsAndEverything, List<PVariable> variablesInOrder, boolean forceMakeIndex);
}
