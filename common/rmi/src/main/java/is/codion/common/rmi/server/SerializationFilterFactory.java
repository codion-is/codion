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

import is.codion.common.Configuration;
import is.codion.common.property.PropertyValue;

import java.io.ObjectInputFilter;

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
	public static final PropertyValue<String> SERIALIZATION_FILTER_PATTERNS = Configuration.stringValue("codion.server.serialization.filter.patterns");

	/**
	 * <p>The path to the serialization pattern file to use.
	 * <p>Supports 'classpath:' prefix for a pattern file in the classpath root.
	 */
	public static final PropertyValue<String> SERIALIZATION_FILTER_PATTERN_FILE = Configuration.stringValue("codion.server.serialization.filter.patternFile");

	/**
	 * If specified then a list of all deserialized classes is written to the given file on server shutdown. Note this overwrites the file if it already exists.
	 */
	public static final PropertyValue<String> SERIALIZATION_FILTER_DRYRUN_FILE = Configuration.stringValue("codion.server.serialization.filter.dryRunFile");

	@Override
	public ObjectInputFilter createObjectInputFilter() {
		if (!SERIALIZATION_FILTER_PATTERN_FILE.isNull()) {
			return SerializationFilter.fromFile(SERIALIZATION_FILTER_PATTERN_FILE.getOrThrow());
		}
		if (!SERIALIZATION_FILTER_PATTERNS.isNull()) {
			return SerializationFilter.fromPatterns(SERIALIZATION_FILTER_PATTERNS.getOrThrow());
		}
		if (!SERIALIZATION_FILTER_DRYRUN_FILE.isNull()) {
			return SerializationFilter.whitelistDryRun(SERIALIZATION_FILTER_DRYRUN_FILE.getOrThrow());
		}

		throw new IllegalStateException("No serialization filter pattern configuration available");
	}
}
