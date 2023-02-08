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
import com.tyron.builder.project.cache.CacheHolder;
import com.tyron.common.util.Cache;

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
import java.util.Set;
import java.util.HashSet;

import com.sun.source.util.JavacTask;
import com.sun.tools.javac.api.JavacTool;
import com.sun.tools.javac.file.JavacFileManager;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardLocation;
import com.tyron.builder.model.DiagnosticWrapper;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import com.tyron.builder.internal.jar.AssembleJar;
import java.util.zip.ZipOutputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.io.FileOutputStream;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.SimpleFileVisitor;
import java.util.zip.ZipEntry;
import java.nio.file.Paths;

public class IncrementalAssembleAarTask extends Task<AndroidModule> {
	
	public static final CacheHolder.CacheKey<String, List<File>> CACHE_KEY =
	new CacheHolder.CacheKey<>("javaCache");
    private static final String TAG = "assembleAar";
	private Cache<String, List<File>> mClassCache;
	
    public IncrementalAssembleAarTask(Project project,
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

		String projects = getModule().getSettings().getString(ModuleSettings.INCLUDE, "[]");
		String replace = projects.replace("[", "").replace("]", "").replace(",", " ");
		String[] names = replace.split("\\s");

		for (int i = 0; i < names.length; i++) {
			File res = new File(getModule().getRootFile().getParentFile(), names[i] + "/src/main/res");
			File bin_res = new File(getModule().getRootFile().getParentFile(), names[i] + "/build/bin/res");		
			File build = new File(getModule().getRootFile().getParentFile(), names[i] + "/build");		
			File manifest = new File(getModule().getRootFile().getParentFile(), names[i] + "/src/main/AndroidManifest.xml");
			File assets = new File(getModule().getRootFile().getParentFile(), names[i] + "/src/main/assets");
			File java = new File(getModule().getRootFile().getParentFile(), names[i] + "/src/main/java");
			File classes = new File(getModule().getRootFile().getParentFile(), names[i] + "/build/bin/java/classes");		
			File gen = new File(getModule().getRootFile().getParentFile(), names[i] + "/build/gen");
			File aar = new File(getModule().getRootFile().getParentFile(), names[i] + "/build/bin/aar");		

			if (res.exists() && manifest.exists()) {
				if (build.exists()) {
					FileUtils.deleteDirectory(build);
				}	

				compileRes(res, bin_res, names[i]);
				linkRes(bin_res, names[i], manifest, assets);

				if (java.exists()) {
					compileJava(java, gen , classes);	
				}
				if (classes.exists()) {
					assembleAar(classes, aar, build, names[i]);
				}
			}
		}		
	}

	private void assembleAar(File input, File aar, File build, String name) throws IOException, CompilationFailedException {
		if (!aar.exists()) {
			if (!aar.mkdirs()) {
				throw new IOException("Failed to create resource aar directory");
			}
		}
		AssembleJar assembleJar = new AssembleJar(false);
		assembleJar.setOutputFile(new File(aar.getAbsolutePath(), "classes.jar"));
		assembleJar.createJarArchive(input);

		File libs = new File(build.getAbsolutePath() , "libs");
		if (!libs.exists()) {
			if (!libs.mkdirs()) {
				throw new IOException("Failed to create resource libs directory");
			}
		}
		copyResources(new File(build.getParentFile().getAbsolutePath() , "src/main/AndroidManifest.xml"), aar.getAbsolutePath());
		copyResources(new File(build.getParentFile().getAbsolutePath() , "src/main/res"), aar.getAbsolutePath());

		File assets = new File(build.getParentFile().getAbsolutePath() , "src/main/assets");
		File jniLibs  = new File(build.getParentFile().getAbsolutePath() , "src/main/jniLibs");

		if (assets.exists()) {
			copyResources(assets, aar.getAbsolutePath());
		}
		if (jniLibs.exists()) {
			copyResources(jniLibs, aar.getAbsolutePath());	
			File jni = new File(aar.getAbsolutePath(), "jniLibs");
			jni.renameTo(new File(aar.getAbsolutePath(), "jni"));	
		}
		zipFolder(Paths.get(aar.getAbsolutePath()), Paths.get(libs.getAbsolutePath(), name + ".aar"));
		if (aar.exists()) {
			FileUtils.deleteDirectory(aar);
		}	
	}

	private boolean mHasErrors = false;

	public void compileJava(File java, File gen, File out) throws IOException,
	CompilationFailedException {

		if (!out.exists()) {
			if (!out.mkdirs()) {
				throw new IOException("Failed to create resource output directory");
			}
		}

		List<File> mFilesToCompile = new ArrayList<>();
		
		mClassCache = getModule().getCache(CACHE_KEY, new Cache<>());
		for (Cache.Key<String> key : new HashSet<>(mClassCache.getKeys())) {
            if (!mFilesToCompile.contains(key.file.toFile())) {
                File file = mClassCache.get(key.file, "class").iterator().next();
                deleteAllFiles(file, ".class");
                mClassCache.remove(key.file, "class", "dex");
            }
        }
		
		List<File> classpath = new ArrayList<>();		
		List<JavaFileObject> javaFileObjects = new ArrayList<>();

		mFilesToCompile.addAll(getJavaFiles(java));
		mFilesToCompile.addAll(getJavaFiles(gen));

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

	private void linkRes(File in, String name, File manifest, File assets) throws CompilationFailedException, IOException {
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
		args.add(createNewFile(new File(in.getParentFile().getAbsolutePath(), "aar"), "proguard.txt").getAbsolutePath());

		File resource = new File(in.getAbsolutePath(), name + "_res.zip");			
		if (!resource.exists()) {
			throw new IOException("Unable to get resource file");
		}			
		args.add("-R");
		args.add(resource.getAbsolutePath());

		args.add("--java");
		File gen = new File(getModule().getRootFile().getParentFile(), name + "/build/gen");
		if (!gen.exists()) {
			if (!gen.mkdirs()) {
				throw new CompilationFailedException("Failed to create gen folder");
			}
		}
		args.add(gen.getAbsolutePath());

		args.add("--manifest");
		if (!manifest.exists()) {
			throw new IOException("Unable to get project manifest file");
		}
		args.add(manifest.getAbsolutePath());

		args.add("-o");
		File out = new File(in.getParentFile().getAbsolutePath(), "generated.aar.res");	
		args.add(out.getAbsolutePath());			

		args.add("--output-text-symbols");
		File file = new File(new File(in.getParentFile().getAbsolutePath(), "aar"), "R.txt");
		Files.deleteIfExists(file.toPath());
		if (!file.createNewFile()) {
			throw new IOException("Unable to create R.txt file");
		}
		args.add(file.getAbsolutePath());

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

	private void compileRes(File res, File out, String name) throws IOException, CompilationFailedException {
		if (!out.exists()) {
			if (!out.mkdirs()) {
				throw new IOException("Failed to create resource output directory");
			}
		}
		List<String> args = new ArrayList<>();
		args.add("--dir");
		args.add(res.getAbsolutePath());
		args.add("-o");
		args.add(createNewFile(out, name + "_res.zip").getAbsolutePath());

		int compile = Aapt2Jni.compile(args);
		List<DiagnosticWrapper> logs = Aapt2Jni.getLogs();
		LogUtils.log(logs, getLogger());

		if (compile != 0) {
			throw new CompilationFailedException(
				"Compilation failed, check logs for more details.");
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

	private void zipFolder(final Path sourceFolderPath, Path zipPath) throws IOException {
        final ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipPath.toFile()));
        Files.walkFileTree(sourceFolderPath, new SimpleFileVisitor<Path>() {
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					zos.putNextEntry(new ZipEntry(sourceFolderPath.relativize(file).toString()));
					Files.copy(file, zos);
					zos.closeEntry();
					return FileVisitResult.CONTINUE;
				}
			});
        zos.close();
    }

	private void copyResources(File file, String path) throws IOException {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File child : files) {
                    copyResources(child, path + "/" + file.getName());
                }
            }
        } else {
            File directory = new File(path);
            if (!directory.exists() && !directory.mkdirs()) {
                throw new IOException("Failed to create directory " + directory);
            }

            FileUtils.copyFileToDirectory(file, directory);
        }
    }
	
	private void deleteAllFiles(File classFile, String ext) throws IOException {
        File parent = classFile.getParentFile();
        String name = classFile.getName().replace(ext, "");
        if (parent != null) {
            File[] children =
				parent.listFiles((c) -> c.getName().endsWith(ext) && c.getName().contains("$"));
            if (children != null) {
                for (File child : children) {
                    if (child.getName().startsWith(name)) {
                        FileUtils.delete(child);
                    }
                }
            }
        }
        if (classFile.exists()) {
            FileUtils.delete(classFile);
        }
    }
}
