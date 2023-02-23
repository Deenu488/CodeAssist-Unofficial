package com.deenu143.gradle.utils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.io.FileUtils;

public class GradleUtils {

  private static final Pattern PLUGINS_ID =
      Pattern.compile("\\s*(id)\\s*(')([a-zA-Z0-9.'/-:\\-]+)(')");
  private static final Pattern PLUGINS_ID_QUOT =
      Pattern.compile("\\s*(id)\\s*(\")([a-zA-Z0-9.'/-:\\-]+)(\")");
  private static final Pattern NAMESPLACE =
      Pattern.compile("\\s*(namespace)\\s*(')([a-zA-Z0-9.'/-:\\-]+)(')");
  private static final Pattern NAMESPLACE_QUOT =
      Pattern.compile("\\s*(namespace)\\s*(\")([a-zA-Z0-9.'/-:\\-]+)(\")");
  private static final Pattern APPLICATION_ID =
      Pattern.compile("\\s*(applicationId)\\s*(')([a-zA-Z0-9.'/-:\\-]+)(')");
  private static final Pattern APPLICATION_ID_QUOT =
      Pattern.compile("\\s*(applicationId)\\s*(\")([a-zA-Z0-9.'/-:\\-]+)(\")");
  private static final Pattern MIN_SDK =
      Pattern.compile("\\s*(minSdk)\\s*()([a-zA-Z0-9.'/-:\\-]+)()");
  private static final Pattern TARGET_SDK =
      Pattern.compile("\\s*(targetSdk)\\s*()([a-zA-Z0-9.'/-:\\-]+)()");
  private static final Pattern VERSION_CODE =
      Pattern.compile("\\s*(versionCode)\\s*()([a-zA-Z0-9.'/-:\\-]+)()");
  private static final Pattern VERSION_NAME =
      Pattern.compile("\\s*(versionName)\\s*(')([a-zA-Z0-9.'/-:\\-]+)(')");
  private static final Pattern VERSION_NAME_QUOT =
      Pattern.compile("\\s*(versionName)\\s*(\")([a-zA-Z0-9.'/-:\\-]+)(\")");
  private static final Pattern MINIFY_ENABLED =
      Pattern.compile("\\s*(minifyEnabled)\\s*()([a-zA-Z0-9.'/-:\\-]+)()");
  private static final Pattern SHRINK_RESOURCES =
      Pattern.compile("\\s*(shrinkResources)\\s*()([a-zA-Z0-9.'/-:\\-]+)()");
  private static final Pattern USE_LEGACY_PACKAGING =
      Pattern.compile("\\s*(useLegacyPackaging)\\s*()([a-zA-Z0-9.'/-:\\-]+)()");
  private static final Pattern IMPLEMENTATION_PROJECT =
      Pattern.compile("\\s*(implementation project)\\s*(')([a-zA-Z0-9.'/-:\\-]+)(')");
  private static final Pattern IMPLEMENTATION_PROJECT_QUOT =
      Pattern.compile("\\s*(implementation project)\\s*(\")([a-zA-Z0-9.'/-:\\-]+)(\")");
  private static final Pattern INCLUDE =
      Pattern.compile("\\s*(include)\\s*(')([a-zA-Z0-9.'/-:\\-]+)(')");
  private static final Pattern INCLUDE_QUOT =
      Pattern.compile("\\s*(include)\\s*(\")([a-zA-Z0-9.'/-:\\-]+)(\")");

  public static List<String> parsePlugins(File file) throws IOException {
    String readString = FileUtils.readFileToString(file, Charset.defaultCharset());
    return parsePlugins(readString);
  }

  public static List<String> parsePlugins(String readString) throws IOException {
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
    return plugins;
  }

  public static String parseNameSpace(File file) throws IOException {
    String readString = FileUtils.readFileToString(file, Charset.defaultCharset());
    return parseNameSpace(readString);
  }

  public static String parseNameSpace(String readString) throws IOException {
    readString = readString.replaceAll("\\s*//.*", "");
    Matcher matcher = NAMESPLACE.matcher(readString);
    while (matcher.find()) {
      String declaration = matcher.group(3);
      if (declaration != null) {
        String namespace = String.valueOf(declaration);
        return namespace;
      }
    }
    matcher = NAMESPLACE_QUOT.matcher(readString);
    while (matcher.find()) {
      String declaration = matcher.group(3);
      if (declaration != null) {
        String namespace = String.valueOf(declaration);
        return namespace;
      }
    }
    return null;
  }

  public static String parseApplicationId(File file) throws IOException {
    String readString = FileUtils.readFileToString(file, Charset.defaultCharset());
    return parseApplicationId(readString);
  }

  public static String parseApplicationId(String readString) throws IOException {
    readString = readString.replaceAll("\\s*//.*", "");
    Matcher matcher = APPLICATION_ID.matcher(readString);
    while (matcher.find()) {
      String declaration = matcher.group(3);
      if (declaration != null) {
        String applicationId = String.valueOf(declaration);
        return applicationId;
      }
    }
    matcher = APPLICATION_ID_QUOT.matcher(readString);
    while (matcher.find()) {
      String declaration = matcher.group(3);
      if (declaration != null) {
        String applicationId = String.valueOf(declaration);
        return applicationId;
      }
    }
    return null;
  }

  public static String parseMinSdk(File file) throws IOException {
    String readString = FileUtils.readFileToString(file, Charset.defaultCharset());
    return parseMinSdk(readString);
  }

  public static String parseMinSdk(String readString) throws IOException {
    Matcher matcher = MIN_SDK.matcher(readString);
    while (matcher.find()) {
      String declaration = matcher.group(3);
      if (declaration != null) {
        int minSdk = Integer.parseInt(String.valueOf(declaration));
        return String.valueOf(minSdk);
      }
    }
    return null;
  }

  public static String parseTargetSdk(File file) throws IOException {
    String readString = FileUtils.readFileToString(file, Charset.defaultCharset());
    return parseTargetSdk(readString);
  }

  public static String parseTargetSdk(String readString) throws IOException {
    Matcher matcher = TARGET_SDK.matcher(readString);
    while (matcher.find()) {
      String declaration = matcher.group(3);
      if (declaration != null) {
        int targetSdk = Integer.parseInt(String.valueOf(declaration));
        return String.valueOf(targetSdk);
      }
    }
    return null;
  }

  public static String parseVersionCode(File file) throws IOException {
    String readString = FileUtils.readFileToString(file, Charset.defaultCharset());
    return parseVersionCode(readString);
  }

  public static String parseVersionCode(String readString) throws IOException {
    Matcher matcher = VERSION_CODE.matcher(readString);
    while (matcher.find()) {
      String declaration = matcher.group(3);
      if (declaration != null) {
        int versionCode = Integer.parseInt(String.valueOf(declaration));
        return String.valueOf(versionCode);
      }
    }
    return null;
  }

  public static String parseVersionName(File file) throws IOException {
    String readString = FileUtils.readFileToString(file, Charset.defaultCharset());
    return parseVersionName(readString);
  }

  public static String parseVersionName(String readString) throws IOException {
    readString = readString.replaceAll("\\s*//.*", "");
    Matcher matcher = VERSION_NAME.matcher(readString);
    while (matcher.find()) {
      String declaration = matcher.group(3);
      if (declaration != null) {
        String versionName = String.valueOf(declaration);
        return versionName;
      }
    }
    matcher = VERSION_NAME_QUOT.matcher(readString);
    while (matcher.find()) {
      String declaration = matcher.group(3);
      if (declaration != null) {
        String versionName = String.valueOf(declaration);
        return versionName;
      }
    }
    return null;
  }

  public static String parseMinfyEnabled(File file) throws IOException {
    String readString = FileUtils.readFileToString(file, Charset.defaultCharset());
    return parseMinfyEnabled(readString);
  }

  public static String parseMinfyEnabled(String readString) throws IOException {
    Matcher matcher = MINIFY_ENABLED.matcher(readString);
    while (matcher.find()) {
      String declaration = matcher.group(3);
      if (declaration != null) {
        String minifyEnabled = String.valueOf(declaration);
        return minifyEnabled;
      }
    }
    return null;
  }

  public static String parseUseLegacyPackaging(File file) throws IOException {
    String readString = FileUtils.readFileToString(file, Charset.defaultCharset());
    return parseUseLegacyPackaging(readString);
  }

  public static String parseUseLegacyPackaging(String readString) throws IOException {
    Matcher matcher = USE_LEGACY_PACKAGING.matcher(readString);
    while (matcher.find()) {
      String declaration = matcher.group(3);
      if (declaration != null) {
        String useLegacyPackaging = String.valueOf(declaration);
        return useLegacyPackaging;
      }
    }
    return null;
  }

  public static String parseShrinkResources(File file) throws IOException {
    String readString = FileUtils.readFileToString(file, Charset.defaultCharset());
    return parseShrinkResources(readString);
  }

  public static String parseShrinkResources(String readString) throws IOException {
    Matcher matcher = SHRINK_RESOURCES.matcher(readString);
    while (matcher.find()) {
      String declaration = matcher.group(3);
      if (declaration != null) {
        String shrinkResources = String.valueOf(declaration);
        return shrinkResources;
      }
    }
    return null;
  }

  public static List<String> parseImplementationProject(File file) throws IOException {
    String readString = FileUtils.readFileToString(file, Charset.defaultCharset());
    return parseImplementationProject(readString);
  }

  public static List<String> parseImplementationProject(String readString) throws IOException {
    readString =
        readString
            .replaceAll("\\s*//.*", "")
            .replace("(", "")
            .replace(")", "")
            .replace(":", "")
            .replace("path", "");
    Matcher matcher = IMPLEMENTATION_PROJECT.matcher(readString);
    List<String> implementationProject = new ArrayList<>();
    while (matcher.find()) {
      String declaration = matcher.group(3);
      if (declaration != null) {
        implementationProject.add(String.valueOf(declaration));
      }
    }
    matcher = IMPLEMENTATION_PROJECT_QUOT.matcher(readString);
    while (matcher.find()) {
      String declaration = matcher.group(3);
      if (declaration != null) {
        implementationProject.add(String.valueOf(declaration));
      }
    }
    return implementationProject;
  }

  public static List<String> parseInclude(File file) throws IOException {
    String readString = FileUtils.readFileToString(file, Charset.defaultCharset());
    return parseInclude(readString);
  }

  public static List<String> parseInclude(String readString) throws IOException {
    readString = readString.replaceAll("\\s*//.*", "").replace(":", "");
    Matcher matcher = INCLUDE.matcher(readString);
    List<String> include = new ArrayList<>();
    while (matcher.find()) {
      String declaration = matcher.group(3);
      if (declaration != null) {
        include.add(String.valueOf(declaration));
      }
    }
    matcher = INCLUDE_QUOT.matcher(readString);
    while (matcher.find()) {
      String declaration = matcher.group(3);
      if (declaration != null) {
        include.add(String.valueOf(declaration));
      }
    }
    return include;
  }
}
