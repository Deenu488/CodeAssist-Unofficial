package com.tyron.code.ui.main.action.compile;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.tyron.actions.ActionGroup;
import com.tyron.actions.ActionPlaces;
import com.tyron.actions.AnAction;
import com.tyron.actions.AnActionEvent;
import com.tyron.actions.CommonDataKeys;
import com.tyron.code.R;
import com.tyron.code.ui.main.CompileCallback;
import com.tyron.code.ui.main.MainFragment;
import com.tyron.builder.project.Project;

import java.util.ArrayList;
import java.util.List;
import com.tyron.code.ui.main.MainViewModel;
import androidx.annotation.CallSuper;

public class CompileActionGroup extends ActionGroup {

    public static final String ID = "compileActionGroup";

    @Override
    public void update(@NonNull AnActionEvent event) {
        CompileCallback data = event.getData(MainFragment.COMPILE_CALLBACK_KEY);
        if (data == null) {
            event.getPresentation().setVisible(false);
            return;
        }

        if (!ActionPlaces.MAIN_TOOLBAR.equals(event.getPlace())) {
            event.getPresentation().setVisible(false);
            return;
        }

        Context context = event.getData(CommonDataKeys.CONTEXT);
        if (context == null) {
            event.getPresentation().setVisible(false);
            return;
        }

        event.getPresentation().setVisible(true);
        event.getPresentation().setEnabled(false);
        event.getPresentation().setText(context.getString(R.string.menu_run));
        event.getPresentation().setIcon(ContextCompat.getDrawable(context, R.drawable.round_play_arrow_24));

		MainViewModel mainViewModel = event.getData(MainFragment.MAIN_VIEW_MODEL_KEY);
        if (mainViewModel == null) {		
			return; 
		}
		Boolean indexing = mainViewModel.isIndexing().getValue();
        if (indexing == null) {
            indexing = false;
        }

		Project project = event.getData(CommonDataKeys.PROJECT);
        if (project == null) {
            return;
        }

		if (project != null && !project.isCompiling() && !indexing) {      
			event.getPresentation().setEnabled(true);	
		}
	}

	@CallSuper
    @Override
    public void actionPerformed(@NonNull AnActionEvent e) {
    }	

    @Override
    public AnAction[] getChildren(@Nullable AnActionEvent e) {
		return new AnAction[]{};
    }
}
