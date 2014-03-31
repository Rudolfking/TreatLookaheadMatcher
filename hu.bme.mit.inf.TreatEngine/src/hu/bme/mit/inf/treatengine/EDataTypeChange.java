package hu.bme.mit.inf.treatengine;

import org.eclipse.emf.ecore.EDataType;

public class EDataTypeChange extends ModelChange
{
	private EDataType change;
	private Object instance;
	
	public EDataTypeChange(EDataType changed, Object inst, boolean isAddition)
	{
		this.isAdd = isAddition;
		change = changed;
		instance = inst;
	}

	public EDataType getChange()
	{
		return change;
	}

	public Object getInstance()
	{
		return instance;
	}
}
