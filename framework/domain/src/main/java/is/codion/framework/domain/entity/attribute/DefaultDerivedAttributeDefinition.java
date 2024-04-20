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
package is.codion.framework.domain.entity.attribute;

import java.io.Serial;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;

final class DefaultDerivedAttributeDefinition<T> extends AbstractAttributeDefinition<T> implements DerivedAttributeDefinition<T> {

	@Serial
	private static final long serialVersionUID = 1;

	private final DerivedAttribute.Provider<T> valueProvider;
	private final List<Attribute<?>> sourceAttributes;

	private DefaultDerivedAttributeDefinition(DefaultDerivedAttributeDefinitionBuilder<T, ?> builder) {
		super(builder);
		this.valueProvider = builder.valueProvider;
		this.sourceAttributes = builder.sourceAttributes;
	}

	@Override
	public DerivedAttribute.Provider<T> valueProvider() {
		return valueProvider;
	}

	@Override
	public List<Attribute<?>> sourceAttributes() {
		return sourceAttributes;
	}

	@Override
	public boolean derived() {
		return true;
	}

	static final class DefaultDerivedAttributeDefinitionBuilder<T, B extends AttributeDefinition.Builder<T, B>>
					extends AbstractAttributeDefinitionBuilder<T, B> implements AttributeDefinition.Builder<T, B> {

		private final DerivedAttribute.Provider<T> valueProvider;
		private final List<Attribute<?>> sourceAttributes;

		DefaultDerivedAttributeDefinitionBuilder(Attribute<T> attribute, DerivedAttribute.Provider<T> valueProvider, Attribute<?>... sourceAttributes) {
			super(attribute);
			this.valueProvider = requireNonNull(valueProvider);
			if (sourceAttributes.length == 0) {
				throw new IllegalArgumentException("No source attributes, a derived attribute must be derived from one or more existing attributes");
			}
			for (Attribute<?> sourceAttribute : sourceAttributes) {
				if (!attribute.entityType().equals(sourceAttribute.entityType())) {
					throw new IllegalArgumentException("Source attribute must be from same entity as the derived column");
				}
				if (attribute.equals(sourceAttribute)) {
					throw new IllegalArgumentException("Derived attribute can not be derived from itself");
				}
			}
			this.sourceAttributes = asList(sourceAttributes);
		}

		@Override
		public B defaultValue(ValueSupplier<T> supplier) {
			throw new UnsupportedOperationException("A derived attribute can not have a default value");
		}

		@Override
		public B nullable(boolean nullable) {
			throw new UnsupportedOperationException("Can not set the nullable state of a derived attribute");
		}

		@Override
		public B maximumLength(int maximumLength) {
			throw new UnsupportedOperationException("Can not set the maximum length of a derived attribute");
		}

		@Override
		public B valueRange(Number minimumValue, Number maximumValue) {
			throw new UnsupportedOperationException("Can not set minimum or maximum value of a derived attribute");
		}

		@Override
		public AttributeDefinition<T> build() {
			return new DefaultDerivedAttributeDefinition<>(this);
		}
	}
}
