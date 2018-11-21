package org.eclipse.jdt.ls.core.internal.handlers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchParticipant;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.SearchRequestor;
import org.eclipse.jdt.internal.core.SourceType;
import org.eclipse.jdt.ls.core.internal.JDTUtils;
import org.eclipse.jdt.ls.core.internal.JavaLanguageServerPlugin;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.TextDocumentIdentifier;

/**
 * @author Pixy Yuan
 * on 2018/11/21
 */
public class FileReferencesHandler {

	public FileReferencesHandler() {
	}

	private IJavaSearchScope createSearchScope(ICompilationUnit unit) {
		IJavaProject project = unit.getJavaProject();
		return SearchEngine.createJavaSearchScope(new IJavaElement[] {project}, IJavaSearchScope.SOURCES);
	}

	public List<Location> findReferences(TextDocumentIdentifier param, IProgressMonitor monitor) {

		final List<Location> locations = new ArrayList<>();
		try {
			ICompilationUnit unit = JDTUtils.resolveCompilationUnit(param.getUri());

			if (unit == null) {
				return locations;
			}

			SourceType cls = Arrays.stream(unit.getChildren())
				.filter(el -> el.getElementType() == SourceType.TYPE)
				.map(el -> (SourceType)el)
				.filter(el -> {
					try {
						return el.isClass() || el.isInterface() || el.isEnum() || el.isAnnotation();
					} catch (JavaModelException e) {
						return false;
					}
				})
				.findFirst()
				.orElse(null);
			if (cls == null) {
				return locations;
			}
			SearchEngine engine = new SearchEngine();
			SearchPattern pattern = SearchPattern.createPattern(cls, IJavaSearchConstants.REFERENCES);

			IJavaSearchScope searchScope = createSearchScope(unit);

			engine.search(pattern, new SearchParticipant[] { SearchEngine.getDefaultSearchParticipant() }, searchScope, new SearchRequestor() {

				@Override
				public void acceptSearchMatch(SearchMatch match) throws CoreException {
					Object o = match.getElement();
					if (o instanceof IJavaElement) {
						IJavaElement element = (IJavaElement) o;
						ICompilationUnit compilationUnit = (ICompilationUnit) element.getAncestor(IJavaElement.COMPILATION_UNIT);
						Location location = null;
						if (compilationUnit != null) {
							location = JDTUtils.toLocation(compilationUnit, match.getOffset(), match.getLength());
						}
						if (location != null) {
							locations.add(location);
						}
					}
				}
			}, monitor);

		} catch (CoreException e) {
			JavaLanguageServerPlugin.logException("Find references failure ", e);
		}
		return locations;
	}
}
