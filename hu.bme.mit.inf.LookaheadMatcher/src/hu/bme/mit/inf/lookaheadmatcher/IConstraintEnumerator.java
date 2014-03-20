package hu.bme.mit.inf.lookaheadmatcher;

import java.util.List;
import java.util.HashMap;

import org.eclipse.incquery.runtime.matchers.psystem.PVariable;

import hu.bme.mit.inf.lookaheadmatcher.impl.AxisConstraint;

public interface IConstraintEnumerator
{
	int getCost(AxisConstraint constraint, HashMap<PVariable, Object> matchingVariables);
	
	List<Object[]> enumerateConstraint(AxisConstraint constraint, HashMap<PVariable, Object> matchingVariables);
}
