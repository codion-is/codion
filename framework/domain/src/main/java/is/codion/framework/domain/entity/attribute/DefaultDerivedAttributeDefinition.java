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
 * Copyright (c) 2019 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.framework.domain.entity.attribute;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;

final class DefaultDerivedAttributeDefinition<T> extends AbstractAttributeDefinition<T> implements DerivedAttributeDefinition<T> {

	@Serial
	private static final long serialVersionUID = 1;

	private final DerivedAttribute.Provider<T> provider;
	private final List<Attribute<?>> sources;
	private final boolean cached;

	private DefaultDerivedAttributeDefinition(DefaultDerivedAttributeDefinitionBuilder<T, ?> builder) {
		super(builder);
		this.provider = builder.provider;
		this.sources = builder.sources;
		this.cached = builder.cached;
	}

	@Override
	public DerivedAttribute.Provider<T> provider() {
		return provider;
	}

	@Override
	public List<Attribute<?>> sources() {
		return sources;
	}

	@Override
	public boolean cached() {
		return cached;
	}

	@Override
	public boolean derived() {
		return true;
	}

	static final class DefaultValueProviderStage<T, B extends DerivedAttributeDefinition.Builder<T, B>>
					implements DerivedAttributeDefinition.Builder.ProviderStage<T, B> {

		private final Attribute<T> attribute;
		private final List<Attribute<?>> sources;

		DefaultValueProviderStage(Attribute<T> attribute, List<Attribute<?>> sources) {
			this.attribute = requireNonNull(attribute);
			this.sources = requireNonNull(sources);
		}

		@Override
		public DerivedAttributeDefinition.Builder<T, B> provider(DerivedAttribute.Provider<T> provider) {
			return new DefaultDerivedAttributeDefinitionBuilder<>(attribute, sources, provider);
		}
	}

	static final class DefaultDerivedAttributeDefinitionBuilder<T, B extends DerivedAttributeDefinition.Builder<T, B>>
					extends AbstractAttributeDefinitionBuilder<T, B> implements DerivedAttributeDefinition.Builder<T, B> {

		private final DerivedAttribute.Provider<T> provider;
		private final List<Attribute<?>> sources;

		private boolean cached;

		DefaultDerivedAttributeDefinitionBuilder(Attribute<T> attribute,
																						 List<Attribute<?>> sources,
																						 DerivedAttribute.Provider<T> provider) {
			super(attribute);
			this.provider = requireNonNull(provider);
			for (Attribute<?> sourceAttribute : sources) {
				if (!attribute.entityType().equals(sourceAttribute.entityType())) {
					throw new IllegalArgumentException("Source attribute must be from same entity as the derived attribute");
				}
				if (attribute.equals(sourceAttribute)) {
					throw new IllegalArgumentException("Attribute can not be derived from itself");
				}
			}
			this.sources = unmodifiableList(new ArrayList<>(sources));
			this.cached = !denormalized(provider) && !this.sources.isEmpty();
		}

		@Override
		public DerivedAttributeDefinition.Builder<T, B> cached(boolean cached) {
			if (cached && denormalized(provider)) {
				throw new IllegalArgumentException("Denormalized attribute values can not be cached");
			}
			this.cached = cached;
			return self();
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

		private static boolean denormalized(DerivedAttribute.Provider<?> valueProvider) {
			return valueProvider instanceof DenormalizedValueProvider<?>;
		}
	}
}
