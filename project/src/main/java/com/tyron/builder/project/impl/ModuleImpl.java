package com.tyron.builder.project.impl;

import androidx.annotation.Nullable;
import com.tyron.builder.model.ModuleSettings;
import com.tyron.builder.project.api.FileManager;
import com.tyron.builder.project.api.Module;
import com.tyron.builder.project.cache.CacheHolder.CacheKey;
import com.tyron.common.util.Cache;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.com.intellij.openapi.util.Key;
import org.jetbrains.kotlin.com.intellij.openapi.util.KeyWithDefaultValue;
import org.jetbrains.kotlin.com.intellij.util.concurrency.AtomicFieldUpdater;
import org.jetbrains.kotlin.com.intellij.util.keyFMap.KeyFMap;

public class ModuleImpl implements Module {

  /** Concurrent writes to this field are via CASes only, using the {@link #updater} */
  @NotNull private volatile KeyFMap myUserMap = KeyFMap.EMPTY_MAP;

  private final File mRoot;
  private ModuleSettings myModuleSettings;
  private FileManager mFileManager;

  public ModuleImpl(File root) {
    mRoot = root;
    mFileManager = new FileManagerImpl(root);
  }

  @Override
  public void open() throws IOException {
    File codeassist = new File(getProjectDir(), ".idea");
    if (!codeassist.exists()) {
      if (!codeassist.mkdirs()) {}
    }
    myModuleSettings =
        new ModuleSettings(new File(codeassist, getRootFile().getName() + "_libraries.json"));
  }

  @Override
  public void clear() {}

  @Override
  public void index() {}

  @Override
  public File getBuildDirectory() {
    File custom = getPathSetting("build_directory");
    if (custom.exists()) {
      return custom;
    }
    return new File(getRootFile(), "build");
  }

  @Override
  public File getBuildClassesDirectory() {
    File custom = getPathSetting("build_classes_directory");
    if (custom.exists()) {
      return custom;
    }
    return new File(getRootFile(), "build/bin/java/classes");
  }

  @Override
  public ModuleSettings getSettings() {
    return myModuleSettings;
  }

  @Override
  public FileManager getFileManager() {
    return mFileManager;
  }

  @Override
  public File getRootFile() {
    return mRoot;
  }

  @Override
  public File getProjectDir() {
    return mRoot.getParentFile();
  }

  @Override
  public File getGradleFile() {
    File gradleFile = new File(getRootFile(), "build.gradle");
    return gradleFile;
  }

  @Override
  public File getSettingsGradleFile() {
    File settingsGradleFile = new File(getProjectDir(), "settings.gradle");
    return settingsGradleFile;
  }

  @Override
  public List<String> getPlugins() {
    return parsePlugins(getGradleFile());
  }

  @Override
  public List<String> getPlugins(File gradleFile) {
    return parsePlugins(gradleFile);
  }

  @Override
  public List<String> getAllProjects() {
    return parseAllProjects(getGradleFile());
  }

  @Override
  public List<String> getAllProjects(File gradleFile) {
    return parseAllProjects(gradleFile);
  }

  @Override
  public List<String> getIncludedProjects() {
    return parseIncludedProjects(getSettingsGradleFile());
  }

  @Override
  public List<AbstractMap.SimpleEntry<String, ArrayList<String>>> extractDirAndIncludes(
      String scope) {
    return parseDirAndIncludes(getGradleFile(), scope);
  }

  @Override
  public List<AbstractMap.SimpleEntry<String, ArrayList<String>>> extractDirAndIncludes(
      File gradleFile, String scope) {
    return parseDirAndIncludes(gradleFile, scope);
  }

  @Override
  public AbstractMap.SimpleEntry<ArrayList<String>, ArrayList<String>> extractListDirAndIncludes(
      String scope) {
    return parseListDirAndIncludes(getGradleFile(), scope);
  }

  @Override
  public AbstractMap.SimpleEntry<ArrayList<String>, ArrayList<String>> extractListDirAndIncludes(
      File gradleFile, String scope) {
    return parseListDirAndIncludes(gradleFile, scope);
  }

  @Override
  public int getMinSdk() {
    return parseMinSdk(getGradleFile());
  }

  private int parseMinSdk(File gradle) {
    if (gradle != null && gradle.exists()) {
      try {
        String readString = FileUtils.readFileToString(gradle, Charset.defaultCharset());
        return parseMinSdk(readString);
      } catch (IOException e) {
        // handle the exception here, if needed
      }
    }
    return 21;
  }

  public static int parseMinSdk(String readString) throws IOException {
    Pattern MIN_SDK = Pattern.compile("\\s*(minSdk)\\s*()([a-zA-Z0-9.'/-:\\-]+)()");
    Matcher matcher = MIN_SDK.matcher(readString);
    while (matcher.find()) {
      String declaration = matcher.group(3);
      if (declaration != null & !declaration.isEmpty()) {
        try {
          int minSdk = Integer.parseInt(String.valueOf(declaration));
          return minSdk;
        } catch (NumberFormatException e) {
          // Handle the exception here, such as logging an error or returning a default value
          e.printStackTrace();
        }
      }
    }
    return 21;
  }

  @Nullable
  @Override
  public <T> T getUserData(@NotNull Key<T> key) {
    T t = myUserMap.get(key);
    if (t == null && key instanceof KeyWithDefaultValue) {
      t = ((KeyWithDefaultValue<T>) key).getDefaultValue();
      putUserData(key, t);
    }
    return t;
  }

  @Override
  public <T> void putUserData(@NotNull Key<T> key, @Nullable T value) {
    while (true) {
      KeyFMap map = myUserMap;
      KeyFMap newMap = value == null ? map.minus(key) : map.plus(key, value);
      if (newMap == map || updater.compareAndSet(this, map, newMap)) {
        break;
      }
    }
  }

  @NotNull
  @Override
  public <T> T putUserDataIfAbsent(@NotNull Key<T> key, @NotNull T value) {
    while (true) {
      KeyFMap map = myUserMap;
      T oldValue = map.get(key);
      if (oldValue != null) {
        return oldValue;
      }
      KeyFMap newMap = map.plus(key, value);
      if (newMap == map || updater.compareAndSet(this, map, newMap)) {
        return value;
      }
    }
  }

  @Override
  public <T> boolean replace(@NotNull Key<T> key, @Nullable T oldValue, @Nullable T newValue) {
    while (true) {
      KeyFMap map = myUserMap;
      if (map.get(key) != oldValue) {
        return false;
      }
      KeyFMap newMap = newValue == null ? map.minus(key) : map.plus(key, newValue);
      if (newMap == map || updater.compareAndSet(this, map, newMap)) {
        return true;
      }
    }
  }

  protected File getPathSetting(String key) {
    String path = getSettings().getString(key, "");
    return new File(path);
  }

  @Override
  public int hashCode() {
    return mRoot.hashCode();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof ModuleImpl)) return false;
    ModuleImpl project = (ModuleImpl) o;
    return mRoot.equals(project.mRoot);
  }

  private static final AtomicFieldUpdater<ModuleImpl, KeyFMap> updater =
      AtomicFieldUpdater.forFieldOfType(ModuleImpl.class, KeyFMap.class);

  private final Map<CacheKey<?, ?>, Cache<?, ?>> mCacheMap = new HashMap<>();

  @Override
  public <K, V> Cache<K, V> getCache(CacheKey<K, V> key, Cache<K, V> defaultValue) {
    Object o = mCacheMap.get(key);
    if (o == null) {
      put(key, defaultValue);
      return defaultValue;
    }
    //noinspection unchecked
    return (Cache<K, V>) o;
  }

  public <K, V> void removeCache(CacheKey<K, V> key) {
    mCacheMap.remove(key);
  }

  @Override
  public <K, V> void put(CacheKey<K, V> key, Cache<K, V> value) {
    mCacheMap.put(key, value);
  }

  private List<String> parsePlugins(File gradleFile) {
    try {
      String readString = FileUtils.readFileToString(gradleFile, Charset.defaultCharset());
      return parsePlugins(readString);
    } catch (IOException e) {
    }
    return null;
  }

  private List<String> parsePlugins(String readString) {
    Pattern APPLY_PLUGIN = Pattern.compile("\\s*apply\\s+plugin:\\s+[\"'](.+?)[\"']");
    Pattern PLUGINS_ID = Pattern.compile("\\s*id\\s*[\"']([a-zA-Z0-9.'/-:\\-]+)[\"'].*");
    List<String> plugins = new ArrayList<>();

    readString = readString.replaceAll("\\s*//.*", "");
    Matcher matcher = PLUGINS_ID.matcher(readString);
    while (matcher.find()) {
      String declaration = matcher.group(1);
      if (declaration != null) {
        plugins.add(String.valueOf(declaration));
      }
    }

    matcher = APPLY_PLUGIN.matcher(readString);
    while (matcher.find()) {
      String declaration = matcher.group(1);
      if (declaration != null) {
        plugins.add(String.valueOf(declaration));
      }
    }
    return plugins;
  }

  private List<String> parseProjects(File gradleFile) {
    try {
      String readString = FileUtils.readFileToString(gradleFile, Charset.defaultCharset());
      return parseProjects(readString);
    } catch (IOException e) {
    }
    return null;
  }

  public static List<String> parseProjects(String readString) throws IOException {
    final Pattern PROJECT_PATTERN =
        Pattern.compile("implementation project\\(path:\\s*['\"]([^'\"]+)['\"]\\)");
    final Pattern PROJECT_PATTERN_QUOT =
        Pattern.compile("implementation project\\(\\s*['\"]([^'\"]+)['\"]\\)");

    readString = readString.replaceAll("\\s*//.*", "");
    List<String> projects = new ArrayList<>();
    Matcher matcher = PROJECT_PATTERN.matcher(readString);
    while (matcher.find()) {
      String declaration = matcher.group(1);
      if (declaration != null) {
        declaration = declaration.replaceAll(":", "/");
        projects.add(declaration);
      }
    }

    matcher = PROJECT_PATTERN_QUOT.matcher(readString);
    while (matcher.find()) {
      String declaration = matcher.group(1);
      if (declaration != null) {
        declaration = declaration.replaceAll(":", "/");
        projects.add(declaration);
      }
    }
    return projects;
  }

  private List<String> parseAllProjects(File gradleFile) {
    try {
      String readString = FileUtils.readFileToString(gradleFile, Charset.defaultCharset());
      return parseAllProjects(readString);
    } catch (IOException e) {
    }
    return null;
  }

  public static List<String> parseAllProjects(String readString) throws IOException {
    final Pattern IMPLEMENTATION_PROJECT_PATH =
        Pattern.compile("implementation project\\(path:\\s*['\"]([^'\"]+)['\"]\\)");
    final Pattern IMPLEMENTATION_PROJECT =
        Pattern.compile("implementation project\\(\\s*['\"]([^'\"]+)['\"]\\)");
    final Pattern API_PROJECT_PATH =
        Pattern.compile("api project\\(path:\\s*['\"]([^'\"]+)['\"]\\)");
    final Pattern API_PROJECT = Pattern.compile("api project\\(\\s*['\"]([^'\"]+)['\"]\\)");
    final Pattern COMPILE_ONLY_PROJECT_PATH =
        Pattern.compile("compileOnly project\\(path:\\s*['\"]([^'\"]+)['\"]\\)");
    final Pattern COMPILE_ONLY_PROJECT =
        Pattern.compile("compileOnly project\\(\\s*['\"]([^'\"]+)['\"]\\)");
    final Pattern RUNTIME_ONLY_PROJECT_PATH =
        Pattern.compile("runtimeOnly project\\(path:\\s*['\"]([^'\"]+)['\"]\\)");
    final Pattern RUNTIME_ONLY_PROJECT =
        Pattern.compile("runtimeOnly project\\(\\s*['\"]([^'\"]+)['\"]\\)");

    final Pattern COMPILE_ONLY_API_PROJECT_PATH =
        Pattern.compile("compileOnlyApi project\\(path:\\s*['\"]([^'\"]+)['\"]\\)");
    final Pattern COMPILE_ONLY_API_PROJECT =
        Pattern.compile("compileOnlyApi project\\(\\s*['\"]([^'\"]+)['\"]\\)");
    final Pattern RUNTIME_ONLY_API_PROJECT_PATH =
        Pattern.compile("runtimeOnlyApi project\\(path:\\s*['\"]([^'\"]+)['\"]\\)");
    final Pattern RUNTIME_ONLY_API_PROJECT =
        Pattern.compile("runtimeOnlyApi project\\(\\s*['\"]([^'\"]+)['\"]\\)");

    readString = readString.replaceAll("\\s*//.*", "");
    List<String> projects = new ArrayList<>();

    Matcher matcher = IMPLEMENTATION_PROJECT_PATH.matcher(readString);
    while (matcher.find()) {
      String declaration = matcher.group(1);
      if (declaration != null) {
        declaration = declaration.replaceAll(":", "/");
        projects.add(declaration);
      }
    }

    matcher = IMPLEMENTATION_PROJECT.matcher(readString);
    while (matcher.find()) {
      String declaration = matcher.group(1);
      if (declaration != null) {
        declaration = declaration.replaceAll(":", "/");
        projects.add(declaration);
      }
    }

    matcher = API_PROJECT_PATH.matcher(readString);
    while (matcher.find()) {
      String declaration = matcher.group(1);
      if (declaration != null) {
        declaration = declaration.replaceAll(":", "/");
        projects.add(declaration);
      }
    }

    matcher = API_PROJECT.matcher(readString);
    while (matcher.find()) {
      String declaration = matcher.group(1);
      if (declaration != null) {
        declaration = declaration.replaceAll(":", "/");
        projects.add(declaration);
      }
    }

    matcher = COMPILE_ONLY_PROJECT_PATH.matcher(readString);
    while (matcher.find()) {
      String declaration = matcher.group(1);
      if (declaration != null) {
        declaration = declaration.replaceAll(":", "/");
        projects.add(declaration);
      }
    }

    matcher = COMPILE_ONLY_PROJECT.matcher(readString);
    while (matcher.find()) {
      String declaration = matcher.group(1);
      if (declaration != null) {
        declaration = declaration.replaceAll(":", "/");
        projects.add(declaration);
      }
    }

    matcher = RUNTIME_ONLY_PROJECT_PATH.matcher(readString);
    while (matcher.find()) {
      String declaration = matcher.group(1);
      if (declaration != null) {
        declaration = declaration.replaceAll(":", "/");
        projects.add(declaration);
      }
    }

    matcher = RUNTIME_ONLY_PROJECT.matcher(readString);
    while (matcher.find()) {
      String declaration = matcher.group(1);
      if (declaration != null) {
        declaration = declaration.replaceAll(":", "/");
        projects.add(declaration);
      }
    }

    matcher = COMPILE_ONLY_API_PROJECT_PATH.matcher(readString);
    while (matcher.find()) {
      String declaration = matcher.group(1);
      if (declaration != null) {
        declaration = declaration.replaceAll(":", "/");
        projects.add(declaration);
      }
    }

    matcher = COMPILE_ONLY_API_PROJECT.matcher(readString);
    while (matcher.find()) {
      String declaration = matcher.group(1);
      if (declaration != null) {
        declaration = declaration.replaceAll(":", "/");
        projects.add(declaration);
      }
    }

    matcher = RUNTIME_ONLY_API_PROJECT_PATH.matcher(readString);
    while (matcher.find()) {
      String declaration = matcher.group(1);
      if (declaration != null) {
        declaration = declaration.replaceAll(":", "/");
        projects.add(declaration);
      }
    }

    matcher = RUNTIME_ONLY_API_PROJECT.matcher(readString);
    while (matcher.find()) {
      String declaration = matcher.group(1);
      if (declaration != null) {
        declaration = declaration.replaceAll(":", "/");
        projects.add(declaration);
      }
    }

    return projects;
  }

  public static List<String> parseIncludedProjects(File file) {
    try {
      String readString = FileUtils.readFileToString(file, Charset.defaultCharset());
      return parseIncludedProjects(readString);
    } catch (IOException e) {
    }
    return null;
  }

  public static List<String> parseIncludedProjects(String readString) throws IOException {
    final Pattern INCLUDE = Pattern.compile("\\s*include\\s*(?:'|\\\")([\\w./:-]+)(?:'|\\\")");

    Matcher matcher = INCLUDE.matcher(readString.replaceAll("\\s*//.*", ""));
    List<String> included = new ArrayList<>();
    while (matcher.find()) {
      String declaration = matcher.group(1);
      if (declaration != null) {
        declaration = declaration.replaceAll(":", "/");
        included.add(declaration);
      }
    }
    return included;
  }

  private List<AbstractMap.SimpleEntry<String, ArrayList<String>>> parseDirAndIncludes(
      File file, String scope) {
    try {
      String readString = FileUtils.readFileToString(file, Charset.defaultCharset());
      return parseDirAndIncludes(readString, scope);
    } catch (IOException e) {
    }
    return null;
  }

  public static List<AbstractMap.SimpleEntry<String, ArrayList<String>>> parseDirAndIncludes(
      String readString, String scope) throws IOException {
    List<AbstractMap.SimpleEntry<String, ArrayList<String>>> results = new ArrayList<>();
    Pattern pattern =
        Pattern.compile(
            scope + "\\s+fileTree\\(dir:\\s*'([^']*)',\\s*include:\\s*\\[([^\\]]*)\\]\\)");
    readString = readString.replaceAll("\\s*//.*", "");
    Matcher matcher = pattern.matcher(readString);
    while (matcher.find()) {
      String dirValue = matcher.group(1);
      ArrayList<String> includeValues =
          new ArrayList<String>(Arrays.asList(matcher.group(2).split(",\\s*")));
      for (int i = 0; i < includeValues.size(); i++) {
        includeValues.set(i, includeValues.get(i).trim().replace("'", ""));
        includeValues.set(i, includeValues.get(i).replace("*", ""));
      }
      results.add(new AbstractMap.SimpleEntry<String, ArrayList<String>>(dirValue, includeValues));
    }
    return results;
  }

  public AbstractMap.SimpleEntry<ArrayList<String>, ArrayList<String>> parseListDirAndIncludes(
      File file, String scope) {
    try {
      String readString = FileUtils.readFileToString(file, Charset.defaultCharset());
      return parseListDirAndIncludes(readString, scope);
    } catch (IOException e) {
    }
    return null;
  }

  public static AbstractMap.SimpleEntry<ArrayList<String>, ArrayList<String>>
      parseListDirAndIncludes(String readString, String scope) throws IOException {
    Pattern pattern = Pattern.compile(scope + "\\s+files\\(([^)]+)\\)");
    readString = readString.replaceAll("\\s*//.*", "");
    ArrayList<String> dirValues = new ArrayList<>();
    ArrayList<String> includeValues = new ArrayList<>();
    Matcher matcher = pattern.matcher(readString);
    while (matcher.find()) {
      String[] filepaths = matcher.group(1).split(",");
      for (String filepath : filepaths) {
        String trimmedPath = filepath.trim().replaceAll("[\"']", "");
        dirValues.add(new File(trimmedPath).getParent());
        includeValues.add(new File(trimmedPath).getName());
      }
    }
    if (!dirValues.isEmpty() && !includeValues.isEmpty()) {
      return new AbstractMap.SimpleEntry<ArrayList<String>, ArrayList<String>>(
          dirValues, includeValues);
    }
    return null;
  }

  @Override
  public String getMainClass() {
    return parseMainClass(getGradleFile());
  }

  private String parseMainClass(File gradleFile) {
    try {
      String readString = FileUtils.readFileToString(gradleFile, Charset.defaultCharset());
      return parseMainClass(readString);
    } catch (IOException e) {
    }
    return null;
  }

  private String parseMainClass(String readString) {
    final Pattern mainClass = Pattern.compile("mainClass\\s*=\\s*[\"']([^\"']*)[\"']");

    readString = readString.replaceAll("\\s*//.*", "");

    Matcher matcher = mainClass.matcher(readString);
    while (matcher.find()) {
      String declaration = matcher.group(1);
      if (declaration != null) {
        return declaration;
      }
    }

    return null;
  }
}
