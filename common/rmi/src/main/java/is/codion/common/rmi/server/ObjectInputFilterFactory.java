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

import java.io.ObjectInputFilter;
import java.util.ServiceLoader;

import static java.util.Objects.requireNonNull;

/**
 * Creates {@link ObjectInputFilter} a instance.
 */
public interface ObjectInputFilterFactory {

	/**
	 * @return a {@link ObjectInputFilter} instance
	 */
	ObjectInputFilter createObjectInputFilter();

	/**
	 * Returns the {@link ObjectInputFilterFactory} implementation found by the {@link ServiceLoader} of the given type.
	 * @param classname the classname of the required factory
	 * @return a {@link ObjectInputFilterFactory} implementation of the given type from the {@link ServiceLoader}.
	 * @throws IllegalStateException in case no such {@link ObjectInputFilterFactory} implementation is available.
	 */
	static ObjectInputFilterFactory instance(String classname) {
		requireNonNull(classname, "classname");
		ServiceLoader<ObjectInputFilterFactory> loader = ServiceLoader.load(ObjectInputFilterFactory.class);
		for (ObjectInputFilterFactory factory : loader) {
			if (factory.getClass().getName().equals(classname)) {
				return factory;
			}
		}

		throw new IllegalStateException("No object input filter factory of type: " + classname + " available");
	}
}
