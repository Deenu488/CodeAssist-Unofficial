package com.tyron.code.ui.main.action.other;

import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import com.tyron.actions.ActionPlaces;
import com.tyron.actions.AnAction;
import com.tyron.actions.AnActionEvent;
import com.tyron.actions.CommonDataKeys;
import com.tyron.builder.project.Project;
import com.tyron.code.ApplicationLoader;
import com.tyron.code.ui.editor.impl.text.rosemoe.CodeEditorFragment;
import com.tyron.code.ui.main.MainFragment;
import com.tyron.code.ui.main.MainViewModel;
import com.tyron.fileeditor.api.FileEditor;
import org.codeassist.unofficial.R;

public class FormatAction extends AnAction {

  public static final String ID = "formatAction";

  @Override
  public void update(@NonNull AnActionEvent event) {
    event.getPresentation().setVisible(false);
    if (!ActionPlaces.MAIN_TOOLBAR.equals(event.getPlace())) {
      return;
    }

    FileEditor fileEditor = event.getData(CommonDataKeys.FILE_EDITOR_KEY);

    if (fileEditor == null) {
      event.getPresentation().setVisible(false);
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

    event.getPresentation().setVisible(true);
    event.getPresentation().setText(event.getDataContext().getString(R.string.menu_format));
  }

  @Override
  public void actionPerformed(@NonNull AnActionEvent e) {
    FileEditor fileEditor = e.getRequiredData(CommonDataKeys.FILE_EDITOR_KEY);
    Fragment fragment = fileEditor.getFragment();

    SharedPreferences sharedPreferences = ApplicationLoader.getDefaultPreferences();
    boolean format_all_java = sharedPreferences.getBoolean("format_all_java", false);
    boolean format_all_kotlin = sharedPreferences.getBoolean("format_all_kotlin", false);

    if (!format_all_java && !format_all_kotlin) {

      if (fragment instanceof CodeEditorFragment) {
        ((CodeEditorFragment) fragment).format();
      }
    }

    Project project = e.getData(CommonDataKeys.PROJECT);
    if (project == null) {
      return;
    }

    if (format_all_java || format_all_kotlin) {
      if (fragment instanceof CodeEditorFragment) {
        ((CodeEditorFragment) fragment).format();
      }
    }
  }
}
