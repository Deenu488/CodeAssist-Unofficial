package com.tyron.builder.compiler;

import androidx.annotation.NonNull;
import com.tyron.builder.compiler.builder.JavaLibraryBuilder;
import com.tyron.builder.exception.CompilationFailedException;
import com.tyron.builder.log.ILogger;
import com.tyron.builder.project.Project;
import com.tyron.builder.project.api.AndroidModule;
import com.tyron.builder.project.api.JavaModule;
import com.tyron.builder.project.api.Module;
import java.io.IOException;
import java.util.List;

public class ProjectBuilder {

  private final List<Module> mModules;
  private final Project mProject;
  private final ILogger mLogger;
  private Builder.TaskListener mTaskListener;

  public ProjectBuilder(Project project, ILogger logger) throws IOException {
    mProject = project;
    mLogger = logger;
    mModules = project.getBuildOrder();
  }

  public void setTaskListener(@NonNull Builder.TaskListener listener) {
    mTaskListener = listener;
  }

  public void build(BuildType type) throws IOException, CompilationFailedException {
    for (Module module : mModules) {
      module.clear();
      module.open();
      module.index();

      Builder<? extends Module> builder = null;
      AndroidModule androidModule = (AndroidModule) module;
      String moduleType = module.getPlugins();

      if (moduleType.contains("java-library")) {
        builder = new JarBuilder(mProject, (JavaModule) module, mLogger);
      } else if (moduleType.contains("com.android.application")) {
        if (type == BuildType.AAB) {
          builder = new AndroidAppBundleBuilder(mProject, androidModule, mLogger);
        } else {
          builder = new AndroidAppBuilder(mProject, androidModule, mLogger);
        }
      } else {
        throw new CompilationFailedException(
            "Unabled to find any plugins, check project plugins and refresh module");
      }

      builder.setTaskListener(mTaskListener);
      builder.build(type);
    }
  }
}
