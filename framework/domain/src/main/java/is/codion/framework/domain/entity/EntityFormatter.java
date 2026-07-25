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
package is.codion.framework.domain.entity;

import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.ForeignKey;

import java.io.Serializable;
import java.text.Format;
import java.util.function.Function;

/**
 * Formats {@link Entity} instances into their string representations.
 * Instances are built via {@link #builder()}.
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
 *  void testFormatter() {
 * 			Entity department = createDepartment();// With name: Accounting
 *  		Entity employee = createEmployee(department);// With name: John and the above department
 *
 * 			EntityFormatter formatter =
 * 					EntityFormatter.builder()
 *             .text("Name=")
 *             .value(Employee.NAME)
 *             .text(", Department='")
 *             .value(Employee.DEPARTMENT_FK, Department.NAME)
 *             .text("'")
 *             .build();
 *
 *  		System.out.println(formatter.apply(employee));
 * }
 *}
 * Outputs the following String:
 * <p>
 * {@code Name=John, Department='Accounting'}<br><br>
 * given the entities above.
 * </p>
 */
public interface EntityFormatter extends Function<Entity, String>, Serializable {

	/**
	 * @return a {@link Builder} instance for configuring a formatter {@link Function} for entities.
	 */
	static Builder builder() {
		return new DefaultEntityFormatterBuilder();
	}

	/**
	 * A Builder for a formatter, which provides toString() values for entities.
	 */
	sealed interface Builder permits DefaultEntityFormatterBuilder {

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
		 * @return a new EntityFormatter based on this builder
		 */
		EntityFormatter build();
	}
}
