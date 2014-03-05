package hu.bme.mit.inf.treatengine;

import hu.bme.mit.inf.lookaheadmatcher.LookaheadMatcherInterface;
import hu.bme.mit.inf.lookaheadmatcher.impl.AheadStructure;
import hu.bme.mit.inf.lookaheadmatcher.impl.AxisConstraint;
import hu.bme.mit.inf.lookaheadmatcher.impl.LookaheadMatching;
import hu.bme.mit.inf.lookaheadmatcher.impl.MultiSet;
import hu.bme.mit.inf.lookaheadmatcher.impl.RelationConstraint;
import hu.bme.mit.inf.lookaheadmatcher.impl.TypeConstraint;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.incquery.runtime.base.api.DataTypeListener;
import org.eclipse.incquery.runtime.base.api.FeatureListener;
import org.eclipse.incquery.runtime.base.api.InstanceListener;
import org.eclipse.incquery.runtime.matchers.psystem.PQuery;
import org.eclipse.incquery.runtime.matchers.psystem.PVariable;

import com.google.common.collect.HashMultimap;

public class MyFeatureListeners
{
	private LookaheadMatcherTreat treat;
	private LookaheadMatcherInterface lookaheadMatcher;


	public MyFeatureListeners(LookaheadMatcherTreat treat, LookaheadMatcherInterface lookMatcher)
	{
		this.treat = treat; 
		this.lookaheadMatcher = lookMatcher;
	}
	
	private boolean isModified;
	
	// captures an "instance" (eclass) change (insertion, deletion) then copies and modifies the aheadstructures to match insertion/deletion, then runs a patternmatcher, then updates the cached matchings
	public InstanceListener classListener = new InstanceListener()
	{
		@Override
		public void instanceInserted(EClass clazz, EObject instance)
		{
			// InstanceDelta instInsert = new InstanceDelta(clazz, instance, true);
			System.out.println("[ECLASS] Update affected patterns' matchings started!");
			long start = System.currentTimeMillis();
			// changes:
			ArrayList<Delta> deltas = new ArrayList<Delta>();
			
			// check all patterns affected!
			for (PQuery maybeModPattern : LookaheadMatcherTreat.RelativeSet.get(clazz))
			{
				// this lookvariable should be ---> this object (the matcher will bind it)
				HashMap<PVariable, Object> knownLocalAndParameters = new HashMap<PVariable, Object>();
				
				// get cached pattern structures
				ArrayList<AheadStructure> cachedStructures = LookaheadMatcherTreat.GodSetStructures.get(maybeModPattern);
				for (AheadStructure aSn : cachedStructures)
				{
					// find all typeConstraints (eclass affected!)
					for (AxisConstraint tC : aSn.SearchedConstraints)
					{
						if (tC instanceof TypeConstraint)
						{
							if (((TypeConstraint) tC).getType().equals(clazz))
							{
								// affected typeconstraint's lookvariable should be bound!!
								knownLocalAndParameters.put(((TypeConstraint) tC).getTypedVariable(), instance);
							}
						}
					}
				}
				
				// the new matches that'll appear in matching
				MultiSet<LookaheadMatching> newbies = lookaheadMatcher.searchChangesAll(treat.getIncQueryEngine(), maybeModPattern, cachedStructures, knownLocalAndParameters);
				
				// a new map to store a matching and whether it is added or removed
				HashMultimap<LookaheadMatching, Boolean> newMatchingsAndAddition = HashMultimap.create();
				
				// iterate over multiset and create delta
				for (Entry<LookaheadMatching, Integer> inners : newbies.getInnerMap().entrySet())
				{
					for (int pi = 0; pi < inners.getValue(); pi++)
						newMatchingsAndAddition.put(inners.getKey(), true); // the count in multiset (more of tha same found: more changes)
				}
				// delta needed to propagate the changes
				Delta d = new Delta(maybeModPattern, newMatchingsAndAddition);
				deltas.add(d);
				
				// ret
				
				/*
				isModified = false;
				ArrayList<AheadStructure> newStructs = createNewFromOldTypeC(true, clazz, instance, GodSetStructures.get(maybeMod));
				if (isModified)
				{
					MultiSet<LookaheadMatching> newbies = matcher.updateAll(engine, maybeMod, newStructs);
					HashMap<LookaheadMatching, Boolean> newMap = new HashMap<LookaheadMatching, Boolean>();
					if (newbies.size() > 0)
					{
						MultiSet<LookaheadMatching> combinedMatching = GodSet.get(maybeMod);
						for (Entry<LookaheadMatching, Integer> inners : newbies.getInnerMap().entrySet())
						{
							combinedMatching.add(inners.getKey());
							for (int pi = 0; pi < inners.getValue(); pi++)
								newMap.put(inners.getKey(), true); // the count in multiset (more of tha same found: more changes)
						}
						GodSet.put(maybeMod, combinedMatching);
					}
				}
				*/
			}
			// apply deltas (depth apply!)
			for (Delta delta : deltas)
			{
				DeltaProcessor.getInstance().ProcessDelta(delta);
			}
			// match based on the new structs!
			System.out.println("[ECLASS] Update matchings finished! Time:" + Long.toString(System.currentTimeMillis() - start));
		}
		
		@Override
		public void instanceDeleted(EClass clazz, EObject instance)
		{
			// InstanceDelta instRemov = new InstanceDelta(clazz, instance, false);
			System.out.println("[ECLASS] Delete affected patterns' matchings if class instance used in them started!");
			long start = System.currentTimeMillis();
			
			// changes:
			ArrayList<Delta> deltas = new ArrayList<Delta>();
			
			for (PQuery maybeModPattern : LookaheadMatcherTreat.RelativeSet.get(clazz))
			{
				// this lookvariable should be ---> this object (the matcher will bind it)
				HashMap<PVariable, Object> knownLocalAndParameters = new HashMap<PVariable, Object>();
				
				isModified = false;
				ArrayList<AheadStructure> cachedStructures = LookaheadMatcherTreat.GodSetStructures.get(maybeModPattern);
				for (AheadStructure aSn : cachedStructures)
				{
					// find all typeConstraints (eclass affected!)
					for (AxisConstraint tC : aSn.SearchedConstraints)
					{
						if (tC instanceof TypeConstraint)
						{
							if (((TypeConstraint) tC).getType().equals(clazz))
							{
								// affected typeconstraint's lookvariable should be bound!!
								knownLocalAndParameters.put(((TypeConstraint) tC).getTypedVariable(), instance);
							}
						}
					}
				}
				
				
				// the new matches that'll appear in matching
				MultiSet<LookaheadMatching> newbies_todelete = lookaheadMatcher.searchChangesAll(treat.getIncQueryEngine(), maybeModPattern, cachedStructures, knownLocalAndParameters);
				
				// a new map to store a matching and whether it is added or removed
				HashMultimap<LookaheadMatching, Boolean> newMatchingsAndAddition = HashMultimap.create();//<LookaheadMatching, Boolean>();
				
				// iterate over multiset and create delta
				for (Entry<LookaheadMatching, Integer> inners : newbies_todelete.getInnerMap().entrySet())
				{
					for (int pi = 0; pi < inners.getValue(); pi++)
						newMatchingsAndAddition.put(inners.getKey(), false); // the count in multiset (more of tha same found: more changes), false: deleted!!
				}
				// delta needed to propagate the changes
				Delta d = new Delta(maybeModPattern, newMatchingsAndAddition);
				deltas.add(d);
				
				
				/* ArrayList<AheadStructure> newStructs = createNewFromOldTypeC(true, clazz, instance, GodSetStructures.get(maybeMod)); if (isModified) { MultiSet<LookaheadMatching> toDeletes = matcher.updateAll(engine, maybeMod,
				 * newStructs); HashMap<LookaheadMatching, Boolean> newMap = new HashMap<LookaheadMatching, Boolean>(); if (toDeletes.size()>0) { MultiSet<LookaheadMatching> combinedMatching = GodSet.get(maybeMod); for
				 * (Entry<LookaheadMatching, Integer> inners : toDeletes.getInnerMap().entrySet()) { boolean removed_e = combinedMatching.remove(inners.getKey()); if (removed_e == false) {
				 * System.out.println("Nagy a baj, removed de nem volt benne! Exception!"); } for(int pi=0;pi<inners.getValue();pi++) newMap.put(inners.getKey(), false); // false: deleted matching } GodSet.put(maybeMod, combinedMatching);
				 * Delta d = new Delta(maybeMod, newMap); deltas.add(d); } } else { System.out.println("It is not affected!"); } */
			}
			
			// apply deltas (depth apply!)
			for (Delta delta : deltas)
			{
				DeltaProcessor.getInstance().ProcessDelta(delta);
			}
			
			System.out.println("[ECLASS] Delete affected patterns' matchings ended! Time:" + Long.toString(System.currentTimeMillis() - start));
		}
	};
	
	// listens to datatype changes, works as the instancelistener, but uses relation structure update (matches relations by hand before matching)
	public DataTypeListener dataTypeListener = new DataTypeListener()
	{
		@Override
		public void dataTypeInstanceInserted(EDataType type, Object instance, boolean firstOccurrence)
		{
			// DatatypeDelta dataInser = new DatatypeDelta(type, instance, true);
			System.out.println("[EDATATYPE] Update affected patterns' matchings started!");
			long start = System.currentTimeMillis();
			
			// changes:
			ArrayList<Delta> deltas = new ArrayList<Delta>();
			
			for (PQuery maybeModPattern : LookaheadMatcherTreat.RelativeSet.get(type))
			{
				// this lookvariable should be ---> this object (the matcher will bind it)
				HashMap<PVariable, Object> knownLocalAndParameters = new HashMap<PVariable, Object>();
				
				isModified = false;
				ArrayList<AheadStructure> cachedStructures = LookaheadMatcherTreat.GodSetStructures.get(maybeModPattern);
				for (AheadStructure aSn : cachedStructures)
				{
					// find all typeConstraints (eclass affected!)
					for (AxisConstraint tC : aSn.SearchedConstraints)
					{
						if (tC instanceof TypeConstraint)
						{
							if (((TypeConstraint) tC).getType().equals(type))
							{
								// affected typeconstraint's lookvariable should be bound!!
								knownLocalAndParameters.put(((TypeConstraint) tC).getTypedVariable(), instance);
							}
						}
					}
				}
				
				
				// the new matches that'll appear in matching
				MultiSet<LookaheadMatching> newbies_toadd = lookaheadMatcher.searchChangesAll(treat.getIncQueryEngine(), maybeModPattern, cachedStructures, knownLocalAndParameters);
				
				// a new map to store a matching and whether it is added or removed
				HashMultimap<LookaheadMatching, Boolean> newMatchingsAndAddition = HashMultimap.create();//<LookaheadMatching, Boolean>();
				
				// iterate over multiset and create delta
				for (Entry<LookaheadMatching, Integer> inners : newbies_toadd.getInnerMap().entrySet())
				{
					for (int pi = 0; pi < inners.getValue(); pi++)
						newMatchingsAndAddition.put(inners.getKey(), true); // the count in multiset (more of tha same found: more changes)
				}
				// delta needed to propagate the changes
				Delta d = new Delta(maybeModPattern, newMatchingsAndAddition);
				deltas.add(d);
				
				
				/* ArrayList<AheadStructure> newStructs = createNewFromOldTypeC(false, type, instance, GodSetStructures.get(maybeModPattern)); if (isModified) { MultiSet<LookaheadMatching> newbies = matcher.updateAll(engine,
				 * maybeModPattern, newStructs); if (newbies.size()>0) { MultiSet<LookaheadMatching> combinedMatching = GodSet.get(maybeModPattern); for (LookaheadMatching lMa : newbies.toArrayList()) { combinedMatching.add(lMa); }
				 * GodSet.put(maybeModPattern, combinedMatching); } } */
			}
			
			for (Delta delta : deltas)
			{
				DeltaProcessor.getInstance().ProcessDelta(delta);
			}
			
			// match based on the new structs!
			System.out.println("[EDATATYPE] Update matchings finished! Time:" + Long.toString(System.currentTimeMillis() - start));
		}
		
		@Override
		public void dataTypeInstanceDeleted(EDataType type, Object instance, boolean firstOccurrence)
		{
			// changes:
			ArrayList<Delta> deltas = new ArrayList<Delta>();
			
			// DatatypeDelta dataDelete = new DatatypeDelta(type, instance, false);
			System.out.println("[EDATATYPE] Delete affected patterns' matchings if class instance used in them started!");
			long start = System.currentTimeMillis();
			
			for (PQuery maybeModPattern : LookaheadMatcherTreat.RelativeSet.get(type))
			{
				HashMap<PVariable, Object> knownLocalAndParameters = new HashMap<PVariable, Object>();
				
				isModified = false;
				ArrayList<AheadStructure> cachedStructures = LookaheadMatcherTreat.GodSetStructures.get(maybeModPattern);
				for (AheadStructure aSn : cachedStructures)
				{
					// find all typeConstraints (eclass affected!)
					for (AxisConstraint tC : aSn.SearchedConstraints)
					{
						if (tC instanceof TypeConstraint)
						{
							if (((TypeConstraint) tC).getType().equals(type))
							{
								// affected typeconstraint's lookvariable should be bound!!
								knownLocalAndParameters.put(((TypeConstraint) tC).getTypedVariable(), instance);
							}
						}
					}
				}
				// the new matches that'll appear in matching
				MultiSet<LookaheadMatching> newbies_todelete = lookaheadMatcher.searchChangesAll(treat.getIncQueryEngine(), maybeModPattern, cachedStructures, knownLocalAndParameters);
				
				// a new map to store a matching and whether it is added or removed
				HashMultimap<LookaheadMatching, Boolean> newMatchingsAndRemoval = HashMultimap.create(); // <LookaheadMatching, Boolean>();
				
				// iterate over multiset and create delta
				for (Entry<LookaheadMatching, Integer> inners : newbies_todelete.getInnerMap().entrySet())
				{
					for (int pi = 0; pi < inners.getValue(); pi++)
						newMatchingsAndRemoval.put(inners.getKey(), false); // the count in multiset (more of tha same found: more changes), false: deleted!!
				}
				// delta needed to propagate the changes
				Delta d = new Delta(maybeModPattern, newMatchingsAndRemoval);
				deltas.add(d);
				
				
				
				
				/* ArrayList<AheadStructure> newStructs = createNewFromOldTypeC(false, type, instance, GodSetStructures.get(maybeMod)); if (isModified) { MultiSet<LookaheadMatching> toDeletes = matcher.updateAll(engine, maybeMod,
				 * newStructs); if (toDeletes.size()>0) { MultiSet<LookaheadMatching> combinedMatching = GodSet.get(maybeMod); for (LookaheadMatching lMa : toDeletes.toArrayList()) { boolean removed_e = combinedMatching.remove(lMa); if
				 * (removed_e == false) { System.out.println("Nagy a baj, removed de nem volt benne! Exception!"); } } GodSet.put(maybeMod, combinedMatching); } } */
			}
			
			for (Delta delta : deltas)
			{
				DeltaProcessor.getInstance().ProcessDelta(delta);
			}
			
			System.out.println("[EDATATYPE] Delete affected patterns' matchings ended! Time:" + Long.toString(System.currentTimeMillis() - start));
		}
	};
	
	
	// listens to structuralfeature changes, uses relation modification, others are like instancelistener)
	public FeatureListener featureListener = new FeatureListener()
	{
		@Override
		public void featureInserted(EObject host, EStructuralFeature feature, Object value)
		{
			// Delta featureInser = new FeatureDelta(feature, host, true, value);
			System.out.println("[ESTRUCTURALFEATURE] Update affected patterns' matchings started!");
			long start = System.currentTimeMillis();
			// changes:
			ArrayList<Delta> deltas = new ArrayList<Delta>();
			
			for (PQuery maybeModPattern : LookaheadMatcherTreat.RelativeSet.get(feature))
			{
				// this lookvariable should be ---> this object (the matcher will bind it)
				HashMap<PVariable, Object> knownLocalAndParameters = new HashMap<PVariable, Object>();
				
				isModified = false;
				ArrayList<AheadStructure> cachedStructures = LookaheadMatcherTreat.GodSetStructures.get(maybeModPattern);
				for (AheadStructure aSn : cachedStructures)
				{
					// find all relationConstraints
					for (AxisConstraint rC : aSn.SearchedConstraints)
					{
						if (rC instanceof RelationConstraint)
						{
							if (((RelationConstraint) rC).getEdge().equals(feature))
							{
								// affected relaconstraint's lookvariables should be bound!!
								knownLocalAndParameters.put(((RelationConstraint) rC).getSource(), host);
								knownLocalAndParameters.put(((RelationConstraint) rC).getTarget(), value);
							}
						}
					}
				}
				
				
				// the new matches that'll appear in matching
				MultiSet<LookaheadMatching> newbies_toadd = lookaheadMatcher.searchChangesAll(treat.getIncQueryEngine(), maybeModPattern, cachedStructures, knownLocalAndParameters);
				
				// a new map to store a matching and whether it is added or removed
				HashMultimap<LookaheadMatching, Boolean> newMatchingsAndAddition = HashMultimap.create(); // <LookaheadMatching, Boolean>();
				
				// iterate over multiset and create delta
				for (Entry<LookaheadMatching, Integer> inners : newbies_toadd.getInnerMap().entrySet())
				{
					for (int pi = 0; pi < inners.getValue(); pi++)
						newMatchingsAndAddition.put(inners.getKey(), true); // the count in multiset (more of tha same found: more changes)
				}
				// delta needed to propagate the changes
				Delta d = new Delta(maybeModPattern, newMatchingsAndAddition);
				deltas.add(d);
			}
			// apply deltas
			for (Delta delta : deltas)
			{
				DeltaProcessor.getInstance().ProcessDelta(delta);
			}
			
			System.out.println("[ESTRUCTURALFEATURE] Update matchings finished! Time:" + Long.toString(System.currentTimeMillis() - start));
		}
		
		@Override
		public void featureDeleted(EObject host, EStructuralFeature feature, Object value)
		{
			// Delta featureDeleted = new FeatureDelta(feature, host, false, value);
			System.out.println("[ESTRUCTURALFEATURE] Delete affected patterns' matchings if class instance used in them started!");
			long start = System.currentTimeMillis();
			
			// changes:
			ArrayList<Delta> deltas = new ArrayList<Delta>();
			
			for (PQuery maybeModPattern : LookaheadMatcherTreat.RelativeSet.get(feature))
			{
				// this lookvariable should be ---> this object (the matcher will bind it)
				HashMap<PVariable, Object> knownLocalAndParameters = new HashMap<PVariable, Object>();
				
				isModified = false;
				ArrayList<AheadStructure> cachedStructures = LookaheadMatcherTreat.GodSetStructures.get(maybeModPattern);
				for (AheadStructure aSn : cachedStructures)
				{
					// find all relationConstraints
					for (AxisConstraint rC : aSn.SearchedConstraints)
					{
						if (rC instanceof RelationConstraint)
						{
							if (((RelationConstraint) rC).getEdge().equals(feature))
							{
								// affected relaconstraint's lookvariables should be bound!!
								knownLocalAndParameters.put(((RelationConstraint) rC).getSource(), host);
								knownLocalAndParameters.put(((RelationConstraint) rC).getTarget(), value);
							}
						}
					}
				}
				
				
				// the new matches that'll appear in matching
				MultiSet<LookaheadMatching> newbies_toremove = lookaheadMatcher.searchChangesAll(treat.getIncQueryEngine(), maybeModPattern, cachedStructures, knownLocalAndParameters);
				
				// a new map to store a matching and whether it is added or removed
				HashMultimap<LookaheadMatching, Boolean> newMatchingsAndRemoval = HashMultimap.create(); // <LookaheadMatching, Boolean>();
				
				// iterate over multiset and create delta
				for (Entry<LookaheadMatching, Integer> inners : newbies_toremove.getInnerMap().entrySet())
				{
					for (int pi = 0; pi < inners.getValue(); pi++)
						newMatchingsAndRemoval.put(inners.getKey(), false); // the count in multiset (more of tha same found: more changes)
				}
				// delta needed to propagate the changes
				Delta d = new Delta(maybeModPattern, newMatchingsAndRemoval);
				deltas.add(d);
				
				/* isModified = false; ArrayList<AheadStructure> newStructs = createNewFromOldRelaC(host, value, feature, GodSetStructures.get(maybeModPattern)); if (isModified) { MultiSet<LookaheadMatching> toDeletes =
				 * matcher.updateAll(engine, maybeModPattern, newStructs); if (toDeletes.size()>0) { MultiSet<LookaheadMatching> combinedMatching = GodSet.get(maybeModPattern); for (LookaheadMatching lMa : toDeletes.toArrayList()) {
				 * boolean
				 * removed_e = combinedMatching.remove(lMa); if (removed_e == false) { System.out.println("Nagy a baj, removed de nem volt benne! Exception!"); } } GodSet.put(maybeModPattern, combinedMatching); } } */
			}
			// apply deltas
			for (Delta delta : deltas)
			{
				DeltaProcessor.getInstance().ProcessDelta(delta);
			}
			
			System.out.println("[ESTRUCTURALFEATURE] Delete affected patterns' matchings ended! Time:" + Long.toString(System.currentTimeMillis() - start));
			
		}
	};
}
