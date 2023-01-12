package com.tyron.code.ui.file.action.git;

import com.tyron.code.ui.file.action.FileAction;
import android.content.Context;
import java.io.File;
import com.tyron.actions.AnActionEvent;
import androidx.annotation.NonNull;
import com.tyron.code.R;
import com.tyron.builder.project.Project;
import com.tyron.actions.CommonDataKeys;
import com.tyron.code.ui.file.CommonFileKeys;
import com.tyron.code.ui.file.tree.model.TreeFile;
import com.tyron.ui.treeview.TreeNode;
import com.tyron.ui.treeview.TreeView;
import com.tyron.code.ui.file.tree.TreeFileManagerFragment;
import com.tyron.code.tasks.git.GitCommitTask;

public class GitCommitAction extends FileAction {

    public static final String ID = "fileManagerGitCommitAction";
    private Project project;
    private Context context;
    @Override
    public String getTitle(Context context) {
        return context.getString(R.string.menu_commit);
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
        GitCommitTask.INSTANCE.commit(project, context);
 
    }
}

    
 
