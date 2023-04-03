package com.tyron.builder.project.mock;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.common.collect.ImmutableMap;
import com.tyron.builder.compiler.manifest.xml.AndroidManifestParser;
import com.tyron.builder.compiler.manifest.xml.ManifestData;
import com.tyron.builder.model.ModuleSettings;
import com.tyron.builder.project.api.AndroidModule;
import com.tyron.builder.project.api.FileManager;
import com.tyron.common.util.StringSearch;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.io.FileUtils;

public class MockAndroidModule extends MockJavaModule implements AndroidModule {

  private final Map<String, File> mKotlinFiles = new HashMap<>();

  private int mTargetSdk = 31;
  private int mMinSdk = 21;

  private ManifestData mManifestData;
  private ManifestData manifestData;
  private String mPackageName;

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

    if (getManifestFile().exists()) {
      mManifestData = AndroidManifestParser.parse(getManifestFile());
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

  public void setPackageName(@NonNull String name) {
    mPackageName = name;
  }

  @Override
  public String getPackageName() {
    if (mPackageName != null) {
      return mPackageName;
    }

    if (mManifestData == null) {
      throw new IllegalStateException("Project is not yet opened");
    }
    return mManifestData.getPackage();
  }

  @Override
  public String getPackageName(File manifest) {
    try {
      if (manifest.exists()) {
        manifestData = AndroidManifestParser.parse(manifest);
      }
    } catch (IOException e) {
    }

    if (manifestData == null) {
      throw new IllegalStateException("Project is not yet opened");
    }
    return manifestData.getPackage();
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
        return minifyEnabled;
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
    return mTargetSdk;
  }

  public void setTargetSdk(int targetSdk) {
    mTargetSdk = targetSdk;
  }

  @Override
  public int getMinSdk() {
    return mMinSdk;
  }

  @Override
  public Map<String, File> getResourceClasses() {
    return new HashMap<>();
  }

  @Override
  public void addResourceClass(@NonNull File file) {}

  public void setMinSdk(int min) {
    mMinSdk = min;
  }

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
