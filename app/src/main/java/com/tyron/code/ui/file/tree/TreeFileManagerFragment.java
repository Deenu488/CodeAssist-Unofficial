package com.tyron.code.ui.file.tree;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;
import com.tyron.actions.ActionManager;
import com.tyron.actions.ActionPlaces;
import com.tyron.actions.CommonDataKeys;
import com.tyron.actions.DataContext;
import com.tyron.builder.project.Project;
import com.tyron.builder.project.api.JavaModule;
import com.tyron.builder.project.api.Module;
import com.tyron.code.ApplicationLoader;
import com.tyron.code.R;
import com.tyron.code.event.EventManager;
import com.tyron.code.ui.editor.impl.FileEditorManagerImpl;
import com.tyron.code.ui.file.CommonFileKeys;
import com.tyron.code.ui.file.FileViewModel;
import com.tyron.code.ui.file.event.RefreshRootEvent;
import com.tyron.code.ui.file.tree.binder.TreeFileNodeViewBinder.TreeFileNodeListener;
import com.tyron.code.ui.file.tree.binder.TreeFileNodeViewFactory;
import com.tyron.code.ui.file.tree.model.TreeFile;
import com.tyron.code.ui.main.MainViewModel;
import com.tyron.code.ui.project.ProjectManager;
import com.tyron.code.util.UiUtilsKt;
import com.tyron.common.util.SingleTextWatcher;
import com.tyron.completion.progress.ProgressManager;
import com.tyron.ui.treeview.TreeNode;
import com.tyron.ui.treeview.TreeView;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TreeFileManagerFragment extends Fragment {

  /**
   * @deprecated Instantiate this fragment directly without arguments and use {@link FileViewModel}
   *     to update the nodes
   */
  @Deprecated
  public static TreeFileManagerFragment newInstance(File root) {
    TreeFileManagerFragment fragment = new TreeFileManagerFragment();
    Bundle args = new Bundle();
    args.putSerializable("rootFile", root);
    fragment.setArguments(args);
    return fragment;
  }

  private MainViewModel mMainViewModel;
  private FileViewModel mFileViewModel;
  private TreeView<TreeFile> treeView;

  public TreeFileManagerFragment() {
    super(R.layout.tree_file_manager_fragment);
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    mMainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
    mFileViewModel = new ViewModelProvider(requireActivity()).get(FileViewModel.class);
  }

  @SuppressLint("ClickableViewAccessibility")
  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    ViewCompat.requestApplyInsets(view);
    UiUtilsKt.addSystemWindowInsetToPadding(view, false, true, false, true);

    SwipeRefreshLayout refreshLayout = view.findViewById(R.id.refreshLayout);
    refreshLayout.setOnRefreshListener(
        () ->
            partialRefresh(
                () -> {
                  refreshLayout.setRefreshing(false);
                  treeView.refreshTreeView();
                }));

    treeView = new TreeView<>(requireContext(), TreeNode.root(Collections.emptyList()));

    MaterialButton addLibrary = view.findViewById(R.id.addNewLibrary);
    MaterialButton projectInfo = view.findViewById(R.id.projectProperties);

    addLibrary.setOnClickListener(
        v -> {
          showNewLibrary();
        });

    projectInfo.setOnClickListener(
        v -> {
          showProjectInfo();
        });

    HorizontalScrollView horizontalScrollView = view.findViewById(R.id.horizontalScrollView);
    horizontalScrollView.addView(
        treeView.getView(),
        new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT));
    treeView.getView().setNestedScrollingEnabled(false);

    EventManager eventManager = ApplicationLoader.getInstance().getEventManager();
    eventManager.subscribeEvent(
        getViewLifecycleOwner(),
        RefreshRootEvent.class,
        (event, unsubscribe) -> {
          File refreshRoot = event.getRoot();
          TreeNode<TreeFile> currentRoot = treeView.getRoot();
          if (currentRoot != null && refreshRoot.equals(currentRoot.getValue().getFile())) {
            partialRefresh(() -> treeView.refreshTreeView());
          } else {
            ProgressManager.getInstance()
                .runNonCancelableAsync(
                    () -> {
                      TreeNode<TreeFile> node = TreeNode.root(TreeUtil.getNodes(refreshRoot));
                      ProgressManager.getInstance()
                          .runLater(
                              () -> {
                                if (getActivity() == null) {
                                  return;
                                }
                                treeView.refreshTreeView(node);
                              });
                    });
          }
        });

    treeView.setAdapter(
        new TreeFileNodeViewFactory(
            new TreeFileNodeListener() {
              @Override
              public void onNodeToggled(TreeNode<TreeFile> treeNode, boolean expanded) {
                if (treeNode.isLeaf()) {
                  if (treeNode.getValue().getFile().isFile()) {
                    FileEditorManagerImpl.getInstance()
                        .openFile(requireContext(), treeNode.getValue().getFile(), true);
                  }
                }
              }

              @Override
              public boolean onNodeLongClicked(
                  View view, TreeNode<TreeFile> treeNode, boolean expanded) {
                PopupMenu popupMenu = new PopupMenu(requireContext(), view);
                addMenus(popupMenu, treeNode);
                // Get the background drawable and set its shape
                try {
                  Field popupField = PopupMenu.class.getDeclaredField("mPopup");
                  popupField.setAccessible(true);
                  Object menuPopupHelper = popupField.get(popupMenu);
                  Class<?> classPopupHelper = Class.forName(menuPopupHelper.getClass().getName());
                  Method setForceIcons =
                      classPopupHelper.getMethod("setForceShowIcon", boolean.class);
                  setForceIcons.invoke(menuPopupHelper, true);
                  PopupWindow popupWindow =
                      (PopupWindow)
                          menuPopupHelper
                              .getClass()
                              .getDeclaredField("mPopup")
                              .get(menuPopupHelper);
                  popupWindow.setBackgroundDrawable(
                      getResources().getDrawable(R.drawable.rounded_popup_menu_background, null));
                } catch (Exception e) {
                  e.printStackTrace();
                }
                popupMenu.show();
                return true;
              }
            }));
    mFileViewModel
        .getNodes()
        .observe(
            getViewLifecycleOwner(),
            node -> {
              treeView.refreshTreeView(node);
            });
  }

  private void showNewLibrary() {
    // Inflate the view
    LayoutInflater inflater = LayoutInflater.from(requireContext());
    View layout = inflater.inflate(R.layout.add_new_library_dialog, null);
    ProjectManager manager = ProjectManager.getInstance();
    Project project = manager.getCurrentProject();
    Module module = project.getMainModule();
    JavaModule javaModule = (JavaModule) module;

    AlertDialog dialog =
        new MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.new_library)
            .setPositiveButton(R.string.create, null)
            .setNegativeButton(android.R.string.cancel, null)
            .setView(layout)
            .create();
    dialog.setOnShowListener(
        d -> {
          final TextInputLayout nameLayout = layout.findViewById(R.id.name);
          final TextInputLayout packageNameLayout = layout.findViewById(R.id.packageName);

          final EditText nameEditText = nameLayout.getEditText();
          final EditText packageNameEditText = packageNameLayout.getEditText();
          final Button button = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
          button.setEnabled(false);
          SingleTextWatcher textWatcher =
              new SingleTextWatcher() {
                @Override
                public void afterTextChanged(Editable editable) {
                  String name = nameEditText.getText().toString();
                  String packageName = packageNameEditText.getText().toString();
                  if (TextUtils.isEmpty(name) || TextUtils.isEmpty(packageName)) {
                    button.setEnabled(false);
                  } else if (packageName.endsWith(".") || packageName.contains(" ")) {
                    button.setEnabled(false);
                  } else {

                    boolean isLibraryExists = false;
                    List<String> implementationProjects = javaModule.getAllProjects();

                    for (String implementationProject : implementationProjects) {
                      if (implementationProject.contains(name)) {
                        isLibraryExists = true;
                        break;
                      }
                    }
                    if (isLibraryExists) {
                      nameLayout.setError("Library with this name already exists.");
                      button.setEnabled(false);
                    } else {

                      nameLayout.setError(null);

                      button.setEnabled(true);
                    }
                  }
                }
              };

          SingleTextWatcher textWatcher2 =
              new SingleTextWatcher() {
                @Override
                public void afterTextChanged(Editable editable) {
                  String name = nameEditText.getText().toString();
                  String packageName = packageNameEditText.getText().toString();
                  String package_name = editable.toString();

                  if (TextUtils.isEmpty(name)
                      || TextUtils.isEmpty(package_name)
                      || packageName.endsWith(".")
                      || package_name.contains(" ")) {
                    button.setEnabled(false);
                  } else {
                    boolean isLibraryExists = false;
                    List<String> implementationProjects = javaModule.getAllProjects();

                    for (String implementationProject : implementationProjects) {
                      if (implementationProject.contains(name)) {
                        isLibraryExists = true;
                        break;
                      }
                    }
                    if (isLibraryExists) {
                      nameLayout.setError("Library with this name already exists.");
                      button.setEnabled(false);
                    } else {

                      nameLayout.setError(null);

                      button.setEnabled(true);
                    }
                  }
                }
              };

          nameEditText.addTextChangedListener(textWatcher);
          packageNameEditText.addTextChangedListener(textWatcher2);

          button.setOnClickListener(
              v -> {
                String name = nameEditText.getText().toString();
                String packageName = packageNameEditText.getText().toString();
                addLibrary(javaModule.getGradleFile(), name);
                File root = new File(module.getProjectDir(), "settings.gradle");
                addToInclude(root, name);
                createAndroidLibrary(module.getProjectDir(), packageName, name);
                dialog.dismiss();
              });
        });
    dialog.show();
  }

  private void createAndroidLibrary(File root, String packageName, String name) {
    // Create the library directory structure
    File libraryDir = new File(root, name);
    File srcDir = new File(libraryDir, "src/main/java/" + packageName.replace(".", "/"));
    File resDir = new File(libraryDir, "src/main/res");
    File manifestFile = new File(libraryDir, "src/main/AndroidManifest.xml");

    if (!libraryDir.exists()) {
      libraryDir.mkdirs();
    }

    if (!srcDir.exists()) {
      srcDir.mkdirs();
    }

    if (!resDir.exists()) {
      resDir.mkdirs();
    }

    // Create the build.gradle file
    String gradleCode =
        "plugins {\n"
            + "    id 'com.android.library'\n"
            + "}\n"
            + "\n"
            + "android {\n"
            + "    compileSdkVersion 33\n"
            + "\n"
            + "    defaultConfig {\n"
            + "        minSdkVersion 21\n"
            + "        targetSdkVersion 33\n"
            + "        versionCode 1\n"
            + "        versionName \"1.0\"\n"
            + "    }\n"
            + "\n"
            + "    buildTypes {\n"
            + "        release {\n"
            + "            minifyEnabled false\n"
            + "            proguardFiles getDefaultProguardFile('proguard-android.txt'), 'proguard-rules.pro'\n"
            + "        }\n"
            + "    }\n"
            + "\n"
            + "    compileOptions {\n"
            + "        sourceCompatibility JavaVersion.VERSION_1_8\n"
            + "        targetCompatibility JavaVersion.VERSION_1_8\n"
            + "    }\n"
            + "}\n"
            + "\n"
            + "dependencies {\n"
            + "    implementation fileTree(dir: 'libs', include: ['*.jar'])\n"
            + "}";

    File gradleFile = new File(libraryDir, "build.gradle");
    try {
      gradleFile.createNewFile();
      FileWriter gradleWriter = new FileWriter(gradleFile);
      gradleWriter.write(gradleCode);
      gradleWriter.close();
    } catch (IOException e) {
      e.printStackTrace();
    }

    // Create the Android Manifest file
    String manifestCode =
        "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
            + "    package=\""
            + packageName
            + "\"\n"
            + "    android:versionCode=\"1\"\n"
            + "    android:versionName=\"1.0\">\n"
            + "\n"
            + "</manifest>";

    try {
      manifestFile.createNewFile();
      FileWriter manifestWriter = new FileWriter(manifestFile);
      manifestWriter.write(manifestCode);
      manifestWriter.close();
    } catch (IOException e) {
      e.printStackTrace();
    }

    // Create the strings.xml file
    File resFile = new File(resDir, "values/strings.xml");
    try {
      resFile.getParentFile().mkdirs();
      resFile.createNewFile();
      FileWriter resWriter = new FileWriter(resFile);
      resWriter.write(
          "<resources>\n"
              + "    <string name=\"library_name\">"
              + name
              + "</string>\n"
              + "</resources>");
      resWriter.close();
    } catch (IOException e) {
      e.printStackTrace();
    }

    // Create a sample Java file
    File javaFile = new File(srcDir, "Library.java");
    String javaCode =
        "package "
            + packageName
            + ";\n\n"
            + "public class Library {\n\n"
            + "  public static String getMessage() {\n"
            + "    return \"Hello from the library!\";\n"
            + "  }\n\n"
            + "}\n";
    try {
      FileWriter writer = new FileWriter(javaFile);
      writer.write(javaCode);
      writer.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void showProjectInfo() {
    ProjectManager manager = ProjectManager.getInstance();
    Project project = manager.getCurrentProject();
    Module module = project.getMainModule();
    JavaModule javaModule = (JavaModule) module;

    String root = javaModule.getRootFile().getName();

    LayoutInflater inflater =
        (LayoutInflater) requireContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    View v = inflater.inflate(R.layout.dialog_project_information, null);
    TextView projectPath = v.findViewById(R.id.projectPath);
    TextView libraryProjects = v.findViewById(R.id.libraryProjects);
    TextView includedProjects = v.findViewById(R.id.includedProjects);
    TextView projectLibraries = v.findViewById(R.id.projectLibraries);

    File prPath = javaModule.getRootFile();
    String pr =
        root.substring(0, 1).toUpperCase() + root.substring(1) + " " + prPath.getAbsolutePath();

    projectPath.setText(pr);

    List<String> implementationProjects = javaModule.getAllProjects();
    List<String> included = javaModule.getIncludedProjects();

    String implementationText =
        implementationProjects.isEmpty()
            ? "sub projects: <none>"
            : "sub projects:\n" + String.join("\n", implementationProjects);
    libraryProjects.setText(implementationText);

    String includedText =
        included.isEmpty()
            ? "included projects: <none>"
            : "included projects:\n" + String.join("\n", included);
    includedProjects.setText(includedText);

    File[] fileLibraries = getProjectLibraries(javaModule.getLibraryDirectory());

    if (fileLibraries != null && fileLibraries.length > 0) {
      StringBuilder libraryTextBuilder = new StringBuilder(prPath.getName() + " libraries:\n");
      for (File fileLibrary : fileLibraries) {
        libraryTextBuilder.append(fileLibrary.getName()).append("\n");
      }
      String libraryText = libraryTextBuilder.toString().trim();
      projectLibraries.setText(libraryText);
    } else {
      projectLibraries.setText(prPath.getName() + " libraries: <none>");
    }

    new MaterialAlertDialogBuilder(requireContext())
        .setTitle("Project " + "'" + root + "'")
        .setView(v)
        .setPositiveButton(android.R.string.ok, null)
        .setNeutralButton(
            R.string.variants,
            (d, w) -> {
              showBuildVariants();
            })
        .show();
  }

  private static final String LAST_SELECTED_INDEX_KEY = "last_selected_index";
  private int lastSelectedIndex = 0;

  private void showBuildVariants() {
    List<String> variants = new ArrayList<>();
    variants.add("Release");
    variants.add("Debug");
    final String[] options = variants.toArray(new String[0]);

    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(requireContext());
    int defaultSelection = preferences.getInt(LAST_SELECTED_INDEX_KEY, 1);
    lastSelectedIndex = defaultSelection;

    new MaterialAlertDialogBuilder(requireContext())
        .setTitle(R.string.build_variants)
        .setSingleChoiceItems(
            options,
            defaultSelection,
            (dialog, which) -> {
              lastSelectedIndex = which;
              preferences.edit().putInt(LAST_SELECTED_INDEX_KEY, which).apply();
            })
        .setPositiveButton(
            android.R.string.ok,
            (dialog, which) -> {
              String selectedVariant = options[lastSelectedIndex];
              if (defaultSelection != lastSelectedIndex) {
                String message = getString(R.string.switched_to_variant) + " " + selectedVariant;
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
              }
            })
        .show();
  }

  private File[] getProjectLibraries(File dir) {
    File[] fileLibraries =
        dir.listFiles(c -> c.getName().endsWith(".aar") || c.getName().endsWith(".jar"));
    return fileLibraries;
  }

  private void partialRefresh(Runnable callback) {
    ProgressManager.getInstance()
        .runNonCancelableAsync(
            () -> {
              if (!treeView.getAllNodes().isEmpty()) {
                TreeNode<TreeFile> node = treeView.getAllNodes().get(0);
                TreeUtil.updateNode(node);
                ProgressManager.getInstance()
                    .runLater(
                        () -> {
                          if (getActivity() == null) {
                            return;
                          }
                          callback.run();
                        });
              }
            });
  }

  @Override
  public void onSaveInstanceState(@NonNull Bundle outState) {
    super.onSaveInstanceState(outState);
  }

  /**
   * Add menus to the current PopupMenu based on the current {@link TreeNode}
   *
   * @param popupMenu The PopupMenu to add to
   * @param node The current TreeNode in the file tree
   */
  private void addMenus(PopupMenu popupMenu, TreeNode<TreeFile> node) {
    DataContext dataContext = DataContext.wrap(requireContext());
    dataContext.putData(CommonDataKeys.FILE, node.getContent().getFile());
    dataContext.putData(CommonDataKeys.PROJECT, ProjectManager.getInstance().getCurrentProject());
    dataContext.putData(CommonDataKeys.FRAGMENT, TreeFileManagerFragment.this);
    dataContext.putData(CommonDataKeys.ACTIVITY, requireActivity());
    dataContext.putData(CommonFileKeys.TREE_NODE, node);

    ActionManager.getInstance()
        .fillMenu(dataContext, popupMenu.getMenu(), ActionPlaces.FILE_MANAGER, true, false);
  }

  public TreeView<TreeFile> getTreeView() {
    return treeView;
  }

  public MainViewModel getMainViewModel() {
    return mMainViewModel;
  }

  public FileViewModel getFileViewModel() {
    return mFileViewModel;
  }

  private void addLibrary(File gradleFile, String name) {
    String dependencyLine = "\timplementation project(':" + name + "')\n";

    try {
      // Read the contents of the build.gradle file
      FileInputStream inputStream = new FileInputStream(gradleFile);
      BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
      StringBuilder stringBuilder = new StringBuilder();
      String line;
      while ((line = reader.readLine()) != null) {
        // Add the dependency line after the last line that starts with "dependencies {"
        if (line.trim().startsWith("dependencies {")) {
          stringBuilder.append(line).append("\n");
          while ((line = reader.readLine()) != null) {
            stringBuilder.append(line).append("\n");
          }
          stringBuilder.insert(stringBuilder.lastIndexOf("}"), dependencyLine);
          break;
        } else {
          stringBuilder.append(line).append("\n");
        }
      }
      inputStream.close();

      // Write the modified contents back to the build.gradle file
      FileOutputStream outputStream = new FileOutputStream(gradleFile);
      outputStream.write(stringBuilder.toString().getBytes());
      outputStream.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void addToInclude(File gradleSettingsFile, String name) {
    String includeLine = "include ':" + name + "'\n";

    try {
      // Read the contents of the settings.gradle file
      FileInputStream inputStream = new FileInputStream(gradleSettingsFile);
      BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
      StringBuilder stringBuilder = new StringBuilder();
      String line;
      boolean includeExists = false;
      while ((line = reader.readLine()) != null) {
        // Check if the include line already exists
        if (line.trim().equals(includeLine.trim())) {
          includeExists = true;
        }
        stringBuilder.append(line).append("\n");
      }
      inputStream.close();

      // Add the include line if it does not already exist
      if (!includeExists) {
        stringBuilder.append(includeLine);
      }

      // Write the modified contents back to the settings.gradle file
      FileOutputStream outputStream = new FileOutputStream(gradleSettingsFile);
      outputStream.write(stringBuilder.toString().getBytes());
      outputStream.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
