package hu.bme.mit.inf.treatengine;

import hu.bme.mit.inf.lookaheadmatcher.IDelta;

public abstract class ModelChange implements IDelta
{
	public boolean isAddition()
	{
		return isAdd;
	}
	
	protected boolean isAdd;
}
