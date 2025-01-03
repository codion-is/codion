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

import java.io.Serial;
import java.io.Serializable;

import static java.util.Objects.requireNonNull;
import static java.util.ResourceBundle.getBundle;

final class ItemI18n<T> extends AbstractItem<T> implements Serializable {

	@Serial
	private static final long serialVersionUID = 1;

	private final String resourceBundleName;
	private final String resourceBundleKey;

	private transient @Nullable String caption;

	ItemI18n(@Nullable T value, String resourceBundleName, String resourceBundleKey) {
		super(value);
		getBundle(requireNonNull(resourceBundleName)).getString(requireNonNull(resourceBundleKey));
		this.resourceBundleName = resourceBundleName;
		this.resourceBundleKey = resourceBundleKey;
	}

	@Override
	public String caption() {
		if (caption == null) {
			caption = getBundle(resourceBundleName).getString(resourceBundleKey);
		}

		return caption;
	}
}
