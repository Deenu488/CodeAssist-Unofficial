package com.tyron.builder.compiler.buildconfig;

import android.util.Log;
import com.tyron.builder.compiler.BuildType;
import com.tyron.builder.compiler.Task;
import com.tyron.builder.exception.CompilationFailedException;
import com.tyron.builder.log.ILogger;
import com.tyron.builder.model.ModuleSettings;
import com.tyron.builder.project.Project;
import com.tyron.builder.project.api.AndroidModule;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import org.apache.commons.io.FileUtils;

public class GenerateReleaseBuildConfigTask extends Task<AndroidModule> {

  private static final String TAG = "generateReleaseBuildConfig";

  public GenerateReleaseBuildConfigTask(Project project, AndroidModule module, ILogger logger) {
    super(project, module, logger);
  }

  @Override
  public String getName() {
    return TAG;
  }

  @Override
  public void prepare(BuildType type) throws IOException {
    getModule().getJavaFiles();
    getModule().getKotlinFiles();
  }

  @Override
  public void run() throws IOException, CompilationFailedException {
    GenerateBuildConfig();
  }

  private void GenerateBuildConfig() throws IOException {
    Log.d(TAG, "Generating BuildConfig.java");
	  String packageName = getModule().getNameSpace();
    if (packageName == null) {
      throw new IOException("Unable to find namespace in build.gradle file");
    }
    File packageDir =
        new File(
            getModule().getBuildDirectory() + "/gen", getModule().getNameSpace().replace('.', '/'));
    File buildConfigClass = new File(packageDir, "/BuildConfig.java");
    if (packageDir.exists()) {
    } else {
      packageDir.mkdirs();
    }

    if (!buildConfigClass.exists() && !buildConfigClass.createNewFile()) {
      throw new IOException("Unable to generate BuildConfig.java");
    }

    String buildConfigString =
        "/**"
            + "\n"
            + "* Automatically generated file. DO NOT MODIFY"
            + "\n"
            + "*/"
            + "\n"
            + "package "
            + getModule().getNameSpace()
            + ";\n"
            + "\n"
            + "public final class BuildConfig {"
            + "\n"
            + "    public static final boolean DEBUG = "
            + "false"
            + ";\n"
            + "    public static final String APPLICATION_ID = "
            + "\"$package_name\"".replace("$package_name", getModule().getNameSpace())
            + ";\n"
            + "    public static final String BUILD_TYPE = "
            + "\"release\""
            + ";\n"
            + "    public static final int VERSION_CODE = "
            + getModule().getSettings().getInt(ModuleSettings.VERSION_CODE, 1)
            + ";\n"
            + "    public static final String VERSION_NAME = "
            + "\"$version_name\""
                .replace(
                    "$version_name",
                    getModule().getSettings().getString(ModuleSettings.VERSION_NAME, "1.0"))
            + ";\n"
            + "}\n";

    FileUtils.writeStringToFile(buildConfigClass, buildConfigString, Charset.defaultCharset());
  }

  public void GenerateBuildConfig(String packageName, File genDir) throws IOException {
    Log.d(TAG, "Generating BuildConfig.java");
    if (packageName == null) {
      throw new IOException("Unable to find namespace in build.gradle file");
    }
    File dir = new File(genDir, packageName.replace('.', '/'));

    File buildConfigClass = new File(dir, "/BuildConfig.java");
    if (genDir.exists()) {
    } else {
      genDir.mkdirs();
    }

    if (!buildConfigClass.exists() && !buildConfigClass.createNewFile()) {
      throw new IOException("Unable to generate BuildConfig.java");
    }

    String buildConfigString =
        "/**"
            + "\n"
            + "* Automatically generated file. DO NOT MODIFY"
            + "\n"
            + "*/"
            + "\n"
            + "package "
            + packageName
            + ";\n"
            + "\n"
            + "public final class BuildConfig {"
            + "\n"
            + "    public static final boolean DEBUG = "
            + "false"
            + ";\n"
            + "    public static final String LIBRARY_PACKAGE_NAME = "
            + "\"$package_name\"".replace("$package_name", packageName)
            + ";\n"
            + "    public static final String BUILD_TYPE = "
            + "\"release\""
            + ";\n"
            + "}\n";

    FileUtils.writeStringToFile(buildConfigClass, buildConfigString, Charset.defaultCharset());
  }
}
