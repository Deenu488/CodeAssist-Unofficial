package com.tyron.builder.project.api;

import com.tyron.builder.model.ModuleSettings;
import com.tyron.builder.project.cache.CacheHolder;
import java.io.File;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.kotlin.com.intellij.openapi.util.UserDataHolderEx;

public interface Module extends UserDataHolderEx, CacheHolder {

  ModuleSettings getSettings();

  FileManager getFileManager();

  File getRootFile();

  File getGradleFile();

  File getSettingsGradleFile();

  File getProjectDir();

  int getMinSdk();

  String getMainClass();

  List<String> getPlugins();

  List<String> getPlugins(File file);

  List<String> getAllProjects();

  List<String> getAllProjects(File file);

  List<String> getIncludedProjects();

  List<AbstractMap.SimpleEntry<String, ArrayList<String>>> extractDirAndIncludes(String scope);

  List<AbstractMap.SimpleEntry<String, ArrayList<String>>> extractDirAndIncludes(
      File file, String scope);

  AbstractMap.SimpleEntry<ArrayList<String>, ArrayList<String>> extractListDirAndIncludes(
      String scope);

  AbstractMap.SimpleEntry<ArrayList<String>, ArrayList<String>> extractListDirAndIncludes(
      File file, String scope);

  default String getName() {
    return getRootFile().getName();
  }

  /**
   * Start parsing the project contents such as manifest data, project settings, etc.
   *
   * <p>Implementations may throw an IOException if something went wrong during parsing
   */
  void open() throws IOException;

  /** Remove all the indexed files */
  void clear();

  void index();

  /**
   * @return The directory that this project can use to compile files
   */
  File getBuildDirectory();

  File getBuildClassesDirectory();
}
