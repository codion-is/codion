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

import is.codion.framework.db.EntityConnection.Update;
import is.codion.framework.db.TestDomain.Department;
import is.codion.framework.db.TestDomain.Employee;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.condition.Condition;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("UpdateTest")
public final class UpdateTest {

	@Nested
	@DisplayName("Basic Update Building")
	class BasicUpdateBuilding {

		@Test
		@DisplayName("update with single value builds correctly")
		void update_singleValue_buildsCorrectly() {
			Condition condition = Employee.DEPARTMENT.equalTo(10);
			Update update = Update.where(condition)
					.set(Employee.SALARY, 55000d)
					.build();
			
			assertEquals(condition, update.where());
			Map<Column<?>, Object> values = update.values();
			assertEquals(1, values.size());
			assertEquals(55000d, values.get(Employee.SALARY));
		}

		@Test
		@DisplayName("update all with entity type builds correctly")
		void update_allWithEntityType_buildsCorrectly() {
			Update update = Update.all(Employee.TYPE)
					.set(Employee.COMMISSION, 500d)
					.build();
			
			assertEquals(Condition.all(Employee.TYPE), update.where());
			Map<Column<?>, Object> values = update.values();
			assertEquals(1, values.size());
			assertEquals(500d, values.get(Employee.COMMISSION));
		}

		@Test
		@DisplayName("update with multiple values builds correctly")
		void update_multipleValues_buildsCorrectly() {
			LocalDate now = LocalDate.now();
			Update update = Update.where(Employee.ID.equalTo(1))
					.set(Employee.NAME, "John Doe")
					.set(Employee.SALARY, 60000d)
					.set(Employee.HIREDATE, now)
					.set(Employee.JOB, "MANAGER")
					.build();
			
			Map<Column<?>, Object> values = update.values();
			assertEquals(4, values.size());
			assertEquals("John Doe", values.get(Employee.NAME));
			assertEquals(60000d, values.get(Employee.SALARY));
			assertEquals(now, values.get(Employee.HIREDATE));
			assertEquals("MANAGER", values.get(Employee.JOB));
		}
	}

	@Nested
	@DisplayName("Null Value Handling")
	class NullValueHandling {

		@Test
		@DisplayName("update with null value sets null correctly")
		void update_nullValue_setsNullCorrectly() {
			Update update = Update.where(Employee.ID.equalTo(1))
					.set(Employee.COMMISSION, null)
					.build();
			
			Map<Column<?>, Object> values = update.values();
			assertEquals(1, values.size());
			assertTrue(values.containsKey(Employee.COMMISSION));
			assertNull(values.get(Employee.COMMISSION));
		}

		@Test
		@DisplayName("update with mixed null and non-null values")
		void update_mixedNullAndNonNull_buildsCorrectly() {
			Update update = Update.where(Employee.ID.equalTo(1))
					.set(Employee.COMMISSION, null)
					.set(Employee.SALARY, 70000d)
					.set(Employee.MGR, null)
					.build();
			
			Map<Column<?>, Object> values = update.values();
			assertEquals(3, values.size());
			assertNull(values.get(Employee.COMMISSION));
			assertEquals(70000d, values.get(Employee.SALARY));
			assertNull(values.get(Employee.MGR));
		}
	}

	@Nested
	@DisplayName("Different Column Types")
	class DifferentColumnTypes {

		@Test
		@DisplayName("update with various column types")
		void update_variousColumnTypes_buildsCorrectly() {
			LocalDateTime hireDate = LocalDateTime.now();
			Update update = Update.where(Employee.ID.equalTo(1))
					// String
					.set(Employee.NAME, "Test Name")
					.set(Employee.JOB, "CLERK")
					// Numbers
					.set(Employee.SALARY, 45000d)
					.set(Employee.COMMISSION, 100d)
					.set(Employee.DEPARTMENT, 20)
					.set(Employee.MGR, 7839)
					// Date/Time
					.set(Employee.HIREDATE, hireDate)
					.build();
			
			Map<Column<?>, Object> values = update.values();
			assertEquals(7, values.size());
			assertEquals("Test Name", values.get(Employee.NAME));
			assertEquals("CLERK", values.get(Employee.JOB));
			assertEquals(45000d, values.get(Employee.SALARY));
			assertEquals(100d, values.get(Employee.COMMISSION));
			assertEquals(20, values.get(Employee.DEPARTMENT));
			assertEquals(7839, values.get(Employee.MGR));
			assertEquals(hireDate, values.get(Employee.HIREDATE));
		}

		@Test
		@DisplayName("update department with boolean and string")
		void update_departmentColumns_buildsCorrectly() {
			Update update = Update.where(Department.ID.equalTo(10))
					.set(Department.NAME, "ACCOUNTING")
					.set(Department.LOCATION, "NEW YORK")
					.build();
			
			Map<Column<?>, Object> values = update.values();
			assertEquals(2, values.size());
			assertEquals("ACCOUNTING", values.get(Department.NAME));
			assertEquals("NEW YORK", values.get(Department.LOCATION));
		}
	}

	@Nested
	@DisplayName("Error Conditions")
	class ErrorConditions {

		@Test
		@DisplayName("duplicate column throws IllegalStateException")
		void update_duplicateColumn_throwsIllegalStateException() {
			assertThrows(IllegalStateException.class, () -> 
					Update.all(Employee.TYPE)
							.set(Employee.COMMISSION, 123d)
							.set(Employee.COMMISSION, 456d)
			);
		}

		@Test
		@DisplayName("no values throws IllegalStateException")
		void update_noValues_throwsIllegalStateException() {
			assertThrows(IllegalStateException.class, () -> 
					Update.all(Employee.TYPE).build()
			);
		}

		@Test
		@DisplayName("null column throws NullPointerException")
		void update_nullColumn_throwsNullPointerException() {
			assertThrows(NullPointerException.class, () -> 
					Update.all(Employee.TYPE)
							.set(null, "value")
			);
		}
	}

	@Nested
	@DisplayName("Equality and HashCode")
	class EqualityAndHashCode {

		@Test
		@DisplayName("equals with same condition and values")
		void update_equals_withSameConditionAndValues() {
			Update update1 = Update.where(Employee.DEPARTMENT.equalTo(10))
					.set(Employee.SALARY, 50000d)
					.set(Employee.COMMISSION, 100d)
					.build();
			
			Update update2 = Update.where(Employee.DEPARTMENT.equalTo(10))
					.set(Employee.SALARY, 50000d)
					.set(Employee.COMMISSION, 100d)
					.build();
			
			assertEquals(update1, update2);
			assertEquals(update1.hashCode(), update2.hashCode());
		}

		@Test
		@DisplayName("not equals with different conditions")
		void update_notEquals_withDifferentConditions() {
			Update update1 = Update.where(Employee.DEPARTMENT.equalTo(10))
					.set(Employee.SALARY, 50000d)
					.build();
			
			Update update2 = Update.where(Employee.DEPARTMENT.equalTo(20))
					.set(Employee.SALARY, 50000d)
					.build();
			
			assertNotEquals(update1, update2);
		}

		@Test
		@DisplayName("not equals with different values")
		void update_notEquals_withDifferentValues() {
			Update update1 = Update.all(Employee.TYPE)
					.set(Employee.SALARY, 50000d)
					.build();
			
			Update update2 = Update.all(Employee.TYPE)
					.set(Employee.SALARY, 60000d)
					.build();
			
			assertNotEquals(update1, update2);
		}

		@Test
		@DisplayName("not equals with different columns")
		void update_notEquals_withDifferentColumns() {
			Update update1 = Update.all(Employee.TYPE)
					.set(Employee.SALARY, 50000d)
					.build();
			
			Update update2 = Update.all(Employee.TYPE)
					.set(Employee.COMMISSION, 50000d)
					.build();
			
			assertNotEquals(update1, update2);
		}
	}

	@Nested
	@DisplayName("ToString")
	class ToStringTest {

		@Test
		@DisplayName("toString includes condition and values")
		void update_toString_includesConditionAndValues() {
			Update update = Update.where(Employee.NAME.equalTo("Test"))
					.set(Employee.SALARY, 55000d)
					.set(Employee.COMMISSION, 200d)
					.build();
			
			String toString = update.toString();
			assertTrue(toString.contains("where="));
			assertTrue(toString.contains("values="));
			assertTrue(toString.contains("Test"));
		}
	}

	@Nested
	@DisplayName("Value Ordering")
	class ValueOrdering {

		@Test
		@DisplayName("values maintain insertion order")
		void update_values_maintainInsertionOrder() {
			Update update = Update.all(Employee.TYPE)
					.set(Employee.NAME, "A")
					.set(Employee.JOB, "B")
					.set(Employee.SALARY, 1d)
					.set(Employee.COMMISSION, 2d)
					.set(Employee.DEPARTMENT, 3)
					.build();
			
			// LinkedHashMap should maintain insertion order
			Map<Column<?>, Object> values = update.values();
			Object[] columns = values.keySet().toArray();
			assertEquals(Employee.NAME, columns[0]);
			assertEquals(Employee.JOB, columns[1]);
			assertEquals(Employee.SALARY, columns[2]);
			assertEquals(Employee.COMMISSION, columns[3]);
			assertEquals(Employee.DEPARTMENT, columns[4]);
		}
	}
}
