package hu.bme.mit.inf.treatengine;

import hu.bme.mit.inf.lookaheadmatcher.impl.NACConstraint;

import java.util.HashSet;
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

	public static boolean isProperIndex(NACConstraint nacC, IndexDelta indexDelta)
	{
		Set<String> indexMightBe = new HashSet<String>();
		for (PVariable s : nacC.getAffectedVariables())
		{
			if (isRunning(s) == false && s.isVirtual() == false)
				indexMightBe.add(s.getName()); // csunya, de vegul is ez a "parameter"
		}
		
		if (indexDelta.getIndexVariables().equals(indexMightBe))
			return true;
		return false;
	}
	
	public static boolean isRunning(PVariable pVar)
	{
		if (pVar.isDeducable() == false || pVar.getName().startsWith("_")) // (o_O)
			return true;
		return false;
	}
}
