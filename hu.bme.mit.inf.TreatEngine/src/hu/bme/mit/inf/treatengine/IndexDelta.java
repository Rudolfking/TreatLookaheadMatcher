package hu.bme.mit.inf.treatengine;

import hu.bme.mit.inf.lookaheadmatcher.impl.LookaheadMatching;

import java.util.HashMap;
import java.util.Set;

import org.eclipse.incquery.runtime.matchers.psystem.PQuery;

public class IndexDelta extends Delta {

	private Set<String> indexVariables;
	
	public IndexDelta(PQuery changedPattern, HashMap<LookaheadMatching, Boolean> changes, Set<String> indexVars)
	{
		// classic delta:
		super(changedPattern, changes);
		
		indexVariables = indexVars;
	}

	public Set<String> getIndexVariables()
	{
		return indexVariables;
	}
}
