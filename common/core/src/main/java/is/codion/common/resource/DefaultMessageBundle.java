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
package is.codion.common.resource;

import java.util.Enumeration;
import java.util.Iterator;
import java.util.ResourceBundle;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

import static java.util.Objects.requireNonNull;

final class DefaultMessageBundle extends ResourceBundle implements MessageBundle {

	private static final Messages DEFAULT = new DefaultMessages();

	private static final Messages INSTANCE = instance();

	private final Class<?> resourceOwner;
	private final ResourceBundle bundle;

	DefaultMessageBundle(Class<?> resourceOwner, ResourceBundle bundle) {
		this.resourceOwner = resourceOwner;
		this.bundle = bundle;
	}

	@Override
	public Enumeration<String> getKeys() {
		return bundle.getKeys();
	}

	@Override
	protected Object handleGetObject(String key) {
		requireNonNull(key);
		if (!bundle.containsKey(key)) {
			throw new IllegalArgumentException("Invalid resource key: '" + key
							+ "' for bundle: " + bundle.getBaseBundleName());
		}

		return INSTANCE.get(resourceOwner.getName(), key, bundle.getString(key));
	}

	private static Messages instance() {
		try {
			ServiceLoader<Messages> loader = ServiceLoader.load(Messages.class);
			Iterator<Messages> iterator = loader.iterator();
			if (iterator.hasNext()) {
				return iterator.next();
			}

			return DEFAULT;
		}
		catch (ServiceConfigurationError e) {
			Throwable cause = e.getCause();
			if (cause instanceof RuntimeException) {
				throw (RuntimeException) cause;
			}
			throw new RuntimeException(cause);
		}
	}

	private static final class DefaultMessages implements Messages {

		@Override
		public String get(String baseBundleName, String key, String defaultString) {
			return defaultString;
		}
	}
}
