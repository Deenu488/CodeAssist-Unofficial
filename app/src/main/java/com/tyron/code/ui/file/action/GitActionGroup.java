package com.tyron.code.ui.file.action;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.tyron.actions.CommonDataKeys;
import com.tyron.actions.ActionGroup;
import com.tyron.actions.AnAction;
import com.tyron.actions.AnActionEvent;
import com.tyron.actions.Presentation;
import com.tyron.code.R;
import com.tyron.code.ui.file.CommonFileKeys;
import com.tyron.code.ui.file.tree.model.TreeFile;
import com.tyron.ui.treeview.TreeNode;
import com.tyron.code.ui.file.action.git.GitAddToStageAction;
import com.tyron.code.ui.file.action.git.GitResetChangesAction;
import com.tyron.builder.project.Project;
import java.io.File;
import com.tyron.code.ui.file.action.git.GitRemoveFromIndex;

public class GitActionGroup extends ActionGroup {

    public static final String ID = "fileManagerGitGroup";
    private Project project;

    @Override
    public void update(@NonNull AnActionEvent event) {
        Presentation presentation = event.getPresentation();
        presentation.setVisible(false);

        TreeNode<TreeFile> data = event.getData(CommonFileKeys.TREE_NODE);
        if (data == null) {
            return;
        }

        project = event.getData(CommonDataKeys.PROJECT);
        if (project == null) {
            return;
        }
        File gitConfig = new File(project.getRootFile(), ".git/config");
        if (gitConfig.exists()) {
            presentation.setVisible(true);
        }
        presentation.setText(event.getDataContext().getString(R.string.menu_git));
    }

    @Override
    public AnAction[] getChildren(@Nullable AnActionEvent e) {
        return new AnAction[] { new GitAddToStageAction(), new GitResetChangesAction(), new GitRemoveFromIndex()};
    }
}
