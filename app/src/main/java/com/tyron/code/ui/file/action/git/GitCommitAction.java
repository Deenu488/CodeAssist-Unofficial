package com.tyron.code.ui.file.action.git;

import android.content.Context;
import androidx.annotation.NonNull;
import com.tyron.actions.AnActionEvent;
import com.tyron.actions.CommonDataKeys;
import com.tyron.builder.project.Project;
import com.tyron.code.R;
import com.tyron.code.tasks.git.GitCommitTask;
import com.tyron.code.ui.file.action.FileAction;
import java.io.File;

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
