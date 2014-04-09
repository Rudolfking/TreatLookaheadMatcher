package hu.bme.mit.inf.lookaheadmatcher.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
	
	ArrayList<Object[]> possibleRetVal = null;
	
	/**
	 * Should return the cost for the given constraint and partial matching. This might be faster than enumerating it.
	 * Hardly relies on 'enumerateConstraint(..)', almost code duplicate (without that much memory footprint)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public int getCost(AxisConstraint constraint, HashMap<PVariable, Object> matchingVariables)
	{
		if (constraint instanceof EasyConstraint)
		{
			// if that "easy one" is not the desired, it is a bad matching, cost should be 0
			if (matchingVariables.get(((EasyConstraint) constraint).getOnlyVariable()) == null)
				return 1; // not bound, easy can offer 1
			if (matchingVariables.get(((EasyConstraint) constraint).getOnlyVariable()).equals(((EasyConstraint) constraint).getKnownValue()) == false)
				return 0; // bound and BAD
			// else correct bind
			return 1;
		}
		else if (constraint instanceof FindConstraint)
		{
			FindConstraint findCons = (FindConstraint) constraint;
			int founds = findCons.GetMatchCountFromPartial(matchingVariables);
			if (founds == -1)
			{
				System.err.println("Critical error in match find.");
				throw new AssertionError("Error in find constraint!");
			}
			
			// ki kell tolteni a costs-ot
			return founds; // this much
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
								return 1;
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
								return 1;
							}
							else isFoundAndBad = true;
						}
					}
				}
			}
			
			//if found and bad
			if (isFoundAndBad)
			{
				return 0;
			}
			else if (cost != 1) // if the COSTS[melyikszaml] not equals to 1 (see above)
			{
				//else if not 0/1 found in an other way (1)
				return getAllInstanceSize(typeCons.getType());
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
				return getAllSourceTargetPairSize(conCons.getEdge());
			}
			else if (boundSorcO != null && boundTargO == null)
			{
				EObject boundSorc = (EObject) boundSorcO;
				// source bound: get all outgoind structuralfeature size
				//costs[i] = boundSorc.getAllRelationFromByType((IRelation) conrest.GetType()).size();
				EList<EStructuralFeature> tempL = boundSorc.eClass().getEAllStructuralFeatures();
				
				int mtch = 0;
				for (EStructuralFeature esf : tempL)
				{
					if (esf.equals(conCons.getEdge()) && boundSorc.eGet(esf) instanceof EObject)
					{
						// if an outgoing relation type has one other end, remember the "other end"
						mtch++;
					}
					else if (esf.equals(conCons.getEdge()) && boundSorc.eGet(esf) instanceof EList<?>)
					{
						// if an outgoing relation type has other ends, remember the "other ends"
						mtch += ((EList<EObject>) boundSorc.eGet(esf)).size();
					}
					else
					{
						if (esf.equals(conCons.getEdge()) && boundSorc.eGet(esf) instanceof Object)
						{
							// if an outgoing relation ends in some special wtf
							mtch++;
						}
					}
				}
				return mtch;
			}
			else if (boundSorcO == null && boundTargO != null && boundTargO instanceof EObject)
			{
				// if target is bound, try to navigate backwards
				if (conCons.PointsToAttribute() == false)
				{
					// eclass-eclass connection
					return (this.navigationHelper.getInverseReferences((EObject) boundTargO, (EReference) conCons.getEdge())).size();
				}
				else
				{
					// eclass-object connection
					return (this.navigationHelper.findByAttributeValue(boundTargO, (EAttribute) conCons.getEdge())).size();
				}
			}
			else if (boundSorcO == null && boundTargO != null) // && ! ( boundTargO instanceof EObject)
			{
				// source unknown and target is NOT EObject, rather some strange bullshit
				return (this.navigationHelper.findByAttributeValue(boundTargO, (EAttribute) conCons.getEdge())).size();
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
							if (((EObject) boundSorc.eGet(conCons.getEdge())).equals(boundTarg))
							{
								return 1;
							}
							else
							{
								// the bound source did not lead to the bound target, wrong relation!
								// throw new AssertionError("Cost might be 0, 'erdekes'");
								return 0;
							}
						}
						else
						{
							// the bound source did not lead to the bound target, wrong relation!
							// this is because not even EObject type is there
							return 0;
						}
					}
					else
					{
						// list of target objects along this edge
						if (((List<Object>) boundSorc.eGet(conCons.getEdge())).contains(boundTarg))
						{
							return 1; // the edge is okay, this is a match with cost 1
						}
						else
						{
							return 0; // no edges
						}
					}
				}
				else
				// conCons points to attribute
				{
					EObject boundSorc = (EObject) boundSorcO;
					// boundTarg is boundTargO
					if (!(this.navigationHelper.findByAttributeValue(boundTargO, (EAttribute) conCons.getEdge())).contains(boundSorc))
					{
						// no source that contains this target
						return 0;
					}
					else
					{
						return 1;
					}
				}
			}
			else
			{
				// will it ever pass to here?
				// no source-target bound
				return getAllSourceTargetPairSize(conCons.getEdge());
			}
		}
		return 0;
		
		
		
		
		
//		// buta megoldas:
//		
//		possibleRetVal = null;
//		// lama!!! bena java!!!! se out parameter, se ugya mugy parameter... gagyii
//		possibleRetVal = enumerateConstraint(constraint, matchingVariables);
//		return cost;
	}

	@Override
	public List<Object[]> enumerateConstraint(AxisConstraint constraint, HashMap<PVariable, Object> matchingVariables)
	{
		if (possibleRetVal != null)
		{
			@SuppressWarnings("unchecked")
			List<Object[]> ret = (List<Object[]>)possibleRetVal.clone();
			possibleRetVal = null;
			return ret; // bena cacheeles
		}
		
		List<Object[]> returnList = new ArrayList<Object[]>();

		if (constraint instanceof EasyConstraint)
		{
			// if that "easy one" is not the desired, it is a bad matching, cost should be 0
			if (matchingVariables.get(((EasyConstraint) constraint).getOnlyVariable()) != null &&
				matchingVariables.get(((EasyConstraint) constraint).getOnlyVariable()).equals(((EasyConstraint) constraint).getKnownValue()) == false)
			{
				ArrayList<Object[]> temp = new ArrayList<Object[]>();
				cost = 0;
				return temp;
			}
			// null or bound and okay:
			cost = 1;
			ArrayList<Object[]> temp = new ArrayList<Object[]>();
			temp.add(new Object[]{((EasyConstraint) constraint).getKnownValue()});
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
			returnList = founds;
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
								ArrayList<Object[]> temp = new ArrayList<Object[]>();
								temp.add(new Object[]{mVar.getValue()});
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
								ArrayList<Object[]> temp = new ArrayList<Object[]>();
								temp.add(new Object[]{mVar.getValue()});
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
				returnList = new ArrayList<Object[]>();
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
				cost = returnList.size();
			}
			else if (boundSorcO != null && boundTargO == null)
			{
				EObject boundSorc = (EObject) boundSorcO;
				// source bound: get all outgoind structuralfeature size
				//costs[i] = boundSorc.getAllRelationFromByType((IRelation) conrest.GetType()).size();
				EList<EStructuralFeature> tempL = boundSorc.eClass().getEAllStructuralFeatures();
				
				int mtch = 0;
				ArrayList<Object[]> temp = new ArrayList<Object[]>(); // possible endings
				for (EStructuralFeature esf : tempL)
				{
					if (esf.equals(conCons.getEdge()) && boundSorc.eGet(esf) instanceof EObject)
					{
						// if an outgoing relation type has one other end, remember the "other end"
						mtch++;
						Object[] inTmp = new Object[2];
						inTmp[0]=boundSorc;
						inTmp[1]=(EObject) boundSorc.eGet(esf);
						temp.add(inTmp);
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
							Object[] inTmp = new Object[2];
							inTmp[0]=boundSorc;
							inTmp[1]=lot;
							temp.add(inTmp);
						}
					}
					else
					{
						if (esf.equals(conCons.getEdge()) && boundSorc.eGet(esf) instanceof Object)
						{
							// if an outgoing relation ends in some special wtf
							mtch++;
							Object[] inTmp = new Object[2];
							inTmp[0]=boundSorc;
							inTmp[1]=boundSorc.eGet(esf);
							temp.add(inTmp);
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
				ArrayList<Object[]> temp = new ArrayList<Object[]>(); // possible endings
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
						Object[] inTmp = new Object[2];
						inTmp[0]=(EObject) sorc;
						inTmp[1]=boundTargO;
						temp.add(inTmp);
					}
					else
					{
						System.out.println("Breee, noooooo, some target's inverse navigation source is not EObject - then wtf what?");
						throw new AssertionError("Inverse navigate does not target an instance of EObject. What?");
					}
				}
				cost = mtch; // items that match from this source
				returnList = temp;
			}
			else if (boundSorcO == null && boundTargO != null) // && ! ( boundTargO instanceof EObject)
			{
				// source unknown and target is NOT EObject, rather some strange bullshit
				Collection<EObject> attrOwners = this.navigationHelper.findByAttributeValue(boundTargO, (EAttribute) conCons.getEdge());

				ArrayList<Object[]> temp = new ArrayList<Object[]>();
				for (EObject attrSource : attrOwners)
				{
					Object[] inTmp = new Object[2];
					inTmp[0]=attrSource;
					inTmp[1]=boundTargO;
					temp.add(inTmp);
				}
				returnList = temp;
				cost = returnList.size();
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
								ArrayList<Object[]> temp = new ArrayList<Object[]>();
								temp.add(new Object[2]);
								temp.get(0)[0]=boundSorc;
								temp.get(0)[1]=boundTarg;
								returnList = temp;
							}
							else
							{
								// the bound source did not lead to the bound target, wrong relation!
								cost = 0;
								returnList = new ArrayList<Object[]>();
								System.out.println("Erdekes1");
							}
						}
						else
						{
							// the bound source did not lead to the bound target, wrong relation!
							// this is because not even EObject type is there
							cost = 0;
							returnList = new ArrayList<Object[]>();
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
							ArrayList<Object[]> temp = new ArrayList<Object[]>();
							temp.add(new Object[2]);
							temp.get(0)[0]=boundSorc;
							temp.get(0)[1]=boundTarg;
							cost = 1; // the edge is okay, this is a match with cost 1
							returnList = temp;
						}
						else
						{
							cost = 0; // no edges
							returnList = new ArrayList<Object[]>();
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
						returnList = new ArrayList<Object[]>();
					}
					else
					{
						ArrayList<Object[]> temp = new ArrayList<Object[]>();
						temp.add(new Object[2]);
						temp.get(0)[0]=boundSorc;
						temp.get(0)[1]=boundTargO;
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
				cost = returnList.size();
			}
		}
		return returnList;
	}

	public SimpleConstraintEnumerator(NavigationHelper navHelper)
	{
		this.navigationHelper = navHelper;
	}
	

	// gets all instances of an eclass / edatatype
	private List<Object[]> getAllInstances(ENamedElement type)
	{
		List<Object[]> ret = new ArrayList<Object[]>();
		
		if (type instanceof EClass)
		{
			// ret will be filled with EClasses
			Set<EObject> allIn = this.navigationHelper.getAllInstances((EClass) type);
			for (Object ins : allIn)
			{
				ret.add(new Object[]{ins});
			}
		}
		else if (type instanceof EDataType)
		{
			Set<Object> allIn = this.navigationHelper.getDataTypeInstances((EDataType) type);
			for (Object ins : allIn)
			{
				ret.add(new Object[]{ins});
			}
		}
		
		return ret;
	}
	
	private int getAllInstanceSize(ENamedElement type)
	{
		if (type instanceof EClass)
		{
			// ret will be filled with EClasses
			return this.navigationHelper.getAllInstances((EClass) type).size();
		}
		else if (type instanceof EDataType)
		{
			return this.navigationHelper.getDataTypeInstances((EDataType) type).size();
		}
		else return 0;
	}
	
	// gets all source-target pairs based on an ereference OR eattribute
	private List<Object[]> getAllSourceTargetPairs(EStructuralFeature fet)
	{
		List<Object[]> ret = new ArrayList<Object[]>();
		
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
					Object[] objR = new Object[2];
					objR[0] = source;
					objR[1] = actTarget;
					ret.add(objR);
				}
			}
			else
			{
				Object[] objR = new Object[2];
				objR[0] = source;
				objR[1] = target;
				ret.add(objR);
			}
		}
		return ret;
	}
	
	@SuppressWarnings("unchecked")
	private int getAllSourceTargetPairSize(EStructuralFeature fet)
	{
		int ret = 0;
		Collection<EObject> sources = this.navigationHelper.getHoldersOfFeature(fet);
		for (EObject obj : sources)
		{
			EObject source = (EObject) obj;
			Object target = source.eGet(fet);
			boolean fetismany_hehe = fet.isMany();
			if (fetismany_hehe)
			{
				ret += ((EList<EObject>) target).size();
			}
			else
			{
				ret++;
			}
		}
		return ret;
	}

	public int ejnyeGetCost()
	{
		return this.cost;
	}
}
