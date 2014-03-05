//package hu.bme.mit.inf.lookaheadmatcher.impl;
//
//import java.net.MalformedURLException;
//import java.net.URL;
//import java.net.URLClassLoader;
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.Map.Entry;
//
//import org.eclipse.core.resources.IFile;
//import org.eclipse.core.resources.IProject;
//import org.eclipse.core.resources.IWorkspaceRoot;
//import org.eclipse.core.runtime.CoreException;
//import org.eclipse.core.runtime.IConfigurationElement;
//import org.eclipse.core.runtime.IPath;
//import org.eclipse.core.runtime.Path;
//import org.eclipse.core.runtime.Platform;
//import org.eclipse.emf.common.util.URI;
//import org.eclipse.emf.ecore.resource.Resource;
//import org.eclipse.incquery.runtime.IExtensions;
//import org.eclipse.incquery.runtime.extensibility.IMatchChecker;
//import org.eclipse.xtext.naming.QualifiedName;
//import org.eclipse.xtext.util.CancelIndicator;
//import org.eclipse.xtext.xbase.interpreter.impl.XbaseInterpreter;
//import org.eclipse.xtext.xbase.XExpression;
//import org.eclipse.xtext.xbase.interpreter.IEvaluationContext;
//import org.eclipse.xtext.xbase.interpreter.IEvaluationResult;
//import org.eclipse.xtext.xbase.interpreter.IExpressionInterpreter;
//import org.eclipse.incquery.runtime.matchers.tuple.FlatTuple;
//import org.eclipse.incquery.patternlanguage.emf.internal.XtextInjectorProvider;
//import org.eclipse.incquery.patternlanguage.patternLanguage.Pattern;
//import org.eclipse.jdt.core.IJavaProject;
//import org.eclipse.jdt.core.JavaCore;
//import org.eclipse.jdt.launching.JavaRuntime;
//
//import com.google.inject.Injector;
//import com.google.inject.Provider;
//
//public class CheckExpressionConstraint extends CheckableConstraint implements
//		IConstraint
//{
//	private ArrayList<LookVariable> affectedVariables;
//
//	// some kind of generated code matchChecker
//	private IMatchChecker matchChecker;
//
//	// some kind of interprerter checker
//	private XbaseInterpreter interpreter;
//	
//	// the expression
//	private XExpression expression;
//	
//	// the pattern
//	private Pattern containerPattern;
//	
//	// ki tudja mi ez...
//	private Provider<IEvaluationContext> contextProvider;
//
//    private IWorkspaceRoot root;
//    
//	@SuppressWarnings("restriction")
//	@Override
//	public boolean Evaluate(HashMap<LookVariable, Object> matchingVariables)
//	{
//		// First option: try to evalute with the generated code
//		if (matchChecker != null)
//		{
//			// need tuple
//			Object[] obi = new Object[this.affectedVariables.size()];
//			Map< String, Integer> mepp = new HashMap<String, Integer>();
//			int cu =0;
//			for (LookVariable vai : this.affectedVariables)
//			{
//				Object meccsed = matchingVariables.get(vai);
//				obi[cu]=meccsed;
//				String qualifiedName = vai.getVariable().getSimpleName();
//				mepp.put(qualifiedName, cu++);
//			}
//			FlatTuple tupi = new FlatTuple(obi);
//			return (boolean) matchChecker.evaluateXExpression(tupi, mepp);
//		}
//
//		// Second option: try to evaluate with the interpreted approach
//		IEvaluationContext context = contextProvider.get();
//		for (Entry<LookVariable, Object> entry : matchingVariables.entrySet())
//		{
//			context.newValue(QualifiedName.create(entry.getKey().getVariable().getSimpleName()),entry.getValue());
//		}
//		IEvaluationResult result = interpreter.evaluate(this.expression, context, CancelIndicator.NullImpl);
//		if (result == null)
//		{
//			System.out.println("Result nem jo, nagy a baj, 0c16");
//		}
//		if (result.getException() != null)
//			System.out.println("Result nem jo, nagy a baj, 0r576");
//		return (Boolean)(result.getResult());
//	}
//
//	@Override
//	public boolean CanBeEvaluated(HashMap<LookVariable, Object> matchingVariables)
//	{
//		//return matchingVariables.keySet().containsAll(affectedVariables);
//		for(LookVariable v : this.affectedVariables)
//		{
//			if (matchingVariables.get(v)==null)
//			{
//				return false;
//			}
//		}
//		return true; // canna be
//	}
//	
//	@SuppressWarnings("unused")
//	private CheckExpressionConstraint(){}
//	
//	@SuppressWarnings("restriction")
//	public CheckExpressionConstraint(XExpression checkExpression, Pattern pattern, LookVariable[] affecteds)
//	{
//		this.expression = checkExpression;
//		this.containerPattern = pattern;
//
//		this.affectedVariables = new ArrayList<LookVariable>();
//		for (LookVariable lookVariable : affecteds) {
//			this.affectedVariables.add(lookVariable);
//		}
//		// get ready
//		
//		
//		
//		// generated code?? not working, i dont find getExpressionUniqueID(pattern, xexpression)
////		this.matchChecker = null;
////		
////		IConfigurationElement[] configurationElements = Platform.getExtensionRegistry().getConfigurationElementsFor(IExtensions.QUERY_SPECIFICATION_EXTENSION_POINT_ID);
////		for (IConfigurationElement configurationElement : configurationElements)
////		{
////			String id = configurationElement.getAttribute("id");
////			if (id.equals(CheckExpressionUtil.getExpressionUniqueID(this.containerPattern, this.expression)))
////			{
////				Object object = null;
////				try
////				{
////					object = configurationElement.createExecutableExtension("evaluatorClass");
////				}
////				catch (CoreException e)
////				{
////					e.printStackTrace();
////				}
////				if (object != null && object instanceof IMatchChecker)
////				{
////					matchChecker = (IMatchChecker) object;
////				}
////			}
////		}
//		
//		
//		
//		// interpreter if simple mode failed:
//		if (matchChecker == null)
//		{
//			Injector injector = XtextInjectorProvider.INSTANCE.getInjector();
//			interpreter = (XbaseInterpreter) injector.getInstance(IExpressionInterpreter.class);
//			try
//			{
//				ClassLoader classLoader = getClassLoader(getIFile(this.containerPattern));
//				if (classLoader != null)
//				{
//					interpreter.setClassLoader(getClassLoader(getIFile(this.containerPattern)));
//				}
//			}
//			catch (MalformedURLException e)
//			{
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//			catch (CoreException e)
//			{
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//			contextProvider = injector.getProvider(IEvaluationContext.class);
//		}
//	}
//	
//	
//	// this code is stolen from JavaProjectClassLoaderProvider.java, which i couldnt include
//    public ClassLoader getClassLoader(IFile file) throws CoreException, MalformedURLException {
//        if (file != null) {
//            IProject project = file.getProject();
//            IJavaProject jp = JavaCore.create(project);
//            String[] classPathEntries = JavaRuntime.computeDefaultRuntimeClassPath(jp);
//            List<URL> classURLs = getClassesAsURLs(classPathEntries);
//            URL[] urls = (URL[]) classURLs.toArray(new URL[classURLs.size()]);
//            URLClassLoader loader = URLClassLoader.newInstance(urls, jp.getClass().getClassLoader());
//            return loader;
//        }
//        return null;
//    }
//    public IFile getIFile(Pattern pattern) {
//        if (pattern != null) {
//            Resource resource = pattern.eResource();
//            if (resource != null) {
//                URI uri = resource.getURI();
////                uri = resource.getResourceSet().getURIConverter().normalize(uri);
//                String scheme = uri.scheme();
//                if ("platform".equals(scheme) && uri.segmentCount() > 1 && "resource".equals(uri.segment(0))) {
//                    StringBuffer platformResourcePath = new StringBuffer();
//                    for (int j = 1, size = uri.segmentCount(); j < size; ++j) {
//                        platformResourcePath.append('/');
//                        platformResourcePath.append(uri.segment(j));
//                    }
//                    return root.getFile(new Path(platformResourcePath.toString()));
//                }
//            }
//        }
//        return null;
//    }    
//    private List<URL> getClassesAsURLs(String[] classPathEntries) throws MalformedURLException {
//        List<URL> urlList = new ArrayList<URL>();
//        for (int i = 0; i < classPathEntries.length; i++) {
//            String entry = classPathEntries[i];
//            IPath path = new Path(entry);
//            URL url = path.toFile().toURI().toURL();
//            urlList.add(url);
//        }
//        return urlList;
//    }
//    // lopas idaig
//
//	@Override
//	public String toString() 
//	{
//		return "check("+writeArray(this.affectedVariables)+")";
//	}
//	
//	private String writeArray(ArrayList<LookVariable> vv)
//	{
//		String ret="";
//		for(LookVariable v:vv)
//		{
//			ret+=v.getVariableName()+",";
//		}
//		ret = ret.substring(0, ret.length()-1);
//		return ret;
//	}
//}
