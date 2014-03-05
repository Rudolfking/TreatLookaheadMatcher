//package hu.bme.mit.inf.lookaheadmatcher.impl;
//
//import java.util.ArrayList;
//
//import org.eclipse.incquery.patternlanguage.patternLanguage.Variable;
//import org.eclipse.incquery.runtime.matchers.psystem.PVariable;
//
//public class LookVariable
//{
//	private String variableName;
//	private String prefix;
//	private boolean isPrefixed;
//	private Variable variable;
//	private boolean isSymbolic;
//	
//	private PVariable innerVariable;
//
//	public ArrayList<LookVariable> Children;
//	private LookVariable ancestor;
//	
//	public String getVariableName() {
//		return innerVariable.getName();
//	}
//	public String getPrefix() {
//		return prefix;
//	}
//	public boolean isPrefixed() {
//		return isPrefixed;
//	}
//	public Variable getVariable() {
//		return this.variable;
//	}
//	public boolean isSymbolic() {
//		return isSymbolic;
//	}
//	public void setSymbolic(boolean symbolic) {
//		isSymbolic = symbolic;
//	}
//	
//	public LookVariable getAncestor() {
//		return ancestor;
//	}
//	public void setAncestor(LookVariable ancestor) {
//		this.ancestor = ancestor;
//	}
//	
//	@SuppressWarnings("unused")
//	private LookVariable()
//	{
//		super();
//	}
//	public LookVariable(Variable var, String variableName)
//	{
//		this.variable = var;
//		this.variableName = variableName;
//		this.isPrefixed=false;
//		this.isSymbolic=false;
//		
//		ancestor = null;
//		Children = new ArrayList<LookVariable>();
//	}
//	public LookVariable(String prefixlessVariableName)
//	{
//		this.variable = null;
//		this.variableName = prefixlessVariableName;
//		this.isPrefixed=false;
//		this.isSymbolic=false;
//		
//		ancestor = null;
//		Children = new ArrayList<LookVariable>();
//	}
//	public LookVariable(String prefixlessVariableName, String prefix)
//	{
//		this.variable = null;
//		this.variableName = prefix+prefixlessVariableName;
//		this.prefix=prefix;
//		this.isPrefixed=true;
//		this.isSymbolic=false;
//		
//		ancestor = null;
//		Children = new ArrayList<LookVariable>();
//	}
//	public LookVariable(String prefixlessVariableName, String prefix, Variable var)
//	{
//		this.variable = var;
//		this.variableName = prefix+prefixlessVariableName;
//		this.prefix=prefix;
//		this.isPrefixed=true;
//		this.isSymbolic=false;
//		
//		ancestor = null;
//		Children = new ArrayList<LookVariable>();
//	}
//	
//	public LookVariable(PVariable inner)
//	{
//		this.innerVariable = inner;
//	}
//	
//	public LookVariable findRoot()
//	{
//		// step up in the union-where tree
//		LookVariable rette = this;
//		while(rette.ancestor != null)
//		{
//			rette = rette.ancestor;
//		}
//		return rette;
//	}
//
//	@Override
//	public String toString()
//	{
//		return this.variableName;
//	}
//	
//	public PVariable getInnerVariable() {
//		return innerVariable;
//	}
//	public void setInnerVariable(PVariable innerVariable) {
//		this.innerVariable = innerVariable;
//	}
//}
