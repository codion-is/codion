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
 * Copyright (c) 2019 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.common.value;

import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.requireNonNull;

final class DefaultValue<T> extends AbstractValue<T> {

	private T value;

	private DefaultValue(DefaultBuilder<T> builder) {
		super(builder.nullValue, builder.notify);
		set(builder.initialValue);
		builder.validators.forEach(this::addValidator);
	}

	@Override
	public T get() {
		return value;
	}

	@Override
	protected void setValue(T value) {
		this.value = value;
	}

	static final class DefaultBuilder<T> implements Builder<T> {

		private final T nullValue;
		private final List<Validator<T>> validators = new ArrayList<>();

		private T initialValue;
		private Notify notify = Notify.WHEN_CHANGED;

		DefaultBuilder() {
			this.nullValue = null;
		}

		DefaultBuilder(T nullValue) {
			this.nullValue = requireNonNull(nullValue);
			this.initialValue = nullValue;
		}

		@Override
		public Builder<T> initialValue(T initialValue) {
			this.initialValue = initialValue == null ? nullValue : initialValue;
			return this;
		}

		@Override
		public Builder<T> notify(Notify notify) {
			this.notify = requireNonNull(notify);
			return this;
		}

		@Override
		public Builder<T> validator(Validator<T> validator) {
			this.validators.add(requireNonNull(validator));
			return this;
		}

		@Override
		public Value<T> build() {
			return new DefaultValue<>(this);
		}
	}
}
