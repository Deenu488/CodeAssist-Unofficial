package com.tyron.builder.project.mock;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.common.collect.ImmutableMap;
import com.tyron.builder.model.ModuleSettings;
import com.tyron.builder.project.api.AndroidModule;
import com.tyron.builder.project.api.FileManager;
import com.tyron.common.util.StringSearch;
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

public class MockAndroidModule extends MockJavaModule implements AndroidModule {

  private final Map<String, File> mKotlinFiles = new HashMap<>();

  private File mAndroidResourcesDir;

  private final ModuleSettings mockSettings = new MockModuleSettings();

  public MockAndroidModule(File rootDir, FileManager fileManager) {
    super(rootDir, fileManager);
  }

  @Override
  public ModuleSettings getSettings() {
    return mockSettings;
  }

  @Override
  public void open() throws IOException {
    super.open();

    File gradle = getGradleFile();
    if (!gradle.exists()) {
      throw new IOException("Unable to open build.gradle file");
    }
  }

  @Override
  public void index() {}

  public void setAndroidResourcesDirectory(File dir) {
    mAndroidResourcesDir = dir;
  }

  @Override
  public File getAndroidResourcesDirectory() {
    if (mAndroidResourcesDir != null) {
      return mAndroidResourcesDir;
    }
    return new File(getRootFile(), "src/main/res");
  }

  @Override
  public File getNativeLibrariesDirectory() {
    return new File(getRootFile(), "src/main/jniLibs");
  }

  @Override
  public File getAssetsDirectory() {
    return new File(getRootFile(), "src/main/assets");
  }

  public void setPackageName(@NonNull String name) {}

  @Override
  public String getNameSpace() {
    return parseNameSpace(getGradleFile());
  }

  private String parseNameSpace(File gradle) {
    if (gradle != null && gradle.exists()) {
      try {
        String readString = FileUtils.readFileToString(gradle, Charset.defaultCharset());
        return parseNameSpace(readString);
      } catch (IOException e) {
        // handle the exception here, if needed
      }
    }
    return null;
  }

  @Override
  public String getApplicationId() {
    return parseApplicationId(getGradleFile());
  }

  private String parseApplicationId(File gradle) {
    if (gradle != null && gradle.exists()) {
      try {
        String readString = FileUtils.readFileToString(gradle, Charset.defaultCharset());
        return parseApplicationId(readString);
      } catch (IOException e) {
        // handle the exception here, if needed
      }
    }
    return null;
  }

  public String parseNameSpace(String readString) throws IOException {
    Pattern NAMESPLACE = Pattern.compile("\\s*(namespace)\\s*([\"'])([a-zA-Z0-9._'/\\\\:-]+)\\2");
    readString = readString.replaceAll("\\s*//.*", "");
    Matcher matcher = NAMESPLACE.matcher(readString);
    while (matcher.find()) {
      String declaration = matcher.group(3);
      if (declaration != null) {
        String namespace = String.valueOf(declaration);
        if (namespace != null && !namespace.isEmpty()) {
          return namespace;
        }
      }
    }
    return null;
  }

  public String parseApplicationId(String readString) throws IOException {
    Pattern APPLICATION_ID = Pattern.compile("\\s*(applicationId)\\s*(')([a-zA-Z0-9.'/-:\\-]+)(')");
    Pattern APPLICATION_ID_QUOT =
        Pattern.compile("\\s*(applicationId)\\s*(\")([a-zA-Z0-9.'/-:\\-]+)(\")");

    readString = readString.replaceAll("\\s*//.*", "");

    Matcher matcher = APPLICATION_ID.matcher(readString);
    while (matcher.find()) {
      String declaration = matcher.group(3);
      if (declaration != null) {
        String applicationId = String.valueOf(declaration);
        if (applicationId != null && !applicationId.isEmpty()) {
          return applicationId;
        }
      }
    }
    matcher = APPLICATION_ID_QUOT.matcher(readString);
    while (matcher.find()) {
      String declaration = matcher.group(3);
      if (declaration != null) {
        String applicationId = String.valueOf(declaration);
        if (applicationId != null && !applicationId.isEmpty()) {
          return applicationId;
        }
      }
    }
    return null;
  }

  @Override
  public String getNameSpace(File gradle) {
    return parseNameSpace(gradle);
  }

  @Override
  public String getApplicationId(File gradle) {
    return parseApplicationId(gradle);
  }

  @Override
  public boolean getViewBindingEnabled() {
    return parseViewBindingEnabled(getGradleFile());
  }

  @Override
  public boolean getViewBindingEnabled(File file) {
    return parseViewBindingEnabled(file);
  }

  private boolean parseViewBindingEnabled(File gradle) {
    if (gradle != null && gradle.exists()) {
      try {
        String readString = FileUtils.readFileToString(gradle, Charset.defaultCharset());
        return parseViewBindingEnabled(readString);
      } catch (IOException e) {
        // handle the exception here, if needed
      }
    }
    return false;
  }

  private boolean parseViewBindingEnabled(String readString) throws IOException {
    Pattern VIEW_BINDING_ENABLED =
        Pattern.compile("\\s*(viewBinding)\\s*()([a-zA-Z0-9.'/-:\\-]+)()");
    Matcher matcher = VIEW_BINDING_ENABLED.matcher(readString);
    while (matcher.find()) {
      String declaration = matcher.group(3);
      if (declaration != null && !declaration.isEmpty()) {
        boolean minifyEnabled = Boolean.parseBoolean(String.valueOf(declaration));
        if (minifyEnabled) {
          return true;
        } else {
          return false;
        }
      }
    }
    return false;
  }

  @Override
  public File getManifestFile() {
    return new File(getRootFile(), "src/main/AndroidManifest.xml");
  }

  @Override
  public int getTargetSdk() {
    return parseTargetSdk(getGradleFile());
  }

  @Override
  public int getTargetSdk(File gradle) {
    return parseTargetSdk(gradle);
  }

  private int parseTargetSdk(File gradle) {
    if (gradle != null && gradle.exists()) {
      try {
        String readString = FileUtils.readFileToString(gradle, Charset.defaultCharset());
        return parseTargetSdk(readString);
      } catch (IOException e) {
        // handle the exception here, if needed
      }
    }
    return 33;
  }

  public static int parseTargetSdk(String readString) throws IOException {
    Pattern TARGET_SDK = Pattern.compile("\\s*(targetSdk)\\s*()([a-zA-Z0-9.'/-:\\-]+)()");
    Pattern TARGET_SDK_VERSION =
        Pattern.compile("\\s*(targetSdkVersion)\\s*()([a-zA-Z0-9.'/-:\\-]+)()");

    Matcher matcher = TARGET_SDK.matcher(readString);
    while (matcher.find()) {
      String declaration = matcher.group(3);
      if (declaration != null & !declaration.isEmpty()) {

        try {
          int targetSdk = Integer.parseInt(String.valueOf(declaration));
          return targetSdk;
        } catch (NumberFormatException e) {
          // Handle the exception here, such as logging an error or returning a default value
          e.printStackTrace();
        }
      }
    }
    matcher = TARGET_SDK_VERSION.matcher(readString);
    while (matcher.find()) {
      String declaration = matcher.group(3);
      if (declaration != null & !declaration.isEmpty()) {

        try {
          int targetSdk = Integer.parseInt(String.valueOf(declaration));
          return targetSdk;
        } catch (NumberFormatException e) {
          // Handle the exception here, such as logging an error or returning a default value
          e.printStackTrace();
        }
      }
    }
    return 33;
  }

  @Override
  public int getMinSdk() {
    return parseMinSdk(getGradleFile());
  }

  @Override
  public int getMinSdk(File gradle) {
    return parseMinSdk(gradle);
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
    Pattern MIN_SDK_VERSION = Pattern.compile("\\s*(minSdkVersion)\\s*()([a-zA-Z0-9.'/-:\\-]+)()");
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
    matcher = MIN_SDK_VERSION.matcher(readString);
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

  @Override
  public int getVersionCode() {
    return parseVersionCode(getGradleFile());
  }

  @Override
  public int getVersionCode(File gradle) {
    return parseVersionCode(gradle);
  }

  private int parseVersionCode(File gradle) {
    if (gradle != null && gradle.exists()) {
      try {
        String readString = FileUtils.readFileToString(gradle, Charset.defaultCharset());
        return parseVersionCode(readString);
      } catch (IOException e) {
        // handle the exception here, if needed
      }
    }
    return 1;
  }

  public static int parseVersionCode(String readString) throws IOException {
    Pattern VERSION_CODE = Pattern.compile("\\s*(versionCode)\\s*()([a-zA-Z0-9.'/-:\\-]+)()");
    Matcher matcher = VERSION_CODE.matcher(readString);
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
    return 1;
  }

  @Override
  public String getVersionName() {
    return parseVersionName(getGradleFile());
  }

  @Override
  public String getVersionName(File gradle) {
    return parseVersionName(gradle);
  }

  private String parseVersionName(File gradle) {
    if (gradle != null && gradle.exists()) {
      try {
        String readString = FileUtils.readFileToString(gradle, Charset.defaultCharset());
        return parseVersionName(readString);
      } catch (IOException e) {
        // handle the exception here, if needed
      }
    }
    return "1.0";
  }

  public static String parseVersionName(String readString) throws IOException {
    Pattern VERSION_NAME = Pattern.compile("\\s*(versionName)\\s*(')([a-zA-Z0-9.'/-:\\-]+)(')");
    Pattern VERSION_NAME_QUOT =
        Pattern.compile("\\s*(versionName)\\s*(\")([a-zA-Z0-9.'/-:\\-]+)(\")");

    readString = readString.replaceAll("\\s*//.*", "");
    Matcher matcher = VERSION_NAME.matcher(readString);
    while (matcher.find()) {
      String declaration = matcher.group(3);
      if (declaration != null && !declaration.isEmpty()) {
        String versionName = String.valueOf(declaration);
        if (versionName != null && !versionName.isEmpty()) {
          return versionName;
        }
      }
    }
    matcher = VERSION_NAME_QUOT.matcher(readString);
    while (matcher.find()) {
      String declaration = matcher.group(3);
      if (declaration != null && !declaration.isEmpty()) {
        String versionName = String.valueOf(declaration);
        if (versionName != null && !versionName.isEmpty()) {
          return versionName;
        }
      }
    }
    return "1.0";
  }

  @Override
  public boolean getMinifyEnabled() {
    return parseMinifyEnabled(getGradleFile());
  }

  @Override
  public boolean getMinifyEnabled(File file) {
    return parseMinifyEnabled(file);
  }

  private boolean parseMinifyEnabled(File gradle) {
    if (gradle != null && gradle.exists()) {
      try {
        String readString = FileUtils.readFileToString(gradle, Charset.defaultCharset());
        return parseMinifyEnabled(readString);
      } catch (IOException e) {
        // handle the exception here, if needed
      }
    }
    return false;
  }

  private boolean parseMinifyEnabled(String readString) throws IOException {
    Pattern MINIFY_ENABLED = Pattern.compile("\\s*(minifyEnabled)\\s*()([a-zA-Z0-9.'/-:\\-]+)()");
    Matcher matcher = MINIFY_ENABLED.matcher(readString);
    while (matcher.find()) {
      String declaration = matcher.group(3);
      if (declaration != null && !declaration.isEmpty()) {
        boolean minifyEnabled = Boolean.parseBoolean(String.valueOf(declaration));
        if (minifyEnabled) {
          return true;
        } else {
          return false;
        }
      }
    }
    return false;
  }

  @Override
  public boolean getZipAlignEnabled() {
    return parseZipAlignEnabled(getGradleFile());
  }

  private boolean parseZipAlignEnabled(File gradle) {
    if (gradle != null && gradle.exists()) {
      try {
        String readString = FileUtils.readFileToString(gradle, Charset.defaultCharset());
        return parseZipAlignEnabled(readString);
      } catch (IOException e) {
        // handle the exception here, if needed
      }
    }
    return false;
  }

  private boolean parseZipAlignEnabled(String readString) throws IOException {
    Pattern ZIP_ALIGN_ENABLED =
        Pattern.compile("\\s*(zipAlignEnabled)\\s*()([a-zA-Z0-9.'/-:\\-]+)()");
    Matcher matcher = ZIP_ALIGN_ENABLED.matcher(readString);
    while (matcher.find()) {
      String declaration = matcher.group(3);
      if (declaration != null && !declaration.isEmpty()) {
        boolean zipAlignEnabled = Boolean.parseBoolean(String.valueOf(declaration));
        if (zipAlignEnabled) {
          return true;
        } else {
          return false;
        }
      }
    }
    return false;
  }

  @Override
  public boolean getUseLegacyPackaging() {
    return parseUseLegacyPackaging(getGradleFile());
  }

  private boolean parseUseLegacyPackaging(File gradle) {
    if (gradle != null && gradle.exists()) {
      try {
        String readString = FileUtils.readFileToString(gradle, Charset.defaultCharset());
        return parseUseLegacyPackaging(readString);
      } catch (IOException e) {
        // handle the exception here, if needed
      }
    }
    return false;
  }

  private boolean parseUseLegacyPackaging(String readString) throws IOException {
    Pattern USE_LEGACY_PACKAGING =
        Pattern.compile("\\s*(useLegacyPackaging)\\s*()([a-zA-Z0-9.'/-:\\-]+)()");
    Matcher matcher = USE_LEGACY_PACKAGING.matcher(readString);
    while (matcher.find()) {
      String declaration = matcher.group(3);
      if (declaration != null && !declaration.isEmpty()) {
        boolean useLegacyPackaging = Boolean.parseBoolean(String.valueOf(declaration));
        if (useLegacyPackaging) {
          return true;
        } else {
          return false;
        }
      }
    }
    return false;
  }

  @Override
  public List<String> getExcludes() {
    return parseExcludes(getGradleFile());
  }

  private List<String> parseExcludes(File gradle) {
    if (gradle != null && gradle.exists()) {

      try {
        String readString = FileUtils.readFileToString(gradle, Charset.defaultCharset());
        return parseExcludes(readString);
      } catch (IOException e) {
      }
    }
    return null;
  }

  private List<String> parseExcludes(String readString) throws IOException {
    List<String> excludes = new ArrayList<>();
    Matcher matcher =
        Pattern.compile("resources\\.excludes\\s*\\+?=\\s*\\[(.*)\\]").matcher(readString);
    if (matcher.find()) {
      String[] exclusions = matcher.group(1).split(",");
      for (String declaration : exclusions) {
        if (declaration != null && !declaration.isEmpty()) {
          excludes.add(declaration.trim());
        }
      }
    }

    // Match file exclusions
    matcher =
        Pattern.compile("(exclude|exclude\\s+group:)\\s+[\"']([^\"']*)[\"']").matcher(readString);
    while (matcher.find()) {
      String declaration = matcher.group(3);
      if (declaration != null && !declaration.isEmpty()) {
        excludes.add(declaration);
      }
    }
    return excludes;
  }

  @Override
  public HashMap<String, String> getSigningConfigs() {
    return parseSigningConfigs(getGradleFile());
  }

  private HashMap<String, String> parseSigningConfigs(File gradle) {
    if (gradle != null && gradle.exists()) {
      try {
        String readString = FileUtils.readFileToString(gradle, Charset.defaultCharset());
        return parseSigningConfigs(readString);
      } catch (IOException e) {
      }
    }
    return null;
  }

  private HashMap<String, String> parseSigningConfigs(String readString) {
    Pattern pattern =
        Pattern.compile(
            "storeFile\\s*file\\s*\\(['\"](.*)['\"]\\)\\s*keyAlias\\s*['\"](.*)['\"]\\s*storePassword\\s*['\"](.*)['\"]\\s*keyPassword\\s*['\"](.*)['\"]");
    readString = readString.replaceAll("\\s*//.*", "");
    Matcher matcher = pattern.matcher(readString);
    HashMap<String, String> signingConfigs = new HashMap<>();

    if (matcher.find()) {

      String storeFile = matcher.group(1);
      String keyAlias = matcher.group(2);
      String storePassword = matcher.group(3);
      String keyPassword = matcher.group(4);

      if (storeFile != null && !storeFile.isEmpty()) {
        signingConfigs.put("storeFile", storeFile);
      } else {
        signingConfigs.put("storeFile", "");
      }
      if (keyAlias != null && !keyAlias.isEmpty()) {
        signingConfigs.put("keyAlias", keyAlias);
      } else {
        signingConfigs.put("keyAlias", "");
      }
      if (storePassword != null && !storePassword.isEmpty()) {
        signingConfigs.put("storePassword", storePassword);
      } else {
        signingConfigs.put("storePassword", "");
      }
      if (keyPassword != null && !keyPassword.isEmpty()) {
        signingConfigs.put("keyPassword", keyPassword);
      } else {
        signingConfigs.put("keyPassword", "");
      }
    }
    return signingConfigs;
  }

  @Override
  public Map<String, File> getResourceClasses() {
    return new HashMap<>();
  }

  @Override
  public void addResourceClass(@NonNull File file) {}

  @NonNull
  @Override
  public Map<String, File> getKotlinFiles() {
    return ImmutableMap.copyOf(mKotlinFiles);
  }

  @NonNull
  @Override
  public File getKotlinDirectory() {
    return new File(getRootFile(), "src/main/kotlin");
  }

  @Nullable
  @Override
  public File getKotlinFile(String packageName) {
    return mKotlinFiles.get(packageName);
  }

  @Override
  public void addKotlinFile(File file) {
    mKotlinFiles.put(StringSearch.packageName(file), file);
  }

  @Override
  public Map<String, File> getInjectedClasses() {
    return null;
  }

  @Override
  public void addInjectedClass(@NonNull File file) {}
}
