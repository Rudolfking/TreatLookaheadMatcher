package hu.bme.mit.inf.lookaheadmatcher.impl;

public abstract class AxisConstraint implements IConstraint
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
