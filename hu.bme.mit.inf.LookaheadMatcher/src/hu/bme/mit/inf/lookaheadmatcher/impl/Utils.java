package hu.bme.mit.inf.lookaheadmatcher.impl;

import org.eclipse.incquery.runtime.matchers.psystem.PVariable;

public class Utils
{
	public static boolean isRunning(PVariable pVar)
	{
		if (pVar.getName().startsWith("_")) // (o_O)
			return true;
		return false;
	}
}
