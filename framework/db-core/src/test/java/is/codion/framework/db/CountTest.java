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
 * Copyright (c) 2025, Björn Darri Sigurðsson.
 */
package is.codion.framework.db;

import is.codion.framework.db.EntityConnection.Count;
import is.codion.framework.db.TestDomain.Department;
import is.codion.framework.db.TestDomain.Employee;
import is.codion.framework.domain.entity.condition.Condition;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static is.codion.framework.domain.entity.condition.Condition.and;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CountTest")
public final class CountTest {

	@Nested
	@DisplayName("Basic Count Building")
	class BasicCountBuilding {

		@Test
		@DisplayName("count with where condition builds correctly")
		void count_whereCondition_buildsCorrectly() {
			Condition condition = Department.LOCATION.equalTo("New York");
			Count count = Count.where(condition);

			assertEquals(condition, count.where());
			assertEquals(Condition.all(Department.TYPE), count.having());
		}

		@Test
		@DisplayName("count all with entity type builds correctly")
		void count_allWithEntityType_buildsCorrectly() {
			Count count = Count.all(Department.TYPE);

			assertEquals(Condition.all(Department.TYPE), count.where());
			assertEquals(Condition.all(Department.TYPE), count.having());
		}
	}

	@Nested
	@DisplayName("Having Clause")
	class HavingClause {

		@Test
		@DisplayName("having clause configuration")
		void count_having_configuresCorrectly() {
			Condition whereCondition = Employee.DEPARTMENT.equalTo(10);
			Condition havingCondition = Employee.SALARY.greaterThan(50000d);

			Count count = Count.builder(whereCondition)
							.having(havingCondition)
							.build();

			assertEquals(whereCondition, count.where());
			assertEquals(havingCondition, count.having());
		}

		@Test
		@DisplayName("having clause defaults to all condition")
		void count_havingDefaults_toAllCondition() {
			Count count = Count.all(Employee.TYPE);
			assertEquals(Condition.all(Employee.TYPE), count.having());
		}

		@Test
		@DisplayName("null having throws NullPointerException")
		void count_nullHaving_throwsNullPointerException() {
			assertThrows(NullPointerException.class, () ->
							Count.builder(Condition.all(Employee.TYPE)).having(null));
		}
	}

	@Nested
	@DisplayName("Complex Conditions")
	class ComplexConditions {

		@Test
		@DisplayName("count with complex where condition")
		void count_complexWhere_buildsCorrectly() {
			Condition condition = and(
							Employee.DEPARTMENT.equalTo(20),
							Employee.JOB.in("CLERK", "ANALYST"),
							Employee.SALARY.between(30000d, 50000d)
			);

			Count count = Count.where(condition);
			assertEquals(condition, count.where());
		}

		@Test
		@DisplayName("count with custom condition")
		void count_customCondition_buildsCorrectly() {
			// Using a custom condition (if available in test domain)
			Condition customCondition = and(Employee.COMMISSION.isNotNull(), Employee.COMMISSION.greaterThan(100d));

			Count count = Count.where(customCondition);
			assertEquals(customCondition, count.where());
		}
	}

	@Nested
	@DisplayName("ToString")
	class ToStringTest {

		@Test
		@DisplayName("toString includes where and having")
		void count_toString_includesWhereAndHaving() {
			Condition whereCondition = Employee.NAME.like("J%");
			Condition havingCondition = Employee.SALARY.greaterThan(40000d);

			Count count = Count.builder(whereCondition)
							.having(havingCondition)
							.build();

			String toString = count.toString();
			assertTrue(toString.contains("where="));
			assertTrue(toString.contains("having="));
			assertTrue(toString.contains("J%"));
		}
	}

	@Nested
	@DisplayName("Entity Type Consistency")
	class EntityTypeConsistency {

		@Test
		@DisplayName("where and having conditions use same entity type")
		void count_whereAndHaving_sameEntityType() {
			Condition whereCondition = Employee.DEPARTMENT.equalTo(10);
			Count count = Count.where(whereCondition);

			// Default having should be for same entity type as where
			assertEquals(whereCondition.entityType(), count.having().entityType());
		}
	}

	@Nested
	@DisplayName("Builder Chaining")
	class BuilderChaining {

		@Test
		@DisplayName("builder methods can be chained")
		void count_builderChaining_worksCorrectly() {
			Condition havingCondition = Employee.SALARY.greaterThan(30000d);

			Count count = Count.builder(Condition.all(Employee.TYPE))
							.having(havingCondition)
							.build();

			assertEquals(Condition.all(Employee.TYPE), count.where());
			assertEquals(havingCondition, count.having());
		}

		@Test
		@DisplayName("having can be changed multiple times")
		void count_havingMultipleCalls_lastOneWins() {
			Condition having1 = Employee.SALARY.greaterThan(30000d);
			Condition having2 = Employee.SALARY.greaterThan(40000d);
			Condition having3 = Employee.SALARY.greaterThan(50000d);

			Count count = Count.builder(Condition.all(Employee.TYPE))
							.having(having1)
							.having(having2)
							.having(having3)
							.build();

			assertEquals(having3, count.having());
		}
	}
}