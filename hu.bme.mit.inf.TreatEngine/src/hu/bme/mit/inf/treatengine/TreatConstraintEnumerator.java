package hu.bme.mit.inf.treatengine;

import java.util.ArrayList;
import java.util.Arrays;
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
			if (!(constraint instanceof FindConstraint))
				throw new AssertionError("Not findconstraint mailbox content is not supported!"); // but should return fallback? no, should not
			else
			{
				// get and return: this will filter by content
				return enumerateConstraint(constraint, matchingVariables).size();
			}
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
			ArrayList<Object> additions = new ArrayList<Object>();
			// check all delta:
			for (IDelta deltai : deltas)
			{
				Delta delta = (Delta)deltai;
				if (constraint instanceof FindConstraint)
				{
					for (Entry<LookaheadMatching, Boolean> change : delta.getChangeset().entrySet())
					{
						for (int cd = 0; cd < candidates.size(); cd++)
						{
							boolean equal = true;
							
							// add or remove from changeset!
							for (int vizs = 0; vizs < change.getKey().getParameterMatchValuesOnlyAsArray().length; vizs++)
							{
								if (change.getKey().getParameterMatchValuesOnlyAsArray()[vizs].equals(candidates.get(cd)[vizs]) == false)
									equal = false;
							}
							
							if (equal)
							{
								if (change.getValue() == false)
									deleteIndexes.add(cd);
								else
									additions.addAll(Arrays.asList(change.getKey().getParameterMatchValuesOnlyAsArray()));
							}
						}
					}
				}
				else if (constraint instanceof TypeConstraint)
				{
					// hope should not even implement
					throw new AssertionError("Not implemented!");
				}
				else if (constraint instanceof RelationConstraint)
				{
					throw new AssertionError("Not implemented!");
				}
				else
				{
					throw new AssertionError("Unknown constraint, which should be filtered!");
				}
			}
			ret = new ArrayList<Object[]>();
			// then delete the indexes, (candidate), so full
			for (int i=0;i<candidates.size();i++)
			{
				if (deleteIndexes.contains(Integer.valueOf(i)))
					continue; // leave out deleteds
				ret.add(candidates.get(i));
			}
		}
		
		// if had delta, ret changed but okay
		if (ret == null)
			return candidates; // no mailbox
		return ret; // mailbox, processed
	}

}
