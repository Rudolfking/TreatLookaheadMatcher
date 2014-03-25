
package hu.bme.mit.inf.treatenginegui.handlers;

import hu.bme.mit.inf.lookaheadmatcher.impl.LookaheadMatching;
import hu.bme.mit.inf.lookaheadmatcher.impl.MultiSet;
import hu.bme.mit.inf.treatengine.LookaheadMatcherTreat;
import hu.bme.mit.inf.treatengine.TreatRegistrarImpl;

import java.util.List;
import java.util.Map.Entry;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.incquery.tooling.ui.queryexplorer.QueryExplorer;
import org.eclipse.incquery.tooling.ui.queryexplorer.content.matcher.ObservablePatternMatcherRoot;
import org.eclipse.incquery.tooling.ui.queryexplorer.util.QueryExplorerPatternRegistry;
import org.eclipse.incquery.patternlanguage.patternLanguage.Pattern;
import org.eclipse.incquery.runtime.api.*;
import org.eclipse.incquery.runtime.api.IQuerySpecification;


/**
 * Our sample handler extends AbstractHandler, an IHandler base class.
 * 
 * @see org.eclipse.core.commands.IHandler
 * @see org.eclipse.core.commands.AbstractHandler
 */
public class CallMatcherHandler extends AbstractHandler
{
	/**
	 * The constructor.
	 */
	public CallMatcherHandler()
	{
	}
	
	/**
	 * the command has been executed, so extract extract the needed information
	 * from the application context.
	 */
	
	private IncQueryEngine engine = null;
	String offeredPattern = null;
	
	public Object execute(ExecutionEvent event) throws ExecutionException
	{
		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
		
		// get patterns
		List<IQuerySpecification<?>> patterns = QueryExplorerPatternRegistry.getInstance().getActivePatterns();
		System.out.println("Registered pattern list, num of patterns: " + patterns.size());
		
		offeredPattern = QueryExplorer.getInstance().getPatternsViewer().getSelection().toString();
		if (offeredPattern.length() > 1 && offeredPattern.charAt(offeredPattern.length() - 1) == ']' && offeredPattern.charAt(0) == '[')
		{
			offeredPattern = offeredPattern.substring(1, offeredPattern.length() - 1);
			System.out.println("Last offered (selected) pattern: " + offeredPattern);
		}
		
		IStructuredSelection selection = (IStructuredSelection) HandlerUtil.getCurrentSelection(event);
		Object firstElement = selection.getFirstElement();
		if (firstElement instanceof ObservablePatternMatcherRoot)
		{
			engine = ((ObservablePatternMatcherRoot) firstElement).getKey().getEngine();
			System.out.println("Registered engine: " + engine.toString());
		}
		else
		{
			MessageDialog.openInformation(window.getShell(), "LookaheadMatcher", "Error: unrecognised selection (instancemodel file (resource) or ObservablePatternMatcherRoot from Query Explorer, IncQuery!");
		}
		
		if (patterns.size() == 0)
		{
			System.out.println("Error: no patterns definied/found");
		}
		else if (engine != null && offeredPattern != null && offeredPattern.length() > 0)
		{
			LookaheadMatcherTreat treat = new LookaheadMatcherTreat(engine);
			TreatRegistrarImpl.LookaheadToEngineConnector.Connect(engine, treat); // memory drot, ha kell
			
			
			// you can MATCH: IncQueryEngine engine (benne a notifier->model), List<Pattern> patterns, String offeredPattern (selected)
			IQuerySpecification<?> chosenSpecification = null;
			for (int i = 0; i < patterns.size(); i++)
			{
				IQuerySpecification<?> temPat = patterns.get(i);				
			}
			for (IQuerySpecification<?> specification : QueryExplorerPatternRegistry.getInstance().getActivePatterns()) 
			{
				if (specification.getFullyQualifiedName().endsWith(offeredPattern))
				{
					chosenSpecification = specification;
				}
			}
			System.out.println("Started matching everything!");
			long begin = System.currentTimeMillis();
			for (IQuerySpecification<?> specification : QueryExplorerPatternRegistry.getInstance().getActivePatterns()) 
			{
				MultiSet<LookaheadMatching> matches = null;
				try
				{
					// matches = TreatRegistrarImpl.LookaheadToEngineConnector.GetLookaheadMatcherTreat(engine).matchThePattern(specification);
				}
				catch (Exception e)
				{
					System.out.println("Failed to match " + specification.getFullyQualifiedName() + " because of: " + e.getMessage());
					continue;
				}
				catch (AssertionError er)
				{
					System.out.println("Failed to match " + specification.getFullyQualifiedName() + " because of an [ASSERT]: " + er.getMessage());
					continue;
				}
				if (matches != null) // might represent "no-matching"?
					System.out.println("Matched " + specification.getFullyQualifiedName() + " count: " + matches.uniqueSize() + "(" +  matches.size() + ")");
			}
			System.out.println("Finished matching everything!");
			MultiSet<LookaheadMatching> matches = TreatRegistrarImpl.LookaheadToEngineConnector.GetLookaheadMatcherTreat(engine).matchThePattern(chosenSpecification);
			if (matches.size() == 0)
				System.out.println("No matches!");
			else
			{
				for (Entry<LookaheadMatching, Integer> mg : matches.getInnerMap().entrySet())
				{
					if (mg.getValue() < 2)
						System.out.println(mg.toString());
					else System.out.println(mg.getValue().toString() + " matches of: " + mg.toString());
				}
			}
			System.out.println("Time spent to match: " + Long.toString(System.currentTimeMillis() - begin));
			if (matches == null)
				return null;
		}
		return null;
	}
}
