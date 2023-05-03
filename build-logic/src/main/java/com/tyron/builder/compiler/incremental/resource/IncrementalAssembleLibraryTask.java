package com.tyron.builder.compiler.incremental.resource;

import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.NonNull;
import com.android.tools.aapt2.Aapt2Jni;
import com.google.common.base.Throwables;
import com.sun.source.util.JavacTask;
import com.sun.tools.javac.api.JavacTool;
import com.sun.tools.javac.file.JavacFileManager;
import com.tyron.builder.compiler.BuildType;
import com.tyron.builder.compiler.Task;
import com.tyron.builder.compiler.buildconfig.GenerateDebugBuildConfigTask;
import com.tyron.builder.compiler.buildconfig.GenerateReleaseBuildConfigTask;
import com.tyron.builder.compiler.jar.BuildJarTask;
import com.tyron.builder.compiler.manifest.ManifestMergeTask;
import com.tyron.builder.exception.CompilationFailedException;
import com.tyron.builder.internal.jar.AssembleJar;
import com.tyron.builder.log.ILogger;
import com.tyron.builder.log.LogUtils;
import com.tyron.builder.model.DiagnosticWrapper;
import com.tyron.builder.model.ModuleSettings;
import com.tyron.builder.project.Project;
import com.tyron.builder.project.api.AndroidModule;
import com.tyron.builder.project.cache.CacheHolder;
import com.tyron.common.util.Cache;
import com.tyron.common.util.Decompress;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipOutputStream;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardLocation;
import kotlin.jvm.functions.Function0;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.build.report.ICReporterBase;
import org.jetbrains.kotlin.cli.common.ExitCode;
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments;
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity;
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation;
import org.jetbrains.kotlin.cli.common.messages.MessageCollector;
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler;
import org.jetbrains.kotlin.incremental.IncrementalJvmCompilerRunnerKt;

public class IncrementalAssembleLibraryTask extends Task<AndroidModule> {

  public static final CacheHolder.CacheKey<String, List<File>> CACHE_KEY =
      new CacheHolder.CacheKey<>("javaCache");
  private static final String TAG = "assembleLibraries";
  private Cache<String, List<File>> mClassCache;
  private List<File> subCompileClassPath = new ArrayList<>();
  private List<File> subRuntimeClassPath = new ArrayList<>();
  private final MessageCollector mCollector = new Collector();
  private BuildType mBuildType;
  private Set<String> builtProjects = new HashSet<>();

  public IncrementalAssembleLibraryTask(Project project, AndroidModule module, ILogger logger) {
    super(project, module, logger);
  }

  @Override
  public String getName() {
    return TAG;
  }

  @Override
  public void prepare(BuildType type) throws IOException {
    mBuildType = type;
  }

  public void run() throws IOException, CompilationFailedException {
    List<String> projects = new ArrayList<>();
    projects.addAll(getModule().getAllProjects(getModule().getGradleFile()));
    initializeProjects(getModule().getProjectDir(), projects);
  }

  private void initializeProjects(File directory, List<String> rootProjects)
      throws IOException, CompilationFailedException {
    Map<Integer, List<String>> projectsByInclusion = new HashMap<>();
    int maxInclusion = 0;
    for (String projectName : rootProjects) {
      List<String> subProjects =
          getModule().getAllProjects(new File(directory, projectName + "/build.gradle"));
      int numSubProjects = subProjects.size();
      if (numSubProjects == 0) {
        projectsByInclusion
            .computeIfAbsent(numSubProjects, k -> new ArrayList<>())
            .add(projectName);
      }
    }
    for (String projectName : rootProjects) {
      List<String> subProjects =
          getModule().getAllProjects(new File(directory, projectName + "/build.gradle"));
      int numSubProjects = subProjects.size();
      if (numSubProjects > 0) {
        maxInclusion = Math.max(maxInclusion, numSubProjects);
        projectsByInclusion
            .computeIfAbsent(numSubProjects, k -> new ArrayList<>())
            .add(projectName);
      }
    }
    for (int i = 0; i <= maxInclusion; i++) {
      if (projectsByInclusion.containsKey(i)) {
        List<String> projects = projectsByInclusion.get(i);
        processProjects(directory, projects);
      }
    }
  }

  private void processProjects(File projectDir, List<String> projects)
      throws IOException, CompilationFailedException {

    for (String projectName : projects) {
      String name = projectName.replaceFirst("/", "").replaceAll("/", ":");
      // getLogger().debug("Project: " + name);

      Set<String> processedSubProjects = new HashSet<>();
      subCompileClassPath.clear();
      subRuntimeClassPath.clear();
      prepairSubProjects(projectDir, name, processedSubProjects);
    }
  }

  private void prepairSubProjects(File projectDir, String name, Set<String> processedSubProjects)
      throws IOException, CompilationFailedException {

    File gradleFile = new File(projectDir, name + "/build.gradle");

    List<String> subProjects = getModule().getAllProjects(gradleFile);

    while (!subProjects.isEmpty()) {
      String subProject = subProjects.remove(0);
      String subName = subProject.replaceFirst("/", "").replaceAll("/", ":");

      if (processedSubProjects.contains(subName)) {
        // getLogger().debug("Skipping duplicate sub-project: " + subName);
        continue;
      } else {
        processedSubProjects.add(subName);
      }

      File sub_libraries = new File(projectDir, subName + "/build/libraries");

      List<String> sub =
          getModule().getAllProjects(new File(projectDir, subName + "/build.gradle"));

      for (String projectName : sub) {
        String n = projectName.replaceFirst("/", "").replaceAll("/", ":");
        File l = new File(projectDir, n + "/build/libraries");
        prepairSubProjects(projectDir, n, processedSubProjects);
        subCompileClassPath.addAll(getCompileClassPath(l));
        subRuntimeClassPath.addAll(getRuntimeClassPath(l));
        subCompileClassPath.addAll(addToClassPath(l));
        subRuntimeClassPath.addAll(addToClassPath(l));
      }

      // getLogger().debug("Building sub project: " + subName);

      subCompileClassPath.addAll(getCompileClassPath(sub_libraries));
      subRuntimeClassPath.addAll(getRuntimeClassPath(sub_libraries));
      buildSubProject(projectDir, subName, subCompileClassPath, subRuntimeClassPath);
    }

    if (!name.isEmpty()) {
      if (processedSubProjects.contains(name)) {
        // getLogger().debug("Skipping duplicate project: " + name);
        return;
      }

      processedSubProjects.add(name);

      File libraries = new File(projectDir, name + "/build/libraries");

      // getLogger().debug("Building project: " + name);
      subCompileClassPath.addAll(addToClassPath(libraries));
      subRuntimeClassPath.addAll(addToClassPath(libraries));
      subCompileClassPath.addAll(getCompileClassPath(libraries));
      subRuntimeClassPath.addAll(getRuntimeClassPath(libraries));
      buildProject(projectDir, name, subCompileClassPath, subRuntimeClassPath);
    }
  }

  private void buildSubProject(
      File projectDir, String subName, List<File> compileClassPath, List<File> runtimeClassPath)
      throws CompilationFailedException, IOException {
    File subGradleFile = new File(projectDir, subName + "/build.gradle");
    List<String> pluginTypes = new ArrayList<>();
    if (builtProjects.contains(subName)) {
      // getLogger().debug("Already built project: " + subName);
      pluginTypes = getPlugins(subName, subGradleFile);
    } else {
      pluginTypes = checkPlugins(subName, subGradleFile);
    }
    if (pluginTypes.isEmpty()) {
      getLogger().error("No plugins applied");
      throw new CompilationFailedException(
          "Unable to find any plugins in "
              + subName
              + "/build.gradle, check project's gradle plugins and build again.");
    }

    String pluginType = pluginTypes.toString();
    compileProject(pluginType, projectDir, subName, compileClassPath, runtimeClassPath);
  }

  private void buildProject(
      File projectDir, String name, List<File> compileClassPath, List<File> runtimeClassPath)
      throws CompilationFailedException, IOException {
    File gradleFile = new File(projectDir, name + "/build.gradle");
    List<String> pluginTypes = new ArrayList<>();
    if (builtProjects.contains(name)) {
      // getLogger().debug("Already built project: " + name);
      pluginTypes = getPlugins(name, gradleFile);
    } else {
      pluginTypes = checkPlugins(name, gradleFile);
    }

    if (pluginTypes.isEmpty()) {
      getLogger().error("No plugins applied");
      throw new CompilationFailedException(
          "Unable to find any plugins in "
              + name
              + "/build.gradle, check project's gradle plugins and build again.");
    }

    String pluginType = pluginTypes.toString();
    compileProject(pluginType, projectDir, name, compileClassPath, runtimeClassPath);
  }

  public static boolean hasDirectoryBeenModifiedSinceLastRun(Set<File> files, File config)
      throws IOException {
    if (files.isEmpty()) {
      return false;
    }
    List<File> fileList = new ArrayList<>(files);
    File lastModifiedFile = fileList.get(0);
    for (int i = 0; i < fileList.size(); i++) {
      if (lastModifiedFile.lastModified() < fileList.get(i).lastModified()) {
        lastModifiedFile = fileList.get(i);
      }
    }
    ModuleSettings myModuleSettings = new ModuleSettings(config);
    long lastModifiedTime = Long.parseLong(myModuleSettings.getString("lastBuildTime", "0"));
    if (lastModifiedTime >= lastModifiedFile.lastModified()) {
      return false;
    } else {
      myModuleSettings
          .edit()
          .putString("lastBuildTime", String.valueOf(lastModifiedFile.lastModified()))
          .apply();
      return true;
    }
  }

  private List<String> checkPlugins(String name, File gradleFile) {
    List<String> plugins = new ArrayList<>();
    List<String> unsupported_plugins = new ArrayList<>();
    for (String plugin : getModule().getPlugins(gradleFile)) {
      if (plugin.equals("java-library")
          || plugin.equals("com.android.library")
          || plugin.equals("kotlin")
          || plugin.equals("kotlin-android")) {
        plugins.add(plugin);
      } else {
        unsupported_plugins.add(plugin);
      }
    }
    String pluginType = plugins.toString();

    getLogger().debug("> Task :" + name + ":" + "checkingPlugins");
    if (plugins.isEmpty()) {
    } else {
      getLogger().debug("NOTE: " + "Plugins applied: " + plugins.toString());
      if (unsupported_plugins.isEmpty()) {
      } else {
        getLogger()
            .debug(
                "NOTE: "
                    + "Unsupported plugins: "
                    + unsupported_plugins.toString()
                    + " will not be used");
      }
    }
    return plugins;
  }

  private List<String> getPlugins(String projectName, File gradleFile) {
    List<String> plugins = new ArrayList<>();
    List<String> unsupported_plugins = new ArrayList<>();
    for (String plugin : getModule().getPlugins(gradleFile)) {
      if (plugin.equals("java-library")
          || plugin.equals("com.android.library")
          || plugin.equals("kotlin")
          || plugin.equals("kotlin-android")) {
        plugins.add(plugin);
      } else {
        unsupported_plugins.add(plugin);
      }
    }

    return plugins;
  }

  public void compileProject(
      String pluginType,
      File projectDir,
      String projectName,
      List<File> compileClassPath,
      List<File> runtimeClassPath)
      throws IOException, CompilationFailedException {
    File gradleFile = new File(projectDir, projectName + "/build.gradle");
    File jarDir = new File(projectDir, projectName + "/build/outputs/jar");
    File jarFileDir = new File(jarDir, projectName + ".jar");
    File root = new File(projectDir, projectName);
    File javaDir = new File(projectDir, projectName + "/src/main/java");
    File kotlinDir = new File(projectDir, projectName + "/src/main/kotlin");
    File javaClassesDir = new File(projectDir, projectName + "/build/classes/java/main");
    File kotlinClassesDir = new File(projectDir, projectName + "/build/classes/kotlin/main");
    File transformsDir =
        new File(projectDir, projectName + "/build/.transforms/transformed/" + projectName);
    File transformedJarFileDir = new File(transformsDir, projectName + ".jar");
    File classesJarFileDir = new File(transformsDir, "classes.jar");

    File resDir = new File(projectDir, projectName + "/src/main/res");
    File binResDir = new File(projectDir, projectName + "/build/bin/res");
    File buildDir = new File(projectDir, projectName + "/build");
    File buildGenDir = new File(projectDir, projectName + "/build/gen");
    File viewBindingDir = new File(projectDir, projectName + "/build/view_binding");
    File manifestBinFileDir = new File(projectDir, projectName + "/build/bin/AndroidManifest.xml");
    File manifestFileDir = new File(projectDir, projectName + "/src/main/AndroidManifest.xml");
    File assetsDir = new File(projectDir, projectName + "/src/main/assets");
    File aarDir = new File(projectDir, projectName + "/build/bin/aar");
    File outputsDir = new File(projectDir, projectName + "/build/outputs/aar");
    File aarFileDir = new File(outputsDir, projectName + ".aar");

    File config = new File(jarDir, "last-build.bin");

    Set<File> javaFiles = new HashSet<>();
    Set<File> kotlinFiles = new HashSet<>();

    if (pluginType.equals("[java-library]")) {
      if (builtProjects.contains(projectName)) {
        // getLogger().debug("Already built project: " + projectName);
        subCompileClassPath.add(new File(transformsDir, "classes.jar"));
        subRuntimeClassPath.add(new File(transformsDir, "classes.jar"));
        getModule().addLibrary(new File(transformsDir, "classes.jar"));
        return;
      }
      javaFiles.addAll(getFiles(javaDir, ".java"));
      javaFiles.addAll(getFiles(buildGenDir, ".java"));
      if (!jarFileDir.exists() || hasDirectoryBeenModifiedSinceLastRun(javaFiles, config)) {
        // If the JAR file directory doesn't exist or the Java files have been modified,
        // compile the Java files, create a JAR file, and add the JAR file to the classpaths.

        compileClassPath.add(javaClassesDir);
        runtimeClassPath.add(javaClassesDir);
        compileJava(javaFiles, javaClassesDir, projectName, compileClassPath, runtimeClassPath);
        BuildJarTask buildJarTask = new BuildJarTask(getProject(), getModule(), getLogger());
        buildJarTask.assembleJar(javaClassesDir, jarFileDir);
        getLogger().debug("> Task :" + projectName + ":" + "jar");
        copyResources(jarFileDir, transformsDir.getAbsolutePath());
        if (!transformedJarFileDir.renameTo(classesJarFileDir)) {
          getLogger().warning("Failed to rename " + transformedJarFileDir.getName() + " file");
        }
      } else {
        getLogger().debug("> Task :" + projectName + ":" + "jar SKIPPED");
      }
      builtProjects.add(projectName);
      subCompileClassPath.add(new File(transformsDir, "classes.jar"));
      subRuntimeClassPath.add(new File(transformsDir, "classes.jar"));
      getModule().addLibrary(new File(transformsDir, "classes.jar"));

    } else if (pluginType.equals("[java-library, kotlin]")
        || pluginType.equals("[kotlin, java-library]")) {
      if (builtProjects.contains(projectName)) {
        // getLogger().debug("Already built project: " + projectName);
        subCompileClassPath.add(new File(transformsDir, "classes.jar"));
        subRuntimeClassPath.add(new File(transformsDir, "classes.jar"));
        getModule().addLibrary(new File(transformsDir, "classes.jar"));
        return;
      }
      kotlinFiles.addAll(getFiles(kotlinDir, ".kt"));
      kotlinFiles.addAll(getFiles(javaDir, ".kt"));
      javaFiles.addAll(getFiles(javaDir, ".java"));
      javaFiles.addAll(getFiles(buildGenDir, ".java"));
      List<File> sourceFolders = new ArrayList<>();
      sourceFolders.add(new File(projectDir, projectName + "/build/classes/java/main"));
      sourceFolders.add(new File(projectDir, projectName + "/build/classes/kotlin/main"));

      if (!jarFileDir.exists()
          || hasDirectoryBeenModifiedSinceLastRun(kotlinFiles, config)
          || hasDirectoryBeenModifiedSinceLastRun(javaFiles, config)) {
        // If the JAR file directory doesn't exist or the Java files have been modified,
        // compile the Java files, create a JAR file, and add the JAR file to the classpaths.

        compileKotlin(
            kotlinFiles, kotlinClassesDir, projectName, compileClassPath, runtimeClassPath);
        compileClassPath.add(javaClassesDir);
        runtimeClassPath.add(javaClassesDir);
        compileClassPath.add(kotlinClassesDir);
        runtimeClassPath.add(kotlinClassesDir);
        compileJava(javaFiles, javaClassesDir, projectName, compileClassPath, runtimeClassPath);
        BuildJarTask buildJarTask = new BuildJarTask(getProject(), getModule(), getLogger());
        buildJarTask.assembleJar(sourceFolders, jarFileDir);
        getLogger().debug("> Task :" + projectName + ":" + "jar");
        copyResources(jarFileDir, transformsDir.getAbsolutePath());
        if (!transformedJarFileDir.renameTo(classesJarFileDir)) {
          getLogger().warning("Failed to rename " + transformedJarFileDir.getName() + " file");
        }
      } else {
        getLogger().debug("> Task :" + projectName + ":" + "jar SKIPPED");
      }
      builtProjects.add(projectName);
      subCompileClassPath.add(new File(transformsDir, "classes.jar"));
      subRuntimeClassPath.add(new File(transformsDir, "classes.jar"));
      getModule().addLibrary(new File(transformsDir, "classes.jar"));
    } else if (pluginType.equals("[com.android.library]")) {
      if (builtProjects.contains(projectName)) {
        // getLogger().debug("Already built project: " + projectName);
        subCompileClassPath.add(new File(transformsDir, "classes.jar"));
        subRuntimeClassPath.add(new File(transformsDir, "classes.jar"));
        getModule().addLibrary(new File(transformsDir, "classes.jar"));
        return;
      }
      javaFiles.addAll(getFiles(javaDir, ".java"));

      if (manifestFileDir.exists()) {
        if (mBuildType == BuildType.RELEASE || mBuildType == BuildType.AAB) {
          getLogger().debug("> Task :" + projectName + ":" + "generateReleaseBuildConfig");
          GenerateReleaseBuildConfigTask generateReleaseBuildConfigTask =
              new GenerateReleaseBuildConfigTask(getProject(), getModule(), getLogger());
          generateReleaseBuildConfigTask.GenerateBuildConfig(
              getModule().getNameSpace(gradleFile), buildGenDir);

        } else if (mBuildType == BuildType.DEBUG) {
          getLogger().debug("> Task :" + projectName + ":" + "generateDebugBuildConfig");
          GenerateDebugBuildConfigTask generateDebugBuildConfigTask =
              new GenerateDebugBuildConfigTask(getProject(), getModule(), getLogger());
          generateDebugBuildConfigTask.GenerateBuildConfig(
              getModule().getNameSpace(gradleFile), buildGenDir);
        }
        ManifestMergeTask manifestMergeTask =
            new ManifestMergeTask(getProject(), getModule(), getLogger());
        manifestMergeTask.merge(root, gradleFile);

        if (resDir.exists()) {
          if (javaClassesDir.exists()) {
            FileUtils.deleteDirectory(javaClassesDir);
          }
          if (aarDir.exists()) {
            FileUtils.deleteDirectory(aarDir);
          }
          List<File> librariesToCompile = getLibraries(projectName, binResDir);
          compileRes(resDir, binResDir, projectName);
          compileLibraries(librariesToCompile, projectName, binResDir);
          linkRes(binResDir, projectName, manifestBinFileDir, assetsDir);
        }

        javaFiles.addAll(getFiles(buildGenDir, ".java"));
        javaFiles.addAll(getFiles(viewBindingDir, ".java"));
        compileClassPath.add(javaClassesDir);
        runtimeClassPath.add(javaClassesDir);
        compileJava(javaFiles, javaClassesDir, projectName, compileClassPath, runtimeClassPath);
        getLogger().debug("> Task :" + projectName + ":" + "aar");
        assembleAar(javaClassesDir, aarDir, buildDir, projectName);
        Decompress.unzip(aarFileDir.getAbsolutePath(), transformsDir.getAbsolutePath());
      } else {
        throw new CompilationFailedException("Manifest file does not exist.");
      }
      builtProjects.add(projectName);
      subCompileClassPath.add(new File(transformsDir, "classes.jar"));
      subRuntimeClassPath.add(new File(transformsDir, "classes.jar"));
      getModule().addLibrary(new File(transformsDir, "classes.jar"));
    } else if (pluginType.equals("[com.android.library, kotlin]")
        || pluginType.equals("[kotlin, com.android.library]")
        || pluginType.equals("[com.android.library, kotlin-android]")
        || pluginType.equals("[kotlin-android, com.android.library]")) {
      if (builtProjects.contains(projectName)) {
        // getLogger().debug("Already built project: " + projectName);
        subCompileClassPath.add(new File(transformsDir, "classes.jar"));
        subRuntimeClassPath.add(new File(transformsDir, "classes.jar"));
        getModule().addLibrary(new File(transformsDir, "classes.jar"));
        return;
      }
      kotlinFiles.addAll(getFiles(kotlinDir, ".kt"));
      kotlinFiles.addAll(getFiles(javaDir, ".kt"));
      javaFiles.addAll(getFiles(javaDir, ".java"));
      List<File> sourceFolders = new ArrayList<>();
      sourceFolders.add(new File(projectDir, projectName + "/build/classes/java/main"));
      sourceFolders.add(new File(projectDir, projectName + "/build/classes/kotlin/main"));

      if (manifestFileDir.exists()) {
        if (mBuildType == BuildType.RELEASE || mBuildType == BuildType.AAB) {
          getLogger().debug("> Task :" + projectName + ":" + "generateReleaseBuildConfig");
          GenerateReleaseBuildConfigTask generateReleaseBuildConfigTask =
              new GenerateReleaseBuildConfigTask(getProject(), getModule(), getLogger());
          generateReleaseBuildConfigTask.GenerateBuildConfig(
              getModule().getNameSpace(gradleFile), buildGenDir);

        } else if (mBuildType == BuildType.DEBUG) {
          getLogger().debug("> Task :" + projectName + ":" + "generateDebugBuildConfig");
          GenerateDebugBuildConfigTask generateDebugBuildConfigTask =
              new GenerateDebugBuildConfigTask(getProject(), getModule(), getLogger());
          generateDebugBuildConfigTask.GenerateBuildConfig(
              getModule().getNameSpace(gradleFile), buildGenDir);
        }
        ManifestMergeTask manifestMergeTask =
            new ManifestMergeTask(getProject(), getModule(), getLogger());
        manifestMergeTask.merge(root, gradleFile);
        if (resDir.exists()) {
          if (javaClassesDir.exists()) {
            FileUtils.deleteDirectory(javaClassesDir);
          }
          if (aarDir.exists()) {
            FileUtils.deleteDirectory(aarDir);
          }
          List<File> librariesToCompile = getLibraries(projectName, binResDir);
          compileRes(resDir, binResDir, projectName);
          compileLibraries(librariesToCompile, projectName, binResDir);
          linkRes(binResDir, projectName, manifestBinFileDir, assetsDir);
        }

        javaFiles.addAll(getFiles(buildGenDir, ".java"));
        javaFiles.addAll(getFiles(viewBindingDir, ".java"));
        compileKotlin(
            kotlinFiles, kotlinClassesDir, projectName, compileClassPath, runtimeClassPath);
        compileClassPath.add(javaClassesDir);
        runtimeClassPath.add(javaClassesDir);
        compileClassPath.add(kotlinClassesDir);
        runtimeClassPath.add(kotlinClassesDir);
        compileJava(javaFiles, javaClassesDir, projectName, compileClassPath, runtimeClassPath);
        getLogger().debug("> Task :" + projectName + ":" + "aar");
        assembleAar(sourceFolders, aarDir, buildDir, projectName);
        Decompress.unzip(aarFileDir.getAbsolutePath(), transformsDir.getAbsolutePath());
      } else {
        throw new CompilationFailedException("Manifest file does not exist.");
      }
      builtProjects.add(projectName);
      subCompileClassPath.add(new File(transformsDir, "classes.jar"));
      subRuntimeClassPath.add(new File(transformsDir, "classes.jar"));
      getModule().addLibrary(new File(transformsDir, "classes.jar"));
    }
  }

  private List<File> getCompileClassPath(File libraries) {
    List<File> compileClassPath = new ArrayList<>();
    compileClassPath.addAll(getJarFiles(new File(libraries, "api_files/libs")));
    compileClassPath.addAll(getJarFiles(new File(libraries, "api_libs")));
    compileClassPath.addAll(getJarFiles(new File(libraries, "implementation_files/libs")));
    compileClassPath.addAll(getJarFiles(new File(libraries, "implementation_libs")));
    compileClassPath.addAll(getJarFiles(new File(libraries, "compileOnly_files/libs")));
    compileClassPath.addAll(getJarFiles(new File(libraries, "compileOnly_libs")));
    compileClassPath.addAll(getJarFiles(new File(libraries, "compileOnlyApi_files/libs")));
    compileClassPath.addAll(getJarFiles(new File(libraries, "compileOnlyApi_libs")));

    return compileClassPath;
  }

  private List<File> addToClassPath(File libraries) {
    List<File> classPath = new ArrayList<>();
    classPath.addAll(getJarFiles(new File(libraries, "api_files/libs")));
    classPath.addAll(getJarFiles(new File(libraries, "api_libs")));
    for (File jar : classPath) {
      getModule().addLibrary(jar);
    }
    return classPath;
  }

  private List<File> getRuntimeClassPath(File libraries) {
    List<File> runtimeClassPath = new ArrayList<>();
    runtimeClassPath.addAll(getJarFiles(new File(libraries, "runtimeOnly_files/libs")));
    runtimeClassPath.addAll(getJarFiles(new File(libraries, "runtimeOnly_libs")));
    runtimeClassPath.addAll(getJarFiles(new File(libraries, "api_files/libs")));
    runtimeClassPath.addAll(getJarFiles(new File(libraries, "api_libs")));
    runtimeClassPath.addAll(getJarFiles(new File(libraries, "implementation_files/libs")));
    runtimeClassPath.addAll(getJarFiles(new File(libraries, "implementation_libs")));
    runtimeClassPath.addAll(getJarFiles(new File(libraries, "runtimeOnlyApi_files/libs")));
    runtimeClassPath.addAll(getJarFiles(new File(libraries, "runtimeOnlyApi_libs")));

    return runtimeClassPath;
  }

  private List<File> getLibraries(String root, File bin_res) throws IOException {
    if (!bin_res.exists()) {
      if (!bin_res.mkdirs()) {
        throw new IOException("Failed to create resource directory");
      }
    }

    List<File> libraries = new ArrayList<>();

    for (File library :
        getLibraries(
            new File(getModule().getProjectDir(), root + "/build/libraries/api_files/libs"))) {
      File parent = library.getParentFile();
      if (parent != null) {
        if (!new File(parent, "res").exists()) {
          // we don't need to check it if it has no resource directory
          continue;
        }
        File check = new File(bin_res, parent.getName() + ".zip");
        if (!check.exists()) {
          libraries.add(library);
        }
      }
    }

    for (File library :
        getLibraries(new File(getModule().getProjectDir(), root + "/build/libraries/api_libs"))) {
      File parent = library.getParentFile();
      if (parent != null) {
        if (!new File(parent, "res").exists()) {
          // we don't need to check it if it has no resource directory
          continue;
        }
        File check = new File(bin_res, parent.getName() + ".zip");
        if (!check.exists()) {
          libraries.add(library);
        }
      }
    }
    for (File library :
        getLibraries(
            new File(
                getModule().getProjectDir(),
                root + "/build/libraries/implementation_files/libs"))) {
      File parent = library.getParentFile();
      if (parent != null) {
        if (!new File(parent, "res").exists()) {
          // we don't need to check it if it has no resource directory
          continue;
        }
        File check = new File(bin_res, parent.getName() + ".zip");
        if (!check.exists()) {
          libraries.add(library);
        }
      }
    }

    for (File library :
        getLibraries(
            new File(getModule().getProjectDir(), root + "/build/libraries/implementation_libs"))) {
      File parent = library.getParentFile();
      if (parent != null) {
        if (!new File(parent, "res").exists()) {
          // we don't need to check it if it has no resource directory
          continue;
        }
        File check = new File(bin_res, parent.getName() + ".zip");
        if (!check.exists()) {
          libraries.add(library);
        }
      }
    }

    for (File library :
        getLibraries(
            new File(
                getModule().getProjectDir(), root + "/build/libraries/compileOnly_files/libs"))) {
      File parent = library.getParentFile();
      if (parent != null) {
        if (!new File(parent, "res").exists()) {
          // we don't need to check it if it has no resource directory
          continue;
        }
        File check = new File(bin_res, parent.getName() + ".zip");
        if (!check.exists()) {
          libraries.add(library);
        }
      }
    }
    for (File library :
        getLibraries(
            new File(
                getModule().getProjectDir(),
                root + "/build/libraries/compileOnlyApi_files/libs"))) {
      File parent = library.getParentFile();
      if (parent != null) {
        if (!new File(parent, "res").exists()) {
          // we don't need to check it if it has no resource directory
          continue;
        }
        File check = new File(bin_res, parent.getName() + ".zip");
        if (!check.exists()) {
          libraries.add(library);
        }
      }
    }

    for (File library :
        getLibraries(
            new File(getModule().getProjectDir(), root + "/build/libraries/compileOnly_libs"))) {
      File parent = library.getParentFile();
      if (parent != null) {
        if (!new File(parent, "res").exists()) {
          // we don't need to check it if it has no resource directory
          continue;
        }
        File check = new File(bin_res, parent.getName() + ".zip");
        if (!check.exists()) {
          libraries.add(library);
        }
      }
    }

    for (File library :
        getLibraries(
            new File(getModule().getProjectDir(), root + "/build/libraries/compileOnlyApi_libs"))) {
      File parent = library.getParentFile();
      if (parent != null) {
        if (!new File(parent, "res").exists()) {
          // we don't need to check it if it has no resource directory
          continue;
        }
        File check = new File(bin_res, parent.getName() + ".zip");
        if (!check.exists()) {
          libraries.add(library);
        }
      }
    }

    for (File library :
        getLibraries(
            new File(
                getModule().getProjectDir(), root + "/build/libraries/runtimeOnly_files/libs"))) {
      File parent = library.getParentFile();
      if (parent != null) {
        if (!new File(parent, "res").exists()) {
          // we don't need to check it if it has no resource directory
          continue;
        }
        File check = new File(bin_res, parent.getName() + ".zip");
        if (!check.exists()) {
          libraries.add(library);
        }
      }
    }

    for (File library :
        getLibraries(
            new File(
                getModule().getProjectDir(),
                root + "/build/libraries/runtimeOnlyApi_files/libs"))) {
      File parent = library.getParentFile();
      if (parent != null) {
        if (!new File(parent, "res").exists()) {
          // we don't need to check it if it has no resource directory
          continue;
        }
        File check = new File(bin_res, parent.getName() + ".zip");
        if (!check.exists()) {
          libraries.add(library);
        }
      }
    }

    for (File library :
        getLibraries(
            new File(getModule().getProjectDir(), root + "/build/libraries/runtimeOnly_libs"))) {
      File parent = library.getParentFile();
      if (parent != null) {
        if (!new File(parent, "res").exists()) {
          // we don't need to check it if it has no resource directory
          continue;
        }
        File check = new File(bin_res, parent.getName() + ".zip");
        if (!check.exists()) {
          libraries.add(library);
        }
      }
    }

    for (File library :
        getLibraries(
            new File(getModule().getProjectDir(), root + "/build/libraries/runtimeOnlyApi_libs"))) {
      File parent = library.getParentFile();
      if (parent != null) {
        if (!new File(parent, "res").exists()) {
          // we don't need to check it if it has no resource directory
          continue;
        }
        File check = new File(bin_res, parent.getName() + ".zip");
        if (!check.exists()) {
          libraries.add(library);
        }
      }
    }

    return libraries;
  }

  private List<File> getLibraries(File dir) {
    List<File> libraries = new ArrayList<>();
    File[] libs = dir.listFiles(File::isDirectory);
    if (libs != null) {
      for (File directory : libs) {
        File check = new File(directory, "classes.jar");
        if (check.exists()) {
          libraries.add(check);
        }
      }
    }
    return libraries;
  }

  private void compileLibraries(List<File> libraries, String root, File bin_res)
      throws IOException, CompilationFailedException {
    Log.d(TAG, "Compiling libraries.");
    if (!bin_res.exists()) {
      if (!bin_res.mkdirs()) {
        throw new IOException("Failed to create resource output directory");
      }
    }

    for (File file : libraries) {
      File parent = file.getParentFile();
      if (parent == null) {
        throw new IOException("Library folder doesn't exist");
      }
      File[] files = parent.listFiles();
      if (files == null) {
        continue;
      }

      for (File inside : files) {
        if (inside.isDirectory() && inside.getName().equals("res")) {
          List<String> args = new ArrayList<>();
          args.add("--dir");
          args.add(inside.getAbsolutePath());
          args.add("-o");
          args.add(createNewFile(bin_res, parent.getName() + ".zip").getAbsolutePath());

          int compile = Aapt2Jni.compile(args);
          List<DiagnosticWrapper> logs = Aapt2Jni.getLogs();
          LogUtils.log(logs, getLogger());

          if (compile != 0) {
            throw new CompilationFailedException(
                "Compilation failed, check logs for more details.");
          }
        }
      }
    }
  }

  private void assembleAar(List<File> inputFolders, File aar, File build, String name)
      throws IOException, CompilationFailedException {
    if (!aar.exists()) {
      if (!aar.mkdirs()) {
        throw new IOException("Failed to create resource aar directory");
      }
    }
    AssembleJar assembleJar = new AssembleJar(false);
    assembleJar.setOutputFile(new File(aar.getAbsolutePath(), "classes.jar"));
    assembleJar.createJarArchive(inputFolders);
    File library = new File(getModule().getProjectDir(), name + "/build/outputs/aar/");

    if (!library.exists()) {
      if (!library.mkdirs()) {
        throw new IOException("Failed to create resource libs directory");
      }
    }
    File res = new File(getModule().getProjectDir(), name + "/src/main/res");
    copyResources(
        new File(getModule().getProjectDir(), name + "/src/main/AndroidManifest.xml"),
        aar.getAbsolutePath());
    if (res.exists()) {
      copyResources(res, aar.getAbsolutePath());
    }
    File assets = new File(getModule().getProjectDir(), name + "/src/main/assets");
    File jniLibs = new File(getModule().getProjectDir(), name + "/src/main/jniLibs");

    if (assets.exists()) {
      copyResources(assets, aar.getAbsolutePath());
    }
    if (jniLibs.exists()) {
      copyResources(jniLibs, aar.getAbsolutePath());
      File jni = new File(aar.getAbsolutePath(), "jniLibs");
      jni.renameTo(new File(aar.getAbsolutePath(), "jni"));
    }
    zipFolder(
        Paths.get(aar.getAbsolutePath()), Paths.get(library.getAbsolutePath(), name + ".aar"));
    if (aar.exists()) {
      FileUtils.deleteDirectory(aar);
    }
  }

  private void assembleAar(File input, File aar, File build, String name)
      throws IOException, CompilationFailedException {
    if (!aar.exists()) {
      if (!aar.mkdirs()) {
        throw new IOException("Failed to create resource aar directory");
      }
    }
    AssembleJar assembleJar = new AssembleJar(false);
    assembleJar.setOutputFile(new File(aar.getAbsolutePath(), "classes.jar"));
    assembleJar.createJarArchive(input);
    File library = new File(getModule().getProjectDir(), name + "/build/outputs/aar/");

    if (!library.exists()) {
      if (!library.mkdirs()) {
        throw new IOException("Failed to create resource libs directory");
      }
    }
    File res = new File(getModule().getProjectDir(), name + "/src/main/res");
    copyResources(
        new File(getModule().getProjectDir(), name + "/src/main/AndroidManifest.xml"),
        aar.getAbsolutePath());
    if (res.exists()) {
      copyResources(res, aar.getAbsolutePath());
    }
    File assets = new File(getModule().getProjectDir(), name + "/src/main/assets");
    File jniLibs = new File(getModule().getProjectDir(), name + "/src/main/jniLibs");

    if (assets.exists()) {
      copyResources(assets, aar.getAbsolutePath());
    }
    if (jniLibs.exists()) {
      copyResources(jniLibs, aar.getAbsolutePath());
      File jni = new File(aar.getAbsolutePath(), "jniLibs");
      jni.renameTo(new File(aar.getAbsolutePath(), "jni"));
    }
    zipFolder(
        Paths.get(aar.getAbsolutePath()), Paths.get(library.getAbsolutePath(), name + ".aar"));
    if (aar.exists()) {
      FileUtils.deleteDirectory(aar);
    }
  }

  public void compileKotlin(
      Set<File> kotlinFiles,
      File out,
      String name,
      List<File> compileClassPath,
      List<File> runtimeClassPath)
      throws IOException, CompilationFailedException {
    if (!out.exists()) {
      if (!out.mkdirs()) {
        throw new IOException("Failed to create resource output directory");
      }
    }

    List<File> mFilesToCompile = new ArrayList<>();

    mClassCache = getModule().getCache(CACHE_KEY, new Cache<>());
    for (Cache.Key<String> key : new HashSet<>(mClassCache.getKeys())) {
      if (!mFilesToCompile.contains(key.file.toFile())) {
        File file = mClassCache.get(key.file, "class").iterator().next();
        deleteAllFiles(file, ".class");
        mClassCache.remove(key.file, "class", "dex");
      }
    }

    mFilesToCompile.addAll(kotlinFiles);
    if (mFilesToCompile.isEmpty()) {
      getLogger().debug("> Task :" + name + ":" + "compileKotlin SKIPPED");
      return;
    } else {
      getLogger().debug("> Task :" + name + ":" + "compileKotlin");
    }

    List<File> classpath = new ArrayList<>();
    classpath.add(getModule().getBootstrapJarFile());
    classpath.add(getModule().getLambdaStubsJarFile());
    classpath.addAll(compileClassPath);
    classpath.addAll(runtimeClassPath);
    List<String> arguments = new ArrayList<>();
    Collections.addAll(
        arguments,
        "-cp",
        classpath.stream()
            .map(File::getAbsolutePath)
            .collect(Collectors.joining(File.pathSeparator)));
    arguments.add("-Xskip-metadata-version-check");
    File javaDir = new File(getModule().getProjectDir(), name + "/src/main/java");
    File kotlinDir = new File(getModule().getProjectDir(), name + "/src/main/kotlin");
    File buildGenDir = new File(getModule().getProjectDir(), name + "/build/gen");
    File viewBindingDir = new File(getModule().getProjectDir(), name + "/build/view_binding");

    List<File> javaSourceRoots = new ArrayList<>();
    if (javaDir.exists()) {
      javaSourceRoots.addAll(getFiles(javaDir, ".java"));
    }
    if (buildGenDir.exists()) {
      javaSourceRoots.addAll(getFiles(buildGenDir, ".java"));
    }
    if (viewBindingDir.exists()) {
      javaSourceRoots.addAll(getFiles(viewBindingDir, ".java"));
    }

    try {
      K2JVMCompiler compiler = new K2JVMCompiler();
      K2JVMCompilerArguments args = new K2JVMCompilerArguments();
      compiler.parseArguments(arguments.toArray(new String[0]), args);

      args.setUseJavac(false);
      args.setCompileJava(false);
      args.setIncludeRuntime(false);
      args.setNoJdk(true);
      args.setModuleName(name);
      args.setNoReflect(true);
      args.setNoStdlib(true);
      args.setSuppressWarnings(true);
      args.setJavaSourceRoots(
          javaSourceRoots.stream().map(File::getAbsolutePath).toArray(String[]::new));
      // args.setKotlinHome(mKotlinHome.getAbsolutePath());
      args.setDestination(out.getAbsolutePath());

      List<File> plugins = getPlugins();
      getLogger().debug("Loading kotlin compiler plugins: " + plugins);

      args.setPluginClasspaths(plugins.stream().map(File::getAbsolutePath).toArray(String[]::new));
      args.setPluginOptions(getPluginOptions());

      File cacheDir =
          new File(getModule().getProjectDir(), name + "/build/kotlin/compileKotlin/cacheable");

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

      IncrementalJvmCompilerRunnerKt.makeIncrementally(
          cacheDir,
          Arrays.asList(fileList.toArray(new File[0])),
          args,
          mCollector,
          new ICReporterBase() {
            @Override
            public void report(@NonNull Function0<String> function0) {
              // getLogger().info()
              function0.invoke();
            }

            @Override
            public void reportVerbose(@NonNull Function0<String> function0) {
              // getLogger().verbose()
              function0.invoke();
            }

            @Override
            public void reportCompileIteration(
                boolean incremental,
                @NonNull Collection<? extends File> sources,
                @NonNull ExitCode exitCode) {}
          });
    } catch (Exception e) {
      throw new CompilationFailedException(Throwables.getStackTraceAsString(e));
    }

    if (mCollector.hasErrors()) {
      throw new CompilationFailedException("Compilation failed, see logs for more details");
    } else {
      getLogger().debug("> Task :" + name + ":" + "classes");
    }
  }

  private boolean mHasErrors = false;

  public void compileJava(
      Set<File> javaFiles,
      File out,
      String name,
      List<File> compileClassPath,
      List<File> runtimeClassPath)
      throws IOException, CompilationFailedException {

    if (!out.exists()) {
      if (!out.mkdirs()) {
        throw new IOException("Failed to create resource output directory");
      }
    }

    List<File> mFilesToCompile = new ArrayList<>();

    mClassCache = getModule().getCache(CACHE_KEY, new Cache<>());
    for (Cache.Key<String> key : new HashSet<>(mClassCache.getKeys())) {
      if (!mFilesToCompile.contains(key.file.toFile())) {
        File file = mClassCache.get(key.file, "class").iterator().next();
        deleteAllFiles(file, ".class");
        mClassCache.remove(key.file, "class", "dex");
      }
    }

    runtimeClassPath.add(getModule().getBootstrapJarFile());
    runtimeClassPath.add(getModule().getLambdaStubsJarFile());

    mFilesToCompile.addAll(javaFiles);
    List<JavaFileObject> javaFileObjects = new ArrayList<>();

    if (mFilesToCompile.isEmpty()) {
      getLogger().debug("> Task :" + name + ":" + "compileJava SKIPPED");
      return;
    } else {
      getLogger().debug("> Task :" + name + ":" + "compileJava");
    }

    List<String> options = new ArrayList<>();
    options.add("-proc:none");
    options.add("-source");
    options.add("1.8");
    options.add("-target");
    options.add("1.8");
    options.add("-Xlint:cast");
    options.add("-Xlint:deprecation");
    options.add("-Xlint:empty");
    options.add("-Xlint" + ":fallthrough");
    options.add("-Xlint:finally");
    options.add("-Xlint:path");
    options.add("-Xlint:unchecked");
    options.add("-Xlint" + ":varargs");
    options.add("-Xlint:static");
    for (File file : mFilesToCompile) {
      javaFileObjects.add(
          new SimpleJavaFileObject(file.toURI(), JavaFileObject.Kind.SOURCE) {
            @Override
            public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
              return FileUtils.readFileToString(file, StandardCharsets.UTF_8);
            }
          });
    }

    DiagnosticListener<JavaFileObject> diagnosticCollector =
        diagnostic -> {
          switch (diagnostic.getKind()) {
            case ERROR:
              mHasErrors = true;
              getLogger().error(new DiagnosticWrapper(diagnostic));
              break;
            case WARNING:
              getLogger().warning(new DiagnosticWrapper(diagnostic));
          }
        };

    JavacTool tool = JavacTool.create();
    JavacFileManager standardJavaFileManager =
        tool.getStandardFileManager(
            diagnosticCollector, Locale.getDefault(), Charset.defaultCharset());
    standardJavaFileManager.setSymbolFileEnabled(false);
    standardJavaFileManager.setLocation(
        StandardLocation.CLASS_OUTPUT, Collections.singletonList(out));
    standardJavaFileManager.setLocation(StandardLocation.PLATFORM_CLASS_PATH, runtimeClassPath);
    standardJavaFileManager.setLocation(StandardLocation.CLASS_PATH, compileClassPath);
    standardJavaFileManager.setLocation(StandardLocation.SOURCE_PATH, mFilesToCompile);

    JavacTask task =
        tool.getTask(
            null, standardJavaFileManager, diagnosticCollector, options, null, javaFileObjects);
    task.parse();
    task.analyze();
    task.generate();

    if (mHasErrors) {
      throw new CompilationFailedException("Compilation failed, check logs for more details");
    } else {
      getLogger().debug("> Task :" + name + ":" + "classes");
    }
  }

  private void linkRes(File in, String name, File manifest, File assets)
      throws CompilationFailedException, IOException {
    getLogger().debug("> Task :" + name + ":" + "mergeResources");

    List<String> args = new ArrayList<>();
    args.add("-I");
    args.add(getModule().getBootstrapJarFile().getAbsolutePath());
    args.add("--allow-reserved-package-id");
    args.add("--no-version-vectors");
    args.add("--no-version-transitions");
    args.add("--auto-add-overlay");
    args.add("--min-sdk-version");
    args.add(String.valueOf(getModule().getMinSdk()));
    args.add("--target-sdk-version");
    args.add(String.valueOf(getModule().getTargetSdk()));
    args.add("--proguard");
    File buildAar =
        new File(getModule().getProjectDir().getAbsolutePath(), name + "/build/bin/aar");
    args.add(createNewFile((buildAar), "proguard.txt").getAbsolutePath());

    File[] libraryResources = getOutputPath(name).listFiles();
    if (libraryResources != null) {
      for (File resource : libraryResources) {
        if (resource.isDirectory()) {
          continue;
        }
        if (!resource.getName().endsWith(".zip")) {
          Log.w(TAG, "Unrecognized file " + resource.getName());
          continue;
        }

        if (resource.length() == 0) {
          Log.w(TAG, "Empty zip file " + resource.getName());
        }

        args.add("-R");
        args.add(resource.getAbsolutePath());
      }
    }

    args.add("--java");
    File gen = new File(getModule().getProjectDir(), name + "/build/gen");
    if (!gen.exists()) {
      if (!gen.mkdirs()) {
        throw new CompilationFailedException("Failed to create gen folder");
      }
    }
    args.add(gen.getAbsolutePath());

    args.add("--manifest");
    if (!manifest.exists()) {
      throw new IOException("Unable to get project manifest file");
    }
    args.add(manifest.getAbsolutePath());

    args.add("-o");

    File buildBin = new File(getModule().getProjectDir().getAbsolutePath(), name + "/build/bin");

    File out = new File(buildBin, "generated.aar.res");
    args.add(out.getAbsolutePath());

    args.add("--output-text-symbols");
    File file = new File(buildAar, "R.txt");
    Files.deleteIfExists(file.toPath());
    if (!file.createNewFile()) {
      throw new IOException("Unable to create R.txt file");
    }
    args.add(file.getAbsolutePath());

    for (File library :
        getLibraries(
            new File(getModule().getProjectDir(), name + "/build/libraries/api_files/libs"))) {
      File parent = library.getParentFile();
      if (parent == null) {
        continue;
      }

      File assetsDir = new File(parent, "assets");
      if (assetsDir.exists()) {
        args.add("-A");
        args.add(assetsDir.getAbsolutePath());
      }
    }

    for (File library :
        getLibraries(new File(getModule().getProjectDir(), name + "/build/libraries/api_libs"))) {
      File parent = library.getParentFile();
      if (parent == null) {
        continue;
      }

      File assetsDir = new File(parent, "assets");
      if (assetsDir.exists()) {
        args.add("-A");
        args.add(assetsDir.getAbsolutePath());
      }
    }

    for (File library :
        getLibraries(
            new File(
                getModule().getProjectDir(),
                name + "/build/libraries/implementation_files/libs"))) {
      File parent = library.getParentFile();
      if (parent == null) {
        continue;
      }

      File assetsDir = new File(parent, "assets");
      if (assetsDir.exists()) {
        args.add("-A");
        args.add(assetsDir.getAbsolutePath());
      }
    }

    for (File library :
        getLibraries(
            new File(getModule().getProjectDir(), name + "/build/libraries/implementation_libs"))) {
      File parent = library.getParentFile();
      if (parent == null) {
        continue;
      }

      File assetsDir = new File(parent, "assets");
      if (assetsDir.exists()) {
        args.add("-A");
        args.add(assetsDir.getAbsolutePath());
      }
    }

    for (File library :
        getLibraries(
            new File(
                getModule().getProjectDir(), name + "/build/libraries/compileOnly_files/libs"))) {
      File parent = library.getParentFile();
      if (parent == null) {
        continue;
      }

      File assetsDir = new File(parent, "assets");
      if (assetsDir.exists()) {
        args.add("-A");
        args.add(assetsDir.getAbsolutePath());
      }
    }

    for (File library :
        getLibraries(
            new File(
                getModule().getProjectDir(),
                name + "/build/libraries/compileOnlyApi_files/libs"))) {
      File parent = library.getParentFile();
      if (parent == null) {
        continue;
      }

      File assetsDir = new File(parent, "assets");
      if (assetsDir.exists()) {
        args.add("-A");
        args.add(assetsDir.getAbsolutePath());
      }
    }

    for (File library :
        getLibraries(
            new File(getModule().getProjectDir(), name + "/build/libraries/compileOnly_libs"))) {
      File parent = library.getParentFile();
      if (parent == null) {
        continue;
      }

      File assetsDir = new File(parent, "assets");
      if (assetsDir.exists()) {
        args.add("-A");
        args.add(assetsDir.getAbsolutePath());
      }
    }

    for (File library :
        getLibraries(
            new File(getModule().getProjectDir(), name + "/build/libraries/compileOnlyApi_libs"))) {
      File parent = library.getParentFile();
      if (parent == null) {
        continue;
      }

      File assetsDir = new File(parent, "assets");
      if (assetsDir.exists()) {
        args.add("-A");
        args.add(assetsDir.getAbsolutePath());
      }
    }

    for (File library :
        getLibraries(
            new File(
                getModule().getProjectDir(), name + "/build/libraries/runtimeOnly_files/libs"))) {
      File parent = library.getParentFile();
      if (parent == null) {
        continue;
      }

      File assetsDir = new File(parent, "assets");
      if (assetsDir.exists()) {
        args.add("-A");
        args.add(assetsDir.getAbsolutePath());
      }
    }

    for (File library :
        getLibraries(
            new File(
                getModule().getProjectDir(),
                name + "/build/libraries/runtimeOnlyApi_files/libs"))) {
      File parent = library.getParentFile();
      if (parent == null) {
        continue;
      }

      File assetsDir = new File(parent, "assets");
      if (assetsDir.exists()) {
        args.add("-A");
        args.add(assetsDir.getAbsolutePath());
      }
    }

    for (File library :
        getLibraries(
            new File(getModule().getProjectDir(), name + "/build/libraries/runtimeOnly_libs"))) {
      File parent = library.getParentFile();
      if (parent == null) {
        continue;
      }

      File assetsDir = new File(parent, "assets");
      if (assetsDir.exists()) {
        args.add("-A");
        args.add(assetsDir.getAbsolutePath());
      }
    }

    for (File library :
        getLibraries(
            new File(getModule().getProjectDir(), name + "/build/libraries/runtimeOnlyApi_libs"))) {
      File parent = library.getParentFile();
      if (parent == null) {
        continue;
      }

      File assetsDir = new File(parent, "assets");
      if (assetsDir.exists()) {
        args.add("-A");
        args.add(assetsDir.getAbsolutePath());
      }
    }

    if (assets.exists()) {
      args.add("-A");
      args.add(assets.getAbsolutePath());
    }

    int compile = Aapt2Jni.link(args);
    List<DiagnosticWrapper> logs = Aapt2Jni.getLogs();
    LogUtils.log(logs, getLogger());

    if (compile != 0) {
      throw new CompilationFailedException("Compilation failed, check logs for more details.");
    }
  }

  private void compileRes(File res, File out, String name)
      throws IOException, CompilationFailedException {
    if (!out.exists()) {
      if (!out.mkdirs()) {
        throw new IOException("Failed to create resource output directory");
      }
    }

    List<String> args = new ArrayList<>();
    args.add("--dir");
    args.add(res.getAbsolutePath());
    args.add("-o");
    args.add(createNewFile(out, name + "_res.zip").getAbsolutePath());

    int compile = Aapt2Jni.compile(args);
    List<DiagnosticWrapper> logs = Aapt2Jni.getLogs();
    LogUtils.log(logs, getLogger());

    if (compile != 0) {
      throw new CompilationFailedException("Compilation failed, check logs for more details.");
    }
  }

  private File createNewFile(File parent, String name) throws IOException {
    File createdFile = new File(parent, name);
    if (!parent.exists()) {
      if (!parent.mkdirs()) {
        throw new IOException("Unable to create directories");
      }
    }
    if (!createdFile.exists() && !createdFile.createNewFile()) {
      throw new IOException("Unable to create file " + name);
    }
    return createdFile;
  }

  public static Set<File> getFiles(File dir, String ext) {
    Set<File> Files = new HashSet<>();

    File[] files = dir.listFiles();
    if (files == null) {
      return Collections.emptySet();
    }

    for (File file : files) {
      if (file.isDirectory()) {
        Files.addAll(getFiles(file, ext));
      } else {
        if (file.getName().endsWith(ext)) {
          Files.add(file);
        }
      }
    }

    return Files;
  }

  private void zipFolder(final Path sourceFolderPath, Path zipPath) throws IOException {
    final ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipPath.toFile()));
    Files.walkFileTree(
        sourceFolderPath,
        new SimpleFileVisitor<Path>() {
          public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
              throws IOException {
            zos.putNextEntry(new ZipEntry(sourceFolderPath.relativize(file).toString()));
            Files.copy(file, zos);
            zos.closeEntry();
            return FileVisitResult.CONTINUE;
          }
        });
    zos.close();
  }

  private void copyResources(File file, String path) throws IOException {
    if (file.isDirectory()) {
      File[] files = file.listFiles();
      if (files != null) {
        for (File child : files) {
          copyResources(child, path + "/" + file.getName());
        }
      }
    } else {
      File directory = new File(path);
      if (!directory.exists() && !directory.mkdirs()) {
        throw new IOException("Failed to create directory " + directory);
      }

      FileUtils.copyFileToDirectory(file, directory);
    }
  }

  private void deleteAllFiles(File classFile, String ext) throws IOException {
    File parent = classFile.getParentFile();
    String name = classFile.getName().replace(ext, "");
    if (parent != null) {
      File[] children =
          parent.listFiles((c) -> c.getName().endsWith(ext) && c.getName().contains("$"));
      if (children != null) {
        for (File child : children) {
          if (child.getName().startsWith(name)) {
            FileUtils.delete(child);
          }
        }
      }
    }
    if (classFile.exists()) {
      FileUtils.delete(classFile);
    }
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
      getLogger().warning(message);
      return false;
    } catch (IOException e) {
      // If there is some other error reading the JarFile, return false
      getLogger().warning(message);
      return false;
    }
  }

  private File getOutputPath(String name) throws IOException {
    File file = new File(getModule().getProjectDir(), name + "/build/bin/res");
    if (!file.exists()) {
      if (!file.mkdirs()) {
        throw new IOException("Failed to get resource directory");
      }
    }
    return file;
  }

  private List<File> getPlugins() {
    File pluginDir = new File(getModule().getBuildDirectory(), "plugins");
    File[] children = pluginDir.listFiles(c -> c.getName().endsWith(".jar"));

    if (children == null) {
      return Collections.emptyList();
    }

    return Arrays.stream(children).collect(Collectors.toList());
  }

  private String[] getPluginOptions() throws IOException {
    File pluginDir = new File(getModule().getBuildDirectory(), "plugins");
    File args = new File(pluginDir, "args.txt");
    if (!args.exists()) {
      return new String[0];
    }

    String string = FileUtils.readFileToString(args, StandardCharsets.UTF_8);
    return string.split(" ");
  }

  private static class Diagnostic extends DiagnosticWrapper {
    private final CompilerMessageSeverity mSeverity;
    private final String mMessage;
    private final CompilerMessageSourceLocation mLocation;

    public Diagnostic(
        CompilerMessageSeverity severity, String message, CompilerMessageSourceLocation location) {
      mSeverity = severity;
      mMessage = message;

      if (location == null) {
        mLocation =
            new CompilerMessageSourceLocation() {
              @NonNull
              @Override
              public String getPath() {
                return "UNKNOWN";
              }

              @Override
              public int getLine() {
                return 0;
              }

              @Override
              public int getColumn() {
                return 0;
              }

              @Override
              public int getLineEnd() {
                return 0;
              }

              @Override
              public int getColumnEnd() {
                return 0;
              }

              @Override
              public String getLineContent() {
                return "";
              }
            };
      } else {
        mLocation = location;
      }
    }

    @Override
    public File getSource() {
      if (mLocation == null || TextUtils.isEmpty(mLocation.getPath())) {
        return new File("UNKNOWN");
      }
      return new File(mLocation.getPath());
    }

    @Override
    public Kind getKind() {
      switch (mSeverity) {
        case ERROR:
          return Kind.ERROR;
        case STRONG_WARNING:
          return Kind.MANDATORY_WARNING;
        case WARNING:
          return Kind.WARNING;
        case LOGGING:
          return Kind.OTHER;
        default:
        case INFO:
          return Kind.NOTE;
      }
    }

    @Override
    public long getLineNumber() {
      return mLocation.getLine();
    }

    @Override
    public long getColumnNumber() {
      return mLocation.getColumn();
    }

    @Override
    public String getMessage(Locale locale) {
      return mMessage;
    }
  }

  private class Collector implements MessageCollector {

    private final List<Diagnostic> mDiagnostics = new ArrayList<>();
    private boolean mHasErrors;

    @Override
    public void clear() {
      mDiagnostics.clear();
    }

    @Override
    public boolean hasErrors() {
      return mHasErrors;
    }

    @Override
    public void report(
        @NotNull CompilerMessageSeverity severity,
        @NotNull String message,
        CompilerMessageSourceLocation location) {
      if (message.contains("No class roots are found in the JDK path")) {
        // Android does not have JDK so its okay to ignore this error
        return;
      }
      Diagnostic diagnostic = new Diagnostic(severity, message, location);
      mDiagnostics.add(diagnostic);

      switch (severity) {
        case ERROR:
          mHasErrors = true;
          getLogger().error(diagnostic);
          break;
        case STRONG_WARNING:
        case WARNING:
          getLogger().warning(diagnostic);
          break;
        case INFO:
          getLogger().info(diagnostic);
          break;
        default:
          getLogger().debug(diagnostic);
      }
    }
  }
}
