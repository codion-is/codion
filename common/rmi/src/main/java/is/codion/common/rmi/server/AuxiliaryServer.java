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
 * Copyright (c) 2020 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.common.rmi.server;

/**
 * Auxiliary servers to be run in conjunction with a {@link Server} must implement this interface.
 */
public interface AuxiliaryServer {

	/**
	 * Starts this server
	 * @throws Exception in case of an exception
	 */
	void start() throws Exception;

	/**
	 * Stops this server
	 * @throws Exception in case of an exception
	 */
	void stop() throws Exception;

	/**
	 * @return a String describing the server and its configuration, for logging purposes
	 */
	String information();
}
