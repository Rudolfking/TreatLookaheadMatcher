package hu.bme.mit.inf.treatengine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.incquery.runtime.matchers.psystem.PQuery;
import org.eclipse.incquery.runtime.matchers.psystem.PVariable;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import hu.bme.mit.inf.lookaheadmatcher.IPartialPatternCacher;
import hu.bme.mit.inf.lookaheadmatcher.impl.LookaheadMatching;
import hu.bme.mit.inf.lookaheadmatcher.impl.MultiSet;

/**
 * Implementation of the IPartialPatternCacher class. Can cache partial matches, also incrementally
 * maintain the matches' consistency. (If forceMakeIndex is false and this partial match is not cached, returns 0!!)
 * @author Balazs Pal
 *
 */
public class TreatPartialPatternCacher implements IPartialPatternCacher
{

	public TreatPartialPatternCacher(LookaheadMatcherTreat lookTreat)
	{ 
		lookaheadTreat = lookTreat;
		// should not know about LookaheadMatcher itself (not enabled to use it yet!)
	}

	private LookaheadMatcherTreat lookaheadTreat;

	public LookaheadMatcherTreat getLookaheadTreat()
	{
		return lookaheadTreat;
	}
	
	private class PatternPartialMatchingData
	{
		PQuery theQuery;
		
		private Map<Set<PVariable>, Multimap<List<Object>, LookaheadMatching>> indexes = new HashMap<Set<PVariable>, Multimap<List<Object>, LookaheadMatching>>();

		public PatternPartialMatchingData(PQuery resolvingQuery)//, Set<PVariable> filterKeys, Collection<Object> filteringValues, MultiSet<LookaheadMatching> foundFilteredMatches)
		{
			theQuery = resolvingQuery;
			//insertOne(filterKeys, filteringValues, foundFilteredMatches);
		}
		
		/**
		 * Must resolve delta and update inner changes
		 * @param delta
		 */
		public void maintainIntegrity(Delta delta)
		{
			for (Entry<LookaheadMatching, Boolean> entry : delta.getChangeset().entries())
			{
				ezt meg kéne írni nem?
			}
			for (Entry<Set<PVariable>, Multimap<List<Object>, LookaheadMatching>> index : indexes.entrySet())
			{
				
			}
		}

//		public void insertOne(Set<PVariable> filterKeys, Collection<Object> filteringValues, MultiSet<LookaheadMatching> foundFilteredMatches)
//		{
//			Multimap<Collection<Object>, LookaheadMatching> szarTemp = HashMultimap.create();//<Collection<Object>, LookaheadMatching>();
//			szarTemp.putAll(filteringValues, foundFilteredMatches.toArrayList(true));
//			indexes.put(filterKeys, szarTemp);
//		}
		
		public void insertOne(Set<PVariable> filterKeys, Multimap<List<Object>, LookaheadMatching> foundFilteredMatchMap)
		{
			indexes.put(filterKeys, foundFilteredMatchMap);
		}
	}
	
	private HashMap<PQuery, PatternPartialMatchingData> patternsPartialMatchings = new HashMap<PQuery, PatternPartialMatchingData>();
	
	/**
	 * Might be faster to call this first!
	 */
	@Override
	public int GetMatchCountFromPartial(PQuery resolvingQuery, HashMap<PVariable, Object> partialMatching, List<PVariable> variablesInOrder, boolean forceMakeIndexIfNotIndexed)
	{
		// buta solution:
		return this.GetMatchingsFromPartial(resolvingQuery, partialMatching, variablesInOrder, forceMakeIndexIfNotIndexed).size();
	}



	@Override
	/**
	 * Get the possible matchings from a partial match (if forceMakeIndex is false and this partial match is not cached, returns 0!!)
	 */
	public MultiSet<LookaheadMatching> GetMatchingsFromPartial(PQuery resolvingQuery, HashMap<PVariable, Object> partialMatchingWithNullsAndEverything, List<PVariable> variablesInOrder, boolean forceMakeIndexIfNotIndexed)
	{
		if (partialMatchingWithNullsAndEverything == null)
			return this.lookaheadTreat.matchThePattern(resolvingQuery);
		
		HashMap<PVariable, Object> partialMatching = new HashMap<PVariable, Object>();
		// create a "partial matching" where nulls are removed (to use as key, and use counts etc.)
		for (Entry<PVariable, Object> entry : partialMatchingWithNullsAndEverything.entrySet())
		{
			if (entry.getValue() != null && variablesInOrder.contains(entry.getKey()))
				partialMatching.put(entry.getKey(), entry.getValue()); // add to watch set if (1) its value is not null (2) key is in affected variables of query ( parameter )
		}
		
		if (partialMatching.size() == 0)
		{
			// case is easy: get ALL matches! should check forcemakeindex (matchThePattern() will cache...)
			return this.lookaheadTreat.matchThePattern(resolvingQuery);
		}
		else
		{
			
			// the case is more complex: partial matching!
			if (patternsPartialMatchings.containsKey(resolvingQuery) && 
					patternsPartialMatchings.get(resolvingQuery).indexes.containsKey(partialMatching.keySet()))
			{
				// we have an index for this, return fast with it
				MultiSet<LookaheadMatching> ms = new MultiSet<LookaheadMatching>();
				Multimap<List<Object>, LookaheadMatching> valueMatchingPairs = patternsPartialMatchings.get(resolvingQuery).indexes.get(partialMatching.keySet());
				// Collection<Object> vals = partialMatching.values();
				ArrayList<Object> sortedVals = new ArrayList<Object>();
				for (PVariable pV : variablesInOrder)
				{
					if (partialMatching.get(pV) != null)
						sortedVals.add(partialMatching.get(pV));
				}
				Collection<LookaheadMatching> cacMach = valueMatchingPairs.get(sortedVals);
				ms.addAll(cacMach);
				return ms;
			}
			else
			{			
				// can we even cache this pattern? apparently: yes! (solve flattening later)
				
				// vars to hashset:
				HashSet<PVariable> affectedVars = new HashSet<PVariable>();
				affectedVars.addAll(variablesInOrder);
				
				
				// match! (if needed, else get from cache):
				// it is a MUST to call lookaheadTreat.matchThePattern() because if the pattern is new, only this will subscribe the patterns inner
				// type changes from the model (so we can maintain this partial matching). So: for a partial matching we ALWAYS have to know all matchings
				// for that specific pattern (~query). If not, we cannot even maintain the matchings (via deltas).
				MultiSet<LookaheadMatching> foundUnfilteredMatches = this.lookaheadTreat.matchThePattern(resolvingQuery);
				
				// get in-order values from the partial matching overlayed on the pattern call
				ArrayList<Object> sortedVals = new ArrayList<Object>();
				for(int i=0;i<variablesInOrder.size();i++)
				{
					// add item: if known, the known value, if not known, null
					sortedVals.add(partialMatching.containsKey(variablesInOrder.get(i)) ? partialMatching.get(variablesInOrder.get(i)) : null);
				}
				// filter the matchings
				Multimap<List<Object>, LookaheadMatching> foundFilteredMatchMap = HashMultimap.create();//<Collection<Object>, LookaheadMatching>();
				for (LookaheadMatching match : foundUnfilteredMatches.toArrayList(true))
				{
					if (match.getParameterMatchValuesOnlyAsArray().length != sortedVals.size())
					{
						System.err.println("The cached match size is not equal to requested match size! (Typical \"cannot happen\".)");
						// problem is big!
						int h = 0;
						int g = 5/h;
						continue;
					}
					// check equality:
					boolean okay = true;
					Object[] otherPatternsVals = match.getParameterMatchValuesOnlyAsArray(); // in-order
					ArrayList<Object> keyObjects = new ArrayList<Object>();
					for (int i = 0; i < otherPatternsVals.length; i++)
					{
						if (sortedVals.get(i) != null) // key item
							keyObjects.add(otherPatternsVals[i]);
						
					}
					if (okay)
					{
						foundFilteredMatchMap.put(keyObjects, match);
					}
				}
				
				// this is only to get the length
				if (patternsPartialMatchings.containsKey(resolvingQuery))
				{
					// we have some index for this, add a new one
					patternsPartialMatchings.get(resolvingQuery).insertOne(partialMatching.keySet(), foundFilteredMatchMap);
				}
				else	
				{
					// add a completely new pattern-indexes class instance
					patternsPartialMatchings.put(resolvingQuery, new PatternPartialMatchingData(resolvingQuery));//, partialMatching.keySet(), partialMatching.values(), foundFilteredMatches)); // like insertOne (constructor calls anyway)
					patternsPartialMatchings.get(resolvingQuery).insertOne(partialMatching.keySet(), foundFilteredMatchMap);
				}
				MultiSet<LookaheadMatching> ret = new MultiSet<LookaheadMatching>();
				for (int i = 0; i < sortedVals.size();i++)
				{
					if (sortedVals.get(i) == null)
					{
						sortedVals.remove(i);
						i--;
					}
				}
				ret.addAll(patternsPartialMatchings.get(resolvingQuery).indexes.get(partialMatching.keySet()).get(sortedVals));
				return ret;
			}
		}
	}
	
	public void ProcessADelta(Delta delta)
	{
		if (this.patternsPartialMatchings.get(delta.getPattern()) != null) // if in indexed list!
			this.patternsPartialMatchings.get(delta.getPattern()).maintainIntegrity(delta); // add delta to maintain
	}
}
