package hu.bme.mit.inf.treatengine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.eclipse.incquery.runtime.base.api.NavigationHelper;
import org.eclipse.incquery.runtime.matchers.psystem.PVariable;

import hu.bme.mit.inf.lookaheadmatcher.IConstraintEnumerator;
import hu.bme.mit.inf.lookaheadmatcher.IDelta;
import hu.bme.mit.inf.lookaheadmatcher.impl.AxisConstraint;
import hu.bme.mit.inf.lookaheadmatcher.impl.FindConstraint;
import hu.bme.mit.inf.lookaheadmatcher.impl.LookaheadMatching;
import hu.bme.mit.inf.lookaheadmatcher.impl.EasyConstraint;
import hu.bme.mit.inf.lookaheadmatcher.impl.RelationConstraint;
import hu.bme.mit.inf.lookaheadmatcher.impl.SimpleConstraintEnumerator;
import hu.bme.mit.inf.lookaheadmatcher.impl.TypeConstraint;

public class TreatConstraintEnumerator implements IConstraintEnumerator
{
	// a simple inner searcher used by easy mode
	SimpleConstraintEnumerator simpleInner;
	
	public TreatConstraintEnumerator(NavigationHelper navHelper)
	{
		simpleInner = new SimpleConstraintEnumerator(navHelper);
	}

	@Override
	public int getCost(AxisConstraint constraint, HashMap<PVariable, Object> matchingVariables)
	{
		// should use deltas!! TODO big TODO
		if (!constraint.hasMailboxContent())
			return simpleInner.getCost(constraint, matchingVariables);
		else
		{
			if (constraint instanceof EasyConstraint)
			{
				return simpleInner.getCost(constraint, matchingVariables);
				// fuck easy constraint
			}
			else if (constraint instanceof RelationConstraint)
			{
				// should get delta of caller!
				List<IDelta> d = constraint.getMailboxContent();
				List<Object[]> newMatchMagic = simpleInner.enumerateConstraint(constraint, matchingVariables);
				int newMatchCount = newMatchMagic.size();
				for (IDelta del : d)
				{
					ModelChange mchan = (ModelChange)del;
					if (mchan instanceof EFeatureChange)
					{
						for (Object[] change : newMatchMagic)
						{
							if (change[0].equals(((EFeatureChange) mchan).getHost()) && change[1].equals(((EFeatureChange) mchan).getInstance()))
							{
								// this relation is found but in delta too: mirror!
								// "rollback" this change
								if (mchan.isAddition())
									newMatchCount--;
								else
									newMatchCount++;
							}
						}
					}
				}
				return newMatchCount;
			}
			else if (constraint instanceof TypeConstraint)
			{
				// should get delta of caller!
				List<IDelta> d = constraint.getMailboxContent();
				List<Object[]> newMatchMagic = simpleInner.enumerateConstraint(constraint, matchingVariables);
				int newMatchCount = newMatchMagic.size();
				for (IDelta del : d)
				{
					ModelChange mchan = (ModelChange)del;
					if (mchan instanceof EClassChange)
					{
						for (Object[] change : newMatchMagic)
						{
							// no type check needed
							if (change[0].equals(((EClassChange) mchan).getInstance()))
							{
								// this relation is found but in delta too: mirror!
								// "rollback" this change
								if (mchan.isAddition())
									newMatchCount--;
								else
									newMatchCount++;
							}
						}
					}
					else if (mchan instanceof EDataTypeChange)
					{
						for (Object[] change : newMatchMagic)
						{
							// no type check needed
							if (change[0].equals(((EDataTypeChange) mchan).getInstance()))
							{
								// this relation is found but in delta too: mirror!
								// "rollback" this change
								if (mchan.isAddition())
									newMatchCount--;
								else
									newMatchCount++;
							}
						}
					}
				}
				return newMatchCount;
			}
			else if (constraint instanceof FindConstraint)
			{
				// get and return: this will filter by content
				try
				{
					// should get delta of caller!
					List<IDelta> d = constraint.getMailboxContent();
					int newMatchCount = LookaheadMatcherTreat.GodSet.get(((FindConstraint)constraint).getInnerFindCall().getReferredQuery()).size();
					for (IDelta del : d)
					{
						Delta delta = (Delta)del;
						for (Entry<LookaheadMatching, Boolean> change : delta.getChangeset().entrySet())
						{
							// "rollback" this change
							if (change.getValue())
								newMatchCount--;
							else
								newMatchCount++;
						}
					}
					return newMatchCount;
				}
				catch (Exception e)
				{
					// fallback:
					return enumerateConstraint(constraint, matchingVariables).size();
				}
			}
			else
				return -1;
		}
		// return 0; // something went bad
	}

	@Override
	public List<Object[]> enumerateConstraint(AxisConstraint constraint, HashMap<PVariable, Object> matchingVariables)
	{
		// tricking view ("rollback delta" for one time) view!!
		
		List<Object[]> candidates = simpleInner.enumerateConstraint(constraint, matchingVariables);

		List<Object[]> ret = null;
		
		// filter ret... (this can be really resource consuming!)
		if (constraint.hasMailboxContent())
		{			
			// filter by deltas
			List<IDelta> deltas = constraint.getMailboxContent();
			// modifications:
			List<Integer> deleteIndexes = new ArrayList<Integer>();
			List<Object[]> additions = new ArrayList<Object[]>();
			// check all delta:
			if (constraint instanceof FindConstraint)
			{
				for (IDelta deltai : deltas)
				{
					Delta delta = (Delta)deltai;
					for (Entry<LookaheadMatching, Boolean> change : delta.getChangeset().entrySet())
					{
						for (int cd = 0; cd < candidates.size(); cd++)
						{
							boolean equal = true;
							
							// add or remove from changeset!
							for (int vizs = 0; vizs < change.getKey().getParameterMatchValuesOnlyAsArray().size(); vizs++)
							{
								if (change.getKey().getParameterMatchValuesOnlyAsArray().get(vizs).equals(candidates.get(cd)[vizs]) == false)
									equal = false;
							}
							
							if (equal)
							{
								if (change.getValue())
									deleteIndexes.add(cd);
								else
									additions.add(change.getKey().getParameterMatchValuesOnlyAsArray().toArray());
							}
						}
					}
				}
			}
			else if (constraint instanceof TypeConstraint)
			{
				// hope should not even implement
				// should get delta of caller!
				List<IDelta> d = constraint.getMailboxContent();
				for (IDelta del : d)
				{
					for (int cd = 0; cd < candidates.size(); cd++)
					{
						ModelChange mchan = (ModelChange)del;
						if (mchan instanceof EClassChange)
						{
							// no type check needed
							if (candidates.get(cd)[0].equals(((EClassChange) mchan).getInstance()))
							{
								// this relation is found but in delta too: mirror!
								// "rollback" this change
								if (mchan.isAddition())
									deleteIndexes.add(cd);
								else
									additions.add(new Object[]{((EClassChange) mchan).getInstance()});
							}
						}
						else if (mchan instanceof EDataTypeChange)
						{
							// no type check needed
							if (candidates.get(cd)[0].equals(((EDataTypeChange) mchan).getInstance()))
							{
								// this relation is found but in delta too: mirror!
								// "rollback" this change
								if (mchan.isAddition())
									deleteIndexes.add(cd);
								else
									additions.add(new Object[]{((EDataTypeChange) mchan).getInstance()});
							}
						}
					}
				}
			}
			else if (constraint instanceof RelationConstraint)
			{				
				// should get delta of caller!
				List<IDelta> d = constraint.getMailboxContent();
				for (IDelta del : d)
				{
					for (int cd = 0; cd < candidates.size(); cd++)
					{
						ModelChange mchan = (ModelChange)del;
						if (mchan instanceof EFeatureChange)
						{
							if (candidates.get(cd)[0].equals(((EFeatureChange) mchan).getHost()) && candidates.get(cd)[1].equals(((EFeatureChange) mchan).getInstance()))
							{
								// this relation is found but in delta too: mirror!
								// "rollback" this change
								if (mchan.isAddition())
									deleteIndexes.add(cd);
								else
									additions.add(new Object[]{((EFeatureChange) mchan).getHost(), ((EFeatureChange) mchan).getInstance()});
							}
						}
					}
				}
			}
			else
			{
				throw new AssertionError("Unknown constraint, which should be filtered!");
			}
			ret = new ArrayList<Object[]>();
			// then delete the indexes, (candidate), so full
			for (int i=0;i<candidates.size();i++)
			{
				if (deleteIndexes.contains(Integer.valueOf(i)))
					continue; // leave out deleteds
				ret.add(candidates.get(i));
			}
			for (Object[] fasz : additions)
			{
				ret.add(fasz);
			}
		}
		
		// if had delta, ret changed but okay
		if (ret == null)
			return candidates; // no mailbox
		return ret; // mailbox, processed
	}

}
