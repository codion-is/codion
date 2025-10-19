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

import is.codion.common.property.PropertyValue;

import java.io.ObjectInputFilter;

import static is.codion.common.Configuration.*;

/**
 * A {@link ObjectInputFilterFactory} implementation based on patterns, specified as a string via {@link #SERIALIZATION_FILTER_PATTERNS}
 * or from a file via {@link #SERIALIZATION_FILTER_PATTERN_FILE}.
 * <p>See <a href="https://docs.oracle.com/en/java/javase/23/core/java-serialization-filters.html">Java Serialization Filters</a> and
 * <a href="https://openjdk.org/jeps/290">JEP 290: Filter Incoming Serialization Data</a>
 */
public final class SerializationFilterFactory implements ObjectInputFilterFactory {

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

	@Override
	public ObjectInputFilter createObjectInputFilter() {
		if (!SERIALIZATION_FILTER_PATTERN_FILE.isNull()) {
			return SerializationFilter.fromFile(SERIALIZATION_FILTER_PATTERN_FILE.getOrThrow(), buildLimitsPrefix());
		}
		if (!SERIALIZATION_FILTER_PATTERNS.isNull()) {
			return SerializationFilter.fromPatterns(SERIALIZATION_FILTER_PATTERNS.getOrThrow(), buildLimitsPrefix());
		}
		if (!SERIALIZATION_FILTER_DRYRUN_FILE.isNull()) {
			return SerializationFilter.whitelistDryRun(SERIALIZATION_FILTER_DRYRUN_FILE.getOrThrow());
		}

		throw new IllegalStateException("No serialization filter pattern configuration available");
	}

	private static String buildLimitsPrefix() {
		return new StringBuilder()
						.append("maxbytes=").append(SERIALIZATION_FILTER_MAX_BYTES.getOrThrow()).append(";")
						.append("maxarray=").append(SERIALIZATION_FILTER_MAX_ARRAY.getOrThrow()).append(";")
						.append("maxdepth=").append(SERIALIZATION_FILTER_MAX_DEPTH.getOrThrow()).append(";")
						.append("maxrefs=").append(SERIALIZATION_FILTER_MAX_REFS.getOrThrow()).append(";")
						.toString();
	}
}
