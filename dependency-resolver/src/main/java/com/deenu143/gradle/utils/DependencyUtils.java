package com.deenu143.gradle.utils;

import com.tyron.builder.log.ILogger;
import com.tyron.resolver.ScopeType;
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

  // parseDependencies
  public static List<Dependency> parseDependencies(
      RepositoryManager repositoryManager, String readString, ILogger logger, ScopeType scopeType)
      throws IOException {
    final Pattern DEPENDENCIES =
        Pattern.compile(
            "\\s*(" + scopeType.getStringValue() + ")\\s*([\"'])([a-zA-Z0-9.'/-:\\-]+)\\2");
    final Pattern DEPENDENCIES_QUOTE =
        Pattern.compile("\\s*(" + scopeType.getStringValue() + ")\\s*\\([\"']([^'\"]+)[\"']\\)");

    readString = readString.replaceAll("\\s*//.*", "");
    List<Dependency> dependencies = new ArrayList<>();
    Matcher matcher = DEPENDENCIES.matcher(readString);
    while (matcher.find()) {
      String declaration = matcher.group(3);
      if (declaration != null) {
        try {
          dependencies.add(Dependency.valueOf(declaration));
        } catch (Exception e) {
          logger.warning("Failed to add dependency " + declaration);
        }
      }
    }

    matcher = DEPENDENCIES_QUOTE.matcher(readString);
    while (matcher.find()) {
      String declaration = matcher.group(2);
      if (declaration != null) {
        try {
          dependencies.add(Dependency.valueOf(declaration));
        } catch (Exception e) {
          logger.warning("Failed to add dependency " + declaration);
        }
      }
    }

    return dependencies;
  }

  public static List<Dependency> parseDependencies(
      RepositoryManager repositoryManager, File file, ILogger logger, ScopeType scopeType)
      throws IOException {
    String readString = FileUtils.readFileToString(file, Charset.defaultCharset());
    return parseDependencies(repositoryManager, readString, logger, scopeType);
  }
}
