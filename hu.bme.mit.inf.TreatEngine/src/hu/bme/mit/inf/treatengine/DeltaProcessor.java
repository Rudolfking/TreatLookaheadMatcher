package hu.bme.mit.inf.treatengine;

import hu.bme.mit.inf.lookaheadmatcher.LookaheadMatcherInterface;
import hu.bme.mit.inf.lookaheadmatcher.impl.AheadStructure;
import hu.bme.mit.inf.lookaheadmatcher.impl.AxisConstraint;
import hu.bme.mit.inf.lookaheadmatcher.impl.FindConstraint;
import hu.bme.mit.inf.lookaheadmatcher.impl.LookaheadMatching;
import hu.bme.mit.inf.lookaheadmatcher.impl.MultiSet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.incquery.runtime.api.IQuerySpecification;
import org.eclipse.incquery.runtime.api.IncQueryEngine;
import org.eclipse.incquery.runtime.matchers.psystem.PQuery;
import org.eclipse.incquery.runtime.matchers.psystem.PVariable;
import org.eclipse.incquery.patternlanguage.patternLanguage.Pattern;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

public class DeltaProcessor
{
	/*
	public static HashMap<Pattern, MultiSet<LookaheadMatching>> GodSet = new HashMap<Pattern, MultiSet<LookaheadMatching>>();
	public static HashMap<Pattern, ArrayList<AheadStructure>> GodSetStructures = new HashMap<Pattern, ArrayList<AheadStructure>>();
	public static HashMap<Pattern, HashSet<Pattern>> PatternCallsPatterns = new HashMap<Pattern, HashSet<Pattern>>();
	public static HashMap<ENamedElement, HashSet<Pattern>> RelativeSet = new HashMap<ENamedElement, HashSet<Pattern>>();
	 */
	
	// a ZH olyan, mint a palinkafozes: 50% alatt sikertelen
	public boolean ProcessDelta(Delta delta)
	{
		// apply changes
		// get old matches and apply the changeset (delta changes)
		MultiSet<LookaheadMatching> oldMatches = LookaheadMatcherTreat.GodSet.get(delta.getPattern());
		// multimap entries: more value to one key: iterates distinctly
		for (Entry<LookaheadMatching, Boolean> changedMatch : delta.getChangeset().entries())
		{
			// remove or add (based on change type)
			if (changedMatch.getValue() == true)
				oldMatches.add(changedMatch.getKey());
			else oldMatches.remove(changedMatch.getKey());
		}
		
		// propagate changes
		HashMap<PQuery, Boolean> affectedPatterns = new HashMap<PQuery, Boolean>();
		for (Entry<PQuery, HashMap<PQuery, Boolean>> patEntr : LookaheadMatcherTreat.PatternCallsPatterns.entrySet())
		{
			if (patEntr.getValue().containsKey(delta.getPattern()))
			{
				affectedPatterns.put(patEntr.getKey(), patEntr.getValue().get(delta.getPattern()));
			}
		}
		//HashMap<Pattern, Boolean> affectedPatterns = LookaheadMatcherTreat.PatternCallsPatterns.get(delta.getPattern());
		if (affectedPatterns.size() == 0)
			return true; // no pattern to update
			
		// the freshly change-matches entryset:     ( matching <-> isAdded )
		Multimap<LookaheadMatching, Boolean> changes = delta.getChangeset(); // hashmultimap
		
		// for each pattern update matchings (propagate delta)
		// we MUST update matches for this pattern
		for (Entry<PQuery, Boolean> patternToUpdateEntry : affectedPatterns.entrySet())
		{
			// this pattern should be updated
			PQuery patternToUpdate = patternToUpdateEntry.getKey();
			
			// the old matches to update
			// MultiSet<LookaheadMatching> updatingMatches = TreatRegistrarImpl.LookaheadToEngineConnector.GetLookaheadMatcherTreat(this.engine).matchThePattern(patternToUpdate);
			// LookaheadMatcherTreat.Match!!//GodSet.get(patternToUpdate);
			
			
			// find esetén megbindolom a friss matchinget (paramétereket) és lefuttatok egy lookaheadet
			// majd booleannek megfelelõen létrehozok egy deltát, ami deleteeli vagy inserteli a halmazba a talált matcheket (frissítés)
			// a delta propagálása meg már lehet vegyes (+- matchek)
			
			Multimap<LookaheadMatching, Boolean> changesToUpdatingPattern = HashMultimap.create();
			
			// if change is FINDdelta (not negFind)
			if (patternToUpdateEntry.getValue() == true)
			{
				
				//patternToUpdate BODYYY
				ArrayList<AheadStructure> findedPatternsStructures = LookaheadMatcherTreat.GodSetStructures.get(patternToUpdate);
				for (AheadStructure struct : findedPatternsStructures)
				{
					
					// check whether find is in this structure
					List<PVariable> affectedFindVars = new ArrayList<PVariable>();
					boolean okay = false;
					for (AxisConstraint constr : struct.SearchedConstraints)
					{
						if ((constr instanceof FindConstraint))
						{
							if (((FindConstraint) constr).getInnerFindCall().getReferredQuery().equals(delta.getPattern()))
							{
								// not in here
								okay = true;
								affectedFindVars = ((FindConstraint) constr).getAffectedVariables();
								break;
							}
						}
					}
					if (!okay)
						continue;
					
					// matcher
					
					
					// foreach all new (changes) match
					for (Entry<LookaheadMatching, Boolean> changingMatchEntry : changes.entries())
					{
						HashMap<PVariable, Object> knownLocalAndParameters = new HashMap<PVariable, Object>();
						LookaheadMatching changingMatch = changingMatchEntry.getKey();
						
						// foreach new match's vars
						/*for (Entry<LookVariable, Object> var : changingMatch.getParameterMatchesOnly().entrySet())
						{
							// all variable can be found because find is in this one
							knownLocalAndParameters.put(var.getKey(), var.getValue());
						}*/
						
						//for (PVariable affectedVar : affectedFindVars)
						for (int i = 0; i<affectedFindVars.size(); i++)
						{
							// affected[i] -> match.paramVars.get(affected[i])
							knownLocalAndParameters.put(affectedFindVars.get(i), changingMatch.getParameterMatchesOnly().get(changingMatch.getParameterVariables()[i]));
						}
						
						// ready to match
						LookaheadMatcherInterface matcher = new LookaheadMatcherInterface();
						MultiSet<LookaheadMatching> changesE = matcher.searchChangesAll(engine, patternToUpdate, struct.clone(), knownLocalAndParameters);
						
						for (Entry<LookaheadMatching, Integer> newMatchAndType : changesE.getInnerMap().entrySet())
						{
							for (int i = 0; i < newMatchAndType.getValue(); i++)
							{
								changesToUpdatingPattern.put(newMatchAndType.getKey(), changingMatchEntry.getValue());
							}
						}
					}
				}
			}
			else
			{
				// neg find!
				// neg find esetén bonyolultabb a képlet, de még nem tudom, mi
			}
			
			if (changesToUpdatingPattern.size() > 0)
			{
				Delta newDelta = new Delta(patternToUpdate, changesToUpdatingPattern);
				DeltaProcessor.getInstance().ProcessDelta(newDelta);
			}
		}
		return false;
	}
	private DeltaProcessor()
	{
		// empty...
	}
	
	private IncQueryEngine engine;
	
	private static DeltaProcessor instance = null;
	public static DeltaProcessor getInstance()
	{
		if (instance == null)
			instance = new DeltaProcessor();
		return instance;
	}
	
	public void setEngine(IncQueryEngine engineRe)
	{
		this.engine = engineRe;
	}
}
