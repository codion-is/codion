/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2018 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.common.rmi.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputFilter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

/**
 * Implements a serialization whitelist
 */
final class SerializationFilter {

	private static final Logger LOG = LoggerFactory.getLogger(SerializationFilter.class);

	private static final String CLASSPATH_PREFIX = "classpath:";

	private SerializationFilter() {}

	/**
	 * Creates a serialization filter based on a pattern.
	 * @param pattern the serilization filter patterns
	 */
	static ObjectInputFilter fromPatterns(String patterns) {
		ObjectInputFilter filter = ObjectInputFilter.Config.createFilter(patterns);
		LOG.info("Serialization filter created from patterns: {}", patterns);

		return filter;
	}

	/**
	 * Creates a serialization filter based on a file containing patterns.
	 * @param patternFile the path to the file containing the serilization filter patterns
	 */
	static ObjectInputFilter fromFile(String patternFile) {
		ObjectInputFilter filter = ObjectInputFilter.Config.createFilter(readPattern(patternFile));
		LOG.info("Serialization filter created from pattern file: {}", patternFile);

		return filter;
	}

	/**
	 * Creates a serialization filter for a whitelist dry run.
	 * @param patternFile the file to write the dry-run results to
	 * @throws IllegalArgumentException in case of a classpath dry run file
	 */
	static DryRun whitelistDryRun(String patternFile) {
		return new DryRun(patternFile);
	}

	static void handleDryRun() {
		ObjectInputFilter serialFilter = ObjectInputFilter.Config.getSerialFilter();
		if (serialFilter instanceof DryRun) {
			((DryRun) serialFilter).writeToFile();
		}
	}

	static final class DryRun implements ObjectInputFilter {

		private final String patternFile;
		private final Set<Class<?>> deserializedClasses = new HashSet<>();

		private DryRun(String patternFile) {
			if (requireNonNull(patternFile).toLowerCase().startsWith(CLASSPATH_PREFIX)) {
				throw new IllegalArgumentException("Filter dry run can not be performed with a classpath result file: " + patternFile);
			}
			this.patternFile = patternFile;
		}

		@Override
		public Status checkInput(FilterInfo filterInfo) {
			Class<?> clazz = filterInfo.serialClass();
			if (clazz != null) {
				while (clazz.isArray()) {
					clazz = clazz.getComponentType();
				}
				deserializedClasses.add(clazz);
			}

			return Status.ALLOWED;
		}

		/**
		 * Writes all classnames found during the dry-run to the whitelist file.
		 */
		synchronized void writeToFile() {
			try {
				Files.write(Paths.get(patternFile), deserializedClasses.stream()
								.map(Class::getName)
								.sorted()
								.collect(toList()), StandardOpenOption.CREATE);
				LOG.info("Serialization dryrun result written to file: {}", patternFile);
			}
			catch (Exception e) {
				LOG.error("Error while writing dryrun results to file: {}", patternFile, e);
			}
		}
	}

	static String readPattern(String patternFile) {
		Collection<String> lines;
		if (requireNonNull(patternFile).startsWith(CLASSPATH_PREFIX)) {
			lines = readClasspathWhitelistItems(patternFile);
		}
		else {
			lines = readFileWhitelistItems(patternFile);
		}

		return lines.stream()
						.filter(line -> !line.startsWith("#"))
						.collect(joining(";"));
	}

	private static Collection<String> readClasspathWhitelistItems(String patternFile) {
		String path = classpathFilepath(patternFile);
		try (InputStream patternFileStream = SerializationFilter.class.getClassLoader().getResourceAsStream(path)) {
			if (patternFileStream == null) {
				throw new RuntimeException("Seralization pattern file file not found on classpath: " + path);
			}
			return new BufferedReader(new InputStreamReader(patternFileStream, StandardCharsets.UTF_8))
							.lines()
							.collect(toList());
		}
		catch (IOException e) {
			throw new RuntimeException("Unable to load seralization pattern file from classpath: " + patternFile, e);
		}
	}

	private static Collection<String> readFileWhitelistItems(String patternFile) {
		try {
			return new HashSet<>(Files.readAllLines(Paths.get(patternFile)));
		}
		catch (IOException e) {
			LOG.error("Unable to read serialization pattern file: {}", patternFile);
			throw new RuntimeException(e);
		}
	}

	private static String classpathFilepath(String patternFile) {
		String path = patternFile.substring(CLASSPATH_PREFIX.length());
		if (path.startsWith("/")) {
			path = path.substring(1);
		}
		if (path.contains("/")) {
			throw new IllegalArgumentException("Serialization pattern file file must be in the classpath root");
		}

		return path;
	}
}
