package com.tyron.code.ui.main.action.project;

import androidx.annotation.NonNull;
import com.tyron.actions.ActionPlaces;
import com.tyron.actions.AnAction;
import com.tyron.actions.AnActionEvent;
import com.tyron.actions.CommonDataKeys;
import android.content.Context;
import com.tyron.actions.Presentation;
import com.tyron.builder.project.Project;
import com.tyron.code.R;
import com.tyron.code.ui.main.MainFragment;
import com.tyron.code.ui.main.MainViewModel;
import com.tyron.code.ApplicationLoader;
import com.tyron.code.tasks.git.GitTask;

public class GitAction extends AnAction {
	public static Context context;
	private Project project;
    @Override
    public void update(@NonNull AnActionEvent event) {
        Presentation presentation = event.getPresentation();
        presentation.setVisible(false);
			
        if (!ActionPlaces.MAIN_TOOLBAR.equals(event.getPlace())) {
            return;
        }

         project = event.getData(CommonDataKeys.PROJECT);
        if (project == null) {
            return;
        }

        MainViewModel mainViewModel = event.getData(MainFragment.MAIN_VIEW_MODEL_KEY);
        if (mainViewModel == null) {
            return;
        }

        presentation.setVisible(true);
        presentation.setText("Git");
    }

	@Override
	public void actionPerformed(@NonNull AnActionEvent e) {
		Context context = e.getData(CommonDataKeys.CONTEXT);
        GitTask.INSTANCE.showTasks((Context) context, project);
	}
	}
	
