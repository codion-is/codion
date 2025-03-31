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

import java.io.ObjectInputFilter;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

import static java.util.Objects.requireNonNull;
import static java.util.stream.StreamSupport.stream;

/**
 * Creates an {@link ObjectInputFilter} instance.
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
		requireNonNull(classname);
		try {
			return stream(ServiceLoader.load(ObjectInputFilterFactory.class).spliterator(), false)
							.filter(factory -> factory.getClass().getName().equals(classname))
							.findFirst()
							.orElseThrow(() -> new IllegalStateException("No object input filter factory of type: " + classname + " available"));
		}
		catch (ServiceConfigurationError e) {
			Throwable cause = e.getCause();
			if (cause instanceof RuntimeException) {
				throw (RuntimeException) cause;
			}
			throw new RuntimeException(cause);
		}
	}
}
