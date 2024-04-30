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
package is.codion.common.resources;

import java.util.ResourceBundle;

import static java.util.Objects.requireNonNull;

/**
 * An overridable resource bundle.
 * @see Messages
 */
public interface MessageBundle {

	/**
	 * @param key the key
	 * @return the string associated with the given key
	 */
	String getString(String key);

	/**
	 * @param resourceOwner the resource owner
	 * @param bundle the resource bundle to override
	 * @return a new {@link MessageBundle} instance
	 */
	static MessageBundle messageBundle(Class<?> resourceOwner, ResourceBundle bundle) {
		return new DefaultMessageBundle(requireNonNull(resourceOwner), requireNonNull(bundle));
	}
}
