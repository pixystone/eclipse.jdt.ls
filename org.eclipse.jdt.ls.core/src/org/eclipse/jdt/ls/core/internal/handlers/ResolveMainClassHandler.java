/*******************************************************************************
 * Copyright (c) 2017 Microsoft Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Microsoft Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.ls.core.internal.handlers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchParticipant;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.SearchRequestor;
import org.eclipse.jdt.ls.core.internal.JDTUtils;
import org.eclipse.jdt.ls.core.internal.ResourceUtils;
import org.eclipse.jdt.ls.core.internal.WorkspaceIdentifier;
import org.eclipse.jdt.ls.core.internal.managers.ProjectsManager;

public class ResolveMainClassHandler {
    private static final Logger logger = Logger.getLogger(ResolveMainClassHandler.class.getName());

    /**
     * resolve main class and project name.
     * @return an array of main class and project name
     */
    public List<ResolutionItem> resolveMainClass(WorkspaceIdentifier params, IProgressMonitor monitor) {
        if (monitor.isCanceled() || params == null) {
            return Collections.emptyList();
        }
        return resolveMainClassCore(params.getUri());
    }

    private List<ResolutionItem> resolveMainClassCore(String projectUri) {
        IPath rootPath = null;
        if (projectUri != null && !projectUri.isEmpty()) {
            rootPath = ResourceUtils.filePathFromURI(projectUri);
        }
        final ArrayList<IPath> targetProjectPath = new ArrayList<>();
        if (rootPath != null) {
            targetProjectPath.add(rootPath);
        }
        IJavaSearchScope searchScope = SearchEngine.createWorkspaceScope();
        SearchPattern pattern = SearchPattern.createPattern("main(String[]) void", IJavaSearchConstants.METHOD,
                IJavaSearchConstants.DECLARATIONS, SearchPattern.R_CASE_SENSITIVE | SearchPattern.R_EXACT_MATCH);
        ArrayList<ResolutionItem> res = new ArrayList<>();
        SearchRequestor requestor = new SearchRequestor() {
            @Override
            public void acceptSearchMatch(SearchMatch match) {
                Object element = match.getElement();
                if (element instanceof IMethod) {
                    IMethod method = (IMethod) element;
                    try {
                        if (method.isMainMethod()) {
                            IResource resource = method.getResource();
                            if (resource != null) {
                                IProject project = resource.getProject();
                                if (project != null) {
                                    String mainClass = method.getDeclaringType().getFullyQualifiedName();
                                    IJavaProject javaProject = JDTUtils.getJavaProject(project);
                                    if (javaProject != null) {
                                        String moduleName = JDTUtils.getModuleName(javaProject);
                                        if (moduleName != null) {
                                            mainClass = moduleName + "/" + mainClass;
                                        }
                                    }
                                    String projectName = ProjectsManager.DEFAULT_PROJECT_NAME.equals(project.getName()) ? null : project.getName();
                                    if (projectName == null
                                        || targetProjectPath.isEmpty()
                                        || ResourceUtils.isContainedIn(project.getLocation(), targetProjectPath)) {
                                        String filePath = null;

                                        if (match.getResource() instanceof IFile) {
                                            try {
                                                filePath = match.getResource().getLocation().toOSString();
                                            } catch (Exception ex) {
                                                // ignore
                                            }
                                        }
                                        res.add(new ResolutionItem(mainClass, projectName, filePath));
                                    }
                                }
                            }
                        }
                    } catch (JavaModelException e) {
                        // ignore
                    }
                }
            }
        };
        SearchEngine searchEngine = new SearchEngine();
        try {
            searchEngine.search(pattern, new SearchParticipant[] {SearchEngine.getDefaultSearchParticipant()},
                    searchScope, requestor, null /* progress monitor */);
        } catch (Exception e) {
            logger.log(Level.SEVERE, String.format("Searching the main class failure: %s", e.toString()), e);
        }
        return res.stream().distinct().collect(Collectors.toList());
    }

    public static class ResolutionItem {
        private String mainClass;
        private String projectName;
        private String filePath;

        public ResolutionItem(String mainClass, String projectName, String filePath) {
            this.mainClass = mainClass;
            this.projectName = projectName;
            this.filePath = filePath;
        }

        public String getMainClass() {
            return mainClass;
        }

        public String getProjectName() {
            return projectName;
        }

        public String getFilePath() {
            return filePath;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o instanceof ResolutionItem) {
                ResolutionItem item = (ResolutionItem)o;
                return Objects.equals(mainClass, item.mainClass)
                    && Objects.equals(projectName, item.projectName)
                    && Objects.equals(filePath, item.filePath);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return Objects.hash(mainClass, projectName, filePath);
        }

        @Override
        public String toString() {
            return new ToStringBuilder(this)
                .append("mainClass", mainClass)
                .append("projectName", projectName)
                .append("filePath", filePath)
                .toString();
        }
    }
}
