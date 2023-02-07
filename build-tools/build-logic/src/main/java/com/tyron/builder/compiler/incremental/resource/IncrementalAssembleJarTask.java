package com.tyron.builder.compiler.incremental.resource;

import androidx.annotation.VisibleForTesting;
import com.sun.source.util.JavacTask;
import com.sun.tools.javac.api.JavacTool;
import com.sun.tools.javac.file.JavacFileManager;
import com.tyron.builder.compiler.BuildType;
import com.tyron.builder.compiler.Task;
import com.tyron.builder.compiler.incremental.kotlin.IncrementalKotlinCompiler;
import com.tyron.builder.exception.CompilationFailedException;
import com.tyron.builder.log.ILogger;
import com.tyron.builder.model.DiagnosticWrapper;
import com.tyron.builder.project.Project;
import com.tyron.builder.project.api.AndroidModule;
import com.tyron.builder.project.api.JavaModule;
import com.tyron.builder.project.cache.CacheHolder;
import com.tyron.common.util.Cache;
import com.tyron.builder.model.ModuleSettings;
import org.apache.commons.io.FileUtils;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardLocation;
import android.util.Log;
import java.util.Set;
import com.tyron.builder.internal.jar.AssembleJar;

public class IncrementalAssembleJarTask extends Task<JavaModule> {

    private static final String TAG = "assembleJar";
	
	public IncrementalAssembleJarTask(Project project, JavaModule module, ILogger logger) {
        super(project, module, logger);
    }

    @Override
    public String getName() {
        return TAG;
    }

    @Override
    public void prepare(BuildType type) throws IOException {
	}

    private boolean mHasErrors = false;

    @Override
    public void run() throws IOException, CompilationFailedException {
		String projects = getModule().getSettings().getString(ModuleSettings.INCLUDE, "[]");
		String replace = projects.replace("[", "").replace("]", "").replace(",", " ");
		String[] names = replace.split("\\s");

		for (int i = 0; i < names.length; i++) {
			File java = new File(getModule().getRootFile().getParentFile(), names[i] + "/src/main/java");
			File classes = new File(getModule().getRootFile().getParentFile(), names[i] + "/build/bin/java/classes");		
			File out = new File(getModule().getRootFile().getParentFile(), names[i] + "/build/libs/" + names[i] + ".jar");			
			File build = new File(getModule().getRootFile().getParentFile(), names[i] + "/build");

			if (build.exists()) {
			FileUtils.deleteDirectory(build);
			}

			if (java.exists()) {
			compileJava(java, classes);
			}
			if (classes.exists()) {
				assembleJar(classes,out);
			}
		}	
	}

	private void assembleJar(File input,File out) throws IOException, CompilationFailedException {
		if (!out.getParentFile().exists()) {
			if (!out.getParentFile().mkdirs()) {
				throw new IOException("Failed to create resource output directory");
			}
		}
		AssembleJar assembleJar = new AssembleJar(false);
		assembleJar.setOutputFile(out);
		assembleJar.createJarArchive(input);
	}

	public void compileJava(File java, File out) throws IOException,
	CompilationFailedException {
		
		if (!out.exists()) {
			if (!out.mkdirs()) {
				throw new IOException("Failed to create resource output directory");
			}
		}
		
		File res = new File(java.getParentFile().getAbsolutePath(),"res");
		if (res.exists()){
		FileUtils.deleteDirectory(out);	
		return;
		}
		
		List<File> mFilesToCompile = new ArrayList<>();
		List<File> classpath = new ArrayList<>();		
		List<JavaFileObject> javaFileObjects = new ArrayList<>();

		mFilesToCompile.addAll(getJavaFiles(java));

		if (mFilesToCompile.isEmpty()) {
            return;
        }

		List<String> options = new ArrayList<>();
        options.add("-source");
        options.add("1.8");
        options.add("-target");
        options.add("1.8");

		for (File file : mFilesToCompile) {
            javaFileObjects.add(new SimpleJavaFileObject(file.toURI(), JavaFileObject.Kind.SOURCE) {
					@Override
					public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
						return FileUtils.readFileToString(file, StandardCharsets.UTF_8);
					}
				});
        }

		DiagnosticListener<JavaFileObject> diagnosticCollector = diagnostic -> {
			switch (diagnostic.getKind()) {
				case ERROR:
					mHasErrors = true;
					getLogger().error(new DiagnosticWrapper(diagnostic));
					break;
				case WARNING:
					getLogger().warning(new DiagnosticWrapper(diagnostic));
			}
		};

		JavacTool tool = JavacTool.create();
        JavacFileManager standardJavaFileManager =
			tool.getStandardFileManager(diagnosticCollector, Locale.getDefault(),
										Charset.defaultCharset());
		standardJavaFileManager.setSymbolFileEnabled(false);
		standardJavaFileManager.setLocation(StandardLocation.CLASS_OUTPUT,
											Collections.singletonList(out));
		standardJavaFileManager.setLocation(StandardLocation.PLATFORM_CLASS_PATH,
											Arrays.asList(getModule().getBootstrapJarFile(),
														  getModule().getLambdaStubsJarFile()));
		standardJavaFileManager.setLocation(StandardLocation.CLASS_PATH, classpath);
		standardJavaFileManager.setLocation(StandardLocation.SOURCE_PATH, mFilesToCompile);

		JavacTask task = tool.getTask(null, standardJavaFileManager, diagnosticCollector,
									  options, null, javaFileObjects);	
		task.parse();
		task.analyze();
		task.generate();

		if (mHasErrors) {
            throw new CompilationFailedException("Compilation failed, check logs for more details");
        }
	}

	public static Set<File> getJavaFiles(File dir) {
        Set<File> javaFiles = new HashSet<>();

        File[] files = dir.listFiles();
        if (files == null) {
            return Collections.emptySet();
        }

        for (File file : files) {
            if (file.isDirectory()) {
                javaFiles.addAll(getJavaFiles(file));
            } else {
                if (file.getName().endsWith(".java")) {
                    javaFiles.add(file);
                }
            }
        }

        return javaFiles;
    }
}
