package hu.bme.mit.inf.lookaheadmatcher.impl;

import java.util.HashMap;

import org.eclipse.incquery.runtime.matchers.psystem.PVariable;
import org.eclipse.incquery.runtime.matchers.psystem.basicdeferred.ExpressionEvaluation;

public class CheckEvalExpressionConstraint extends CheckableConstraint
{
	private ExpressionEvaluation innerExpressionEvalConstraint;
	
	public CheckEvalExpressionConstraint(ExpressionEvaluation evalCons)
	{
		this.innerExpressionEvalConstraint = evalCons;
	}
	
	@Override
	public boolean Evaluate(HashMap<PVariable, Object> matchingVariables)
	{
		try 
		{
			Object result = this.innerExpressionEvalConstraint.getEvaluator().evaluateExpression(new MyValueProvider(matchingVariables));
			// result can be null?
			if (result instanceof Boolean)
				return (Boolean)result;
			else if (result != null)
			{
				matchingVariables.put(this.innerExpressionEvalConstraint.getOutputVariable(), result);
				return true;
			}
			else
			{
				System.err.println("Expression eval returned null!" + this.innerExpressionEvalConstraint.toString());
			}
			return false;
		} 
		catch (Exception e)
		{
			System.out.println("Failed to evaluate expression!");
			e.printStackTrace();
			return false;
		}
	}
	
	@Override
	public boolean CanBeEvaluated(java.util.HashMap<PVariable,Object> matchingVariables)
	{
		for(PVariable var : innerExpressionEvalConstraint.getAffectedVariables())
		{
			if (matchingVariables.get(var) == null)
				return false;
		}
		return true;
	}

	public ExpressionEvaluation getInnerExpressionEvalConstraint() {
		return innerExpressionEvalConstraint;
	}

	public void setInnerExpressionEvalConstraint(
			ExpressionEvaluation innerExpressionEvalConstraint) {
		this.innerExpressionEvalConstraint = innerExpressionEvalConstraint;
	}
}
