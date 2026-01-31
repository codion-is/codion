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
 * Copyright (c) 2019 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.common.reactive.value;

import org.jspecify.annotations.Nullable;

final class DefaultValue<T> extends AbstractValue<T> {

	static final BuilderFactory BUILDER_FACTORY = new DefaultBuilderFactory();

	private @Nullable T value;

	private DefaultValue(DefaultBuilder<T, ?> builder) {
		super(builder);
	}

	@Override
	protected @Nullable T getValue() {
		return value;
	}

	@Override
	protected void setValue(@Nullable T value) {
		this.value = value;
	}

	private static final class DefaultBuilderFactory implements BuilderFactory {

		@Override
		public <T> Value.Builder<T, ?> nonNull(T nullValue) {
			return new DefaultBuilder<>(nullValue);
		}

		@Override
		public <T> Value.Builder<T, ?> nullable() {
			return new DefaultBuilder<>();
		}

		@Override
		public <T> Value.Builder<T, ?> nullable(@Nullable T value) {
			return (Value.Builder<T, ?>) new DefaultBuilder<>()
							.value(value);
		}
	}

	private static final class DefaultBuilder<T, B extends Value.Builder<T, B>>
					extends AbstractBuilder<T, B> implements Value.Builder<T, B> {

		private DefaultBuilder() {}

		private DefaultBuilder(T nullValue) {
			super(nullValue);
		}

		@Override
		public Value<T> build() {
			return new DefaultValue<>(this);
		}
	}
}
