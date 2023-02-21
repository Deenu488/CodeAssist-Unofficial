package com.tyron.code.ui.file.action.git;

import android.content.Context;
import androidx.annotation.NonNull;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.tyron.actions.AnActionEvent;
import com.tyron.actions.CommonDataKeys;
import com.tyron.builder.project.Project;
import com.tyron.code.R;
import com.tyron.code.tasks.git.RemoveFromIndexTask;
import com.tyron.code.ui.file.CommonFileKeys;
import com.tyron.code.ui.file.action.FileAction;
import com.tyron.code.ui.file.tree.TreeFileManagerFragment;
import com.tyron.code.ui.file.tree.model.TreeFile;
import com.tyron.ui.treeview.TreeNode;
import com.tyron.ui.treeview.TreeView;
import java.io.File;

public class GitRemoveFromIndexAction extends FileAction {

  public static final String ID = "fileManagerGitRemoveFromIndexAction";
  private Project project;
  private Context context;

  @Override
  public String getTitle(Context context) {
    return context.getString(R.string.menu_git_remove_from_index);
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
    String name = currentFile.getName();
    String message = context.getString(R.string.remove_from_index_msg, name);

    new MaterialAlertDialogBuilder(context)
        .setTitle(R.string.remove_from_index)
        .setMessage(message)
        .setPositiveButton(
            R.string.remove,
            (d, w) -> RemoveFromIndexTask.INSTANCE.remove(project, path, name, context))
        .setNegativeButton(android.R.string.cancel, null)
        .show();
  }
}
