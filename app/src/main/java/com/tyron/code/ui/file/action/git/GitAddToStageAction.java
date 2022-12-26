package com.tyron.code.ui.file.action.git;

import com.tyron.code.ui.file.action.FileAction;
import android.content.Context;
import java.io.File;
import com.tyron.actions.AnActionEvent;
import androidx.annotation.NonNull;
import com.tyron.code.R;
import com.tyron.code.tasks.git.AddToStageTask;

public class GitAddToStageAction extends FileAction {

    public static final String ID = "fileManagerGitAddToStageAction";

    @Override
    public String getTitle(Context context) {
        return context.getString(R.string.menu_git_add_to_stage);
    }

    @Override
    public boolean isApplicable(File file) {
        return true;
    }

    @Override
    public void actionPerformed(@NonNull AnActionEvent e) {
        AddToStageTask.INSTANCE.add();
    }
}

    
 
