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
 * Copyright (c) 2026, Björn Darri Sigurðsson.
 */
package is.codion.framework.domain.entity;

import is.codion.framework.domain.entity.EntityFormatter.Builder;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.ForeignKey;

import org.jspecify.annotations.Nullable;

import java.io.Serial;
import java.io.Serializable;
import java.text.Format;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;

final class DefaultEntityFormatterBuilder implements Builder {

	private final List<Function<Entity, String>> valueProviders = new ArrayList<>();

	private @Nullable EntityType entityType;

	@Override
	public Builder value(Attribute<?> attribute) {
		validateEntityType(attribute);
		valueProviders.add(new StringValueProvider(attribute));
		return this;
	}

	@Override
	public Builder value(Attribute<?> attribute, Format format) {
		validateEntityType(attribute);
		valueProviders.add(new FormattedValueProvider(attribute, format));
		return this;
	}

	@Override
	public Builder value(ForeignKey foreignKey, Attribute<?> attribute) {
		validateEntityType(foreignKey);
		valueProviders.add(new ForeignKeyValueProvider(foreignKey, attribute));
		return this;
	}

	@Override
	public Builder text(String text) {
		valueProviders.add(new StaticTextProvider(text));
		return this;
	}

	@Override
	public EntityFormatter build() {
		return new DefaultEntityFormatter(this);
	}

	private void validateEntityType(Attribute<?> attribute) {
		if (entityType == null) {
			entityType = attribute.entityType();
		}
		else if (!attribute.entityType().equals(entityType)) {
			throw new IllegalArgumentException("entityType " + entityType + " expected, got: " + attribute.entityType());
		}
	}

	private static final class FormattedValueProvider implements Function<Entity, String>, Serializable {

		@Serial
		private static final long serialVersionUID = 1;

		private final Attribute<?> attribute;
		private final Format format;

		private FormattedValueProvider(Attribute<?> attribute, Format format) {
			this.attribute = requireNonNull(attribute);
			this.format = requireNonNull(format);
		}

		@Override
		public String apply(Entity entity) {
			if (!entity.present(attribute)) {
				return "";
			}

			return format.format(entity.get(attribute));
		}
	}

	private static final class ForeignKeyValueProvider implements Function<Entity, String>, Serializable {

		@Serial
		private static final long serialVersionUID = 1;

		private final ForeignKey foreignKey;
		private final Attribute<?> attribute;

		private ForeignKeyValueProvider(ForeignKey foreignKey, Attribute<?> attribute) {
			this.foreignKey = requireNonNull(foreignKey);
			this.attribute = requireNonNull(attribute);
			if (!attribute.entityType().equals(foreignKey.referencedType())) {
				throw new IllegalArgumentException("Attribute " + attribute + " is not part of entity: " + foreignKey.entityType());
			}
		}

		@Override
		public String apply(Entity entity) {
			if (!entity.present(foreignKey)) {
				return "";
			}

			return entity.entity(foreignKey).formatted(attribute);
		}
	}

	private static final class StringValueProvider implements Function<Entity, String>, Serializable {

		@Serial
		private static final long serialVersionUID = 1;

		private final Attribute<?> attribute;

		private StringValueProvider(Attribute<?> attribute) {
			this.attribute = requireNonNull(attribute);
		}

		@Override
		public String apply(Entity entity) {
			return entity.formatted(attribute);
		}
	}

	private static final class StaticTextProvider implements Function<Entity, String>, Serializable {

		@Serial
		private static final long serialVersionUID = 1;

		private final String text;

		private StaticTextProvider(String text) {
			this.text = requireNonNull(text);
		}

		@Override
		public String apply(Entity entity) {
			return text;
		}
	}

	private static final class DefaultEntityFormatter implements EntityFormatter {

		@Serial
		private static final long serialVersionUID = 1;

		/**
		 * Holds the ValueProviders used when constructing the String representation
		 */
		private final List<Function<Entity, String>> valueProviders;

		/**
		 * Instantiates a new {@link EntityFormatter} instance
		 */
		private DefaultEntityFormatter(DefaultEntityFormatterBuilder builder) {
			this.valueProviders = unmodifiableList(builder.valueProviders);
		}

		/**
		 * Returns a String representation of the given entity
		 * @param entity the entity, may not be null
		 * @return a String representation of the entity
		 */
		@Override
		public String apply(Entity entity) {
			requireNonNull(entity);
			if (valueProviders.size() == 1) {
				return valueProviders.get(0).apply(entity);
			}

			return valueProviders.stream()
							.map(valueProvider -> valueProvider.apply(entity))
							.collect(joining());
		}
	}
}
