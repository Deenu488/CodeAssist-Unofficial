package com.deenu143.gradle.utils;

import com.tyron.builder.log.ILogger;
import com.tyron.resolver.model.Dependency;
import com.tyron.resolver.repository.RepositoryManager;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DependencyUtils {

	private static final Pattern DEPENDENCIES = Pattern.compile("\\s*(implementation)\\s*(')([a-zA-Z0-9.'/-:\\-]+)(')");
	private static final Pattern DEPENDENCIES_QUOT = Pattern
	.compile("\\s*(implementation)\\s*(\")([a-zA-Z0-9.'/-:\\-]+)(\")");
	
	public static List<Dependency> parseDependencies(RepositoryManager repository, File file, ILogger logger)
	throws IOException {
		String readString = FileUtils.readFileToString(file, Charset.defaultCharset());
		return parseDependencies(repository, readString, logger);
	}

	public static List<Dependency> parseDependencies(RepositoryManager repositoryManager, String readString,
													 ILogger logger) throws IOException {
		readString = readString.replaceAll("\\s*//.*", "");
		Matcher matcher = DEPENDENCIES.matcher(readString);
		List<Dependency> dependencies = new ArrayList<>();
		while (matcher.find()) {
			String declaration = matcher.group(3);
			if (declaration != null) {
				dependencies.add(Dependency.valueOf(declaration));
			}
		}
		matcher = DEPENDENCIES_QUOT.matcher(readString);
		while (matcher.find()) {
			String declaration = matcher.group(3);
			if (declaration != null) {
				try {
					Dependency dependency = Dependency.valueOf(declaration);
					dependencies.add(dependency);
				} catch (IllegalArgumentException e) {
					logger.warning("Failed to add dependency " + e.getMessage());
				}
			}
		}
		return dependencies;
	}
}
