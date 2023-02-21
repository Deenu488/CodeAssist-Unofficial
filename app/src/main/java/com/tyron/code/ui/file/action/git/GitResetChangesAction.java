package com.tyron.code.ui.file.action.git;

import android.content.Context;
import androidx.annotation.NonNull;
import com.tyron.actions.AnActionEvent;
import com.tyron.actions.CommonDataKeys;
import com.tyron.builder.project.Project;
import com.tyron.code.R;
import com.tyron.code.tasks.git.ResetChangesTask;
import com.tyron.code.ui.file.CommonFileKeys;
import com.tyron.code.ui.file.action.FileAction;
import com.tyron.code.ui.file.tree.TreeFileManagerFragment;
import com.tyron.code.ui.file.tree.model.TreeFile;
import com.tyron.ui.treeview.TreeNode;
import com.tyron.ui.treeview.TreeView;
import java.io.File;

public class GitResetChangesAction extends FileAction {
  public static final String ID = "fileManagerGitResetChangesAction";
  private Project project;
  private Context context;

  @Override
  public String getTitle(Context context) {
    return context.getString(R.string.menu_git_reset_changes);
  }

  @Override
  public boolean isApplicable(File file) {
    return true;
  }

  @Override
  public void actionPerformed(@NonNull AnActionEvent e) {
    project = e.getData(CommonDataKeys.PROJECT);
    if (project == null) {
      return;
    }
    context = e.getData(CommonDataKeys.CONTEXT);
    TreeFileManagerFragment fragment =
        (TreeFileManagerFragment) e.getRequiredData(CommonDataKeys.FRAGMENT);
    TreeView<TreeFile> treeView = fragment.getTreeView();
    TreeNode<TreeFile> currentNode = e.getRequiredData(CommonFileKeys.TREE_NODE);

    File rootProject = project.getRootFile();
    File currentFile = currentNode.getValue().getFile();
    String path =
        currentFile
            .getAbsolutePath()
            .substring((rootProject.getAbsolutePath() + "/app").lastIndexOf("/") + 1);

    ResetChangesTask.INSTANCE.reset(project, path, context);
  }
}
