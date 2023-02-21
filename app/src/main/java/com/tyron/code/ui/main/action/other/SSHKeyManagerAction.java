package com.tyron.code.ui.main.action.other;

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
import com.tyron.code.R;
import com.tyron.code.ui.ssh.SshKeyManagerFragment;

public class SSHKeyManagerAction extends AnAction {

  public static final String ID = "sSHKeyManagerAction";

  @Override
  public void update(@NonNull AnActionEvent event) {
    Presentation presentation = event.getPresentation();
    Context context = event.getDataContext();

    presentation.setVisible(false);
    if (!ActionPlaces.MAIN_TOOLBAR.equals(event.getPlace())) {
      return;
    }

    presentation.setText(context.getString(R.string.ssh_key_manager));
    presentation.setVisible(true);
    presentation.setEnabled(true);
  }

  @Override
  public void actionPerformed(@NonNull AnActionEvent e) {
    Context context = e.getRequiredData(CommonDataKeys.CONTEXT);
    context = getActivityContext(context);

    if (context instanceof AppCompatActivity) {
      FragmentManager fragmentManager = ((AppCompatActivity) context).getSupportFragmentManager();
      Fragment fragment = SshKeyManagerFragment.newInstance();
      fragmentManager
          .beginTransaction()
          .add(R.id.fragment_container, fragment, SshKeyManagerFragment.TAG)
          .addToBackStack(SshKeyManagerFragment.TAG)
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
