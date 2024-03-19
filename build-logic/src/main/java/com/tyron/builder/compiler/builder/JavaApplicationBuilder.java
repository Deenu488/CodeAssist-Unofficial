package com.tyron.builder.compiler.builder;

import com.tyron.builder.compiler.BuildType;
import com.tyron.builder.compiler.BuilderImpl;
import com.tyron.builder.compiler.CleanTask;
import com.tyron.builder.compiler.Task;
import com.tyron.builder.compiler.apk.PackageTask;
import com.tyron.builder.compiler.dex.R8Task;
import com.tyron.builder.compiler.incremental.dex.IncrementalD8Task;
import com.tyron.builder.compiler.incremental.java.IncrementalJavaFormatTask;
import com.tyron.builder.compiler.incremental.java.IncrementalJavaTask;
import com.tyron.builder.compiler.incremental.kotlin.IncrementalKotlinCompiler;
import com.tyron.builder.compiler.incremental.kotlin.IncrementalKotlinFormatTask;
import com.tyron.builder.compiler.incremental.resource.IncrementalAssembleLibraryTask;
import com.tyron.builder.compiler.java.CheckLibrariesTask;
import com.tyron.builder.compiler.java.RunTask;
import com.tyron.builder.log.ILogger;
import com.tyron.builder.project.Project;
import com.tyron.builder.project.api.AndroidModule;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.json.JSONObject;

public class JavaApplicationBuilder extends BuilderImpl<AndroidModule> {

  public JavaApplicationBuilder(Project project, AndroidModule module, ILogger logger) {
    super(project, module, logger);
  }

  @Override
  public List<Task<? super AndroidModule>> getTasks(BuildType type) {

    AndroidModule module = getModule();
    ILogger logger = getLogger();

    List<Task<? super AndroidModule>> tasks = new ArrayList<>();
    tasks.add(new CleanTask(getProject(), module, logger));

    tasks.add(new IncrementalKotlinFormatTask(getProject(), module, logger));
    tasks.add(new IncrementalJavaFormatTask(getProject(), module, logger));

    tasks.add(new CheckLibrariesTask(getProject(), module, getLogger()));

    try {
      File buildSettings =
          new File(
              getProject().getRootFile(),
              ".idea/" + getProject().getRootName() + "_compiler_settings.json");
      String content = new String(Files.readAllBytes(Paths.get(buildSettings.getAbsolutePath())));

      JSONObject buildSettingsJson = new JSONObject(content);

      boolean isDexLibrariesOnPrebuild =
          Optional.ofNullable(buildSettingsJson.optJSONObject("dex"))
              .map(json -> json.optString("isDexLibrariesOnPrebuild", "false"))
              .map(Boolean::parseBoolean)
              .orElse(false);

      if (isDexLibrariesOnPrebuild) {
        tasks.add(new IncrementalD8Task(getProject(), module, logger));
      }
    } catch (Exception e) {
    }

    tasks.add(new IncrementalAssembleLibraryTask(getProject(), module, getLogger()));
    tasks.add(new IncrementalKotlinCompiler(getProject(), module, logger));
    tasks.add(new IncrementalJavaTask(getProject(), module, getLogger()));
    if (type == BuildType.RELEASE) {
      tasks.add(new R8Task(getProject(), module, getLogger()));
    } else {
      tasks.add(new IncrementalD8Task(getProject(), module, getLogger()));
    }
    tasks.add(new PackageTask(getProject(), module, getLogger()));
    tasks.add(new RunTask(getProject(), module, getLogger()));

    return tasks;
  }
}
