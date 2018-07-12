package org.eclipse.jdt.ls.core.internal.handlers;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.eclipse.jdt.ls.core.internal.WorkspaceIdentifier;
import org.eclipse.jdt.ls.core.internal.handlers.ResolveMainClassHandler.ResolutionItem;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
    public void testResolveMainClassHandler() throws Exception {
        List<ResolutionItem> items = handler.resolveMainClass(new WorkspaceIdentifier(), monitor);
        assertFalse(items.isEmpty());
        assertEquals("java.Foo", items.get(0).getMainClass());
        assertEquals("salut", items.get(0).getProjectName());
        assertTrue(items.get(0).getFilePath().contains("maven/salut/src/main/java/java/Foo.java"));
    }
}
