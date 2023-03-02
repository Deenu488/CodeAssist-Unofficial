package com.tyron.builder.compiler.incremental.resource;

import android.util.Log;
import com.android.tools.aapt2.Aapt2Jni;
import com.sun.source.util.JavacTask;
import com.sun.tools.javac.api.JavacTool;
import com.sun.tools.javac.file.JavacFileManager;
import com.tyron.builder.compiler.BuildType;
import com.tyron.builder.compiler.Task;
import com.tyron.builder.exception.CompilationFailedException;
import com.tyron.builder.internal.jar.AssembleJar;
import com.tyron.builder.log.ILogger;
import com.tyron.builder.log.LogUtils;
import com.tyron.builder.model.DiagnosticWrapper;
import com.tyron.builder.project.Project;
import com.tyron.builder.project.api.AndroidModule;
import com.tyron.builder.project.cache.CacheHolder;
import com.tyron.common.util.Cache;
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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipOutputStream;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardLocation;
import org.apache.commons.io.FileUtils;

public class IncrementalAssembleLibraryTask extends Task<AndroidModule> {

  public static final CacheHolder.CacheKey<String, List<File>> CACHE_KEY =
      new CacheHolder.CacheKey<>("javaCache");
  private static final String TAG = "assembleLibraries";
  private Cache<String, List<File>> mClassCache;

  public IncrementalAssembleLibraryTask(Project project, AndroidModule module, ILogger logger) {
    super(project, module, logger);
  }

  @Override
  public String getName() {
    return TAG;
  }

  @Override
  public void prepare(BuildType type) throws IOException {}

  public void run() throws IOException, CompilationFailedException {
    List<String> projects = new ArrayList<>();
    projects.addAll(getModule().getAllProjects(getModule().getGradleFile()));
    Set<String> resolvedProjects = new HashSet<>();
    while (!projects.isEmpty()) {
      String include = projects.remove(0);
      if (resolvedProjects.contains(include)) {
        continue;
      }
      resolvedProjects.add(include);
      File gradleFile = new File(getModule().getProjectDir(), include + "/build.gradle");
      if (gradleFile.exists()) {
        List<String> includedInBuildGradle = getModule().getAllProjects(gradleFile);
        if (!includedInBuildGradle.isEmpty()) {
          projects.addAll(includedInBuildGradle);
        }
        File includeName = new File(getModule().getProjectDir(), include);
        String root = include.replaceFirst("/", "").replaceAll("/", ":");
        try {
          List<String> plugins = new ArrayList<>();
          List<String> unsupported_plugins = new ArrayList<>();

          for (String plugin : getModule().getPlugins(gradleFile)) {
            if (plugin.equals("java-library")
                || plugin.equals("com.android.library")
                || plugin.equals("kotlin")) {
              plugins.add(plugin);
            } else {
              unsupported_plugins.add(plugin);
            }
          }
          String pluginType = plugins.toString();

          if (plugins.isEmpty()) {
            getLogger().error("No plugins applied");
          } else {
            getLogger().debug("NOTE: " + "Plugins applied: " + plugins.toString());
            if (unsupported_plugins.isEmpty()) {
            } else {
              getLogger()
                  .debug(
                      "NOTE: "
                          + "Unsupported plugins : "
                          + unsupported_plugins.toString()
                          + " will not be used");
            }
          }

          if (pluginType.contains("java-library")) {
            // compileJava
          } else if (pluginType.contains("java-library") && pluginType.contains("kotlin")) {
            // compileKotiln
            // compileJava
          } else if (pluginType.contains("com.android.library")) {
            // compileJava
          } else if (pluginType.contains("com.android.library") && pluginType.contains("kotlin")) {
            // compileKotlin
            // compileJava
          } else {
            throw new CompilationFailedException(
                "Unabled to find any plugins in " + gradleFile.getAbsolutePath());
          }

          File res = new File(getModule().getProjectDir(), root + "/src/main/res");
          File bin_res = new File(getModule().getProjectDir(), root + "/build/bin/res");
          File build = new File(getModule().getProjectDir(), root + "/build");
          File manifest =
              new File(getModule().getProjectDir(), root + "/src/main/AndroidManifest.xml");
          File assets = new File(getModule().getProjectDir(), root + "/src/main/assets");
          File java = new File(getModule().getProjectDir(), root + "/src/main/java");
          File classes = new File(getModule().getProjectDir(), root + "/build/bin/java/classes");
          File gen = new File(getModule().getProjectDir(), root + "/build/gen");
          File aar = new File(getModule().getProjectDir(), root + "/build/bin/aar");
          File outputs = new File(getModule().getProjectDir(), root + "/build/outputs/aar");

          List<File> librariesToCompile = getLibraries(root, bin_res);

          if (res.exists() && manifest.exists()) {
            if (classes.exists()) {
              FileUtils.deleteDirectory(classes);
            }
            if (aar.exists()) {
              FileUtils.deleteDirectory(aar);
            }
            if (outputs.exists()) {
              FileUtils.deleteDirectory(outputs);
            }
            compileRes(res, bin_res, root);
            compileLibraries(librariesToCompile, root, bin_res);
            linkRes(bin_res, root, manifest, assets);

            if (java.exists()) {
              getLogger().debug("> Task :" + root + ":" + "compileJava");
              compileJava(java, gen, classes, root);
            }

            if (classes.exists()) {
              getLogger().debug("> Task :" + root + ":" + "assembleAar");
              assembleAar(classes, aar, build, root);
            }
          }

        } catch (IOException e) {
        }
      }
    }
  }

  private List<File> getLibraries(String root, File bin_res) throws IOException {
    if (!bin_res.exists()) {
      if (!bin_res.mkdirs()) {
        throw new IOException("Failed to create resource directory");
      }
    }

    List<File> libraries = new ArrayList<>();

    for (File library :
        getLibraries(new File(getModule().getProjectDir(), root + "/build/api_files/libs"))) {
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
        getLibraries(new File(getModule().getProjectDir(), root + "/build/api_libs"))) {
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
            new File(getModule().getProjectDir(), root + "/build/implementation_files/libs"))) {
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
        getLibraries(new File(getModule().getProjectDir(), root + "/build/implementation_libs"))) {
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
            new File(getModule().getProjectDir(), root + "/build/compileOnly_files/libs"))) {
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
        getLibraries(new File(getModule().getProjectDir(), root + "/build/compileOnly_libs"))) {
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
            new File(getModule().getProjectDir(), root + "/build/runtimeOnly_files/libs"))) {
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
        getLibraries(new File(getModule().getProjectDir(), root + "/build/runtimeOnly_libs"))) {
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
    copyResources(
        new File(getModule().getProjectDir(), name + "/src/main/AndroidManifest.xml"),
        aar.getAbsolutePath());
    copyResources(
        new File(getModule().getProjectDir(), name + "/src/main/res"), aar.getAbsolutePath());

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

  private boolean mHasErrors = false;

  public void compileJava(File java, File gen, File out, String name)
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
    File api_files = new File(getModule().getProjectDir(), name + "/build/api_files/libs");
    File api_libs = new File(getModule().getProjectDir(), name + "/build/api_libs");

    File implementation_files =
        new File(getModule().getProjectDir(), name + "/build/implementation_files/libs");
    File implementation_libs =
        new File(getModule().getProjectDir(), name + "/build/implementation_libs");

    File runtimeOnly_files =
        new File(getModule().getProjectDir(), name + "/build/runtimeOnly_files/libs");
    File runtimeOnly_libs = new File(getModule().getProjectDir(), name + "/build/runtimeOnly_libs");

    File compileOnly_files =
        new File(getModule().getProjectDir(), name + "/build/compileOnly_files/libs");
    File compileOnly_libs = new File(getModule().getProjectDir(), name + "/build/compileOnly_libs");

    List<File> compileClassPath = new ArrayList<>();
    compileClassPath.addAll(getJarFiles(api_files));
    compileClassPath.addAll(getJarFiles(api_libs));
    compileClassPath.addAll(getJarFiles(implementation_files));
    compileClassPath.addAll(getJarFiles(implementation_libs));
    compileClassPath.addAll(getJarFiles(compileOnly_files));
    compileClassPath.addAll(getJarFiles(compileOnly_libs));

    List<File> runtimeClassPath = new ArrayList<>();
    runtimeClassPath.addAll(getJarFiles(runtimeOnly_files));
    runtimeClassPath.addAll(getJarFiles(runtimeOnly_libs));
    runtimeClassPath.add(getModule().getBootstrapJarFile());
    runtimeClassPath.add(getModule().getLambdaStubsJarFile());
    runtimeClassPath.addAll(getJarFiles(api_files));
    runtimeClassPath.addAll(getJarFiles(api_libs));

    List<JavaFileObject> javaFileObjects = new ArrayList<>();

    mFilesToCompile.addAll(getJavaFiles(java));
    mFilesToCompile.addAll(getJavaFiles(gen));

    if (mFilesToCompile.isEmpty()) {
      return;
    }

    List<String> options = new ArrayList<>();
    options.add("-source");
    options.add("1.8");
    options.add("-target");
    options.add("1.8");

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
    }
  }

  private void linkRes(File in, String name, File manifest, File assets)
      throws CompilationFailedException, IOException {
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
        getLibraries(new File(getModule().getProjectDir(), name + "/build/api_files/libs"))) {
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
        getLibraries(new File(getModule().getProjectDir(), name + "/build/api_libs"))) {
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
            new File(getModule().getProjectDir(), name + "/build/implementation_files/libs"))) {
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
        getLibraries(new File(getModule().getProjectDir(), name + "/build/implementation_libs"))) {
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
            new File(getModule().getProjectDir(), name + "/build/compileOnly_files/libs"))) {
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
        getLibraries(new File(getModule().getProjectDir(), name + "/build/compileOnly_libs"))) {
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
            new File(getModule().getProjectDir(), name + "/build/runtimeOnly_files/libs"))) {
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
        getLibraries(new File(getModule().getProjectDir(), name + "/build/runtimeOnly_libs"))) {
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
}
