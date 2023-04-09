package com.tyron.builder.compiler.builder;

import com.tyron.builder.compiler.BuildType;
import com.tyron.builder.compiler.BuilderImpl;
import com.tyron.builder.compiler.CleanTask;
import com.tyron.builder.compiler.Task;
import com.tyron.builder.compiler.aar.BuildAarTask;
import com.tyron.builder.compiler.buildconfig.GenerateDebugBuildConfigTask;
import com.tyron.builder.compiler.buildconfig.GenerateReleaseBuildConfigTask;
import com.tyron.builder.compiler.incremental.java.IncrementalJavaTask;
import com.tyron.builder.compiler.incremental.kotlin.IncrementalKotlinCompiler;
import com.tyron.builder.compiler.incremental.resource.IncrementalAapt2Task;
import com.tyron.builder.compiler.incremental.resource.IncrementalAssembleLibraryTask;
import com.tyron.builder.compiler.java.CheckLibrariesTask;
import com.tyron.builder.compiler.manifest.ManifestMergeTask;
import com.tyron.builder.compiler.symbol.MergeSymbolsTask;
import com.tyron.builder.compiler.viewbinding.GenerateViewBindingTask;
import com.tyron.builder.log.ILogger;
import com.tyron.builder.project.Project;
import com.tyron.builder.project.api.AndroidModule;
import java.util.ArrayList;
import java.util.List;

public class AndroidLibraryBuilder extends BuilderImpl<AndroidModule> {

  public AndroidLibraryBuilder(Project project, AndroidModule module, ILogger logger) {
    super(project, module, logger);
  }

  @Override
  public List<Task<? super AndroidModule>> getTasks(BuildType type) {

    AndroidModule module = getModule();
    ILogger logger = getLogger();

    List<Task<? super AndroidModule>> tasks = new ArrayList<>();
    tasks.add(new CleanTask(getProject(), module, logger));
    tasks.add(new CheckLibrariesTask(getProject(), module, getLogger()));
    tasks.add(new IncrementalAssembleLibraryTask(getProject(), module, getLogger()));
    tasks.add(new ManifestMergeTask(getProject(), module, logger));
    if (type == BuildType.DEBUG) {
      tasks.add(new GenerateDebugBuildConfigTask(getProject(), module, logger));
    } else {
      tasks.add(new GenerateReleaseBuildConfigTask(getProject(), module, logger));
    }
    tasks.add(new IncrementalAapt2Task(getProject(), module, logger, false));
    tasks.add(new GenerateViewBindingTask(getProject(), module, logger, true));
    tasks.add(new MergeSymbolsTask(getProject(), module, logger));
    tasks.add(new IncrementalKotlinCompiler(getProject(), module, logger));
    tasks.add(new IncrementalJavaTask(getProject(), module, getLogger()));
    tasks.add(new BuildAarTask(getProject(), module, getLogger()));
    return tasks;
  }
}
