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
package is.codion.framework.domain.entity.attribute;

import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.DefaultDerivedAttributeDefinition.DefaultProviderBuilder;
import is.codion.framework.domain.entity.attribute.DefaultTransientAttributeDefinition.DefaultTransientAttributeDefinitionBuilder;
import is.codion.framework.domain.entity.attribute.DerivedAttributeDefinition.Builder;

import org.jspecify.annotations.Nullable;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.temporal.Temporal;
import java.util.Objects;

import static is.codion.common.Text.nullOrEmpty;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;

final class DefaultAttribute<T> implements Attribute<T>, Serializable {

	@Serial
	private static final long serialVersionUID = 1;

	private final String name;
	private final DefaultType<T> type;
	private final int hashCode;

	DefaultAttribute(String name, Class<T> valueClass, EntityType entityType) {
		if (nullOrEmpty(name)) {
			throw new IllegalArgumentException("name must be a non-empty string");
		}
		this.name = name;
		this.type = new DefaultType<>(requireNonNull(entityType), requireNonNull(valueClass));
		this.hashCode = Objects.hash(name, entityType);
	}

	@Override
	public AttributeDefiner<T> define() {
		return new DefaultAttributeDefiner<>(this);
	}

	@Override
	public Type<T> type() {
		return type;
	}

	@Override
	public String name() {
		return name;
	}

	@Override
	public EntityType entityType() {
		return type.entityType;
	}

	@Override
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}
		if (!(object instanceof DefaultAttribute)) {
			return false;
		}
		DefaultAttribute<?> that = (DefaultAttribute<?>) object;

		return hashCode == that.hashCode && name.equals(that.name) && type.entityType.equals(that.type.entityType);
	}

	@Override
	public int hashCode() {
		return hashCode;
	}

	@Override
	public String toString() {
		return type.entityType.name() + "." + name;
	}

	private final class DefaultType<T> implements Type<T>, Serializable {

		@Serial
		private static final long serialVersionUID = 1;

		private final EntityType entityType;
		private final Class<T> valueClass;

		private DefaultType(EntityType entityType, Class<T> valueClass) {
			this.entityType = entityType;
			this.valueClass = valueClass;
		}

		@Override
		public Class<T> valueClass() {
			return valueClass;
		}

		@Override
		public @Nullable T validateType(@Nullable T value) {
			if (value != null && valueClass != value.getClass() && !valueClass.isAssignableFrom(value.getClass())) {
				throw new IllegalArgumentException("Value of type " + valueClass +
								" expected for attribute " + DefaultAttribute.this + ", got: " + value.getClass());
			}

			return value;
		}

		@Override
		public boolean isNumerical() {
			return Number.class.isAssignableFrom(valueClass);
		}

		@Override
		public boolean isTemporal() {
			return Temporal.class.isAssignableFrom(valueClass);
		}

		@Override
		public boolean isLocalDate() {
			return isType(LocalDate.class);
		}

		@Override
		public boolean isLocalDateTime() {
			return isType(LocalDateTime.class);
		}

		@Override
		public boolean isLocalTime() {
			return isType(LocalTime.class);
		}

		@Override
		public boolean isOffsetDateTime() {
			return isType(OffsetDateTime.class);
		}

		@Override
		public boolean isCharacter() {
			return isType(Character.class);
		}

		@Override
		public boolean isString() {
			return isType(String.class);
		}

		@Override
		public boolean isLong() {
			return isType(Long.class);
		}

		@Override
		public boolean isInteger() {
			return isType(Integer.class);
		}

		@Override
		public boolean isShort() {
			return isType(Short.class);
		}

		@Override
		public boolean isDouble() {
			return isType(Double.class);
		}

		@Override
		public boolean isBigDecimal() {
			return isType(BigDecimal.class);
		}

		@Override
		public boolean isDecimal() {
			return isDouble() || isBigDecimal();
		}

		@Override
		public boolean isBoolean() {
			return isType(Boolean.class);
		}

		@Override
		public boolean isByteArray() {
			return isType(byte[].class);
		}

		@Override
		public boolean isEnum() {
			return valueClass.isEnum();
		}

		@Override
		public boolean isEntity() {
			return isType(Entity.class);
		}

		private boolean isType(Class<?> valueClass) {
			return this.valueClass.equals(valueClass);
		}
	}

	static class DefaultAttributeDefiner<T> implements AttributeDefiner<T> {

		private final Attribute<T> attribute;

		DefaultAttributeDefiner(Attribute<T> attribute) {
			this.attribute = attribute;
		}

		@Override
		public final <B extends TransientAttributeDefinition.Builder<T, B>> TransientAttributeDefinition.Builder<T, B> attribute() {
			return new DefaultTransientAttributeDefinitionBuilder<>(attribute);
		}

		@Override
		public final <B extends DerivedAttributeDefinition.Builder<T, B>> DerivedAttributeDefinition.Builder<T, B> denormalized(Attribute<Entity> entityAttribute,
																																																														Attribute<T> denormalizedAttribute) {
			return (DerivedAttributeDefinition.Builder<T, B>) new DefaultProviderBuilder<>(attribute, singletonList(entityAttribute))
							.provider(new DenormalizedValueProvider<>(entityAttribute, denormalizedAttribute));
		}

		@Override
		public final <B extends DerivedAttributeDefinition.Builder<T, B>> Builder.ProviderBuilder<T, B> derived(Attribute<?>... from) {
			return new DefaultProviderBuilder<>(attribute, asList(from));
		}
	}
}
