package com.tyron.builder.compiler.incremental.java;

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

public class IncrementalCompileJavaTask extends Task<JavaModule> {


    private static final String TAG = "compileJava";
    private File mOutputDir;
    private List<File> mFilesToCompile;

    public IncrementalCompileJavaTask(Project project, JavaModule module, ILogger logger) {
        super(project, module, logger);
    }

    @Override
    public String getName() {
        return TAG;
    }

    @Override
    public void prepare(BuildType type) throws IOException {
        mOutputDir = new File(getModule().getBuildDirectory(), "bin/java/classes");
        if (!mOutputDir.exists() && !mOutputDir.mkdirs()) {
            throw new IOException("Unable to create output directory");
        }

        mFilesToCompile = new ArrayList<>();     
        mFilesToCompile.addAll(getJavaFiles(new File(getModule().getBuildDirectory(), "gen")));
		mFilesToCompile.addAll(getJavaFiles(new File(getModule().getBuildDirectory(), "view_binding")));
    }

    private boolean mHasErrors = false;

    @Override
    public void run() throws IOException, CompilationFailedException {
        if (mFilesToCompile.isEmpty()) {
            return;
        }

        Log.d(TAG, "Compiling java files");

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

        List<File> classpath = new ArrayList<>(getModule().getLibraries());
        classpath.add(mOutputDir);
        File kotlinOutputDir = new File(getModule().getBuildDirectory(), "bin/kotlin/classes");
        classpath.add(kotlinOutputDir);

		standardJavaFileManager.setLocation(StandardLocation.CLASS_OUTPUT,
											Collections.singletonList(mOutputDir));
		standardJavaFileManager.setLocation(StandardLocation.PLATFORM_CLASS_PATH,
											Arrays.asList(getModule().getBootstrapJarFile(),
														  getModule().getLambdaStubsJarFile()));
		standardJavaFileManager.setLocation(StandardLocation.CLASS_PATH, classpath);
		standardJavaFileManager.setLocation(StandardLocation.SOURCE_PATH, mFilesToCompile);

        List<JavaFileObject> javaFileObjects = new ArrayList<>();
        for (File file : mFilesToCompile) {
            javaFileObjects.add(new SimpleJavaFileObject(file.toURI(), JavaFileObject.Kind.SOURCE) {
					@Override
					public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
						return FileUtils.readFileToString(file, StandardCharsets.UTF_8);
					}
				});
        }

        List<String> options = new ArrayList<>();
        options.add("-source");
        options.add("1.8");
        options.add("-target");
        options.add("1.8");
        JavacTask task = tool.getTask(null, standardJavaFileManager, diagnosticCollector,
									  options, null, javaFileObjects); 
		task.parse();
		task.analyze();
		task.generate();


        if (mHasErrors) {
            throw new CompilationFailedException("Compilation failed, check logs for more details");
        }
    }

    @VisibleForTesting
    public List<File> getCompiledFiles() {
        return mFilesToCompile;
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
