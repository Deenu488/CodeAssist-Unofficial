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

public class GenerateBuildConfigReleaseTask extends Task<AndroidModule> {

    private static final String TAG = "GenerateBuildConfigReleaseTask";
    private File mBuildConfigFile;

    public GenerateBuildConfigReleaseTask(Project project, AndroidModule module, ILogger logger) {
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
        getLogger().debug("Generating BuildConfig.java");

        File packageDir = new File(getModule().getJavaDirectory(), getModule().getPackageName()
                                   .replace('.', '/'));
        File buildConfigClass = new File(getModule().getJavaDirectory(), getModule().getPackageName()
                                         .replace('.', '/') + "/BuildConfig.java");
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
            "    public static final boolean DEBUG = " + "false" + ";\n" +
            "    public static final String APPLICATION_ID = " + "\"$package_name\"" .replace("$package_name", getModule().getPackageName())  + ";\n" +
            "    public static final String BUILD_TYPE = " + "\"release\"" + ";\n" +
            "    public static final int VERSION_CODE = " + getModule().getSettings().getInt(ModuleSettings.VERSION_CODE, 1) + ";\n" +
            "    public static final String VERSION_NAME = " + "\"$version_name\""  .replace("$version_name", getModule().getSettings().getString(ModuleSettings.VERSION_NAME, "1.0"))  + ";\n" +
            "}\n";

        FileUtils.writeStringToFile(buildConfigClass, buildConfigString, Charset.defaultCharset());
        mBuildConfigFile = buildConfigClass;
        getModule().addJavaFile(buildConfigClass);
    }
}
