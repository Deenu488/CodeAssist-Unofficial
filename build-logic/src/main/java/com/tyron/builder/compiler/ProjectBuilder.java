package com.tyron.builder.compiler;

import androidx.annotation.NonNull;
import com.tyron.builder.compiler.builder.AndroidLibraryBuilder;
import com.tyron.builder.compiler.builder.JavaApplicationBuilder;
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
            || plugin.equals("kotlin")
            || plugin.equals("application")
            || plugin.equals("kotlin-android")) {
          plugins.add(plugin);
        }
      }

      String moduleType = plugins.toString();

      if (moduleType.equals("[java-library]")) {
        builder = new JavaLibraryBuilder(mProject, androidModule, mLogger);
      } else if (moduleType.equals("[java-library, kotlin]")
          || moduleType.equals("[kotlin, java-library]")) {
        builder = new JavaLibraryBuilder(mProject, androidModule, mLogger);
      } else if (moduleType.equals("[com.android.library]")) {
        builder = new AndroidLibraryBuilder(mProject, androidModule, mLogger);
      } else if (moduleType.equals("[com.android.library, kotlin]")
          || moduleType.equals("[kotlin, com.android.library]")) {
        builder = new AndroidLibraryBuilder(mProject, androidModule, mLogger);
      } else if (moduleType.equals("[com.android.application]")) {
        if (type == BuildType.AAB) {
          builder = new AndroidAppBundleBuilder(mProject, androidModule, mLogger);
        } else {
          builder = new AndroidAppBuilder(mProject, androidModule, mLogger);
        }
      } else if (moduleType.equals("[application]")) {
        builder = new JavaApplicationBuilder(mProject, androidModule, mLogger);
      } else if (moduleType.equals("[com.android.application, kotlin-android]")
          || moduleType.equals("[kotlin-android, com.android.application]")) {
        if (type == BuildType.AAB) {
          builder = new AndroidAppBundleBuilder(mProject, androidModule, mLogger);
        } else {
          builder = new AndroidAppBuilder(mProject, androidModule, mLogger);
        }
      } else {
        throw new CompilationFailedException(
            "Unable to find any plugins in "
                + mProject.getRootFile().getName()
                + "/build.gradle, check project's gradle plugins and build again.");
      }
      builder.setTaskListener(mTaskListener);
      builder.build(type);
    }
  }
}
