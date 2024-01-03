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
 * Copyright (c) 2009 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.framework.db.local;

import is.codion.common.db.database.ConnectionProvider;
import is.codion.common.db.database.Database;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.db.exception.DeleteException;
import is.codion.common.db.exception.MultipleRecordsFoundException;
import is.codion.common.db.exception.RecordModifiedException;
import is.codion.common.db.exception.RecordNotFoundException;
import is.codion.common.db.exception.ReferentialIntegrityException;
import is.codion.common.db.exception.UniqueConstraintException;
import is.codion.common.db.exception.UpdateException;
import is.codion.common.db.result.ResultIterator;
import is.codion.common.user.User;
import is.codion.dbms.h2database.H2DatabaseFactory;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnection.Count;
import is.codion.framework.db.EntityConnection.Select;
import is.codion.framework.db.EntityConnection.Update;
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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.TimeZone;
import java.util.function.Consumer;

import static is.codion.framework.db.EntityConnection.Count.where;
import static is.codion.framework.db.local.LocalEntityConnection.localEntityConnection;
import static is.codion.framework.db.local.TestDomain.*;
import static is.codion.framework.domain.entity.Entity.primaryKeys;
import static is.codion.framework.domain.entity.OrderBy.descending;
import static is.codion.framework.domain.entity.condition.Condition.*;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;

public class DefaultLocalEntityConnectionTest {

  private static final User UNIT_TEST_USER =
          User.parse(System.getProperty("codion.test.user", "scott:tiger"));

  private LocalEntityConnection connection;

  private static final TestDomain DOMAIN = new TestDomain();
  private static final Entities ENTITIES = DOMAIN.entities();

  @BeforeEach
  void setup() throws DatabaseException {
    connection = createConnection();
  }

  @AfterEach
  void tearDown() {
    connection.close();
  }

  @Test
  void configureDatabase() throws DatabaseException {
    try (LocalEntityConnection connection = new DefaultLocalEntityConnection(Database.instance(), new ConfigureDb(), UNIT_TEST_USER)) {
      //throws exception if table does not exist, which is created during connection configuration
      connection.select(all(Configured.TYPE));
    }
  }

  @Test
  void copy() throws DatabaseException {
    try (EntityConnection sourceConnection = new DefaultLocalEntityConnection(Database.instance(), DOMAIN, UNIT_TEST_USER);
         EntityConnection destinationConnection = createDestinationConnection()) {
      EntityConnection.copyEntities(sourceConnection, destinationConnection)
              .entityTypes(Department.TYPE)
              .batchSize(2)
              .execute();

      assertEquals(sourceConnection.count(Count.all(Department.TYPE)),
              destinationConnection.count(Count.all(Department.TYPE)));

      assertThrows(IllegalArgumentException.class, () -> EntityConnection.copyEntities(sourceConnection, destinationConnection)
              .entityTypes(Employee.TYPE)
              .batchSize(-10));

      EntityConnection.copyEntities(sourceConnection, destinationConnection)
              .entityTypes(Employee.TYPE)
              .batchSize(2)
              .includePrimaryKeys(false)
              .condition(Employee.SALARY.greaterThan(1000d))
              .execute();
      assertEquals(13, destinationConnection.count(Count.all(Employee.TYPE)));

      destinationConnection.delete(all(Employee.TYPE));
      destinationConnection.delete(all(Department.TYPE));
    }
  }

  @Test
  void insert() throws DatabaseException {
    try (EntityConnection sourceConnection = new DefaultLocalEntityConnection(Database.instance(), DOMAIN, UNIT_TEST_USER);
         EntityConnection destinationConnection = createDestinationConnection()) {
      List<Entity> departments = sourceConnection.select(all(Department.TYPE));
      assertThrows(IllegalArgumentException.class, () -> EntityConnection.insertEntities(destinationConnection, departments.iterator())
              .batchSize(-10));

      Consumer<Integer> progressReporter = currentProgress -> {};
      EntityConnection.insertEntities(destinationConnection, departments.iterator())
              .batchSize(2)
              .progressReporter(progressReporter)
              .onInsert(keys -> {})
              .execute();
      assertEquals(sourceConnection.count(Count.all(Department.TYPE)),
              destinationConnection.count(Count.all(Department.TYPE)));

      EntityConnection.insertEntities(destinationConnection, Collections.emptyIterator())
              .batchSize(10)
              .execute();
      destinationConnection.delete(all(Department.TYPE));
    }
  }

  @Test
  void delete() throws Exception {
    connection.beginTransaction();
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
    connection.beginTransaction();
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
    connection.beginTransaction();
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
    assertThrows(ReferentialIntegrityException.class, () -> connection.delete(key));
  }

  @Test
  void insertUniqueConstraint() {
    Entity department = ENTITIES.builder(Department.TYPE)
            .with(Department.DEPTNO, 1000)
            .with(Department.DNAME, "SALES")
            .build();
    assertThrows(UniqueConstraintException.class, () -> connection.insert(department));
  }

  @Test
  void updateUniqueConstraint() throws DatabaseException {
    Entity department = connection.selectSingle(Department.DEPTNO.equalTo(20));
    department.put(Department.DNAME, "SALES");
    assertThrows(UniqueConstraintException.class, () -> connection.update(department));
  }

  @Test
  void insertNoParentKey() {
    Entity emp = ENTITIES.builder(Employee.TYPE)
            .with(Employee.ID, -100)
            .with(Employee.NAME, "Testing")
            .with(Employee.DEPARTMENT, -1010)//not available
            .with(Employee.SALARY, 2000d)
            .build();
    assertThrows(ReferentialIntegrityException.class, () -> connection.insert(emp));
  }

  @Test
  void insertNoPk() throws DatabaseException {
    Entity noPk = ENTITIES.builder(NoPrimaryKey.TYPE)
            .with(NoPrimaryKey.COL_1, 10)
            .with(NoPrimaryKey.COL_2, "10")
            .with(NoPrimaryKey.COL_3, "10")
            .with(NoPrimaryKey.COL_4, 10)
            .build();

    Entity.Key key = connection.insert(noPk);
    assertTrue(key.isNull());
  }

  @Test
  void updateNoParentKey() throws DatabaseException {
    Entity emp = connection.selectSingle(Employee.ID.equalTo(3));
    emp.put(Employee.DEPARTMENT, -1010);//not available
    assertThrows(ReferentialIntegrityException.class, () -> connection.update(emp));
  }

  @Test
  void deleteByKeyWithForeignKeys() throws DatabaseException {
    Entity accounting = connection.selectSingle(Department.DNAME.equalTo("ACCOUNTING"));
    assertThrows(ReferentialIntegrityException.class, () -> connection.delete(accounting.primaryKey()));
  }

  @Test
  void deleteByConditionWithForeignKeys() {
    assertThrows(ReferentialIntegrityException.class, () ->
            connection.delete(Department.DNAME.equalTo("ACCOUNTING")));
  }

  @Test
  void report() throws Exception {
    Map<String, Object> reportParameters = new HashMap<>();
    reportParameters.put("DEPTNO", asList(10, 20));
    assertEquals("result", connection.report(REPORT, reportParameters));
  }

  @Test
  void dependencies() throws Exception {
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
  void selectLimitOffset() throws Exception {
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
  void select() throws Exception {
    Collection<Entity> result = connection.select(new ArrayList<>());
    assertTrue(result.isEmpty());
    result = connection.select(Department.DEPTNO.in(10, 20));
    assertEquals(2, result.size());
    result = connection.select(primaryKeys(result));
    assertEquals(2, result.size());
    result = connection.select(Condition.custom(Department.DEPARTMENT_CONDITION_TYPE,
            asList(Department.DEPTNO, Department.DEPTNO), asList(10, 20)));
    assertEquals(2, result.size());
    result = connection.select(Condition.custom(EmpnoDeptno.CONDITION));
    assertEquals(7, result.size());

    Select select = Select.where(Condition.custom(Employee.NAME_IS_BLAKE_CONDITION)).build();
    result = connection.select(select);
    Entity emp = result.iterator().next();
    assertNotNull(emp.get(Employee.DEPARTMENT_FK));
    assertNotNull(emp.get(Employee.MGR_FK));
    emp = emp.referencedEntity(Employee.MGR_FK);
    assertNull(emp.get(Employee.MGR_FK));

    select = Select.where(select.where())
            .fetchDepth(Employee.DEPARTMENT_FK, 0)
            .build();
    result = connection.select(select);
    assertEquals(1, result.size());
    emp = result.iterator().next();
    assertNull(emp.get(Employee.DEPARTMENT_FK));
    assertNotNull(emp.get(Employee.MGR_FK));

    select = Select.where(select.where())
            .fetchDepth(Employee.DEPARTMENT_FK, 0)
            .fetchDepth(Employee.MGR_FK, 0)
            .build();
    result = connection.select(select);
    assertEquals(1, result.size());
    emp = result.iterator().next();
    assertNull(emp.get(Employee.DEPARTMENT_FK));
    assertNull(emp.get(Employee.MGR_FK));

    select = Select.where(select.where())
            .fetchDepth(Employee.DEPARTMENT_FK, 0)
            .fetchDepth(Employee.MGR_FK, 2)
            .build();
    result = connection.select(select);
    assertEquals(1, result.size());
    emp = result.iterator().next();
    assertNull(emp.get(Employee.DEPARTMENT_FK));
    assertNotNull(emp.get(Employee.MGR_FK));
    emp = emp.referencedEntity(Employee.MGR_FK);
    assertNotNull(emp.get(Employee.MGR_FK));

    select = Select.where(select.where())
            .fetchDepth(Employee.DEPARTMENT_FK, 0)
            .fetchDepth(Employee.MGR_FK, -1)
            .build();
    result = connection.select(select);
    assertEquals(1, result.size());
    emp = result.iterator().next();
    assertNull(emp.get(Employee.DEPARTMENT_FK));
    assertNotNull(emp.get(Employee.MGR_FK));
    emp = emp.referencedEntity(Employee.MGR_FK);
    assertNotNull(emp.get(Employee.MGR_FK));

    assertEquals(4, connection.count(where(Employee.ID.in(asList(1, 2, 3, 4)))));
    assertEquals(0, connection.count(where(Employee.DEPARTMENT.isNull())));
    assertEquals(0, connection.count(where(Employee.DEPARTMENT_FK.isNull())));
    assertEquals(1, connection.count(where(Employee.MGR.isNull())));
    assertEquals(1, connection.count(where(Employee.MGR_FK.isNull())));

    assertFalse(connection.select(Employee.DEPARTMENT_FK.in(connection.select(Department.DEPTNO.equalTo(20)))).isEmpty());
  }

  @Test
  void selectLimit() throws DatabaseException {
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
    departments = connection.select(selectBuilder.limit(-1).build());
    assertEquals(4, departments.size());
  }

  @Test
  void selectByKey() throws DatabaseException {
    Entity.Key deptKey = ENTITIES.primaryKey(Department.TYPE, 10);
    Entity.Key empKey = ENTITIES.primaryKey(Employee.TYPE, 8);

    Collection<Entity> selected = connection.select(asList(deptKey, empKey));
    assertEquals(2, selected.size());
  }

  @Test
  void foreignKeyAttributes() throws DatabaseException {
    List<Entity> emps = connection.select(Select.where(EmployeeFk.MGR_FK.isNotNull())
            .fetchDepth(EmployeeFk.MGR_FK, 2)
            .build());
    for (Entity emp : emps) {
      Entity mgr = emp.referencedEntity(EmployeeFk.MGR_FK);
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
  void attributes() throws Exception {
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
    assertThrows(DatabaseException.class, () -> connection.select(Condition.custom(Department.DEPARTMENT_CONDITION_INVALID_COLUMN_TYPE)));
  }

  @Test
  void count() throws Exception {
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
  void selectSingle() throws Exception {
    Entity sales = connection.selectSingle(Department.DNAME.equalTo("SALES"));
    assertEquals(sales.get(Department.DNAME), "SALES");
    sales = connection.select(sales.primaryKey());
    assertEquals(sales.get(Department.DNAME), "SALES");
    sales = connection.selectSingle(Condition.custom(Department.DEPARTMENT_CONDITION_SALES_TYPE));
    assertEquals(sales.get(Department.DNAME), "SALES");

    Entity king = connection.selectSingle(Employee.NAME.equalTo("KING"));
    assertTrue(king.contains(Employee.MGR_FK));
    assertNull(king.get(Employee.MGR_FK));

    king = connection.selectSingle(Employee.MGR_FK.isNull());
    assertNull(king.get(Employee.MGR_FK));
  }

  @Test
  void customCondition() throws DatabaseException {
    assertEquals(4, connection.select(Condition.custom(Employee.MGR_GREATER_THAN_CONDITION,
            singletonList(Employee.MGR), singletonList(5))).size());
  }

  @Test
  void executeFunction() throws DatabaseException {
    connection.execute(FUNCTION_ID);
  }

  @Test
  void executeProcedure() throws DatabaseException {
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
    connection.beginTransaction();
    try {
      Entity department = ENTITIES.entity(Department.TYPE);
      assertThrows(DatabaseException.class, () -> connection.insert(department));
    }
    finally {
      connection.rollbackTransaction();
    }
  }

  @Test
  void updateNoModifiedValues() throws DatabaseException {
    connection.beginTransaction();
    try {
      Entity department = connection.selectSingle(Department.DEPTNO.equalTo(10));
      assertThrows(DatabaseException.class, () -> connection.update(department));
    }
    finally {
      connection.rollbackTransaction();
    }
  }

  @Test
  void dateTime() throws DatabaseException {
    Entity sales = connection.selectSingle(Department.DNAME.equalTo("SALES"));
    final double salary = 1500;

    Entity emp = ENTITIES.builder(Employee.TYPE)
            .with(Employee.DEPARTMENT_FK, sales)
            .with(Employee.NAME, "Nobody")
            .with(Employee.SALARY, salary)
            .build();
    LocalDate hiredate = LocalDate.parse("03-10-1975", DateTimeFormatter.ofPattern("dd-MM-yyyy"));
    emp.put(Employee.HIREDATE, hiredate);
    OffsetDateTime hiretime = LocalDateTime.parse("03-10-1975 08:30:22", DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"))
            .atZone(TimeZone.getDefault().toZoneId()).toOffsetDateTime();
    emp.put(Employee.HIRETIME, hiretime);

    emp = connection.select(connection.insert(emp));

    assertEquals(hiredate, emp.get(Employee.HIREDATE));
    assertEquals(hiretime, emp.get(Employee.HIRETIME));

    connection.delete(emp.primaryKey());
  }

  @Test
  void insertWithNullValues() throws DatabaseException {
    Entity sales = connection.selectSingle(Department.DNAME.equalTo("SALES"));
    final String name = "Nobody";
    final double salary = 1500;
    final double defaultCommission = 200;

    Entity emp = ENTITIES.builder(Employee.TYPE)
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

    emp.put(Employee.COMMISSION, null);//default value should not kick in
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
  void insertEmptyList() throws DatabaseException {
    assertTrue(connection.insert(new ArrayList<>()).isEmpty());
  }

  @Test
  void updateDifferentEntities() throws DatabaseException {
    connection.beginTransaction();
    try {
      Entity sales = connection.selectSingle(Department.DNAME.equalTo("SALES"));
      Entity king = connection.selectSingle(Employee.NAME.equalTo("KING"));
      final String newName = "New name";
      sales.put(Department.DNAME, newName);
      king.put(Employee.NAME, newName);
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
  void updateNonExisting() throws DatabaseException {
    Entity employee = connection.selectSingle(EmployeeNonOpt.ID.equalTo(4));
    employee.put(EmployeeNonOpt.ID, -888);//non existing
    employee.save();
    employee.put(EmployeeNonOpt.NAME, "New name");
    assertThrows(UpdateException.class, () -> connection.update(employee));
  }

  @Test
  void update() throws DatabaseException {
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
    Update update = Update.all(Employee.TYPE).build();
    assertThrows(IllegalArgumentException.class, () -> connection.update(update));
  }

  @Test
  void updateWithCondition() throws DatabaseException {
    Condition condition = Employee.COMMISSION.isNull();

    List<Entity> entities = connection.select(condition);

    Update update = Update.where(Employee.COMMISSION.isNull())
            .set(Employee.COMMISSION, 500d)
            .set(Employee.SALARY, 4200d)
            .build();
    connection.beginTransaction();
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
  void updateWithConditionNoRows() throws DatabaseException {
    Update update = Update.where(Employee.ID.isNull())
            .set(Employee.SALARY, 4200d)
            .build();
    connection.beginTransaction();
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
  void selectValues() throws Exception {
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
  void selectValuesCustomQuery() throws Exception {
    connection.select(EmpnoDeptno.DEPTNO);
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
  void selectValuesLimit() throws DatabaseException {
    List<Integer> deptnos = connection.select(Department.DEPTNO, Select.all(Department.TYPE)
            .limit(2)
            .orderBy(descending(Department.DEPTNO))
            .build());
    assertEquals(40, deptnos.get(0));
    assertEquals(30, deptnos.get(1));
  }

  @Test
  void selectForUpdateModified() throws Exception {
    LocalEntityConnection connection = createConnection();
    LocalEntityConnection connection2 = createConnection();
    String originalLocation;
    try {
      Select select = Select.where(Department.DNAME.equalTo("SALES")).forUpdate().build();

      Entity sales = connection.selectSingle(select);
      originalLocation = sales.get(Department.LOC);

      sales.put(Department.LOC, "Syracuse");
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
        sales.put(Department.LOC, originalLocation);
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
  void optimisticLockingDeleted() throws Exception {
    LocalEntityConnection connection = createConnection();
    EntityConnection connection2 = createConnection();
    connection.setOptimisticLocking(true);
    Entity allen;
    try {
      Condition condition = Employee.NAME.equalTo("ALLEN");

      allen = connection.selectSingle(condition);

      connection2.delete(allen.primaryKey());

      allen.put(Employee.JOB, "CLERK");
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
  void optimisticLockingModified() throws Exception {
    LocalEntityConnection baseConnection = createConnection();
    LocalEntityConnection optimisticConnection = createConnection(true);
    optimisticConnection.setOptimisticLocking(true);
    assertTrue(optimisticConnection.isOptimisticLocking());
    String oldLocation = null;
    Entity updatedDepartment = null;
    try {
      Entity department = baseConnection.selectSingle(Department.DNAME.equalTo("SALES"));
      oldLocation = department.put(Department.LOC, "NEWLOC");
      updatedDepartment = baseConnection.updateSelect(department);
      try {
        optimisticConnection.update(department);
        fail("RecordModifiedException should have been thrown");
      }
      catch (RecordModifiedException e) {
        assertTrue(((Entity) e.modifiedRow()).valuesEqual(updatedDepartment));
        assertTrue(((Entity) e.row()).valuesEqual(department));
      }
    }
    finally {
      try {
        if (updatedDepartment != null && oldLocation != null) {
          updatedDepartment.put(Department.LOC, oldLocation);
          baseConnection.update(updatedDepartment);
        }
      }
      catch (DatabaseException e) {
        e.printStackTrace();
      }
      baseConnection.close();
      optimisticConnection.close();
    }
  }

  @Test
  void optimisticLockingBlob() throws Exception {
    LocalEntityConnection baseConnection = createConnection();
    LocalEntityConnection optimisticConnection = createConnection();
    optimisticConnection.setOptimisticLocking(true);
    Entity updatedEmployee = null;
    try {
      Random random = new Random();
      byte[] bytes = new byte[1024];
      random.nextBytes(bytes);

      Entity employee = baseConnection.selectSingle(Employee.NAME.equalTo("BLAKE"));
      employee.put(Employee.DATA, bytes);
      updatedEmployee = baseConnection.updateSelect(employee);

      random.nextBytes(bytes);
      employee.put(Employee.DATA, bytes);

      try {
        optimisticConnection.update(employee);
        fail("RecordModifiedException should have been thrown");
      }
      catch (RecordModifiedException e) {
        //use columns here since the modified row entity contains no foreign key values
        Collection<Column<?>> columns = updatedEmployee.definition().columns().get();
        assertTrue(((Entity) e.modifiedRow()).valuesEqual(updatedEmployee, columns));
        assertTrue(((Entity) e.row()).valuesEqual(employee, columns));
      }
    }
    finally {
      baseConnection.close();
      optimisticConnection.close();
    }
  }

  @Test
  void iterator() throws Exception {
    try (LocalEntityConnection connection = createConnection()) {
      Condition condition = all(Employee.TYPE);
      ResultIterator<Entity> resultIterator = connection.iterator(condition);
      Iterator<Entity> iterator = resultIterator.iterator();
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
      resultIterator.close();
      resultIterator = connection.iterator(condition);
      iterator = resultIterator.iterator();
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
  void dualIterator() throws Exception {
    try (LocalEntityConnection connection = createConnection();
         ResultIterator<Entity> deptIterator = connection.iterator(all(Department.TYPE))) {
      while (deptIterator.hasNext()) {
        try (ResultIterator<Entity> empIterator =
                     connection.iterator(Employee.DEPARTMENT_FK.equalTo(deptIterator.next()))) {
          while (empIterator.hasNext()) {
            empIterator.next();
          }
        }
      }
    }
  }

  @Test
  void testConstructor() throws Exception {
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
  void readWriteBlob() throws DatabaseException {
    byte[] lazyBytes = new byte[1024];
    byte[] bytes = new byte[1024];
    Random random = new Random();
    random.nextBytes(lazyBytes);
    random.nextBytes(bytes);

    Entity scott = connection.selectSingle(Employee.ID.equalTo(7));
    scott.put(Employee.DATA_LAZY, lazyBytes);
    scott.put(Employee.DATA, bytes);
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

    scott.put(Employee.DATA_LAZY, newLazyBytes);
    scott.put(Employee.DATA, newBytes);

    connection.update(scott);

    lazyFromDb = connection.select(Employee.DATA_LAZY, key(scott.primaryKey())).get(0);
    assertArrayEquals(newLazyBytes, lazyFromDb);

    scottFromDb = connection.select(scott.primaryKey());
    assertArrayEquals(newBytes, scottFromDb.get(Employee.DATA));
  }

  @Test
  void testUUIDPrimaryKeyColumnWithDefaultValue() throws DatabaseException {
    Entity entity = ENTITIES.builder(UUIDTestDefault.TYPE)
            .with(UUIDTestDefault.DATA, "test")
            .build();
    connection.insert(entity);
    assertNotNull(entity.get(UUIDTestDefault.ID));
    assertEquals("test", entity.get(UUIDTestDefault.DATA));
  }

  @Test
  void testUUIDPrimaryKeyColumnWithoutDefaultValue() throws DatabaseException {
    Entity entity = ENTITIES.builder(UUIDTestNoDefault.TYPE)
            .with(UUIDTestNoDefault.DATA, "test")
            .build();
    connection.insert(entity);
    assertNotNull(entity.get(UUIDTestNoDefault.ID));
    assertEquals("test", entity.get(UUIDTestNoDefault.DATA));
  }

  @Test
  void entityWithoutPrimaryKey() throws DatabaseException {
    List<Entity> entities = connection.select(all(NoPrimaryKey.TYPE));
    assertEquals(6, entities.size());
    entities = connection.select(or(
            NoPrimaryKey.COL_1.equalTo(2),
            NoPrimaryKey.COL_3.equalTo("5")));
    assertEquals(4, entities.size());
  }

  @Test
  void beans() throws DatabaseException {
    connection.beginTransaction();
    try {
      List<Entity> departments = connection.select(all(Department.TYPE));
      Department department = departments.get(0).castTo(Department.class);
      department.setName("New Name");

      department = connection.updateSelect(department).castTo(Department.class);

      assertEquals("New Name", department.getName());

      Collection<Department> departmentsCast = Entity.castTo(Department.class, connection.select(all(Department.TYPE)));

      departmentsCast.forEach(dept -> dept.setName(dept.getName() + "N"));

      departmentsCast = Entity.castTo(Department.class, connection.updateSelect(new ArrayList<>(departmentsCast)));

      Department newDept1 = ENTITIES.entity(Department.TYPE).castTo(Department.class);
      newDept1.setId(-1);
      newDept1.setName("hello1");
      newDept1.setLocation("location");

      Department newDept2 = ENTITIES.entity(Department.TYPE).castTo(Department.class);
      newDept2.setId(-2);
      newDept2.setName("hello2");
      newDept2.setLocation("location");

      List<Entity.Key> keys = new ArrayList<>(connection.insert(asList(newDept1, newDept2)));
      assertEquals(Integer.valueOf(-1), keys.get(0).get());
      assertEquals(Integer.valueOf(-2), keys.get(1).get());
    }
    finally {
      connection.rollbackTransaction();
    }
  }

  @Test
  void selectQuery() throws DatabaseException {
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
  void queryCache() throws DatabaseException {
    connection.setQueryCacheEnabled(true);
    assertTrue(connection.isQueryCacheEnabled());

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

    connection.setQueryCacheEnabled(false);
    assertFalse(connection.isQueryCacheEnabled());

    result = connection.select(Department.DEPTNO.greaterThanOrEqualTo(20));
    result2 = connection.select(Department.DEPTNO.greaterThanOrEqualTo(20));
    assertNotSame(result, result2);
  }

  @Test
  void orderByNullOrder() throws DatabaseException {
    List<Entity> result = connection.select(Select.all(Employee.TYPE)
            .orderBy(OrderBy.builder()
                    .ascendingNullsFirst(Employee.MGR)
                    .build())
            .build());
    assertEquals(result.get(0).get(Employee.NAME), "KING");

    result = connection.select(Select.all(Employee.TYPE)
            .orderBy(OrderBy.builder()
                    .ascendingNullsLast(Employee.MGR)
                    .build())
            .build());
    assertEquals(result.get(result.size() - 1).get(Employee.NAME), "KING");

    result = connection.select(Select.all(Employee.TYPE)
            .orderBy(OrderBy.builder()
                    .descendingNullsFirst(Employee.MGR)
                    .build())
            .build());
    assertEquals(result.get(0).get(Employee.NAME), "KING");

    result = connection.select(Select.all(Employee.TYPE)
            .orderBy(OrderBy.builder()
                    .descendingNullsLast(Employee.MGR)
                    .build())
            .build());
    assertEquals(result.get(result.size() - 1).get(Employee.NAME), "KING");
  }

  @Test
  void singleGeneratedColumnInsert() throws DatabaseException {
    connection.delete(connection.insert(ENTITIES.builder(Master.TYPE).build()));
  }

  @Test
  void transactions() throws DatabaseException {
    try (LocalEntityConnection connection1 = createConnection();
         LocalEntityConnection connection2 = createConnection()) {
      Entity department = ENTITIES.builder(Department.TYPE)
              .with(Department.DEPTNO, -42)
              .with(Department.DNAME, "hello")
              .build();
      connection1.beginTransaction();
      assertThrows(IllegalStateException.class, connection1::beginTransaction);
      assertTrue(connection1.transactionOpen());
      assertFalse(connection2.transactionOpen());
      connection1.insert(department);
      assertTrue(connection2.select(Department.DEPTNO.equalTo(-42)).isEmpty());
      connection1.commitTransaction();
      assertThrows(IllegalStateException.class, connection1::rollbackTransaction);
      assertThrows(IllegalStateException.class, connection1::commitTransaction);
      assertFalse(connection1.transactionOpen());
      assertFalse(connection2.select(Department.DEPTNO.equalTo(-42)).isEmpty());
      connection2.beginTransaction();
      assertTrue(connection2.transactionOpen());
      assertFalse(connection1.transactionOpen());
      connection2.delete(department.primaryKey());
      assertFalse(connection1.select(Department.DEPTNO.equalTo(-42)).isEmpty());
      connection2.commitTransaction();
      assertTrue(connection1.select(Department.DEPTNO.equalTo(-42)).isEmpty());
    }
  }

  @Test
  void foreignKeyFetchDepth() throws DatabaseException {
    try (LocalEntityConnection connection = createConnection()) {
      connection.setLimitForeignKeyFetchDepth(false);
      assertFalse(connection.isLimitForeignKeyFetchDepth());
      Entity employee = connection.selectSingle(Employee.ID.equalTo(10));
      Entity manager = employee.get(Employee.MGR_FK);
      assertNotNull(manager);
      Entity managersManager = manager.get(Employee.MGR_FK);
      assertNotNull(managersManager);
      connection.setLimitForeignKeyFetchDepth(true);
      assertTrue(connection.isLimitForeignKeyFetchDepth());
      employee = connection.selectSingle(Employee.ID.equalTo(10));
      manager = employee.get(Employee.MGR_FK);
      assertNotNull(manager);
      managersManager = manager.get(Employee.MGR_FK);
      assertNull(managersManager);
    }
  }

  @Test
  void nonPrimaryKeyForeignKey() throws DatabaseException {
    Entity selected = connection.selectSingle(all(DetailFk.TYPE));
    assertEquals(1, selected.get(DetailFk.MASTER_FK).get(MasterFk.ID));

    connection.insert(connection.entities().builder(MasterFk.TYPE)
            .with(MasterFk.ID, 2)
            .with(MasterFk.NAME, "name")
            .build());
    assertThrows(IllegalStateException.class, () -> connection.selectSingle(all(DetailFk.TYPE)));
  }

  @Test
  void modifiedColumns() {
    Entity entity = ENTITIES.builder(Department.TYPE)
            .with(Department.DEPTNO, 1)
            .with(Department.LOC, "Location")
            .with(Department.DNAME, "Name")
            .with(Department.ACTIVE, true)
            .build();

    Entity current = ENTITIES.builder(Department.TYPE)
            .with(Department.DEPTNO, 1)
            .with(Department.LOC, "Location")
            .with(Department.DNAME, "Name")
            .build();

    assertFalse(DefaultLocalEntityConnection.valueMissingOrModified(current, entity, Department.DEPTNO));
    assertFalse(DefaultLocalEntityConnection.valueMissingOrModified(current, entity, Department.LOC));
    assertFalse(DefaultLocalEntityConnection.valueMissingOrModified(current, entity, Department.DNAME));

    current.put(Department.DEPTNO, 2);
    current.save();
    assertTrue(DefaultLocalEntityConnection.valueMissingOrModified(current, entity, Department.DEPTNO));
    assertEquals(Department.DEPTNO, DefaultLocalEntityConnection.modifiedColumns(current, entity).iterator().next());
    Integer id = current.remove(Department.DEPTNO);
    assertEquals(2, id);
    current.save();
    assertTrue(DefaultLocalEntityConnection.valueMissingOrModified(current, entity, Department.DEPTNO));
    assertTrue(DefaultLocalEntityConnection.modifiedColumns(current, entity).isEmpty());
    current.put(Department.DEPTNO, 1);
    current.save();
    assertFalse(DefaultLocalEntityConnection.valueMissingOrModified(current, entity, Department.DEPTNO));
    assertTrue(DefaultLocalEntityConnection.modifiedColumns(current, entity).isEmpty());

    current.put(Department.LOC, "New location");
    current.save();
    assertTrue(DefaultLocalEntityConnection.valueMissingOrModified(current, entity, Department.LOC));
    assertEquals(Department.LOC, DefaultLocalEntityConnection.modifiedColumns(current, entity).iterator().next());
    current.remove(Department.LOC);
    current.save();
    assertTrue(DefaultLocalEntityConnection.valueMissingOrModified(current, entity, Department.LOC));
    assertTrue(DefaultLocalEntityConnection.modifiedColumns(current, entity).isEmpty());
    current.put(Department.LOC, "Location");
    current.save();
    assertFalse(DefaultLocalEntityConnection.valueMissingOrModified(current, entity, Department.LOC));
    assertTrue(DefaultLocalEntityConnection.modifiedColumns(current, entity).isEmpty());

    entity.put(Department.LOC, "new loc");
    entity.put(Department.DNAME, "new name");

    assertEquals(2, DefaultLocalEntityConnection.modifiedColumns(current, entity).size());
  }

  @Test
  void modifiedColumnWithBlob() {
    Random random = new Random();
    byte[] bytes = new byte[1024];
    random.nextBytes(bytes);
    byte[] modifiedBytes = new byte[1024];
    random.nextBytes(modifiedBytes);

    //eagerly loaded blob
    Entity emp1 = ENTITIES.builder(Employee.TYPE)
            .with(Employee.ID, 1)
            .with(Employee.NAME, "name")
            .with(Employee.SALARY, 1300d)
            .with(Employee.DATA, bytes)
            .build();

    Entity emp2 = emp1.copyBuilder()
            .with(Employee.DATA, modifiedBytes)
            .build();

    Collection<Column<?>> modifiedColumns = DefaultLocalEntityConnection.modifiedColumns(emp1, emp2);
    assertTrue(modifiedColumns.contains(Employee.DATA));

    //lazy loaded blob
    Entity dept1 = ENTITIES.builder(Department.TYPE)
            .with(Department.DNAME, "name")
            .with(Department.LOC, "loc")
            .with(Department.ACTIVE, true)
            .with(Department.DATA, bytes)
            .build();

    Entity dept2 = dept1.copyBuilder()
            .with(Department.DATA, modifiedBytes)
            .build();

    modifiedColumns = DefaultLocalEntityConnection.modifiedColumns(dept1, dept2);
    assertFalse(modifiedColumns.contains(Department.DATA));

    dept2.put(Department.LOC, "new loc");
    modifiedColumns = DefaultLocalEntityConnection.modifiedColumns(dept1, dept2);
    assertTrue(modifiedColumns.contains(Department.LOC));

    dept2.remove(Department.DATA);
    modifiedColumns = DefaultLocalEntityConnection.modifiedColumns(dept1, dept2);
    assertFalse(modifiedColumns.contains(Department.DATA));
  }

  @Test
  void having() throws Exception {
    List<Entity> jobs = connection.select(Select.where(all(Job.TYPE))
            .having(and(
                    Job.MAX_COMMISSION.equalTo(1500d),
                    Job.MIN_COMMISSION.equalTo(1200d)))
            .build());
    assertEquals(1, jobs.size());
    assertEquals("CLERK", jobs.get(0).get(Job.JOB));
  }

  private static LocalEntityConnection createConnection() throws DatabaseException {
    return createConnection(false);
  }

  private static LocalEntityConnection createConnection(boolean setLockTimeout) throws DatabaseException {
    Database database = Database.instance();
    if (setLockTimeout) {
      database.setConnectionProvider(new ConnectionProvider() {
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
      destinationConnection.databaseConnection().getConnection().createStatement().execute("alter table scott.emp drop constraint emp_mgr_fk if exists");
      destinationConnection.delete(all(Employee.TYPE));
      destinationConnection.delete(all(Department.TYPE));

      return destinationConnection;
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}