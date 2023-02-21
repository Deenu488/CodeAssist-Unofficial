package com.tyron.code.ui.main;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.transition.MaterialSharedAxis;
import com.tyron.builder.project.Project;
import com.tyron.code.BuildConfig;
import com.tyron.code.R;
import com.tyron.code.tasks.git.GitCloneTask;
import com.tyron.code.ui.project.ProjectSheetFragment;
import com.tyron.code.ui.settings.SettingsActivity;
import com.tyron.code.ui.wizard.WizardFragment;
import com.tyron.common.SharedPreferenceKeys;
import java.io.File;

public class HomeFragment extends Fragment {

  public static final String TAG = HomeFragment.class.getSimpleName();
  private MaterialButton create_new_project,
      clone_git_repository,
      open_project_manager,
      configure_settings;
  private SharedPreferences mPreferences;
  private boolean mShowDialogOnPermissionGrant;
  private ActivityResultLauncher<String[]> mPermissionLauncher;
  private final ActivityResultContracts.RequestMultiplePermissions mPermissionsContract =
      new ActivityResultContracts.RequestMultiplePermissions();
  private TextView app_version;

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setExitTransition(new MaterialSharedAxis(MaterialSharedAxis.X, false));
    mPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext());
    mPermissionLauncher =
        registerForActivityResult(
            mPermissionsContract,
            isGranted -> {
              if (isGranted.containsValue(false)) {
                new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.project_manager_permission_denied)
                    .setMessage(R.string.project_manager_android11_notice)
                    .setPositiveButton(
                        R.string.project_manager_button_request_again,
                        (d, which) -> {
                          mShowDialogOnPermissionGrant = true;
                          requestPermissions();
                        })
                    .setNegativeButton(
                        R.string.project_manager_button_continue,
                        (d, which) -> {
                          mShowDialogOnPermissionGrant = false;
                          setSavePath(
                              requireContext().getExternalFilesDir("/Projects").getAbsolutePath());
                        })
                    .show();
                setSavePath(requireContext().getExternalFilesDir("/Projects").getAbsolutePath());
              } else {
                if (mShowDialogOnPermissionGrant) {
                  mShowDialogOnPermissionGrant = false;
                  savePath();
                }
              }
            });
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
    toolbar.setTitle(R.string.welcome_message);
    toolbar.setTitleCentered(true);
    create_new_project = view.findViewById(R.id.createNewProject);
    clone_git_repository = view.findViewById(R.id.gitCloneRepo);
    open_project_manager = view.findViewById(R.id.openProjectManager);
    configure_settings = view.findViewById(R.id.configureSettings);
    app_version = view.findViewById(R.id.app_version);
    String versionName = String.valueOf(BuildConfig.VERSION_NAME);
    app_version.setText("Version " + versionName);
    create_new_project.setOnClickListener(
        v -> {
          WizardFragment wizardFragment = new WizardFragment();
          wizardFragment.setOnProjectCreatedListener(this::openProject);
          getParentFragmentManager()
              .beginTransaction()
              .replace(R.id.fragment_container, wizardFragment)
              .addToBackStack(null)
              .commit();
        });

    clone_git_repository.setOnClickListener(
        v -> {
          GitCloneTask.INSTANCE.clone((Context) requireContext());
        });

    open_project_manager.setOnClickListener(
        v -> {
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            showProjectManager();
          } else {
            showProjectManager();
          }
        });

    configure_settings.setOnClickListener(
        v -> {
          Intent intent = new Intent();
          intent.setClass(requireActivity(), SettingsActivity.class);
          startActivity(intent);
        });
  }

  private void showProjectManager() {
    ProjectSheetFragment projectSheetFragment = new ProjectSheetFragment();
    projectSheetFragment.show(getFragmentManager(), ProjectSheetFragment.TAG);
  }

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.home_fragment, container, false);
  }

  @Override
  public void onResume() {
    super.onResume();
    checkSavePath();
  }

  private void checkSavePath() {
    String path = mPreferences.getString(SharedPreferenceKeys.PROJECT_SAVE_PATH, null);
    if (path == null && Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
      if (permissionsGranted()) {
        savePath();
      } else if (shouldShowRequestPermissionRationale()) {
        if (shouldShowRequestPermissionRationale()) {
          new MaterialAlertDialogBuilder(requireContext())
              .setMessage(R.string.project_manager_permission_rationale)
              .setPositiveButton(
                  R.string.project_manager_button_allow,
                  (d, which) -> {
                    mShowDialogOnPermissionGrant = true;
                    requestPermissions();
                  })
              .setNegativeButton(
                  R.string.project_manager_button_use_internal,
                  (d, which) ->
                      setSavePath(Environment.getExternalStorageDirectory().getAbsolutePath()))
              .setTitle(R.string.project_manager_rationale_title)
              .show();
        }
      } else {
        requestPermissions();
      }
    } else {
    }
  }

  private void setSavePath(String path) {
    mPreferences.edit().putString(SharedPreferenceKeys.PROJECT_SAVE_PATH, path).apply();
  }

  private void savePath() {
    String path =
        Environment.getExternalStorageDirectory().getAbsolutePath() + "/CodeAssistProjects";
    File file = new File(path);
    if (file.exists()) {
    } else {
      file.mkdirs();
    }
    setSavePath(path);
  }

  private void openProject(Project project) {
    MainFragment fragment =
        MainFragment.newInstance(project.getRootFile().getAbsolutePath(), "app");
    getParentFragmentManager()
        .beginTransaction()
        .replace(R.id.fragment_container, fragment)
        .addToBackStack(null)
        .commit();
  }

  private boolean permissionsGranted() {
    return ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
            == PackageManager.PERMISSION_GRANTED
        && ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE)
            == PackageManager.PERMISSION_GRANTED;
  }

  private boolean shouldShowRequestPermissionRationale() {
    return shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE)
        || shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE);
  }

  private void requestPermissions() {
    mPermissionLauncher.launch(
        new String[] {
          Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE
        });
  }
}
