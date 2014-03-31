package hu.bme.mit.inf.treatengine;

public abstract class ModelChange
{
	public boolean isAddition()
	{
		return isAdd;
	}
	
	protected boolean isAdd;
}
