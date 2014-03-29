package hu.bme.mit.inf.treatengine;

import hu.bme.mit.inf.lookaheadmatcher.impl.LookaheadMatching;

import java.util.HashMap;
import java.util.Set;

import org.eclipse.incquery.runtime.matchers.psystem.PQuery;
import org.eclipse.incquery.runtime.matchers.psystem.PVariable;

public class IndexDelta extends Delta {

	private Set<PVariable> indexVariables;
	
	public IndexDelta(PQuery changedPattern, HashMap<LookaheadMatching, Boolean> changes, Set<PVariable> indexVars)
	{
		// classic delta:
		super(changedPattern, changes);
		
		indexVariables = indexVars;
	}

	public Set<PVariable> getIndexVariables()
	{
		return indexVariables;
	}
}
