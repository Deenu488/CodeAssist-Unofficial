package com.tyron.code.ui.drawable.manager;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.transition.TransitionManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.transition.MaterialFade;
import com.tyron.code.R;
import com.tyron.code.ui.drawable.Drawables;
import com.tyron.code.ui.drawable.adapter.DrawableManagerAdapter;
import com.tyron.code.ui.project.ProjectManager;
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

public class DrawableManagerFragment extends Fragment {

  public static final String TAG = DrawableManagerFragment.class.getSimpleName();

  private RecyclerView mRecyclerView;
  private DrawableManagerAdapter mAdapter;

  public static DrawableManagerFragment newInstance() {
    DrawableManagerFragment fragment = new DrawableManagerFragment();
    return fragment;
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
  }

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.drawable_manager_fragment, container, false);
    view.setClickable(true);

    Toolbar toolbar = view.findViewById(R.id.toolbar);
    toolbar.setNavigationOnClickListener(v -> getParentFragmentManager().popBackStack());

    return view;
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    Toolbar toolbar = view.findViewById(R.id.toolbar);
    toolbar.setOnMenuItemClickListener(menu -> getParentFragmentManager().popBackStackImmediate());

    mAdapter = new DrawableManagerAdapter();
    mAdapter.setOnDrawableSelectedListener(this::showDialog);
    mRecyclerView = view.findViewById(R.id.recyclerView);
    mRecyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 4));
    mRecyclerView.setAdapter(mAdapter);
    loadDrwables();
  }

  private void loadDrwables() {
    toggleLoading(true);

    Executors.newSingleThreadExecutor()
        .execute(
            () -> {
              String path =
                  ProjectManager.getInstance().getCurrentProject().getRootFile().getAbsolutePath()
                      + "/app/src/main/res/drawable/";

              File drawableDir = new File(path);
              if (drawableDir.exists()) {

              } else {
                drawableDir.mkdirs();
              }
              File[] files = drawableDir.listFiles(File::isFile);

              List<Drawables> Drawables = new ArrayList<>();
              if (files != null) {
                Arrays.sort(files, Comparator.comparingLong(File::lastModified));
                for (File file : files) {

                  if (file.exists()) {
                    Drawables drawable =
                        new Drawables(new File(file.getAbsolutePath().replaceAll("%20", " ")));
                    if (drawable.getRootFile().getName().endsWith(".png")) {
                      Drawables.add(drawable);
                    }
                    if (drawable.getRootFile().getName().endsWith(".webp")) {
                      Drawables.add(drawable);
                    }
                    if (drawable.getRootFile().getName().endsWith(".xml")) {
                      Drawables.add(drawable);
                    }
                    if (drawable.getRootFile().getName().endsWith(".jpg")) {
                      Drawables.add(drawable);
                    }
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
                                    mAdapter.submitList(Drawables);
                                    toggleNullProject(Drawables);
                                  },
                                  300);
                        });
              }
            });
  }

  private void toggleNullProject(List<Drawables> drawables) {
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

              View recycler = view.findViewById(R.id.recyclerView);
              View empty = view.findViewById(R.id.empty_drawables);

              TransitionManager.beginDelayedTransition(
                  (ViewGroup) recycler.getParent(), new MaterialFade());
              if (drawables.size() == 0) {
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
              View recycler = view.findViewById(R.id.recyclerView);
              View empty = view.findViewById(R.id.empty_container);
              View empty_drawables = view.findViewById(R.id.empty_drawables);
              empty_drawables.setVisibility(View.GONE);

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

  private boolean showDialog(final Drawables drawable) {
    String[] option = {"Rename", "Delete"};
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

                                  String path =
                                      ProjectManager.getInstance()
                                              .getCurrentProject()
                                              .getRootFile()
                                              .getAbsolutePath()
                                          + "/app/src/main/res/drawable";

                                  File oldDir = drawable.getRootFile();
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
                                              loadDrwables();
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
                        getString(R.string.dialog_confirm_delete, drawable.getRootFile().getName());
                    new MaterialAlertDialogBuilder(requireContext())
                        .setTitle(R.string.dialog_delete)
                        .setMessage(message)
                        .setPositiveButton(android.R.string.yes, (d, w) -> deleteDrawable(drawable))
                        .setNegativeButton(android.R.string.no, null)
                        .show();
                    break;
                }
              }
            })
        .show();

    return true;
  }

  private void deleteDrawable(Drawables drawable) {

    Executors.newSingleThreadExecutor()
        .execute(
            () -> {
              try {
                FileUtils.forceDelete(drawable.getRootFile());
                if (getActivity() != null) {
                  requireActivity()
                      .runOnUiThread(
                          () -> {
                            Toast toast =
                                Toast.makeText(
                                    requireContext(), R.string.delete_success, Toast.LENGTH_LONG);
                            toast.show();
                            loadDrwables();
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
}
