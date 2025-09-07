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
package is.codion.framework.domain.entity;

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

/**
 * Factory class for building functions for String representations of {@link Entity} instances.
 * {@snippet :
 *  interface Department {
 *  		EntityType TYPE = DOMAIN.entityType("employees.department");
 *  		Column<Integer> ID = TYPE.integerColumn("id");
 *  		Column<String> NAME = TYPE.stringColumn("name");
 *  }
 *
 *  interface Employee {
 *  		EntityType TYPE = DOMAIN.entityType("employees.employee");
 *  		Column<String> NAME = TYPE.stringColumn("name");
 *  		Column<Integer> DEPARTMENT_ID = TYPE.integerColumn("department_id");
 *  		ForeignKey DEPARTMENT_FK = TYPE.foreignKey("department_fk", DEPARTMENT_ID, Department.ID);
 *  }
 *
 *  void testStringFactory() {
 * 			Entity department = createDepartment();// With name: Accounting
 *  		Entity employee = createEmployee(department);// With name: John and the above department
 *
 * 			Function<Entity, String> stringFactory =
 * 					StringFactory.builder()
 *             .text("Name=")
 *             .value(Employee.NAME)
 *             .text(", Department='")
 *             .value(Employee.DEPARTMENT_FK, Department.NAME)
 *             .text("'");
 *
 *  		System.out.println(stringFactory.apply(employee));
 * }
 *}
 * Outputs the following String:
 * <p>
 * {@code Name=John, Department='Accounting'}<br><br>
 * given the entities above.
 * </p>
 */
public final class StringFactory {

	private StringFactory() {}

	/**
	 * @return a {@link Builder} instance for configuring a string factory {@link Function} for entities.
	 */
	public static Builder builder() {
		return new DefaultStringFactoryBuilder();
	}

	/**
	 * A Builder for a string function, which provides toString() values for entities.
	 */
	public sealed interface Builder permits DefaultStringFactoryBuilder {

		/**
		 * Adds the value mapped to the given key to this {@link Builder}
		 * @param attribute the attribute which value should be added to the string representation
		 * @return this {@link Builder} instance
		 */
		Builder value(Attribute<?> attribute);

		/**
		 * Adds the value mapped to the given key to this StringProvider
		 * @param attribute the attribute which value should be added to the string representation
		 * @param format the Format to use when appending the value
		 * @return this {@link Builder} instance
		 */
		Builder value(Attribute<?> attribute, Format format);

		/**
		 * Adds the value mapped to the given attribute in the {@link Entity} instance mapped to the given foreign key
		 * to this {@link Builder}
		 * @param foreignKey the foreign key
		 * @param attribute the attribute in the referenced entity to use
		 * @return this {@link Builder} instance
		 */
		Builder value(ForeignKey foreignKey, Attribute<?> attribute);

		/**
		 * Adds the given static text to this {@link Builder}
		 * @param text the text to add
		 * @return this {@link Builder} instance
		 */
		Builder text(String text);

		/**
		 * @return a new string factory function based on this builder
		 */
		Function<Entity, String> build();
	}

	private static final class DefaultStringFactory implements Function<Entity, String>, Serializable {

		@Serial
		private static final long serialVersionUID = 1;

		/**
		 * Holds the ValueProviders used when constructing the String representation
		 */
		private final List<Function<Entity, String>> valueProviders;

		/**
		 * Instantiates a new {@link StringFactory} instance
		 */
		private DefaultStringFactory(DefaultStringFactoryBuilder builder) {
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
			if (entity.isNull(attribute)) {
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
			if (entity.isNull(foreignKey)) {
				return "";
			}

			return entity.entity(foreignKey).format(attribute);
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
			return entity.format(attribute);
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

	private static final class DefaultStringFactoryBuilder implements Builder {

		private final List<Function<Entity, String>> valueProviders = new ArrayList<>();

		private @Nullable EntityType entityType;

		@Override
		public Builder value(Attribute<?> attribute) {
			valueProviders.add(new StringValueProvider(attribute));
			validateEntityType(attribute);
			return this;
		}

		@Override
		public Builder value(Attribute<?> attribute, Format format) {
			valueProviders.add(new FormattedValueProvider(attribute, format));
			return this;
		}

		@Override
		public Builder value(ForeignKey foreignKey, Attribute<?> attribute) {
			valueProviders.add(new ForeignKeyValueProvider(foreignKey, attribute));
			return this;
		}

		@Override
		public Builder text(String text) {
			valueProviders.add(new StaticTextProvider(text));
			return this;
		}

		@Override
		public Function<Entity, String> build() {
			return new DefaultStringFactory(this);
		}

		private void validateEntityType(Attribute<?> attribute) {
			if (entityType == null) {
				entityType = attribute.entityType();
			}
			else if (!attribute.entityType().equals(entityType)) {
				throw new IllegalArgumentException("entityType " + entityType + " expected, got: " + attribute.entityType());
			}
		}
	}
}
