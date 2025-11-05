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
package is.codion.common.utilities.resource;

/**
 * Provides overidden resource messages.
 */
public interface Resources {

	/**
	 * Returns a value for overriding the default resource value or the default string if no override is provided
	 * @param baseBundleName the base resource bundle name
	 * @param key the key
	 * @param defaultString the default string
	 * @return a value for overriding the default string or the default string if no override is provided
	 */
	String getString(String baseBundleName, String key, String defaultString);
}
