package hu.bme.mit.inf.treatengine;

import hu.bme.mit.inf.lookaheadmatcher.IDelta;
import hu.bme.mit.inf.lookaheadmatcher.impl.LookaheadMatching;

import org.eclipse.incquery.runtime.matchers.psystem.PQuery;

import com.google.common.collect.HashMultimap;

public class ModelDelta implements IDelta
{
	private PQuery pattern;
	private HashMultimap<LookaheadMatching, Boolean> changeset;
	public PQuery getPattern()
	{
		return pattern;
	}
	public HashMultimap<LookaheadMatching, Boolean> getChangeset()
	{
		return changeset;
	}
	public ModelDelta(PQuery patt, HashMultimap<LookaheadMatching, Boolean> changes)
	{
		changeset = changes;
		pattern = patt;
	}
}
