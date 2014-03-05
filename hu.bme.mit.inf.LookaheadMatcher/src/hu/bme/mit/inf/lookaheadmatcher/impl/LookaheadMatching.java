package hu.bme.mit.inf.lookaheadmatcher.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.incquery.runtime.api.IPatternMatch;
import org.eclipse.incquery.runtime.api.IQuerySpecification;
import org.eclipse.incquery.runtime.api.IncQueryMatcher;
import org.eclipse.incquery.runtime.matchers.psystem.PVariable;

public class LookaheadMatching implements IPatternMatch
{
	private HashMap<PVariable, Object> matches;
	public HashMap<PVariable, Object> getMatches()
	{
		return matches;
	}
	public void setMatches(HashMap<PVariable, Object> matches)
	{
		this.matches = matches;
	}
	
	private PVariable[] ParameterVariables;
	public PVariable[] getParameterVariables()
	{
		return ParameterVariables;
	}
	public void setParameterVariables(PVariable[] parameterLookVariables)
	{
		ParameterVariables = parameterLookVariables;
	}
	
	public HashMap<PVariable, Object> getParameterMatchesOnly()
	{
		HashMap<PVariable, Object> ret = new HashMap<PVariable, Object>();
		for (PVariable parVar : ParameterVariables)
		{
			ret.put(parVar, this.matches.get(parVar));
		}
		return ret;
	}
	public Object[] getParameterMatchValuesOnlyAsArray()
	{
		Object[] ret = new Object[this.ParameterVariables.length];
		int ind = 0;
		for (PVariable parVar : ParameterVariables)
		{
			ret[ind++] = this.matches.get(parVar);
		}
		return ret;
	}
	
	
	@SuppressWarnings("unused")
	private LookaheadMatching()
	{
		super();
	}
	
	public LookaheadMatching(PVariable[] variables, HashMap<PVariable, Object> foundMatches)
	{
		this.ParameterVariables = variables.clone();
		
		this.matches = new HashMap<>();
		for (int i = 0; i < variables.length; i++)
		{
			this.matches.put(variables[i], foundMatches.get(variables[i]));
		}
	}
	
	@Override
	public String toString()
	{
		String ret = "";
		for (Map.Entry<PVariable, Object> entry : this.matches.entrySet())
		{
			ret += "(" + entry.getKey().toString() + "->" + entry.getValue().toString() + ") ";
		}
		return ret;
	}
	
	@Override
	public boolean equals(Object obj)
	{
		if (!(obj instanceof LookaheadMatching))
			return false;
		if (((LookaheadMatching) obj).matches.equals(this.matches))
			return true;
		return false;
		/*LookaheadMatching lookM = ((LookaheadMatching)obj);
		int el,ma;el=ma=0;
		for (Object o : lookM.allMatchedElements) {
			el+=o.hashCode();
		}
		for (Object o : this.allMatchedElements) {
			ma+=o.hashCode();
		}
		if (el==ma)
			return matches.equals(lookM.matches);
		return false;*/
	}
	
	@Override
	public int hashCode()
	{
		/*int ma = 0;
		for (Object o : this.allMatchedElements) {
			ma += o.hashCode();
		}*/
		return matches.hashCode();// + ma;
	}
	
	private PVariable getVariableFromName(String name) {
		for (PVariable item : this.ParameterVariables) {
			if (item.getName().equals(name))
				return item;
		}
		return null;
	}
	
	@Override
	public IQuerySpecification<? extends IncQueryMatcher<? extends IPatternMatch>> specification() {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public String patternName() {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public List<String> parameterNames() {
		ArrayList<String> ret = new ArrayList<String>();
		for (PVariable par : this.ParameterVariables) {
			ret.add(par.getName());
		}
		return ret;
	}
	@Override
	public Object get(String parameterName) {
		
		return this.matches.get(getVariableFromName(parameterName));
	}
	@Override
	public Object get(int position) {
		return this.matches.get(this.ParameterVariables[position]);
	}
	@Override
	public boolean set(String parameterName, Object newValue) {
		return this.matches.put(getVariableFromName(parameterName), newValue) != null;
	}
	@Override
	public boolean set(int position, Object newValue) {
		return this.matches.put(this.ParameterVariables[position], newValue) != null;
	}
	@Override
	public boolean isMutable() {
		return false;
	}
	@Override
	public Object[] toArray() {
		return this.matches.values().toArray();
	}
	@Override
	public String prettyPrint() {
		return this.toString();
	}
	@Override
	public boolean isCompatibleWith(IPatternMatch other) {
		return false;
	}
}
