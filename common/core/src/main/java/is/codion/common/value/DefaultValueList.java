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
package is.codion.common.value;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class DefaultValueList<T> extends DefaultValues<List<T>, T>
				implements ValueList<T> {

	private DefaultValueList(DefaultBuilder<T> builder) {
		super(builder);
	}

	@Override
	public synchronized ValueListObserver<T> observer() {
		return (ValueListObserver<T>) super.observer();
	}

	@Override
	protected ValueListObserver<T> createObserver() {
		return new DefaultValueListObserver<>(this);
	}

	static final class DefaultBuilder<T>
					extends DefaultValues.DefaultBuilder<List<T>, T, ValueList.Builder<T>>
					implements ValueList.Builder<T> {

		DefaultBuilder() {
			super(ArrayList::new, Collections::unmodifiableList);
		}

		@Override
		public ValueList<T> build() {
			return new DefaultValueList<>(this);
		}
	}
}
