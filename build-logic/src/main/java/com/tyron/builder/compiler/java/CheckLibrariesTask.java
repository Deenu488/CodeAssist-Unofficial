package com.tyron.builder.compiler.java;

import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.tyron.builder.compiler.BuildType;
import com.tyron.builder.compiler.ScopeType;
import com.tyron.builder.compiler.Task;
import com.tyron.builder.exception.CompilationFailedException;
import com.tyron.builder.log.ILogger;
import com.tyron.builder.model.Library;
import com.tyron.builder.model.ModuleSettings;
import com.tyron.builder.project.Project;
import com.tyron.builder.project.api.JavaModule;
import com.tyron.builder.project.api.Module;
import com.tyron.common.util.Decompress;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipFile;
import org.apache.commons.io.FileUtils;

/** Task responsible for copying aars/jars from libraries to build/libs */
public class CheckLibrariesTask extends Task<JavaModule> {

  public CheckLibrariesTask(Project project, JavaModule module, ILogger logger) {
    super(project, module, logger);
  }

  @Override
  public String getName() {
    return "checkLibraries";
  }

  @Override
  public void prepare(BuildType type) throws IOException {}

  @Override
  public void run() throws IOException, CompilationFailedException {

    checkLibraries(
        getModule(),
        getModule().getRootFile(),
        getLogger(),
        getModule().getGradleFile(),
        getModule().getRootFile().getName());

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
        getLogger().debug("> Task :" + root + ":" + "checkingLibraries");
        try {
          checkLibraries(getModule(), includeName, getLogger(), gradleFile, root);
        } catch (IOException e) {
        }
      }
    }
  }

  private void checkLibraries(
      JavaModule project, File root, ILogger logger, File gradleFile, String name)
      throws IOException {

    File idea = new File(project.getProjectDir(), ".idea");

    ScopeType api = ScopeType.API;
    String scopeTypeApi = api.getStringValue();
    checkLibraries(project, root, idea, logger, gradleFile, scopeTypeApi);

    ScopeType implementation = ScopeType.IMPLEMENTATION;
    String scopeTypeImplementation = implementation.getStringValue();
    checkLibraries(project, root, idea, logger, gradleFile, scopeTypeImplementation);

    ScopeType compileOnly = ScopeType.COMPILE_ONLY;
    String scopeTypeCompileOnly = compileOnly.getStringValue();
    checkLibraries(project, root, idea, logger, gradleFile, scopeTypeCompileOnly);

    ScopeType runtimeOnly = ScopeType.RUNTIME_ONLY;
    String scopeTypeRuntimeOnly = runtimeOnly.getStringValue();
    checkLibraries(project, root, idea, logger, gradleFile, scopeTypeRuntimeOnly);

    ScopeType compileOnlyApi = ScopeType.COMPILE_ONLY_API;
    String scopeTypeCompileOnlyApi = compileOnlyApi.getStringValue();
    checkLibraries(project, root, idea, logger, gradleFile, scopeTypeCompileOnlyApi);

    ScopeType runtimeOnlyApi = ScopeType.RUNTIME_ONLY_API;
    String scopeTypeRuntimeOnlyApi = runtimeOnlyApi.getStringValue();
    checkLibraries(project, root, idea, logger, gradleFile, scopeTypeRuntimeOnlyApi);
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
      fileLibsHashes.put(calculateMD5(new File(dir, include)), library);
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
            fileLibsHashes.put(calculateMD5(fileLibrary), library);
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
        /*for (Library parsedLibrary : parsedLibraries) {
        if (!libraries.contains(parsedLibrary)) {
        Log.d("LibraryCheck", "Removed library" + parsedLibrary);
        } else {
        libraries.add(parsedLibrary);
        }
        }*/
        libraries.addAll(parsedLibraries);
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
    libraries.forEach(it -> md5Map.put(calculateMD5(it.getSourceFile()), it));

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

    getModule().putLibraryHashes(combined);

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

  public static String calculateMD5(File updateFile) {
    InputStream is;
    try {
      is = new FileInputStream(updateFile);
    } catch (FileNotFoundException e) {
      Log.e("calculateMD5", "Exception while getting FileInputStream", e);
      return null;
    }

    return calculateMD5(is);
  }

  public static String calculateMD5(InputStream is) {
    MessageDigest digest;
    try {
      digest = MessageDigest.getInstance("MD5");
    } catch (NoSuchAlgorithmException e) {
      Log.e("calculateMD5", "Exception while getting Digest", e);
      return null;
    }

    byte[] buffer = new byte[8192];
    int read;
    try {
      while ((read = is.read(buffer)) > 0) {
        digest.update(buffer, 0, read);
      }
      byte[] md5sum = digest.digest();
      BigInteger bigInt = new BigInteger(1, md5sum);
      String output = bigInt.toString(16);
      // Fill to 32 chars
      output = String.format("%32s", output).replace(' ', '0');
      return output;
    } catch (IOException e) {
      throw new RuntimeException("Unable to process file for MD5", e);
    } finally {
      try {
        is.close();
      } catch (IOException e) {
        Log.e("calculateMD5", "Exception on closing MD5 input stream", e);
      }
    }
  }
}
