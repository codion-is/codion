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

import java.rmi.Remote;
import java.util.ServiceLoader;

import static java.util.Objects.requireNonNull;

/**
 * Provides a {@link AuxiliaryServer} implementation.
 * @param <C> the remote connection type provided by the parent server
 * @param <A> the admin connection type provided by the parent server
 * @param <T> the parent server type
 */
public interface AuxiliaryServerFactory<C extends Remote, A extends ServerAdmin, T extends AuxiliaryServer> {

	/**
	 * Creates a server instance using the given configuration.
	 * @param server the parent server
	 * @return a server
	 */
	T createServer(Server<C, A> server);

	/**
	 * Returns the {@link AuxiliaryServerFactory} implementation found by the {@link ServiceLoader} of the given type.
	 * @param classname the classname of the required auxiliary server factory
	 * @param <C> the remote connection type provided by the parent server
	 * @param <A> the admin connection type provided by the parent server
	 * @param <T> the auxiliary server type
	 * @return a {@link AuxiliaryServerFactory} implementation of the given type from the {@link ServiceLoader}.
	 * @throws IllegalStateException in case no such {@link AuxiliaryServerFactory} implementation is available.
	 */
	static <C extends Remote, A extends ServerAdmin, T extends AuxiliaryServer> AuxiliaryServerFactory<C, A, T> instance(String classname) {
		requireNonNull(classname, "classname");
		for (AuxiliaryServerFactory<C, A, T> serverProvider : ServiceLoader.load(AuxiliaryServerFactory.class)) {
			if (serverProvider.getClass().getName().equals(classname)) {
				return serverProvider;
			}
		}

		throw new IllegalStateException("No auxiliary server factory of type: " + classname + " available");
	}
}
