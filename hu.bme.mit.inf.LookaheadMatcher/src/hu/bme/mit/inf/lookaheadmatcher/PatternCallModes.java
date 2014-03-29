package hu.bme.mit.inf.lookaheadmatcher;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.incquery.runtime.matchers.psystem.PQuery;
import org.eclipse.incquery.runtime.matchers.psystem.PVariable;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

public class PatternCallModes
{
	/**
	 * The (current) pattern that calls the modes
	 */
	private PQuery thePattern;
	
	private List<PVariable> paramVars;
	
	/**
	 * The finds called by the current pattern
	 */
	private Collection<PQuery> calledFinds;
	
	/**
	 * The neg finds called by the current pattern (may call only on a set of index
	 */
	private Multimap<PQuery, Set<PVariable>> calledNegFinds;
	
	public PatternCallModes(PQuery theCallee)
	{
		thePattern = theCallee;
		paramVars = thePattern.getContainedBodies().iterator().next().getSymbolicParameters();
		
		calledFinds = new HashSet<PQuery>();
		calledNegFinds = HashMultimap.create();//new HashMap<PQuery, Set<PVariable>>();
	}
	
	/**
	 * The easy case: calling a simple pattern
	 * @param callee The pattern finded (called)
	 */
	public boolean AddPositiveCall(PQuery callee)
	{
		return calledFinds.add(callee);
	}
	
	public boolean AddNegativeCall(PQuery negCallee, Set<PVariable> indexCallingOn)
	{
		return calledNegFinds.put(negCallee, indexCallingOn);
	}

	public int allSize() 
	{
		return calledFinds.size() + calledNegFinds.size();
	}

	public Set<PQuery> getCallingPatternsSimply()
	{
		Set<PQuery> allAffected = new HashSet<PQuery>();
		allAffected.addAll(this.calledFinds);
		allAffected.addAll(this.calledNegFinds.keySet());
		return allAffected;
	}

	public Collection<PQuery> getPositiveCalls()
	{
		return this.calledFinds;
	}

	public boolean containsPattern(PQuery q)
	{
		return this.calledFinds.contains(q) || this.calledNegFinds.keySet().contains(q);
	}
}
