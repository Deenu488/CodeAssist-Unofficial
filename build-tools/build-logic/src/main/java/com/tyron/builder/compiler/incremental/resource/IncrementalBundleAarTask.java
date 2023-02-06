package com.tyron.builder.compiler.incremental.resource;

import com.android.tools.aapt2.Aapt2Jni;
import com.tyron.builder.compiler.BuildType;
import com.tyron.builder.compiler.Task;
import com.tyron.builder.exception.CompilationFailedException;
import com.tyron.builder.log.ILogger;
import com.tyron.builder.log.LogUtils;
import com.tyron.builder.model.DiagnosticWrapper;
import com.tyron.builder.project.Project;
import com.tyron.builder.project.api.AndroidModule;
import com.tyron.builder.project.api.Module;
import com.tyron.builder.model.ModuleSettings;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import android.util.Log;

public class IncrementalBundleAarTask extends Task<AndroidModule> {

    private static final String TAG = "bundleAar";

    public IncrementalBundleAarTask(Project project,
                                AndroidModule module,
                                ILogger logger) {
        super(project, module, logger);       
    }

    @Override
    public String getName() {
        return TAG;
    }

    @Override
    public void prepare(BuildType type) throws IOException {

    }

    public void run() throws IOException, CompilationFailedException {
		
		compileProjects();
        link();
    }

	private void compileProjects() throws IOException,
	CompilationFailedException {

		String projects = getModule().getSettings().getString(ModuleSettings.INCLUDE, "[]");
		String replace = projects.replace("[","").replace("]","").replace(","," ");
		String[] names = replace.split("\\s");

		for (String str:names) {
			File output = new File(getModule().getRootFile().getParentFile(), str + "/build/bin/res");
			if (!output.exists()) {
				if (!output.mkdirs()) {
					throw new IOException("Failed to create resource output directory");
				}
			}
			File res = new File(getModule().getRootFile().getParentFile(), str + "/src/main/res");
			if (res.exists()) {
				List<String> args = new ArrayList<>();
				args.add("--dir");
				args.add(res.getAbsolutePath());
				args.add("-o");
				args.add(createNewFile(output, str + "_res.zip").getAbsolutePath());

				int compile = Aapt2Jni.compile(args);
				List<DiagnosticWrapper> logs = Aapt2Jni.getLogs();
				LogUtils.log(logs, getLogger());

				if (compile != 0) {
					throw new CompilationFailedException(
						"Compilation failed, check logs for more details.");
				}						
			}
	    }
	}
	
	private void link() throws IOException,
	CompilationFailedException {

		String projects = getModule().getSettings().getString(ModuleSettings.INCLUDE, "[]");
		String replace = projects.replace("[","").replace("]","").replace(","," ");
		String[] names = replace.split("\\s");

		for (String str:names) {
			File output = new File(getModule().getRootFile().getParentFile(), str + "/build/bin/res");
			if (!output.exists()) {
				if (!output.mkdirs()) {
					throw new IOException("Failed to create resource output directory");
				}
			}
			File res = new File(getModule().getRootFile().getParentFile(), str + "/src/main/res");
			if (res.exists()) {
				List<String> args = new ArrayList<>();
				args.add("-I");
				args.add(getModule().getBootstrapJarFile().getAbsolutePath());
				args.add("--allow-reserved-package-id");
				args.add("--no-version-vectors");
				args.add("--no-version-transitions");
				args.add("--auto-add-overlay");
				args.add("--min-sdk-version");
				args.add(String.valueOf(getModule().getMinSdk()));
				args.add("--target-sdk-version");
				args.add(String.valueOf(getModule().getTargetSdk()));
				args.add("--proguard");
				args.add(createNewFile(output, "proguard.txt").getAbsolutePath());
				
				File resource = new File(output.getAbsolutePath(), str + "_res.zip");			
				if (!resource.exists()) {
					throw new IOException("Unable to get resource file");
				}			
				args.add("-R");
				args.add(resource.getAbsolutePath());
				
				args.add("--java");
				File gen = new File(getModule().getRootFile().getParentFile(), str + "/build/gen");
				if (!gen.exists()) {
					if (!gen.mkdirs()) {
						throw new CompilationFailedException("Failed to create gen folder");
					}
				}
				args.add(gen.getAbsolutePath());

				args.add("--manifest");
				File projectsManifest = new File(getModule().getRootFile().getParentFile(), str + "/src/main/AndroidManifest.xml");
				if (!projectsManifest.exists()) {
					throw new IOException("Unable to get project manifest file");
				}
				args.add(projectsManifest.getAbsolutePath());
				
				args.add("-o");
				File out = new File(getModule().getRootFile().getParentFile(), str + "/build/bin/generated.aar.res");	
				args.add(out.getAbsolutePath());			
				
				args.add("--output-text-symbols");
				File file = new File(output.getAbsolutePath(), "R.txt");
				Files.deleteIfExists(file.toPath());
				if (!file.createNewFile()) {
					throw new IOException("Unable to create R.txt file");
				}
				args.add(file.getAbsolutePath());
				
				File assets = new File(getModule().getRootFile().getParentFile(), str + "/src/main/assets");	
				if (assets.exists()) {
					args.add("-A");
					args.add(assets.getAbsolutePath());
				}
				
				int compile = Aapt2Jni.link(args);
				List<DiagnosticWrapper> logs = Aapt2Jni.getLogs();
				LogUtils.log(logs, getLogger());

				if (compile != 0) {
					throw new CompilationFailedException(
						"Compilation failed, check logs for more details.");
				}
			}
	    }
	}

    private File createNewFile(File parent, String name) throws IOException {
        File createdFile = new File(parent, name);
        if (!parent.exists()) {
            if (!parent.mkdirs()) {
                throw new IOException("Unable to create directories");
            }
        }
        if (!createdFile.exists() && !createdFile.createNewFile()) {
            throw new IOException("Unable to create file " + name);
        }
        return createdFile;
    }
}
