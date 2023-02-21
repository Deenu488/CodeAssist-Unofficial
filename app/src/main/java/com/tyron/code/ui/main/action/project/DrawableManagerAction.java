package com.tyron.code.ui.main.action.project;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import com.tyron.actions.ActionPlaces;
import com.tyron.actions.AnAction;
import com.tyron.actions.AnActionEvent;
import com.tyron.actions.CommonDataKeys;
import com.tyron.actions.Presentation;
import com.tyron.builder.project.Project;
import com.tyron.code.R;
import com.tyron.code.ui.drawable.manager.DrawableManagerFragment;
import com.tyron.code.ui.main.MainFragment;
import com.tyron.code.ui.main.MainViewModel;

public class DrawableManagerAction extends AnAction {

  private Project project;

  @Override
  public void update(@NonNull AnActionEvent event) {
    Presentation presentation = event.getPresentation();
    presentation.setVisible(false);
    Context context = event.getData(CommonDataKeys.CONTEXT);
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
    presentation.setText(context.getString(R.string.menu_drawable_manager));
  }

  @Override
  public void actionPerformed(@NonNull AnActionEvent e) {
    Context context = e.getRequiredData(CommonDataKeys.CONTEXT);
    context = getActivityContext(context);

    if (context instanceof AppCompatActivity) {
      FragmentManager fragmentManager = ((AppCompatActivity) context).getSupportFragmentManager();
      Fragment fragment = DrawableManagerFragment.newInstance();
      fragmentManager
          .beginTransaction()
          .add(R.id.fragment_container, fragment, DrawableManagerFragment.TAG)
          .addToBackStack(DrawableManagerFragment.TAG)
          .commit();
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
