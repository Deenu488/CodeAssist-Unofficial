package com.tyron.builder.project.impl;

import androidx.annotation.Nullable;
import com.tyron.builder.model.ModuleSettings;
import com.tyron.builder.project.api.FileManager;
import com.tyron.builder.project.api.Module;
import com.tyron.common.util.Cache;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
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
  private static final Pattern PLUGINS_ID =
      Pattern.compile("\\s*(id)\\s*(')([a-zA-Z0-9.'/-:\\-]+)(')");
  private static final Pattern PLUGINS_ID_QUOT =
      Pattern.compile("\\s*(id)\\s*(\")([a-zA-Z0-9.'/-:\\-]+)(\")");

  public ModuleImpl(File root) {
    mRoot = root;
    mFileManager = new FileManagerImpl(root);
  }

  @Override
  public void open() throws IOException {
    File codeassist = new File(getRootProject(), ".idea");
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
  public File getRootProject() {
    return mRoot.getParentFile();
  }

  @Override
  public File getGradleFile() {
    File gradleFile = new File(getRootFile(), "build.gradle");
    return gradleFile;
  }

  @Override
  public File getSettingsGradleFile() {
    File settingsGradleFile = new File(getRootProject(), "settings.gradle");
    return settingsGradleFile;
  }

  @Override
  public String getPlugins() {
    return getPlugins(getGradleFile());
  }

  @Override
  public List<String> getImplementationProjects() {
    return parseImplementationProjects(getGradleFile());
  }

  @Override
  public List<String> getImplementationProjects(File gradleFile) {
    return parseImplementationProjects(gradleFile);
  }

  @Override
  public List<String> getIncludedProjects() {
    return parseIncludedProjects(getSettingsGradleFile());
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

  private String getPlugins(File gradleFile) {
    try {
      String readString = FileUtils.readFileToString(gradleFile, Charset.defaultCharset());
      return getPlugins(readString);
    } catch (IOException e) {
    }
    return null;
  }

  private String getPlugins(String readString) {
    Pattern APPLY_PLUGINS = Pattern.compile("\\s*apply\\s+plugin:\\s+[\"'](.+?)[\"']");

    readString = readString.replaceAll("\\s*//.*", "");
    Matcher matcher = PLUGINS_ID.matcher(readString);
    List<String> plugins = new ArrayList<>();
    while (matcher.find()) {
      String declaration = matcher.group(3);
      if (declaration != null) {
        plugins.add(String.valueOf(declaration));
      }
    }
    matcher = PLUGINS_ID_QUOT.matcher(readString);
    while (matcher.find()) {
      String declaration = matcher.group(3);
      if (declaration != null) {
        plugins.add(String.valueOf(declaration));
      }
    }
    matcher = APPLY_PLUGINS.matcher(readString);
    while (matcher.find()) {
      String declaration = matcher.group(1);
      if (declaration != null) {
        plugins.add(String.valueOf(declaration));
      }
    }
    return plugins.toString().replace("[", "").replace("]", "");
  }

  private List<String> parseImplementationProjects(File gradleFile) {
    try {
      String readString = FileUtils.readFileToString(gradleFile, Charset.defaultCharset());
      return parseImplementationProjects(readString);
    } catch (IOException e) {
    }
    return null;
  }

  public static List<String> parseImplementationProjects(String readString) throws IOException {
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
}
