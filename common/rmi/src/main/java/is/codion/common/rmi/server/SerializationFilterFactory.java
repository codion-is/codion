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
 * A {@link ObjectInputFilterFactory} implementation based on a pattern file.
 */
public final class SerializationFilterFactory implements ObjectInputFilterFactory {

	/**
	 * The serialization whitelist file to use if any
	 */
	public static final PropertyValue<String> SERIALIZATION_FILTER_PATTERN_FILE = Configuration.stringValue("codion.server.serializationFilterPatternFile");

	/**
	 * The serialization dryrun output file
	 */
	public static final PropertyValue<String> SERIALIZATION_FILTER_DRYRUN_OUTPUT = Configuration.stringValue("codion.server.serializationFilterDryRunOutput");

	/**
	 * If true then the serialization whitelist specified by {@link #SERIALIZATION_FILTER_DRYRUN_OUTPUT} is populated
	 * with the names of all deserialized classes on server shutdown. Note this overwrites the file if it already exists.
	 */
	public static final PropertyValue<Boolean> SERIALIZATION_FILTER_DRYRUN = Configuration.booleanValue("codion.server.serializationFilterDryRun", false);

	@Override
	public ObjectInputFilter createObjectInputFilter() {
		if (SERIALIZATION_FILTER_DRYRUN.getOrThrow()) {
			return SerializationFilter.whitelistDryRun(SERIALIZATION_FILTER_DRYRUN_OUTPUT.getOrThrow());
		}

		return SerializationFilter.patternFilter(SERIALIZATION_FILTER_PATTERN_FILE.getOrThrow());
	}
}
