package hu.bme.mit.inf.treatengine;

import hu.bme.mit.inf.lookaheadmatcher.impl.LookaheadMatching;

import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.eclipse.incquery.runtime.matchers.psystem.PQuery;

public class IndexDelta extends Delta {

	private List<String> indexVariables;
	
	public IndexDelta(PQuery changedPattern, HashMap<LookaheadMatching, Boolean> changes, List<String> indexVars)
	{
		// classic delta:
		super(changedPattern, changes);
		
		indexVariables = indexVars;
	}

	public List<String> getIndexVariables()
	{
		return indexVariables;
	}
}
