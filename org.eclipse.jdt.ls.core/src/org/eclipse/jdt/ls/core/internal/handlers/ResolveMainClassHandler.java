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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.MethodNameMatch;
import org.eclipse.jdt.core.search.MethodNameMatchRequestor;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.ls.core.internal.JDTUtils;
import org.eclipse.jdt.ls.core.internal.ProjectUtils;
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
        return resolveMainClassCore(params.getUri(), monitor);
    }

    private List<ResolutionItem> resolveMainClassCore(String projectUri, IProgressMonitor monitor) {
        List<IJavaProject> targetProjects = new ArrayList<>();
        IJavaProject[] javaProjects = ProjectUtils.getJavaProjects();
        if (javaProjects == null || javaProjects.length == 0) {
            return Collections.emptyList();
        }
        if (projectUri != null && !projectUri.isEmpty()) {
            final IPath rootPath = ResourceUtils.filePathFromURI(projectUri);
            Optional<IJavaProject> project = Arrays.stream(javaProjects)
                .filter(p -> {
                    IPath path = p.getPath();
                    return path != null && path.equals(rootPath);
                })
                .findAny();
            if (project.isPresent()) {
                javaProjects = new IJavaProject[] {project.get()};
                targetProjects.add(project.get());
            }
        }
        final ArrayList<ResolutionItem> res = new ArrayList<>();
        try {
            new SearchEngine().searchAllMethodNames(null, SearchPattern.R_PATTERN_MATCH,
                "main".toCharArray(),
                SearchPattern.R_FULL_MATCH,
                SearchEngine.createJavaSearchScope(javaProjects, IJavaSearchScope.SOURCES),
                new MethodNameMatchRequestor() {
                    @Override
                    public void acceptMethodNameMatch(MethodNameMatch methodNameMatch) {
                        collectResult(methodNameMatch.getMethod(), targetProjects, res);
                    }
                }, IJavaSearchConstants.WAIT_UNTIL_READY_TO_SEARCH, monitor);
        } catch (Exception e) {
            logger.log(Level.SEVERE, String.format("Searching the main class failure: %s", e.toString()), e);
        }
        return res.stream().distinct().collect(Collectors.toList());
    }

    private void collectResult(IMethod method, List<IJavaProject> targetProjects, ArrayList<ResolutionItem> res) {
        if (method == null) {
            return;
        }
        try {
            if (method.isMainMethod()) {
                IJavaProject methodJavaProject = method.getJavaProject();
                if (methodJavaProject == null) {
                    return;
                }
                String mainClass = method.getDeclaringType().getFullyQualifiedName();
                String moduleName = JDTUtils.getModuleName(methodJavaProject);
                if (moduleName != null) {
                    mainClass = moduleName + "/" + mainClass;
                }
                IProject project = methodJavaProject.getProject();
                String projectName = ProjectsManager.DEFAULT_PROJECT_NAME.equals(project.getName()) ? null : project.getName();
                if (projectName == null
                    || targetProjects.isEmpty()
                    || isContainsIn(methodJavaProject, targetProjects)) {
                    String filePath = null;

                    if (method.getResource() instanceof IFile) {
                        try {
                            filePath = method.getResource().getLocation().toOSString();
                        } catch (Exception ex) {
                            // ignore
                        }
                    }
                    res.add(new ResolutionItem(mainClass, projectName, filePath));
                }
            }
        } catch (JavaModelException e) {
            // ignore
        }
    }

    private static boolean isContainsIn(IJavaProject project, List<IJavaProject> targetProjects) {
        return project != null && targetProjects.stream().anyMatch(project::equals);
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
