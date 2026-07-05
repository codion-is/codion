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
 * Copyright (c) 2026, Björn Darri Sigurðsson.
 */
package is.codion.common.model.worker;

import is.codion.common.utilities.exceptions.Exceptions;

import java.util.Iterator;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

/**
 * Lazily resolves and caches the {@link Dispatcher} implementation. The platform dispatcher does not
 * change at runtime, so it is resolved once, on first use, via the thread-safe class initialization guarantee.
 */
final class DispatcherHolder {

	static final Dispatcher INSTANCE = load();

	private DispatcherHolder() {}

	private static Dispatcher load() {
		try {
			Iterator<Dispatcher> iterator = ServiceLoader.load(Dispatcher.class).iterator();

			return iterator.hasNext() ? iterator.next() : Dispatcher.SYNCHRONOUS;
		}
		catch (ServiceConfigurationError e) {
			throw Exceptions.runtime(e, ServiceConfigurationError.class);
		}
	}
}
