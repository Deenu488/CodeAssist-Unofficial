package com.tyron.builder.compiler.incremental.java;

import com.tyron.builder.compiler.BuildType;
import com.tyron.builder.compiler.Task;
import com.tyron.builder.exception.CompilationFailedException;
import com.tyron.builder.log.ILogger;
import com.tyron.builder.project.Project;
import com.tyron.builder.project.api.JavaModule;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.io.FileUtils;
import org.json.JSONObject;

public class IncrementalJavaFormatTask extends Task<JavaModule> {

  private static final String TAG = "formatJava";
  private List<File> mJavaFiles;

  public IncrementalJavaFormatTask(Project project, JavaModule module, ILogger logger) {
    super(project, module, logger);
  }

  @Override
  public String getName() {
    return TAG;
  }

  @Override
  public void prepare(BuildType type) throws IOException {
    mJavaFiles = new ArrayList<>();
    mJavaFiles.addAll(getJavaFiles(new File(getModule().getRootFile() + "/src/main/java")));
  }

  @Override
  public void run() throws IOException, CompilationFailedException {
    if (mJavaFiles.isEmpty()) {
      return;
    }

    try {
      File buildSettings =
          new File(
              getModule().getProjectDir(),
              ".idea/" + getModule().getRootFile().getName() + "_compiler_settings.json");
      String content = new String(Files.readAllBytes(Paths.get(buildSettings.getAbsolutePath())));

      JSONObject buildSettingsJson = new JSONObject(content);

      String applyJavaFormat =
          buildSettingsJson.optJSONObject("java").optString("applyJavaFormat", "false");
      String isGoogleJavaFormat =
          buildSettingsJson.optJSONObject("java").optString("isGoogleJavaFormat", "false");

      if (Boolean.parseBoolean(applyJavaFormat)) {

        for (File mJava : mJavaFiles) {

          if (   ! Boolean.parseBoolean(  isGoogleJavaFormat)) {

            String text = new String(Files.readAllBytes(mJava.toPath()));

            String formatted = com.tyron.eclipse.formatter.Formatter.format(text, 0);

            if (formatted != null && !formatted.isEmpty()) {
              FileUtils.writeStringToFile(mJava, formatted, Charset.defaultCharset());
            }

          } else {
          }
        }
      }

    } catch (Exception e) {
      throw new CompilationFailedException(e);
    }
  }

  public static Set<File> getJavaFiles(File dir) {
    Set<File> javaFiles = new HashSet<>();

    File[] files = dir.listFiles();
    if (files == null) {
      return Collections.emptySet();
    }

    for (File file : files) {
      if (file.isDirectory()) {
        javaFiles.addAll(getJavaFiles(file));
      } else {
        if (file.getName().endsWith(".java")) {
          javaFiles.add(file);
        }
      }
    }

    return javaFiles;
  }
}