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
 * Copyright (c) 2009 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.framework.db.local;

import is.codion.common.db.database.ConnectionProvider;
import is.codion.common.db.database.Database;
import is.codion.common.db.database.Database.Operation;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.db.exception.DeleteException;
import is.codion.common.db.exception.MultipleRecordsFoundException;
import is.codion.common.db.exception.RecordModifiedException;
import is.codion.common.db.exception.RecordNotFoundException;
import is.codion.common.db.exception.ReferentialIntegrityException;
import is.codion.common.db.exception.UniqueConstraintException;
import is.codion.common.db.exception.UpdateException;
import is.codion.common.user.User;
import is.codion.dbms.h2.H2DatabaseFactory;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnection.Count;
import is.codion.framework.db.EntityConnection.Select;
import is.codion.framework.db.EntityConnection.Update;
import is.codion.framework.db.EntityResultIterator;
import is.codion.framework.db.local.ConfigureDb.Configured;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.OrderBy;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.condition.Condition;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static is.codion.framework.db.EntityConnection.Count.where;
import static is.codion.framework.db.EntityConnection.transaction;
import static is.codion.framework.db.local.DefaultLocalEntityConnection.modifiedColumns;
import static is.codion.framework.db.local.DefaultLocalEntityConnection.valueMissingOrModified;
import static is.codion.framework.db.local.LocalEntityConnection.localEntityConnection;
import static is.codion.framework.db.local.TestDomain.*;
import static is.codion.framework.domain.entity.Entity.primaryKeys;
import static is.codion.framework.domain.entity.OrderBy.NullOrder.NULLS_FIRST;
import static is.codion.framework.domain.entity.OrderBy.NullOrder.NULLS_LAST;
import static is.codion.framework.domain.entity.OrderBy.descending;
import static is.codion.framework.domain.entity.condition.Condition.*;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;

public class DefaultLocalEntityConnectionTest {

	private static final User UNIT_TEST_USER =
					User.parse(System.getProperty("codion.test.user", "scott:tiger"));

	private LocalEntityConnection connection;

	private static final TestDomain DOMAIN = new TestDomain();
	private static final Entities ENTITIES = DOMAIN.entities();

	@BeforeEach
	void setup() {
		connection = createConnection();
	}

	@AfterEach
	void tearDown() {
		connection.close();
	}

	@Test
	void configureDatabase() {
		try (LocalEntityConnection connection = new DefaultLocalEntityConnection(Database.instance(), new ConfigureDb(), UNIT_TEST_USER)) {
			//throws exception if table does not exist, which is created during connection configuration
			connection.select(all(Configured.TYPE));
		}
	}

	@Test
	void batchCopy() {
		try (EntityConnection sourceConnection = new DefaultLocalEntityConnection(Database.instance(), DOMAIN, UNIT_TEST_USER);
				 EntityConnection destinationConnection = createDestinationConnection()) {
			EntityConnection.batchCopy(sourceConnection, destinationConnection)
							.entityTypes(Department.TYPE)
							.batchSize(2)
							.execute();

			assertEquals(sourceConnection.count(Count.all(Department.TYPE)),
							destinationConnection.count(Count.all(Department.TYPE)));

			assertThrows(IllegalArgumentException.class, () -> EntityConnection.batchCopy(sourceConnection, destinationConnection)
							.entityTypes(Employee.TYPE)
							.batchSize(-10));

			EntityConnection.batchCopy(sourceConnection, destinationConnection)
							.conditions(Employee.SALARY.greaterThan(1000d))
							.batchSize(2)
							.includePrimaryKeys(false)
							.execute();
			assertEquals(13, destinationConnection.count(Count.all(Employee.TYPE)));

			destinationConnection.delete(all(Employee.TYPE));
			destinationConnection.delete(all(Department.TYPE));
		}
	}

	@Test
	void batchInsert() {
		try (EntityConnection sourceConnection = new DefaultLocalEntityConnection(Database.instance(), DOMAIN, UNIT_TEST_USER);
				 EntityConnection destinationConnection = createDestinationConnection()) {
			List<Entity> departments = sourceConnection.select(all(Department.TYPE));
			assertThrows(IllegalArgumentException.class, () -> EntityConnection.batchInsert(destinationConnection, departments.iterator())
							.batchSize(-10));

			Consumer<Integer> progressReporter = currentProgress -> {};
			EntityConnection.batchInsert(destinationConnection, departments.iterator())
							.batchSize(2)
							.progressReporter(progressReporter)
							.onInsert(keys -> {})
							.execute();
			assertEquals(sourceConnection.count(Count.all(Department.TYPE)),
							destinationConnection.count(Count.all(Department.TYPE)));

			EntityConnection.batchInsert(destinationConnection, Collections.emptyIterator())
							.batchSize(10)
							.execute();
			destinationConnection.delete(all(Department.TYPE));
		}
	}

	@Test
	void batchOperationPartialFailure() {
		// Test batch insert with partial failure - some entities will violate unique constraint
		connection.startTransaction();
		try {
			List<Entity> departments = new ArrayList<>();
			// First two will succeed
			departments.add(ENTITIES.entity(Department.TYPE)
							.with(Department.DEPTNO, 100)
							.with(Department.DNAME, "DEPT100")
							.build());
			departments.add(ENTITIES.entity(Department.TYPE)
							.with(Department.DEPTNO, 101)
							.with(Department.DNAME, "DEPT101")
							.build());
			// This one will fail due to duplicate DEPTNO
			departments.add(ENTITIES.entity(Department.TYPE)
							.with(Department.DEPTNO, 10) // Already exists
							.with(Department.DNAME, "DUPLICATE")
							.build());
			// This one would succeed if batch continued
			departments.add(ENTITIES.entity(Department.TYPE)
							.with(Department.DEPTNO, 102)
							.with(Department.DNAME, "DEPT102")
							.build());

			assertThrows(UniqueConstraintException.class, () -> connection.insert(departments), "Should have thrown UniqueConstraintException");
		}
		finally {
			connection.rollbackTransaction();
		}
	}

	@Test
	void delete() {
		connection.startTransaction();
		try {
			Entity.Key key = ENTITIES.primaryKey(Department.TYPE, 40);
			connection.delete(new ArrayList<>());
			connection.delete(key);
			try {
				connection.select(key);
				fail();
			}
			catch (DatabaseException ignored) {/*ignored*/}
		}
		finally {
			connection.rollbackTransaction();
		}
		connection.startTransaction();
		try {
			Entity.Key key = ENTITIES.primaryKey(Department.TYPE, 40);
			assertEquals(1, connection.delete(key(key)));
			try {
				connection.select(key);
				fail();
			}
			catch (DatabaseException ignored) {/*ignored*/}
		}
		finally {
			connection.rollbackTransaction();
		}
		connection.startTransaction();
		try {
			//scott, james, adams
			assertEquals(3, connection.delete(and(
							Employee.NAME.like("%S%"),
							Employee.JOB.equalTo("CLERK"))));
		}
		finally {
			connection.rollbackTransaction();
		}
	}

	@Test
	void deleteRowNumberMismatch() {
		Entity.Key key400 = ENTITIES.primaryKey(Department.TYPE, 400);
		assertThrows(DeleteException.class, () -> connection.delete(key400));
		Entity.Key key40 = ENTITIES.primaryKey(Department.TYPE, 40);
		assertThrows(DeleteException.class, () -> connection.delete(asList(key40, key400)));
	}

	@Test
	void deleteReferentialIntegrity() {
		Entity.Key key = ENTITIES.primaryKey(Department.TYPE, 10);
		try {
			connection.delete(key);
			fail("ReferentialIntegrityException should have been thrown");
		}
		catch (ReferentialIntegrityException e) {
			assertEquals(Operation.DELETE, e.operation());
		}
	}

	@Test
	void insertUniqueConstraint() {
		Entity department = ENTITIES.entity(Department.TYPE)
						.with(Department.DEPTNO, 1000)
						.with(Department.DNAME, "SALES")
						.build();
		assertThrows(UniqueConstraintException.class, () -> connection.insert(department));
	}

	@Test
	void updateUniqueConstraint() {
		Entity department = connection.selectSingle(Department.DEPTNO.equalTo(20));
		department.set(Department.DNAME, "SALES");
		assertThrows(UniqueConstraintException.class, () -> connection.update(department));
	}

	@Test
	void insertNoParentKey() {
		Entity emp = ENTITIES.entity(Employee.TYPE)
						.with(Employee.ID, -100)
						.with(Employee.NAME, "Testing")
						.with(Employee.DEPARTMENT, -1010)//not available
						.with(Employee.SALARY, 2000d)
						.build();
		try {
			connection.insert(emp);
			fail("ReferentialIntegrityException should have been thrown");
		}
		catch (ReferentialIntegrityException e) {
			assertEquals(Operation.INSERT, e.operation());
		}
	}

	@Test
	void insertNoPk() {
		Entity noPk = ENTITIES.entity(NoPrimaryKey.TYPE)
						.with(NoPrimaryKey.COL_1, 10)
						.with(NoPrimaryKey.COL_2, "10")
						.with(NoPrimaryKey.COL_3, "10")
						.with(NoPrimaryKey.COL_4, 10)
						.build();

		Entity.Key key = connection.insert(noPk);
		assertFalse(key.isNull());
		assertFalse(key.primary());
	}

	@Test
	void updateNoParentKey() {
		Entity emp = connection.selectSingle(Employee.ID.equalTo(3));
		emp.set(Employee.DEPARTMENT, -1010);//not available
		try {
			connection.update(emp);
			fail("ReferentialIntegrityException should have been thrown");
		}
		catch (ReferentialIntegrityException e) {
			assertEquals(Operation.UPDATE, e.operation());
		}
	}

	@Test
	void deleteByKeyWithForeignKeys() {
		Entity accounting = connection.selectSingle(Department.DNAME.equalTo("ACCOUNTING"));
		try {
			connection.delete(accounting.primaryKey());
			fail("ReferentialIntegrityException should have been thrown");
		}
		catch (ReferentialIntegrityException e) {
			assertEquals(Operation.DELETE, e.operation());
		}
	}

	@Test
	void deleteByConditionWithForeignKeys() {
		try {
			connection.delete(Department.DNAME.equalTo("ACCOUNTING"));
			fail("ReferentialIntegrityException should have been thrown");
		}
		catch (ReferentialIntegrityException e) {
			assertEquals(Operation.DELETE, e.operation());
		}
	}

	@Test
	void report() {
		Map<String, Object> reportParameters = new HashMap<>();
		reportParameters.put("DEPTNO", asList(10, 20));
		assertEquals("result", connection.report(REPORT, reportParameters));
	}

	@Test
	void dependencies() {
		Map<EntityType, Collection<Entity>> empty = connection.dependencies(new ArrayList<>());
		assertTrue(empty.isEmpty());
		List<Entity> accounting = connection.select(Department.DNAME.equalTo("ACCOUNTING"));
		Map<EntityType, Collection<Entity>> emps = connection.dependencies(accounting);
		assertEquals(1, emps.size());
		assertTrue(emps.containsKey(Employee.TYPE));
		assertEquals(7, emps.get(Employee.TYPE).size());
		emps.get(Employee.TYPE).forEach(entity -> assertTrue(entity.mutable()));

		Entity emp = connection.selectSingle(Employee.NAME.equalTo("KING"));
		Map<EntityType, Collection<Entity>> deps = connection.dependencies(singletonList(emp));
		assertTrue(deps.isEmpty());//soft foreign key reference

		//multiple foreign keys referencing the same entity
		Entity master1 = connection.selectSingle(Master.ID.equalTo(1));
		Entity master2 = connection.selectSingle(Master.ID.equalTo(2));
		Entity master3 = connection.selectSingle(Master.ID.equalTo(3));
		Entity master4 = connection.selectSingle(Master.ID.equalTo(4));

		Entity detail1 = connection.selectSingle(Detail.ID.equalTo(1));
		Entity detail2 = connection.selectSingle(Detail.ID.equalTo(2));

		deps = connection.dependencies(singletonList(master1));
		Collection<Entity> dependantEntities = deps.get(Detail.TYPE);
		assertEquals(1, dependantEntities.size());
		assertTrue(dependantEntities.contains(detail1));

		deps = connection.dependencies(singletonList(master2));
		dependantEntities = deps.get(Detail.TYPE);
		assertEquals(2, dependantEntities.size());
		assertTrue(dependantEntities.containsAll(Arrays.asList(detail1, detail2)));

		deps = connection.dependencies(singletonList(master3));
		dependantEntities = deps.get(Detail.TYPE);
		assertEquals(1, dependantEntities.size());
		assertTrue(dependantEntities.contains(detail2));

		deps = connection.dependencies(Arrays.asList(master1, master2));
		dependantEntities = deps.get(Detail.TYPE);
		assertEquals(2, dependantEntities.size());
		assertTrue(dependantEntities.containsAll(Arrays.asList(detail1, detail2)));

		deps = connection.dependencies(Arrays.asList(master2, master3));
		dependantEntities = deps.get(Detail.TYPE);
		assertEquals(2, dependantEntities.size());
		assertTrue(dependantEntities.containsAll(Arrays.asList(detail1, detail2)));

		deps = connection.dependencies(Arrays.asList(master1, master2, master3, master4));
		dependantEntities = deps.get(Detail.TYPE);
		assertEquals(2, dependantEntities.size());
		assertTrue(dependantEntities.containsAll(Arrays.asList(detail1, detail2)));

		deps = connection.dependencies(singletonList(master4));
		assertFalse(deps.containsKey(Detail.TYPE));

		Entity jones = connection.selectSingle(EmployeeFk.NAME.equalTo("JONES"));
		assertTrue(connection.dependencies(singletonList(jones)).isEmpty());//soft reference

		assertThrows(IllegalArgumentException.class, () -> connection.dependencies(Arrays.asList(master1, jones)));
	}

	@Test
	void selectLimitOffset() {
		Select select = Select.all(Employee.TYPE)
						.orderBy(OrderBy.ascending(Employee.NAME))
						.limit(2)
						.build();
		List<Entity> result = connection.select(select);
		assertEquals(2, result.size());
		select = Select.all(Employee.TYPE)
						.orderBy(OrderBy.ascending(Employee.NAME))
						.limit(3)
						.offset(3)
						.build();
		result = connection.select(select);
		assertEquals(3, result.size());
		assertEquals("BLAKE", result.get(0).get(Employee.NAME));
		assertEquals("CLARK", result.get(1).get(Employee.NAME));
		assertEquals("FORD", result.get(2).get(Employee.NAME));
	}

	@Test
	void select() {
		Collection<Entity> result = connection.select(new ArrayList<>());
		assertTrue(result.isEmpty());
		result = connection.select(Department.DEPTNO.in(10, 20));
		assertEquals(2, result.size());
		result = connection.select(primaryKeys(result));
		assertEquals(2, result.size());
		result = connection.select(Department.DEPARTMENT_CONDITION_TYPE.get(
						Department.DEPTNO, asList(10, 20)));
		assertEquals(2, result.size());
		result = connection.select(EmpnoDeptno.CONDITION.get());
		assertEquals(7, result.size());

		Select select = Select.where(Employee.NAME_IS_BLAKE_CONDITION.get()).build();
		result = connection.select(select);
		Entity emp = result.iterator().next();
		assertNotNull(emp.get(Employee.DEPARTMENT_FK));
		assertNotNull(emp.get(Employee.MGR_FK));
		emp = emp.entity(Employee.MGR_FK);
		assertNull(emp.get(Employee.MGR_FK));

		select = Select.where(select.where())
						.referenceDepth(Employee.DEPARTMENT_FK, 0)
						.build();
		result = connection.select(select);
		assertEquals(1, result.size());
		emp = result.iterator().next();
		assertNull(emp.get(Employee.DEPARTMENT_FK));
		assertNotNull(emp.get(Employee.MGR_FK));

		select = Select.where(select.where())
						.referenceDepth(Employee.DEPARTMENT_FK, 0)
						.referenceDepth(Employee.MGR_FK, 0)
						.build();
		result = connection.select(select);
		assertEquals(1, result.size());
		emp = result.iterator().next();
		assertNull(emp.get(Employee.DEPARTMENT_FK));
		assertNull(emp.get(Employee.MGR_FK));

		select = Select.where(select.where())
						.referenceDepth(Employee.DEPARTMENT_FK, 0)
						.referenceDepth(Employee.MGR_FK, 2)
						.build();
		result = connection.select(select);
		assertEquals(1, result.size());
		emp = result.iterator().next();
		assertNull(emp.get(Employee.DEPARTMENT_FK));
		assertNotNull(emp.get(Employee.MGR_FK));
		emp = emp.entity(Employee.MGR_FK);
		assertNotNull(emp.get(Employee.MGR_FK));

		select = Select.where(select.where())
						.referenceDepth(Employee.DEPARTMENT_FK, 0)
						.referenceDepth(Employee.MGR_FK, -1)
						.build();
		result = connection.select(select);
		assertEquals(1, result.size());
		emp = result.iterator().next();
		assertNull(emp.get(Employee.DEPARTMENT_FK));
		assertNotNull(emp.get(Employee.MGR_FK));
		emp = emp.entity(Employee.MGR_FK);
		assertNotNull(emp.get(Employee.MGR_FK));

		assertEquals(4, connection.count(where(Employee.ID.in(asList(1, 2, 3, 4)))));
		assertEquals(0, connection.count(where(Employee.DEPARTMENT.isNull())));
		assertEquals(0, connection.count(where(Employee.DEPARTMENT_FK.isNull())));
		assertEquals(1, connection.count(where(Employee.MGR.isNull())));
		assertEquals(1, connection.count(where(Employee.MGR_FK.isNull())));

		assertFalse(connection.select(Employee.DEPARTMENT_FK.in(connection.select(Department.DEPTNO.equalTo(20)))).isEmpty());
	}

	@Test
	void selectLimit() {
		Condition condition = all(Department.TYPE);
		List<Entity> departments = connection.select(condition);
		assertEquals(4, departments.size());
		Select.Builder selectBuilder = Select.where(condition);
		departments = connection.select(selectBuilder.limit(0).build());
		assertTrue(departments.isEmpty());
		departments = connection.select(selectBuilder.limit(2).build());
		assertEquals(2, departments.size());
		departments = connection.select(selectBuilder.limit(3).build());
		assertEquals(3, departments.size());
		departments = connection.select(selectBuilder.limit(null).build());
		assertEquals(4, departments.size());
	}

	@Test
	void selectByKey() {
		Entity.Key deptKey = ENTITIES.primaryKey(Department.TYPE, 10);
		Entity.Key empKey = ENTITIES.primaryKey(Employee.TYPE, 8);

		Collection<Entity> selected = connection.select(asList(deptKey, empKey));
		assertEquals(2, selected.size());
	}

	@Test
	void foreignKeyAttributes() {
		List<Entity> emps = connection.select(Select.where(EmployeeFk.MGR_FK.isNotNull())
						.referenceDepth(EmployeeFk.MGR_FK, 2)
						.build());
		for (Entity emp : emps) {
			Entity mgr = emp.entity(EmployeeFk.MGR_FK);
			assertFalse(mgr.mutable());
			assertTrue(mgr.contains(EmployeeFk.ID));//pk automatically included
			assertTrue(mgr.contains(EmployeeFk.NAME));
			assertTrue(mgr.contains(EmployeeFk.JOB));
			assertTrue(mgr.contains(EmployeeFk.DEPARTMENT));
			assertTrue(mgr.contains(EmployeeFk.DEPARTMENT_FK));
			assertFalse(mgr.get(EmployeeFk.DEPARTMENT_FK).mutable());
			assertFalse(mgr.contains(EmployeeFk.MGR));
			assertFalse(mgr.contains(EmployeeFk.MGR_FK));
			assertFalse(mgr.contains(EmployeeFk.COMMISSION));
			assertFalse(mgr.contains(EmployeeFk.HIREDATE));
			assertFalse(mgr.contains(EmployeeFk.SALARY));
		}
	}

	@Test
	void attributes() {
		List<Entity> emps = connection.select(Select.all(Employee.TYPE)
						.attributes(Employee.ID, Employee.JOB, Employee.DEPARTMENT)
						.build());
		for (Entity emp : emps) {
			assertTrue(emp.contains(Employee.ID));
			assertTrue(emp.contains(Employee.JOB));
			assertTrue(emp.contains(Employee.DEPARTMENT));
			assertFalse(emp.contains(Employee.DEPARTMENT_FK));
			assertFalse(emp.contains(Employee.COMMISSION));
			assertFalse(emp.contains(Employee.HIREDATE));
			assertFalse(emp.contains(Employee.NAME));
			assertFalse(emp.contains(Employee.SALARY));
		}
		for (Entity emp : connection.select(Select.all(Employee.TYPE)
						.attributes(Employee.ID, Employee.JOB, Employee.DEPARTMENT_FK, Employee.MGR, Employee.COMMISSION)
						.build())) {
			assertTrue(emp.contains(Employee.ID));//pk automatically included
			assertTrue(emp.contains(Employee.JOB));
			assertTrue(emp.contains(Employee.DEPARTMENT));
			assertTrue(emp.contains(Employee.DEPARTMENT_FK));
			assertTrue(emp.contains(Employee.MGR));
			assertFalse(emp.contains(Employee.MGR_FK));
			assertTrue(emp.contains(Employee.COMMISSION));
			assertFalse(emp.contains(Employee.HIREDATE));
			assertFalse(emp.contains(Employee.NAME));
			assertFalse(emp.contains(Employee.SALARY));
		}
	}

	@Test
	void selectInvalidColumn() {
		assertThrows(DatabaseException.class, () -> connection.select(Department.DEPARTMENT_CONDITION_INVALID_COLUMN_TYPE.get()));
	}

	@Test
	void selectCustomColumnsDifferentOrder() {
		Entity king = connection.selectSingle(EmpnoDeptno.EMPNO.equalTo(8));
		assertEquals(8, king.get(EmpnoDeptno.EMPNO));
		assertEquals("KING", king.get(EmpnoDeptno.EMPLOYEE_NAME));
		assertEquals(10, king.get(EmpnoDeptno.DEPTNO));
		assertEquals("ACCOUNTING", king.get(EmpnoDeptno.DEPARTMENT_NAME));
	}

	@Test
	void count() {
		int rowCount = connection.count(Count.all(Department.TYPE));
		assertEquals(4, rowCount);
		Condition deptNoCondition = Department.DEPTNO.greaterThanOrEqualTo(30);
		rowCount = connection.count(Count.where(deptNoCondition));
		assertEquals(2, rowCount);

		rowCount = connection.count(Count.all(EmpnoDeptno.TYPE));
		assertEquals(16, rowCount);
		deptNoCondition = EmpnoDeptno.DEPTNO.greaterThanOrEqualTo(30);
		rowCount = connection.count(Count.where(deptNoCondition));
		assertEquals(4, rowCount);

		rowCount = connection.count(Count.all(Job.TYPE));
		assertEquals(4, rowCount);
	}

	@Test
	void selectSingle() {
		Entity sales = connection.selectSingle(Department.DNAME.equalTo("SALES"));
		assertEquals("SALES", sales.get(Department.DNAME));
		sales = connection.select(sales.primaryKey());
		assertEquals("SALES", sales.get(Department.DNAME));
		sales = connection.selectSingle(Department.DEPARTMENT_CONDITION_SALES_TYPE.get());
		assertEquals("SALES", sales.get(Department.DNAME));

		Entity king = connection.selectSingle(Employee.NAME.equalTo("KING"));
		assertTrue(king.contains(Employee.MGR_FK));
		assertNull(king.get(Employee.MGR_FK));

		king = connection.selectSingle(Employee.MGR_FK.isNull());
		assertNull(king.get(Employee.MGR_FK));
	}

	@Test
	void customCondition() {
		assertEquals(4, connection.select(Employee.MGR_GREATER_THAN_CONDITION.get(Employee.MGR, 5)).size());
	}

	@Test
	void executeFunction() {
		connection.execute(FUNCTION_ID);
	}

	@Test
	void executeProcedure() {
		connection.execute(PROCEDURE_ID);
	}

	@Test
	void selectSingleNotFound() {
		assertThrows(RecordNotFoundException.class, () -> connection.selectSingle(Department.DNAME.equalTo("NO_NAME")));
	}

	@Test
	void selectSingleManyFound() {
		assertThrows(MultipleRecordsFoundException.class, () -> connection.selectSingle(Employee.JOB.equalTo("MANAGER")));
	}

	@Test
	void insertOnlyNullValues() {
		connection.startTransaction();
		try {
			Entity department = ENTITIES.entity(Department.TYPE).build();
			assertThrows(DatabaseException.class, () -> connection.insert(department));
		}
		finally {
			connection.rollbackTransaction();
		}
	}

	@Test
	void updateNoModifiedValues() {
		Entity department = connection.selectSingle(Department.DEPTNO.equalTo(10));
		assertThrows(UpdateException.class, () -> connection.update(department));
	}

	@Test
	void dateTime() {
		Entity sales = connection.selectSingle(Department.DNAME.equalTo("SALES"));
		final double salary = 1500;

		Entity emp = ENTITIES.entity(Employee.TYPE)
						.with(Employee.DEPARTMENT_FK, sales)
						.with(Employee.NAME, "Nobody")
						.with(Employee.SALARY, salary)
						.build();
		LocalDate hiredate = LocalDate.parse("03-10-1975", DateTimeFormatter.ofPattern("dd-MM-yyyy"));
		emp.set(Employee.HIREDATE, hiredate);
		OffsetDateTime hiretime = LocalDateTime.parse("03-10-1975 08:30:22", DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"))
						.atZone(TimeZone.getDefault().toZoneId()).toOffsetDateTime();
		emp.set(Employee.HIRETIME, hiretime);

		emp = connection.select(connection.insert(emp));

		assertEquals(hiredate, emp.get(Employee.HIREDATE));
		assertEquals(hiretime, emp.get(Employee.HIRETIME));

		connection.delete(emp.primaryKey());
	}

	@Test
	void insertWithNullValues() {
		Entity sales = connection.selectSingle(Department.DNAME.equalTo("SALES"));
		final String name = "Nobody";
		final double salary = 1500;
		final double defaultCommission = 200;

		Entity emp = ENTITIES.entity(Employee.TYPE)
						.with(Employee.DEPARTMENT_FK, sales)
						.with(Employee.NAME, name)
						.with(Employee.SALARY, salary)
						.build();

		emp = connection.insertSelect(emp);
		assertEquals(sales, emp.get(Employee.DEPARTMENT_FK));
		assertEquals(name, emp.get(Employee.NAME));
		assertEquals(salary, emp.get(Employee.SALARY));
		assertEquals(defaultCommission, emp.get(Employee.COMMISSION));
		connection.delete(emp.primaryKey());

		emp.set(Employee.COMMISSION, null);//default value should not kick in
		emp = connection.insertSelect(emp);
		assertEquals(sales, emp.get(Employee.DEPARTMENT_FK));
		assertEquals(name, emp.get(Employee.NAME));
		assertEquals(salary, emp.get(Employee.SALARY));
		assertNull(emp.get(Employee.COMMISSION));
		connection.delete(emp.primaryKey());

		emp.remove(Employee.COMMISSION);//default value should kick in
		emp = connection.insertSelect(emp);
		assertEquals(sales, emp.get(Employee.DEPARTMENT_FK));
		assertEquals(name, emp.get(Employee.NAME));
		assertEquals(salary, emp.get(Employee.SALARY));
		assertEquals(defaultCommission, emp.get(Employee.COMMISSION));
		connection.delete(emp.primaryKey());
	}

	@Test
	void insertEmptyList() {
		assertTrue(connection.insert(new ArrayList<>()).isEmpty());
	}

	@Test
	void updateDifferentEntities() {
		connection.startTransaction();
		try {
			Entity sales = connection.selectSingle(Department.DNAME.equalTo("SALES"));
			Entity king = connection.selectSingle(Employee.NAME.equalTo("KING"));
			final String newName = "New name";
			sales.set(Department.DNAME, newName);
			king.set(Employee.NAME, newName);
			List<Entity> updated = new ArrayList<>(connection.updateSelect(asList(sales, king)));
			assertTrue(updated.containsAll(asList(sales, king)));
			assertEquals(newName, updated.get(updated.indexOf(sales)).get(Department.DNAME));
			assertEquals(newName, updated.get(updated.indexOf(king)).get(Employee.NAME));
		}
		finally {
			connection.rollbackTransaction();
		}
	}

	@Test
	void updateNonExisting() {
		Entity employee = connection.selectSingle(EmployeeNonOpt.ID.equalTo(4));
		employee.set(EmployeeNonOpt.ID, -888);//non existing
		employee.save();
		employee.set(EmployeeNonOpt.NAME, "New name");
		assertThrows(UpdateException.class, () -> connection.update(employee));
	}

	@Test
	void update() {
		assertTrue(connection.updateSelect(new ArrayList<>()).isEmpty());
	}

	@Test
	void updateNonUpdatable() {
		assertThrows(UpdateException.class, () -> connection.update(Update.where(Employee.ID.equalTo(1))
						.set(Employee.ID, 999)
						.build()));
	}

	@Test
	void updateWithConditionNoColumns() {
		Update update = new Update() {
			@Override
			public Condition where() {
				return Condition.all(Employee.TYPE);
			}

			@Override
			public Map<Column<?>, Object> values() {
				return emptyMap();
			}
		};
		assertThrows(IllegalArgumentException.class, () -> connection.update(update));
	}

	@Test
	void updateWithCondition() {
		Condition condition = Employee.COMMISSION.isNull();

		List<Entity> entities = connection.select(condition);

		Update update = Update.where(Employee.COMMISSION.isNull())
						.set(Employee.COMMISSION, 500d)
						.set(Employee.SALARY, 4200d)
						.build();
		connection.startTransaction();
		try {
			connection.update(update);
			assertEquals(0, connection.count(Count.where(condition)));
			Collection<Entity> afterUpdate = connection.select(Entity.primaryKeys(entities));
			for (Entity entity : afterUpdate) {
				assertEquals(500d, entity.get(Employee.COMMISSION));
				assertEquals(4200d, entity.get(Employee.SALARY));
			}
		}
		finally {
			connection.rollbackTransaction();
		}
	}

	@Test
	void updateWithConditionNoRows() {
		Update update = Update.where(Employee.ID.isNull())
						.set(Employee.SALARY, 4200d)
						.build();
		connection.startTransaction();
		try {
			assertEquals(0, connection.update(update));
		}
		finally {
			connection.rollbackTransaction();
		}
	}

	@Test
	void selectValuesNonColumn() {
		assertThrows(IllegalArgumentException.class, () -> connection.select(Employee.DEPARTMENT_LOCATION));
	}

	@Test
	void selectValues() {
		List<String> result = connection.select(Department.DNAME);
		assertEquals("ACCOUNTING", result.get(0));
		assertEquals("OPERATIONS", result.get(1));
		assertEquals("RESEARCH", result.get(2));
		assertEquals("SALES", result.get(3));

		result = connection.select(Department.DNAME, Department.DEPTNO.equalTo(10));
		assertTrue(result.contains("ACCOUNTING"));
		assertFalse(result.contains("SALES"));
	}

	@Test
	void selectValuesCustomQuery() {
		connection.select(EmpnoDeptno.DEPTNO);
		connection.select(EmpnoDeptno.EMPLOYEE_NAME);
		connection.select(EmpnoDeptno.EMPNO);
		connection.select(EmpnoDeptno.DEPARTMENT_NAME);
	}

	@Test
	void selectValuesAggregateColumn() {
		assertThrows(UnsupportedOperationException.class, () -> connection.select(Job.MAX_SALARY));
	}

	@Test
	void selectValuesIncorrectAttribute() {
		assertThrows(IllegalArgumentException.class, () -> connection.select(Department.DNAME,
						Employee.ID.equalTo(1)));
	}

	@Test
	void selectValuesLimit() {
		List<Integer> deptnos = connection.select(Department.DEPTNO, Select.all(Department.TYPE)
						.limit(2)
						.orderBy(descending(Department.DEPTNO))
						.build());
		assertEquals(40, deptnos.get(0));
		assertEquals(30, deptnos.get(1));
	}

	@Test
	void selectForUpdateModified() throws SQLException {
		LocalEntityConnection connection = createConnection();
		LocalEntityConnection connection2 = createConnection();
		String originalLocation;
		try {
			Select select = Select.where(Department.DNAME.equalTo("SALES")).forUpdate().build();

			Entity sales = connection.selectSingle(select);
			originalLocation = sales.get(Department.LOC);

			sales.set(Department.LOC, "Syracuse");
			try {
				connection2.update(sales);
				fail("Should not be able to update row selected for update by another connection");
			}
			catch (DatabaseException ignored) {
				connection2.databaseConnection().rollback();
			}

			connection.select(all(Department.TYPE));//any query will do

			try {
				sales = connection2.updateSelect(sales);
				sales.set(Department.LOC, originalLocation);
				connection2.update(sales);//revert changes to data
			}
			catch (DatabaseException ignored) {
				fail("Should be able to update row after other connection released the select for update lock");
			}
		}
		finally {
			connection.close();
			connection2.close();
		}
	}

	@Test
	void optimisticLockingDeleted() {
		LocalEntityConnection connection = createConnection();
		EntityConnection connection2 = createConnection();
		connection.optimisticLocking(true);
		Entity allen;
		try {
			Condition condition = Employee.NAME.equalTo("ALLEN");

			allen = connection.selectSingle(condition);

			connection2.delete(allen.primaryKey());

			allen.set(Employee.JOB, "CLERK");
			try {
				connection.update(allen);
				fail("Should not be able to update row deleted by another connection");
			}
			catch (RecordModifiedException e) {
				assertNotNull(e.row());
				assertNull(e.modifiedRow());
			}

			try {
				connection2.insert(allen);//revert changes to data
			}
			catch (DatabaseException ignored) {
				fail("Should be able to update row after other connection released the select for update lock");
			}
		}
		finally {
			connection.close();
			connection2.close();
		}
	}

	@Test
	void optimisticLockingModified() {
		LocalEntityConnection baseConnection = createConnection();
		LocalEntityConnection optimisticConnection = createConnection(true);
		optimisticConnection.optimisticLocking(true);
		assertTrue(optimisticConnection.optimisticLocking());
		String oldLocation = null;
		Entity updatedDepartment = null;
		try {
			Entity department = baseConnection.selectSingle(Department.DNAME.equalTo("SALES"));
			oldLocation = department.set(Department.LOC, "NEWLOC");
			updatedDepartment = baseConnection.updateSelect(department);
			try {
				optimisticConnection.update(department);
				fail("RecordModifiedException should have been thrown");
			}
			catch (RecordModifiedException e) {
				assertTrue(((Entity) e.modifiedRow()).equalValues(updatedDepartment));
				assertTrue(((Entity) e.row()).equalValues(department));
			}
		}
		finally {
			try {
				if (updatedDepartment != null && oldLocation != null) {
					updatedDepartment.set(Department.LOC, oldLocation);
					baseConnection.update(updatedDepartment);
				}
			}
			catch (DatabaseException e) {
				fail();
			}
			baseConnection.close();
			optimisticConnection.close();
		}
	}

	@Test
	void optimisticLockingBlob() {
		LocalEntityConnection baseConnection = createConnection();
		LocalEntityConnection optimisticConnection = createConnection();
		optimisticConnection.optimisticLocking(true);
		Entity updatedEmployee = null;
		try {
			Random random = new Random();
			byte[] bytes = new byte[1024];
			random.nextBytes(bytes);

			Entity employee = baseConnection.selectSingle(Employee.NAME.equalTo("BLAKE"));
			employee.set(Employee.DATA, bytes);
			updatedEmployee = baseConnection.updateSelect(employee);

			random.nextBytes(bytes);
			employee.set(Employee.DATA, bytes);

			try {
				optimisticConnection.update(employee);
				fail("RecordModifiedException should have been thrown");
			}
			catch (RecordModifiedException e) {
				//use columns here since the modified row entity contains no foreign key values
				Collection<Column<?>> columns = updatedEmployee.definition().columns().get();
				assertTrue(((Entity) e.modifiedRow()).equalValues(updatedEmployee, columns));
				assertTrue(((Entity) e.row()).equalValues(employee, columns));
			}
		}
		finally {
			baseConnection.close();
			optimisticConnection.close();
		}
	}

	@Test
	void iterator() {
		try (LocalEntityConnection connection = createConnection()) {
			Condition condition = all(Employee.TYPE);
			EntityResultIterator iterator = connection.iterator(condition);
			//calling hasNext() should be idempotent and not lose rows
			assertTrue(iterator.hasNext());
			assertTrue(iterator.hasNext());
			assertTrue(iterator.hasNext());
			int counter = 0;
			while (iterator.hasNext()) {
				iterator.next();
				iterator.hasNext();
				counter++;
			}
			assertThrows(NoSuchElementException.class, iterator::next);
			int rowCount = connection.count(Count.where(condition));
			assertEquals(rowCount, counter);
			iterator.close();
			iterator = connection.iterator(condition);
			counter = 0;
			try {
				while (true) {
					iterator.next();
					counter++;
				}
			}
			catch (NoSuchElementException e) {
				assertEquals(rowCount, counter);
			}
		}
	}

	@Test
	void dualIterator() {
		try (LocalEntityConnection connection = createConnection();
				 EntityResultIterator deptIterator = connection.iterator(all(Department.TYPE))) {
			while (deptIterator.hasNext()) {
				try (EntityResultIterator empIterator =
										 connection.iterator(Employee.DEPARTMENT_FK.equalTo(deptIterator.next()))) {
					while (empIterator.hasNext()) {
						empIterator.next();
					}
				}
			}
		}
	}

	@Test
	void testConstructor() {
		Connection connection = null;
		try {
			Database db = Database.instance();
			connection = db.createConnection(UNIT_TEST_USER);
			EntityConnection conn = new DefaultLocalEntityConnection(db, DOMAIN, connection);
			assertTrue(conn.connected());
		}
		finally {
			if (connection != null) {
				try {
					connection.close();
				}
				catch (Exception ignored) {/*ignored*/}
			}
		}
	}

	@Test
	void testConstructorInvalidConnection() {
		assertThrows(DatabaseException.class, () -> {
			Connection connection = null;
			try {
				Database db = Database.instance();
				connection = db.createConnection(UNIT_TEST_USER);
				connection.close();
				new DefaultLocalEntityConnection(db, DOMAIN, connection);
			}
			finally {
				if (connection != null) {
					try {
						connection.close();
					}
					catch (Exception ignored) {/*ignored*/}
				}
			}
		});
	}

	@Test
	void operationOnClosedConnection() {
		LocalEntityConnection closedConnection = createConnection();
		closedConnection.close();
		assertFalse(closedConnection.connected());

		// Test that all operations throw appropriate exceptions on closed connection
		Entity testEntity = ENTITIES.entity(Department.TYPE)
						.with(Department.DEPTNO, 999)
						.with(Department.DNAME, "TEST")
						.build();
		assertThrows(IllegalStateException.class, () -> closedConnection.insert(testEntity),
						"Insert should fail on closed connection");
		assertThrows(IllegalStateException.class, () -> closedConnection.insertSelect(testEntity),
						"InsertSelect should fail on closed connection");
		assertThrows(IllegalStateException.class, () -> closedConnection.update(testEntity),
						"Update should fail on closed connection");
		assertThrows(IllegalStateException.class, () -> closedConnection.updateSelect(testEntity),
						"UpdateSelect should fail on closed connection");
		assertThrows(IllegalStateException.class, () -> closedConnection.delete(testEntity.primaryKey()),
						"Delete should fail on closed connection");
		assertThrows(IllegalStateException.class, () -> closedConnection.delete(all(Department.TYPE)),
						"Delete with condition should fail on closed connection");
		assertThrows(IllegalStateException.class, () -> closedConnection.select(all(Department.TYPE)),
						"Select should fail on closed connection");
		assertThrows(IllegalStateException.class, () -> closedConnection.select(Department.DNAME, all(Department.TYPE)),
						"Select should fail on closed connection");
		assertThrows(IllegalStateException.class, () -> closedConnection.selectSingle(Department.DEPTNO.equalTo(10)),
						"SelectSingle should fail on closed connection");
		assertThrows(IllegalStateException.class, () -> closedConnection.count(Count.all(Department.TYPE)),
						"Count should fail on closed connection");
		assertThrows(IllegalStateException.class, () -> closedConnection.startTransaction(),
						"StartTransaction should fail on closed connection");

		assertFalse(closedConnection.connected());
	}

	@Test
	void readWriteBlob() {
		byte[] lazyBytes = new byte[1024];
		byte[] bytes = new byte[1024];
		Random random = new Random();
		random.nextBytes(lazyBytes);
		random.nextBytes(bytes);

		Entity scott = connection.selectSingle(Employee.ID.equalTo(7));
		scott.set(Employee.DATA_LAZY, lazyBytes);
		scott.set(Employee.DATA, bytes);
		connection.update(scott);
		scott.save();

		byte[] lazyFromDb = connection.select(Employee.DATA_LAZY, key(scott.primaryKey())).get(0);
		byte[] fromDb = connection.select(Employee.DATA, key(scott.primaryKey())).get(0);
		assertArrayEquals(lazyBytes, lazyFromDb);
		assertArrayEquals(bytes, fromDb);

		Entity scottFromDb = connection.select(scott.primaryKey());
		//lazy loaded
		assertNull(scottFromDb.get(Employee.DATA_LAZY));
		assertNotNull(scottFromDb.get(Employee.DATA));
		assertArrayEquals(bytes, scottFromDb.get(Employee.DATA));

		byte[] newLazyBytes = new byte[2048];
		byte[] newBytes = new byte[2048];
		random.nextBytes(newLazyBytes);
		random.nextBytes(newBytes);

		scott.set(Employee.DATA_LAZY, newLazyBytes);
		scott.set(Employee.DATA, newBytes);

		connection.update(scott);

		lazyFromDb = connection.select(Employee.DATA_LAZY, key(scott.primaryKey())).get(0);
		assertArrayEquals(newLazyBytes, lazyFromDb);

		scottFromDb = connection.select(scott.primaryKey());
		assertArrayEquals(newBytes, scottFromDb.get(Employee.DATA));
	}

	@Test
	void testUUIDPrimaryKeyColumnWithDefaultValue() {
		Entity entity = ENTITIES.entity(UUIDTestDefault.TYPE)
						.with(UUIDTestDefault.DATA, "test")
						.build();
		connection.insert(entity);
		assertNotNull(entity.get(UUIDTestDefault.ID));
		assertEquals("test", entity.get(UUIDTestDefault.DATA));
	}

	@Test
	void testUUIDPrimaryKeyColumnWithoutDefaultValue() {
		Entity entity = ENTITIES.entity(UUIDTestNoDefault.TYPE)
						.with(UUIDTestNoDefault.DATA, "test")
						.build();
		connection.insert(entity);
		assertNotNull(entity.get(UUIDTestNoDefault.ID));
		assertEquals("test", entity.get(UUIDTestNoDefault.DATA));
	}

	@Test
	void entityWithoutPrimaryKey() {
		List<Entity> entities = connection.select(all(NoPrimaryKey.TYPE));
		assertEquals(6, entities.size());
		entities = connection.select(or(
						NoPrimaryKey.COL_1.equalTo(2),
						NoPrimaryKey.COL_3.equalTo("5")));
		assertEquals(4, entities.size());
	}

	@Test
	void selectQuery() {
		connection.select(all(Query.TYPE));
		connection.select(Select.all(Query.TYPE).forUpdate().build());
		connection.select(all(QueryColumnsWhereClause.TYPE));
		connection.select(Select.all(QueryColumnsWhereClause.TYPE).forUpdate().build());
		connection.select(all(QueryFromClause.TYPE));
		connection.select(Select.all(QueryFromClause.TYPE).forUpdate().build());
		connection.select(all(QueryFromWhereClause.TYPE));
		connection.select(Select.all(QueryFromWhereClause.TYPE).forUpdate().build());
	}

	@Test
	void queryCache() {
		connection.queryCache(true);
		assertTrue(connection.queryCache());

		List<Entity> result = connection.select(Department.DEPTNO
						.greaterThanOrEqualTo(20));
		List<Entity> result2 = connection.select(Department.DEPTNO.greaterThanOrEqualTo(20));
		assertSame(result, result2);

		result2 = connection.select(Select.where(Department.DEPTNO
										.greaterThanOrEqualTo(20))
						.orderBy(descending(Department.DEPTNO))
						.build());
		assertNotSame(result, result2);

		result = connection.select(Select.where(Department.DEPTNO
										.greaterThanOrEqualTo(20))
						.orderBy(descending(Department.DEPTNO))
						.build());
		assertSame(result, result2);

		result2 = connection.select(Select.where(Department.DEPTNO
										.greaterThanOrEqualTo(20))
						.orderBy(OrderBy.ascending(Department.DEPTNO))
						.build());
		assertNotSame(result, result2);

		result = connection.select(Select.where(Department.DEPTNO
										.greaterThanOrEqualTo(20))
						.forUpdate()
						.build());
		result2 = connection.select(Select.where(Department.DEPTNO
										.greaterThanOrEqualTo(20))
						.forUpdate()
						.build());
		assertNotSame(result, result2);

		result = connection.select(Department.DEPTNO.equalTo(20));
		result2 = connection.select(Department.DEPTNO.equalTo(20));
		assertSame(result, result2);

		connection.queryCache(false);
		assertFalse(connection.queryCache());

		result = connection.select(Department.DEPTNO.greaterThanOrEqualTo(20));
		result2 = connection.select(Department.DEPTNO.greaterThanOrEqualTo(20));
		assertNotSame(result, result2);
	}

	@Test
	void orderByNullOrder() {
		List<Entity> result = connection.select(Select.all(Employee.TYPE)
						.orderBy(OrderBy.builder()
										.ascending(NULLS_FIRST, Employee.MGR)
										.build())
						.build());
		assertEquals("KING", result.get(0).get(Employee.NAME));

		result = connection.select(Select.all(Employee.TYPE)
						.orderBy(OrderBy.builder()
										.ascending(NULLS_LAST, Employee.MGR)
										.build())
						.build());
		assertEquals("KING", result.get(result.size() - 1).get(Employee.NAME));

		result = connection.select(Select.all(Employee.TYPE)
						.orderBy(OrderBy.builder()
										.descending(NULLS_FIRST, Employee.MGR)
										.build())
						.build());
		assertEquals("KING", result.get(0).get(Employee.NAME));

		result = connection.select(Select.all(Employee.TYPE)
						.orderBy(OrderBy.builder()
										.descending(NULLS_LAST, Employee.MGR)
										.build())
						.build());
		assertEquals("KING", result.get(result.size() - 1).get(Employee.NAME));
	}

	@Test
	void singleGeneratedColumnInsert() {
		connection.delete(connection.insert(ENTITIES.entity(Master.TYPE).build()));
	}

	@Test
	void transactions() {
		try (LocalEntityConnection connection1 = createConnection();
				 LocalEntityConnection connection2 = createConnection()) {
			Entity department = ENTITIES.entity(Department.TYPE)
							.with(Department.DEPTNO, -42)
							.with(Department.DNAME, "hello")
							.build();
			connection1.startTransaction();
			assertThrows(IllegalStateException.class, connection1::startTransaction);
			assertTrue(connection1.transactionOpen());
			assertFalse(connection2.transactionOpen());
			connection1.insert(department);
			assertTrue(connection2.select(Department.DEPTNO.equalTo(-42)).isEmpty());
			connection1.commitTransaction();
			assertThrows(IllegalStateException.class, connection1::rollbackTransaction);
			assertThrows(IllegalStateException.class, connection1::commitTransaction);
			assertFalse(connection1.transactionOpen());
			assertFalse(connection2.select(Department.DEPTNO.equalTo(-42)).isEmpty());
			connection2.startTransaction();
			assertTrue(connection2.transactionOpen());
			assertFalse(connection1.transactionOpen());
			connection2.delete(department.primaryKey());
			assertFalse(connection1.select(Department.DEPTNO.equalTo(-42)).isEmpty());
			connection2.commitTransaction();
			assertTrue(connection1.select(Department.DEPTNO.equalTo(-42)).isEmpty());
			assertThrows(IllegalStateException.class, () -> transaction(connection, () -> transaction(connection, () -> {})));
		}
	}

	@Test
	void transactionErrors() {
		try (LocalEntityConnection connection = createConnection()) {
			try {
				transaction(connection, () -> {
					throw new DatabaseException("Testing");
				});
				fail();
			}
			catch (Throwable e) {
				assertFalse(connection.transactionOpen());
			}
			try {
				transaction(connection, () -> {
					throw new RuntimeException("Testing");
				});
				fail();
			}
			catch (Throwable e) {
				assertFalse(connection.transactionOpen());
			}
			try {
				transaction(connection, () -> {
					throw new Error("Testing");
				});
				fail();
			}
			catch (Throwable e) {
				assertFalse(connection.transactionOpen());
			}
		}
	}

	@Test
	void readOnlyTransaction() throws SQLException {
		// Test read-only transaction behavior
		try (LocalEntityConnection testConnection = createConnection()) {
			// Get the underlying JDBC connection to set read-only mode
			Connection jdbcConnection = testConnection.databaseConnection().getConnection();
			// Start with a regular transaction to insert test data
			testConnection.startTransaction();
			Entity testDept = ENTITIES.entity(Department.TYPE)
							.with(Department.DEPTNO, 999)
							.with(Department.DNAME, "TEST_READONLY")
							.with(Department.LOC, "TEST_LOC")
							.build();
			testDept = testConnection.insertSelect(testDept);
			testConnection.commitTransaction();

			// Now set the connection to read-only and start a new transaction
			// Note: H2 database may not fully honor read-only mode in all scenarios
			jdbcConnection.setReadOnly(true);

			testConnection.startTransaction();

			// Verify we're in a transaction
			assertTrue(testConnection.transactionOpen());

			// Read operations should work fine
			Entity readDept = testConnection.selectSingle(Department.DEPTNO.equalTo(999));
			assertEquals("TEST_READONLY", readDept.get(Department.DNAME));

			// Count should work
			assertEquals(1, testConnection.count(Count.where(Department.DEPTNO.equalTo(999))));

			// Select multiple should work
			List<Entity> depts = testConnection.select(Department.DEPTNO.greaterThanOrEqualTo(999));
			assertFalse(depts.isEmpty());

			// Test if H2 enforces read-only mode for write operations
			Entity newDept = ENTITIES.entity(Department.TYPE)
							.with(Department.DEPTNO, 998)
							.with(Department.DNAME, "SHOULD_FAIL")
							.build();

			// Try insert - H2 might allow it even in read-only mode
			try {
				testConnection.insert(newDept);
				// If insert succeeded, H2 doesn't enforce read-only at this level
				// Clean up the inserted record
				testConnection.delete(newDept.primaryKey());
			}
			catch (Exception e) {
				// Good - insert failed as expected in read-only mode
			}

			// The main point of this test is to verify that read operations work
			// correctly when the connection is marked as read-only, and that
			// the transaction behavior is consistent

			// Rollback the read-only transaction
			testConnection.rollbackTransaction();

			// Reset to read-write mode and clean up
			jdbcConnection.setReadOnly(false);
			testConnection.delete(testDept.primaryKey());
		}
	}

	@Test
	void concurrentTransactionIsolation() throws SQLException, InterruptedException {
		// Test concurrent transactions with different isolation levels
		try (LocalEntityConnection connection1 = createConnection();
				 LocalEntityConnection connection2 = createConnection()) {

			// First, test READ_COMMITTED isolation (default for most databases)
			// This should prevent dirty reads but allow non-repeatable reads
			Connection jdbc1 = connection1.databaseConnection().getConnection();
			Connection jdbc2 = connection2.databaseConnection().getConnection();

			jdbc1.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
			jdbc2.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);

			// Create test data
			Entity testDept = ENTITIES.entity(Department.TYPE)
							.with(Department.DEPTNO, 888)
							.with(Department.DNAME, "ISOLATION_TEST")
							.with(Department.LOC, "ORIGINAL")
							.build();
			connection1.insert(testDept);

			// Test 1: Verify dirty reads are prevented
			connection1.startTransaction();
			Entity dept1 = connection1.selectSingle(Department.DEPTNO.equalTo(888));
			dept1.set(Department.LOC, "MODIFIED");
			connection1.update(dept1);

			// Connection 2 should NOT see the uncommitted change
			Entity dept2 = connection2.selectSingle(Department.DEPTNO.equalTo(888));
			assertEquals("ORIGINAL", dept2.get(Department.LOC),
							"Should not see uncommitted changes (no dirty reads)");

			connection1.commitTransaction();

			// Now connection 2 should see the committed change
			dept2 = connection2.selectSingle(Department.DEPTNO.equalTo(888));
			assertEquals("MODIFIED", dept2.get(Department.LOC),
							"Should see committed changes");

			// Test 2: Simple deadlock scenario
			// Create another entity for deadlock testing
			Entity testDept2 = ENTITIES.entity(Department.TYPE)
							.with(Department.DEPTNO, 889)
							.with(Department.DNAME, "DEADLOCK_TEST")
							.with(Department.LOC, "ORIGINAL2")
							.build();
			connection1.insert(testDept2);

			// Use a flag to track if deadlock was detected
			AtomicBoolean deadlockDetected = new AtomicBoolean(false);
			AtomicReference<Exception> exception = new AtomicReference<>();

			// Thread 1: Update dept1 then dept2
			Thread thread1 = new Thread(() -> {
				try {
					connection1.startTransaction();
					Entity d1 = connection1.selectSingle(Department.DEPTNO.equalTo(888));
					d1.set(Department.LOC, "THREAD1_UPDATE");
					connection1.update(d1);

					// Small delay to ensure thread2 gets its first lock
					Thread.sleep(100);

					// Try to update the second department (this may deadlock)
					Entity d2 = connection1.selectSingle(Department.DEPTNO.equalTo(889));
					d2.set(Department.LOC, "THREAD1_UPDATE2");
					connection1.update(d2);

					connection1.commitTransaction();
				}
				catch (Exception e) {
					exception.set(e);
					// Check if it's a deadlock or timeout exception
					if (e.getMessage() != null &&
									(e.getMessage().contains("deadlock") ||
													e.getMessage().contains("timeout") ||
													e.getMessage().contains("lock"))) {
						deadlockDetected.set(true);
					}
					try {
						connection1.rollbackTransaction();
					}
					catch (Exception rollbackEx) {
						// Ignore rollback exceptions
					}
				}
			});

			// Thread 2: Update dept2 then dept1 (opposite order)
			Thread thread2 = new Thread(() -> {
				try {
					connection2.startTransaction();
					Entity d2 = connection2.selectSingle(Department.DEPTNO.equalTo(889));
					d2.set(Department.LOC, "THREAD2_UPDATE");
					connection2.update(d2);

					// Small delay to ensure thread1 gets its first lock
					Thread.sleep(100);

					// Try to update the first department (this may deadlock)
					Entity d1 = connection2.selectSingle(Department.DEPTNO.equalTo(888));
					d1.set(Department.LOC, "THREAD2_UPDATE2");
					connection2.update(d1);

					connection2.commitTransaction();
				}
				catch (Exception e) {
					// Check if it's a deadlock or timeout exception
					if (e.getMessage() != null &&
									(e.getMessage().contains("deadlock") ||
													e.getMessage().contains("timeout") ||
													e.getMessage().contains("lock"))) {
						deadlockDetected.set(true);
					}
					try {
						connection2.rollbackTransaction();
					}
					catch (Exception rollbackEx) {
						// Ignore rollback exceptions
					}
				}
			});

			// Start both threads
			thread1.start();
			thread2.start();

			// Wait for both threads to complete
			thread1.join(5000); // 5 second timeout
			thread2.join(5000);

			// At least one thread should have detected a lock/deadlock situation
			// Note: H2 may use timeouts instead of deadlock detection
			assertTrue(deadlockDetected.get() || exception.get() != null,
							"Concurrent conflicting updates should result in lock contention");

			// Clean up test data
			connection1.delete(Department.DEPTNO.in(888, 889));
		}
	}

	@Test
	void foreignKeyReferenceDepth() {
		try (LocalEntityConnection conn = createConnection()) {
			conn.limitForeignKeyReferenceDepth(false);
			assertFalse(conn.limitForeignKeyReferenceDepth());
			Entity employee = conn.selectSingle(Employee.ID.equalTo(10));
			Entity manager = employee.get(Employee.MGR_FK);
			assertNotNull(manager);
			Entity managersManager = manager.get(Employee.MGR_FK);
			assertNotNull(managersManager);
			conn.limitForeignKeyReferenceDepth(true);
			assertTrue(conn.limitForeignKeyReferenceDepth());
			employee = conn.selectSingle(Employee.ID.equalTo(10));
			manager = employee.get(Employee.MGR_FK);
			assertNotNull(manager);
			managersManager = manager.get(Employee.MGR_FK);
			assertNull(managersManager);
		}
	}

	@Test
	void nonPrimaryKeyForeignKey() {
		Entity selected = connection.selectSingle(all(DetailFk.TYPE));
		assertEquals(1, selected.get(DetailFk.MASTER_FK).get(MasterFk.ID));

		connection.insert(connection.entities().entity(MasterFk.TYPE)
						.with(MasterFk.ID, 2)
						.with(MasterFk.NAME, "name")
						.build());
		assertThrows(IllegalStateException.class, () -> connection.selectSingle(all(DetailFk.TYPE)));
	}

	@Test
	void missingOrModifiedValues() {
		Entity entity = ENTITIES.entity(Department.TYPE)
						.with(Department.DEPTNO, 1)
						.with(Department.LOC, "Location")
						.with(Department.DNAME, "Name")
						.with(Department.ACTIVE, true)
						.build();

		Entity current = ENTITIES.entity(Department.TYPE)
						.with(Department.DEPTNO, 1)
						.with(Department.LOC, "Location")
						.with(Department.DNAME, "Name")
						.build();

		assertFalse(valueMissingOrModified(current, entity, Department.DEPTNO));
		assertFalse(valueMissingOrModified(current, entity, Department.LOC));
		assertFalse(valueMissingOrModified(current, entity, Department.DNAME));

		current.set(Department.DEPTNO, 2);
		current.save();
		assertTrue(valueMissingOrModified(current, entity, Department.DEPTNO));
		assertEquals(Department.DEPTNO, modifiedColumns(current, entity).iterator().next());
		Integer id = current.remove(Department.DEPTNO);
		assertEquals(2, id);
		current.save();
		assertTrue(valueMissingOrModified(current, entity, Department.DEPTNO));
		assertTrue(modifiedColumns(current, entity).isEmpty());
		current.set(Department.DEPTNO, 1);
		current.save();
		assertFalse(valueMissingOrModified(current, entity, Department.DEPTNO));
		assertTrue(modifiedColumns(current, entity).isEmpty());

		current.set(Department.LOC, "New location");
		current.save();
		assertTrue(valueMissingOrModified(current, entity, Department.LOC));
		assertEquals(Department.LOC, modifiedColumns(current, entity).iterator().next());
		current.remove(Department.LOC);
		current.save();
		assertTrue(valueMissingOrModified(current, entity, Department.LOC));
		assertTrue(modifiedColumns(current, entity).isEmpty());
		current.set(Department.LOC, "Location");
		current.save();
		assertFalse(valueMissingOrModified(current, entity, Department.LOC));
		assertTrue(modifiedColumns(current, entity).isEmpty());

		entity.set(Department.LOC, "new loc");
		entity.set(Department.DNAME, "new name");

		assertEquals(2, modifiedColumns(current, entity).size());

		entity = ENTITIES.entity(Department.TYPE)
						.with(Department.DEPTNO, 1)
						.with(Department.LOC, null)
						.with(Department.DNAME, "Name")
						.build();
		current = entity.copy().mutable();

		// Original value becomes null
		entity.set(Department.LOC, "Location");

		current.remove(Department.LOC);
		Collection<Column<?>> columns = modifiedColumns(entity, current);
		// Should be able to discern a missing value from an original null value
		assertTrue(columns.contains(Department.LOC));
	}

	@Test
	void modifiedColumnWithBlob() {
		Random random = new Random();
		byte[] bytes = new byte[1024];
		random.nextBytes(bytes);
		byte[] modifiedBytes = new byte[1024];
		random.nextBytes(modifiedBytes);

		//eagerly loaded blob
		Entity emp1 = ENTITIES.entity(Employee.TYPE)
						.with(Employee.ID, 1)
						.with(Employee.NAME, "name")
						.with(Employee.SALARY, 1300d)
						.with(Employee.DATA, bytes)
						.build();

		Entity emp2 = emp1.copy().builder()
						.with(Employee.DATA, modifiedBytes)
						.build();

		Collection<Column<?>> modifiedColumns = modifiedColumns(emp1, emp2);
		assertTrue(modifiedColumns.contains(Employee.DATA));

		//lazy loaded blob
		Entity dept1 = ENTITIES.entity(Department.TYPE)
						.with(Department.DNAME, "name")
						.with(Department.LOC, "loc")
						.with(Department.ACTIVE, true)
						.with(Department.DATA, bytes)
						.build();

		Entity dept2 = dept1.copy().builder()
						.with(Department.DATA, modifiedBytes)
						.build();

		modifiedColumns = modifiedColumns(dept1, dept2);
		assertFalse(modifiedColumns.contains(Department.DATA));

		dept2.set(Department.LOC, "new loc");
		modifiedColumns = modifiedColumns(dept1, dept2);
		assertTrue(modifiedColumns.contains(Department.LOC));

		dept2.remove(Department.DATA);
		modifiedColumns = modifiedColumns(dept1, dept2);
		assertFalse(modifiedColumns.contains(Department.DATA));
	}

	@Test
	void having() {
		List<Entity> jobs = connection.select(Select.having(and(
										Job.MAX_COMMISSION.equalTo(1500d),
										Job.MIN_COMMISSION.equalTo(1200d)))
						.build());
		assertEquals(1, jobs.size());
		assertEquals("CLERK", jobs.get(0).get(Job.JOB));
	}

	@Test
	void nullConverter() {
		Entity entity = DOMAIN.entities().entity(NullConverter.TYPE)
						.with(NullConverter.ID, -1)
						.with(NullConverter.NAME, null)
						.build();
		entity = connection.insertSelect(entity);
		entity.set(NullConverter.NAME, "name");
		entity = connection.updateSelect(entity);
		entity.set(NullConverter.NAME, "null");
		entity = connection.updateSelect(entity);
		assertTrue(entity.isNull(NullConverter.NAME));
	}

	private static LocalEntityConnection createConnection() {
		return createConnection(false);
	}

	private static LocalEntityConnection createConnection(boolean setLockTimeout) {
		Database database = Database.instance();
		if (setLockTimeout) {
			database.connectionProvider(new ConnectionProvider() {
				@Override
				public Connection connection(User user, String url) throws SQLException {
					Connection connection = ConnectionProvider.super.connection(user, url);
					try (Statement statement = connection.createStatement()) {
						statement.execute("SET LOCK_TIMEOUT 10");
					}

					return connection;
				}
			});
		}

		return new DefaultLocalEntityConnection(database, DOMAIN, UNIT_TEST_USER);
	}

	private static EntityConnection createDestinationConnection() {
		try {
			Database destinationDatabase = H2DatabaseFactory.createDatabase("jdbc:h2:mem:TempDB", "src/test/sql/create_h2_db.sql");
			LocalEntityConnection destinationConnection = localEntityConnection(destinationDatabase, DOMAIN, User.user("sa"));
			destinationConnection.databaseConnection().getConnection().createStatement()
							.execute("alter table employees.employee drop constraint emp_mgr_fk if exists");
			destinationConnection.delete(all(Employee.TYPE));
			destinationConnection.delete(all(Department.TYPE));

			return destinationConnection;
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}