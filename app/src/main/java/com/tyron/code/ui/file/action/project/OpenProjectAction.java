package com.tyron.code.ui.file.action.project;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.tyron.actions.AnActionEvent;
import com.tyron.actions.CommonDataKeys;
import com.tyron.builder.project.api.FileManager;
import com.tyron.code.ui.editor.impl.FileEditorManagerImpl;
import com.tyron.code.ui.file.tree.TreeUtil;
import com.tyron.completion.progress.ProgressManager;
import com.tyron.ui.treeview.TreeNode;
import com.tyron.ui.treeview.TreeView;
import com.tyron.code.ui.file.CommonFileKeys;
import com.tyron.code.ui.file.tree.TreeFileManagerFragment;
import com.tyron.code.ui.file.tree.model.TreeFile;
import com.tyron.code.ui.project.ProjectManager;
import com.tyron.builder.project.api.JavaModule;
import com.tyron.builder.project.api.Module;
import com.tyron.code.R;
import com.tyron.code.ui.file.action.FileAction;
import com.tyron.common.util.StringSearch;
import com.tyron.common.SharedPreferenceKeys;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import com.tyron.actions.Presentation;
import com.tyron.builder.project.Project;

import kotlin.io.FileWalkDirection;
import kotlin.io.FilesKt;
import com.tyron.code.ui.main.MainFragment;
import android.content.SharedPreferences;
import com.tyron.code.ApplicationLoader;

public class OpenProjectAction extends FileAction {

    public static final String ID = "fileManagerOpenProjectAction";
	private Project project;
	SharedPreferences sharedPreferences = ApplicationLoader.getDefaultPreferences();

    @Override
    public String getTitle(Context context) {
        return context.getString(R.string.open_project);
    }

    @Override
    public boolean isApplicable(File file) {
        return false;
    }

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

		Context context = event.getData(CommonDataKeys.CONTEXT);
        if (context == null) {
            event.getPresentation().setVisible(false);
            return;
        }

        TreeFileManagerFragment fragment =
            (TreeFileManagerFragment) event.getRequiredData(CommonDataKeys.FRAGMENT);
        TreeView<TreeFile> treeView = fragment.getTreeView();
        TreeNode<TreeFile> currentNode = event.getRequiredData(CommonFileKeys.TREE_NODE);

        File currentFile = currentNode.getValue().getFile();
		File main = new File(currentFile, "src/main");

		String RootName  = sharedPreferences.getString(SharedPreferenceKeys.SAVED_PROJECT_ROOT_NAME, "");

	    if (main.isDirectory() || main.exists()) {
			if (currentFile.getName().equals(RootName)) {
				presentation.setVisible(false);     
			}  else {
				presentation.setVisible(true);     	
			} 
		}

		presentation.setText(getTitle(fragment.requireContext()));
	}


    @Override
    public void actionPerformed(@NonNull AnActionEvent e) {
        TreeFileManagerFragment fragment =
			(TreeFileManagerFragment) e.getRequiredData(CommonDataKeys.FRAGMENT);
        TreeView<TreeFile> treeView = fragment.getTreeView();
        TreeNode<TreeFile> currentNode = e.getRequiredData(CommonFileKeys.TREE_NODE);

	}
}
