package com.tyron.builder.compiler.manifest;

import com.tyron.builder.compiler.BuildType;
import com.tyron.builder.compiler.Task;
import com.tyron.builder.compiler.manifest.ManifestMerger2.SystemProperty;
import com.tyron.builder.compiler.manifest.xml.XmlFormatPreferences;
import com.tyron.builder.compiler.manifest.xml.XmlFormatStyle;
import com.tyron.builder.compiler.manifest.xml.XmlPrettyPrinter;
import com.tyron.builder.exception.CompilationFailedException;
import com.tyron.builder.log.ILogger;
import com.tyron.builder.project.Project;
import com.tyron.builder.project.api.AndroidModule;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.w3c.dom.Document;

public class ManifestMergeTask extends Task<AndroidModule> {

  private File mOutputFile;
  private File mMainManifest;
  private File[] mLibraryManifestFiles;
  private String mPackageName;

  public ManifestMergeTask(Project project, AndroidModule module, ILogger logger) {
    super(project, module, logger);
  }

  @Override
  public String getName() {
    return "mergeManifest";
  }

  @Override
  public void prepare(BuildType type) throws IOException {
    mPackageName = getApplicationId();

    mOutputFile = new File(getModule().getBuildDirectory(), "bin");
    if (!mOutputFile.exists()) {
      if (!mOutputFile.mkdirs()) {
        throw new IOException("Unable to create build directory");
      }
    }
    mOutputFile = new File(mOutputFile, "AndroidManifest.xml");
    if (!mOutputFile.exists()) {
      if (!mOutputFile.createNewFile()) {
        throw new IOException("Unable to create manifest file");
      }
    }

    mMainManifest = getModule().getManifestFile();
    if (!mMainManifest.exists()) {
      throw new IOException("Unable to find the main manifest file");
    }

    List<File> manifests = new ArrayList<>();
    List<File> libraries = getModule().getLibraries();

    // Filter the libraries and add all that has a AndroidManifest.xml file
    for (File library : libraries) {
      File parent = library.getParentFile();
      if (parent == null) {
        getLogger().warning("Unable to access parent directory of a library");
        continue;
      }

      File manifest = new File(parent, "AndroidManifest.xml");
      if (manifest.exists()) {
        if (manifest.length() != 0) {
          manifests.add(manifest);
        }
      }
    }

    mLibraryManifestFiles = manifests.toArray(new File[0]);
  }

  @Override
  public void run() throws IOException, CompilationFailedException {

    ManifestMerger2.Invoker<?> invoker =
        ManifestMerger2.newMerger(
            mMainManifest, getLogger(), ManifestMerger2.MergeType.APPLICATION);
    invoker.setOverride(SystemProperty.PACKAGE, mPackageName);
    invoker.setOverride(SystemProperty.MIN_SDK_VERSION, String.valueOf(getModule().getMinSdk()));
    invoker.setOverride(
        SystemProperty.TARGET_SDK_VERSION, String.valueOf(getModule().getTargetSdk()));
    invoker.setOverride(SystemProperty.VERSION_CODE, String.valueOf(getModule().getVersionCode()));
    invoker.setOverride(SystemProperty.VERSION_NAME, getModule().getVersionName());
    if (mLibraryManifestFiles != null) {
      invoker.addLibraryManifests(mLibraryManifestFiles);
    }
    invoker.setVerbose(false);
    try {
      MergingReport report = invoker.merge();
      if (report.getResult().isError()) {
        report.log(getLogger());
        throw new CompilationFailedException(report.getReportString());
      }
      if (report.getMergedDocument().isPresent()) {
        Document document = report.getMergedDocument().get().getXml();
        // inject the tools namespace, some libraries may use the tools attribute but
        // the main manifest may not have it defined
        document
            .getDocumentElement()
            .setAttribute(
                SdkConstants.XMLNS_PREFIX + SdkConstants.TOOLS_PREFIX, SdkConstants.TOOLS_URI);
        String contents =
            XmlPrettyPrinter.prettyPrint(
                document,
                XmlFormatPreferences.defaults(),
                XmlFormatStyle.get(document),
                null,
                false);
        FileUtils.writeStringToFile(mOutputFile, contents, Charset.defaultCharset());
      }
    } catch (ManifestMerger2.MergeFailureException e) {
      throw new CompilationFailedException(e);
    }
  }

  public void merge(File root, File gradle) throws IOException, CompilationFailedException {
    getLogger().debug("> Task :" + root.getName() + ":" + "mergeManifest");
    String namespace = namespace(root.getName(), gradle);
    File outputFile = new File(root, "build/bin/AndroidManifest.xml");
    File mainManifest = new File(root, "src/main/AndroidManifest.xml");
    if (!outputFile.getParentFile().exists()) {
      outputFile.getParentFile().mkdirs();
    }

    if (!mainManifest.getParentFile().exists()) {
      throw new IOException("Unable to find the main manifest file");
    }

    int minSdk = getModule().getMinSdk(gradle);
    int targetSdk = getModule().getTargetSdk(gradle);
    int versionCode = getModule().getVersionCode(gradle);
    String versionName = getModule().getVersionName(gradle);
    merge(mainManifest, namespace, minSdk, targetSdk, versionCode, versionName, outputFile);
  }

  public void merge(
      File mainManifest,
      String namespace,
      int minSdk,
      int targetSdk,
      int versionCode,
      String versionName,
      File outputFile)
      throws IOException, CompilationFailedException {

    ManifestMerger2.Invoker<?> invoker =
        ManifestMerger2.newMerger(mainManifest, getLogger(), ManifestMerger2.MergeType.LIBRARY);
    invoker.setOverride(SystemProperty.PACKAGE, namespace);
    invoker.setOverride(SystemProperty.MIN_SDK_VERSION, String.valueOf(minSdk));
    invoker.setOverride(SystemProperty.TARGET_SDK_VERSION, String.valueOf(targetSdk));
    invoker.setOverride(SystemProperty.VERSION_CODE, String.valueOf(versionCode));
    invoker.setOverride(SystemProperty.VERSION_NAME, versionName);
    invoker.setVerbose(false);
    try {
      MergingReport report = invoker.merge();
      if (report.getResult().isError()) {
        report.log(getLogger());
        throw new CompilationFailedException(report.getReportString());
      }
      if (report.getMergedDocument().isPresent()) {
        Document document = report.getMergedDocument().get().getXml();
        // inject the tools namespace, some libraries may use the tools attribute but
        // the main manifest may not have it defined
        String contents =
            XmlPrettyPrinter.prettyPrint(
                document,
                XmlFormatPreferences.defaults(),
                XmlFormatStyle.get(document),
                null,
                false);
        FileUtils.writeStringToFile(outputFile, contents, Charset.defaultCharset());
      }
    } catch (ManifestMerger2.MergeFailureException e) {
      throw new CompilationFailedException(e);
    }
  }

  private String getApplicationId() throws IOException {
    String packageName = getModule().getNameSpace();
    String content = parseString(getModule().getGradleFile());

    if (content != null) {
      boolean isAndroidLibrary = false;
      if (content.contains("com.android.library")) {
        isAndroidLibrary = true;
      }

      if (isAndroidLibrary) {
        return packageName;
      } else {

        if (content.contains("namespace") && !content.contains("applicationId")) {
          throw new IOException(
              "Unable to find applicationId in "
                  + getModule().getRootFile().getName()
                  + "/build.gradle file");

        } else if (content.contains("applicationId") && content.contains("namespace")) {
          return packageName;
        } else if (content.contains("applicationId") && !content.contains("namespace")) {
          packageName = getModule().getApplicationId();
        } else {
          throw new IOException(
              "Unable to find namespace or applicationId in "
                  + getModule().getRootFile().getName()
                  + "/build.gradle file");
        }
      }
    } else {
      throw new IOException(
          "Unable to read " + getModule().getRootFile().getName() + "/build.gradle file");
    }

    return packageName;
  }

  private String parseString(File gradle) {
    if (gradle != null && gradle.exists()) {
      try {
        String readString = FileUtils.readFileToString(gradle, Charset.defaultCharset());
        if (readString != null && !readString.isEmpty()) {
          return readString;
        }
      } catch (IOException e) {
        // handle the exception here, if needed
      }
    }
    return null;
  }

  private String namespace(String root, File gradle) throws IOException {
    String packageName = getModule().getNameSpace(gradle);
    String content = parseString(gradle);

    if (content != null) {
      if (!content.contains("namespace")) {
        throw new IOException("Unable to find namespace in " + root + "/build.gradle file");
      }
    } else {
      throw new IOException("Unable to read " + root + "/build.gradle file");
    }
    return packageName;
  }
}
