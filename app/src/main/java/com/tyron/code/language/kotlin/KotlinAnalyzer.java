package com.tyron.code.language.kotlin;

import android.content.res.AssetManager;
import com.tyron.builder.BuildModule;
import com.tyron.builder.project.Project;
import com.tyron.builder.project.api.AndroidModule;
import com.tyron.builder.project.api.JavaModule;
import com.tyron.builder.project.api.Module;
import com.tyron.code.ApplicationLoader;
import com.tyron.code.analyzer.DiagnosticTextmateAnalyzer;
import com.tyron.code.ui.editor.impl.text.rosemoe.CodeEditorView;
import com.tyron.code.ui.project.ProjectManager;
import com.tyron.code.util.TaskExecutor;
import com.tyron.common.SharedPreferenceKeys;
import com.tyron.common.util.BinaryExecutor;
import com.tyron.common.util.ExecutionResult;
import com.tyron.completion.progress.ProgressManager;
import com.tyron.editor.Editor;
import com.tyron.kotlin_completion.CompletionEngine;
import io.github.rosemoe.sora.langs.textmate.theme.TextMateColorScheme;
import io.github.rosemoe.sora.textmate.core.theme.IRawTheme;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.zip.ZipException;
import org.apache.commons.io.FileUtils;
import org.json.JSONObject;

public class KotlinAnalyzer extends DiagnosticTextmateAnalyzer {

  private static final String GRAMMAR_NAME = "kotlin.tmLanguage";
  private static final String LANGUAGE_PATH = "textmate/kotlin/syntaxes/kotlin.tmLanguage";
  private static final String CONFIG_PATH = "textmate/kotlin/language-configuration.json";

  public static KotlinAnalyzer create(Editor editor) {
    try {
      AssetManager assetManager = ApplicationLoader.applicationContext.getAssets();

      try (InputStreamReader config = new InputStreamReader(assetManager.open(CONFIG_PATH))) {
        return new KotlinAnalyzer(
            editor,
            GRAMMAR_NAME,
            assetManager.open(LANGUAGE_PATH),
            config,
            ((TextMateColorScheme) ((CodeEditorView) editor).getColorScheme()).getRawTheme());
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public KotlinAnalyzer(
      Editor editor,
      String grammarName,
      InputStream grammarIns,
      Reader languageConfiguration,
      IRawTheme theme)
      throws Exception {
    super(editor, grammarName, grammarIns, languageConfiguration, theme);
  }

  @Override
  public void analyzeInBackground(CharSequence content) {

    if (mEditor == null) {
      return;
    }

    Project currentProject = ProjectManager.getInstance().getCurrentProject();
    if (currentProject != null) {
      Module module = currentProject.getModule(mEditor.getCurrentFile());
      if (module instanceof AndroidModule) {

        CompletableFuture<String> future =
            TaskExecutor.executeAsyncProvideError(
                () -> {
                  File buildSettings =
                      new File(
                          module.getProjectDir(),
                          ".idea/" + module.getRootFile().getName() + "_compiler_settings.json");
                  String json =
                      new String(Files.readAllBytes(Paths.get(buildSettings.getAbsolutePath())));

                  JSONObject buildSettingsJson = new JSONObject(json);

                  boolean isCompilerEnabled =
                      Boolean.parseBoolean(
                          buildSettingsJson
                              .optJSONObject("kotlin")
                              .optString("isCompilerEnabled", "false"));

                  String jvm_target =
                      buildSettingsJson.optJSONObject("kotlin").optString("jvmTarget", "1.8");

                  if (isCompilerEnabled) {

                    File mClassOutput =
                        new File(
                            module.getBuildDirectory(),
                            "libraries/kotlin_runtime/" + module.getRootFile().getName() + ".jar");
                    if (mClassOutput.getParentFile().exists()) {
                      mClassOutput.getParentFile().mkdirs();
                    }

                    File api_files =
                        new File(module.getRootFile(), "/build/libraries/api_files/libs");
                    File api_libs = new File(module.getRootFile(), "/build/libraries/api_libs");
                    File kotlinOutputDir =
                        new File(module.getBuildDirectory(), "bin/kotlin/classes");
                    File javaOutputDir = new File(module.getBuildDirectory(), "bin/java/classes");
                    File implementation_files =
                        new File(
                            module.getRootFile(), "/build/libraries/implementation_files/libs");
                    File implementation_libs =
                        new File(module.getRootFile(), "/build/libraries/implementation_libs");

                    File runtimeOnly_files =
                        new File(module.getRootFile(), "/build/libraries/runtimeOnly_files/libs");
                    File runtimeOnly_libs =
                        new File(module.getRootFile(), "/build/libraries/runtimeOnly_libs");

                    File compileOnly_files =
                        new File(module.getRootFile(), "/build/libraries/compileOnly_files/libs");
                    File compileOnly_libs =
                        new File(module.getRootFile(), "/build/libraries/compileOnly_libs");

                    File runtimeOnlyApi_files =
                        new File(
                            module.getRootFile(), "/build/libraries/runtimeOnlyApi_files/libs");
                    File runtimeOnlyApi_libs =
                        new File(module.getRootFile(), "/build/libraries/runtimeOnlyApi_libs");

                    File compileOnlyApi_files =
                        new File(
                            module.getRootFile(), "/build/libraries/compileOnlyApi_files/libs");
                    File compileOnlyApi_libs =
                        new File(module.getRootFile(), "/build/libraries/compileOnlyApi_libs");

                    List<File> compileClassPath = new ArrayList<>();
                    compileClassPath.addAll(getJarFiles(api_files));
                    compileClassPath.addAll(getJarFiles(api_libs));
                    compileClassPath.addAll(getJarFiles(implementation_files));
                    compileClassPath.addAll(getJarFiles(implementation_libs));
                    compileClassPath.addAll(getJarFiles(compileOnly_files));
                    compileClassPath.addAll(getJarFiles(compileOnly_libs));
                    compileClassPath.addAll(getJarFiles(compileOnlyApi_files));
                    compileClassPath.addAll(getJarFiles(compileOnlyApi_libs));

                    compileClassPath.add(javaOutputDir);
                    compileClassPath.add(kotlinOutputDir);

                    List<File> runtimeClassPath = new ArrayList<>();
                    runtimeClassPath.addAll(getJarFiles(runtimeOnly_files));
                    runtimeClassPath.addAll(getJarFiles(runtimeOnly_libs));
                    runtimeClassPath.addAll(getJarFiles(runtimeOnlyApi_files));
                    runtimeClassPath.addAll(getJarFiles(runtimeOnlyApi_libs));
                    runtimeClassPath.add(BuildModule.getAndroidJar());
                    runtimeClassPath.add(BuildModule.getLambdaStubs());
                    runtimeClassPath.addAll(getJarFiles(api_files));
                    runtimeClassPath.addAll(getJarFiles(api_libs));

                    runtimeClassPath.add(javaOutputDir);
                    runtimeClassPath.add(kotlinOutputDir);

                    List<File> classpath = new ArrayList<>();
                    classpath.add(module.getBuildClassesDirectory());

                    AndroidModule androidModule = (AndroidModule) module;
                    if (androidModule instanceof JavaModule) {
                      JavaModule javaModule = (JavaModule) androidModule;
                      classpath.addAll(javaModule.getLibraries());
                    }

                    classpath.addAll(compileClassPath);
                    classpath.addAll(runtimeClassPath);

                    List<String> arguments = new ArrayList<>();
                    Collections.addAll(
                        arguments,
                        classpath.stream()
                            .map(File::getAbsolutePath)
                            .collect(Collectors.joining(File.pathSeparator)));

                    File javaDir = new File(module.getRootFile() + "/src/main/java");
                    File kotlinDir = new File(module.getRootFile() + "/src/main/kotlin");
                    File buildGenDir = new File(module.getRootFile() + "/build/gen");
                    File viewBindingDir = new File(module.getRootFile() + "/build/view_binding");

                    List<File> javaSourceRoots = new ArrayList<>();
                    if (javaDir.exists()) {
                      javaSourceRoots.add(javaDir);
                    }
                    if (buildGenDir.exists()) {
                      javaSourceRoots.add(buildGenDir);
                    }
                    if (viewBindingDir.exists()) {
                      javaSourceRoots.add(viewBindingDir);
                    }

                    List<File> fileList = new ArrayList<>();
                    if (javaDir.exists()) {
                      fileList.add(javaDir);
                    }
                    if (buildGenDir.exists()) {
                      fileList.add(buildGenDir);
                    }
                    if (viewBindingDir.exists()) {
                      fileList.add(viewBindingDir);
                    }
                    if (kotlinDir.exists()) {
                      fileList.add(kotlinDir);
                    }

                    List<String> args = new ArrayList<>();
                    args.add("dalvikvm");
                    args.add("-Xcompiler-option");
                    args.add("--compiler-filter=speed");
                    args.add("-Xmx256m");
                    args.add("-cp");
                    args.add(BuildModule.getKotlinc().getAbsolutePath());
                    args.add("org.jetbrains.kotlin.cli.jvm.K2JVMCompiler");

                    args.add("-no-jdk");
                    args.add("-no-stdlib");
                    args.add("-no-reflect");
                    args.add("-jvm-target");
                    args.add(jvm_target);
                    args.add("-cp");
                    args.add(String.join(", ", arguments));
                    args.add(
                        "-Xjava-source-roots="
                            + String.join(
                                ", ",
                                javaSourceRoots.stream()
                                    .map(File::getAbsolutePath)
                                    .toArray(String[]::new)));

                    for (File file : fileList) {
                      args.add(file.getAbsolutePath());
                    }

                    args.add("-d");
                    args.add(mClassOutput.getAbsolutePath());

                    args.add("-module-name");
                    args.add(module.getRootFile().getName());
                    List<File> plugins = getPlugins(module);
                    String plugin = "";
                    String pluginString =
                        Arrays.toString(
                                plugins.stream().map(File::getAbsolutePath).toArray(String[]::new))
                            .replace("[", "")
                            .replace("]", "");

                    String pluginOptionsString =
                        Arrays.toString(getPluginOptions(module)).replace("[", "").replace("]", "");

                    plugin =
                        pluginString
                            + ":"
                            + (pluginOptionsString.isEmpty() ? ":=" : pluginOptionsString);

                    args.add("-P");
                    args.add("plugin:" + plugin);

                    BinaryExecutor executor = new BinaryExecutor();
                    executor.setCommands(args);
                    ExecutionResult result = executor.run();
                  }

                  return null;
                },
                (result, throwable) -> {});

        if (ApplicationLoader.getDefaultPreferences()
            .getBoolean(SharedPreferenceKeys.KOTLIN_HIGHLIGHTING, true)) {
          ProgressManager.getInstance()
              .runLater(
                  () -> {
                    mEditor.setAnalyzing(true);

                    CompletionEngine.getInstance((AndroidModule) module)
                        .doLint(
                            mEditor.getCurrentFile(),
                            content.toString(),
                            diagnostics -> {
                              mEditor.setDiagnostics(diagnostics);

                              ProgressManager.getInstance()
                                  .runLater(() -> mEditor.setAnalyzing(false), 300);
                            });
                  },
                  900);
        }
      }
    }
  }

  public List<File> getFiles(File dir) {
    List<File> ktfiles = new ArrayList<>();
    File[] files = dir.listFiles();
    if (files != null) {
      for (File file : files) {
        if (file.isFile() && file.getName().endsWith(".kt")) {
          ktfiles.add(file);
        } else if (file.isDirectory()) {
          ktfiles.addAll(getFiles(file));
        }
      }
    }
    return ktfiles;
  }

  public List<File> getJarFiles(File dir) {
    List<File> jarFiles = new ArrayList<>();
    File[] files = dir.listFiles();
    if (files != null) {
      for (File file : files) {
        if (file.isFile() && file.getName().endsWith(".jar")) {
          // Check if the JarFile is valid before adding it to the list
          if (isJarFileValid(file)) {
            jarFiles.add(file);
          }
        } else if (file.isDirectory()) {
          // Recursively add JarFiles from subdirectories
          jarFiles.addAll(getJarFiles(file));
        }
      }
    }
    return jarFiles;
  }

  public boolean isJarFileValid(File file) {
    String message = "File " + file.getParentFile().getName() + " is corrupt! Ignoring.";
    try {
      // Try to open the JarFile
      JarFile jarFile = new JarFile(file);
      // If it opens successfully, close it and return true
      jarFile.close();
      return true;
    } catch (ZipException e) {
      // If the JarFile is invalid, it will throw a ZipException

      return false;
    } catch (IOException e) {
      // If there is some other error reading the JarFile, return false

      return false;
    }
  }

  private List<File> getPlugins(Module module) {
    File pluginDir = new File(module.getBuildDirectory(), "plugins");
    File[] children = pluginDir.listFiles(c -> c.getName().endsWith(".jar"));

    if (children == null) {
      return Collections.emptyList();
    }

    return Arrays.stream(children).collect(Collectors.toList());
  }

  private String[] getPluginOptions(Module module) throws IOException {
    File pluginDir = new File(module.getBuildDirectory(), "plugins");
    File args = new File(pluginDir, "args.txt");
    if (!args.exists()) {
      return new String[0];
    }

    String string = FileUtils.readFileToString(args, StandardCharsets.UTF_8);
    return string.split(" ");
  }
}
