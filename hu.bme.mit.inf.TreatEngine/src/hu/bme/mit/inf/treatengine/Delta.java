package hu.bme.mit.inf.treatengine;

import hu.bme.mit.inf.lookaheadmatcher.IDelta;
import hu.bme.mit.inf.lookaheadmatcher.impl.LookaheadMatching;

import org.eclipse.incquery.runtime.matchers.psystem.PQuery;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

// a pattern match set changed: either added (+) or deleted(-) and it references to a pattern
// references to a changeset (+-) and 
public class Delta implements IDelta
{
	private PQuery pattern;
	private Multimap<LookaheadMatching, Boolean> changeset;
	//private AheadStructure structure;
	
	public PQuery getPattern()
	{
		return pattern;
	}
	
	public void setPattern(PQuery pattern)
	{
		this.pattern = pattern;
	}
	
	public Multimap<LookaheadMatching, Boolean> getChangeset()
	{
		return changeset;
	}
	
	public void setChangeset(Multimap<LookaheadMatching, Boolean> changeset)
	{
		this.changeset = changeset;
	}
	
	
//	public AheadStructure getStructure()
//	{
//		return structure;
//	}
//
//	public void setStructure(AheadStructure structure)
//	{
//		this.structure = structure;
//	}

	public Delta(PQuery changedPattern, Multimap<LookaheadMatching, Boolean> changes)
	{
		this.pattern = changedPattern;
		this.changeset = HashMultimap.create();//<LookaheadMatching,Boolean>();
		// copy
		/*for (Entry<LookaheadMatching, Boolean> mac : changes.entrySet())
		{
			changeset.put(mac.getKey(), mac.getValue());
		}*/
		this.changeset = changes;
	}
}
