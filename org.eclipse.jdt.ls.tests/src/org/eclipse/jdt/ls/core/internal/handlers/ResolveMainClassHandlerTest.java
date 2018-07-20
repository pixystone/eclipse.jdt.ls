package org.eclipse.jdt.ls.core.internal.handlers;

import java.util.List;

import org.eclipse.jdt.ls.core.internal.WorkspaceIdentifier;
import org.eclipse.jdt.ls.core.internal.handlers.ResolveMainClassHandler.ResolutionItem;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Pixy Yuan
 * on 2018/7/12
 */
@RunWith(MockitoJUnitRunner.class)
public class ResolveMainClassHandlerTest extends AbstractCompilationUnitBasedTest {

    ResolveMainClassHandler handler;

    @Before
    public void setUp() throws Exception {
        importProjects("maven/salut");
        handler = new ResolveMainClassHandler();
    }

    @Test
    public void testResolveMainClassHandler() {
        List<ResolutionItem> items = handler.resolveMainClass(new WorkspaceIdentifier(), monitor);
        assertEquals(2, items.size());
        assertTrue(items.stream().anyMatch(i -> "java.Foo".equals(i.getMainClass())));
        assertTrue(items.stream().anyMatch(i -> "salut".equals(i.getProjectName())));
        assertTrue(items.stream().anyMatch(i -> i.getFilePath().endsWith("maven/salut/src/main/java/java/Foo.java")));
    }
}
