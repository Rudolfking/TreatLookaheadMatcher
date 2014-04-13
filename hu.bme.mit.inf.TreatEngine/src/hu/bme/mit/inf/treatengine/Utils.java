package hu.bme.mit.inf.treatengine;

import hu.bme.mit.inf.lookaheadmatcher.impl.NACConstraint;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.incquery.runtime.matchers.psystem.PVariable;

public class Utils
{
//	public static PVariable getVariableFromParamString(Set<PVariable> vars, String param)
//	{
//		for (PVariable var : vars)
//		{
//			if (var.getName().equals(param))
//				return var;
//		}
//		return null;
//	}

	public static boolean isProperIndex(NACConstraint nacC, IndexDelta indexDelta)
	{
		List<String> indexMightBe = new ArrayList<String>();
		for (PVariable s : nacC.getAffectedVariables())
		{
			if (isRunning(s) == false && s.isVirtual() == false)
				indexMightBe.add(s.getName()); // csunya, de vegul is ez a "parameter"
			else
				indexMightBe.add(null);
		}
		
		if ((indexDelta.getIndexVariables() == null && indexMightBe.size() == nacC.getAffectedVariables().size()) 
				|| sameNulls(indexDelta.getIndexVariables(),indexMightBe))
			return true;
		return false;
	}
	
	private static boolean sameNulls(List<String> listA, List<String> listB)
	{
		if (listA == null || listB == null)
			return false;
		if (listA.size() != listB.size())
			return false;
		for (int i=0;i<listA.size();i++)
		{
			if ((listA.get(i) == null && listB.get(i) != null) ||
				listA.get(i) != null && listB.get(i) == null)
				return false;
		}
		return true;
	}

	public static boolean isRunning(PVariable pVar)
	{
		if (pVar.isDeducable() == false || pVar.getName().startsWith("_")) // (o_O)
			return true;
		return false;
	}
}
