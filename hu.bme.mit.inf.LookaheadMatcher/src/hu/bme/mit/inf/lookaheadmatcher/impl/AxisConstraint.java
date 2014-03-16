package hu.bme.mit.inf.lookaheadmatcher.impl;


public abstract class AxisConstraint extends AConstraint
{
	public boolean isAxis()
	{
		return true;
	}
	public boolean isCheckable()
	{
		return false;
	}
}
