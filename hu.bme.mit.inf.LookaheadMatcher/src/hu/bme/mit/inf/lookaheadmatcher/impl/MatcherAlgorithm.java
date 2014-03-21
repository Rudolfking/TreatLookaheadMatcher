package hu.bme.mit.inf.lookaheadmatcher.impl;

import hu.bme.mit.inf.lookaheadmatcher.IConstraintEnumerator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.ENamedElement;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.EStructuralFeature.Setting;
import org.eclipse.incquery.runtime.base.api.NavigationHelper;
import org.eclipse.incquery.runtime.matchers.psystem.PVariable;

public class MatcherAlgorithm
{
	// the navigationHelper
	private NavigationHelper navigationHelper;

	// to enumerate constraint
	private IConstraintEnumerator constraintEnumerator;
	
	// how many matches do we have?
	private int matchCounter = 0;
	
	// write out every small detail?
	private boolean verbose = false;
	
	// match one or all?
	private boolean matchOne = false;
	private boolean oneMatched = false;
	
	// reference to pattern definition
	private AheadStructure actualPatternDefinition;
	
	// already found matches list
	private MultiSet<LookaheadMatching> foundMatches = new MultiSet<LookaheadMatching>();
	
	public MultiSet<LookaheadMatching> getPatternMatches(AheadStructure patternState, boolean matchOne, NavigationHelper navHelper, ArrayList<Object> knownValues, IConstraintEnumerator consEnumerator)
	{
		this.navigationHelper = navHelper;
		this.matchOne = matchOne;
		actualPatternDefinition = patternState;
		if (consEnumerator != null)
			this.constraintEnumerator = consEnumerator;
		else
			this.constraintEnumerator = new SimpleConstraintEnumerator(navHelper);
		
		
		if (knownValues != null && knownValues.size() == patternState.FixSymbolicVariables.length)
		{
			// insert known values
			for (int i = 0; i < knownValues.size(); i++)
			{
				bindToVariable(patternState.FixSymbolicVariables[i], knownValues.get(i), patternState.MatchingVariables);
				//patternState.MatchingVariables.put(patternState.FixSymbolicVariables[i], knownValues.get(i));
			}
		}
		
		matchPattern(patternState.SearchedConstraints, patternState.FoundConstraints, patternState.CheckConstraints, patternState.MatchingVariables);
		System.out.println("Matches are the following:");
		
		System.out.println("Match for this body finished! Found: " + Integer.toString(matchCounter));
		return foundMatches;
	}
	
	private void matchPattern(ArrayList<AxisConstraint> SearchedConstraints, // searched ones
			ArrayList<AxisConstraint> FoundConstraints, // found ones (to be able to restore)
			ArrayList<CheckableConstraint> checkableConstraints, // to be checked
			HashMap<PVariable, Object> MatchingVariables) // matched vars
	{
		
		
		// we should check if one match is found aaand we need no more, return:
		if (checkExit())
			return;
		
		
		
		
		// we should check the current match's eligibility: no mismatched relations and others
		if (!checkCurrentPattern(FoundConstraints, MatchingVariables))
		{
			if (verbose)
				System.out.println(", that's why the current partial matching is broken!");
			return;
		}
		
		
		ArrayList<CheckableConstraint> currenctCheckable = new ArrayList<CheckableConstraint>(checkableConstraints);
		// check the remaining checkable constraints: if one of them fails, cut the recursion
		for (int i = 0; i < currenctCheckable.size(); i++)
		{
			// if specific variables are bound
			if (currenctCheckable.get(i).CanBeEvaluated(MatchingVariables))
			{
				// this will evaluate currently: TermCheck, NACCheck
				if (!(currenctCheckable.get(i).Evaluate(MatchingVariables)))
				{
					// check failed
					return;
				}
				else
				// evaluate result == true
				{
					//remChe.add(CheckRestrictions.get(i));
					currenctCheckable.remove(i); // not searched anymore
					i--; // check the left-shifting element
				}
			}
		}
		
		
		// we should check if we matched one!
		// condition of found patterns
		if (SearchedConstraints.size() == 0 && currenctCheckable.size() == 0)
		{
			matchCounter++;
			try
			{
				// create a new matching:
				LookaheadMatching newMatch = new LookaheadMatching(actualPatternDefinition.FixSymbolicVariables, MatchingVariables);
				if (newMatch.getMatches().containsValue(null))
				{
					System.out.println("Critical error, one variable in found match is not bound!");
				}
				/*if (!foundMatches.add(newMatch))
				{
					matchCounter--;
					return;
				}*/
				foundMatches.add(newMatch);
			}
			catch (Exception e)
			{
				if (verbose)
					System.out.println(e.getMessage()); // not all variables are bound (how can it be)
				matchCounter--; // sorry-sorry-sorry-sorry
			}
			if (matchOne == true)
				oneMatched = true; // match only one, but as quick as we can
			return; //success
		}
		
		
		
		// we should iterate through constraints and get costs
		if (verbose)
			System.out.println("New step");
		// create and initialize the array "costs"
		int[] costs = new int[SearchedConstraints.size()];
		for (int i = 0; i < costs.length; i++)
			costs[i] = -1; // init
		
		
		
		// the objects we can satisfy next
		// a fix (searchedconstraints) size array of lists (each list is the corresponding constraint's matchable variables)
		@SuppressWarnings("unchecked")
		//List<List<Object>> listOfSatisfieds[] = new (List<List<Object>>) new ArrayList[SearchedConstraints.size()];// CollectConstraintMatchings(SearchedConstraints, MatchingVariables, costs);
		
		int posSat = 0;
		for (AxisConstraint constraint : SearchedConstraints)
		{
			// for now costs AND enumerable are collected (should be only costs!) TODO optimization
			costs[posSat] = constraintEnumerator.getCost(constraint, MatchingVariables);
			//listOfSatisfieds[posSat++] = constraintEnumerator.enumerateConstraint(constraint, MatchingVariables);
		}
		
		
		
		// we should choose the cheapest and declare as winner
		if (verbose)
			System.out.println("Costs for next move:");
		if (verbose)
		{
			for (int i = 0; i < costs.length; i++)
				System.out.println(costs[i]);
		}
		int min = Integer.MAX_VALUE;
		int foundIndex = -1;
		boolean isNullIn = false;
		for (int i = 0; i < costs.length; i++)
		{
			if (costs[i] == 0)
				isNullIn = true;
			if (costs[i] != 0 && costs[i] < min)
			{
				foundIndex = i;
				min = costs[i];
			}
		}
		if (isNullIn)
		{
			if (verbose)
				System.out.println("There are some steps with 0 cost, so no use to go further..");
			return;
		}
		if (foundIndex == -1 || min == Integer.MAX_VALUE) // no cost item
		{
			if (verbose)
				System.out.println("No element in costs found..");
			return; // we have simply no axisrestrictions left (patten matched? not? why?)
		}
		AxisConstraint winner = SearchedConstraints.get(foundIndex); // this is the found restriction!!
		if (verbose)
			System.out.println("The winner:");
		if (winner instanceof TypeConstraint)
		{
			if (verbose)
				System.out.println("Type:" + ((TypeConstraint) winner).getType().toString());
		}
		else if (winner instanceof RelationConstraint)
		{
			if (verbose)
				System.out.println(" Connection:" + ((RelationConstraint) winner).getEdge().getName() + "From:" + ((RelationConstraint) winner).getSource().toString() + "  To:" + ((RelationConstraint) winner).getTarget().toString());
		}
		
		
		
		// we should bind new variables, remove searched and add found constraints
		// select restriction and satisfy it:
		SearchedConstraints.remove(foundIndex);
		FoundConstraints.add(winner);
		
		List<Object[]> winnerList = constraintEnumerator.enumerateConstraint(winner, MatchingVariables);
		
		// loop through winner instances
		if (winner instanceof FindConstraint)
		{
			int findedLen = ((FindConstraint) winner).getAffectedVariables().size(); // the item 'length'
			int minmin = winnerList.size(); // the items found
			for (int i = 0; i < minmin; i++)
			{
				// indexeles:  [ i*findedLen ... (i+1)*findedLen -1 ]
				boolean[] mvadded = new boolean[((FindConstraint) winner).getAffectedVariables().size()];
				int sz = 0;
				// algo: minden nem megkotottet megkotjuk, ennyi
				int affectedSize = 0;
				for (PVariable affVar : ((FindConstraint) winner).getAffectedVariables())
				{
					boolean mvaddedE = true;
					if (!bindToVariable(affVar, winnerList.get(i)[affectedSize], MatchingVariables))
					{
						// bind unsuccessful
						mvaddedE = false;
					}
					mvadded[sz++] = mvaddedE;
					affectedSize++;
				}
				affectedSize = 0;
				
				// match after connection!
				matchPattern(SearchedConstraints, FoundConstraints, currenctCheckable, MatchingVariables);
				// if the recursion returned with a matching, and only one is needed, exit (do not release and call another stuff)!!!
				if (checkExit())
					return;
				// algo: minden nem megkotottet megkotjuk, ennyi
				sz = 0;
				for (PVariable affVar : ((FindConstraint) winner).getAffectedVariables())
				{
					// unBIND (remove, forget, clear, etc.) connection matches (if recently added)
					if (mvadded[sz++])
						bindToVariable(affVar, null, MatchingVariables);
				}
			}
		}
		else if (winner instanceof TypeConstraint)
		{
			for (Object[] winnerElementList : winnerList)
			{
				Object winnerElement = winnerElementList[0];
				if (winnerElementList.length > 1)
				{
					System.err.println("Baj van!");
					throw new AssertionError("Winner list item length should be 1!");
				}
				boolean mvadded = true;
				if (!bindToVariable(((TypeConstraint) winner).getTypedVariable(), winnerElement, MatchingVariables))
				{
					// bind unsuccessful
					mvadded = false;
				}
				
				// match!
				matchPattern(SearchedConstraints, FoundConstraints, currenctCheckable, MatchingVariables);
				// if the recursion returned with a matching, and only one is needed, exit (do not release and call another stuff)!!!
				if (checkExit())
					return;
				
				// undo (forget) the bind operation, thus bind null :)
				if (mvadded)
					bindToVariable(((TypeConstraint) winner).getTypedVariable(), null, MatchingVariables);
			}
		}
		else if (winner instanceof EasyConstraint)
		{
			for (Object[] winnerElementList : winnerList)
			{
				Object winnerElement = winnerElementList[0];
				if (winnerElementList.length > 1)
				{
					System.err.println("Baj van!");
					throw new AssertionError("Winner list item length should be 1!");
				}
				boolean mvadded;
				mvadded = true;
				if (!bindToVariable(((EasyConstraint) winner).getOnlyVariable(), winnerElement, MatchingVariables))
				{
					// bind unsuccessful
					mvadded = false;
				}
				
				// match!
				matchPattern(SearchedConstraints, FoundConstraints, currenctCheckable, MatchingVariables);
				// if the recursion returned with a matching, and only one is needed, exit (do not release and call another stuff)!!!
				if (checkExit())
					return;
				
				// undo (forget) the bind operation, thus bind null :)
				if (mvadded)
					bindToVariable(((EasyConstraint) winner).getOnlyVariable(), null, MatchingVariables);
			}
		}
		else if (winner instanceof RelationConstraint)
		{
			for (int i = 0; i < winnerList.size(); i++)
			{
				Object first = winnerList.get(i)[0];
				Object second = winnerList.get(i)[1];
				if (winnerList.get(i).length != 2)
				{
					System.err.println("Baj van!");
					throw new AssertionError("Winner list item length should be 2!");
				}
				boolean mvSadded, mvTadded;
				mvSadded = mvTadded = true;
				if (!bindToVariable(((RelationConstraint) winner).getSource(), first, MatchingVariables))
				{
					mvSadded = false; // sorry, already bound...
				}
				if (!bindToVariable(((RelationConstraint) winner).getTarget(), second, MatchingVariables))
				{
					mvTadded = false; // same
				}
				
				// match after connection!
				matchPattern(SearchedConstraints, FoundConstraints, currenctCheckable, MatchingVariables);
				// if the recursion returned with a matching, and only one is needed, exit (do not release and call another stuff)!!!
				if (checkExit())
					return;
				
				// undo (remove, forget, clear, etc.) connection matches (if recently added)
				if (mvSadded)
					bindToVariable(((RelationConstraint) winner).getSource(), null, MatchingVariables);
				if (mvTadded)
					bindToVariable(((RelationConstraint) winner).getTarget(), null, MatchingVariables);
			}
		}
		
		
		// we should restore new variables, restore searched and remove found constraints
		
		// nothing found (?), so step one level up in the search tree
		if (verbose)
			System.out.println("Step up!");
		SearchedConstraints.add(winner);
		FoundConstraints.remove(winner);
		return;
	}
	
	/*
	 * Helper methods follow:
	 */
	
	
	// gets all instances of an eclass / edatatype
//	private ArrayList<Object> getAllInstances(ENamedElement type)
//	{
//		ArrayList<Object> ret = new ArrayList<Object>();
//		
//		if (type instanceof EClass)
//		{
//			// ret will be filled with EClasses
//			ret.addAll(this.navigationHelper.getAllInstances((EClass) type));
//		}
//		else if (type instanceof EDataType)
//		{
//			ret.addAll(this.navigationHelper.getDataTypeInstances((EDataType) type));
//		}
//		
//		return ret;
//	}
//	
//	// gets all source-target pairs based on an ereference OR eattribute
//	private ArrayList<Object> getAllSourceTargetPairs(EStructuralFeature fet)
//	{
//		ArrayList<Object> ret = new ArrayList<Object>();
//		
//		Collection<EObject> sources = this.navigationHelper.getHoldersOfFeature(fet);
//		for (EObject obj : sources)
//		{
//			EObject source = (EObject) obj;
//			Object target = source.eGet(fet);
//			boolean fetismany_hehe = fet.isMany();
//			if (fetismany_hehe)
//			{
//				@SuppressWarnings("unchecked")
//				EList<EObject> targets = (EList<EObject>) target;
//				for (EObject actTarget : targets)
//				{
//					ret.add(source);
//					ret.add(actTarget);
//				}
//			}
//			else
//			{
//				ret.add(source);
//				ret.add(target);
//			}
//		}
//		return ret;
//	}

	// checks if the recursion must exit:
	private boolean checkExit()
	{
		if (matchOne == true && oneMatched == true)
			return true;
		return false;
	}
	
	// checks the current pattern if it passes on certain tests (integrity, etc.)
	private boolean checkCurrentPattern(ArrayList<AxisConstraint> FoundConstraints, HashMap<PVariable, Object> MatchingVariables)
	{
		// all connection source-target variables are okay
		// so a connection's source's (and target's) type suits
		// with this check, we can restrict a connection without checking its source and target, 
		// which is time-consuming, but it must be checked sometime during the matching process - here it is:
		/*for (IConstraint rest : FoundConstraints)
		{
			if (rest instanceof RelationConstraint)
			{
				RelationConstraint conCons = (RelationConstraint) rest;
				EObject mSource = (EObject) MatchingVariables.get(conCons.getSource()); // matched source, must be EObject
				Object mTarget = MatchingVariables.get(conCons.getTarget()); // matched target
				EStructuralFeature mRelation = conCons.getEdge();
				
				if (mSource!=null && mTarget!=null)
				{
					// both bound, navigation should work
					Object correct = mSource.eGet(mRelation);
					if (mRelation.isMany())
					{
						@SuppressWarnings("unchecked")
						EList<EObject> corrects = (EList<EObject>) correct;
						if (!corrects.contains(mTarget))
						{
							if (verbose) System.out.print("Wrong relation at all (1)");
							return false;
						}
					}
					else if (correct == null || (correct != null && !correct.equals(mTarget)))
					{
						if (verbose) System.out.print("Wrong relation at all (2)");
						return false;
					}
				}
				
				if (mSource!=null)
				{
					// so it is already bound!
					// if there is a source and the edge is not contained by the source's outcoming relations, ERROR
					if (!mSource.eClass().getEAllStructuralFeatures().contains(mRelation))
					{
						if (verbose) System.out.print("Wrong relation source");
						return false;
					}
				}
				
				if (mTarget!=null)
				{
					// so it is already bound
					// if there is a target bound and this node doesn't have the edge in its incoming relations, ERROR
					/*if (mTarget instanceof EObject && !((EObject)mTarget).eClass().getEAllStructuralFeatures().contains(((EReference)mRelation).getEOpposite()))
					{
						// ha nem EObject, hanem valami egyéb szar, amúgyis bajban vagyunk, ráadásul nem feltétln van EOpposite-ja
						if (verbose) System.out.print("Wrong relation target");
						return false;
						// TODO eopposite okoskodáás
					}
					
					if (mTarget instanceof EObject)
					{
						// if mTarget is EObject, then edge is Reference
						Collection<Setting> attrSources = navigationHelper.getInverseReferences((EObject)mTarget);
						boolean contains = false;
						for (Setting oneSet : attrSources)
						{
							if (oneSet.getEStructuralFeature().equals(mRelation))
								contains = true;
						}
						if (!contains)
						{
							if (verbose) System.out.print("Wrong relation target (1)");
							return false;
						}
					}
					else // mTarget egy OBJECT
					{
						// if mTarget is not an EObject, then edge is Attribute
						Collection<Setting> attrSources = navigationHelper.findByAttributeValue(mTarget);
						boolean contains = false;
						for (Setting oneSet : attrSources)
						{
							if (oneSet.getEStructuralFeature().equals(mRelation))
								contains = true;
						}
						if (!contains)
						{
							if (verbose) System.out.print("Wrong relation target (2)");
							return false;
						}
					}
				}
			}
		}*/
		return true;// pattern passed
	}
	
//	// collects ALL the instances from the model using the available constraints
//	@SuppressWarnings("unchecked")
//	private ArrayList<Object>[] CollectConstraintMatchings(ArrayList<AxisConstraint> SearchedConstraints, HashMap<PVariable, Object> MatchingVariables, /*out*/ int[] costs)
//	{
//		ArrayList<Object>[] listOfSatisfieds = (ArrayList<Object>[]) new ArrayList[SearchedConstraints.size()];
//		for (int i = 0; i < SearchedConstraints.size(); i++)
//		{
//			IConstraint actCons = SearchedConstraints.get(i);
//			if (actCons instanceof EasyConstraint)
//			{
//				costs[i] = 1;
//				ArrayList<Object> temp = new ArrayList<Object>();
//				temp.add(((EasyConstraint) actCons).getKnownValue());
//				listOfSatisfieds[i] = temp;
//			}
//			else if (actCons instanceof FindConstraint)
//			{
//				FindConstraint findCons = (FindConstraint) actCons;
//				List<Object[]> founds = findCons.GetMatchingsFromPartial(MatchingVariables);
//				if (founds == null)
//				{
//					System.err.println("Critical error in match find.");
//					return null; // error somewhere else, too :)
//				}
//				
//				// ki kell tolteni a costs-ot
//				costs[i] = founds.size(); // this much
//				// es ki kell tolteni a listofsatisfieds-et
//				
//				ArrayList<Object> temp = new ArrayList<Object>(); // possible endings
//				for (int fi = 0; fi < founds.size(); fi++)
//				{
//					// indexeles:  [ fi*findedLen ... (fi+1)*findedLen -1 ]
//					
//					// algo: minden nem megkotottet megkotjuk, ennyi
//					int findedLen = findCons.getAffectedVariables().size();
//					for (int j = 0; j < findedLen; j++)
//					{
//						temp.add(founds.get(fi)[j]);
//					}
//				}
//				// minden belepakolva finded*affectedVar.len 2D, de 1D-be teritve
//				listOfSatisfieds[i] = temp;
//			}
//			else if (actCons instanceof TypeConstraint)
//			{
//				TypeConstraint typeCons = (TypeConstraint) actCons;
//				boolean isFoundAndBad = false;
//				// if it is already bound, select 1 or 0, it (the only one) can be WRONG (if it is wrong, it will be 0)
//				// the way it can be a wrong bind: we bound it as a source/target of a relation, and it fails on the '.isInstanceOf(..)' test
//				for (Map.Entry<PVariable, Object> mVar : MatchingVariables.entrySet())
//				{
//					// if it is already bound
//					if (mVar.getValue() != null)
//					{
//						// if this variable equals to the TypeConstraint's variable, which is already bound
//						if (mVar.getKey().getName().equals(typeCons.getTypedVariable().getName()))
//						{
//							if (typeCons.isDatatype())
//							{
//								// this is some stupid object (Integer, Boolean etc.)
//								if (mVar.getValue().getClass().equals(typeCons.getType().getClass()))
//								{
//									costs[i] = 1;
//									ArrayList<Object> temp = new ArrayList<Object>();
//									temp.add(mVar.getValue());
//									listOfSatisfieds[i] = temp;
//								}
//								else
//								{
//									// it is not the searched type but already bound: costs[i] MUST be zero
//									// why? because the restriction is satisfied, the currently investigated variable (mVar.key) is bound, and their value doesn't pass on type test
//									// so they're of a different type and the binding is from wrong type, we have to cut this recursion tree part (vagy mi)
//									isFoundAndBad = true;
//								}
//							}
//							else
//							{
//								if (((EObject) mVar.getValue()).eClass().equals(typeCons.getType()))
//								{
//									// so the (already) matched EObj's metamodel class is equal to type: good
//									costs[i] = 1;
//									ArrayList<Object> temp = new ArrayList<Object>();
//									temp.add(mVar.getValue());
//									listOfSatisfieds[i] = temp;
//								}
//								else isFoundAndBad = true;
//							}
//						}
//					}
//				}
//				
//				//if found and bad
//				if (isFoundAndBad)
//				{
//					costs[i] = 0;
//					listOfSatisfieds[i] = new ArrayList<Object>();
//					// it almost equals to a recursive 'return;'
//				}
//				else if (costs[i] != 1) // if the COSTS[melyikszaml] not equals to 1 (see above)
//				{
//					//else if not 0/1 found in an other way (1)
//					listOfSatisfieds[i] = getAllInstances(typeCons.getType());
//					costs[i] = listOfSatisfieds[i].size();
//				}
//			}
//			else if (actCons instanceof RelationConstraint)
//			{
//				// if rest is connection type, find the relations only touching bound variables (if not, collect all)
//				RelationConstraint conCons = (RelationConstraint) actCons;
//				EObject boundSorcO = (EObject) MatchingVariables.get(conCons.getSource());
//				Object boundTargO = MatchingVariables.get(conCons.getTarget());
//				if (boundSorcO == null && boundTargO == null)
//				{
//					// no source-target bound
//					listOfSatisfieds[i] = getAllSourceTargetPairs(conCons.getEdge());
//					costs[i] = listOfSatisfieds[i].size() / 2;
//				}
//				else if (boundSorcO != null && boundTargO == null)
//				{
//					EObject boundSorc = (EObject) boundSorcO;
//					// source bound: get all outgoind structuralfeature size
//					//costs[i] = boundSorc.getAllRelationFromByType((IRelation) conrest.GetType()).size();
//					EList<EStructuralFeature> tempL = boundSorc.eClass().getEAllStructuralFeatures();
//					
//					int mtch = 0;
//					ArrayList<Object> temp = new ArrayList<Object>(); // possible endings
//					for (EStructuralFeature esf : tempL)
//					{
//						if (esf.equals(conCons.getEdge()) && boundSorc.eGet(esf) instanceof EObject)
//						{
//							// if an outgoing relation type has one other end, remember the "other end"
//							mtch++;
//							temp.add(boundSorc);
//							temp.add((EObject) boundSorc.eGet(esf));
//						}
//						else if (esf.equals(conCons.getEdge()) && boundSorc.eGet(esf) instanceof EList<?>)
//						{
//							// if an outgoing relation type has other ends, remember the "other ends"
//							EList<EObject> listaOf = (EList<EObject>) boundSorc.eGet(esf);
//							for (EObject lot : listaOf)
//							{
//								// there is edge between boundSorc and lot, because EStructuralFeature was multiple-instance: we need all!
//								mtch++;
//								temp.add(boundSorc);
//								temp.add(lot);
//							}
//						}
//						else
//						{
//							if (esf.equals(conCons.getEdge()) && boundSorc.eGet(esf) instanceof Object)
//							{
//								// if an outgoing relation ends in some special wtf
//								mtch++;
//								temp.add(boundSorc);
//								temp.add(boundSorc.eGet(esf));
//							}
//						}
//					}
//					costs[i] = mtch; // items that match from this source
//					listOfSatisfieds[i] = temp;
//				}
//				else if (boundSorcO == null && boundTargO != null && boundTargO instanceof EObject)
//				{
//					// if target is bound, try to navigate backwards
//					Collection<EObject> sources = new ArrayList<EObject>();
//					ArrayList<Object> temp = new ArrayList<Object>(); // possible endings
//					if (conCons.PointsToAttribute() == false)
//					{
//						// eclass-eclass connection
//						sources = this.navigationHelper.getInverseReferences((EObject) boundTargO, (EReference) conCons.getEdge());
//					}
//					else
//					{
//						// eclass-object connection
//						sources = this.navigationHelper.findByAttributeValue(boundTargO, (EAttribute) conCons.getEdge());
//					}
//					int mtch = 0;
//					for (EObject sorc : sources)
//					{
//						if (sorc instanceof EObject)
//						{
//							//it should be instance of EObject
//							mtch++;
//							temp.add((EObject) sorc);
//							temp.add(boundTargO);
//						}
//						else
//						{
//							System.out.println("Breee, noooooo, some target's inverse navigation source is not EObject - then wtf what?");
//						}
//					}
//					costs[i] = mtch; // items that match from this source
//					listOfSatisfieds[i] = temp;
//				}
//				else if (boundSorcO == null && boundTargO != null) // && ! ( boundTargO instanceof EObject)
//				{
//					// source unknown and target is NOT EObject, rather some strange bullshit
//					Collection<EObject> attrOwners = navigationHelper.findByAttributeValue(boundTargO, (EAttribute) conCons.getEdge());
//					
//					ArrayList<Object> temp = new ArrayList<Object>();
//					for (EObject attrSource : attrOwners)
//					{
//						temp.add(attrSource);
//						temp.add(boundTargO);
//					}
//					listOfSatisfieds[i] = temp;
//					costs[i] = listOfSatisfieds[i].size() / 2;
//				}
//				else if (boundSorcO != null && boundTargO != null)
//				{
//					if (conCons.PointsToAttribute() == false)
//					{
//						// source & target bound
//						EObject boundSorc = (EObject) boundSorcO;
//						EObject boundTarg = (EObject) boundTargO;
//						if (!conCons.getEdge().isMany())
//						{
//							if (boundSorc.eGet(conCons.getEdge()) instanceof EObject)
//							{
//								// is there eobj at the end?
//								EObject targetOfSource = (EObject) boundSorc.eGet(conCons.getEdge());
//								if (targetOfSource.equals(boundTarg))
//								{
//									costs[i] = 1;
//									ArrayList<Object> temp = new ArrayList<Object>();
//									temp.add(boundSorc);
//									temp.add(boundTarg);
//									listOfSatisfieds[i] = temp;
//								}
//								else
//								{
//									// the bound source did not lead to the bound target, wrong relation!
//									costs[i] = 0;
//									listOfSatisfieds[i] = new ArrayList<Object>();
//									System.out.println("Erdekes1");
//								}
//							}
//							else
//							{
//								// the bound source did not lead to the bound target, wrong relation!
//								// this is because not even EObject type is there
//								// ooooor feature is deleted!!! TODO
//								costs[i] = 0;
//								listOfSatisfieds[i] = new ArrayList<Object>();
//								System.out.println("Erdekes2");
//							}
//						}
//						else
//						{
//							// list of target objects along this edge
//							List<Object> targetObjects = (List<Object>) boundSorc.eGet(conCons.getEdge());
//							if (targetObjects.contains(boundTarg))
//							{
//								ArrayList<Object> temp = new ArrayList<Object>();
//								temp.add(boundSorc);
//								temp.add(boundTarg);
//								costs[i] = 1; // the edge is okay, this is a match with cost 1
//								listOfSatisfieds[i] = temp;
//							}
//							else
//							{
//								costs[i] = 0; // no edges
//								listOfSatisfieds[i] = new ArrayList<Object>();
//							}
//						}
//					}
//					else
//					// conCons points to attribute
//					{
//						EObject boundSorc = (EObject) boundSorcO;
//						// boundTarg is boundTargO
//						Collection<EObject> attrOwners = navigationHelper.findByAttributeValue(boundTargO, (EAttribute) conCons.getEdge());
//						if (!attrOwners.contains(boundSorc))
//						{
//							// no source that contains this target
//							costs[i] = 0;
//							listOfSatisfieds[i] = new ArrayList<Object>();
//						}
//						else
//						{
//							ArrayList<Object> temp = new ArrayList<Object>();
//							temp.add(boundSorc);
//							temp.add(boundTargO);
//							costs[i] = 1;
//							listOfSatisfieds[i] = temp;
//						}
//					}
//				}
//				else
//				{
//					// will it ever pass to here?
//					// no source-target bound
//					listOfSatisfieds[i] = getAllSourceTargetPairs(conCons.getEdge());
//					costs[i] = listOfSatisfieds[i].size() / 2;
//				}
//			}
//		}
//		return listOfSatisfieds;
//	}
	
	// bind interface
	// returns true if bind is successful (alters trueMatchingVariables)
	// returns false if already bound (does not alter trueMatchingVariables)
	public boolean bindToVariable(PVariable key, Object value, HashMap<PVariable, Object> trueMatchingVariables)
	{
		PVariable keyRoot = key.getUnifiedIntoRoot();
		if (trueMatchingVariables.get(keyRoot) == null && value == null)
			return false;
		else if (trueMatchingVariables.get(keyRoot) != null && value != null && !trueMatchingVariables.get(keyRoot).equals(value))
			return false; // already bound to something else
		else if (trueMatchingVariables.get(keyRoot) != null && value != null && trueMatchingVariables.get(keyRoot).equals(value))
			return false;
		// now bind!
		bindAndBindToChildren(keyRoot, value, trueMatchingVariables);
		return true; // release
	}
	
	// depth search and bind
	private void bindAndBindToChildren(PVariable key, Object value, HashMap<PVariable, Object> trueMatchingVariables)
	{
		trueMatchingVariables.put(key, value);
		// egy kis butasag (gyereket bindolni? what?): ne is hagyjuk benne...
		//bindAndBindToChildren(key.getDirectUnifiedInto(), value, trueMatchingVariables);
		
//		for (PVariable child : CHILDREN!!!)
//		{
//			bindAndBindToChildren(child, value, trueMatchingVariables);
//		}
	}
	
	/* BINDINGS*/
	// binds the "key" variable to the "value" IME, and also binds to the key's equal variables (ant their, and their...)
	// also works (I hope) with 'null' value, it will bind all the equal variables to null
	// returns with the old value (if null, it returns null)
	/*private Object bindToVariableInner(LookVariable key, Object value, HashMap<LookVariable, Object> trueMatchingVariables)
	{
		// if value is null, we want to bind the equal variables to null, too...
		if ((value == null && trueMatchingVariables.get(key) == null))
			return value; // we want to remove (replace with null), but we have to cut the recursion
		if (value != null && trueMatchingVariables.get(key) != null && trueMatchingVariables.get(key).equals(value))
			return value; // we want to bind, but it's already bound, so we have to cut the recursion
		// bind it! (eg.: 
		//					if value==null and oldvalue is not: we want to unbind (remove))
		//					if oldvalue==null and new value is not: we want to bind (replace))
		Object old = trueMatchingVariables.put(key, value); 
		if (actualPatternDefinition.EqualVariables.get(key)!=null)
		{
			for(LookVariable lV : actualPatternDefinition.EqualVariables.get(key))
			{
				bindToVariableInner(lV, value, trueMatchingVariables); // call the equals recursion!
			}
		}
		return old;
	}
	
	// the interface to use
	private boolean bindToVariable(LookVariable key, Object value, HashMap<LookVariable, Object> trueMatchingVariables)
	{
		Object old = bindToVariableInner(key, value, trueMatchingVariables);
		if (old != null && value != null)
		{
			bindToVariableInner(key, old, trueMatchingVariables); // restore if not the 'null' is the goal
			return false;
		}
		else
		{
			return true; // success
		}
	}*/
}
