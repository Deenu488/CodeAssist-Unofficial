package com.tyron.code.ui.project;

import android.util.Log;
import com.deenu143.gradle.utils.DependencyUtils;
import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.tyron.builder.log.ILogger;
import com.tyron.builder.model.Library;
import com.tyron.builder.model.ModuleSettings;
import com.tyron.builder.project.api.AndroidModule;
import com.tyron.builder.project.api.JavaModule;
import com.tyron.builder.project.api.Module;
import com.tyron.code.ApplicationLoader;
import com.tyron.common.util.AndroidUtilities;
import com.tyron.common.util.Decompress;
import com.tyron.resolver.DependencyResolver;
import com.tyron.resolver.RepositoryModel;
import com.tyron.resolver.ScopeType;
import com.tyron.resolver.model.Dependency;
import com.tyron.resolver.model.Pom;
import com.tyron.resolver.repository.LocalRepository;
import com.tyron.resolver.repository.RemoteRepository;
import com.tyron.resolver.repository.Repository;
import com.tyron.resolver.repository.RepositoryManager;
import com.tyron.resolver.repository.RepositoryManagerImpl;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipFile;
import org.apache.commons.io.FileUtils;

public class DependencyManager {

  private static final String REPOSITORIES_JSON = "repositories.json";

  private final RepositoryManager mRepository;

  public DependencyManager(JavaModule module, File cacheDir) throws IOException {
    extractCommonPomsIfNeeded();

    mRepository = new RepositoryManagerImpl();
    mRepository.setCacheDirectory(cacheDir);
    for (Repository repository : getFromModule(module)) {
      mRepository.addRepository(repository);
    }
    mRepository.initialize();
  }

  public static List<Repository> getFromModule(JavaModule module) throws IOException {
    File repositoriesFile = new File(module.getProjectDir(), ".idea/" + REPOSITORIES_JSON);
    List<RepositoryModel> repositoryModels = parseFile(repositoriesFile);
    List<Repository> repositories = new ArrayList<>();
    for (RepositoryModel model : repositoryModels) {
      if (model.getName() == null) {
        continue;
      }
      if (model.getUrl() == null) {
        repositories.add(new LocalRepository(model.getName()));
      } else {
        repositories.add(new RemoteRepository(model.getName(), model.getUrl()));
      }
    }
    return repositories;
  }

  public static List<RepositoryModel> parseFile(File file) throws IOException {
    try {
      String contents = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
      Type type = new TypeToken<List<RepositoryModel>>() {}.getType();
      List<RepositoryModel> models =
          new GsonBuilder().setLenient().create().fromJson(contents, type);
      if (models != null) {
        return models;
      }
    } catch (JsonSyntaxException e) {
      // returning an empty list for now, should probably log this
      return Collections.emptyList();
    } catch (IOException ignored) {
      // add default ones
    }

    List<RepositoryModel> defaultRepositories = getDefaultRepositories();
    String jsonContents =
        new GsonBuilder().setPrettyPrinting().create().toJson(defaultRepositories);
    FileUtils.writeStringToFile(file, jsonContents, StandardCharsets.UTF_8);
    return defaultRepositories;
  }

  public static List<RepositoryModel> getDefaultRepositories() {
    return ImmutableList.<RepositoryModel>builder()
        .add(new RepositoryModel("maven", "https://repo1.maven.org/maven2"))
        .add(new RepositoryModel("google-maven", "https://maven.google.com"))
        .add(new RepositoryModel("jitpack", "https://jitpack.io"))
        .add(new RepositoryModel("jcenter", "https://jcenter.bintray.com"))
        .build();
  }

  private void extractCommonPomsIfNeeded() {
    File cacheDir = ApplicationLoader.applicationContext.getExternalFilesDir("cache");
    File pomsDir = new File(cacheDir, "google-maven");
    File[] children = pomsDir.listFiles();
    if (!pomsDir.exists() || children == null || children.length == 0) {
      Decompress.unzipFromAssets(
          ApplicationLoader.applicationContext, "google-maven.zip", pomsDir.getParent());
    }
  }

  public void resolve(JavaModule project, ProjectManager.TaskListener listener, ILogger logger)
      throws IOException {

    listener.onTaskStarted("Resolving dependencies");
    logger.debug("> Configure project :" + project.getRootFile().getName());

    resolveMainDependency(
        project,
        project.getRootFile(),
        listener,
        logger,
        project.getGradleFile(),
        project.getRootFile().getName());

    List<String> projects = new ArrayList<>();
    projects.addAll(project.getAllProjects(project.getGradleFile()));
    Set<String> resolvedProjects = new HashSet<>();
    while (!projects.isEmpty()) {
      String include = projects.remove(0);
      if (resolvedProjects.contains(include)) {
        continue;
      }
      resolvedProjects.add(include);
      File gradleFile = new File(project.getProjectDir(), include + "/build.gradle");
      if (gradleFile.exists()) {
        List<String> includedInBuildGradle = project.getAllProjects(gradleFile);
        if (!includedInBuildGradle.isEmpty()) {
          projects.addAll(includedInBuildGradle);
        }
        File includeName = new File(project.getProjectDir(), include);
        String root = include.replaceFirst("/", "").replaceAll("/", ":");
        logger.debug("> Task :" + root + ":" + "resolvingDependencies");
        try {
          resolveMainDependency(project, includeName, listener, logger, gradleFile, root);
        } catch (IOException e) {
        }
      }
    }
  }

  private void resolveMainDependency(
      JavaModule project,
      File root,
      ProjectManager.TaskListener listener,
      ILogger logger,
      File gradleFile,
      String name)
      throws IOException {
    List<Dependency> declaredApiDependencies =
        DependencyUtils.parseDependencies(mRepository, gradleFile, logger, ScopeType.API);

    List<Dependency> declaredImplementationDependencies =
        DependencyUtils.parseDependencies(
            mRepository, gradleFile, logger, ScopeType.IMPLEMENTATION);

    AndroidModule androidModule = (AndroidModule) project;
    if (project instanceof AndroidModule) {
      if (androidModule.getViewBindingEnabled() && project.getRootFile().getName().equals(name)) {
        Dependency databindingDependency =
            new Dependency("androidx.databinding", "viewbinding", "8.1.0-alpha11");
        declaredImplementationDependencies.add(databindingDependency);
      }
    }
    List<Dependency> declaredCompileOnlyDependencies =
        DependencyUtils.parseDependencies(mRepository, gradleFile, logger, ScopeType.COMPILE_ONLY);
    List<Dependency> declaredRuntimeOnlyDependencies =
        DependencyUtils.parseDependencies(mRepository, gradleFile, logger, ScopeType.RUNTIME_ONLY);

    List<Dependency> declaredCompileOnlyApiDependencies =
        DependencyUtils.parseDependencies(
            mRepository, gradleFile, logger, ScopeType.COMPILE_ONLY_API);
    List<Dependency> declaredRuntimeOnlyApiDependencies =
        DependencyUtils.parseDependencies(
            mRepository, gradleFile, logger, ScopeType.RUNTIME_ONLY_API);

    DependencyResolver mResolver = new DependencyResolver(mRepository);

    mResolver.setResolveListener(
        new DependencyResolver.ResolveListener() {
          @Override
          public void onResolve(String message) {}

          @Override
          public void onFailure(String message) {
            logger.error(message);
          }
        });

    List<Pom> resolvedApiPoms = new ArrayList<>();
    if (resolvedApiPoms != null) {
      resolvedApiPoms.clear();
    }
    List<Pom> resolvedImplementationPoms = new ArrayList<>();
    if (resolvedImplementationPoms != null) {
      resolvedImplementationPoms.clear();
    }
    List<Pom> resolvedCompileOnlyPoms = new ArrayList<>();
    if (resolvedCompileOnlyPoms != null) {
      resolvedCompileOnlyPoms.clear();
    }
    List<Pom> resolvedRuntimeOnlyPoms = new ArrayList<>();
    if (resolvedRuntimeOnlyPoms != null) {
      resolvedRuntimeOnlyPoms.clear();
    }
    List<Pom> resolvedCompileOnlyApiPoms = new ArrayList<>();
    if (resolvedCompileOnlyPoms != null) {
      resolvedCompileOnlyPoms.clear();
    }
    List<Pom> resolvedRuntimeOnlyApiPoms = new ArrayList<>();
    if (resolvedRuntimeOnlyPoms != null) {
      resolvedRuntimeOnlyPoms.clear();
    }

    File idea = new File(project.getProjectDir(), ".idea");
    listener.onTaskStarted("Downloading dependencies");
    logger.debug("> Task :" + name + ":" + "downloadingDependencies");

    ScopeType api = ScopeType.API;
    String scopeTypeApi = api.getStringValue();

    if (!declaredApiDependencies.isEmpty()) {
      resolvedApiPoms = mResolver.resolveDependencies(declaredApiDependencies);
      List<Library> apiLibraries = getFiles(resolvedApiPoms, logger);
      checkDependencies(project, root, idea, logger, apiLibraries, gradleFile, scopeTypeApi);
      resolvedApiPoms.clear();
      apiLibraries.clear();
    }

    checkLibraries(project, root, idea, logger, gradleFile, scopeTypeApi);

    ScopeType implementation = ScopeType.IMPLEMENTATION;
    String scopeTypeImplementation = implementation.getStringValue();

    if (!declaredImplementationDependencies.isEmpty()) {
      resolvedImplementationPoms =
          mResolver.resolveDependencies(declaredImplementationDependencies);
      List<Library> implementationLibraries = getFiles(resolvedImplementationPoms, logger);
      checkDependencies(
          project,
          root,
          idea,
          logger,
          implementationLibraries,
          gradleFile,
          scopeTypeImplementation);
      resolvedImplementationPoms.clear();
      implementationLibraries.clear();
    }

    checkLibraries(project, root, idea, logger, gradleFile, scopeTypeImplementation);

    ScopeType compileOnly = ScopeType.COMPILE_ONLY;
    String scopeTypeCompileOnly = compileOnly.getStringValue();

    if (!declaredCompileOnlyDependencies.isEmpty()) {
      resolvedCompileOnlyPoms = mResolver.resolveDependencies(declaredCompileOnlyDependencies);
      List<Library> compileOnlyLibraries = getFiles(resolvedCompileOnlyPoms, logger);
      checkDependencies(
          project, root, idea, logger, compileOnlyLibraries, gradleFile, scopeTypeCompileOnly);
      resolvedCompileOnlyPoms.clear();
      compileOnlyLibraries.clear();
    }

    checkLibraries(project, root, idea, logger, gradleFile, scopeTypeCompileOnly);

    ScopeType compileOnlyApi = ScopeType.COMPILE_ONLY_API;
    String scopeTypeCompileOnlyApi = compileOnlyApi.getStringValue();

    if (!declaredCompileOnlyApiDependencies.isEmpty()) {
      resolvedCompileOnlyApiPoms =
          mResolver.resolveDependencies(declaredCompileOnlyApiDependencies);
      List<Library> compileOnlyApiLibraries = getFiles(resolvedCompileOnlyApiPoms, logger);
      checkDependencies(
          project,
          root,
          idea,
          logger,
          compileOnlyApiLibraries,
          gradleFile,
          scopeTypeCompileOnlyApi);
      resolvedCompileOnlyApiPoms.clear();
      compileOnlyApiLibraries.clear();
    }

    checkLibraries(project, root, idea, logger, gradleFile, scopeTypeCompileOnlyApi);

    ScopeType runtimeOnly = ScopeType.RUNTIME_ONLY;
    String scopeTypeRuntimeOnly = runtimeOnly.getStringValue();

    if (!declaredRuntimeOnlyDependencies.isEmpty()) {
      resolvedRuntimeOnlyPoms = mResolver.resolveDependencies(declaredRuntimeOnlyDependencies);
      List<Library> runtimeOnlyLibraries = getFiles(resolvedRuntimeOnlyPoms, logger);
      checkDependencies(
          project, root, idea, logger, runtimeOnlyLibraries, gradleFile, scopeTypeRuntimeOnly);
      resolvedRuntimeOnlyPoms.clear();
      runtimeOnlyLibraries.clear();
    }

    checkLibraries(project, root, idea, logger, gradleFile, scopeTypeRuntimeOnly);

    ScopeType runtimeOnlyApi = ScopeType.RUNTIME_ONLY_API;
    String scopeTypeRuntimeOnlyApi = runtimeOnlyApi.getStringValue();

    if (!declaredRuntimeOnlyApiDependencies.isEmpty()) {
      resolvedRuntimeOnlyApiPoms =
          mResolver.resolveDependencies(declaredRuntimeOnlyApiDependencies);
      List<Library> runtimeOnlyApiLibraries = getFiles(resolvedRuntimeOnlyApiPoms, logger);
      checkDependencies(
          project,
          root,
          idea,
          logger,
          runtimeOnlyApiLibraries,
          gradleFile,
          scopeTypeRuntimeOnlyApi);
      resolvedRuntimeOnlyApiPoms.clear();
      runtimeOnlyApiLibraries.clear();
    }

    checkLibraries(project, root, idea, logger, gradleFile, scopeTypeRuntimeOnlyApi);

    listener.onTaskStarted("Checking libraries");
    logger.debug("> Task :" + name + ":" + "checkingLibraries");
  }

  // checkDependencies
  private void checkDependencies(
      JavaModule project,
      File root,
      File idea,
      ILogger logger,
      List<Library> newLibraries,
      File gradleFile,
      String scope)
      throws IOException {

    Set<Library> libraries = new HashSet<>(newLibraries);
    Map<String, Library> fileLibsHashes = new HashMap<>();
    Map<String, Library> md5Map = new HashMap<>();

    libraries =
        new HashSet<>(
            parseLibraries(
                libraries,
                new File(idea, root.getName() + "_" + scope + "_libraries.json"),
                scope));

    md5Map =
        new HashMap<>(
            checkLibraries(
                md5Map,
                libraries,
                fileLibsHashes,
                new File(root, "build/libraries/" + scope + "_libs")));

    saveLibraryToProject(
        project,
        new File(root, "build/libraries/" + scope + "_libs"),
        new File(idea, root.getName() + "_" + scope + "_libraries.json"),
        scope,
        md5Map,
        fileLibsHashes);
    md5Map.clear();
    fileLibsHashes.clear();
    libraries.clear();
  }

  // checkLibraries
  private void checkLibraries(
      JavaModule project, File root, File idea, ILogger logger, File gradleFile, String scope)
      throws IOException {

    Set<Library> libraries = new HashSet<>();
    Map<String, Library> fileLibsHashes = new HashMap<>();
    Map<String, Library> md5Map = new HashMap<>();

    AbstractMap.SimpleEntry<ArrayList<String>, ArrayList<String>> result =
        project.extractListDirAndIncludes(gradleFile, scope);
    if (result != null) {
      ArrayList<String> dirValue = result.getKey();
      ArrayList<String> includeValues = result.getValue();
      if (dirValue != null && includeValues != null) {
        for (int i = 0; i < dirValue.size(); i++) {
          String dir = dirValue.get(i);
          String include = includeValues.get(i);
          fileLibsHashes =
              new HashMap<>(
                  checkDirLibraries(fileLibsHashes, logger, new File(root, dir), include, scope));
        }
      }
    }

    List<AbstractMap.SimpleEntry<String, ArrayList<String>>> results =
        project.extractDirAndIncludes(gradleFile, scope);
    if (results != null) {
      for (AbstractMap.SimpleEntry<String, ArrayList<String>> entry : results) {
        String dir = entry.getKey();
        ArrayList<String> includes = entry.getValue();
        if (dir != null && includes != null) {
          fileLibsHashes =
              new HashMap<>(
                  checkDirIncludeLibraries(
                      fileLibsHashes, logger, new File(root, dir), includes, scope));
        }
      }
    }

    libraries =
        new HashSet<>(
            parseLibraries(
                libraries,
                new File(idea, root.getName() + "_" + scope + "_libraries.json"),
                root.getName()));

    md5Map =
        new HashMap<>(
            checkLibraries(
                md5Map,
                libraries,
                fileLibsHashes,
                new File(root, "build/libraries/" + scope + "_files/libs")));

    saveLibraryToProject(
        project,
        new File(root, "build/libraries/" + scope + "_files/libs"),
        new File(idea, root.getName() + "_" + scope + "_libraries.json"),
        scope + "Files",
        md5Map,
        fileLibsHashes);
    md5Map.clear();
    fileLibsHashes.clear();
    libraries.clear();
  }

  public Map<String, Library> checkDirLibraries(
      Map<String, Library> fileLibsHashes, ILogger logger, File dir, String include, String scope) {
    try {
      ZipFile zipFile = new ZipFile(new File(dir, include));
      Library library = new Library();
      library.setSourceFile(new File(dir, include));
      fileLibsHashes.put(AndroidUtilities.calculateMD5(new File(dir, include)), library);
    } catch (IOException e) {
      String message = "File " + include + " is corrupt! Ignoring.";
      logger.warning(message);
    }
    return fileLibsHashes;
  }

  public Map<String, Library> checkDirIncludeLibraries(
      Map<String, Library> fileLibsHashes,
      ILogger logger,
      File dir,
      ArrayList<String> includes,
      String scope) {
    for (String ext : includes) {
      File[] fileLibraries = dir.listFiles(c -> c.getName().endsWith(ext));
      if (fileLibraries != null) {
        for (File fileLibrary : fileLibraries) {
          try {
            ZipFile zipFile = new ZipFile(fileLibrary);
            Library library = new Library();
            library.setSourceFile(fileLibrary);
            fileLibsHashes.put(AndroidUtilities.calculateMD5(fileLibrary), library);
          } catch (IOException e) {
            String message = "File " + fileLibrary + " is corrupt! Ignoring.";
            logger.warning(message);
          }
        }
      }
    }
    return fileLibsHashes;
  }

  public Set<Library> parseLibraries(Set<Library> libraries, File file, String scope) {
    ModuleSettings myModuleSettings = new ModuleSettings(file);
    String librariesString = myModuleSettings.getString(scope + "_libraries", "[]");
    try {
      List<Library> parsedLibraries =
          new Gson().fromJson(librariesString, new TypeToken<List<Library>>() {}.getType());
      if (parsedLibraries != null) {
        for (Library parsedLibrary : parsedLibraries) {
          if (!libraries.contains(parsedLibrary)) {
            Log.d("LibraryCheck", "Removed library" + parsedLibrary);
          } else {
            libraries.add(parsedLibrary);
          }
        }
      }
    } catch (Exception ignore) {
    }
    return libraries;
  }

  public Map<String, Library> checkLibraries(
      Map<String, Library> md5Map,
      Set<Library> libraries,
      Map<String, Library> fileLibsHashes,
      File libs)
      throws IOException {
    libraries.forEach(it -> md5Map.put(AndroidUtilities.calculateMD5(it.getSourceFile()), it));

    if (!libs.exists()) {
      if (!libs.mkdirs()) {}
    }
    File[] buildLibraryDirs = libs.listFiles(File::isDirectory);
    if (buildLibraryDirs != null) {
      for (File libraryDir : buildLibraryDirs) {
        String md5Hash = libraryDir.getName();
        if (!md5Map.containsKey(md5Hash) && !fileLibsHashes.containsKey(md5Hash)) {
          FileUtils.deleteDirectory(libraryDir);
          Log.d("LibraryCheck", "Deleting contents of " + md5Hash);
        }
      }
    }
    return md5Map;
  }

  private void saveLibraryToProject(
      Module module,
      File libs,
      File file,
      String scope,
      Map<String, Library> libraries,
      Map<String, Library> fileLibraries)
      throws IOException {
    Map<String, Library> combined = new HashMap<>();
    combined.putAll(libraries);
    combined.putAll(fileLibraries);

    if (module instanceof JavaModule) {
      ((JavaModule) module).putLibraryHashes(combined);
    }

    for (Map.Entry<String, Library> entry : combined.entrySet()) {
      String hash = entry.getKey();
      Library library = entry.getValue();

      File libraryDir = new File(libs, hash);
      if (!libraryDir.exists()) {
        libraryDir.mkdir();
      } else {
        continue;
      }

      if (library.getSourceFile().getName().endsWith(".jar")) {
        FileUtils.copyFileToDirectory(library.getSourceFile(), libraryDir);

        File jar = new File(libraryDir, library.getSourceFile().getName());
        jar.renameTo(new File(libraryDir, "classes.jar"));
      } else if (library.getSourceFile().getName().endsWith(".aar")) {
        Decompress.unzip(library.getSourceFile().getAbsolutePath(), libraryDir.getAbsolutePath());
      }
    }
    saveToGson(combined.values(), file, scope);
  }

  private void saveToGson(Collection values, File file, String scope) {
    ModuleSettings myModuleSettings = new ModuleSettings(file);
    String librariesString = new Gson().toJson(values);
    myModuleSettings.edit().putString(scope + "_libraries", librariesString).apply();
  }

  public List<Library> getFiles(List<Pom> resolvedPoms, ILogger logger) {
    List<Library> files = new ArrayList<>();
    for (Pom resolvedPom : resolvedPoms) {
      try {
        File file = mRepository.getLibrary(resolvedPom);
        if (file != null) {
          Library library = new Library();
          library.setSourceFile(file);
          library.setDeclaration(resolvedPom.getDeclarationString());
          files.add(library);
        }
      } catch (IOException e) {
        logger.error("Unable to download " + resolvedPom + ": " + e.getMessage());
      }
    }
    return files;
  }
}
