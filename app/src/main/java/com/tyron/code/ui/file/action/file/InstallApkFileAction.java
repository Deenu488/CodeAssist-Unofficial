package com.tyron.code.ui.file.action.file;

import android.content.Context;
import androidx.annotation.NonNull;
import com.tyron.actions.AnActionEvent;
import com.tyron.actions.CommonDataKeys;
import com.tyron.code.ui.file.CommonFileKeys;
import com.tyron.code.ui.file.action.FileAction;
import com.tyron.code.ui.file.tree.TreeFileManagerFragment;
import com.tyron.code.ui.file.tree.model.TreeFile;
import com.tyron.code.util.ApkInstaller;
import com.tyron.ui.treeview.TreeNode;
import com.tyron.ui.treeview.TreeView;
import java.io.File;
import org.codeassist.unofficial.R;

public class InstallApkFileAction extends FileAction {

  public static final String ID = "fileManagerInstallApkFileAction";

  @Override
  public String getTitle(Context context) {
    return context.getString(R.string.install);
  }

  @Override
  public boolean isApplicable(File file) {
    return file.getName().endsWith(".apk");
  }

  @Override
  public void actionPerformed(@NonNull AnActionEvent e) {
    TreeFileManagerFragment fragment =
        (TreeFileManagerFragment) e.getRequiredData(CommonDataKeys.FRAGMENT);
    TreeView<TreeFile> treeView = fragment.getTreeView();
    TreeNode<TreeFile> currentNode = e.getRequiredData(CommonFileKeys.TREE_NODE);

    ApkInstaller.installApplication(
        fragment.requireContext(), currentNode.getValue().getFile().getAbsolutePath());
  }
}
