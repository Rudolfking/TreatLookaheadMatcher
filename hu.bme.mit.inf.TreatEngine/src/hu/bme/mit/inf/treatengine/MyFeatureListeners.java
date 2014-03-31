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
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.incquery.runtime.base.api.DataTypeListener;
import org.eclipse.incquery.runtime.base.api.FeatureListener;
import org.eclipse.incquery.runtime.base.api.IncQueryBaseIndexChangeListener;
import org.eclipse.incquery.runtime.base.api.InstanceListener;
import org.eclipse.incquery.runtime.base.api.NavigationHelper;
import org.eclipse.incquery.runtime.matchers.psystem.PQuery;
import org.eclipse.incquery.runtime.matchers.psystem.PVariable;

import com.google.common.collect.HashMultimap;

public class MyFeatureListeners
{
	private LookaheadMatcherTreat treat;
	private LookaheadMatcherInterface lookaheadMatcher;
	private NavigationHelper navHelper;


	public MyFeatureListeners(LookaheadMatcherTreat treat, LookaheadMatcherInterface lookMatcher, NavigationHelper navigationHelper)
	{
		this.treat = treat; 
		this.lookaheadMatcher = lookMatcher;
		this.navHelper = navigationHelper;
	}
	
	private boolean isModified;
	
	
	public IncQueryBaseIndexChangeListener baseIndexChangeListener = new IncQueryBaseIndexChangeListener() {
		
		@Override
		public boolean onlyOnIndexChange()
		{
			return true;
		}
		
		@Override
		public void notifyChanged(boolean indexChanged)
		{
			System.out.println("[MAGIC] After magic collect:" + modelChanges.size());
			MagicProcessor(); // remove list
			return;
		}
	};
	
	
	
	
	// captures an "instance" (eclass) change (insertion, deletion) then copies and modifies the aheadstructures to match insertion/deletion, then runs a patternmatcher, then updates the cached matchings
	public InstanceListener classListener = new InstanceListener()
	{
		@Override
		public void instanceInserted(EClass clazz, EObject instance)
		{
			modelChanges.add(new EClassChange(clazz, instance, true));
			
			
//			
//			
//			
//			// InstanceDelta instInsert = new InstanceDelta(clazz, instance, true);
//			System.out.println("[ECLASS] Update affected patterns' matchings started!");
//			long start = System.currentTimeMillis();
//			// changes:
//			ArrayList<ModelDelta> deltas = new ArrayList<ModelDelta>();
//			
//			// check all patterns affected!
//			for (PQuery maybeModPattern : LookaheadMatcherTreat.RelativeSet.get(clazz))
//			{
//				// this lookvariable should be ---> this object (the matcher will bind it)
//				HashMap<PVariable, Object> knownLocalAndParameters = new HashMap<PVariable, Object>();
//				
//				// get cached pattern structures
//				ArrayList<AheadStructure> cachedStructures = LookaheadMatcherTreat.GodSetStructures.get(maybeModPattern);
//				for (AheadStructure aSn : cachedStructures)
//				{
//					// find all typeConstraints (eclass affected!)
//					for (AxisConstraint tC : aSn.SearchedConstraints)
//					{
//						if (tC instanceof TypeConstraint)
//						{
//							if (((TypeConstraint) tC).getType().equals(clazz))
//							{
//								// affected typeconstraint's lookvariable should be bound!!
//								knownLocalAndParameters.put(((TypeConstraint) tC).getTypedVariable(), instance);
//							}
//						}
//					}
//				}
//				isModified = false;
//				ArrayList<AheadStructure> newStructs = createNewFromOldTypeC(true, clazz, instance, cachedStructures);
//				if (isModified == false)
//					continue;
//				// the new matches that'll appear in matching
//				MultiSet<LookaheadMatching> newbies = lookaheadMatcher.searchChangesAll(treat.getIncQueryEngine(), maybeModPattern, (ArrayList<AheadStructure>)newStructs.clone(), knownLocalAndParameters, null);
//				
//				// a new map to store a matching and whether it is added or removed
//				HashMultimap<LookaheadMatching, Boolean> newMatchingsAndAddition = HashMultimap.create();
//				
//				// iterate over multiset and create delta
//				for (Entry<LookaheadMatching, Integer> inners : newbies.getInnerMap().entrySet())
//				{
//					for (int pi = 0; pi < inners.getValue(); pi++)
//						newMatchingsAndAddition.put(inners.getKey(), true); // the count in multiset (more of tha same found: more changes)
//				}
//				// delta needed to propagate the changes
//				if (newMatchingsAndAddition.size()>0)
//				{
//					ModelDelta d = new ModelDelta(maybeModPattern, newMatchingsAndAddition);
//					deltas.add(d);
//				}
//			}
//			// apply deltas (depth apply!)
//			for (ModelDelta delta : deltas)
//			{
//				AdvancedDeltaProcessor.getInstance().ReceiveDelta(delta);
//			}
//			AdvancedDeltaProcessor.getInstance().ProcessReceivedDeltaSet();
//			// match based on the new structs!
//			System.out.println("[ECLASS] Update matchings finished! Time:" + Long.toString(System.currentTimeMillis() - start));
		}
		
		@Override
		public void instanceDeleted(EClass clazz, EObject instance)
		{
			modelChanges.add(new EClassChange(clazz, instance, false));
			
//			
//			
//			
//			// InstanceDelta instRemov = new InstanceDelta(clazz, instance, false);
//			System.out.println("[ECLASS] Delete affected patterns' matchings if class instance used in them started!");
//			long start = System.currentTimeMillis();
//			
//			// changes:
//			ArrayList<ModelDelta> deltas = new ArrayList<ModelDelta>();
//			
//			for (PQuery maybeModPattern : LookaheadMatcherTreat.RelativeSet.get(clazz))
//			{
//				// this lookvariable should be ---> this object (the matcher will bind it)
//				HashMap<PVariable, Object> knownLocalAndParameters = new HashMap<PVariable, Object>();
//				
//				isModified = false;
//				ArrayList<AheadStructure> cachedStructures = LookaheadMatcherTreat.GodSetStructures.get(maybeModPattern);
//				for (AheadStructure aSn : cachedStructures)
//				{
//					// find all typeConstraints (eclass affected!)
//					for (AxisConstraint tC : aSn.SearchedConstraints)
//					{
//						if (tC instanceof TypeConstraint)
//						{
//							if (((TypeConstraint) tC).getType().equals(clazz))
//							{
//								// affected typeconstraint's lookvariable should be bound!!
//								knownLocalAndParameters.put(((TypeConstraint) tC).getTypedVariable(), instance);
//							}
//						}
//					}
//				}
//								
//				isModified = false;
//				ArrayList<AheadStructure> newStructs = createNewFromOldTypeC(true, clazz, instance, LookaheadMatcherTreat.GodSetStructures.get(maybeModPattern));
//				if (isModified)
//				{
//					// the new matches that'll appear in matching
//					MultiSet<LookaheadMatching> newbies_todelete = lookaheadMatcher.searchChangesAll(treat.getIncQueryEngine(), maybeModPattern, newStructs, knownLocalAndParameters, null);
//					
//					// a new map to store a matching and whether it is added or removed
//					HashMultimap<LookaheadMatching, Boolean> newMatchingsAndRemoval = HashMultimap.create();
//					
//					// iterate over multiset and create delta
//					for (Entry<LookaheadMatching, Integer> inners : newbies_todelete.getInnerMap().entrySet())
//					{
//						for (int pi = 0; pi < inners.getValue(); pi++)
//							newMatchingsAndRemoval.put(inners.getKey(), false); // the count in multiset (more of tha same found: more changes), false: deleted!!
//					}
//					// delta needed to propagate the changes
//					if (newMatchingsAndRemoval.size()>0)
//					{
//						ModelDelta d = new ModelDelta(maybeModPattern, newMatchingsAndRemoval);
//						deltas.add(d);
//					}
//				}
//			}
//			
//			// apply deltas (depth apply!)
//			for (ModelDelta delta : deltas)
//			{
//				AdvancedDeltaProcessor.getInstance().ReceiveDelta(delta);
//			}
//			AdvancedDeltaProcessor.getInstance().ProcessReceivedDeltaSet();
//			
//			System.out.println("[ECLASS] Delete affected patterns' matchings ended! Time:" + Long.toString(System.currentTimeMillis() - start));
		}
	};
	
	// listens to datatype changes, works as the instancelistener, but uses relation structure update (matches relations by hand before matching)
	public DataTypeListener dataTypeListener = new DataTypeListener()
	{
		@Override
		public void dataTypeInstanceInserted(EDataType type, Object instance, boolean firstOccurrence)
		{
			modelChanges.add(new EDataTypeChange(type, instance, true));
			
			
			
			
			// DatatypeDelta dataInser = new DatatypeDelta(type, instance, true);
//			System.out.println("[EDATATYPE] Update affected patterns' matchings started!");
//			long start = System.currentTimeMillis();
//			
//			// changes:
//			ArrayList<ModelDelta> deltas = new ArrayList<ModelDelta>();
//			
//			for (PQuery maybeModPattern : LookaheadMatcherTreat.RelativeSet.get(type))
//			{
//				// this lookvariable should be ---> this object (the matcher will bind it)
//				HashMap<PVariable, Object> knownLocalAndParameters = new HashMap<PVariable, Object>();
//				
//				isModified = false;
//				ArrayList<AheadStructure> cachedStructures = LookaheadMatcherTreat.GodSetStructures.get(maybeModPattern);
//				for (AheadStructure aSn : cachedStructures)
//				{
//					// find all typeConstraints (eclass affected!)
//					for (AxisConstraint tC : aSn.SearchedConstraints)
//					{
//						if (tC instanceof TypeConstraint)
//						{
//							if (((TypeConstraint) tC).getType().equals(type))
//							{
//								// affected typeconstraint's lookvariable should be bound!!
//								knownLocalAndParameters.put(((TypeConstraint) tC).getTypedVariable(), instance);
//							}
//						}
//					}
//				}
//				isModified = false;
//				ArrayList<AheadStructure> newStructs = createNewFromOldTypeC(true, type, instance, cachedStructures);
//				// the new matches that'll appear in matching
//				if (isModified == false)
//					continue;
//				
//				MultiSet<LookaheadMatching> newbies_toadd = lookaheadMatcher.searchChangesAll(treat.getIncQueryEngine(), maybeModPattern, (ArrayList<AheadStructure>)newStructs.clone(), knownLocalAndParameters, null);
//				
//				// a new map to store a matching and whether it is added or removed
//				HashMultimap<LookaheadMatching, Boolean> newMatchingsAndAddition = HashMultimap.create();
//				
//				// iterate over multiset and create delta
//				for (Entry<LookaheadMatching, Integer> inners : newbies_toadd.getInnerMap().entrySet())
//				{
//					for (int pi = 0; pi < inners.getValue(); pi++)
//						newMatchingsAndAddition.put(inners.getKey(), true); // the count in multiset (more of tha same found: more changes)
//				}
//				// delta needed to propagate the changes
//				if (newMatchingsAndAddition.size()>0)
//				{
//					ModelDelta d = new ModelDelta(maybeModPattern, newMatchingsAndAddition);
//					deltas.add(d);
//				}
//			}
//			
//			for (ModelDelta delta : deltas)
//			{
//				AdvancedDeltaProcessor.getInstance().ReceiveDelta(delta);
//			}
//			AdvancedDeltaProcessor.getInstance().ProcessReceivedDeltaSet();
//			
//			// match based on the new structs!
//			System.out.println("[EDATATYPE] Update matchings finished! Time:" + Long.toString(System.currentTimeMillis() - start));
		}
		
		@Override
		public void dataTypeInstanceDeleted(EDataType type, Object instance, boolean firstOccurrence)
		{
			modelChanges.add(new EDataTypeChange(type, instance, false));
			
			
			
//			// changes:
//			ArrayList<ModelDelta> deltas = new ArrayList<ModelDelta>();
//			
//			// DatatypeDelta dataDelete = new DatatypeDelta(type, instance, false);
//			System.out.println("[EDATATYPE] Delete affected patterns' matchings if class instance used in them started!");
//			long start = System.currentTimeMillis();
//			
//			for (PQuery maybeModPattern : LookaheadMatcherTreat.RelativeSet.get(type))
//			{
//				HashMap<PVariable, Object> knownLocalAndParameters = new HashMap<PVariable, Object>();
//				
//				isModified = false;
//				ArrayList<AheadStructure> cachedStructures = LookaheadMatcherTreat.GodSetStructures.get(maybeModPattern);
//				for (AheadStructure aSn : cachedStructures)
//				{
//					// find all typeConstraints (eclass affected!)
//					for (AxisConstraint tC : aSn.SearchedConstraints)
//					{
//						if (tC instanceof TypeConstraint)
//						{
//							if (((TypeConstraint) tC).getType().equals(type))
//							{
//								// affected typeconstraint's lookvariable should be bound!!
//								knownLocalAndParameters.put(((TypeConstraint) tC).getTypedVariable(), instance);
//							}
//						}
//					}
//				}
//				
//				ArrayList<AheadStructure> newStructs = createNewFromOldTypeC(false, type, instance, LookaheadMatcherTreat.GodSetStructures.get(maybeModPattern));
//				if (isModified)
//				{
//					// the new matches that'll appear in matching
//					MultiSet<LookaheadMatching> newbies_todelete = lookaheadMatcher.searchChangesAll(treat.getIncQueryEngine(), maybeModPattern, newStructs, knownLocalAndParameters, null);
//					
//					// a new map to store a matching and whether it is added or removed
//					HashMultimap<LookaheadMatching, Boolean> newMatchingsAndRemoval = HashMultimap.create();
//					
//					// iterate over multiset and create delta
//					for (Entry<LookaheadMatching, Integer> inners : newbies_todelete.getInnerMap().entrySet())
//					{
//						for (int pi = 0; pi < inners.getValue(); pi++)
//							newMatchingsAndRemoval.put(inners.getKey(), false); // the count in multiset (more of tha same found: more changes), false: deleted!!
//					}
//					// delta needed to propagate the changes
//					if (newMatchingsAndRemoval.size()>0)
//					{
//						ModelDelta d = new ModelDelta(maybeModPattern, newMatchingsAndRemoval);
//						deltas.add(d);
//					}
//				}
//			}
//			
//			for (ModelDelta delta : deltas)
//			{
//				AdvancedDeltaProcessor.getInstance().ReceiveDelta(delta);
//			}
//			AdvancedDeltaProcessor.getInstance().ProcessReceivedDeltaSet();
//			
//			System.out.println("[EDATATYPE] Delete affected patterns' matchings ended! Time:" + Long.toString(System.currentTimeMillis() - start));
		}
	};
	
	// listens to structuralfeature changes, uses relation modification, others are like instancelistener)
	public FeatureListener featureListener = new FeatureListener()
	{
		@Override
		public void featureInserted(EObject host, EStructuralFeature feature, Object value)
		{
			modelChanges.add(new EFeatureChange(host, feature, value, true));
			
//			
//			
//			// Delta featureInser = new FeatureDelta(feature, host, true, value);
//			System.out.println("[ESTRUCTURALFEATURE] Update affected patterns' matchings started!");
//			long start = System.currentTimeMillis();
//			// changes:
//			ArrayList<ModelDelta> deltas = new ArrayList<ModelDelta>();
//			
//			for (PQuery maybeModPattern : LookaheadMatcherTreat.RelativeSet.get(feature))
//			{
//				// this lookvariable should be ---> this object (the matcher will bind it)
//				HashMap<PVariable, Object> knownLocalAndParameters = new HashMap<PVariable, Object>();
//				
//				isModified = false;
//				ArrayList<AheadStructure> cachedStructures = LookaheadMatcherTreat.GodSetStructures.get(maybeModPattern);
//				for (AheadStructure aSn : cachedStructures)
//				{
//					// find all relationConstraints
//					for (AxisConstraint rC : aSn.SearchedConstraints)
//					{
//						if (rC instanceof RelationConstraint)
//						{
//							if (((RelationConstraint) rC).getEdge().equals(feature))
//							{
//								// affected relaconstraint's lookvariables should be bound!!
//								knownLocalAndParameters.put(((RelationConstraint) rC).getSource(), host);
//								knownLocalAndParameters.put(((RelationConstraint) rC).getTarget(), value);
//							}
//						}
//					}
//				}
//
//				isModified = false;
//				// manual satisfy:
//				ArrayList<AheadStructure> newStructs = createNewFromOldRelaC(host, value, feature, LookaheadMatcherTreat.GodSetStructures.get(maybeModPattern));
//				if (isModified == false)
//					continue;
//				// the new matches that'll appear in matching
//				MultiSet<LookaheadMatching> newbies_toadd = lookaheadMatcher.searchChangesAll(treat.getIncQueryEngine(), maybeModPattern, newStructs, knownLocalAndParameters, null);
//				
//				// a new map to store a matching and whether it is added or removed
//				HashMultimap<LookaheadMatching, Boolean> newMatchingsAndAddition = HashMultimap.create();
//				
//				// iterate over multiset and create delta
//				for (Entry<LookaheadMatching, Integer> inners : newbies_toadd.getInnerMap().entrySet())
//				{
//					for (int pi = 0; pi < inners.getValue(); pi++)
//						newMatchingsAndAddition.put(inners.getKey(), true); // the count in multiset (more of tha same found: more changes)
//				}
//				// delta needed to propagate the changes
//				if (newMatchingsAndAddition.size()>0)
//				{
//					ModelDelta d = new ModelDelta(maybeModPattern, newMatchingsAndAddition);
//					deltas.add(d);
//				}
//			}
//			// apply deltas
//			for (ModelDelta delta : deltas)
//			{
//				AdvancedDeltaProcessor.getInstance().ReceiveDelta(delta);
//			}
//			AdvancedDeltaProcessor.getInstance().ProcessReceivedDeltaSet();
//			
//			System.out.println("[ESTRUCTURALFEATURE] Update matchings finished! Time:" + Long.toString(System.currentTimeMillis() - start));
		}
		
		@Override
		public void featureDeleted(EObject host, EStructuralFeature feature, Object value)
		{
			modelChanges.add(new EFeatureChange(host, feature, value, false));
			
			
//			
//			// Delta featureDeleted = new FeatureDelta(feature, host, false, value);
//			System.out.println("[ESTRUCTURALFEATURE] Delete affected patterns' matchings if class instance used in them started!");
//			long start = System.currentTimeMillis();
//			
//			// changes:
//			ArrayList<ModelDelta> deltas = new ArrayList<ModelDelta>();
//			
//			for (PQuery maybeModPattern : LookaheadMatcherTreat.RelativeSet.get(feature))
//			{
//				// this lookvariable should be ---> this object (the matcher will bind it)
//				HashMap<PVariable, Object> knownLocalAndParameters = new HashMap<PVariable, Object>();
//				
//				isModified = false;
//				ArrayList<AheadStructure> cachedStructures = LookaheadMatcherTreat.GodSetStructures.get(maybeModPattern);
//				for (AheadStructure aSn : cachedStructures)
//				{
//					// find all relationConstraints
//					for (AxisConstraint rC : aSn.SearchedConstraints)
//					{
//						if (rC instanceof RelationConstraint)
//						{
//							if (((RelationConstraint) rC).getEdge().equals(feature))
//							{
//								// affected relaconstraint's lookvariables should be bound!!
//								knownLocalAndParameters.put(((RelationConstraint) rC).getSource(), host);
//								knownLocalAndParameters.put(((RelationConstraint) rC).getTarget(), value);
//							}
//						}
//					}
//				}
//								
//				isModified = false;
//				// manual satisfy:
//				ArrayList<AheadStructure> newStructs = createNewFromOldRelaC(host, value, feature, LookaheadMatcherTreat.GodSetStructures.get(maybeModPattern));
//				if (isModified)
//				{
//					// the new matches that'll appear in matching based on manually satisfied structure
//					MultiSet<LookaheadMatching> newbies_toremove = lookaheadMatcher.searchChangesAll(treat.getIncQueryEngine(), maybeModPattern, newStructs, knownLocalAndParameters, null);
//					
//					// a new map to store a matching and whether it is added or removed
//					HashMultimap<LookaheadMatching, Boolean> newMatchingsAndRemoval = HashMultimap.create();
//					
//					// iterate over multiset and create delta
//					for (Entry<LookaheadMatching, Integer> inners : newbies_toremove.getInnerMap().entrySet())
//					{
//						for (int pi = 0; pi < inners.getValue(); pi++)
//							newMatchingsAndRemoval.put(inners.getKey(), false); // the count in multiset (more of the same found: more changes)
//					}
//					// delta needed to propagate the changes
//					if (newMatchingsAndRemoval.size()>0)
//					{
//						ModelDelta d = new ModelDelta(maybeModPattern, newMatchingsAndRemoval);
//						deltas.add(d);
//					}
//				}
//			}
//			// apply deltas
//			for (ModelDelta delta : deltas)
//			{
//				AdvancedDeltaProcessor.getInstance().ReceiveDelta(delta);
//			}
//			AdvancedDeltaProcessor.getInstance().ProcessReceivedDeltaSet();
//			
//			System.out.println("[ESTRUCTURALFEATURE] Delete affected patterns' matchings ended! Time:" + Long.toString(System.currentTimeMillis() - start));
//			
		}
	};
	
	private List<ModelChange> modelChanges = new ArrayList<ModelChange>();
	
	public void MagicProcessor()
	{
		if (modelChanges == null)
		{
			modelChanges = new ArrayList<ModelChange>();
			return;
		}
		else if (modelChanges.size() == 0)
			return;
		// else go!
		
		// gets all model deltas and processes!
		System.out.println("[MAGIC] Update match set based on model change started...");
		
		long start = System.currentTimeMillis();

		Set<PQuery> affecteds = new HashSet<PQuery>();
		for (ModelChange change : modelChanges)
		{
			if (change instanceof EFeatureChange)
				affecteds.addAll(LookaheadMatcherTreat.RelativeSet.get(((EFeatureChange) change).getChangedFeature()));
			if (change instanceof EClassChange)
				affecteds.addAll(LookaheadMatcherTreat.RelativeSet.get(((EClassChange) change).getChange()));
			if (change instanceof EDataTypeChange)
				affecteds.addAll(LookaheadMatcherTreat.RelativeSet.get(((EDataTypeChange) change).getChange()));
		}
		
		ArrayList<ModelDelta> deltas = new ArrayList<ModelDelta>();
		
		for (PQuery affectedQuery : affecteds)
		{
			
			ArrayList<AheadStructure> cachedStructures = LookaheadMatcherTreat.GodSetStructures.get(affectedQuery);
			// deliver deltas for pattern!
			for (ModelChange change : modelChanges)
			{
				for (AheadStructure aSn : cachedStructures)
				{
					for (AxisConstraint rC : aSn.SearchedConstraints)
					{
						if (rC instanceof RelationConstraint && change instanceof EFeatureChange)
						{
							EFeatureChange changenow = (EFeatureChange) change;
							if (((RelationConstraint) rC).getEdge().equals(changenow.getChangedFeature()))
								rC.putToMailbox(change);
						}
						else if (rC instanceof TypeConstraint && change instanceof EDataTypeChange)
						{
							EDataTypeChange changenow = (EDataTypeChange) change;
							if (((TypeConstraint) rC).getType().equals(changenow.getChange()))
								rC.putToMailbox(change);
						}
						if (rC instanceof TypeConstraint && change instanceof EClassChange)
						{
							EClassChange changenow = (EClassChange) change;
							if (((TypeConstraint) rC).getType().equals(changenow.getChange()))
								rC.putToMailbox(change);
						}
					}
				}
			}

			for (ModelChange change : modelChanges)
			{
				// process this change: first remove all deltas from constraints with this change
				for (AheadStructure aSn : cachedStructures)
				{
					for (AxisConstraint rC : aSn.SearchedConstraints)
					{
						if (rC.hasMailboxContent())
						{
							if (rC.getMailboxContent().contains(change))
								rC.removeFromMailbox(change);
						}
					}
				}
				// apply modelchange:
				HashMap<PVariable, Object> knownLocalAndParameters = new HashMap<PVariable, Object>();
				for (AheadStructure aSn : cachedStructures)
				{
					// find all relationConstraints
					for (AxisConstraint rC : aSn.SearchedConstraints)
					{
						if (rC instanceof RelationConstraint && change instanceof EFeatureChange)
						{
							EFeatureChange changenow = (EFeatureChange) change;
							if (((RelationConstraint) rC).getEdge().equals(changenow.getChangedFeature()))
							{
								// affected relaconstraint's lookvariables should be bound!!
								knownLocalAndParameters.put(((RelationConstraint) rC).getSource(), changenow.getHost());
								knownLocalAndParameters.put(((RelationConstraint) rC).getTarget(), changenow.getInstance());
							}
						}
						else if (rC instanceof TypeConstraint && change instanceof EDataTypeChange)
						{
							EDataTypeChange changenow = (EDataTypeChange) change;
							if (((TypeConstraint) rC).getType().equals(changenow.getChange()))
							{
								// affected typeconstraint's lookvariable should be bound!!
								knownLocalAndParameters.put(((TypeConstraint) rC).getTypedVariable(), changenow.getInstance());
							}
						}
						if (rC instanceof TypeConstraint && change instanceof EClassChange)
						{
							EClassChange changenow = (EClassChange) change;
							if (((TypeConstraint) rC).getType().equals(changenow.getChange()))
							{
								// affected typeconstraint's lookvariable should be bound!!
								knownLocalAndParameters.put(((TypeConstraint) rC).getTypedVariable(), changenow.getInstance());
							}
						}
					}
				}
				
				// manual satisfy and clone cachedStructures (createNew* clones input):
				ArrayList<AheadStructure> newStructs = null;
				isModified = false;
				if ( change instanceof EFeatureChange)
				{
					EFeatureChange changenow = (EFeatureChange) change;
					newStructs = createNewFromOldRelaC(changenow.getHost(), changenow.getInstance(), changenow.getChangedFeature(), cachedStructures);
				}
				else if (change instanceof EDataTypeChange)
				{
					EDataTypeChange changenow = (EDataTypeChange) change;
					newStructs = createNewFromOldTypeC(false, changenow.getChange(), changenow.getInstance(), cachedStructures);
				}
				if (change instanceof EClassChange)
				{
					EClassChange changenow = (EClassChange) change;
					newStructs = createNewFromOldTypeC(false, changenow.getChange(), changenow.getInstance(), cachedStructures);
				}
				if (isModified)
				{
					// the new matches that'll appear in matching based on manually satisfied structure
					MultiSet<LookaheadMatching> newbies_toExamine = lookaheadMatcher.searchChangesAll(treat.getIncQueryEngine(), affectedQuery, newStructs, knownLocalAndParameters, new TreatConstraintEnumerator(this.navHelper));
					
					// a new map to store a matching and whether it is added or removed
					HashMultimap<LookaheadMatching, Boolean> newMatchingsAndChange = HashMultimap.create();
					
					// iterate over multiset and create delta
					for (Entry<LookaheadMatching, Integer> inners : newbies_toExamine.getInnerMap().entrySet())
					{
						for (int pi = 0; pi < inners.getValue(); pi++)
							newMatchingsAndChange.put(inners.getKey(), change.isAddition());
					}
					// delta needed to propagate the changes
					if (newMatchingsAndChange.size() > 0)
					{
						ModelDelta d = new ModelDelta(affectedQuery, newMatchingsAndChange);
						deltas.add(d);
					}
				}
			}
		}
		
		// apply deltas
		for (ModelDelta delta : deltas)
		{
			AdvancedDeltaProcessor.getInstance().ReceiveDelta(delta);
		}
		AdvancedDeltaProcessor.getInstance().ProcessReceivedDeltaSet();
		
		System.out.println("[MAGIC] Update match set based on model change ended! Time:" + Long.toString(System.currentTimeMillis() - start));
		
		// finally:
		modelChanges = new ArrayList<ModelChange>();
	}
	
	
	// modifies the aheadstructures according to eclass change - matches the constraint by hand (found, not searched, matchingvariables put)
	private ArrayList<AheadStructure> createNewFromOldTypeC(boolean isEClass, EClassifier clazzortype, Object instance, ArrayList<AheadStructure> gotStructs)
	{
		ArrayList<AheadStructure> newStructs = new ArrayList<AheadStructure>();
		for (AheadStructure aSn : gotStructs)
		{
			AheadStructure newaSn = aSn.clone();
			for (AxisConstraint tC : newaSn.SearchedConstraints)
			{
				if (tC instanceof TypeConstraint)
				{
					if (((TypeConstraint) tC).getType().equals(clazzortype))
					{
						newaSn.FoundConstraints.add(tC);
						newaSn.SearchedConstraints.remove(tC);
						isModified = true;
						break;
					}
				}
			}
			newStructs.add(newaSn);
		}
		return newStructs;
	}
	// modifies the aheadstructures according to estructuralfeature change - matches the constraint by hand (found, not searched, matchingvariables put)
	private ArrayList<AheadStructure> createNewFromOldRelaC(EObject host, Object target, EStructuralFeature edge, ArrayList<AheadStructure> gotStructs)
	{
		ArrayList<AheadStructure> newStructs = new ArrayList<AheadStructure>();
		for (AheadStructure aSn : gotStructs)
		{
			AheadStructure newaSn = aSn.clone();
			for (AxisConstraint rC : newaSn.SearchedConstraints)
			{
				if (rC instanceof RelationConstraint)
				{
					if (((RelationConstraint) rC).getEdge().equals(edge))
					{
						// and make constraint "found"
						newaSn.FoundConstraints.add(rC);
						newaSn.SearchedConstraints.remove(rC);
						isModified = true;
						break;
					}
				}
			}
			newStructs.add(newaSn);
		}
		return newStructs;
	}
}
