package com.tyron.code.language.kotlin;

import android.content.SharedPreferences;
import androidx.annotation.Nullable;
import com.tyron.builder.project.Project;
import com.tyron.builder.project.api.AndroidModule;
import com.tyron.builder.project.api.KotlinModule;
import com.tyron.builder.project.api.Module;
import com.tyron.code.ApplicationLoader;
import com.tyron.code.language.AbstractAutoCompleteProvider;
import com.tyron.code.ui.project.ProjectManager;
import com.tyron.common.SharedPreferenceKeys;
import com.tyron.completion.model.CompletionList;
import com.tyron.editor.Editor;
import com.tyron.kotlin.completion.core.model.KotlinEnvironment;
import com.tyron.kotlin_completion.CompletionEngine;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment;
import org.json.JSONObject;

public class KotlinAutoCompleteProvider extends AbstractAutoCompleteProvider {

  private static final String TAG = KotlinAutoCompleteProvider.class.getSimpleName();

  private final Editor mEditor;
  private final SharedPreferences mPreferences;

  private KotlinCoreEnvironment environment;

  public KotlinAutoCompleteProvider(Editor editor) {
    mEditor = editor;
    mPreferences = ApplicationLoader.getDefaultPreferences();
  }

  @Nullable
  @Override
  public CompletionList getCompletionList(String prefix, int line, int column) {
    if (!mPreferences.getBoolean(SharedPreferenceKeys.KOTLIN_COMPLETIONS, false)) {
      return null;
    }

    if (com.tyron.completion.java.provider.CompletionEngine.isIndexing()) {
      return null;
    }

    if (!mPreferences.getBoolean(SharedPreferenceKeys.KOTLIN_COMPLETIONS, false)) {
      return null;
    }

    Project project = ProjectManager.getInstance().getCurrentProject();
    if (project == null) {
      return null;
    }

    Module currentModule = project.getModule(mEditor.getCurrentFile());

    if (!(currentModule instanceof AndroidModule)) {
      return null;
    }

    if (environment == null) {
      environment = KotlinEnvironment.getEnvironment((KotlinModule) currentModule);
    }

    if (mEditor.getCurrentFile() == null) {
      return null;
    }

    CompletionEngine engine = CompletionEngine.getInstance((AndroidModule) currentModule);

    if (engine.isIndexing()) {
      return null;
    }

    Project currentProject = ProjectManager.getInstance().getCurrentProject();
    if (currentProject != null) {
      Module module = currentProject.getModule(mEditor.getCurrentFile());
      if (module instanceof AndroidModule) {
        try {
          File buildSettings =
              new File(
                  module.getProjectDir(),
                  ".idea/" + module.getRootFile().getName() + "_compiler_settings.json");
          String json = new String(Files.readAllBytes(Paths.get(buildSettings.getAbsolutePath())));

          JSONObject buildSettingsJson = new JSONObject(json);

          boolean isKotlinCompletionV2 =
              Boolean.parseBoolean(
                  buildSettingsJson
                      .optJSONObject("kotlin")
                      .optString("isKotlinCompletionV2", "false"));

          // waiting for code editor to support async code completions
          if (!isKotlinCompletionV2) {
            return engine.complete(
                mEditor.getCurrentFile(),
                String.valueOf(mEditor.getContent()),
                prefix,
                line,
                column,
                mEditor.getCaret().getStart());
          } else {
            return engine.completeV2(
                mEditor.getCurrentFile(),
                String.valueOf(mEditor.getContent()),
                prefix,
                line,
                column,
                mEditor.getCaret().getStart());
          }
        } catch (Exception e) {
        }
      }
    }

    return null;
  }
}
