package hu.bme.mit.inf.lookaheadmatcher.impl;

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
import org.eclipse.incquery.runtime.base.api.NavigationHelper;
import org.eclipse.incquery.runtime.matchers.psystem.PVariable;

import hu.bme.mit.inf.lookaheadmatcher.IConstraintEnumerator;

public class SimpleConstraintEnumerator implements IConstraintEnumerator {

	private NavigationHelper navigationHelper;

	int cost = -1;
	
	ArrayList<Object> possibleRetVal = null;
	
	@Override
	public int getCost(AxisConstraint constraint, HashMap<PVariable, Object> matchingVariables)
	{
		possibleRetVal = null;
		// lama!!! bena java!!!! se out parameter, se ugya mugy parameter... gagyii
		possibleRetVal = enumerateConstraint(constraint, matchingVariables);
		return cost;
	}

	@Override
	public ArrayList<Object> enumerateConstraint(AxisConstraint constraint, HashMap<PVariable, Object> matchingVariables)
	{
		if (possibleRetVal != null)
		{
			@SuppressWarnings("unchecked")
			ArrayList<Object> ret = (ArrayList<Object>)possibleRetVal.clone();
			possibleRetVal = null;
			return ret; // bena cacheeles
		}
		
		ArrayList<Object> returnList = new ArrayList<Object>();

		if (constraint instanceof EasyConstraint)
		{
			cost = 1;
			ArrayList<Object> temp = new ArrayList<Object>();
			temp.add(((EasyConstraint) constraint).getKnownValue());
			returnList = temp;
		}
		else if (constraint instanceof FindConstraint)
		{
			FindConstraint findCons = (FindConstraint) constraint;
			List<Object[]> founds = findCons.GetMatchingsFromPartial(matchingVariables);
			if (founds == null)
			{
				System.err.println("Critical error in match find.");
				throw new AssertionError("Error in find constraint!");
			}
			
			// ki kell tolteni a costs-ot
			cost = founds.size(); // this much
			// es ki kell tolteni a listofsatisfieds-et
			
			ArrayList<Object> temp = new ArrayList<Object>(); // possible endings
			for (int fi = 0; fi < founds.size(); fi++)
			{
				// indexeles:  [ fi*findedLen ... (fi+1)*findedLen -1 ]
				
				// algo: minden nem megkotottet megkotjuk, ennyi
				int findedLen = findCons.getAffectedVariables().size();
				for (int j = 0; j < findedLen; j++)
				{
					temp.add(founds.get(fi)[j]);
				}
			}
			// minden belepakolva finded*affectedVar.len 2D, de 1D-be teritve
			returnList = temp;
		}
		else if (constraint instanceof TypeConstraint)
		{
			TypeConstraint typeCons = (TypeConstraint) constraint;
			boolean isFoundAndBad = false;
			// if it is already bound, select 1 or 0, it (the only one) can be WRONG (if it is wrong, it will be 0)
			// the way it can be a wrong bind: we bound it as a source/target of a relation, and it fails on the '.isInstanceOf(..)' test
			for (Map.Entry<PVariable, Object> mVar : matchingVariables.entrySet())
			{
				// if it is already bound
				if (mVar.getValue() != null)
				{
					// if this variable equals to the TypeConstraint's variable, which is already bound
					if (mVar.getKey().getName().equals(typeCons.getTypedVariable().getName()))
					{
						if (typeCons.isDatatype())
						{
							// this is some stupid object (Integer, Boolean etc.)
							if (mVar.getValue().getClass().equals(typeCons.getType().getClass()))
							{
								cost = 1;
								ArrayList<Object> temp = new ArrayList<Object>();
								temp.add(mVar.getValue());
								returnList = temp;
							}
							else
							{
								// it is not the searched type but already bound: costs[i] MUST be zero
								// why? because the restriction is satisfied, the currently investigated variable (mVar.key) is bound, and their value doesn't pass on type test
								// so they're of a different type and the binding is from wrong type, we have to cut this recursion tree part (vagy mi)
								isFoundAndBad = true;
							}
						}
						else
						{
							if (((EObject) mVar.getValue()).eClass().equals(typeCons.getType()))
							{
								// so the (already) matched EObj's metamodel class is equal to type: good
								cost = 1;
								ArrayList<Object> temp = new ArrayList<Object>();
								temp.add(mVar.getValue());
								returnList = temp;
							}
							else isFoundAndBad = true;
						}
					}
				}
			}
			
			//if found and bad
			if (isFoundAndBad)
			{
				cost = 0;
				returnList = new ArrayList<Object>();
				// it almost equals to a recursive 'return;'
			}
			else if (cost != 1) // if the COSTS[melyikszaml] not equals to 1 (see above)
			{
				//else if not 0/1 found in an other way (1)
				returnList = getAllInstances(typeCons.getType());
				cost = returnList.size();
			}
		}
		else if (constraint instanceof RelationConstraint)
		{
			// if rest is connection type, find the relations only touching bound variables (if not, collect all)
			RelationConstraint conCons = (RelationConstraint) constraint;
			EObject boundSorcO = (EObject) matchingVariables.get(conCons.getSource());
			Object boundTargO = matchingVariables.get(conCons.getTarget());
			if (boundSorcO == null && boundTargO == null)
			{
				// no source-target bound
				returnList = getAllSourceTargetPairs(conCons.getEdge());
				cost = returnList.size() / 2;
			}
			else if (boundSorcO != null && boundTargO == null)
			{
				EObject boundSorc = (EObject) boundSorcO;
				// source bound: get all outgoind structuralfeature size
				//costs[i] = boundSorc.getAllRelationFromByType((IRelation) conrest.GetType()).size();
				EList<EStructuralFeature> tempL = boundSorc.eClass().getEAllStructuralFeatures();
				
				int mtch = 0;
				ArrayList<Object> temp = new ArrayList<Object>(); // possible endings
				for (EStructuralFeature esf : tempL)
				{
					if (esf.equals(conCons.getEdge()) && boundSorc.eGet(esf) instanceof EObject)
					{
						// if an outgoing relation type has one other end, remember the "other end"
						mtch++;
						temp.add(boundSorc);
						temp.add((EObject) boundSorc.eGet(esf));
					}
					else if (esf.equals(conCons.getEdge()) && boundSorc.eGet(esf) instanceof EList<?>)
					{
						// if an outgoing relation type has other ends, remember the "other ends"
						@SuppressWarnings("unchecked")
						EList<EObject> listaOf = (EList<EObject>) boundSorc.eGet(esf);
						for (EObject lot : listaOf)
						{
							// there is edge between boundSorc and lot, because EStructuralFeature was multiple-instance: we need all!
							mtch++;
							temp.add(boundSorc);
							temp.add(lot);
						}
					}
					else
					{
						if (esf.equals(conCons.getEdge()) && boundSorc.eGet(esf) instanceof Object)
						{
							// if an outgoing relation ends in some special wtf
							mtch++;
							temp.add(boundSorc);
							temp.add(boundSorc.eGet(esf));
						}
					}
				}
				cost = mtch; // items that match from this source
				returnList = temp;
			}
			else if (boundSorcO == null && boundTargO != null && boundTargO instanceof EObject)
			{
				// if target is bound, try to navigate backwards
				Collection<EObject> sources = new ArrayList<EObject>();
				ArrayList<Object> temp = new ArrayList<Object>(); // possible endings
				if (conCons.PointsToAttribute() == false)
				{
					// eclass-eclass connection
					sources = this.navigationHelper.getInverseReferences((EObject) boundTargO, (EReference) conCons.getEdge());
				}
				else
				{
					// eclass-object connection
					sources = this.navigationHelper.findByAttributeValue(boundTargO, (EAttribute) conCons.getEdge());
				}
				int mtch = 0;
				for (EObject sorc : sources)
				{
					if (sorc instanceof EObject)
					{
						//it should be instance of EObject
						mtch++;
						temp.add((EObject) sorc);
						temp.add(boundTargO);
					}
					else
					{
						System.out.println("Breee, noooooo, some target's inverse navigation source is not EObject - then wtf what?");
					}
				}
				cost = mtch; // items that match from this source
				returnList = temp;
			}
			else if (boundSorcO == null && boundTargO != null) // && ! ( boundTargO instanceof EObject)
			{
				// source unknown and target is NOT EObject, rather some strange bullshit
				Collection<EObject> attrOwners = this.navigationHelper.findByAttributeValue(boundTargO, (EAttribute) conCons.getEdge());
				
				ArrayList<Object> temp = new ArrayList<Object>();
				for (EObject attrSource : attrOwners)
				{
					temp.add(attrSource);
					temp.add(boundTargO);
				}
				returnList = temp;
				cost = returnList.size() / 2;
			}
			else if (boundSorcO != null && boundTargO != null)
			{
				if (conCons.PointsToAttribute() == false)
				{
					// source & target bound
					EObject boundSorc = (EObject) boundSorcO;
					EObject boundTarg = (EObject) boundTargO;
					if (!conCons.getEdge().isMany())
					{
						if (boundSorc.eGet(conCons.getEdge()) instanceof EObject)
						{
							// is there eobj at the end?
							EObject targetOfSource = (EObject) boundSorc.eGet(conCons.getEdge());
							if (targetOfSource.equals(boundTarg))
							{
								cost = 1;
								ArrayList<Object> temp = new ArrayList<Object>();
								temp.add(boundSorc);
								temp.add(boundTarg);
								returnList = temp;
							}
							else
							{
								// the bound source did not lead to the bound target, wrong relation!
								cost = 0;
								returnList = new ArrayList<Object>();
								System.out.println("Erdekes1");
							}
						}
						else
						{
							// the bound source did not lead to the bound target, wrong relation!
							// this is because not even EObject type is there
							// ooooor feature is deleted!!! TODO
							cost = 0;
							returnList = new ArrayList<Object>();
							System.out.println("Erdekes2");
						}
					}
					else
					{
						// list of target objects along this edge
						@SuppressWarnings("unchecked")
						List<Object> targetObjects = (List<Object>) boundSorc.eGet(conCons.getEdge());
						if (targetObjects.contains(boundTarg))
						{
							ArrayList<Object> temp = new ArrayList<Object>();
							temp.add(boundSorc);
							temp.add(boundTarg);
							cost = 1; // the edge is okay, this is a match with cost 1
							returnList = temp;
						}
						else
						{
							cost = 0; // no edges
							returnList = new ArrayList<Object>();
						}
					}
				}
				else
				// conCons points to attribute
				{
					EObject boundSorc = (EObject) boundSorcO;
					// boundTarg is boundTargO
					Collection<EObject> attrOwners = this.navigationHelper.findByAttributeValue(boundTargO, (EAttribute) conCons.getEdge());
					if (!attrOwners.contains(boundSorc))
					{
						// no source that contains this target
						cost = 0;
						returnList = new ArrayList<Object>();
					}
					else
					{
						ArrayList<Object> temp = new ArrayList<Object>();
						temp.add(boundSorc);
						temp.add(boundTargO);
						cost = 1;
						returnList = temp;
					}
				}
			}
			else
			{
				// will it ever pass to here?
				// no source-target bound
				returnList = getAllSourceTargetPairs(conCons.getEdge());
				cost = returnList.size() / 2;
			}
		}
		return returnList;
	}

	public SimpleConstraintEnumerator(NavigationHelper navHelper)
	{
		this.navigationHelper = navHelper;
	}
	

	// gets all instances of an eclass / edatatype
	private ArrayList<Object> getAllInstances(ENamedElement type)
	{
		ArrayList<Object> ret = new ArrayList<Object>();
		
		if (type instanceof EClass)
		{
			// ret will be filled with EClasses
			ret.addAll(this.navigationHelper.getAllInstances((EClass) type));
		}
		else if (type instanceof EDataType)
		{
			ret.addAll(this.navigationHelper.getDataTypeInstances((EDataType) type));
		}
		
		return ret;
	}
	
	// gets all source-target pairs based on an ereference OR eattribute
	private ArrayList<Object> getAllSourceTargetPairs(EStructuralFeature fet)
	{
		ArrayList<Object> ret = new ArrayList<Object>();
		
		Collection<EObject> sources = this.navigationHelper.getHoldersOfFeature(fet);
		for (EObject obj : sources)
		{
			EObject source = (EObject) obj;
			Object target = source.eGet(fet);
			boolean fetismany_hehe = fet.isMany();
			if (fetismany_hehe)
			{
				@SuppressWarnings("unchecked")
				EList<EObject> targets = (EList<EObject>) target;
				for (EObject actTarget : targets)
				{
					ret.add(source);
					ret.add(actTarget);
				}
			}
			else
			{
				ret.add(source);
				ret.add(target);
			}
		}
		return ret;
	}

	public int ejnyeGetCost()
	{
		return this.cost;
	}
}
