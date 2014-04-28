package hu.bme.mit.inf.lookaheadmatcher.impl;

import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.incquery.runtime.matchers.psystem.PConstraint;
import org.eclipse.incquery.runtime.matchers.psystem.PVariable;
import org.eclipse.incquery.runtime.matchers.psystem.basicenumerables.TypeUnary;

public class TypeConstraint extends AxisConstraint // implements IConstraint
{
	// a variable MUST BE a certain type
	// type can be EInt, EString, EClass
	private PVariable pVariable;
	private EClassifier type;
	
	private boolean isDatatype;
	private PConstraint innerTypeConstraint;
	public boolean isDatatype()
	{
		return this.isDatatype;
	}
	
	public PVariable getTypedVariable()
	{
		return pVariable;
	}
	public void setTypedVariable(PVariable variable)
	{
		this.pVariable = variable;
	}
	public EClassifier getType()
	{
		return type;
	}
	public void setType(EClassifier Type)
	{
		this.type = Type;
	}
	
	@SuppressWarnings("unused")
	private TypeConstraint(){}
	
//	public TypeConstraint(LookVariable lookVar, EClassifier Type)
//	{
//		super();
//		this.pVa = lookVar;
//		this.type = Type;
//		if (type instanceof EClass)
//			this.isDatatype = false;
//		else
//			this.isDatatype = true;
//	}
	
	public TypeConstraint(TypeUnary inner)
	{
		this.setInnerTypeConstraint(inner);
		if (inner.getAffectedVariables().size()>1)
		{
			// error
			throw new AssertionError("Unknown affected vars of TypeUnary!");
		}
		this.pVariable = (PVariable) inner.getVariablesTuple().get(0);
		this.type = (EClassifier) inner.getTypeInfo(this.pVariable);
		if (this.type instanceof EDataType)
			this.isDatatype = true;
	}

	@Override
	public String toString()
	{
		return this.type.getName()+"("+this.pVariable.getName()+")";
	}

	public PConstraint getInnerTypeConstraint() {
		return innerTypeConstraint;
	}

	public void setInnerTypeConstraint(PConstraint innerTypeConstraint) {
		this.innerTypeConstraint = innerTypeConstraint;
	}
}
