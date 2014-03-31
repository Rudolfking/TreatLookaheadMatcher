package hu.bme.mit.inf.treatengine;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;

public class EClassChange extends ModelChange
{
	private EClass change;
	private EObject instance;
	
	public EClassChange(EClass changed, EObject inst, boolean isAddition)
	{
		this.isAdd = isAddition;
		change = changed;
		instance = inst;
	}

	public EClass getChange()
	{
		return change;
	}

	public EObject getInstance()
	{
		return instance;
	}
}
