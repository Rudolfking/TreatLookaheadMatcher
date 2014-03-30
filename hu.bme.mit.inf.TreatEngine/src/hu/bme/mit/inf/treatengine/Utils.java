package hu.bme.mit.inf.treatengine;

import java.util.Set;

import org.eclipse.incquery.runtime.matchers.psystem.PVariable;

public class Utils
{
	public static PVariable getVariableFromParamString(Set<PVariable> vars, String param)
	{
		for (PVariable var : vars)
		{
			if (var.getName().equals(param))
				return var;
		}
		return null;
	}
}
