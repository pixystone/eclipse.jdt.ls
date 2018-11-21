package org.eclipse.jdt.ls.core.internal.handlers;

import java.util.List;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.ls.core.internal.JDTUtils;
import org.eclipse.jdt.ls.core.internal.WorkspaceHelper;
import org.eclipse.jdt.ls.core.internal.managers.AbstractProjectsManagerBasedTest;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author gengtuo.ygt
 * on 2018/11/21
 */
@RunWith(MockitoJUnitRunner.class)
public class FileReferencesHandlerTest extends AbstractProjectsManagerBasedTest {

	private FileReferencesHandler handler;
	private String uri;

	@Before
	public void setup() throws Exception {
		importProjects("maven/salut");
		IJavaProject project = JavaCore.create(WorkspaceHelper.getProject("salut"));
		IPackageFragmentRoot sourceFolder = project.getPackageFragmentRoot(project.getProject().getFolder("src/main"));
		IPackageFragment packageFragment = sourceFolder.getPackageFragment("java.java");
		ICompilationUnit unit = packageFragment.getCompilationUnit("Foo.java");
		this.uri = JDTUtils.toURI(unit);
		this.handler = new FileReferencesHandler();
	}

	@Test
	public void testClass() {
		TextDocumentIdentifier identifier = new TextDocumentIdentifier(uri);
		List<Location> references = handler.findReferences(identifier, monitor);
		assertEquals(2, references.size());
		assertEquals(uri, references.get(0).getUri());
		assertEquals(10, references.get(0).getRange().getStart().getLine());
		assertEquals(68, references.get(0).getRange().getStart().getCharacter());
		assertEquals(10, references.get(0).getRange().getEnd().getLine());
		assertEquals(71, references.get(0).getRange().getEnd().getCharacter());
		assertTrue(references.get(1).getUri().contains("Foo2.java"));
		assertEquals(27, references.get(1).getRange().getStart().getLine());
		assertEquals(11, references.get(1).getRange().getStart().getCharacter());
		assertEquals(27, references.get(1).getRange().getEnd().getLine());
		assertEquals(14, references.get(1).getRange().getEnd().getCharacter());
	}
}
