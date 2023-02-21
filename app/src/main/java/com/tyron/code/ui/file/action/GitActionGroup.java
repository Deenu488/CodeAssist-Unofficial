package com.tyron.code.ui.file.action;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.tyron.actions.ActionGroup;
import com.tyron.actions.AnAction;
import com.tyron.actions.AnActionEvent;
import com.tyron.actions.CommonDataKeys;
import com.tyron.actions.Presentation;
import com.tyron.builder.project.Project;
import com.tyron.code.R;
import com.tyron.code.ui.file.CommonFileKeys;
import com.tyron.code.ui.file.action.git.GitAddToStageAction;
import com.tyron.code.ui.file.action.git.GitCommitAction;
import com.tyron.code.ui.file.action.git.GitRemoveFromIndexAction;
import com.tyron.code.ui.file.action.git.GitRemoveFromIndexForceAction;
import com.tyron.code.ui.file.action.git.GitResetChangesAction;
import com.tyron.code.ui.file.tree.TreeFileManagerFragment;
import com.tyron.code.ui.file.tree.model.TreeFile;
import com.tyron.ui.treeview.TreeNode;
import com.tyron.ui.treeview.TreeView;
import java.io.File;

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
    TreeFileManagerFragment fragment =
        (TreeFileManagerFragment) event.getRequiredData(CommonDataKeys.FRAGMENT);
    TreeView<TreeFile> treeView = fragment.getTreeView();
    TreeNode<TreeFile> currentNode = event.getRequiredData(CommonFileKeys.TREE_NODE);

    String path;
    File rootProject = project.getRootFile();
    File gitConfig = new File(rootProject + "/.git/config");
    File currentFile = currentNode.getValue().getFile();

    if (currentFile.isDirectory()) {
      path =
          currentFile
              .getAbsolutePath()
              .substring((rootProject.getAbsolutePath() + "/app").lastIndexOf("/"));
      if (path.startsWith(".git")) {
        presentation.setVisible(false);
      } else if (path.isEmpty()) {
        presentation.setVisible(false);
      }
    } else if (gitConfig.exists()) {
      presentation.setVisible(true);
    }
    presentation.setText(event.getDataContext().getString(R.string.menu_git));
  }

  @Override
  public AnAction[] getChildren(@Nullable AnActionEvent e) {
    return new AnAction[] {
      new GitAddToStageAction(),
      new GitCommitAction(),
      new GitRemoveFromIndexAction(),
      new GitRemoveFromIndexForceAction(),
      new GitResetChangesAction()
    };
  }
}
