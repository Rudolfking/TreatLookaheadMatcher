package hu.bme.mit.inf.treatengine;

import hu.bme.mit.inf.lookaheadmatcher.IPartialPatternCacher;
import hu.bme.mit.inf.lookaheadmatcher.impl.LookaheadMatching;

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
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;

/**
 * Implementation of the IPartialPatternCacher class. Can cache partial matches, also incrementally
 * maintain the matches' consistency. (If forceMakeIndex is false and this partial match is not cached, returns 0!!)
 * @author Balazs Pal
 *
 */
public class TreatPartialPatternCacher implements IPartialPatternCacher
{

	public TreatPartialPatternCacher()
	{
		// should not know about LookaheadMatcher itself (not enabled to use it yet!)
		patternsPartialMatchings = new HashMap<PQuery, PatternPartialMatchingData>();
	}

	private LookaheadMatcherTreat lookaheadTreat;

	public LookaheadMatcherTreat getLookaheadTreat()
	{
		return lookaheadTreat;
	}
	public void setLookaheadTreat(LookaheadMatcherTreat lookTreat)
	{
		lookaheadTreat = lookTreat;
	}
	
	private class PatternPartialMatchingData
	{
		PQuery theQuery;
		List<String> theVariablesInOrder;
		
		private Map<Set<String>, Multimap<List<Object>, LookaheadMatching>> indexes = new HashMap<Set<String>, Multimap<List<Object>, LookaheadMatching>>();

		public PatternPartialMatchingData(PQuery resolvingQuery, List<String> variablesInOrder)//, Set<PVariable> filterKeys, Collection<Object> filteringValues, MultiSet<LookaheadMatching> foundFilteredMatches)
		{
			theQuery = resolvingQuery;
			theVariablesInOrder = variablesInOrder;
		}
		
		/**
		 * Must resolve delta and update inner changes
		 * @param delta
		 */
		public Collection<IndexDelta> maintainIntegrity(Delta delta)
		{
			Set<IndexDelta> deltaSet = new HashSet<IndexDelta>();
			for (Entry<Set<String>, Multimap<List<Object>, LookaheadMatching>> index : indexes.entrySet()) // helyi jarat
			{
				// the changeMap for THIS index
				HashMap<LookaheadMatching, Boolean> changeMap = new HashMap<LookaheadMatching, Boolean>();
				
				for (Entry<LookaheadMatching, Boolean> entry : delta.getChangeset().entrySet()) // for all change in delta
				{
					LookaheadMatching changMatch = entry.getKey();
					// construct the key
					List<Object> currentNewKey = new ArrayList<Object>();
					for (String oneVar : this.theVariablesInOrder)
					{
						if (index.getKey().contains(oneVar))
						{
							currentNewKey.add(changMatch.get(oneVar));//.getMatches().get(Utils.getVariableFromParamString(changMatch.getMatches().keySet(), oneVar)));
						}
					}
					if (entry.getValue())
					{
						if (index.getValue().get(currentNewKey).size() == 0)
						{
							// first+ yeah
							changeMap.put(changMatch, true);
						}
						
						index.getValue().put(currentNewKey, changMatch);
					}
					else
					{
						index.getValue().remove(currentNewKey, changMatch);
						
						if (index.getValue().get(currentNewKey).size() == 0)
						{
							// it was the last!!!!!
							changeMap.put(changMatch, false);
						}
					}
				}
				
				List<String> indexesOrdered = new ArrayList<String>();
				for (String strParam : delta.getPattern().getParameterNames())
				{
					boolean found = false;
					for (String keyParam: index.getKey())
					{
						if (strParam.equals(keyParam))
						{
							indexesOrdered.add(strParam);
							found = true;
						}
					}
					if (!found)
						indexesOrdered.add(null);
				}

				if (changeMap.size() > 0)
					deltaSet.add(new IndexDelta(this.theQuery, changeMap, indexesOrdered));
			}
			// the delta for THIS index
			if (deltaSet.size() != 0)
				return deltaSet;
			return null;
		}
		
		public void insertOne(Set<String> filterKeys, Multimap<List<Object>, LookaheadMatching> foundFilteredMatchMap)
		{
			indexes.put(filterKeys, foundFilteredMatchMap);
		}
	}
	
	private HashMap<PQuery, PatternPartialMatchingData> patternsPartialMatchings;
	
	/**
	 * Might be faster to call this first!
	 */
	@Override
	public int GetMatchCountFromPartial(PQuery resolvingQuery, HashMap<PVariable, Object> partialMatchingWithNullsAndEverything,
			List<PVariable> variablesInOrder, boolean forceMakeIndexIfNotIndexed)
	{
		// if no hints added, return all:
		if (partialMatchingWithNullsAndEverything == null)
			return this.lookaheadTreat.matchThePattern(resolvingQuery).size();

		// the called patterns variables in order (only known)
		List<String> calledPatternsVariablesInOrder = resolvingQuery.getParameterNames();
		
		HashMap<PVariable, Object> partialMatching = new HashMap<PVariable, Object>();
		// create a "partial matching" where nulls are removed (to use as key, and use counts etc.)
		for (Entry<PVariable, Object> entry : partialMatchingWithNullsAndEverything.entrySet())
		{
			if (entry.getValue() != null && variablesInOrder.contains(entry.getKey()))
				partialMatching.put(entry.getKey(), entry.getValue()); // add to watch set if (1) its value is not null (2) key is in affected variables of query ( parameter )
		}
		if (partialMatching.size() == 0)
		{
			// match everything
			return this.lookaheadTreat.matchThePattern(resolvingQuery).size();
		}
		else if (partialMatching.size() == variablesInOrder.size())
		{
//			Multiset<LookaheadMatching> curr = this.lookaheadTreat.matchThePattern(resolvingQuery);
			ArrayList<Object> sortedVals = new ArrayList<Object>();
			for(int i=0;i<variablesInOrder.size();i++)
			{
				// add item: if known, the known value, if not known, null
				sortedVals.add(partialMatching.containsKey(variablesInOrder.get(i)) ? partialMatching.get(variablesInOrder.get(i)) : null);
			}
			LookaheadMatching chec = new LookaheadMatching(calledPatternsVariablesInOrder, sortedVals);
			return this.lookaheadTreat.matchThePattern(resolvingQuery).count(chec);
//			int ret = 0;
//			main: for (LookaheadMatching item : curr.elementSet())//.toArrayList(false))
//			{
//				inner: for (int i = 0; i< item.getParameterMatchValuesOnlyAsArray().size();i++)
//				{
//					Object obj1=item.getParameterMatchValuesOnlyAsArray().get(i);
//					Object obj2=sortedVals.get(i);
//					if (obj1.equals(obj2) == false)
//						continue main;
//				}
//				ret++; // item is found
//			}
//			return ret; // item not found!
		}
		
		// determine key:
		HashSet<String> calledPatternsIndexParamsKeySet = new HashSet<String>();
		for (int i=0;i<variablesInOrder.size();i++)
		{
			if (partialMatching.get(variablesInOrder.get(i)) != null)
				calledPatternsIndexParamsKeySet.add(calledPatternsVariablesInOrder.get(i));
		}
		
		// use key to determine: do we have an index for that?
		if (patternsPartialMatchings.containsKey(resolvingQuery) && 
				patternsPartialMatchings.get(resolvingQuery).indexes.containsKey(calledPatternsIndexParamsKeySet))
		{
			ArrayList<Object> sortedVals = new ArrayList<Object>();
			for (PVariable pV : variablesInOrder)
			{
				if (partialMatching.get(pV) != null)
					sortedVals.add(partialMatching.get(pV));
			}
			// we have an index for this! yeah, return fast with size
			return patternsPartialMatchings.get(resolvingQuery).indexes.get(calledPatternsIndexParamsKeySet).get(sortedVals).size();
		}
		else
		{
			// build index... it is slow, but will be fast when iterating
			return this.GetMatchingsFromPartial(resolvingQuery, partialMatching, variablesInOrder, forceMakeIndexIfNotIndexed).size();
		}
	}



	@Override
	/**
	 * Get the possible matchings from a partial match (if forceMakeIndex is false and this partial match is not cached, returns 0!!)
	 */
	public Multiset<LookaheadMatching> GetMatchingsFromPartial(PQuery resolvingQuery, HashMap<PVariable, Object> partialMatchingWithNullsAndEverything,
			List<PVariable> variablesInOrder, boolean forceMakeIndexIfNotIndexed)
	{
		if (partialMatchingWithNullsAndEverything == null)
			return this.lookaheadTreat.matchThePattern(resolvingQuery);

		List<String> calledPatternsVariablesInOrder = resolvingQuery.getParameterNames();
		
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
		else if (partialMatching.size() == variablesInOrder.size())
		{
//			Multiset<LookaheadMatching> curr = this.lookaheadTreat.matchThePattern(resolvingQuery);
			ArrayList<Object> sortedVals = new ArrayList<Object>();
			for(int i=0;i<variablesInOrder.size();i++)
			{
				// add item: if known, the known value, if not known, null
				sortedVals.add(partialMatching.containsKey(variablesInOrder.get(i)) ? partialMatching.get(variablesInOrder.get(i)) : null);
			}
			LookaheadMatching chec = new LookaheadMatching(calledPatternsVariablesInOrder, sortedVals);
			Multiset<LookaheadMatching> msRet = HashMultiset.create();//new MultiSet<LookaheadMatching>();
			msRet.add(chec, this.lookaheadTreat.matchThePattern(resolvingQuery).count(msRet));
			return msRet;
//			main: for (LookaheadMatching item : curr.elementSet())//.toArrayList(true))
//			{
//				inner: for (int i = 0; i< item.getParameterMatchValuesOnlyAsArray().size();i++)
//				{
//					Object obj1=item.getParameterMatchValuesOnlyAsArray().get(i);
//					Object obj2=sortedVals.get(i);
//					if (obj1.equals(obj2) == false)
//						continue main;
//				}
//				msRet.add(item); 
//			}
//			return msRet; // item not found!
		}
		else
		{
			// the case is more complex: partial matching!
			
			// create the key for indexing (index key)
			HashSet<String> calledPatternsIndexParamsKeySet = new HashSet<String>();
			for (int i=0;i<variablesInOrder.size();i++)
			{
				if (partialMatching.get(variablesInOrder.get(i)) != null)
					calledPatternsIndexParamsKeySet.add(calledPatternsVariablesInOrder.get(i));
			}
			
			if (patternsPartialMatchings.containsKey(resolvingQuery) && 
					patternsPartialMatchings.get(resolvingQuery).indexes.containsKey(calledPatternsIndexParamsKeySet))
			{
				// we have an index for this, return fast with it
				Multimap<List<Object>, LookaheadMatching> valueMatchingPairs = patternsPartialMatchings.get(resolvingQuery).indexes.get(calledPatternsIndexParamsKeySet);
				// Collection<Object> vals = partialMatching.values();
				ArrayList<Object> sortedVals = new ArrayList<Object>();
				for (PVariable pV : variablesInOrder)
				{
					if (partialMatching.get(pV) != null)
						sortedVals.add(partialMatching.get(pV));
				}
				Multiset<LookaheadMatching> ms = HashMultiset.create();//new MultiSet<LookaheadMatching>();
				Collection<LookaheadMatching> cacMach = valueMatchingPairs.get(sortedVals);
				ms.addAll(cacMach);
				return ms;
			}
			else
			{			
				// can we even cache this pattern? apparently: yes! (solve flattening later)
					
				// match! (if needed, else get from cache):
				// it is a MUST to call lookaheadTreat.matchThePattern() because if the pattern is new, only this will subscribe the patterns inner
				// type changes from the model (so we can maintain this partial matching). So: for a partial matching we ALWAYS have to know all matchings
				// for that specific pattern (~query). If not, we cannot even maintain the matchings (via deltas).
				Multiset<LookaheadMatching> foundUnfilteredMatches = this.lookaheadTreat.matchThePattern(resolvingQuery);
								
				// get in-order values from the partial matching overlayed on the pattern call
				ArrayList<Object> sortedVals = new ArrayList<Object>();
				for(int i=0;i<variablesInOrder.size();i++)
				{
					// add item: if known, the known value, if not known, null
					sortedVals.add(partialMatching.containsKey(variablesInOrder.get(i)) ? partialMatching.get(variablesInOrder.get(i)) : null);
				}
				// filter the matchings
				Multimap<List<Object>, LookaheadMatching> foundFilteredMatchMap = HashMultimap.create();//<Collection<Object>, LookaheadMatching>();
				
				// check all matches and filter it according to the condition: create the index set!
				for (com.google.common.collect.Multiset.Entry<LookaheadMatching> match : foundUnfilteredMatches.entrySet())//.toArrayList(true))
				{
					if (match.getElement().getParameterMatchValuesOnlyAsArray().size() != sortedVals.size())
					{
						System.err.println("The cached match size is not equal to requested match size! (Typical \"cannot happen\".)");
						// problem is big!
						throw new AssertionError("The cached match size is not equal to requested match size!");
					}
					// check equality:
					boolean okay = true;
					List<Object> otherPatternsVals = match.getElement().getParameterMatchValuesOnlyAsArray(); // in-order
					ArrayList<Object> keyObjects = new ArrayList<Object>();
					for (int i = 0; i < otherPatternsVals.size(); i++)
					{
						if (sortedVals.get(i) != null) // key item
							keyObjects.add(otherPatternsVals.get(i));
						
					}
					if (okay)
					{
						for (int i=0;i<match.getCount();i++)
							foundFilteredMatchMap.put(keyObjects, match.getElement());
					}
				}
				
				// this is only to get the length
				if (patternsPartialMatchings.containsKey(resolvingQuery))
				{
					// we have some index for this, add a new one
					patternsPartialMatchings.get(resolvingQuery).insertOne(calledPatternsIndexParamsKeySet, foundFilteredMatchMap);
				}
				else	
				{
					// add a completely new pattern-indexes class instance
					patternsPartialMatchings.put(resolvingQuery, new PatternPartialMatchingData(resolvingQuery, calledPatternsVariablesInOrder));//, partialMatching.keySet(), partialMatching.values(), foundFilteredMatches)); // like insertOne (constructor calls anyway)
					patternsPartialMatchings.get(resolvingQuery).insertOne(calledPatternsIndexParamsKeySet, foundFilteredMatchMap);
				}
				Multiset<LookaheadMatching> ret = HashMultiset.create();//new Multiset<LookaheadMatching>();
				for (int i = 0; i < sortedVals.size();i++)
				{
					if (sortedVals.get(i) == null)
					{
						sortedVals.remove(i);
						i--;
					}
				}
				ret.addAll(patternsPartialMatchings.get(resolvingQuery).indexes.get(calledPatternsIndexParamsKeySet).get(sortedVals));
				return ret;
			}
		}
	}
	
	public Collection<IndexDelta> ProcessADelta(Delta delta)
	{
		Delta ddelta = (Delta) delta;
		if (this.patternsPartialMatchings.get(ddelta.getPattern()) != null) // if in indexed list!
		{
			Collection<IndexDelta> ret = this.patternsPartialMatchings.get(ddelta.getPattern()).maintainIntegrity(ddelta); // add delta to maintain
			if (ret == null || ret.size() == 0)
			{
				if (ret == null)
					ret = new HashSet<IndexDelta>();
				// create an empty-type indexdelta if no indexes found (TODO this is bad i think)
				IndexDelta id = new IndexDelta(delta.getPattern(), delta.getChangeset(), null);
				ret.add(id);
				return ret;
			}
			else return ret;
		}
		Collection<IndexDelta> retNl = new HashSet<IndexDelta>();
		IndexDelta id = new IndexDelta(delta.getPattern(), delta.getChangeset(), null);
		retNl.add(id);
		return retNl;
	}
	
	public void clean() 
	{		
		patternsPartialMatchings.clear();
		patternsPartialMatchings = new HashMap<>();
	}
}
