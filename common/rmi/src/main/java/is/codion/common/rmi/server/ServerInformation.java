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
 * Copyright (c) 2020 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.common.rmi.server;

import is.codion.common.version.Version;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Locale;
import java.util.UUID;

/**
 * Encapsulates static server information
 */
public interface ServerInformation {
	/**
	 * @return the server name
	 */
	String serverName();

	/**
	 * @return a unique identifier for this server
	 */
	UUID serverId();

	/**
	 * @return the server framework Version
	 */
	Version serverVersion();

	/**
	 * @return the server port
	 */
	int serverPort();

	/**
	 * @return the time of server startup
	 */
	ZonedDateTime startTime();

	/**
	 * @return the server locale
	 */
	Locale locale();

	/**
	 * @return the server time zone
	 */
	ZoneId timeZone();
}
