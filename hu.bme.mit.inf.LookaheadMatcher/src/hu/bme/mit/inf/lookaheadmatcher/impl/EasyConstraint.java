package hu.bme.mit.inf.lookaheadmatcher.impl;

import org.eclipse.incquery.runtime.matchers.psystem.PVariable;
import org.eclipse.incquery.runtime.matchers.psystem.basicenumerables.ConstantValue;

public class EasyConstraint extends AxisConstraint // implements IConstraint
{
	private PVariable onlyVariable;
	private Object knownValue;
	private ConstantValue innerConstantConstraint;
	
	public PVariable getOnlyVariable() {
		return onlyVariable;
	}
	public void setOnlyVariable(PVariable var) {
		this.onlyVariable = var;
	}
	public Object getKnownValue() {
		return knownValue;
	}
	public void setKnownValue(Object knownValue) {
		this.knownValue = knownValue;
	}
	
	@SuppressWarnings("unused")
	private EasyConstraint(){}
	
//	public EasyConstraint(LookVariable lookVariable, Object alreadyKnownValue)
//	{
//		this.lookvariable = lookVariable;
//		this.knownValue = alreadyKnownValue;
//	}
	
	public EasyConstraint(ConstantValue easyCons)
	{
		this.setInnerConstantConstraint(easyCons);
	}
	@Override
	public String toString()
	{
		return this.onlyVariable.getName()+"="+this.knownValue.toString();
	}
	public ConstantValue getInnerConstantConstraint() {
		return innerConstantConstraint;
	}
	public void setInnerConstantConstraint(ConstantValue innerConstantConstraint) {
		this.innerConstantConstraint = innerConstantConstraint;
		this.onlyVariable = (PVariable) innerConstantConstraint.getVariablesTuple().get(0);
		this.knownValue = innerConstantConstraint.getSupplierKey();
	}
}
