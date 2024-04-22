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
 * Copyright (c) 2024, Björn Darri Sigurðsson.
 */
package is.codion.common.rmi.server;

import is.codion.common.Configuration;
import is.codion.common.property.PropertyValue;

import java.io.ObjectInputFilter;

/**
 * A {@link ObjectInputFilterFactory} implementation based on a whitelist file.
 */
public final class WhitelistInputFilterFactory implements ObjectInputFilterFactory {

	/**
	 * The serialization whitelist file to use if any
	 */
	public static final PropertyValue<String> SERIALIZATION_FILTER_WHITELIST = Configuration.stringValue("codion.server.serializationFilterWhitelist");

	/**
	 * If true then the serialization whitelist specified by {@link #SERIALIZATION_FILTER_WHITELIST} is populated
	 * with the names of all deserialized classes on server shutdown. Note this overwrites the file if it already exists.
	 */
	public static final PropertyValue<Boolean> SERIALIZATION_FILTER_DRYRUN = Configuration.booleanValue("codion.server.serializationFilterDryRun", false);

	@Override
	public ObjectInputFilter createObjectInputFilter() {
		String whitelist = SERIALIZATION_FILTER_WHITELIST.getOrThrow();
		if (SERIALIZATION_FILTER_DRYRUN.get()) {
			return SerializationWhitelist.whitelistDryRun(whitelist);
		}

		return SerializationWhitelist.whitelistFilter(whitelist);
	}
}
