package com.tyron.builder.compiler;

import androidx.annotation.NonNull;
import com.tyron.builder.compiler.builder.AndroidLibraryBuilder;
import com.tyron.builder.compiler.builder.JavaLibraryBuilder;
import com.tyron.builder.exception.CompilationFailedException;
import com.tyron.builder.log.ILogger;
import com.tyron.builder.project.Project;
import com.tyron.builder.project.api.AndroidModule;
import com.tyron.builder.project.api.Module;
import java.io.IOException;
import java.util.ArrayList;
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

      List<String> plugins = new ArrayList<>();

      for (String plugin : module.getPlugins()) {
        if (plugin.equals("java-library")
            || plugin.equals("com.android.library")
            || plugin.equals("com.android.application")
            || plugin.equals("kotlin")) {
          plugins.add(plugin);
        }
      }

      String moduleType = plugins.toString();

      if (moduleType.contains("java-library")) {
        builder = new JavaLibraryBuilder(mProject, androidModule, mLogger);
      } else if (moduleType.contains("java-library") && moduleType.contains("kotlin")) {
        builder = new JavaLibraryBuilder(mProject, androidModule, mLogger);
      } else if (moduleType.contains("com.android.library")) {
        builder = new AndroidLibraryBuilder(mProject, androidModule, mLogger);
      } else if (moduleType.contains("com.android.library") && moduleType.contains("kotlin")) {
        builder = new AndroidLibraryBuilder(mProject, androidModule, mLogger);
      } else if (moduleType.contains("com.android.application")) {
        if (type == BuildType.AAB) {
          builder = new AndroidAppBundleBuilder(mProject, androidModule, mLogger);
        } else {
          builder = new AndroidAppBuilder(mProject, androidModule, mLogger);
        }

      } else if (moduleType.contains("com.android.application") && moduleType.contains("kotlin")) {
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
