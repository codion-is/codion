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
 * Copyright (c) 2024 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.common.utilities.resource;

import is.codion.common.utilities.exceptions.Exceptions;

import java.util.Enumeration;
import java.util.ResourceBundle;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

import static java.util.Objects.requireNonNull;
import static java.util.stream.StreamSupport.stream;

/**
 * An overridable resource bundle.
 * <p>
 * Unlike standard {@link ResourceBundle}, missing resource keys do not throw exceptions.
 * Instead, missing keys return a formatted fallback string in the format {@code "!missing_key!"}
 * to make missing resources obvious while preventing application crashes.
 * @see Resources
 */
public final class MessageBundle extends ResourceBundle {

	private static final Resources DEFAULT = new DefaultResources();
	private static final Resources RESOURCES = resources();

	private final String baseBundleName;
	private final ResourceBundle bundle;

	private MessageBundle(String baseBundleName, ResourceBundle bundle) {
		this.baseBundleName = requireNonNull(baseBundleName);
		this.bundle = bundle;
	}

	@Override
	public Enumeration<String> getKeys() {
		return bundle.getKeys();
	}

	/**
	 * @param resourceOwner the resource owner
	 * @param bundle the resource bundle to override
	 * @return a new {@link MessageBundle} instance
	 */
	public static MessageBundle messageBundle(Class<?> resourceOwner, ResourceBundle bundle) {
		return new MessageBundle(requireNonNull(resourceOwner).getName(), requireNonNull(bundle));
	}

	@Override
	protected Object handleGetObject(String key) {
		requireNonNull(key);
		if (!bundle.containsKey(key)) {
			// Provide a fallback to prevent application crashes
			// Return formatted key to make missing resources obvious
			// Format: "!missing_key!" - clearly indicates missing resource
			return RESOURCES.getString(baseBundleName, key, "!" + key + "!");
		}

		return RESOURCES.getString(baseBundleName, key, bundle.getString(key));
	}

	private static Resources resources() {
		try {
			return stream(ServiceLoader.load(Resources.class).spliterator(), false)
							.findFirst()
							.orElse(DEFAULT);
		}
		catch (ServiceConfigurationError e) {
			throw Exceptions.runtime(e, ServiceConfigurationError.class);
		}
	}

	private static final class DefaultResources implements Resources {

		@Override
		public String getString(String baseBundleName, String key, String defaultString) {
			return defaultString;
		}
	}
}
