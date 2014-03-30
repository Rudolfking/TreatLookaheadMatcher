package hu.bme.mit.inf.lookaheadmatcher.impl;

import hu.bme.mit.inf.lookaheadmatcher.IPartialPatternCacher;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.eclipse.incquery.runtime.api.IncQueryEngine;
import org.eclipse.incquery.runtime.matchers.psystem.PBody;
import org.eclipse.incquery.runtime.matchers.psystem.PQuery;

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
		List<String> vars = pattern.getParameterNames();
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
