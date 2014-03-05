package hu.bme.mit.inf.treatengine;


import java.util.HashMap;
import java.util.Map;

import org.eclipse.incquery.runtime.api.IncQueryEngine;


public class TreatRegistrarImpl
{
	public static class LookaheadToEngineConnector
	{
		// this is a hack to keep my engine in memory by attaching to the incqueryengine
		// garbage collector cannot free it (and all the matched pattern caches) and it's static, too
		private static Map<IncQueryEngine, LookaheadMatcherTreat> TreatIQEngineDrot = null;
		
		public static void Connect(IncQueryEngine engine, LookaheadMatcherTreat lomatreRef)
		{
			if (TreatIQEngineDrot != null)
				return;
			TreatIQEngineDrot = new HashMap<IncQueryEngine, LookaheadMatcherTreat>();
			TreatIQEngineDrot.put(engine,lomatreRef);
		}
		
		public static LookaheadMatcherTreat GetLookaheadMatcherTreat(IncQueryEngine engine)
		{
			if (TreatIQEngineDrot == null)
			{ 
				return null;
			}
			return TreatIQEngineDrot.get(engine);
		}
	}
	
}
