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

import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.attribute.DerivedAttributeDefinition.DenormalizedBuilder.DenormalizedFromStep;
import is.codion.framework.domain.entity.attribute.DerivedAttributeDefinition.DenormalizedBuilder.DenormalizedUsingStep;
import is.codion.framework.domain.entity.attribute.DerivedAttributeDefinition.DerivedBuilder.DerivedFromStep;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;

final class DefaultDerivedAttributeDefinition<T> extends AbstractValueAttributeDefinition<T> implements DerivedAttributeDefinition<T> {

	@Serial
	private static final long serialVersionUID = 1;

	private final DerivedValue<T> value;
	private final List<Attribute<?>> sources;
	private final boolean cached;

	private DefaultDerivedAttributeDefinition(DefaultDerivedAttributeDefinitionBuilder<T, ?> builder) {
		super(builder);
		this.value = builder.derivedValue;
		this.sources = builder.sources;
		this.cached = builder.cached;
	}

	private DefaultDerivedAttributeDefinition(DefaultDenormalizedAttributeDefinitionBuilder<T, ?> builder) {
		super(builder);
		this.sources = singletonList(builder.source);
		this.value = builder.denormalizedValue;
		this.cached = false;
	}

	@Override
	public DerivedValue<T> value() {
		return value;
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

	static final class DefaultDerivedFromStep<T, B extends DerivedBuilder<T, B>> implements DerivedFromStep<T, B> {

		private final Attribute<T> attribute;

		DefaultDerivedFromStep(Attribute<T> attribute) {
			this.attribute = requireNonNull(attribute);
		}

		@Override
		public DerivedBuilder.DerivedWithStep<T, B> from(Attribute<?>... attributes) {
			return new DefaultDerivedWithStep<>(attribute, asList(attributes));
		}
	}

	static final class DefaultDerivedWithStep<T, B extends DerivedBuilder<T, B>>
					implements DerivedBuilder.DerivedWithStep<T, B> {

		private final Attribute<T> attribute;
		private final List<Attribute<?>> sources;

		private DefaultDerivedWithStep(Attribute<T> attribute, List<Attribute<?>> sources) {
			this.attribute = requireNonNull(attribute);
			this.sources = requireNonNull(sources);
		}

		@Override
		public DerivedBuilder<T, B> with(DerivedValue<T> value) {
			return new DefaultDerivedAttributeDefinitionBuilder<>(attribute, sources, value);
		}
	}

	static final class DefaultDerivedAttributeDefinitionBuilder<T, B extends DerivedBuilder<T, B>>
					extends AbstractValueAttributeDefinition.AbstractValueAttributeDefinitionBuilder<T, B> implements DerivedBuilder<T, B> {

		private final DerivedValue<T> derivedValue;
		private final List<Attribute<?>> sources;

		private boolean cached;

		DefaultDerivedAttributeDefinitionBuilder(Attribute<T> attribute,
																						 List<Attribute<?>> sources,
																						 DerivedValue<T> derivedValue) {
			super(attribute);
			this.derivedValue = requireNonNull(derivedValue);
			for (Attribute<?> sourceAttribute : sources) {
				if (!attribute.entityType().equals(sourceAttribute.entityType())) {
					throw new IllegalArgumentException("Source attribute must be from same entity as the derived attribute");
				}
				if (attribute.equals(sourceAttribute)) {
					throw new IllegalArgumentException("Attribute can not be derived from itself");
				}
			}
			this.sources = unmodifiableList(new ArrayList<>(sources));
			this.cached = !sources.isEmpty();
		}

		@Override
		public DerivedBuilder<T, B> cached(boolean cached) {
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
		public B range(Number minimum, Number maximum) {
			throw new UnsupportedOperationException("Can not set minimum or maximum value of a derived attribute");
		}

		@Override
		public ValueAttributeDefinition<T> build() {
			return new DefaultDerivedAttributeDefinition<>(this);
		}
	}

	static final class DefaultDenormalizedFromStep<T, B extends DenormalizedBuilder<T, B>> implements DenormalizedFromStep<T, B> {

		private final Attribute<T> attribute;

		DefaultDenormalizedFromStep(Attribute<T> attribute) {
			this.attribute = requireNonNull(attribute);
		}

		@Override
		public DenormalizedUsingStep<T, B> from(Attribute<Entity> source) {
			return new DefaultDenormalizedUsingStep<>(attribute, source);
		}
	}

	static final class DefaultDenormalizedUsingStep<T, B extends DenormalizedBuilder<T, B>> implements DenormalizedUsingStep<T, B> {

		private final Attribute<T> attribute;
		private final Attribute<Entity> source;

		private DefaultDenormalizedUsingStep(Attribute<T> attribute, Attribute<Entity> source) {
			this.attribute = requireNonNull(attribute);
			this.source = requireNonNull(source);
		}

		@Override
		public DenormalizedBuilder<T, B> using(Attribute<T> denormalized) {
			return new DefaultDenormalizedAttributeDefinitionBuilder<>(attribute, source, denormalized);
		}
	}

	static final class DefaultDenormalizedAttributeDefinitionBuilder<T, B extends DenormalizedBuilder<T, B>>
					extends AbstractValueAttributeDefinition.AbstractValueAttributeDefinitionBuilder<T, B> implements DenormalizedBuilder<T, B> {

		private final Attribute<Entity> source;
		private final DenormalizedValue<T> denormalizedValue;

		DefaultDenormalizedAttributeDefinitionBuilder(Attribute<T> attribute,
																									Attribute<Entity> source,
																									Attribute<T> denormalized) {
			super(attribute);
			if (!attribute.entityType().equals(source.entityType())) {
				throw new IllegalArgumentException("Source attribute must be from same entity as the denormalized attribute");
			}
			if (attribute.equals(source)) {
				throw new IllegalArgumentException("Attribute can not be denormalized from itself");
			}
			this.source = source;
			this.denormalizedValue = new DenormalizedValue<>(source, denormalized);
		}

		@Override
		public B defaultValue(ValueSupplier<T> supplier) {
			throw new UnsupportedOperationException("A denormalized attribute can not have a default value");
		}

		@Override
		public B nullable(boolean nullable) {
			throw new UnsupportedOperationException("Can not set the nullable state of a denormalized attribute");
		}

		@Override
		public B maximumLength(int maximumLength) {
			throw new UnsupportedOperationException("Can not set the maximum length of a denormalized attribute");
		}

		@Override
		public B range(Number minimum, Number maximum) {
			throw new UnsupportedOperationException("Can not set minimum or maximum value of a denormalized attribute");
		}

		@Override
		public ValueAttributeDefinition<T> build() {
			return new DefaultDerivedAttributeDefinition<>(this);
		}
	}
}
