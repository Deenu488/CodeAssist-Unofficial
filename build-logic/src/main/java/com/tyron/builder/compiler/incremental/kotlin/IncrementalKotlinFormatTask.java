package com.tyron.builder.compiler.incremental.kotlin;

import com.google.common.base.Throwables;
import com.tyron.builder.compiler.BuildType;
import com.tyron.builder.compiler.Task;
import com.tyron.builder.exception.CompilationFailedException;
import com.tyron.builder.log.ILogger;
import com.tyron.builder.project.Project;
import com.tyron.builder.project.api.AndroidModule;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.io.FileUtils;
import org.json.JSONObject;

public class IncrementalKotlinFormatTask extends Task<AndroidModule> {

  private static final String TAG = "formatKotlin";

  private List<File> mKotlinFiles;

  public IncrementalKotlinFormatTask(Project project, AndroidModule module, ILogger logger) {
    super(project, module, logger);
  }

  @Override
  public String getName() {
    return TAG;
  }

  @Override
  public void prepare(BuildType type) throws IOException {
    mKotlinFiles = new ArrayList<>();
    mKotlinFiles.addAll(getKotlinFiles(new File(getModule().getRootFile() + "/src/main/kotlin")));
    mKotlinFiles.addAll(getKotlinFiles(new File(getModule().getRootFile() + "/src/main/java")));
  }

  @Override
  public void run() throws IOException, CompilationFailedException {

    if (mKotlinFiles.isEmpty()) {
      return;
    }

    try {
      File buildSettings =
          new File(
              getModule().getProjectDir(),
              ".idea/" + getModule().getRootFile().getName() + "_compiler_settings.json");
      String content = new String(Files.readAllBytes(Paths.get(buildSettings.getAbsolutePath())));

      JSONObject buildSettingsJson = new JSONObject(content);

      String applyKotlinFormat =
          buildSettingsJson.optJSONObject("kotlin").optString("applyKotlinFormat", "false");

      if (Boolean.parseBoolean(applyKotlinFormat)) {

        for (File mKotlin : mKotlinFiles) {

          String text = new String(Files.readAllBytes(mKotlin.toPath()));

          ByteArrayOutputStream out = new ByteArrayOutputStream();
          ByteArrayOutputStream err = new ByteArrayOutputStream();

          com.facebook.ktfmt.cli.Main main =
              new com.facebook.ktfmt.cli.Main(
                  new ByteArrayInputStream(text.toString().getBytes(StandardCharsets.UTF_8)),
                  new PrintStream(out),
                  new PrintStream(err),
                  new String[] {"-"});
          int exitCode = main.run();

          if (exitCode != 0) {
            getLogger().debug("Error: " + mKotlin.getAbsolutePath() + " " + err.toString());
            throw new CompilationFailedException(TAG + " error");
          }

          String formatted = out.toString();
          if (formatted != null && !formatted.isEmpty()) {
            FileUtils.writeStringToFile(mKotlin, formatted, Charset.defaultCharset());
          }
        }
      }

    } catch (Exception e) {

      throw new CompilationFailedException(Throwables.getStackTraceAsString(e));
    }
  }

  public static Set<File> getKotlinFiles(File dir) {
    Set<File> kotlinFiles = new HashSet<>();

    File[] files = dir.listFiles();
    if (files == null) {
      return Collections.emptySet();
    }

    for (File file : files) {
      if (file.isDirectory()) {
        kotlinFiles.addAll(getKotlinFiles(file));
      } else {
        if (file.getName().endsWith(".kt")) {
          kotlinFiles.add(file);
        }
      }
    }

    return kotlinFiles;
  }
}
