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
package is.codion.common.item;

import static java.util.Objects.requireNonNull;
import static java.util.ResourceBundle.getBundle;

final class ItemI18n<T> extends AbstractItem<T> {

	private static final long serialVersionUID = 1;

	private final String resourceBundleName;
	private final String resourceBundleKey;

	private transient String caption;

	ItemI18n(T value, String resourceBundleName, String resourceBundleKey) {
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
