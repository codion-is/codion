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
 * <p>RMI server related classes.
 * <p>Package configuration values:
 * <ul>
 * <li>{@link is.codion.common.rmi.server.ServerConfiguration#RMI_SERVER_HOSTNAME}
 * <li>{@link is.codion.common.rmi.server.ServerConfiguration#SERVER_NAME_PREFIX}
 * <li>{@link is.codion.common.rmi.server.ServerConfiguration#SERVER_PORT}
 * <li>{@link is.codion.common.rmi.server.ServerConfiguration#REGISTRY_PORT}
 * <li>{@link is.codion.common.rmi.server.ServerConfiguration#KEYSTORE}
 * <li>{@link is.codion.common.rmi.server.ServerConfiguration#KEYSTORE_PASSWORD}
 * <li>{@link is.codion.common.rmi.server.ServerConfiguration#ADMIN_PORT}
 * <li>{@link is.codion.common.rmi.server.ServerConfiguration#ADMIN_USER}
 * <li>{@link is.codion.common.rmi.server.ServerConfiguration#SSL_ENABLED}
 * <li>{@link is.codion.common.rmi.server.ServerConfiguration#IDLE_CONNECTION_TIMEOUT}
 * <li>{@link is.codion.common.rmi.server.ServerConfiguration#AUXILIARY_SERVER_FACTORIES}
 * <li>{@link is.codion.common.rmi.server.ServerConfiguration#OBJECT_INPUT_FILTER_FACTORY}
 * <li>{@link is.codion.common.rmi.server.SerializationFilterFactory#SERIALIZATION_FILTER_PATTERNS}
 * <li>{@link is.codion.common.rmi.server.SerializationFilterFactory#SERIALIZATION_FILTER_PATTERN_FILE}
 * <li>{@link is.codion.common.rmi.server.SerializationFilterFactory#SERIALIZATION_FILTER_DRYRUN_FILE}
 * </ul>
 */
@org.jspecify.annotations.NullMarked
package is.codion.common.rmi.server;