package hu.bme.mit.inf.treatengine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import hu.bme.mit.inf.lookaheadmatcher.IDelta;
import hu.bme.mit.inf.lookaheadmatcher.IPartialPatternCacher;
import hu.bme.mit.inf.lookaheadmatcher.LookaheadMatcherInterface;
import hu.bme.mit.inf.lookaheadmatcher.PatternCallModes;
import hu.bme.mit.inf.lookaheadmatcher.impl.AheadStructure;
import hu.bme.mit.inf.lookaheadmatcher.impl.AxisConstraint;
import hu.bme.mit.inf.lookaheadmatcher.impl.CheckableConstraint;
import hu.bme.mit.inf.lookaheadmatcher.impl.FindConstraint;
import hu.bme.mit.inf.lookaheadmatcher.impl.LookaheadMatching;
import hu.bme.mit.inf.lookaheadmatcher.impl.MultiSet;
import hu.bme.mit.inf.lookaheadmatcher.impl.NACConstraint;

import org.eclipse.incquery.runtime.api.IncQueryEngine;
import org.eclipse.incquery.runtime.exception.IncQueryException;
import org.eclipse.incquery.runtime.matchers.psystem.PQuery;
import org.eclipse.incquery.runtime.matchers.psystem.PVariable;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

public class AdvancedDeltaProcessor
{
	private IncQueryEngine engine;
	private IPartialPatternCacher treatpartialCacher;
	
	private static AdvancedDeltaProcessor instance = null;
	public static AdvancedDeltaProcessor getInstance()
	{
		if (instance == null)
			instance = new AdvancedDeltaProcessor();
		return instance;
	}
	
	public void setEngine(IncQueryEngine engineRe)
	{
		this.engine = engineRe;
	}
	public void setPartialCacher(IPartialPatternCacher treatPartial)
	{
		this.treatpartialCacher = treatPartial;
	}

	// private List<Delta> receivedDeltas;
	private Set<PQuery> affectedPatterns;
	
	/**
	 * Receives a delta from the changes, collects into a list
	 * @param d the delta
	 */
	public void ReceiveDelta(IDelta d)
	{
		// receives a delta
		
		// collect affected patterns (quite important)
		if (affectedPatterns == null)
			affectedPatterns = new HashSet<PQuery>();
		
		// update THIS patterns matchings, and if modelDelta, make Delta of it
		Delta properPropagatableDelta = updatePatternsMatchingsFromDelta(d);
		// update indexes!
		Collection<IndexDelta> negFindDeltas = updateIndexesFromDelta(properPropagatableDelta);
		
		// mailbox: deliver deltas and get call hierarchy to extend affected pattern list
		Multimap<PQuery, Boolean> affPats = deliverDelta(properPropagatableDelta);
		if (negFindDeltas != null)
		{
			for (IndexDelta id : negFindDeltas)
			{
				Multimap<PQuery, Boolean> moreAffPats = deliverDelta(id);
				affectedPatterns.addAll(moreAffPats.keySet());
			}
		}
		affectedPatterns.addAll(affPats.keySet());
	}

	@SuppressWarnings("rawtypes")
	public void ProcessReceivedDeltaSet()
	{
		if (affectedPatterns == null || affectedPatterns.size() == 0)
			return; // easy prey
		
		@SuppressWarnings("unchecked")
		HashSet<PQuery> currentAffectedPatterns = (HashSet<PQuery>) ((HashSet) affectedPatterns).clone();
		
		Set<PQuery> topologicalFirstPatterns = getTopologicalOkayPatterns(currentAffectedPatterns);
		
		// evaluate topological okay patterns:
		for (PQuery topOK : topologicalFirstPatterns)
		{
			Delta theChangeForThisPattern = processAllDeltaInMailboxesForPattern(topOK);
			if (theChangeForThisPattern != null)
			{
				// collect again AND apply delta to TREAT cache & indexes
				// might put pattern into affectedPatterns
				ReceiveDelta(theChangeForThisPattern);
			}
		}
		
		// recursive step! this topological order is done, get to next using remaining stuff
		
		// remove done patterns
		for (PQuery topOK : topologicalFirstPatterns)
		{
			currentAffectedPatterns.remove(topOK); // to filter which patterns are not applied yet (see next foreach)
			affectedPatterns.remove(topOK); // delta applied, do not reapply
		}
		// add remaining to affecteds
		for (PQuery remained : currentAffectedPatterns)
			affectedPatterns.add(remained);
		ProcessReceivedDeltaSet();
	}

	private Set<PQuery> getTopologicalOkayPatterns(Set<PQuery> patternSet)
	{
		Set<PQuery> ret = new HashSet<PQuery>();
		for (PQuery q : patternSet)
		{
			if (checkRecursively(q, patternSet))
				ret.add(q);
		}
		return ret;
	}

	private boolean checkRecursively(PQuery q, Set<PQuery> patternSet)
	{
		for(PQuery s : patternSet)
		{
			PatternCallModes callings = LookaheadMatcherTreat.PatternCallsPatterns.get(s);
			if (callings == null || callings.allSize() == 0)
				continue;
			else if (LookaheadMatcherTreat.PatternCallsPatterns.get(s).containsPattern(q))
				return false;
			return checkRecursively(q, LookaheadMatcherTreat.PatternCallsPatterns.get(s).getCallingPatternsSimply());
		}
		// if no return val were false: return true
		return true;
	}

	/**
	 * If we have only a delta, we can apply it to the affected pattern
	 * @param d The delta to apply into matches
	 */
	private Delta updatePatternsMatchingsFromDelta(IDelta delta)
	{
		// updates index and matching for a delta coming from

		if (delta instanceof ModelDelta)
		{
			// this is a model delta, multiples allowed!
			HashMap<LookaheadMatching, Boolean> changes = new HashMap<LookaheadMatching, Boolean>();
			ModelDelta d = (ModelDelta) delta;
			MultiSet<LookaheadMatching> oldMatches = LookaheadMatcherTreat.GodSet.get(d.getPattern());
			for (Entry<LookaheadMatching, Boolean> changedMatch : d.getChangeset().entries())
			{
				// remove or add (based on change type)
				if (changedMatch.getValue() == true)
				{
					if (!oldMatches.contains(changedMatch.getKey()))
						changes.put(changedMatch.getKey(), true); // first match, add!!
					
					oldMatches.add(changedMatch.getKey()); // new match appeared, increase by one (or add as first)
				}
				else
				{
					if (!oldMatches.contains(changedMatch.getKey()))
					{
						throw new AssertionError("This match is not even in this set!");
					}
					oldMatches.remove(changedMatch.getKey()); // model delta means that this is decremented
					if (!oldMatches.contains(changedMatch.getKey()))
						changes.put(changedMatch.getKey(), false); // last match, remove!!
				}
			}
			return new Delta(((ModelDelta) delta).getPattern(), changes); // new form needed (now a delta change is single and means fatal change)
		}
		if (delta instanceof Delta)
		{
			// this is a hard delta, increases or removes all
			Delta d = (Delta) delta;
			MultiSet<LookaheadMatching> oldMatches = LookaheadMatcherTreat.GodSet.get(d.getPattern());
			for (Entry<LookaheadMatching, Boolean> changedMatch : d.getChangeset().entrySet())
			{
				// remove or add (based on change type)
				if (changedMatch.getValue() == true)
				{
					if (oldMatches.contains(changedMatch.getKey()))
					{
						System.err.println("Already contained match, what is the new about it? Is it a bug? Or is it noone's fault?");
					}
					oldMatches.add(changedMatch.getKey()); // new match appeared
				}
				else
				{
					if (!oldMatches.contains(changedMatch.getKey()))
					{
						System.err.println("Not contained match, what to delete? Is it a bug? Or is it noone's fault?");
					}
					oldMatches.removeAll(changedMatch.getKey()); // delta means that this is absolutely KOd
				}
			}
			return (Delta) delta; // no new form needed
		}
		else
		{
			throw new AssertionError("This delta process does not support this delta type! Only model deltas and classic deltas allowed!");
		}
	}
	
	private Collection<IndexDelta> updateIndexesFromDelta(Delta delta)
	{
		// apply the delta on indexes (if needed)
		Collection<IndexDelta> indexDelta = ((TreatPartialPatternCacher) treatpartialCacher).ProcessADelta(delta);
		
		// indexDeltas can occur neg finded pattern calls!
		if (indexDelta != null && indexDelta.size() > 0)
			return indexDelta;
		return null;
	}

	/**
	 * Updates match set (TREAT) and index (in TREAT) targeting a query, that might have 1 or more deltas
	 * on its constraints (mailboxes). TREAT must guarantee, that all called patterns (so delta's patterns)
	 * are up to date and has new indexes, too. This method cannot be called if some of the called patterns
	 * are not updated yet (so topologically earlier patterns must be up to date and no deltas on its constraints).
	 * @param pattern This pattern has deltas in its bodies on its constraints
	 */
	private Delta processAllDeltaInMailboxesForPattern(PQuery pattern)
	{
		HashMap<LookaheadMatching, Boolean> changesToUpdatingPattern = new HashMap<LookaheadMatching, Boolean>();
		// updates pattern, which has deltas in his bodies (use mirrored views...)
		for (AheadStructure body : LookaheadMatcherTreat.GodSetStructures.get(pattern))
		{
			// get a delta and process
			for (AxisConstraint ac : body.SearchedConstraints)
			{
				// find call
				if (ac.hasMailboxContent() == false)
					continue;
				
				// satify call:
				AheadStructure clonedBody = body.clone();
				clonedBody.SearchedConstraints.remove(ac);
				clonedBody.FoundConstraints.add(ac);

				@SuppressWarnings({ "rawtypes", "unchecked" })
				List<IDelta> mailbox = (List<IDelta>) ((ArrayList) ac.getMailboxContent()).clone(); // "copy"
				for (IDelta d : mailbox)
				{
					ac.removeFromMailbox(d); // remove a delta
					Delta delta = (Delta)d;
					for (Entry<LookaheadMatching, Boolean> change : delta.getChangeset().entrySet())
					{
						// match with lookahead, search for changes based on delta change
						LookaheadMatcherInterface lmi = new LookaheadMatcherInterface();
						Object[] callingPatternMatches = change.getKey().getParameterMatchValuesOnlyAsArray();
						HashMap<PVariable, Object> callerMatches = new HashMap<PVariable, Object>();
						List<PVariable> affectedVars = ((FindConstraint)ac).getAffectedVariables();
						for (int i = 0; i < callingPatternMatches.length; i++)
						{
							callerMatches.put(affectedVars.get(i), callingPatternMatches[i]);
						}
						MultiSet<LookaheadMatching> changeResult = null;
						try {
							changeResult = lmi.searchChangesAll(engine, pattern, clonedBody.clone(), callerMatches, new TreatConstraintEnumerator(engine.getBaseIndex()));
						} catch (IncQueryException e) {
							e.printStackTrace();
						}
						if (changeResult == null)
							continue; // failed to get change res or base index etc.
						for(LookaheadMatching changed : changeResult.toArrayList(true))
						{
							boolean additionOrRemoval = change.getValue();
							if (changesToUpdatingPattern.containsKey(changed) && changesToUpdatingPattern.get(changed) != additionOrRemoval)
								changesToUpdatingPattern.remove(changed); // remove!!! because opposite was inside
							else
								changesToUpdatingPattern.put(changed, additionOrRemoval); // put the new
						}
					}
				}
			}
			for (CheckableConstraint cc : body.CheckConstraints)
			{
				// nac call
				if (cc.hasMailboxContent() == false)
					continue;

				AheadStructure clonedBody = body.clone();
				clonedBody.CheckConstraints.remove(cc); // already checked (he-he)
				@SuppressWarnings("unchecked")
				List<IDelta> mailbox = (List<IDelta>) (((ArrayList<IDelta>) cc.getMailboxContent()).clone()); // "copy"
				for (IDelta d : mailbox)
				{
					cc.removeFromMailbox(d); // remove a delta
					if (!(d instanceof IndexDelta))
						throw new AssertionError("This neg find got a not IndexDelta type delta! It cannot process other delta types!");
					IndexDelta delta = (IndexDelta)d;
					delta.getIndexVariables();
					for (Entry<LookaheadMatching, Boolean> change : delta.getChangeset().entrySet())
					{
						// match with lookahead, search for changes based on delta change
						LookaheadMatcherInterface lmi = new LookaheadMatcherInterface();
						Object[] callingPatternMatches = change.getKey().getParameterMatchValuesOnlyAsArray();
						HashMap<PVariable, Object> callerMatches = new HashMap<PVariable, Object>();
						List<PVariable> affectedVars = ((NACConstraint)cc).getAffectedVariables();
						for (int i = 0; i < callingPatternMatches.length; i++)
						{
							callerMatches.put(affectedVars.get(i), callingPatternMatches[i]);
						}
						MultiSet<LookaheadMatching> changeResult = null;
						try {
							changeResult = lmi.searchChangesAll(engine, pattern, clonedBody.clone(), callerMatches, new TreatConstraintEnumerator(engine.getBaseIndex()));
						} catch (IncQueryException e) {
							e.printStackTrace();
						}
						if (changeResult == null)
							continue; // failed to get change res or base index etc.
						for(LookaheadMatching changed : changeResult.toArrayList(true))
						{
							boolean additionOrRemoval = change.getValue() == false; // invert value!! (NAC!)
							if (changesToUpdatingPattern.containsKey(changed) && changesToUpdatingPattern.get(changed) != additionOrRemoval)
								changesToUpdatingPattern.remove(changed); // remove!!! because opposite was inside
							else
								changesToUpdatingPattern.put(changed, additionOrRemoval); // put the new
						}
					}
				}
			}
		}
		
		// create delta from itself!
		if (changesToUpdatingPattern.size() > 0)
		{
			// this pattern has changes in its match set also, propagate them!
			Delta newDelta = new Delta(pattern, changesToUpdatingPattern);
			return newDelta;
		}
		return null;
	}

	/**
	 * Inspects the delta, and delivers it to the patterns calling this delta's pattern. Also puts the delta into the
	 * appropriate constraints' mailboxes.
	 * @param d The delta to inspect (can be IndexDelta)
	 * @return Affected queries (must be processed!) with a boolean (or 2 so multimap): call find or call neg find?
	 */
	private Multimap<PQuery,Boolean> deliverDelta(IDelta delta)
	{
		Multimap<PQuery, Boolean> ret = HashMultimap.create();
		
		// who calls this delta's query?
		for (Entry<PQuery, PatternCallModes> patEntr : LookaheadMatcherTreat.PatternCallsPatterns.entrySet())
		{
			if (delta instanceof ModelDelta)
			{
				// this is a model change, not a classic one
				
			}
			if (delta instanceof IndexDelta)
			{
				// check if call:
				if (patEntr.getValue().getNegativeCalls().contains(((IndexDelta)delta).getPattern()))
				{
					// the patEntr.getKey is who calls the delta's pattern!!
					PQuery caller = patEntr.getKey();
					// put to mailboxes
					for (AheadStructure struct : LookaheadMatcherTreat.GodSetStructures.get(caller))
					{
						// indexdelta: only to NAC
						for (CheckableConstraint cc : struct.CheckConstraints)
						{
							if (cc instanceof NACConstraint)
							{
								NACConstraint nacC = (NACConstraint)cc;
								if (Utils.isProperIndex(nacC, (IndexDelta)delta))
								{
									nacC.putToMailbox(delta);
									ret.put(caller, false);
								}
							}
						}
					}
				}
			}
			else if (delta instanceof Delta && !(delta instanceof IndexDelta))
			{
				// check if call:
				if (patEntr.getValue().getPositiveCalls().contains(((Delta)delta).getPattern()))
				{
					// the patEntr.getKey is who calls the delta's pattern!!
					PQuery caller = patEntr.getKey();
					// put to mailboxes
					for (AheadStructure struct : LookaheadMatcherTreat.GodSetStructures.get(caller))
					{
						// put simple delta only in find calls
						for (AxisConstraint ac : struct.SearchedConstraints)
						{
							if (ac instanceof FindConstraint)
							{
								((FindConstraint)ac).putToMailbox(delta);
								ret.put(caller, true);
							}
						}
					}
				}
			}
		}
		// return pattern-callmode map (callmode: true if FIND false if NEGFIND)
		return ret;
//
//		// caller is who calls the delta's pattern, but caller calls other patterns also, so if caller changes,
//		// these patterns will be affected also (one depth is enough, because this is only one receiveDelta,
//		// when processing all affected patterns, other receiveDeltas will occur on the next, appropriate levels)
		// I think this is not needed!!!!!:
//		for (Entry<PQuery, PatternCallModes> patEntr : LookaheadMatcherTreat.PatternCallsPatterns.entrySet())
//		{
//			// add to affecteds
//			for (PQuery also : ret.keySet())
//			{
//				if (patEntr.getValue().getCallingPatternsSimply().contains(also) && // if someone calls this
//						delta.getPattern().equals(also) == false) // but not the delta itself (might make no sense? (circle?))
//				{
//					ret.put(patEntr.getKey(), patEntr.getValue().getPositiveCalls().contains(also)); // true if positive call
//				}
//			}
//		}
	}
}
