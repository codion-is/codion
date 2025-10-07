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
 * Copyright (c) 2016 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.framework.db;

import is.codion.framework.db.EntityConnection.Select;
import is.codion.framework.db.TestDomain.Department;
import is.codion.framework.db.TestDomain.Employee;
import is.codion.framework.domain.entity.OrderBy;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.condition.Condition;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SelectTest")
public final class SelectTest {

	@Nested
	@DisplayName("Basic Select Building")
	class BasicSelectBuilding {

		@Test
		@DisplayName("select with where condition builds correctly")
		void select_whereCondition_buildsCorrectly() {
			Condition condition = Department.LOCATION.equalTo("New York");
			Select select = Select.where(condition).build();

			assertEquals(condition, select.where());
			assertEquals(Condition.all(Department.TYPE), select.having());
			assertFalse(select.limit().isPresent());
			assertFalse(select.offset().isPresent());
			assertFalse(select.forUpdate());
			assertFalse(select.orderBy().isPresent());
		}

		@Test
		@DisplayName("select all with entity type builds correctly")
		void select_allWithEntityType_buildsCorrectly() {
			Select select = Select.all(Department.TYPE).build();

			assertEquals(Condition.all(Department.TYPE), select.where());
			assertEquals(Condition.all(Department.TYPE), select.having());
			assertFalse(select.limit().isPresent());
		}
	}

	@Nested
	@DisplayName("Query Configuration")
	class QueryConfiguration {

		@Test
		@DisplayName("limit configuration works correctly")
		void select_limit_configuresCorrectly() {
			Select select = Select.all(Department.TYPE)
							.limit(10)
							.build();

			assertTrue(select.limit().isPresent());
			assertEquals(10, select.limit().orElse(-1));

			// Test null limit clears it
			select = Select.all(Department.TYPE)
							.limit(10)
							.limit(null)
							.build();
			assertFalse(select.limit().isPresent());
		}

		@Test
		@DisplayName("offset configuration works correctly")
		void select_offset_configuresCorrectly() {
			Select select = Select.all(Department.TYPE)
							.offset(20)
							.build();

			assertTrue(select.offset().isPresent());
			assertEquals(20, select.offset().orElse(-1));

			// Test null offset clears it
			select = Select.all(Department.TYPE)
							.offset(20)
							.offset(null)
							.build();
			assertFalse(select.offset().isPresent());
		}

		@Test
		@DisplayName("query timeout validation and configuration")
		void select_queryTimeout_validatesAndConfigures() {
			// Negative timeout should throw
			assertThrows(IllegalArgumentException.class,
							() -> Select.all(Department.TYPE).timeout(-1));

			// Valid timeout should work
			Select select = Select.all(Department.TYPE)
							.timeout(30)
							.build();

			assertEquals(30, select.timeout());

			// Default timeout
			select = Select.all(Department.TYPE).build();
			assertEquals(120, select.timeout());
		}
	}

	@Nested
	@DisplayName("OrderBy Configuration")
	class OrderByConfiguration {

		@Test
		@DisplayName("orderBy configuration works correctly")
		void select_orderBy_configuresCorrectly() {
			OrderBy orderBy = OrderBy.ascending(Department.NAME);
			Select select = Select.where(Department.LOCATION.equalTo("New York"))
							.orderBy(orderBy)
							.build();

			assertTrue(select.orderBy().isPresent());
			assertEquals(orderBy, select.orderBy().get());

			// Test null orderBy clears it
			select = Select.where(Department.LOCATION.equalTo("New York"))
							.orderBy(orderBy)
							.orderBy(null)
							.build();
			assertFalse(select.orderBy().isPresent());
		}
	}

	@Nested
	@DisplayName("Attribute Selection")
	class AttributeSelection {

		@Test
		@DisplayName("attributes varargs configuration")
		void select_attributesVarargs_configuresCorrectly() {
			Select select = Select.all(Employee.TYPE)
							.attributes(Employee.ID, Employee.NAME, Employee.DEPARTMENT)
							.build();

			Collection<Attribute<?>> attributes = select.attributes();
			assertEquals(3, attributes.size());
			assertTrue(attributes.contains(Employee.ID));
			assertTrue(attributes.contains(Employee.NAME));
			assertTrue(attributes.contains(Employee.DEPARTMENT));
		}

		@Test
		@DisplayName("attributes collection configuration")
		void select_attributesCollection_configuresCorrectly() {
			List<Attribute<?>> attributeList = Arrays.asList(Employee.ID, Employee.NAME, Employee.SALARY);
			Select select = Select.all(Employee.TYPE)
							.attributes(attributeList)
							.build();

			Collection<Attribute<?>> attributes = select.attributes();
			assertEquals(3, attributes.size());
			assertTrue(attributes.contains(Employee.ID));
			assertTrue(attributes.contains(Employee.NAME));
			assertTrue(attributes.contains(Employee.SALARY));
		}

		@Test
		@DisplayName("empty attributes configuration")
		void select_emptyAttributes_returnsEmptyCollection() {
			// Empty varargs
			Select select = Select.all(Employee.TYPE)
							.attributes()
							.build();
			assertTrue(select.attributes().isEmpty());

			// Empty collection
			select = Select.all(Employee.TYPE)
							.attributes(emptyList())
							.build();
			assertTrue(select.attributes().isEmpty());
		}
	}

	@Nested
	@DisplayName("ForUpdate and Reference Depth")
	class ForUpdateAndReferenceDepth {

		@Test
		@DisplayName("forUpdate configuration sets reference depth to zero")
		void select_forUpdate_setsReferenceDepthToZero() {
			Select select = Select.all(Employee.TYPE)
							.forUpdate()
							.build();

			assertTrue(select.forUpdate());
			assertTrue(select.referenceDepth().isPresent());
			assertEquals(0, select.referenceDepth().orElse(-1));
		}

		@Test
		@DisplayName("reference depth configuration")
		void select_referenceDepth_configuresCorrectly() {
			Select select = Select.all(Employee.TYPE)
							.referenceDepth(2)
							.build();

			assertTrue(select.referenceDepth().isPresent());
			assertEquals(2, select.referenceDepth().orElse(-1));
		}

		@Test
		@DisplayName("foreign key specific reference depth")
		void select_foreignKeyReferenceDepth_configuresCorrectly() {
			Select select = Select.all(Employee.TYPE)
							.referenceDepth(Employee.DEPARTMENT_FK, 3)
							.build();

			assertEquals(3, select.referenceDepth(Employee.DEPARTMENT_FK).orElse(0));

			// Test default when not set
			assertFalse(select.referenceDepth(Employee.MGR_FK).isPresent());
		}

		@Test
		@DisplayName("multiple foreign key reference depths")
		void select_multipleForeignKeyDepths_configuresCorrectly() {
			Select select = Select.all(Employee.TYPE)
							.referenceDepth(Employee.DEPARTMENT_FK, 2)
							.referenceDepth(Employee.MGR_FK, 0)
							.build();

			assertEquals(2, select.referenceDepth(Employee.DEPARTMENT_FK).orElse(0));
			assertEquals(0, select.referenceDepth(Employee.MGR_FK).orElse(-1));
		}
	}

	@Nested
	@DisplayName("Having Clause")
	class HavingClause {

		@Test
		@DisplayName("having clause configuration")
		void select_having_configuresCorrectly() {
			Condition havingCondition = Employee.SALARY.greaterThan(50000d);
			Select select = Select.having(havingCondition).build();

			assertEquals(havingCondition, select.having());
		}

		@Test
		@DisplayName("having clause defaults to all condition")
		void select_havingDefaults_toAllCondition() {
			Select select = Select.all(Employee.TYPE).build();
			assertEquals(Condition.all(Employee.TYPE), select.having());
		}
	}

	@Nested
	@DisplayName("Equality and HashCode")
	class EqualityAndHashCode {

		@Test
		@DisplayName("equals with same conditions")
		void select_equals_withSameConditions() {
			Condition condition1 = Employee.NAME.in("Luke", "John");
			Condition condition2 = Employee.NAME.in("Luke", "John");

			assertEquals(Select.where(condition1).build(),
							Select.where(condition2).build());
		}

		@Test
		@DisplayName("equals with orderBy")
		void select_equals_withOrderBy() {
			Condition condition = Employee.NAME.in("Luke", "John");
			OrderBy orderBy = OrderBy.ascending(Employee.NAME);

			assertEquals(
							Select.where(condition).orderBy(orderBy).build(),
							Select.where(condition).orderBy(orderBy).build()
			);

			assertNotEquals(
							Select.where(condition).orderBy(orderBy).build(),
							Select.where(condition).build()
			);
		}

		@Test
		@DisplayName("equals with attributes")
		void select_equals_withAttributes() {
			Condition condition = Employee.NAME.in("Luke", "John");

			assertEquals(
							Select.where(condition).attributes(Employee.NAME).build(),
							Select.where(condition).attributes(Employee.NAME).build()
			);

			assertNotEquals(
							Select.where(condition).attributes(Employee.NAME).build(),
							Select.where(condition).attributes(Employee.ID).build()
			);
		}

		@Test
		@DisplayName("equals with offset")
		void select_equals_withOffset() {
			Condition condition = Employee.NAME.in("Luke", "John");

			assertEquals(
							Select.where(condition).offset(10).build(),
							Select.where(condition).offset(10).build()
			);

			assertNotEquals(
							Select.where(condition).offset(10).build(),
							Select.where(condition).build()
			);
		}

		@Test
		@DisplayName("hashCode consistency")
		void select_hashCode_consistency() {
			Condition condition = Employee.NAME.equalTo("Test");
			Select select1 = Select.where(condition).limit(10).build();
			Select select2 = Select.where(condition).limit(10).build();

			assertEquals(select1.hashCode(), select2.hashCode());
		}
	}

	@Nested
	@DisplayName("ToString")
	class ToStringTest {

		@Test
		@DisplayName("toString includes all properties")
		void select_toString_includesAllProperties() {
			Select select = Select.where(Employee.NAME.equalTo("Test"))
							.orderBy(OrderBy.ascending(Employee.NAME))
							.limit(10)
							.offset(5)
							.forUpdate()
							.attributes(Employee.ID, Employee.NAME)
							.timeout(30)
							.build();

			String toString = select.toString();
			assertTrue(toString.contains("where="));
			assertTrue(toString.contains("orderBy="));
			assertTrue(toString.contains("limit=10"));
			assertTrue(toString.contains("offset=5"));
			assertTrue(toString.contains("forUpdate=true"));
			assertTrue(toString.contains("attributes="));
			assertTrue(toString.contains("timeout=30"));
		}
	}
}
