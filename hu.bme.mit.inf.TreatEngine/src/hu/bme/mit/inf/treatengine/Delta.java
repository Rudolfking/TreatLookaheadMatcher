package hu.bme.mit.inf.treatengine;

import java.util.HashMap;

import hu.bme.mit.inf.lookaheadmatcher.IDelta;
import hu.bme.mit.inf.lookaheadmatcher.impl.LookaheadMatching;

import org.eclipse.incquery.runtime.matchers.psystem.PQuery;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

// a pattern match set changed: either added (+) or deleted(-) and it references to a pattern
// references to a changeset (+-) and the pattern affected
public class Delta implements IDelta
{
	private PQuery pattern;
	private HashMap<LookaheadMatching, Boolean> changeset;
	
	public PQuery getPattern()
	{
		return pattern;
	}
	
	public void setPattern(PQuery pattern)
	{
		this.pattern = pattern;
	}
	
	public HashMap<LookaheadMatching, Boolean> getChangeset()
	{
		return changeset;
	}
	
	public void setChangeset(HashMap<LookaheadMatching, Boolean> changeset)
	{
		this.changeset = changeset;
	}

	public Delta(PQuery changedPattern, HashMap<LookaheadMatching, Boolean> changes)
	{
		this.pattern = changedPattern;
		this.changeset = changes;
	}
}
