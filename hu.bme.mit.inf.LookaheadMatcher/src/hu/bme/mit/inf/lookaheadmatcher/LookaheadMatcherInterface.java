package hu.bme.mit.inf.lookaheadmatcher;

import hu.bme.mit.inf.lookaheadmatcher.impl.AheadStructure;
import hu.bme.mit.inf.lookaheadmatcher.impl.AxisConstraint;
import hu.bme.mit.inf.lookaheadmatcher.impl.LookaheadMatching;
import hu.bme.mit.inf.lookaheadmatcher.impl.MatcherAlgorithm;
import hu.bme.mit.inf.lookaheadmatcher.impl.MultiSet;
import hu.bme.mit.inf.lookaheadmatcher.impl.PatternProcessor;
import hu.bme.mit.inf.lookaheadmatcher.impl.RelationConstraint;
import hu.bme.mit.inf.lookaheadmatcher.impl.TypeConstraint;
import hu.bme.mit.inf.lookaheadmatcher.impl.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.incquery.runtime.api.IncQueryEngine;
import org.eclipse.incquery.runtime.base.api.NavigationHelper;
import org.eclipse.incquery.runtime.exception.IncQueryException;
import org.eclipse.incquery.runtime.matchers.psystem.PBody;
import org.eclipse.incquery.runtime.matchers.psystem.PConstraint;
import org.eclipse.incquery.runtime.matchers.psystem.PQuery;
import org.eclipse.incquery.runtime.matchers.psystem.PVariable;
import org.eclipse.incquery.runtime.matchers.psystem.basicdeferred.NegativePatternCall;
import org.eclipse.incquery.runtime.matchers.psystem.basicdeferred.PatternCallBasedDeferred;
import org.eclipse.incquery.runtime.matchers.psystem.basicenumerables.PositivePatternCall;
import org.eclipse.incquery.runtime.matchers.tuple.Tuple;

//import org.eclipse.viatra2.emf.incquery.base.exception.IncQueryBaseException;

public class LookaheadMatcherInterface
{
	public LookaheadMatcherInterface()
	{
		super();
	}
	
	private NavigationHelper navigationHelper;
	
	private ArrayList<TypeConstraint> typeConstraints = new ArrayList<TypeConstraint>();
	private ArrayList<RelationConstraint> relationConstraints = new ArrayList<RelationConstraint>();
	private ArrayList<AheadStructure> aheadStructures = new ArrayList<AheadStructure>();
	
	public ArrayList<TypeConstraint> GetTypeConstraints()
	{
		return this.typeConstraints;
	}
	public ArrayList<RelationConstraint> GetRelationConstraints()
	{
		return this.relationConstraints;
	}
	public ArrayList<AheadStructure> GetAheadStructures()
	{
		return this.aheadStructures;
	}
	
	
	/*
	 * Public methods:
	 */	
	
	/**
	 * Tries to match the pattern with the specified engine and known values.
	 * Returns true in at least one matching is found (the matches are not searched and cached!)
	 */
	public boolean tryMatch(IncQueryEngine engine, IPartialPatternCacher patternCacher, PQuery pattern, ArrayList<Object> knownValues, IConstraintEnumerator consEnum)
	{
		try
		{
			this.navigationHelper = engine.getBaseIndex();
		}
		catch (IncQueryException e)
		{
			e.printStackTrace();
			return false;
		}
		PatternProcessor pProc = new PatternProcessor(engine, patternCacher);
		ArrayList<AheadStructure> matchingStates = pProc.Process(pattern);
		
		MultiSet<LookaheadMatching> matches = new MultiSet<LookaheadMatching>();
		
		for (AheadStructure state : matchingStates)
		{
			AheadStructure clonedState = state.clone();// 1st step
			matches.addAll(new MatcherAlgorithm().getPatternMatches(clonedState, true, navigationHelper, knownValues, consEnum));
		}
		
		if (matches.size() > 0)
			return true; // matched on one of the branches
		return false; // no matches
	}
	
	/**
	 *  This is the regular matching method: processes a pattern, then searches all matches, returns as a multiset
	 *  (more matchings of the same parameter values but different local values are count as two
	 */
	@SuppressWarnings("unchecked")
	public MultiSet<LookaheadMatching> matchAll(IncQueryEngine engine, IPartialPatternCacher patternCacher,
			PQuery patternQuery, ArrayList<Object> knownValues, IConstraintEnumerator consEnum)
	{
		// get navhelper
		try
		{
			this.navigationHelper = engine.getBaseIndex();
		}
		catch (IncQueryException e)
		{
			e.printStackTrace();
			return null;
		}
		
		// process pattern
		ArrayList<AheadStructure> matchingStates = processPattern(patternQuery, engine, patternCacher);
		this.aheadStructures = (ArrayList<AheadStructure>) matchingStates.clone(); // clone to cache
		
		
		// return with matches (knownvalues when matching, known_even_locals is that we don't have in this case)
		MultiSet<LookaheadMatching> rett = matchWithProcessed(matchingStates, knownValues, null, consEnum);
		
		return rett;
	}
	
	/**
	 * Searches all changes in matchings when model changes. It requires the pattern, the engine, the processed pattern body (cachedStructure), and the already known values
	 */
	public MultiSet<LookaheadMatching> searchChangesAll(IncQueryEngine engine, PQuery modPattern, AheadStructure cachedStructure,
			HashMap<PVariable, Object> knownValues, IConstraintEnumerator consEnum)
	{
		ArrayList<AheadStructure> structOne = new ArrayList<AheadStructure>();
		structOne.add(cachedStructure);
		return this.searchChangesAll(engine, modPattern, structOne, knownValues,consEnum);
	}
	
	/**
	 * Searches all changes in matchings when model changes. It requires the pattern, the engine, the processed pattern bodies (cachedStructure), and the already known values (for local and parameter vars)
	 */
	public MultiSet<LookaheadMatching> searchChangesAll(IncQueryEngine engine, PQuery modPattern, ArrayList<AheadStructure> cachedStructures, HashMap<PVariable, Object> knownValues, IConstraintEnumerator consEnum)
	{
		try
		{
			this.navigationHelper = engine.getBaseIndex();
		}
		catch (IncQueryException e)
		{
			e.printStackTrace();
			return null;
		}
		
		MultiSet<LookaheadMatching> matches = new MultiSet<LookaheadMatching>();
		
		// for all cached structures
		//for(AheadStructure state : cachedStructures)
		//{
		//	matches.addAll(new MatcherAlgorithm().getPatternMatches(state, false, navigationHelper,null));
		//}
		
		// match all with no known values but known mapped values to bind (can be local etc.)
		matches.addAll(matchWithProcessed(cachedStructures, null, knownValues, consEnum));
		
		// System.out.println("[Update] Find new matches for pattern " + modPattern.getFullyQualifiedName() + " (all bodies): " + matches.size());
		
		return matches;
	}
	
	/**
	 *  returns all findX negfindX patterns, that occur in P pattern (query) (not recursively!)
	 */
	public PatternCallModes getFindListForPattern(PQuery P)
	{
		PatternCallModes mode = new PatternCallModes(P);
		for (PBody body : P.getContainedBodies())
		{
			for (PConstraint calls : body.getConstraints())
			{
				if (calls instanceof NegativePatternCall)// PatternCompositionConstraint)
				{
					PatternCallBasedDeferred pdd = (PatternCallBasedDeferred)calls;
					
					Set<String> filterKey = new HashSet<String>();
					Tuple actuals = pdd.getActualParametersTuple();
					boolean isNotFull = false;
					//for (Object a: actuals.getElements())
					List<String> patternPars = pdd.getReferredQuery().getParameterNames();
					for (int i=0;i<actuals.getSize();i++)
					{
						PVariable callA = (PVariable) actuals.get(i);
						if (callA instanceof PVariable)
						{
							if ((callA).isVirtual() == false && Utils.isRunning(callA) == false)
								filterKey.add(patternPars.get(i));
							else
								isNotFull = true; // virtual (notfull) var
						}
					}
					// put as negative call, but check if notok
					mode.AddNegativeCall(pdd.getReferredQuery(), filterKey, isNotFull);
				}
				else if (calls instanceof PositivePatternCall)
				{
					PositivePatternCall pdd = (PositivePatternCall)calls;
					mode.AddPositiveCall(pdd.getReferredQuery());
				}
			}
		}
		
		return mode;
	}
	
	
	/*
	 * Private methods:
	 */

	// Processes a pattern, creating aheadstructures (aheadstructure: "subPlan")
	private ArrayList<AheadStructure> processPattern(PQuery chosenSpecification, IncQueryEngine engine, IPartialPatternCacher patternCacher)
	{
		return (new PatternProcessor(engine, patternCacher)).Process(chosenSpecification);
	}
	
	/**
	 * process pattern ready, pass to match
	 * two options are available: knownParameterValues (in order of pattern definition variables) are known values of the parameter variables
	 * the other option is knownLocalAndParameterValues (can be any variable present in this pattern) to bind those vars
	 */
	private MultiSet<LookaheadMatching> matchWithProcessed(ArrayList<AheadStructure> preparedStructures, ArrayList<Object> knownParameterValues, HashMap<PVariable, Object> knownLocalAndParameterValues, IConstraintEnumerator consEnum)
	{
		// create matcher algo
		MatcherAlgorithm readyToMatchAlgo = new MatcherAlgorithm();
		
		//matches to return
		MultiSet<LookaheadMatching> matches = new MultiSet<LookaheadMatching>();
		
		// iterate all structure and match them! (collect matches)
		for (AheadStructure stateNoClone : preparedStructures)
		{
			AheadStructure state = stateNoClone.clone(); // 1st step!
			// cache constraints (empty then add)
			this.typeConstraints.clear();
			this.relationConstraints.clear();
			for (AxisConstraint conss : state.SearchedConstraints)
			{
				if (conss instanceof TypeConstraint)
					this.typeConstraints.add((TypeConstraint) conss);
				else if (conss instanceof RelationConstraint)
					this.relationConstraints.add((RelationConstraint) conss);
			}
			
			// insert locals into the game
			if (knownLocalAndParameterValues != null)
			{
				for (Entry<PVariable, Object> knownOne : knownLocalAndParameterValues.entrySet())
				{
					if (state.MatchingVariables.containsKey(knownOne.getKey()) == false)
					{
						System.err.println("Fucking big error, baaad");
					}
					readyToMatchAlgo.bindToVariable(knownOne.getKey(), knownOne.getValue(), state.MatchingVariables);
				}
			}
			// known any kind of var's values are bound, but
			// known values are bound in getPatternMatches(,,,)!!!!!!! - if not null
			matches.addAll(readyToMatchAlgo.getPatternMatches(state, false, navigationHelper, knownParameterValues, consEnum));
		}
		
		// write out!
		int laza = 0;
//		for (LookaheadMatching oneMatch : matches.toArrayListDeprecated())
//		{
//			System.out.println("Match " + Integer.toString(laza) + ":" + oneMatch.toString());
//			laza++;
//		}
		// System.out.println("Total matches from all body: " + matches.size() + " and unique: " + matches.uniqueSize());
		
		return matches;
	}
}
