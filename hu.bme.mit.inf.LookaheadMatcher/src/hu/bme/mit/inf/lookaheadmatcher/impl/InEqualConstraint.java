package hu.bme.mit.inf.lookaheadmatcher.impl;

import java.util.HashMap;

import org.eclipse.incquery.runtime.matchers.psystem.PVariable;
import org.eclipse.incquery.runtime.matchers.psystem.basicdeferred.Inequality;

public class InEqualConstraint extends CheckableConstraint implements IConstraint
{
	private Inequality innerInequalityConstraint;
	
	private PVariable leftVariable;
	public PVariable getLeftVariable() {
		return leftVariable;
	}

	public void setLeftVariable(PVariable leftVariable) {
		this.leftVariable = leftVariable;
	}

	public PVariable getRightVariable() {
		return rightVariable;
	}

	public void setRightVariable(PVariable rightVariable) {
		this.rightVariable = rightVariable;
	}

	private PVariable rightVariable;

	@Override
	public boolean Evaluate(HashMap<PVariable, Object> matchingVariables)
	{
		PVariable leftRoot = this.leftVariable.getUnifiedIntoRoot();
		PVariable rightRoot = this.rightVariable.getUnifiedIntoRoot();
		if (matchingVariables.get(leftRoot) == null ||matchingVariables.get(rightRoot) == null)
			return false; // can be evaluated? forgot to call? :)
		if (matchingVariables.get(leftRoot).equals(matchingVariables.get(rightRoot)))
			return false; // they are equal and they cannot be
		return true; // not equal, okay
	}

	@Override
	public boolean CanBeEvaluated(HashMap<PVariable, Object> matchingVariables)
	{
		if (matchingVariables.get(this.leftVariable) == null ||matchingVariables.get(this.rightVariable) == null)
			return false; // can be evaluated? forgot to call? :)
		return true; // not equal, okay
	}
	
	@SuppressWarnings("unused")
	private InEqualConstraint(){}
	
	public InEqualConstraint(PVariable left, PVariable right, Inequality ineq)
	{
		this.leftVariable = left;
		this.rightVariable = right;
		this.innerInequalityConstraint = ineq;
	}

	@Override
	public String toString()
	{
		return this.leftVariable.toString() + "=/=" + this.rightVariable.toString();
	}

	public Inequality getInnerInequalityConstraint() {
		return innerInequalityConstraint;
	}

	public void setInnerInequalityConstraint(Inequality innerInequalityConstraint) {
		this.innerInequalityConstraint = innerInequalityConstraint;
	}
}
