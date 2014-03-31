package hu.bme.mit.inf.treatengine;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;

public class EFeatureChange extends ModelChange
{
	private EStructuralFeature change;
	private EObject host;
	private Object instance;
	
	public EFeatureChange(EObject hostObj, EStructuralFeature featueChanged, Object inst, boolean isAddition)
	{
		this.isAdd = isAddition;
		host = hostObj;
		change = featueChanged;
		instance = inst;
	}

	public EObject getHost()
	{
		return host;
	}

	public EStructuralFeature getChangedFeature()
	{
		return change;
	}

	public Object getInstance()
	{
		return instance;
	}
}
