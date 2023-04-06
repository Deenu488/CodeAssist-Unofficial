package com.tyron.code.ui.project;

import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.transition.TransitionManager;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.bottomsheet.BottomSheetDragHandleView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.transition.MaterialFade;
import com.tyron.builder.project.Project;
import com.tyron.code.R;
import com.tyron.code.ui.main.MainFragment;
import com.tyron.code.ui.project.adapter.ProjectManagerAdapter;
import com.tyron.common.SharedPreferenceKeys;
import com.tyron.common.util.AndroidUtilities;
import com.tyron.completion.progress.ProgressManager;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Executors;
import org.apache.commons.io.FileUtils;

public class ProjectSheetFragment extends BottomSheetDialogFragment {

  public static final String TAG = ProjectSheetFragment.class.getSimpleName();
  CoordinatorLayout.Behavior behavior;
  private SharedPreferences mPreferences;
  private RecyclerView mRecyclerView;
  private ProjectManagerAdapter mAdapter;

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

    mAdapter = new ProjectManagerAdapter();
    mAdapter.setOnProjectSelectedListener(this::checkRootProject);
    mAdapter.setOnProjectLongClickListener(this::inflateProjectMenus);
    mRecyclerView = view.findViewById(R.id.projects_recycler);
    BottomSheetDragHandleView bottomSheetDragHandleView = view.findViewById(R.id.drag_handle);
    mRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
    mRecyclerView.setAdapter(mAdapter);

    loadProjects();

    CoordinatorLayout.LayoutParams params =
        (CoordinatorLayout.LayoutParams) ((View) view.getParent()).getLayoutParams();
    behavior = params.getBehavior();

    if (behavior != null && behavior instanceof BottomSheetBehavior) {
      ((BottomSheetBehavior) behavior)
          .setBottomSheetCallback(
              new BottomSheetBehavior.BottomSheetCallback() {
                @Override
                public void onStateChanged(@NonNull View bottomSheet, int newState) {
                  String state = "";

                  switch (newState) {
                    case BottomSheetBehavior.STATE_DRAGGING:
                      {
                        bottomSheetDragHandleView.setVisibility(View.VISIBLE);
                        state = "DRAGGING";
                        break;
                      }
                    case BottomSheetBehavior.STATE_SETTLING:
                      {
                        bottomSheetDragHandleView.setVisibility(View.VISIBLE);
                        state = "SETTLING";
                        break;
                      }
                    case BottomSheetBehavior.STATE_EXPANDED:
                      {
                        state = "EXPANDED";
                        bottomSheetDragHandleView.setVisibility(View.GONE);
                        break;
                      }
                    case BottomSheetBehavior.STATE_HIDDEN:
                      {
                        dismiss();
                        state = "HIDDEN";
                        break;
                      }
                  }
                }

                @Override
                public void onSlide(@NonNull View bottomSheet, float slideOffset) {}
              });
    }
  }

  private boolean inflateProjectMenus(View view, Project project) {
    String[] option = {"Rename", "Delete", "Copy Path"};
    new MaterialAlertDialogBuilder(requireContext())
        .setItems(
            option,
            new DialogInterface.OnClickListener() {
              @Override
              public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                  case 0:
                    LayoutInflater inflater =
                        (LayoutInflater)
                            requireContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    View v = inflater.inflate(R.layout.base_textinput_layout, null);
                    TextInputLayout layout = v.findViewById(R.id.textinput_layout);
                    layout.setHint(R.string.new_name);
                    final Editable rename = layout.getEditText().getText();

                    new MaterialAlertDialogBuilder(requireContext())
                        .setTitle(R.string.rename)
                        .setView(v)
                        .setPositiveButton(
                            android.R.string.ok,
                            new DialogInterface.OnClickListener() {

                              @Override
                              public void onClick(DialogInterface dia, int which) {
                                try {
                                  String path;
                                  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                    path =
                                        requireContext()
                                            .getExternalFilesDir("Projects")
                                            .getAbsolutePath();
                                  } else {
                                    path =
                                        Environment.getExternalStorageDirectory()
                                            + "/CodeAssistProjects";
                                  }

                                  File oldDir = project.getRootFile();
                                  File newDir = new File(path + "/" + rename);
                                  if (newDir.exists()) {
                                    throw new IllegalArgumentException();
                                  } else {
                                    oldDir.renameTo(newDir);
                                  }

                                  if (getActivity() != null) {
                                    requireActivity()
                                        .runOnUiThread(
                                            () -> {
                                              AndroidUtilities.showSimpleAlert(
                                                  requireContext(),
                                                  getString(R.string.success),
                                                  getString(R.string.rename_success));
                                              loadProjects();
                                            });
                                  }
                                } catch (Exception e) {
                                  if (getActivity() != null) {
                                    requireActivity()
                                        .runOnUiThread(
                                            () ->
                                                AndroidUtilities.showSimpleAlert(
                                                    requireContext(),
                                                    getString(R.string.error),
                                                    e.getMessage()));
                                  }
                                }
                              }
                            })
                        .setNegativeButton(android.R.string.cancel, null)
                        .show();

                    break;

                  case 1:
                    String message =
                        getString(R.string.dialog_confirm_delete, project.getRootFile().getName());
                    new MaterialAlertDialogBuilder(requireContext())
                        .setTitle(R.string.dialog_delete)
                        .setMessage(message)
                        .setPositiveButton(android.R.string.yes, (d, w) -> deleteProject(project))
                        .setNegativeButton(android.R.string.no, null)
                        .show();
                    break;

                  case 2:
                    ClipboardManager clipboard =
                        (ClipboardManager)
                            requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
                    clipboard.setText(project.getRootFile().toString());
                    Toast toast =
                        Toast.makeText(
                            requireContext(), R.string.copied_to_clipboard, Toast.LENGTH_LONG);
                    toast.show();
                    break;
                }
              }
            })
        .show();

    return true;
  }

  private void deleteProject(Project project) {

    Executors.newSingleThreadExecutor()
        .execute(
            () -> {
              try {
                FileUtils.forceDelete(project.getRootFile());
                if (getActivity() != null) {
                  requireActivity()
                      .runOnUiThread(
                          () -> {
                            Toast toast =
                                Toast.makeText(
                                    requireContext(), R.string.delete_success, Toast.LENGTH_LONG);
                            toast.show();
                            loadProjects();
                          });
                }
              } catch (IOException e) {
                if (getActivity() != null) {
                  requireActivity()
                      .runOnUiThread(
                          () ->
                              AndroidUtilities.showSimpleAlert(
                                  requireContext(), getString(R.string.error), e.getMessage()));
                }
              }
            });
  }

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.project_manager_fragment, container, false);
  }

  @Override
  public void onResume() {
    super.onResume();
  }

  private void setSavePath(String path) {
    mPreferences.edit().putString(SharedPreferenceKeys.PROJECT_SAVE_PATH, path).apply();
    loadProjects();
  }

  private void checkRootProject(Project project) {

    File folder = project.getRootFile();
    try {
      List<String> projects = new ArrayList<>(checkFolders(folder));
      final String[] options = checkFolders(folder).toArray(new String[0]);

      if (projects.isEmpty() || projects.toString().equals("[app]")) {
        openProject(project);
      } else {
        new MaterialAlertDialogBuilder(getActivity())
            .setTitle(R.string.choose_project)
            .setItems(
                options,
                new DialogInterface.OnClickListener() {
                  @Override
                  public void onClick(DialogInterface dialog, int which) {
                    openProject(project, options[which]);
                  }
                })
            .show();
      }
    } catch (IOException e) {
    }
  }

  public static List<String> checkFolders(File folder) throws IOException {
    List<String> root_projects = new ArrayList<>();
    File[] files = folder.listFiles();
    for (File file : files) {
      if (file.isDirectory()) {
        String path =
            file.getAbsolutePath().substring((folder.getAbsolutePath() + "/").lastIndexOf("/") + 1);
        File main = new File(folder, path + "/src/main");
        if (main.isDirectory() || main.exists()) {
          String root =
              file.getAbsolutePath()
                  .substring((file.getAbsolutePath() + path).lastIndexOf("/") + 1);
          root_projects.add(root);
        }
      }
    }
    return root_projects;
  }

  private void openProject(Project project) {
    dismiss();
    MainFragment fragment =
        MainFragment.newInstance(project.getRootFile().getAbsolutePath(), "app");
    getParentFragmentManager()
        .beginTransaction()
        .replace(R.id.fragment_container, fragment)
        .addToBackStack(null)
        .commit();
  }

  private void openProject(Project project, String name) {
    dismiss();
    MainFragment fragment = MainFragment.newInstance(project.getRootFile().getAbsolutePath(), name);
    getParentFragmentManager()
        .beginTransaction()
        .replace(R.id.fragment_container, fragment)
        .addToBackStack(null)
        .commit();
  }

  private void loadProjects() {
    toggleLoading(true);

    Executors.newSingleThreadExecutor()
        .execute(
            () -> {
              String path;
              if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                path = requireContext().getExternalFilesDir("/Projects").getAbsolutePath();
              } else {
                path = Environment.getExternalStorageDirectory() + "/CodeAssistProjects";
                String fileName = ".nomedia";
                File nomediaFile = new File(path, fileName);
                try {
                  if (nomediaFile.createNewFile()) {}
                } catch (IOException e) {
                }
              }
              File projectDir = new File(path);
              if (projectDir.exists()) {

              } else {
                projectDir.mkdirs();
              }
              File[] directories = projectDir.listFiles(File::isDirectory);

              List<Project> projects = new ArrayList<>();
              if (directories != null) {
                Arrays.sort(directories, Comparator.comparingLong(File::lastModified));
                for (File directory : directories) {
                  File appModule = new File(directory, "app");
                  if (appModule.exists()) {
                    Project project =
                        new Project(
                            new File(directory.getAbsolutePath().replaceAll("%20", " ")), "app");
                    // if (project.isValidProject()) {
                    projects.add(project);
                    // }
                  }
                }
              }

              if (getActivity() != null) {
                requireActivity()
                    .runOnUiThread(
                        () -> {
                          toggleLoading(false);
                          ProgressManager.getInstance()
                              .runLater(
                                  () -> {
                                    mAdapter.submitList(projects);
                                    toggleNullProject(projects);
                                  },
                                  300);
                        });
              }
            });
  }

  private void toggleNullProject(List<Project> projects) {
    ProgressManager.getInstance()
        .runLater(
            () -> {
              if (getActivity() == null || isDetached()) {
                return;
              }
              View view = getView();
              if (view == null) {
                return;
              }

              View recycler = view.findViewById(R.id.projects_recycler);
              View empty = view.findViewById(R.id.empty_projects);

              TransitionManager.beginDelayedTransition(
                  (ViewGroup) recycler.getParent(), new MaterialFade());
              if (projects.size() == 0) {
                recycler.setVisibility(View.GONE);
                empty.setVisibility(View.VISIBLE);
              } else {
                recycler.setVisibility(View.VISIBLE);
                empty.setVisibility(View.GONE);
              }
            },
            300);
  }

  private void toggleLoading(boolean show) {
    ProgressManager.getInstance()
        .runLater(
            () -> {
              if (getActivity() == null || isDetached()) {
                return;
              }
              View view = getView();
              if (view == null) {
                return;
              }
              View recycler = view.findViewById(R.id.projects_recycler);
              View empty = view.findViewById(R.id.empty_container);
              View empty_project = view.findViewById(R.id.empty_projects);
              empty_project.setVisibility(View.GONE);

              TransitionManager.beginDelayedTransition(
                  (ViewGroup) recycler.getParent(), new MaterialFade());
              if (show) {
                recycler.setVisibility(View.GONE);
                empty.setVisibility(View.VISIBLE);
              } else {
                recycler.setVisibility(View.VISIBLE);
                empty.setVisibility(View.GONE);
              }
            },
            300);
  }
}
