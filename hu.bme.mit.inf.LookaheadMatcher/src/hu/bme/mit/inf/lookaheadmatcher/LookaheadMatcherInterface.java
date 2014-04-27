package hu.bme.mit.inf.lookaheadmatcher;

import hu.bme.mit.inf.lookaheadmatcher.impl.AheadStructure;
import hu.bme.mit.inf.lookaheadmatcher.impl.AxisConstraint;
import hu.bme.mit.inf.lookaheadmatcher.impl.LookaheadMatching;
import hu.bme.mit.inf.lookaheadmatcher.impl.MatcherAlgorithm;
import hu.bme.mit.inf.lookaheadmatcher.impl.PatternProcessor;
import hu.bme.mit.inf.lookaheadmatcher.impl.RelationConstraint;
import hu.bme.mit.inf.lookaheadmatcher.impl.SimplePatternCacher;
import hu.bme.mit.inf.lookaheadmatcher.impl.TypeConstraint;
import hu.bme.mit.inf.lookaheadmatcher.impl.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.ENamedElement;
import org.eclipse.emf.ecore.EStructuralFeature;
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

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;

//import org.eclipse.viatra2.emf.incquery.base.exception.IncQueryBaseException;

public class LookaheadMatcherInterface
{
	/**
	 * Creates a LookaheadMatcher and stores a navigation helper to guess the counts for the constraints.
	 * @param navHelper
	 */
	public LookaheadMatcherInterface(NavigationHelper navHelper)
	{
		super();
		this.navigationHelper = navHelper;
	}
	
	private NavigationHelper navigationHelper = null;
	
	private List<PQuery> initializeds = new ArrayList<PQuery>();
		
	/**
	 *
	 * Tries to match the pattern with the specified engine and known values.
	 * Returns true in at least one matching is found (the matches are not searched and cached!)
	 * @param engine IncQueryEngine
	 * @param patternCacher The pattern cacher to use for find/nac caching and cost guessing. If set to null, default (slow) is used
	 * @param pattern The query to match
	 * @param knownValues The known values in order of the query parameters (if not known, "null")
	 * @param consEnum The constraint enumerator to use for cost guessing and iterating. If set to null, default is used!
	 * @return True if there is a matching for the query, false if nothing found
	 */
	public boolean tryMatch(IncQueryEngine engine, IPartialPatternCacher patternCacher, PQuery pattern, ArrayList<Object> knownValues, IConstraintEnumerator consEnum)
	{
		if (patternCacher == null)
		{
			patternCacher = new SimplePatternCacher(engine, this.navigationHelper);
		}
		//System.out.println("Match!");
		initialize(pattern, engine);
		
		ArrayList<AheadStructure> matchingStates =  processPattern(pattern, engine, patternCacher);
		
		Multiset<LookaheadMatching> rett = matchWithProcessed(matchingStates, knownValues, null, consEnum);
		
		if (rett.size() > 0)
			return true; // matched on one of the branches
		return false; // no matches
	}
	
	/**
	 *  This is the regular matching method: processes a pattern, then searches all matches, returns as a multiset
	 *  (more matchings of the same parameter values but different local values are count as two)
	 * @param engine IncQueryEngine
	 * @param patternCacher The pattern cacher to use for find/nac caching and cost guessing. If set to null, default (slow) is used
	 * @param patternQuery The query to match
	 * @param knownValues The known values in order of the query parameters (if not known, "null")
	 * @param consEnum The constraint enumerator to use for cost guessing and iterating. If set to null, default is used!
	 * @return The matches found (if one match can be found more times only differing in local variables, it counts as two)
	 */
	public Multiset<LookaheadMatching> matchAll(IncQueryEngine engine, IPartialPatternCacher patternCacher,
			PQuery patternQuery, ArrayList<Object> knownValues, IConstraintEnumerator consEnum)
	{
		if (patternCacher == null)
		{
			patternCacher = new SimplePatternCacher(engine, this.navigationHelper);
		}
		//System.out.println("Match!");
		initialize(patternQuery, engine);
		// process pattern
		ArrayList<AheadStructure> matchingStates = processPattern(patternQuery, engine, patternCacher);
		
		
		// return with matches (knownvalues when matching, known_even_locals is that we don't have in this case)
		Multiset<LookaheadMatching> rett = matchWithProcessed(matchingStates, knownValues, null, consEnum);
		
		return rett;
	}
	
	/**
	 * Searches all changes in matchings when model changes. It requires the pattern, the engine, the processed pattern body (cachedStructure), and the already known values
	 */
	
	/**
	 * We can use this to start form a partial match. The AheadStructure can be modified before passing this method, eg. removing
	 * searched constraints (assuming it is "satisfied"). We can also do other tricks as well: bind variables to values by hand etc.
	 * Note: if you need clean AheadStructures, use PatternOnlyProcess()
	 * @param engine IncQueryEngine
	 * @param modPattern The query to match
	 * @param cachedStructure The cached AheadStructure to use for match from this state
	 * @param knownValues The known values in order of the query parameters (if not known, "null")
	 * @param consEnum The constraint enumerator to use for cost guessing and iterating. If set to null, default is used!
	 * @return The matches found from this AheadStructure cachedStructure
	 */
	public Multiset<LookaheadMatching> searchChangesAll(IncQueryEngine engine, PQuery modPattern, AheadStructure cachedStructure,
			HashMap<PVariable, Object> knownValues, IConstraintEnumerator consEnum)
	{
		initialize(modPattern, engine);
		
		ArrayList<AheadStructure> structOne = new ArrayList<AheadStructure>();
		structOne.add(cachedStructure);
		return this.searchChangesAll(engine, modPattern, structOne, knownValues,consEnum);
	}
	
	/**
	 * We can use this to start form a partial matches. Same as the previous one, but with an ArrayList of AheadStructures
	 * to match from more than one modified AheadStructure. Useful if pattern has more bodies.
	 * @param engine IncQueryEngine
	 * @param modPattern The query to match
	 * @param cachedStructures The cached AheadStructures to use for match from this state
	 * @param knownValues The known values in order of the query parameters (if not known, "null")
	 * @param consEnum The constraint enumerator to use for cost guessing and iterating. If set to null, default is used!
	 * @return The matches found from these AheadStructure cachedStructure
	 */
	public Multiset<LookaheadMatching> searchChangesAll(IncQueryEngine engine, PQuery modPattern, ArrayList<AheadStructure> cachedStructures, HashMap<PVariable, Object> knownValues, IConstraintEnumerator consEnum)
	{
		initialize(modPattern, engine);
		
		Multiset<LookaheadMatching> matches = HashMultiset.create();//new MultiSet<LookaheadMatching>();
		
		
		// match all with no known values but known mapped values to bind (can be local etc.)
		matches.addAll(matchWithProcessed(cachedStructures, null, knownValues, consEnum));
		
		return matches;
	}
	
	/**
	 * Returns all findX negfindX patterns, that occur in P pattern (query) (not recursively, only first depth!)
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
	
	/**
	 * Initializes for the query and all the called and neg called queries. After this, the NavigationHelper is ready
	 * @param query The query and all the called/negcalled queries to initialize.
	 * @param engine The incqueryengine (needed for pattern processing)
	 */
	public void InitializeAll(PQuery query, IncQueryEngine engine)
	{
		initialize(query, engine);
		PatternCallModes list = getFindListForPattern(query);
		for(PQuery q: list.getCallingPatternsSimply())
		{
			// no cycle (DAG):
			InitializeAll(q, engine);
		}
	}
	
	/**
	 *  Processes a pattern, but does not match it. Used to get AheadStructure list for this pattern. One AheadStructure for one body.
	 * @param query The query to process
	 * @param engine The IncQueryEngine
	 * @param patternCacher The partial cacher, if set to null, default is used
	 * @return Returns the processed structures
	 */
	public ArrayList<AheadStructure> PatternOnlyProcess(PQuery query, IncQueryEngine engine, IPartialPatternCacher patternCacher)
	{
		if (patternCacher == null)
		{
			patternCacher = new SimplePatternCacher(engine, this.navigationHelper);
		}
		return processPattern(query, engine, patternCacher);
	}
	
	/**
	 * Initializes for one query, not recursively for all called queries!
	 * @param query The query to initialize into NavigationHelper
	 * @param engine The IncQueryEngine
	 */
	public void initialize(PQuery query, IncQueryEngine engine)
	{
		if (this.initializeds.contains(query))
			return;
		ArrayList<AheadStructure> structs = PatternOnlyProcess(query, engine, null);
		
		Set<ENamedElement> nameds = new HashSet<ENamedElement>();
		
		Set<EDataType> types = new HashSet<EDataType>();
		Set<EClass> classes = new HashSet<EClass>();
		Set<EStructuralFeature> featureds = new HashSet<EStructuralFeature>();
		
		for (AheadStructure struct : structs)
		{
			for (AxisConstraint ac : struct.SearchedConstraints)
			{
				if (ac instanceof TypeConstraint)
				{
					nameds.add(((TypeConstraint)ac).getType());
				}
				else if (ac instanceof RelationConstraint)
				{
					RelationConstraint rCon = (RelationConstraint) ac;
					// register if class and register if not already registered type
					featureds.add(rCon.getEdge());
					// egysegesen
					nameds.add(rCon.getEdge());
				}
			}
		}
		for (ENamedElement eNamed : nameds)
		{
			if (eNamed instanceof EDataType)
				types.add((EDataType) eNamed);
			else if (eNamed instanceof EClass)
				classes.add((EClass) eNamed);
		}
		if (this.navigationHelper == null)
		{
			try
			{
				this.navigationHelper = engine.getBaseIndex();
			}
			catch (IncQueryException e)
			{
				e.printStackTrace();
				return;
			}
		}
		// navhelper: watch all these!!!!:
		this.navigationHelper.registerObservedTypes(classes, types, featureds);
		initializeds.add(query);
	}
	
	
	/*
	 * Private methods:
	 */

	// Processes a pattern, creating aheadstructures (aheadstructure: "subPlan")
	private ArrayList<AheadStructure> processPattern(PQuery chosenSpecification, IncQueryEngine engine, IPartialPatternCacher patternCacher)
	{
		if (patternCacher == null)
		{
			patternCacher = new SimplePatternCacher(engine, this.navigationHelper);
		}
		return (new PatternProcessor(engine, patternCacher)).Process(chosenSpecification);
	}
	
	/**
	 * process pattern ready, pass to match
	 * two options are available: knownParameterValues (in order of pattern definition variables) are known values of the parameter variables
	 * the other option is knownLocalAndParameterValues (can be any variable present in this pattern) to bind those vars
	 */
	private Multiset<LookaheadMatching> matchWithProcessed(ArrayList<AheadStructure> preparedStructures, ArrayList<Object> knownParameterValues, HashMap<PVariable, Object> knownLocalAndParameterValues, IConstraintEnumerator consEnum)
	{
		// create matcher algo
		MatcherAlgorithm readyToMatchAlgo = new MatcherAlgorithm();
		
		//matches to return
		Multiset<LookaheadMatching> matches = HashMultiset.create();//new MultiSet<LookaheadMatching>();
		
		// iterate all structure and match them! (collect matches)
		for (AheadStructure stateNoClone : preparedStructures)
		{
			AheadStructure state = stateNoClone.clone(); // 1st step!
			
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
		
		return matches;
	}
}
