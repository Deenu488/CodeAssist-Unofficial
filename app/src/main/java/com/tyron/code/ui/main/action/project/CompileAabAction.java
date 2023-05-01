package com.tyron.code.ui.main.action.project;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.tyron.actions.ActionPlaces;
import com.tyron.actions.AnAction;
import com.tyron.actions.AnActionEvent;
import com.tyron.actions.CommonDataKeys;
import com.tyron.actions.Presentation;
import com.tyron.builder.compiler.BuildType;
import com.tyron.builder.project.Project;
import com.tyron.builder.project.api.FileManager;
import com.tyron.builder.project.api.Module;
import com.tyron.code.R;
import com.tyron.code.ui.editor.Savable;
import com.tyron.code.ui.main.CompileCallback;
import com.tyron.code.ui.main.MainFragment;
import com.tyron.code.ui.main.MainViewModel;
import com.tyron.completion.progress.ProgressManager;
import com.tyron.fileeditor.api.FileEditor;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.io.FileUtils;

public class CompileAabAction extends AnAction {

  @Override
  public void update(@NonNull AnActionEvent event) {

    CompileCallback data = event.getData(MainFragment.COMPILE_CALLBACK_KEY);
    if (data == null) {
      event.getPresentation().setVisible(false);
      return;
    }

    Presentation presentation = event.getPresentation();
    presentation.setVisible(false);
    Context context = event.getData(CommonDataKeys.CONTEXT);
    if (!ActionPlaces.MAIN_TOOLBAR.equals(event.getPlace())) {
      return;
    }

    Project project = event.getData(CommonDataKeys.PROJECT);
    if (project == null) {
      return;
    }

    Module module = project.getMainModule();
    List<String> plugins = module.getPlugins();
    String pluginType = plugins.toString();
    if (!pluginType.contains("com.android.application")) {
      presentation.setVisible(false);
      return;
    }

    MainViewModel mainViewModel = event.getData(MainFragment.MAIN_VIEW_MODEL_KEY);
    if (mainViewModel == null) {
      return;
    }

    presentation.setVisible(true);
    presentation.setText(context.getString(R.string.menu_build_aab));
  }

  @Override
  public void actionPerformed(@NonNull AnActionEvent event) {
    Context context = event.getData(CommonDataKeys.CONTEXT);
    CompileCallback callback = event.getData(MainFragment.COMPILE_CALLBACK_KEY);
    MainViewModel viewModel = event.getRequiredData(MainFragment.MAIN_VIEW_MODEL_KEY);
    Project project = event.getData(CommonDataKeys.PROJECT);
    if (project == null) {
      return;
    }

    List<FileEditor> editors = viewModel.getFiles().getValue();
    if (editors == null) {
      return;
    }
    // save files before build
    Stream<FileEditor> validEditors =
        editors.stream()
            .filter(it -> it.getFragment() instanceof Savable)
            .filter(it -> ((Savable) it.getFragment()).canSave());
    List<File> filesToSave = validEditors.map(FileEditor::getFile).collect(Collectors.toList());

    ProgressManager.getInstance()
        .runNonCancelableAsync(
            () -> {
              List<IOException> exceptions = saveFiles(project, filesToSave);
              if (!exceptions.isEmpty()) {
                new MaterialAlertDialogBuilder(event.getDataContext())
                    .setTitle(R.string.error)
                    .setPositiveButton(android.R.string.ok, null)
                    .setMessage(
                        exceptions.stream()
                            .map(IOException::getMessage)
                            .collect(Collectors.joining("\n\n")))
                    .show();
              }
            });

    callback.compile(BuildType.AAB);
  }

  @WorkerThread
  private static List<IOException> saveFiles(Project project, List<File> files) {
    List<IOException> exceptions = new ArrayList<>();
    for (File file : files) {
      Module module = project.getModule(file);
      if (module == null) {
        // TODO: try to save files without a module
        continue;
      }

      FileManager fileManager = module.getFileManager();
      Optional<CharSequence> fileContent = fileManager.getFileContent(file);
      if (fileContent.isPresent()) {
        try {
          FileUtils.writeStringToFile(file, fileContent.get().toString(), StandardCharsets.UTF_8);
          Instant instant = Instant.ofEpochMilli(file.lastModified());

          ProgressManager.getInstance().runLater(() -> fileManager.setLastModified(file, instant));
        } catch (IOException e) {
          exceptions.add(e);
        }
      }
    }
    return exceptions;
  }
}
