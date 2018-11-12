package org.eclipse.jdt.ls.core.internal.handlers;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.ls.core.internal.ChangeUtil;
import org.eclipse.jdt.ls.core.internal.JDTUtils;
import org.eclipse.jdt.ls.core.internal.JavaLanguageServerPlugin;
import org.eclipse.jdt.ls.core.internal.corext.refactoring.rename.RenameSupport;
import org.eclipse.jdt.ls.core.internal.preferences.PreferenceManager;
import org.eclipse.lsp4j.WorkspaceEdit;
import org.eclipse.ltk.core.refactoring.CheckConditionsOperation;
import org.eclipse.ltk.core.refactoring.CreateChangeOperation;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

/**
 * @author Pixy Yuan
 * on 2018/11/12
 */
public class RenameClassFileHandler {

	private PreferenceManager preferenceManager;

	public RenameClassFileHandler(PreferenceManager preferenceManager) {
		this.preferenceManager = preferenceManager;
	}

	public WorkspaceEdit rename(RenameClassFileParams params, IProgressMonitor monitor) {
		WorkspaceEdit edit = new WorkspaceEdit();
		if (!preferenceManager.getPreferences().isRenameEnabled()) {
			return edit;
		}
		try {
			final ICompilationUnit unit = JDTUtils.resolveCompilationUnit(params.getCurrentUri());
			if (unit != null) {
				RenameSupport renameSupport = RenameSupport.create(
					unit, params.getNewName(), RenameSupport.UPDATE_REFERENCES);
				Refactoring refactoring = renameSupport
					.getRenameRefactoring();

				CreateChangeOperation create = new CreateChangeOperation(
					new CheckConditionsOperation(
						refactoring, CheckConditionsOperation.ALL_CONDITIONS),
					RefactoringStatus.FATAL);
				create.run(monitor);
				ChangeUtil.convertChanges(create.getChange(), edit);
			}
		} catch (CoreException | NullPointerException ex) {
			JavaLanguageServerPlugin.logException("Problem with classfile rename for " + params.getCurrentUri(), ex);
		}

		return edit;
	}

}
