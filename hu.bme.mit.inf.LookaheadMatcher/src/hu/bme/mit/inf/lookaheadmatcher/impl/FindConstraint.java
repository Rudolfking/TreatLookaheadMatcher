package hu.bme.mit.inf.lookaheadmatcher.impl;

import hu.bme.mit.inf.lookaheadmatcher.IPartialPatternCacher;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.eclipse.incquery.runtime.matchers.psystem.PVariable;
import org.eclipse.incquery.runtime.matchers.psystem.annotations.PAnnotation;
import org.eclipse.incquery.runtime.matchers.psystem.basicenumerables.PositivePatternCall;
import org.eclipse.incquery.runtime.matchers.tuple.Tuple;

import com.google.common.collect.Multiset;

public class FindConstraint extends AxisConstraint implements IConstraint
{
	private List<PVariable> affectedVariables;
	
	private IPartialPatternCacher treatPatternCacher;
	
	private PositivePatternCall innerFindCall;
	
	@SuppressWarnings("unused")
	public List<Object[]> GetMatchingsFromPartial(HashMap<PVariable, Object> MatchingVariables)
	{
		if (true || innerFindCall.getReferredQuery().getAllAnnotations().contains(new PAnnotation("incremental")))
		{
			Multiset<LookaheadMatching> result = treatPatternCacher.GetMatchingsFromPartial(innerFindCall.getReferredQuery(), MatchingVariables, affectedVariables, true);
			// result must be parsed to List<Object[]>
			List<Object[]> ret = new ArrayList<Object[]>();
			// toarraylist false because only REAL matches count as a match, no need to count local-duplicated matches multiple mode
			for(LookaheadMatching match : result.elementSet())//.toArrayList(false))
			{
				// add all matchings as a "line" multi-matches only once
				ret.add(match.getParameterMatchValuesOnlyAsArray().toArray());
			}
			return ret;
		}
		return null;
	}
	
	@SuppressWarnings("unused")
	public int GetMatchCountFromPartial(HashMap<PVariable, Object> MatchingVariables)
	{
		if (true || innerFindCall.getReferredQuery().getAllAnnotations().contains(new PAnnotation("incremental")))
		{
			int result = treatPatternCacher.GetMatchCountFromPartial(innerFindCall.getReferredQuery(), MatchingVariables, affectedVariables, true);
			// result must be parsed to List<Object[]>
			return result;
		}
		// will throw exception!?:
		return -1;
	}
	
	public boolean IsAllAffectedVariablesMatched(HashMap<PVariable, Object> matchingVariables)
	{
		// mhm, can be evaluated when not all variables bound? no
		for (PVariable one : this.affectedVariables)
		{
			if (one.isVirtual() == false && Utils.isRunning(one) == false && matchingVariables.get(one) == null)
				return false;
		}
		return true;
	}
	
	@Override
	public String toString()
	{
		return this.innerFindCall.getReferredQuery().getFullyQualifiedName() + "(" + writeArray(this.affectedVariables) + ")";
	}
	
	public List<PVariable> getAffectedVariables()
	{
		return affectedVariables;
	}

	@SuppressWarnings("unused")
	private FindConstraint()
	{
	}
	
	private String writeArray(List<PVariable> affectedVars)
	{
		String ret = "";
		for (PVariable v : affectedVars)
		{
			ret += v.getName() + ",";
		}
		ret = ret.substring(0, ret.length() - 1);
		return ret;
	}

	public FindConstraint(PositivePatternCall findCons, IPartialPatternCacher treatPatternCacher)
	{
		this.treatPatternCacher = treatPatternCacher;
		this.innerFindCall = findCons;
		// and affecteds from tuple (order needed!)
		Tuple tup = findCons.getVariablesTuple();
		this.affectedVariables = new ArrayList<PVariable>();
		for(int i=0;i<tup.getSize();i++)
		{
			this.affectedVariables.add((PVariable) tup.get(i));
		}
	}

	public PositivePatternCall getInnerFindCall() {
		return innerFindCall;
	}

	public void setInnerFindCall(PositivePatternCall innerFindCall) {
		this.innerFindCall = innerFindCall;
	}
}
