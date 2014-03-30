//package hu.bme.mit.inf.treatengine;
//
//import hu.bme.mit.inf.lookaheadmatcher.IPartialPatternCacher;
//import hu.bme.mit.inf.lookaheadmatcher.LookaheadMatcherInterface;
//import hu.bme.mit.inf.lookaheadmatcher.impl.AheadStructure;
//import hu.bme.mit.inf.lookaheadmatcher.impl.AxisConstraint;
//import hu.bme.mit.inf.lookaheadmatcher.impl.CheckableConstraint;
//import hu.bme.mit.inf.lookaheadmatcher.impl.FindConstraint;
//import hu.bme.mit.inf.lookaheadmatcher.impl.IConstraint;
//import hu.bme.mit.inf.lookaheadmatcher.impl.LookaheadMatching;
//import hu.bme.mit.inf.lookaheadmatcher.impl.MultiSet;
//import hu.bme.mit.inf.lookaheadmatcher.impl.NACConstraint;
//
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.HashMap;
//import java.util.HashSet;
//import java.util.List;
//import java.util.Map.Entry;
//import java.util.Set;
//
//import org.eclipse.core.runtime.AssertionFailedException;
//import org.eclipse.incquery.runtime.api.IQuerySpecification;
//import org.eclipse.incquery.runtime.api.IncQueryEngine;
//import org.eclipse.incquery.runtime.matchers.psystem.PQuery;
//import org.eclipse.incquery.runtime.matchers.psystem.PVariable;
//import org.eclipse.incquery.patternlanguage.patternLanguage.Pattern;
//
//import com.google.common.collect.HashMultimap;
//import com.google.common.collect.Multimap;
//
//public class DeltaProcessor
//{
//	/*
//	public static HashMap<Pattern, MultiSet<LookaheadMatching>> GodSet = new HashMap<Pattern, MultiSet<LookaheadMatching>>();
//	public static HashMap<Pattern, ArrayList<AheadStructure>> GodSetStructures = new HashMap<Pattern, ArrayList<AheadStructure>>();
//	public static HashMap<Pattern, HashSet<Pattern>> PatternCallsPatterns = new HashMap<Pattern, HashSet<Pattern>>();
//	public static HashMap<ENamedElement, HashSet<Pattern>> RelativeSet = new HashMap<ENamedElement, HashSet<Pattern>>();
//	 */
//	
//	// a ZH olyan, mint a palinkafozes: 50% alatt sikertelen
//	/**
//	 * Processes a delta change on a pattern and recursively down.
//	 * @param delta The delta to apply on matches
//	 * @return Returns true, if delta does not affect matches, false otherwise
//	 */
//	public boolean ProcessDelta(Delta delta)
//	{
//		// get old matches and apply the changeset (delta changes)
//		MultiSet<LookaheadMatching> oldMatches = LookaheadMatcherTreat.GodSet.get(delta.getPattern());
//		
//		// multimap entries: more value to one key: iterates distinctly
//		// also apply changes on indexes
//		for (Entry<LookaheadMatching, Boolean> changedMatch : delta.getChangeset().entries())
//		{
//			// remove or add (based on change type)
//			if (changedMatch.getValue() == true)
//				oldMatches.add(changedMatch.getKey()); // new match appeare
//			else oldMatches.removeAll(changedMatch.getKey()); // old match disappeared
//		}
//		// process the delta on indexes (if needed)
//		((TreatPartialPatternCacher) treatpartialCacher).ProcessADelta(delta);
//		
//		
//		// propagate changes
//		HashMap<PQuery, Boolean> affectedPatterns = new HashMap<PQuery, Boolean>();
//		for (Entry<PQuery, Multimap<PQuery, Boolean>> patEntr : LookaheadMatcherTreat.PatternCallsPatterns.entrySet())
//		{
//			if (patEntr.getValue().containsKey(delta.getPattern()))
//			{
//				for (Boolean isPos : patEntr.getValue().get(delta.getPattern()))
//				{
//					affectedPatterns.put(patEntr.getKey(), isPos);
//				}
//			}
//		}
//
//		if (affectedPatterns.size() == 0)
//			return true; // no pattern to update
//			
//		// the freshly change-matches entryset:     ( matching <-> isAdded )
//		// Multimap<LookaheadMatching, Boolean> changes = delta.getChangeset(); // hashmultimap
//		
//		// for each pattern update matchings (propagate delta)
//		// we MUST update matches for this pattern
//		for (Entry<PQuery, Boolean> patternToUpdateEntry : affectedPatterns.entrySet())
//		{
//			// this pattern should be updated
//			PQuery patternToUpdate = patternToUpdateEntry.getKey();
//			
//			// the old matches to update
//			// MultiSet<LookaheadMatching> updatingMatches = TreatRegistrarImpl.LookaheadToEngineConnector.GetLookaheadMatcherTreat(this.engine).matchThePattern(patternToUpdate);
//			// LookaheadMatcherTreat.Match!!//GodSet.get(patternToUpdate);
//			
//			
//			// find esetén megbindolom a friss matchinget (paramétereket) és lefuttatok egy lookaheadet
//			// majd booleannek megfelelõen létrehozok egy deltát, ami deleteeli vagy inserteli a halmazba a talált matcheket (frissítés)
//			// a delta propagálása meg már lehet vegyes (+- matchek)
//			Multimap<LookaheadMatching, Boolean> changesToUpdatingPattern = HashMultimap.create();
//			
//			//patternToUpdate BODY:
//			ArrayList<AheadStructure> findedPatternsStructures = LookaheadMatcherTreat.GodSetStructures.get(patternToUpdate);
//			for (AheadStructure struct_donottouch : findedPatternsStructures)
//			{
//				AheadStructure modifiableStruct = struct_donottouch.clone();
//				// check whether the specific find is in this structure (in this body)
//				// it means that this pattern calls the delta's pattern, but which body?
//				List<PVariable> affectedFindVars = new ArrayList<PVariable>();
//				boolean okay = false;
//				if (patternToUpdateEntry.getValue() == true)
//				{
//					AxisConstraint toRemove = null;
//					for (AxisConstraint constr : modifiableStruct.SearchedConstraints)
//					{
//						if ((constr instanceof FindConstraint))
//						{
//							if (((FindConstraint) constr).getInnerFindCall().getReferredQuery().equals(delta.getPattern()))
//							{
//								// here
//								okay = true;
//								affectedFindVars = ((FindConstraint) constr).getAffectedVariables();
//								toRemove = constr;
//								break;
//							}
//						}
//					}
//					if (okay)
//					{
//						// satisfied
//						modifiableStruct.SearchedConstraints.remove(toRemove);
//						modifiableStruct.FoundConstraints.add(toRemove);
//					}
//				}
//				else
//				{
//					CheckableConstraint toRemove = null;
//					for (CheckableConstraint constr : modifiableStruct.CheckConstraints)
//					{
//						if ((constr instanceof NACConstraint))
//						{
//							if (((NACConstraint) constr).getInnerNegativeCallConstraint().getReferredQuery().equals(delta.getPattern()))
//							{
//								// here
//								okay = true;
//								affectedFindVars = ((NACConstraint) constr).getAffectedVariables();
//								toRemove = constr;
//								break;
//							}
//						}
//					}
//					if (okay)
//					{
//						// checked
//						modifiableStruct.CheckConstraints.remove(toRemove);
//					}
//				}
//				// not in here
//				if (!okay)
//					continue;
//				
//				// matcher
//				
//				
//				// foreach all new (changes) match in DELTA
//				for (Entry<LookaheadMatching, Boolean> changingMatchEntry : delta.getChangeset().entries())
//				{
//					HashMap<PVariable, Object> knownLocalAndParameters = new HashMap<PVariable, Object>();
//					LookaheadMatching changingMatch = changingMatchEntry.getKey();
//					//for (PVariable affectedVar : affectedFindVars)
//					for (int i = 0; i<affectedFindVars.size(); i++)
//					{
//						// affected[i] -> match.paramVars.get(affected[i])
//						knownLocalAndParameters.put(affectedFindVars.get(i), changingMatch.getParameterMatchesOnly().get(changingMatch.getParameterVariables()[i]));
//					}
//					
//					// ready to match, use indexer to get "old" matchings for the updating pattern (might trigger Lookahead?)
//					MultiSet<LookaheadMatching> changesOld = treatpartialCacher.GetMatchingsFromPartial(patternToUpdate, knownLocalAndParameters, affectedFindVars, true);
//
//					
//					if (patternToUpdateEntry.getValue() == true)
//					{
//						// new matches, must use lookahead, because the index for this pattern is OLD
//						// no need to update indexes now, because from the canges new delta will be created and propagated (recursively)
//						LookaheadMatcherInterface matcher = new LookaheadMatcherInterface();
//						MultiSet<LookaheadMatching> changesNew = matcher.searchChangesAll(engine, patternToUpdate, modifiableStruct.clone(), knownLocalAndParameters, null);
//						
//						// after we have the new matches for this type, make sure that they are added
//						for (Entry<LookaheadMatching, Integer> newMatchAndType : changesNew.getInnerMap().entrySet())
//						{
//							// put the match repeatedly as much it matches
//							for (int i = 0; i < newMatchAndType.getValue(); i++)
//							{
//								changesToUpdatingPattern.put(newMatchAndType.getKey(), changingMatchEntry.getValue());
//							}
//						}
//					}
//					else if (patternToUpdateEntry.getValue() == false)
//					{
//						// neg find! P has 
//						
//						if (changesOld.size() > 0 && changingMatchEntry.getValue() == true) //changesNew.size() > 0)
//						{
//							// if first match for this
//							// check if not losing any matching, because we used this one
//							// we had matchings :(
//							for (LookaheadMatching lmi : changesOld.toArrayList(true))
//								changesToUpdatingPattern.put(lmi, false); // removal
//						}
//						else if (changesOld.size() > 0 && changingMatchEntry.getValue() == false)
//						{
//							// cannot happen!
//							throw new AssertionFailedException("Cannot happen! Matches for a neg find that has just decreased? No way!");
//						}
//						// else if (changesOld.size == 0 and changingMatchEntry.value true ==>> we don't care, no changes should happen!
//						else if (changesOld.size() == 0 && changingMatchEntry.getValue() == false) //&& changesNew.size() == 0)
//						{
//							// all matches lost in called pattern? because then there might be new matches in callee
//							MultiSet<LookaheadMatching> calledPatternsMatches = treatpartialCacher.GetMatchingsFromPartial
//									(delta.getPattern(), changingMatch.getParameterMatchesOnly(), Arrays.asList(changingMatch.getParameterVariables()), true);
//							
//							if (calledPatternsMatches.size() > 0)
//							{
//								// more matches it has, so nothing to do
//							}
//							else
//							{
//								// there might be new changes! search? search!
//								LookaheadMatcherInterface matcher = new LookaheadMatcherInterface();
//								MultiSet<LookaheadMatching> changesNew = matcher.searchChangesAll(engine, patternToUpdate, modifiableStruct.clone(), knownLocalAndParameters, null);
//								// after we have the new matches for this type, make sure that they are added
//								for (Entry<LookaheadMatching, Integer> newMatchAndType : changesNew.getInnerMap().entrySet())
//								{
//									// put the match repeatedly as much it matches
//									for (int i = 0; i < newMatchAndType.getValue(); i++)
//									{
//										changesToUpdatingPattern.put(newMatchAndType.getKey(), true);
//									}
//								}
//							}
//						}
//					}
//				}
//			}
//			
//			if (changesToUpdatingPattern.size() > 0)
//			{
//				// this pattern has changes in its match set also, propagate them!
//				Delta newDelta = new Delta(patternToUpdate, changesToUpdatingPattern);
//				ProcessDelta(newDelta);
//			}
//		}
//		return false;
//	}
//	private DeltaProcessor()
//	{
//		// empty...
//	}
//	
//	private IncQueryEngine engine;
//	private IPartialPatternCacher treatpartialCacher;
//	
//	private static DeltaProcessor instance = null;
//	public static DeltaProcessor getInstance()
//	{
//		if (instance == null)
//			instance = new DeltaProcessor();
//		return instance;
//	}
//	
//	public void setEngine(IncQueryEngine engineRe)
//	{
//		this.engine = engineRe;
//	}
//	public void setPartialCacher(IPartialPatternCacher treatPartial)
//	{
//		this.treatpartialCacher = treatPartial;
//	}
//}
