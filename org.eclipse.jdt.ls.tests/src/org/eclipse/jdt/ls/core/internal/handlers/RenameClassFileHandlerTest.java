package org.eclipse.jdt.ls.core.internal.handlers;

import java.util.List;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.ls.core.internal.JDTUtils;
import org.eclipse.jdt.ls.core.internal.JavaLanguageServerPlugin;
import org.eclipse.jdt.ls.core.internal.ResourceUtils;
import org.eclipse.jdt.ls.core.internal.TextEditUtil;
import org.eclipse.jdt.ls.core.internal.WorkspaceHelper;
import org.eclipse.jdt.ls.core.internal.managers.AbstractProjectsManagerBasedTest;
import org.eclipse.jdt.ls.core.internal.preferences.ClientPreferences;
import org.eclipse.jdt.ls.core.internal.preferences.PreferenceManager;
import org.eclipse.jdt.ls.core.internal.preferences.Preferences;
import org.eclipse.lsp4j.ResourceChange;
import org.eclipse.lsp4j.TextDocumentEdit;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.WorkspaceEdit;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author gengtuo.ygt
 * on 2018/11/12
 */
@RunWith(MockitoJUnitRunner.class)
public class RenameClassFileHandlerTest extends AbstractProjectsManagerBasedTest {

	@Mock
	private PreferenceManager preferenceManager;
	@Mock
	private ClientPreferences clientPreferences;

	private RenameClassFileHandler handler;
	private IJavaProject project;
	private ICompilationUnit cu;
	private String sourceCode;

	@Before
	public void setup() throws Exception {
		importProjects("maven/salut");
		project = JavaCore.create(WorkspaceHelper.getProject("salut"));
		IPackageFragmentRoot sourceFolder = project.getPackageFragmentRoot(project.getProject().getFolder("src/main"));
		IPackageFragment packageFragment = sourceFolder.getPackageFragment("java.java");
		cu = packageFragment.getCompilationUnit("Foo.java").getWorkingCopy(monitor);
		sourceCode = cu.getSource();
		JavaLanguageServerPlugin.setPreferencesManager(preferenceManager);
		when(preferenceManager.getClientPreferences()).thenReturn(clientPreferences);
		Preferences p = mock(Preferences.class);
		when(preferenceManager.getPreferences()).thenReturn(p);
		when(p.isRenameEnabled()).thenReturn(true);
		handler = new RenameClassFileHandler(preferenceManager);
	}


	@Test
	public void testRenameClassFile() throws Exception {
		when(clientPreferences.isWorkspaceEditResourceChangesSupported()).thenReturn(false);

		String currentUri = JDTUtils.toURI(cu);
		String newName = "Foo111.java";
		WorkspaceEdit edit = handler.rename(new RenameClassFileParams(currentUri, newName), monitor);
		assertEquals(2, edit.getChanges().size());
		assertNull(edit.getDocumentChanges());
		assertNull(edit.getResourceChanges());

		assertTrue(edit.getChanges().keySet().stream()
			.anyMatch(key -> key.contains("maven/salut/src/main/java/java/Foo.java")));
		List<TextEdit> textEdits = edit.getChanges().entrySet().stream()
			.filter(entry -> entry.getKey().contains("maven/salut/src/main/java/java/Foo2.java"))
			.findFirst()
			.orElseThrow(IllegalStateException::new)
			.getValue();
		assertEquals(1, textEdits.size());

		assertEquals("package java;\n"
			+ "\n"
			+ "import org.apache.commons.lang3.StringUtils;\n"
			+ "\n"
			+ "/**\n"
			+ " * This is foo\n"
			+ " */\n"
			+ "public class Foo111 {\n"
			+ "\n"
			+ "\tpublic static void main(String[] args) {\n"
			+ "      System.out.print( StringUtils.capitalize(\"Hello world! from \"+Foo111.class));\n"
			+ "\t}\n"
			+ "\n"
			+ "\tpublic void linkedFromFoo2() {\n"
			+ "\n"
			+ "\t}\n"
			+ "}",
			TextEditUtil.apply(sourceCode, edit.getChanges().get(currentUri)));
	}

	@Test
	public void testRenameClassFileWithResourceChangesSupported() {
		when(clientPreferences.isWorkspaceEditResourceChangesSupported()).thenReturn(true);

		String currentUri = JDTUtils.toURI(cu);
		String newName = "Foo111.java";
		WorkspaceEdit edit = handler.rename(new RenameClassFileParams(currentUri, newName), monitor);
		assertEquals(0, edit.getChanges().size());
		assertNull(edit.getDocumentChanges());
		assertEquals(4, edit.getResourceChanges().size());

		Either<ResourceChange, TextDocumentEdit> change0 = edit.getResourceChanges().get(0);
		assertTrue(change0.isRight());
		assertTrue(change0.getRight().getTextDocument().getUri().contains("maven/salut/src/main/java/java/Foo.java"));
		assertEquals(1, change0.getRight().getEdits().size());
		assertEquals("Foo111", change0.getRight().getEdits().get(0).getNewText());
		assertEquals(7, change0.getRight().getEdits().get(0).getRange().getStart().getLine());
		assertEquals(13, change0.getRight().getEdits().get(0).getRange().getStart().getCharacter());
		assertEquals(7, change0.getRight().getEdits().get(0).getRange().getEnd().getLine());
		assertEquals(16, change0.getRight().getEdits().get(0).getRange().getEnd().getCharacter());

		Either<ResourceChange, TextDocumentEdit> change1 = edit.getResourceChanges().get(1);
		assertTrue(change1.isRight());
		assertTrue(change1.getRight().getTextDocument().getUri().contains("maven/salut/src/main/java/java/Foo.java"));
		assertEquals(1, change1.getRight().getEdits().size());
		assertEquals("Foo111", change1.getRight().getEdits().get(0).getNewText());
		assertEquals(10, change1.getRight().getEdits().get(0).getRange().getStart().getLine());
		assertEquals(68, change1.getRight().getEdits().get(0).getRange().getStart().getCharacter());
		assertEquals(10, change1.getRight().getEdits().get(0).getRange().getEnd().getLine());
		assertEquals(71, change1.getRight().getEdits().get(0).getRange().getEnd().getCharacter());

		Either<ResourceChange, TextDocumentEdit> change2 = edit.getResourceChanges().get(2);
		assertTrue(change2.isRight());
		assertTrue(change2.getRight().getTextDocument().getUri().contains("maven/salut/src/main/java/java/Foo2.java"));
		assertEquals(1, change2.getRight().getEdits().size());
		assertEquals("Foo111", change2.getRight().getEdits().get(0).getNewText());
		assertEquals(27, change2.getRight().getEdits().get(0).getRange().getStart().getLine());
		assertEquals(11, change2.getRight().getEdits().get(0).getRange().getStart().getCharacter());
		assertEquals(27, change2.getRight().getEdits().get(0).getRange().getEnd().getLine());
		assertEquals(14, change2.getRight().getEdits().get(0).getRange().getEnd().getCharacter());

		Either<ResourceChange, TextDocumentEdit> change3 = edit.getResourceChanges().get(3);
		assertTrue(change3.isLeft());
		assertEquals(currentUri, change3.getLeft().getCurrent());
		assertTrue(change3.getLeft().getNewUri().contains("maven/salut/src/main/java/java/Foo111.java"));
	}

	@Test
	public void testRenameResourceFileWhichIsNotSupported() {
		when(clientPreferences.isWorkspaceEditResourceChangesSupported()).thenReturn(true);

		IPackageFragmentRoot resourcesRoot = project.getPackageFragmentRoot(
			project.getProject().getFolder("src/main/resources"));
		String currentUri = ResourceUtils.fixURI(resourcesRoot.getResource().getLocationURI().resolve("test.properties"));
		String newName = "new.properties";
		WorkspaceEdit edit = handler.rename(new RenameClassFileParams(currentUri, newName), monitor);
		assertEquals(0, edit.getChanges().size());
		assertNull(edit.getDocumentChanges());
		assertNull(null, edit.getResourceChanges());
	}
}