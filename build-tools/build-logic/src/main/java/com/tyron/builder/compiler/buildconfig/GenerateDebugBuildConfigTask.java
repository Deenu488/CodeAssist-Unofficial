package com.tyron.builder.compiler.buildconfig;

import com.tyron.builder.compiler.BuildType;
import com.tyron.builder.compiler.Task;
import com.tyron.builder.exception.CompilationFailedException;
import com.tyron.builder.log.ILogger;
import com.tyron.builder.project.Project;
import com.tyron.builder.project.api.AndroidModule;
import com.tyron.builder.model.ModuleSettings;
import org.apache.commons.io.FileUtils;
import java.io.File;
import java.nio.charset.Charset;
import java.io.IOException;
import android.util.Log;

public class GenerateDebugBuildConfigTask extends Task<AndroidModule> {

    private static final String TAG = "generateDebugBuildConfig";

    public GenerateDebugBuildConfigTask(Project project, AndroidModule module, ILogger logger) {
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

        File packageDir = new File(getModule().getBuildDirectory()+ "/gen", getModule().getPackageName()
                                   .replace('.', '/'));
        File buildConfigClass = new File(packageDir, "/BuildConfig.java");
        if (packageDir.exists()) {
        } else {
            packageDir.mkdirs();  
        }

        if (!buildConfigClass.exists() && !buildConfigClass.createNewFile()) {
            throw new IOException("Unable to generate BuildConfig.java");
        }

        String buildConfigString =
            "/**" + "\n" +
            "* Automatically generated file. DO NOT MODIFY" + "\n" +
            "*/" + "\n" +
            "package " + getModule().getPackageName() + ";\n" + 
            "\n" +  
            "public final class BuildConfig {" + "\n" +
            "    public static final boolean DEBUG = " + "Boolean.parseBoolean(\"true\")"  + ";\n" +
            "    public static final String APPLICATION_ID = " + "\"$package_name\"" .replace("$package_name", getModule().getPackageName())  + ";\n" +
            "    public static final String BUILD_TYPE = " + "\"debug\"" + ";\n" +
            "    public static final int VERSION_CODE = " + getModule().getSettings().getInt(ModuleSettings.VERSION_CODE, 1) + ";\n" +
            "    public static final String VERSION_NAME = " + "\"$version_name\""  .replace("$version_name", getModule().getSettings().getString(ModuleSettings.VERSION_NAME, "1.0"))  + ";\n" +
            "}\n";

        FileUtils.writeStringToFile(buildConfigClass, buildConfigString, Charset.defaultCharset());
    }
}
