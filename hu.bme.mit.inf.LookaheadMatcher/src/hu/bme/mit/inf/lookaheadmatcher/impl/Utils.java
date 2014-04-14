package hu.bme.mit.inf.lookaheadmatcher.impl;

import java.util.List;

import org.eclipse.incquery.runtime.matchers.psystem.PVariable;

public class Utils
{
	public static boolean isRunning(PVariable pVar)
	{
		if (pVar.isDeducable() == false || pVar.getName().startsWith("_"))
			return true;
		return false;
	}
}
