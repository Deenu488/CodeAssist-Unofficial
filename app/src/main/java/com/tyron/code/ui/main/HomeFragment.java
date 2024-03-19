package com.tyron.code.ui.main;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.transition.MaterialSharedAxis;
import com.tyron.builder.project.Project;
import com.tyron.code.tasks.git.GitCloneTask;
import com.tyron.code.ui.project.ImportProjectProgressFragment;
import com.tyron.code.ui.project.ProjectSheetFragment;
import com.tyron.code.ui.settings.SettingsActivity;
import com.tyron.code.ui.wizard.WizardFragment;
import com.tyron.common.SharedPreferenceKeys;
import com.tyron.common.util.AndroidUtilities;
import com.tyron.completion.progress.ProgressManager;
import java.io.File;
import java.util.Objects;
import org.codeassist.unofficial.BuildConfig;
import org.codeassist.unofficial.R;

public class HomeFragment extends Fragment {

  public static final String TAG = HomeFragment.class.getSimpleName();
  private MaterialButton create_new_project,
      clone_git_repository,
      import_project,
      open_custom_project,
      open_project_manager,
      configure_settings;
  private SharedPreferences mPreferences;
  private boolean mShowDialogOnPermissionGrant;
  private ActivityResultLauncher<String[]> mPermissionLauncher;
  private final ActivityResultContracts.RequestMultiplePermissions mPermissionsContract =
      new ActivityResultContracts.RequestMultiplePermissions();
  private TextView app_version;

  private final ActivityResultLauncher<Intent> documentPickerLauncher =
      registerForActivityResult(
          new ActivityResultContracts.StartActivityForResult(),
          result -> {
            if (result.getResultCode() == Activity.RESULT_OK) {
              Intent data = result.getData();
              if (data != null) {
                Uri uri = data.getData();
                if (uri != null) {
                  String name =
                      Objects.requireNonNull(DocumentFile.fromSingleUri(requireContext(), uri))
                          .getName();
                  if (name != null) {
                    name = name.substring(0, name.lastIndexOf('.'));

                    String path;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                      path = requireContext().getExternalFilesDir("Projects").getAbsolutePath();
                    } else {
                      path = Environment.getExternalStorageDirectory() + "/CodeAssistProjects";
                    }

                    File project = new File(path, name);
                    if (project.exists()) {
                      AndroidUtilities.showToast(
                          getString(R.string.project_already_exists, project.getName()));
                      return;
                    }

                    ImportProjectProgressFragment importProjectProgressFragment =
                        ImportProjectProgressFragment.Companion.newInstance(uri);
                    importProjectProgressFragment.setOnSuccessListener(
                        new ImportProjectProgressFragment.OnSuccessListener() {
                          @Override
                          public void onSuccess() {}
                        });

                    importProjectProgressFragment.setOnButtonClickedListener(
                        new ImportProjectProgressFragment.OnButtonClickedListener() {
                          @Override
                          public void onButtonClicked() {
                            openProject(new Project(project));
                            importProjectProgressFragment.dismiss();
                          }
                        });

                    importProjectProgressFragment.show(
                        getChildFragmentManager(), ImportProjectProgressFragment.TAG);
                  }
                }
              }
            }
          });

  private final ActivityResultLauncher<Intent> documentPickerLauncher2 =
      registerForActivityResult(
          new ActivityResultContracts.StartActivityForResult(),
          result -> {
            if (result.getResultCode() == Activity.RESULT_OK) {
              Intent data = result.getData();
              if (data != null) {
                Uri uri = data.getData();
                if (uri != null) {
                  File file = new File(uri.getPath());
                  final String[] split = file.getPath().split(":");

                  //  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                  //  } else {
                  String path =
                      Environment.getExternalStorageDirectory()
                          .getAbsolutePath()
                          .concat("/")
                          .concat(split[1]);
                  openProject(new Project(new File(path)));
                  //  }
                }
              }
            }
          });

  private final ActivityResultLauncher<Intent> permissionLauncher =
      registerForActivityResult(
          new ActivityResultContracts.StartActivityForResult(),
          result -> {
            ProgressManager.getInstance()
                .runLater(
                    () -> {
                      if (Environment.isExternalStorageManager()) {
                        AndroidUtilities.showToast("Permission granted");
                        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                        intent.addCategory(Intent.CATEGORY_DEFAULT);
                        documentPickerLauncher2.launch(intent);
                      } else {
                        AndroidUtilities.showToast("Permission not granted");
                      }
                    },
                    500);
          });

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
    import_project = view.findViewById(R.id.importProject);
    open_custom_project = view.findViewById(R.id.openProject);
    open_project_manager = view.findViewById(R.id.openProjectManager);
    configure_settings = view.findViewById(R.id.configureSettings);
    app_version = view.findViewById(R.id.app_version);
    String versionName = String.valueOf(BuildConfig.VERSION_NAME);
    app_version.setText("Version " + versionName);

    boolean isOpenCustomProject = mPreferences.getBoolean("open_custom_project", false);

    if (isOpenCustomProject) {
      open_project_manager.setVisibility(View.GONE);
      open_custom_project.setVisibility(View.VISIBLE);
    } else {
      open_project_manager.setVisibility(View.VISIBLE);
      open_custom_project.setVisibility(View.GONE);
    }

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

    import_project.setOnClickListener(
        v -> {
          Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
          intent.addCategory(Intent.CATEGORY_OPENABLE);
          intent.setType("application/zip");
          documentPickerLauncher.launch(intent);
        });

    open_custom_project.setOnClickListener(
        v -> {
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {

            if (!Environment.isExternalStorageManager()) {

              new MaterialAlertDialogBuilder(requireContext())
                  .setMessage(
                      "This feature requires access to manage all files. Please grant the necessary"
                          + " permission.")
                  .setPositiveButton(
                      "Grant Permission",
                      (d, which) -> {
                        requestPermission();
                      })
                  .setNegativeButton("Cancel", (d, which) -> {})
                  .setTitle("Permission Required")
                  .show();

            } else {

              Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
              intent.addCategory(Intent.CATEGORY_DEFAULT);
              documentPickerLauncher2.launch(intent);
            }

          } else {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            intent.addCategory(Intent.CATEGORY_DEFAULT);
            documentPickerLauncher2.launch(intent);
          }
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

  private void requestPermission() {
    try {
      Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
      intent.addCategory(Intent.CATEGORY_DEFAULT);
      intent.setData(Uri.parse(String.format("package:%s", requireContext().getPackageName())));
      permissionLauncher.launch(intent);
    } catch (Exception e) {
      Intent intent = new Intent();
      intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
      permissionLauncher.launch(intent);
    }
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

    boolean isOpenCustomProject = mPreferences.getBoolean("open_custom_project", false);

    if (isOpenCustomProject) {
      open_project_manager.setVisibility(View.GONE);
      open_custom_project.setVisibility(View.VISIBLE);
    } else {
      open_project_manager.setVisibility(View.VISIBLE);
      open_custom_project.setVisibility(View.GONE);
    }
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
