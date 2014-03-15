package hu.bme.mit.inf.treatengine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;

import hu.bme.mit.inf.lookaheadmatcher.IPartialPatternCacher;
import hu.bme.mit.inf.lookaheadmatcher.LookaheadMatcherInterface;
import hu.bme.mit.inf.lookaheadmatcher.impl.AheadStructure;
import hu.bme.mit.inf.lookaheadmatcher.impl.AxisConstraint;
import hu.bme.mit.inf.lookaheadmatcher.impl.FindConstraint;
import hu.bme.mit.inf.lookaheadmatcher.impl.LookaheadMatching;
import hu.bme.mit.inf.lookaheadmatcher.impl.MultiSet;
import hu.bme.mit.inf.lookaheadmatcher.impl.RelationConstraint;
import hu.bme.mit.inf.lookaheadmatcher.impl.TypeConstraint;
//import com.google.common.collect.*;

import org.eclipse.incquery.runtime.api.IQuerySpecification;
import org.eclipse.incquery.runtime.api.IncQueryEngine;
import org.eclipse.incquery.patternlanguage.patternLanguage.Pattern;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.ENamedElement;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.common.notify.Notifier;
import org.eclipse.incquery.runtime.base.api.DataTypeListener;
import org.eclipse.incquery.runtime.base.api.FeatureListener;
import org.eclipse.incquery.runtime.base.api.InstanceListener;
import org.eclipse.incquery.runtime.base.api.NavigationHelper;
import org.eclipse.incquery.runtime.exception.IncQueryException;
import org.eclipse.incquery.runtime.matchers.psystem.PQuery;
import org.eclipse.incquery.runtime.matchers.psystem.PVariable;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

public class LookaheadMatcherTreat
{
	// engine (incquery), matcher(lookahead) and navigationhelper(IQbase)
	private IncQueryEngine engine;
	private LookaheadMatcherInterface matcher;
	private NavigationHelper navHelp;
	
	private static TreatPartialPatternCacher treatPartialCacher = new TreatPartialPatternCacher();
	
	private MyFeatureListeners featureListeners;
	
	public static TreatPartialPatternCacher getTreatPartialCacher()
	{
		return treatPartialCacher;
	}
	public static void setTreatPartialCacher(TreatPartialPatternCacher treatPartCach)
	{
		treatPartialCacher = treatPartCach;
	}
	
	public IncQueryEngine getIncQueryEngine() {
		return this.engine;
	}
//	public LookaheadMatcherInterface getMatcher() {
//		return this.matcher;
//	}
	
	/**
	 *  rete engine and navigation helper in constructor
	 * @param engineRe RETE incqueryengine
	 */
	public LookaheadMatcherTreat(IncQueryEngine engineRe)
	{
		matcher = new LookaheadMatcherInterface();
		treatPartialCacher.setLookaheadTreat(this);
		engine = engineRe;
		
		DeltaProcessor.getInstance().setEngine(engineRe);
		DeltaProcessor.getInstance().setPartialCacher(treatPartialCacher);
		
		try
		{
			navHelp = engine.getBaseIndex();
		}
		catch (IncQueryException e)
		{
			e.printStackTrace();
		}
		
		this.featureListeners = new MyFeatureListeners(this, matcher);
	}
	
	/**
	 *  maps a pattern to a matching (multi set): one pattern, more matches and a specific match can occur more than once (different local variables pl.)
	 */
	public static HashMap<PQuery, MultiSet<LookaheadMatching>> GodSet = new HashMap<PQuery, MultiSet<LookaheadMatching>>();
	
	/**
	 *  a pattern and its processed structures
	 */
	public static HashMap<PQuery, ArrayList<AheadStructure>> GodSetStructures = new HashMap<PQuery, ArrayList<AheadStructure>>();
	
	/**
	 *  a pattern and its calls (P -> { find(Q), negfind(R), negfind(S) } ) where find:true, negfind:false
	 */
	public static HashMap<PQuery, Multimap<PQuery, Boolean>> PatternCallsPatterns = new HashMap<PQuery, Multimap<PQuery, Boolean>>();
	
	/**
	 *  a named element (class, structuralfeature) mapped to the affected patterns
	 */
	public static HashMap<ENamedElement, HashSet<PQuery>> RelativeSet = new HashMap<ENamedElement, HashSet<PQuery>>();

	
	/**
	 *  matches a pattern and creates and saves listeners to the pattern's instances/datatypes/structuralfeatures
	 * @param chosenQuery
	 * @return true if succeeded
	 */
	public boolean registerPatternWithMatches(PQuery chosenQuery)
	{
		// match!
		MultiSet<LookaheadMatching> matches = matcher.matchAll(engine, treatPartialCacher, chosenQuery, null, null);

		// put pattern matches to registry
		GodSet.put(chosenQuery, matches);
		// put structures to registry (if modified, fast matching available)
		GodSetStructures.put(chosenQuery, matcher.GetAheadStructures());
		
		// register new types!
		ArrayList<ENamedElement> nameds = new ArrayList<ENamedElement>();
		for (TypeConstraint tCon : matcher.GetTypeConstraints())
		{
			// register if class and register if not already registered type
			if (!nameds.contains(tCon.getType()))
				nameds.add(tCon.getType());
		}
		ArrayList<EDataType> types = new ArrayList<EDataType>();
		ArrayList<EClass> classes = new ArrayList<EClass>();
		for (ENamedElement eNamed : nameds)
		{
			if (eNamed instanceof EDataType)
				types.add((EDataType) eNamed);
			else if (eNamed instanceof EClass)
				classes.add((EClass) eNamed);
		}
		
		// register new relations!
		ArrayList<EStructuralFeature> featureds = new ArrayList<EStructuralFeature>();
		for (RelationConstraint rCon : matcher.GetRelationConstraints())
		{
			// register if class and register if not already registered type
			if (!featureds.contains(rCon.getEdge()))
				featureds.add(rCon.getEdge());
			// egysegesen
			if (!nameds.contains(rCon.getEdge()))
				nameds.add(rCon.getEdge());
		}
		ArrayList<EStructuralFeature> features = new ArrayList<EStructuralFeature>();
		for (EStructuralFeature eFeatured : featureds)
		{
			features.add(eFeatured);
		}
		
		if (types.size() > 0)
			navHelp.addDataTypeListener(types, featureListeners.dataTypeListener);
		if (classes.size() > 0)
			navHelp.addInstanceListener(classes, featureListeners.classListener);
		if (features.size() > 0)
			navHelp.addFeatureListener(features, featureListeners.featureListener);
		
		// fill relativeSet!
		for (ENamedElement eNamed : nameds)
		{
			if (RelativeSet.containsKey(eNamed))
			{
				// add to already existing list
				HashSet<PQuery> pats = RelativeSet.get(eNamed);
				if (!pats.contains(eNamed))
					pats.add(chosenQuery);
				RelativeSet.put(eNamed, pats);
			}
			else
			{
				// add as first (never will be null)
				HashSet<PQuery> newPatInSet = new HashSet<PQuery>();
				newPatInSet.add(chosenQuery);
				RelativeSet.put(eNamed, newPatInSet);
			}
		}
		
		// find/negfind patterns (whole tree) of the pattern
		
		FillPatternCallsPatterns(chosenQuery);
		
		
		return true;
	}
	
	
	/**
	 * matches a pattern using TREAT caching or creates a new cache if pattern is previously not cached
	 * @param chosenQuery Match this pattern (query)
	 * @return The matches from cache or after match
	 */
	public MultiSet<LookaheadMatching> matchThePattern(PQuery chosenQuery)
	{
		// null: does not contain, not null: contains, maybe empty
		if (GodSet.get(chosenQuery) != null)
			return GodSet.get(chosenQuery);
		this.registerPatternWithMatches(chosenQuery);
		if (GodSet.get(chosenQuery) != null)
		{
			return GodSet.get(chosenQuery);
		}
		else
		{
			System.err.println("Fatal error: matcher ran successfully, but something failed during caching.");
			return null; // error
		}
	}
	
	
	/**
	 *  find all finds recursively
	 * @param actRoot Do recursive call hierarchy topologic order for this PQuery
	 */
	private void FillPatternCallsPatterns(PQuery actRoot)
	{
		// find children of this root
		Multimap<PQuery, Boolean> findedNegfindedPatterns = matcher.getFindListForPattern(actRoot);
		if (findedNegfindedPatterns != null && findedNegfindedPatterns.size() > 0)
			PatternCallsPatterns.put(actRoot, findedNegfindedPatterns);
		else return; // no more children
		for (Entry<PQuery, Boolean> finded : findedNegfindedPatterns.entries())
		{
			// iterate for children and find their calls, too
			FillPatternCallsPatterns(finded.getKey());
		}
	}

}
