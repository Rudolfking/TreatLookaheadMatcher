package hu.bme.mit.inf.lookaheadmatcher.impl;

import hu.bme.mit.inf.lookaheadmatcher.IPartialPatternCacher;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.incquery.runtime.api.IQuerySpecification;
import org.eclipse.incquery.runtime.api.IncQueryEngine;
import org.eclipse.incquery.runtime.matchers.psystem.PBody;
import org.eclipse.incquery.runtime.matchers.psystem.PParameter;
import org.eclipse.incquery.runtime.matchers.psystem.PQuery;
import org.eclipse.incquery.runtime.matchers.psystem.PVariable;
import org.eclipse.incquery.patternlanguage.helper.CorePatternLanguageHelper;
import org.eclipse.incquery.patternlanguage.patternLanguage.CheckConstraint;
import org.eclipse.incquery.patternlanguage.patternLanguage.CompareConstraint;
import org.eclipse.incquery.patternlanguage.patternLanguage.CompareFeature;
import org.eclipse.incquery.patternlanguage.patternLanguage.Constraint;
import org.eclipse.incquery.patternlanguage.patternLanguage.IntValue;
import org.eclipse.incquery.patternlanguage.patternLanguage.LiteralValueReference;
import org.eclipse.incquery.patternlanguage.patternLanguage.ParameterRef;
import org.eclipse.incquery.patternlanguage.patternLanguage.PathExpressionConstraint;
import org.eclipse.incquery.patternlanguage.patternLanguage.PathExpressionHead;
import org.eclipse.incquery.patternlanguage.patternLanguage.PathExpressionTail;
import org.eclipse.incquery.patternlanguage.patternLanguage.Pattern;
import org.eclipse.incquery.patternlanguage.patternLanguage.PatternBody;
import org.eclipse.incquery.patternlanguage.patternLanguage.PatternCall;
import org.eclipse.incquery.patternlanguage.patternLanguage.PatternCompositionConstraint;
import org.eclipse.incquery.patternlanguage.patternLanguage.StringValue;
import org.eclipse.incquery.patternlanguage.patternLanguage.ValueReference;
import org.eclipse.incquery.patternlanguage.patternLanguage.Variable;
import org.eclipse.incquery.patternlanguage.patternLanguage.VariableReference;
import org.eclipse.incquery.patternlanguage.patternLanguage.VariableValue;
import org.eclipse.incquery.patternlanguage.emf.eMFPatternLanguage.ClassType;
import org.eclipse.incquery.patternlanguage.emf.eMFPatternLanguage.EClassifierConstraint;
import org.eclipse.incquery.patternlanguage.emf.eMFPatternLanguage.ReferenceType;
import org.eclipse.xtext.xbase.XExpression;
import org.eclipse.xtext.xbase.jvmmodel.IJvmModelAssociations;
import org.eclipse.xtext.common.types.JvmIdentifiableElement;

import com.google.inject.Inject;

public class PatternProcessor
{
	// aheads, yeah
	// private Stack<AheadStructure> aheadStack = new Stack<AheadStructure>();

	// flatten?
	// private boolean flatteningEnabled = false;
	
	IncQueryEngine engineRef;	
	IPartialPatternCacher partialCacher;
	
	@SuppressWarnings("unused")
	private PatternProcessor()
	{
	}
	
	public PatternProcessor(IncQueryEngine engine, IPartialPatternCacher partialer)
	{
		this.engineRef = engine;
		this.partialCacher = partialer;
	}
	
	//flattened final list
	private ArrayList<AheadStructure> flattenedPatterns;
	
	public ArrayList<AheadStructure> Process(PQuery pattern) //, boolean flattenIt)
	{
		// this.flatteningEnabled = flattenIt;
		List<PParameter> vars = pattern.getParameters();
		// parameters
		this.flattenedPatterns = new ArrayList<AheadStructure>();
		
		Set<PBody> pbodies = pattern.getContainedBodies();
		for (PBody pBody : pbodies) {
			AheadStructure as = new AheadStructure(pBody, vars, partialCacher, engineRef);
			flattenedPatterns.add(as);
		}
		
		return this.flattenedPatterns;
	}
}
