package com.tyron.code.ui.file.action.git;

import com.tyron.code.ui.file.action.FileAction;
import android.content.Context;
import java.io.File;
import com.tyron.actions.AnActionEvent;
import androidx.annotation.NonNull;
import com.tyron.code.R;
import com.tyron.code.tasks.git.RemoveFromIndexTask;

public class GitRemoveFromIndex extends FileAction {

    public static final String ID = "fileManagerGitRemoveFromIndex";

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
        RemoveFromIndexTask.INSTANCE.remove();
    }
}
