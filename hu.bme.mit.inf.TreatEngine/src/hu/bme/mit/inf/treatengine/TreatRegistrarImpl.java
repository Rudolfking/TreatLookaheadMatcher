package hu.bme.mit.inf.treatengine;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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
			if (TreatIQEngineDrot == null)
			{
				TreatIQEngineDrot = new HashMap<IncQueryEngine, LookaheadMatcherTreat>();
			}
			TreatIQEngineDrot.put(engine, lomatreRef);
			lomatreRef.subscribeToIndexer();
		}
		
		public static LookaheadMatcherTreat GetLookaheadMatcherTreat(IncQueryEngine engine)
		{
			if (TreatIQEngineDrot == null)
			{ 
				return null;
			}
			return TreatIQEngineDrot.get(engine);
		}
		
		public static boolean RemoveLookaheadMatcherTreat(IncQueryEngine engine)
		{
			if (TreatIQEngineDrot == null)
			{ 
				return false;
			}
			return TreatIQEngineDrot.remove(engine) != null;
		}
		
		public static void RemoveLookaheadMatcherTreat(LookaheadMatcherTreat treat)
		{
			if (TreatIQEngineDrot == null)
			{ 
				return;
			}
			List<IncQueryEngine> engsToRemove = new ArrayList<IncQueryEngine>();
			for (Entry<IncQueryEngine, LookaheadMatcherTreat> entry : TreatIQEngineDrot.entrySet())
			{
				if (entry.getValue().equals(treat))
					engsToRemove.add(entry.getKey());
			}
			for (IncQueryEngine eng : engsToRemove)
				TreatIQEngineDrot.remove(eng);
		}
		
		public static void Clean()
		{
			for (Entry<IncQueryEngine, LookaheadMatcherTreat> entry : TreatIQEngineDrot.entrySet())
			{
				entry.getValue().unregisterAll();
				entry.getValue().unsubscribeFromIndexer();
				entry.getValue().emptyAll();
			}
			TreatIQEngineDrot.clear();
			TreatIQEngineDrot = null;
		}
	}
	
}
