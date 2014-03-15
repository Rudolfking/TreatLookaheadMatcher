package hu.bme.mit.inf.lookaheadmatcher.impl;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.incquery.runtime.matchers.psystem.PVariable;
import org.eclipse.incquery.runtime.matchers.psystem.basicenumerables.TypeBinary;

public class RelationConstraint extends AxisConstraint // implements IConstraint
{
	// source, target and the relation
	
	private TypeBinary innerTypeBinary; 

	public TypeBinary getInnerTypeBinary() {
		return innerTypeBinary;
	}
	
	private PVariable source;
	public PVariable getSource() {
		return source;
	}
	public void setSource(PVariable source) {
		this.source = source;
	}

	private PVariable target;
	public PVariable getTarget() {
		return target;
	}
	public void setTarget(PVariable target) {
		this.target = target;
	}
	
	private EStructuralFeature edge; 
	public EStructuralFeature getEdge() {
		return edge;
	}
	public void setEdge(EStructuralFeature edge) {
		this.edge = edge;
	}
	
	private boolean pointsToAttribute;
	public boolean PointsToAttribute()
	{
		return this.pointsToAttribute;
	}
	
	@SuppressWarnings("unused")
	private RelationConstraint(){}
	
//	public RelationConstraint(LookVariable Source, LookVariable Target, EStructuralFeature Edge)
//	{
//		this.source = Source;
//		this.target = Target;
//		this.edge = Edge;
//		if (Edge instanceof EAttribute)
//			pointsToAttribute = true;
//		else
//			pointsToAttribute = false;
//	}
	
	public RelationConstraint(TypeBinary relCons)
	{
		this.innerTypeBinary = relCons;
		this.edge = (EStructuralFeature) relCons.getSupplierKey();
		this.pointsToAttribute = this.edge instanceof EAttribute;
		this.source = (PVariable) relCons.getVariablesTuple().get(0);
		this.target = (PVariable) relCons.getVariablesTuple().get(1);
	}
	
	@Override
	public String toString()
	{
		return this.source.getName()+"->-"+this.edge.getName()+"->-"+this.target.getName();
	}
}