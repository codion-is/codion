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
 * Copyright (c) 2023 - 2025, Björn Darri Sigurðsson.
 */
/**
 * <p>RMI client/server classes.
 * <ul>
 * <li>{@link is.codion.common.rmi.client.ConnectionRequest}
 * <li>{@link is.codion.common.rmi.server.Server}
 * <li>{@link is.codion.common.rmi.server.ServerConfiguration}
 * <li>{@link is.codion.common.rmi.server.Authenticator}
 * <li>{@link is.codion.common.rmi.server.RemoteClient}
 * <li>{@link is.codion.common.rmi.server.ObjectInputFilterFactory}
 * </ul>
 * @uses is.codion.common.rmi.server.AuxiliaryServerFactory
 * @uses is.codion.common.rmi.server.Authenticator
 * @uses is.codion.common.rmi.server.ObjectInputFilterFactory
 * @provides is.codion.common.rmi.server.ObjectInputFilterFactory
 */
@org.jspecify.annotations.NullMarked
module is.codion.common.rmi {
	requires transitive org.jspecify;
	requires org.slf4j;
	requires jdk.management;
	requires nl.altindag.ssl;
	requires transitive java.rmi;
	requires transitive is.codion.common.utilities;

	exports is.codion.common.rmi.client;
	exports is.codion.common.rmi.server;
	exports is.codion.common.rmi.server.exception;

	uses is.codion.common.rmi.server.AuxiliaryServerFactory;
	uses is.codion.common.rmi.server.Authenticator;
	uses is.codion.common.rmi.server.ObjectInputFilterFactory;

	provides is.codion.common.rmi.server.ObjectInputFilterFactory
					with is.codion.common.rmi.server.SerializationFilterFactory;
}