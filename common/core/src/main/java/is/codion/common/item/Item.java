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
 * Copyright (c) 2020 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.common.item;

import org.jspecify.annotations.Nullable;

/**
 * A class encapsulating a constant value and a caption representing the value.
 * {@link Item}s equality is based on their values only.
 * Factory for {@link Item} instances.
 * @param <T> the type of the value
 * @see #value()
 * @see Item#item(Object)
 * @see Item#item(Object, String)
 * @see Item#itemI18n(Object, String, String)
 */
public interface Item<T> {

	/**
	 * @return the caption
	 */
	String caption();

	/**
	 * @return the item value
	 */
	@Nullable T value();

	/**
	 * Returns an {@link Item}, with the caption as item.toString() or an empty string in case of a null value
	 * @param value the value, may be null
	 * @param <T> the value type
	 * @return an {@link Item} based on the given value
	 */
	static <T> Item<T> item(@Nullable T value) {
		if (value == null) {
			return (Item<T>) DefaultItem.NULL_ITEM;
		}

		return item(value, value.toString());
	}

	/**
	 * Creates a new {@link Item}.
	 * @param value the value, may be null
	 * @param caption the caption
	 * @param <T> the value type
	 * @return an {@link Item} based on the given value and caption
	 * @throws NullPointerException if caption is null
	 */
	static <T> Item<T> item(@Nullable T value, String caption) {
		return new DefaultItem<>(value, caption);
	}

	/**
	 * Creates a new {@link Item}, which gets its caption from a resource bundle.
	 * Note that the caption is cached, so that changing the {@link java.util.Locale} after the
	 * first time {@link Item#caption} is called will not change the caption.
	 * @param value the value, may be null
	 * @param resourceBundleName the resource bundle name
	 * @param resourceBundleKey the resource bundle key for the item caption
	 * @param <T> the value type
	 * @return an Item based on the given value and resource bundle
	 */
	static <T> Item<T> itemI18n(@Nullable T value, String resourceBundleName, String resourceBundleKey) {
		return new ItemI18n<>(value, resourceBundleName, resourceBundleKey);
	}
}
