package com.tyron.code.ui.main.action.project;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import com.tyron.actions.ActionPlaces;
import com.tyron.actions.AnAction;
import com.tyron.actions.AnActionEvent;
import com.tyron.actions.CommonDataKeys;
import com.tyron.actions.Presentation;
import com.tyron.builder.project.Project;
import com.tyron.code.ui.editor.impl.text.rosemoe.CodeEditorFragment;
import com.tyron.code.ui.main.MainFragment;
import com.tyron.code.ui.main.MainViewModel;
import com.tyron.fileeditor.api.FileEditor;

public class SearchAction extends AnAction {

  @Override
  public void update(@NonNull AnActionEvent event) {
    Presentation presentation = event.getPresentation();
    presentation.setVisible(false);

    if (!ActionPlaces.MAIN_TOOLBAR.equals(event.getPlace())) {
      return;
    }
    FileEditor fileEditor = event.getData(CommonDataKeys.FILE_EDITOR_KEY);
    if (fileEditor == null) {
      return;
    }

    Project project = event.getData(CommonDataKeys.PROJECT);
    if (project == null) {
      return;
    }

    MainViewModel mainViewModel = event.getData(MainFragment.MAIN_VIEW_MODEL_KEY);
    if (mainViewModel == null) {
      return;
    }

    presentation.setVisible(true);
    presentation.setText("Search");
  }

  @Override
  public void actionPerformed(@NonNull AnActionEvent e) {
    Context context = e.getRequiredData(CommonDataKeys.CONTEXT);
    context = getActivityContext(context);

    FileEditor fileEditor = e.getRequiredData(CommonDataKeys.FILE_EDITOR_KEY);
    Fragment fragment = fileEditor.getFragment();
    if (fragment instanceof CodeEditorFragment) {
      ((CodeEditorFragment) fragment).search();
    }
  }

  private Context getActivityContext(Context context) {
    Context current = context;
    while (current != null) {
      if (current instanceof Activity) {
        return current;
      }
      if (current instanceof ContextWrapper) {
        current = ((ContextWrapper) current).getBaseContext();
      } else {
        current = null;
      }
    }
    return null;
  }
}
