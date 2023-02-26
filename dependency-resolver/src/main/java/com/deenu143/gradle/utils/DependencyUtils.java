package com.deenu143.gradle.utils;

import com.tyron.builder.log.ILogger;
import com.tyron.resolver.model.Dependency;
import com.tyron.resolver.repository.RepositoryManager;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.io.FileUtils;

public class DependencyUtils {

  // parseImplementationDependencies
  public static List<Dependency> parseImplementationDependencies(
      RepositoryManager repository, File file, ILogger logger) throws IOException {
    String readString = FileUtils.readFileToString(file, Charset.defaultCharset());
    return parseImplementationDependencies(repository, readString, logger);
  }

  public static List<Dependency> parseImplementationDependencies(
      RepositoryManager repositoryManager, String readString, ILogger logger) throws IOException {
    final Pattern DEPENDENCIES =
        Pattern.compile("\\s*(implementation)\\s*([\"'])([a-zA-Z0-9.'/-:\\-]+)\\2");

    readString = readString.replaceAll("\\s*//.*", "");
    List<Dependency> dependencies = new ArrayList<>();
    Matcher matcher = DEPENDENCIES.matcher(readString);
    while (matcher.find()) {
      String declaration = matcher.group(3);
      if (declaration != null) {
        try {
          dependencies.add(Dependency.valueOf(declaration));
        } catch (IllegalArgumentException e) {
          logger.warning("Failed to add dependency " + e.getMessage());
        }
      }
    }
    return dependencies;
  }

  // parseApiDependencies
  public static List<Dependency> parseApiDependencies(
      RepositoryManager repository, File file, ILogger logger) throws IOException {
    String readString = FileUtils.readFileToString(file, Charset.defaultCharset());
    return parseApiDependencies(repository, readString, logger);
  }

  public static List<Dependency> parseApiDependencies(
      RepositoryManager repositoryManager, String readString, ILogger logger) throws IOException {
    final Pattern DEPENDENCIES = Pattern.compile("\\s*(api)\\s*([\"'])([a-zA-Z0-9.'/-:\\-]+)\\2");

    readString = readString.replaceAll("\\s*//.*", "");
    List<Dependency> dependencies = new ArrayList<>();
    Matcher matcher = DEPENDENCIES.matcher(readString);
    while (matcher.find()) {
      String declaration = matcher.group(3);
      if (declaration != null) {
        try {
          dependencies.add(Dependency.valueOf(declaration));
        } catch (IllegalArgumentException e) {
          logger.warning("Failed to add dependency " + e.getMessage());
        }
      }
    }
    return dependencies;
  }

  // parseCompileOnlyDependencies
  public static List<Dependency> parseCompileOnlyDependencies(
      RepositoryManager repository, File file, ILogger logger) throws IOException {
    String readString = FileUtils.readFileToString(file, Charset.defaultCharset());
    return parseCompileOnlyDependencies(repository, readString, logger);
  }

  public static List<Dependency> parseCompileOnlyDependencies(
      RepositoryManager repositoryManager, String readString, ILogger logger) throws IOException {
    final Pattern DEPENDENCIES =
        Pattern.compile("\\s*(compileOnly)\\s*([\"'])([a-zA-Z0-9.'/-:\\-]+)\\2");

    readString = readString.replaceAll("\\s*//.*", "");
    List<Dependency> dependencies = new ArrayList<>();
    Matcher matcher = DEPENDENCIES.matcher(readString);
    while (matcher.find()) {
      String declaration = matcher.group(3);
      if (declaration != null) {
        try {
          dependencies.add(Dependency.valueOf(declaration));
        } catch (IllegalArgumentException e) {
          logger.warning("Failed to add dependency " + e.getMessage());
        }
      }
    }
    return dependencies;
  }

  // parseRuntimeOnlyDependencies
  public static List<Dependency> parseRuntimeOnlyDependencies(
      RepositoryManager repository, File file, ILogger logger) throws IOException {
    String readString = FileUtils.readFileToString(file, Charset.defaultCharset());
    return parseRuntimeOnlyDependencies(repository, readString, logger);
  }

  public static List<Dependency> parseRuntimeOnlyDependencies(
      RepositoryManager repositoryManager, String readString, ILogger logger) throws IOException {
    final Pattern DEPENDENCIES =
        Pattern.compile("\\s*(runtimeOnly)\\s*([\"'])([a-zA-Z0-9.'/-:\\-]+)\\2");

    readString = readString.replaceAll("\\s*//.*", "");
    List<Dependency> dependencies = new ArrayList<>();
    Matcher matcher = DEPENDENCIES.matcher(readString);
    while (matcher.find()) {
      String declaration = matcher.group(3);
      if (declaration != null) {
        try {
          dependencies.add(Dependency.valueOf(declaration));
        } catch (IllegalArgumentException e) {
          logger.warning("Failed to add dependency " + e.getMessage());
        }
      }
    }
    return dependencies;
  }
}
