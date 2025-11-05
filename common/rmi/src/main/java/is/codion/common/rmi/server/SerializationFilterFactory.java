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
 * Copyright (c) 2024 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.common.rmi.server;

import is.codion.common.utilities.property.PropertyValue;

import org.jspecify.annotations.Nullable;
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
import java.util.Collection;
import java.util.LinkedHashSet;

import static is.codion.common.utilities.Configuration.*;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

/**
 * A {@link ObjectInputFilterFactory} implementation based on patterns, specified as a string via {@link #SERIALIZATION_FILTER_PATTERNS}
 * or from a file via {@link #SERIALIZATION_FILTER_PATTERN_FILE}.
 * <p>See <a href="https://docs.oracle.com/en/java/javase/23/core/java-serialization-filters.html">Java Serialization Filters</a> and
 * <a href="https://openjdk.org/jeps/290">JEP 290: Filter Incoming Serialization Data</a>
 */
public final class SerializationFilterFactory implements ObjectInputFilterFactory {

	private static final Logger LOG = LoggerFactory.getLogger(SerializationFilterFactory.class);

	/**
	 * <p>The serialization patterns to use.
	 * <p>Is overridden by {@link #SERIALIZATION_FILTER_PATTERNS}.
	 */
	public static final PropertyValue<String> SERIALIZATION_FILTER_PATTERNS = stringValue("codion.server.serialization.filter.patterns");

	/**
	 * <p>The path to the serialization pattern file to use.
	 * <p>Supports 'classpath:' prefix for a pattern file in the classpath root.
	 */
	public static final PropertyValue<String> SERIALIZATION_FILTER_PATTERN_FILE = stringValue("codion.server.serialization.filter.patternFile");

	/**
	 * If specified then a list of all deserialized classes is written to the given file on server shutdown. Note this overwrites the file if it already exists.
	 */
	public static final PropertyValue<String> SERIALIZATION_FILTER_DRYRUN_FILE = stringValue("codion.server.serialization.filter.dryRunFile");

	/**
	 * The interval in seconds for periodically flushing the dry-run output to disk.
	 * <ul>
	 * <li>Value type: Integer
	 * <li>Default value: 30
	 * </ul>
	 */
	public static final PropertyValue<Integer> SERIALIZATION_FILTER_DRYRUN_FLUSH_INTERVAL = integerValue("codion.server.serialization.filter.dryRunFlushInterval", 30);

	/**
	 * The maximum number of bytes in the input stream to prevent resource exhaustion attacks.
	 * <ul>
	 * <li>Value type: Long
	 * <li>Default value: 10.485.760 (10 MB)
	 * </ul>
	 */
	public static final PropertyValue<Long> SERIALIZATION_FILTER_MAX_BYTES = longValue("codion.server.serialization.filter.maxBytes", 10_485_760L);

	/**
	 * The maximum array size allowed to prevent resource exhaustion attacks.
	 * <ul>
	 * <li>Value type: Integer
	 * <li>Default value: 100.000
	 * </ul>
	 */
	public static final PropertyValue<Integer> SERIALIZATION_FILTER_MAX_ARRAY = integerValue("codion.server.serialization.filter.maxArray", 100_000);

	/**
	 * The maximum depth of the object graph to prevent resource exhaustion attacks.
	 * <ul>
	 * <li>Value type: Integer
	 * <li>Default value: 100
	 * </ul>
	 */
	public static final PropertyValue<Integer> SERIALIZATION_FILTER_MAX_DEPTH = integerValue("codion.server.serialization.filter.maxDepth", 100);

	/**
	 * The maximum number of object references to prevent resource exhaustion attacks.
	 * <ul>
	 * <li>Value type: Integer
	 * <li>Default value: 1.000.000
	 * </ul>
	 */
	public static final PropertyValue<Integer> SERIALIZATION_FILTER_MAX_REFS = integerValue("codion.server.serialization.filter.maxRefs", 1_000_000);

	private static final String CLASSPATH_PREFIX = "classpath:";

	@Override
	public ObjectInputFilter createObjectInputFilter() {
		if (!SERIALIZATION_FILTER_DRYRUN_FILE.isNull()) {
			int flushInterval = SERIALIZATION_FILTER_DRYRUN_FLUSH_INTERVAL.getOrThrow();
			LOG.warn("SerializationFilterDryRun active, no filtering performed (flush interval: {}s)", flushInterval);
			return new SerializationFilterDryRun(SERIALIZATION_FILTER_DRYRUN_FILE.getOrThrow(), true, flushInterval);
		}
		String patterns = createPatterns();
		if (!SERIALIZATION_FILTER_PATTERN_FILE.isNull()) {
			LOG.info("Serialization filter created from pattern file: {} with patterns: {}", SERIALIZATION_FILTER_PATTERN_FILE.getOrThrow(), patterns);
		}
		if (!SERIALIZATION_FILTER_PATTERNS.isNull()) {
			LOG.info("Serialization filter created from patterns: {}", patterns);
		}
		if (patterns == null) {
			throw new IllegalStateException("No serialization filter pattern configuration available");
		}

		return ObjectInputFilter.Config.createFilter(patterns);
	}

	static @Nullable String createPatterns() {
		if (!SERIALIZATION_FILTER_PATTERN_FILE.isNull()) {
			return addLimitsAndExcludeAll(readPattern(SERIALIZATION_FILTER_PATTERN_FILE.getOrThrow()));
		}
		if (!SERIALIZATION_FILTER_PATTERNS.isNull()) {
			return addLimitsAndExcludeAll(SERIALIZATION_FILTER_PATTERNS.getOrThrow());
		}

		return null;
	}

	private static String addLimitsAndExcludeAll(String patterns) {
		return buildLimitsPrefix() + appendExcludeAll(requireNonNull(patterns).trim());
	}

	private static String appendExcludeAll(String patterns) {
		return patterns.endsWith("!*") ? patterns : patterns + ";!*";
	}

	private static String buildLimitsPrefix() {
		return new StringBuilder()
						.append("maxbytes=").append(SERIALIZATION_FILTER_MAX_BYTES.getOrThrow()).append(";")
						.append("maxarray=").append(SERIALIZATION_FILTER_MAX_ARRAY.getOrThrow()).append(";")
						.append("maxdepth=").append(SERIALIZATION_FILTER_MAX_DEPTH.getOrThrow()).append(";")
						.append("maxrefs=").append(SERIALIZATION_FILTER_MAX_REFS.getOrThrow()).append(";")
						.toString();
	}

	private static String readPattern(String patternFile) {
		Collection<String> lines;
		if (requireNonNull(patternFile).startsWith(CLASSPATH_PREFIX)) {
			lines = readClasspathWhitelistItems(patternFile);
		}
		else {
			lines = readFileWhitelistItems(patternFile);
		}

		return lines.stream()
						.filter(line -> !line.startsWith("#"))
						.collect(joining(";"))
						.trim();
	}

	private static Collection<String> readClasspathWhitelistItems(String patternFile) {
		String path = classpathFilepath(patternFile);
		try (InputStream patternFileStream = SerializationFilterFactory.class.getClassLoader().getResourceAsStream(path)) {
			if (patternFileStream == null) {
				throw new RuntimeException("Serialization pattern file not found on classpath: " + path);
			}
			return new BufferedReader(new InputStreamReader(patternFileStream, StandardCharsets.UTF_8))
							.lines()
							.collect(toList());
		}
		catch (IOException e) {
			throw new RuntimeException("Unable to load serialization pattern file from classpath: " + patternFile, e);
		}
	}

	private static Collection<String> readFileWhitelistItems(String patternFile) {
		try {
			return new LinkedHashSet<>(Files.readAllLines(Paths.get(patternFile)));
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
			throw new IllegalArgumentException("Serialization pattern file must be in the classpath root");
		}

		return path;
	}
}
