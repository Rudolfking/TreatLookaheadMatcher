package hu.bme.mit.inf.treatengine;

import hu.bme.mit.inf.lookaheadmatcher.impl.LookaheadMatching;

import java.util.HashMap;
import java.util.Set;

import org.eclipse.incquery.runtime.matchers.psystem.PParameter;
import org.eclipse.incquery.runtime.matchers.psystem.PQuery;

public class IndexDelta extends Delta {

	private Set<PParameter> indexVariables;
	
	public IndexDelta(PQuery changedPattern, HashMap<LookaheadMatching, Boolean> changes, Set<PParameter> indexVars)
	{
		// classic delta:
		super(changedPattern, changes);
		
		indexVariables = indexVars;
	}

	public Set<PParameter> getIndexVariables()
	{
		return indexVariables;
	}
}
