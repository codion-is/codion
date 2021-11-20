/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.local;

import is.codion.common.db.database.Database;
import is.codion.common.db.database.DatabaseFactory;
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
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.condition.Condition;
import is.codion.framework.db.condition.Conditions;
import is.codion.framework.db.condition.SelectCondition;
import is.codion.framework.db.condition.UpdateCondition;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.Key;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TimeZone;

import static is.codion.framework.db.condition.Conditions.condition;
import static is.codion.framework.db.condition.Conditions.where;
import static is.codion.framework.db.local.TestDomain.*;
import static is.codion.framework.domain.entity.Entity.getPrimaryKeys;
import static is.codion.framework.domain.entity.OrderBy.orderBy;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;

public class DefaultLocalEntityConnectionTest {

  private static final User UNIT_TEST_USER =
          User.parseUser(System.getProperty("codion.test.user", "scott:tiger"));

  private DefaultLocalEntityConnection connection;

  private static final TestDomain DOMAIN = new TestDomain();
  private static final Entities ENTITIES = DOMAIN.getEntities();

  @BeforeEach
  void setup() throws ClassNotFoundException, DatabaseException {
    connection = initializeConnection();
  }

  @AfterEach
  void tearDown() {
    connection.close();
  }

  @Test
  void delete() throws Exception {
    connection.beginTransaction();
    try {
      final Key key = ENTITIES.primaryKey(Department.TYPE, 40);
      connection.delete(new ArrayList<>());
      connection.delete(key);
      try {
        connection.selectSingle(key);
        fail();
      }
      catch (final DatabaseException ignored) {/*ignored*/}
    }
    finally {
      connection.rollbackTransaction();
    }
    connection.beginTransaction();
    try {
      final Key key = ENTITIES.primaryKey(Department.TYPE, 40);
      assertEquals(1, connection.delete(Conditions.condition(key)));
      try {
        connection.selectSingle(key);
        fail();
      }
      catch (final DatabaseException ignored) {/*ignored*/}
    }
    finally {
      connection.rollbackTransaction();
    }
    connection.beginTransaction();
    try {
      //scott, james, adams
      assertEquals(3, connection.delete(where(Employee.NAME).equalTo("%S%")
              .and(where(Employee.JOB).equalTo("CLERK"))));
    }
    finally {
      connection.rollbackTransaction();
    }
  }

  @Test
  void deleteRowNumberMismatch() {
    final Key key400 = ENTITIES.primaryKey(Department.TYPE, 400);
    assertThrows(DeleteException.class, () -> connection.delete(key400));
    final Key key40 = ENTITIES.primaryKey(Department.TYPE, 40);
    assertThrows(DeleteException.class, () -> connection.delete(asList(key40, key400)));
  }

  @Test
  void deleteReferentialIntegrity() {
    final Key key = ENTITIES.primaryKey(Department.TYPE, 10);
    assertThrows(ReferentialIntegrityException.class, () -> connection.delete(key));
  }

  @Test
  void insertUniqueConstraint() {
    final Entity department = ENTITIES.builder(Department.TYPE)
            .with(Department.DEPTNO, 1000)
            .with(Department.DNAME, "SALES")
            .build();
    assertThrows(UniqueConstraintException.class, () -> connection.insert(department));
  }

  @Test
  void updateUniqueConstraint() throws DatabaseException {
    final Entity department = connection.selectSingle(Department.DEPTNO, 20);
    department.put(Department.DNAME, "SALES");
    assertThrows(UniqueConstraintException.class, () -> connection.update(department));
  }

  @Test
  void insertNoParentKey() {
    final Entity emp = ENTITIES.builder(Employee.TYPE)
            .with(Employee.ID, -100)
            .with(Employee.NAME, "Testing")
            .with(Employee.DEPARTMENT, -1010)//not available
            .with(Employee.SALARY, 2000d)
            .build();
    assertThrows(ReferentialIntegrityException.class, () -> connection.insert(emp));
  }

  @Test
  void insertNoPk() throws DatabaseException {
    final Entity noPk = ENTITIES.builder(T_NO_PK)
            .with(NO_PK_COL1, 10)
            .with(NO_PK_COL2, "10")
            .with(NO_PK_COL3, "10")
            .with(NO_PK_COL4, 10)
            .build();

    final Key key = connection.insert(noPk);
    assertTrue(key.isNull());
  }

  @Test
  void updateNoParentKey() throws DatabaseException {
    final Entity emp = connection.selectSingle(Employee.ID, 3);
    emp.put(Employee.DEPARTMENT, -1010);//not available
    assertThrows(ReferentialIntegrityException.class, () -> connection.update(emp));
  }

  @Test
  void deleteByKeyWithForeignKeys() throws DatabaseException {
    final Entity accounting = connection.selectSingle(Department.DNAME, "ACCOUNTING");
    assertThrows(ReferentialIntegrityException.class, () -> connection.delete(accounting.getPrimaryKey()));
  }

  @Test
  void deleteByConditionWithForeignKeys() throws DatabaseException {
    assertThrows(ReferentialIntegrityException.class, () ->
            connection.delete(where(Department.DNAME).equalTo("ACCOUNTING")));
  }

  @Test
  void fillReport() throws Exception {
    final Map<String, Object> reportParameters = new HashMap<>();
    reportParameters.put("DEPTNO", asList(10, 20));
    assertEquals("result", connection.fillReport(REPORT, reportParameters));
  }

  @Test
  void selectDependencies() throws Exception {
    final Map<EntityType, Collection<Entity>> empty = connection.selectDependencies(new ArrayList<>());
    assertTrue(empty.isEmpty());
    final List<Entity> accounting = connection.select(Department.DNAME, "ACCOUNTING");
    final Map<EntityType, Collection<Entity>> emps = connection.selectDependencies(accounting);
    assertEquals(1, emps.size());
    assertTrue(emps.containsKey(Employee.TYPE));
    assertEquals(7, emps.get(Employee.TYPE).size());

    final Entity emp = connection.selectSingle(Employee.NAME, "KING");
    final Map<EntityType, Collection<Entity>> deps = connection.selectDependencies(singletonList(emp));
    assertTrue(deps.isEmpty());//soft foreign key reference
  }

  @Test
  void selectLimitOffset() throws Exception {
    final SelectCondition condition = condition(Employee.TYPE).toSelectCondition()
            .orderBy(orderBy().ascending(Employee.NAME)).limit(2);
    List<Entity> result = connection.select(condition);
    assertEquals(2, result.size());
    condition.limit(3);
    condition.offset(3);
    result = connection.select(condition);
    assertEquals(3, result.size());
    assertEquals("BLAKE", result.get(0).get(Employee.NAME));
    assertEquals("CLARK", result.get(1).get(Employee.NAME));
    assertEquals("FORD", result.get(2).get(Employee.NAME));
  }

  @Test
  void selectWhereNull() throws Exception {
    connection.select(Employee.MGR_FK, (Entity) null);
    connection.select(Employee.DATA_LAZY, (byte[]) null);
    assertThrows(NullPointerException.class, () -> connection.select(Employee.DATA_LAZY, (Collection<byte[]>) null));
  }

  @Test
  void select() throws Exception {
    List<Entity> result = connection.select(new ArrayList<>());
    assertTrue(result.isEmpty());
    result = connection.select(Department.DEPTNO, asList(10, 20));
    assertEquals(2, result.size());
    result = connection.select(getPrimaryKeys(result));
    assertEquals(2, result.size());
    result = connection.select(Conditions.customCondition(Department.DEPARTMENT_CONDITION_TYPE,
                    asList(Department.DEPTNO, Department.DEPTNO), asList(10, 20)));
    assertEquals(2, result.size());
    result = connection.select(Conditions.customCondition(JOINED_QUERY_CONDITION_TYPE));
    assertEquals(7, result.size());

    final SelectCondition condition = Conditions.customCondition(Employee.NAME_IS_BLAKE_CONDITION_ID).toSelectCondition();
    result = connection.select(condition);
    Entity emp = result.get(0);
    assertTrue(emp.isLoaded(Employee.DEPARTMENT_FK));
    assertTrue(emp.isLoaded(Employee.MGR_FK));
    emp = emp.getForeignKey(Employee.MGR_FK);
    assertFalse(emp.isLoaded(Employee.MGR_FK));

    result = connection.select(condition.fetchDepth(Employee.DEPARTMENT_FK, 0));
    assertEquals(1, result.size());
    emp = result.get(0);
    assertFalse(emp.isLoaded(Employee.DEPARTMENT_FK));
    assertTrue(emp.isLoaded(Employee.MGR_FK));

    result = connection.select(condition.fetchDepth(Employee.MGR_FK, 0));
    assertEquals(1, result.size());
    emp = result.get(0);
    assertFalse(emp.isLoaded(Employee.DEPARTMENT_FK));
    assertFalse(emp.isLoaded(Employee.MGR_FK));

    result = connection.select(condition.fetchDepth(Employee.MGR_FK, 2));
    assertEquals(1, result.size());
    emp = result.get(0);
    assertFalse(emp.isLoaded(Employee.DEPARTMENT_FK));
    assertTrue(emp.isLoaded(Employee.MGR_FK));
    emp = emp.getForeignKey(Employee.MGR_FK);
    assertTrue(emp.isLoaded(Employee.MGR_FK));

    result = connection.select(condition.fetchDepth(Employee.MGR_FK, -1));
    assertEquals(1, result.size());
    emp = result.get(0);
    assertFalse(emp.isLoaded(Employee.DEPARTMENT_FK));
    assertTrue(emp.isLoaded(Employee.MGR_FK));
    emp = emp.getForeignKey(Employee.MGR_FK);
    assertTrue(emp.isLoaded(Employee.MGR_FK));

    assertEquals(4, connection.rowCount(where(Employee.ID).equalTo(asList(1, 2, 3, 4))));
    assertEquals(0, connection.rowCount(where(Employee.DEPARTMENT).isNull()));
    assertEquals(0, connection.rowCount(where(Employee.DEPARTMENT_FK).isNull()));
    assertEquals(1, connection.rowCount(where(Employee.MGR).isNull()));
    assertEquals(1, connection.rowCount(where(Employee.MGR_FK).isNull()));
  }

  @Test
  void selectFetchCount() throws DatabaseException {
    List<Entity> departments = connection.select(condition(Department.TYPE));
    assertEquals(4, departments.size());
    departments = connection.select(condition(Department.TYPE).toSelectCondition().fetchCount(0));
    assertTrue(departments.isEmpty());
    departments = connection.select(condition(Department.TYPE).toSelectCondition().fetchCount(2));
    assertEquals(2, departments.size());
    departments = connection.select(condition(Department.TYPE).toSelectCondition().fetchCount(3));
    assertEquals(3, departments.size());
    departments = connection.select(condition(Department.TYPE).toSelectCondition().fetchCount(-1));
    assertEquals(4, departments.size());
  }

  @Test
  void selectByKey() throws DatabaseException {
    final Key deptKey = ENTITIES.primaryKey(Department.TYPE, 10);
    final Key empKey = ENTITIES.primaryKey(Employee.TYPE, 8);

    final List<Entity> selected = connection.select(asList(deptKey, empKey));
    assertEquals(2, selected.size());
  }

  @Test
  void foreignKeyAttributes() throws DatabaseException {
    final List<Entity> emps = connection.select(where(EmployeeFk.MGR_FK).isNotNull()
            .toSelectCondition().fetchDepth(EmployeeFk.MGR_FK, 2));
    for (final Entity emp : emps) {
      final Entity mgr = emp.getForeignKey(EmployeeFk.MGR_FK);
      assertTrue(mgr.contains(EmployeeFk.ID));//pk automatically included
      assertTrue(mgr.contains(EmployeeFk.NAME));
      assertTrue(mgr.contains(EmployeeFk.JOB));
      assertTrue(mgr.contains(EmployeeFk.DEPARTMENT));
      assertTrue(mgr.contains(EmployeeFk.DEPARTMENT_FK));
      assertFalse(mgr.contains(EmployeeFk.MGR));
      assertFalse(mgr.contains(EmployeeFk.MGR_FK));
      assertFalse(mgr.contains(EmployeeFk.COMMISSION));
      assertFalse(mgr.contains(EmployeeFk.HIREDATE));
      assertFalse(mgr.contains(EmployeeFk.SALARY));
    }
  }

  @Test
  void selectAttributes() throws Exception {
    final List<Entity> emps = connection.select(condition(Employee.TYPE).toSelectCondition()
            .selectAttributes(Employee.ID, Employee.JOB, Employee.DEPARTMENT));
    for (final Entity emp : emps) {
      assertTrue(emp.contains(Employee.ID));
      assertTrue(emp.contains(Employee.JOB));
      assertTrue(emp.contains(Employee.DEPARTMENT));
      assertFalse(emp.contains(Employee.DEPARTMENT_FK));
      assertFalse(emp.contains(Employee.COMMISSION));
      assertFalse(emp.contains(Employee.HIREDATE));
      assertFalse(emp.contains(Employee.NAME));
      assertFalse(emp.contains(Employee.SALARY));
    }
    for (final Entity emp : connection.select(condition(Employee.TYPE).toSelectCondition()
            .selectAttributes(Employee.ID, Employee.JOB, Employee.DEPARTMENT_FK, Employee.MGR, Employee.COMMISSION))) {
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
  void selectInvalidColumn() throws Exception {
    assertThrows(DatabaseException.class, () -> connection.select(Conditions.customCondition(Department.DEPARTMENT_CONDITION_INVALID_COLUMN_TYPE)));
  }

  @Test
  void rowCount() throws Exception {
    int rowCount = connection.rowCount(condition(Department.TYPE));
    assertEquals(4, rowCount);
    Condition deptNoCondition = where(Department.DEPTNO).greaterThanOrEqualTo(30);
    rowCount = connection.rowCount(deptNoCondition);
    assertEquals(2, rowCount);

    rowCount = connection.rowCount(condition(JOINED_QUERY_ENTITY_TYPE));
    assertEquals(16, rowCount);
    deptNoCondition = where(JOINED_DEPTNO).greaterThanOrEqualTo(30);
    rowCount = connection.rowCount(deptNoCondition);
    assertEquals(4, rowCount);

    rowCount = connection.rowCount(condition(GROUP_BY_QUERY_ENTITY_TYPE));
    assertEquals(4, rowCount);
  }

  @Test
  void selectSingle() throws Exception {
    Entity sales = connection.selectSingle(Department.DNAME, "SALES");
    assertEquals(sales.get(Department.DNAME), "SALES");
    sales = connection.selectSingle(sales.getPrimaryKey());
    assertEquals(sales.get(Department.DNAME), "SALES");
    sales = connection.selectSingle(Conditions.customCondition(Department.DEPARTMENT_CONDITION_SALES_TYPE));
    assertEquals(sales.get(Department.DNAME), "SALES");

    Entity king = connection.selectSingle(Employee.NAME, "KING");
    assertTrue(king.contains(Employee.MGR_FK));
    assertNull(king.get(Employee.MGR_FK));

    king = connection.selectSingle(Employee.MGR_FK, null);
    assertNull(king.get(Employee.MGR_FK));
  }

  @Test
  void customCondition() throws DatabaseException {
    final Condition condition = Conditions.customCondition(Employee.MGR_GREATER_THAN_CONDITION_ID,
            singletonList(Employee.MGR), singletonList(5));

    assertEquals(4, connection.select(condition).size());
  }

  @Test
  void executeFunction() throws DatabaseException {
    connection.executeFunction(FUNCTION_ID);
  }

  @Test
  void executeProcedure() throws DatabaseException {
    connection.executeProcedure(PROCEDURE_ID);
  }

  @Test
  void selectSingleNotFound() throws Exception {
    assertThrows(RecordNotFoundException.class, () -> connection.selectSingle(Department.DNAME, "NO_NAME"));
  }

  @Test
  void selectSingleManyFound() throws Exception {
    assertThrows(MultipleRecordsFoundException.class, () -> connection.selectSingle(Employee.JOB, "MANAGER"));
  }

  @Test
  void insertOnlyNullValues() throws DatabaseException {
    connection.beginTransaction();
    try {
      final Entity department = ENTITIES.entity(Department.TYPE);
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
      final Entity department = connection.selectSingle(Department.DEPTNO, 10);
      assertThrows(DatabaseException.class, () -> connection.update(department));
    }
    finally {
      connection.rollbackTransaction();
    }
  }

  @Test
  void dateTime() throws DatabaseException {
    final Entity sales = connection.selectSingle(Department.DNAME, "SALES");
    final double salary = 1500;

    Entity emp = ENTITIES.builder(Employee.TYPE)
            .with(Employee.DEPARTMENT_FK, sales)
            .with(Employee.NAME, "Nobody")
            .with(Employee.SALARY, salary)
            .build();
    final LocalDate hiredate = LocalDate.parse("03-10-1975", DateTimeFormatter.ofPattern("dd-MM-yyyy"));
    emp.put(Employee.HIREDATE, hiredate);
    final OffsetDateTime hiretime = LocalDateTime.parse("03-10-1975 08:30:22", DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"))
            .atZone(TimeZone.getDefault().toZoneId()).toOffsetDateTime();
    emp.put(Employee.HIRETIME, hiretime);

    emp = connection.selectSingle(connection.insert(emp));

    assertEquals(hiredate, emp.get(Employee.HIREDATE));
    assertEquals(hiretime, emp.get(Employee.HIRETIME));

    connection.delete(emp.getPrimaryKey());
  }

  @Test
  void insertWithNullValues() throws DatabaseException {
    final Entity sales = connection.selectSingle(Department.DNAME, "SALES");
    final String name = "Nobody";
    final double salary = 1500;
    final double defaultCommission = 200;

    Entity emp = ENTITIES.builder(Employee.TYPE)
            .with(Employee.DEPARTMENT_FK, sales)
            .with(Employee.NAME, name)
            .with(Employee.SALARY, salary)
            .build();

    emp = connection.selectSingle(connection.insert(emp));
    assertEquals(sales, emp.get(Employee.DEPARTMENT_FK));
    assertEquals(name, emp.get(Employee.NAME));
    assertEquals(salary, emp.get(Employee.SALARY));
    assertEquals(defaultCommission, emp.get(Employee.COMMISSION));
    connection.delete(emp.getPrimaryKey());

    emp.put(Employee.COMMISSION, null);//default value should not kick in
    emp = connection.selectSingle(connection.insert(emp));
    assertEquals(sales, emp.get(Employee.DEPARTMENT_FK));
    assertEquals(name, emp.get(Employee.NAME));
    assertEquals(salary, emp.get(Employee.SALARY));
    assertNull(emp.get(Employee.COMMISSION));
    connection.delete(emp.getPrimaryKey());

    emp.remove(Employee.COMMISSION);//default value should kick in
    emp = connection.selectSingle(connection.insert(emp));
    assertEquals(sales, emp.get(Employee.DEPARTMENT_FK));
    assertEquals(name, emp.get(Employee.NAME));
    assertEquals(salary, emp.get(Employee.SALARY));
    assertEquals(defaultCommission, emp.get(Employee.COMMISSION));
    connection.delete(emp.getPrimaryKey());
  }

  @Test
  void insertEmptyList() throws DatabaseException {
    final List<Key> pks = connection.insert(new ArrayList<>());
    assertTrue(pks.isEmpty());
  }

  @Test
  void updateDifferentEntities() throws DatabaseException {
    connection.beginTransaction();
    try {
      final Entity sales = connection.selectSingle(Department.DNAME, "SALES");
      final Entity king = connection.selectSingle(Employee.NAME, "KING");
      final String newName = "New name";
      sales.put(Department.DNAME, newName);
      king.put(Employee.NAME, newName);
      final List<Entity> updated = connection.update(asList(sales, king));
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
    //otherwise the optimistic locking triggers an error
    connection.setOptimisticLockingEnabled(false);
    final Entity employee = connection.selectSingle(Employee.ID, 4);
    employee.put(Employee.ID, -888);//non existing
    employee.saveAll();
    employee.put(Employee.NAME, "New name");
    assertThrows(UpdateException.class, () -> connection.update(employee));
  }

  @Test
  void update() throws DatabaseException {
    final List<Entity> updated = connection.update(new ArrayList<>());
    assertTrue(updated.isEmpty());
  }

  @Test
  void updateWithConditionNoProperties() throws DatabaseException {
    final UpdateCondition condition = Conditions.condition(Employee.TYPE).toUpdateCondition();
    assertThrows(IllegalArgumentException.class, () -> connection.update(condition));
  }

  @Test
  void updateWithCondition() throws DatabaseException {
    final Condition condition = where(Employee.COMMISSION).isNull();

    final List<Entity> entities = connection.select(condition);

    final UpdateCondition updateCondition = where(Employee.COMMISSION).isNull().toUpdateCondition()
            .set(Employee.COMMISSION, 500d)
            .set(Employee.SALARY, 4200d);
    connection.beginTransaction();
    try {
      connection.update(updateCondition);
      assertEquals(0, connection.rowCount(condition));
      final List<Entity> afterUpdate = connection.select(Entity.getPrimaryKeys(entities));
      for (final Entity entity : afterUpdate) {
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
    final UpdateCondition updateCondition = where(Employee.ID).isNull().toUpdateCondition()
            .set(Employee.SALARY, 4200d);
    connection.beginTransaction();
    try {
      assertEquals(0, connection.update(updateCondition));
    }
    finally {
      connection.rollbackTransaction();
    }
  }

  @Test
  void selectValuesNonColumnProperty() throws Exception {
    assertThrows(IllegalArgumentException.class, () -> connection.select(Employee.DEPARTMENT_LOCATION));
  }

  @Test
  void selectValues() throws Exception {
    List<String> result = connection.select(Department.DNAME);
    assertEquals("ACCOUNTING", result.get(0));
    assertEquals("OPERATIONS", result.get(1));
    assertEquals("RESEARCH", result.get(2));
    assertEquals("SALES", result.get(3));

    result = connection.select(Department.DNAME, where(Department.DEPTNO).equalTo(10));
    assertTrue(result.contains("ACCOUNTING"));
    assertFalse(result.contains("SALES"));
  }

  @Test
  void selectValuesIncorrectAttribute() throws Exception {
    assertThrows(IllegalArgumentException.class, () -> connection.select(Department.DNAME,
            where(Employee.ID).equalTo(1)));
  }

  @Test
  void selectForUpdateModified() throws Exception {
    final DefaultLocalEntityConnection connection = initializeConnection();
    final DefaultLocalEntityConnection connection2 = initializeConnection();
    final String originalLocation;
    try {
      final SelectCondition condition = where(Department.DNAME).equalTo("SALES").toSelectCondition().forUpdate();

      Entity sales = connection.selectSingle(condition);
      originalLocation = sales.get(Department.LOC);

      sales.put(Department.LOC, "Syracuse");
      try {
        connection2.update(sales);
        fail("Should not be able to update record selected for update by another connection");
      }
      catch (final DatabaseException ignored) {
        connection2.getDatabaseConnection().rollback();
      }

      connection.select(condition(Department.TYPE));//any query will do

      try {
        sales = connection2.update(sales);
        sales.put(Department.LOC, originalLocation);
        connection2.update(sales);//revert changes to data
      }
      catch (final DatabaseException ignored) {
        fail("Should be able to update record after other connection released the select for update lock");
      }
    }
    finally {
      connection.close();
      connection2.close();
    }
  }

  @Test
  void optimisticLockingDeleted() throws Exception {
    final DefaultLocalEntityConnection connection = initializeConnection();
    final EntityConnection connection2 = initializeConnection();
    connection.setOptimisticLockingEnabled(true);
    final Entity allen;
    try {
      final Condition condition = where(Employee.NAME).equalTo("ALLEN");

      allen = connection.selectSingle(condition);

      connection2.delete(allen.getPrimaryKey());

      allen.put(Employee.JOB, "CLERK");
      try {
        connection.update(allen);
        fail("Should not be able to update record deleted by another connection");
      }
      catch (final RecordModifiedException e) {
        assertNotNull(e.getRow());
        assertNull(e.getModifiedRow());
      }

      try {
        connection2.insert(allen);//revert changes to data
      }
      catch (final DatabaseException ignored) {
        fail("Should be able to update record after other connection released the select for update lock");
      }
    }
    finally {
      connection.close();
      connection2.close();
    }
  }

  @Test
  void optimisticLockingModified() throws Exception {
    final DefaultLocalEntityConnection baseConnection = initializeConnection();
    final DefaultLocalEntityConnection optimisticConnection = initializeConnection();
    optimisticConnection.setOptimisticLockingEnabled(true);
    assertTrue(optimisticConnection.isOptimisticLockingEnabled());
    String oldLocation = null;
    Entity updatedDepartment = null;
    try {
      final Entity department = baseConnection.selectSingle(Department.DNAME, "SALES");
      oldLocation = department.put(Department.LOC, "NEWLOC");
      updatedDepartment = baseConnection.update(department);
      try {
        optimisticConnection.update(department);
        fail("RecordModifiedException should have been thrown");
      }
      catch (final RecordModifiedException e) {
        assertTrue(((Entity) e.getModifiedRow()).columnValuesEqual(updatedDepartment));
        assertTrue(((Entity) e.getRow()).columnValuesEqual(department));
      }
    }
    finally {
      try {
        if (updatedDepartment != null && oldLocation != null) {
          updatedDepartment.put(Department.LOC, oldLocation);
          baseConnection.update(updatedDepartment);
        }
      }
      catch (final DatabaseException e) {
        e.printStackTrace();
      }
      baseConnection.close();
      optimisticConnection.close();
    }
  }

  @Test
  void optimisticLockingBlob() throws Exception {
    final DefaultLocalEntityConnection baseConnection = initializeConnection();
    final DefaultLocalEntityConnection optimisticConnection = initializeConnection();
    optimisticConnection.setOptimisticLockingEnabled(true);
    Entity updatedEmployee = null;
    try {
      final Random random = new Random();
      final byte[] bytes = new byte[1024];
      random.nextBytes(bytes);

      final Entity employee = baseConnection.selectSingle(Employee.NAME, "BLAKE");
      employee.put(Employee.DATA, bytes);
      updatedEmployee = baseConnection.update(employee);

      random.nextBytes(bytes);
      employee.put(Employee.DATA, bytes);

      try {
        optimisticConnection.update(employee);
        fail("RecordModifiedException should have been thrown");
      }
      catch (final RecordModifiedException e) {
        assertTrue(((Entity) e.getModifiedRow()).columnValuesEqual(updatedEmployee));
        assertTrue(((Entity) e.getRow()).columnValuesEqual(employee));
      }
    }
    finally {
      baseConnection.close();
      optimisticConnection.close();
    }
  }

  @Test
  void dualIterator() throws Exception {
    final DefaultLocalEntityConnection connection = initializeConnection();
    final ResultIterator<Entity> deptIterator =
            connection.iterator(condition(Department.TYPE));
    while (deptIterator.hasNext()) {
      final ResultIterator<Entity> empIterator =
              connection.iterator(where(Employee.DEPARTMENT_FK).equalTo(deptIterator.next()));
      while (empIterator.hasNext()) {
        empIterator.next();
      }
    }
  }

  @Test
  void testConstructor() throws Exception {
    Connection connection = null;
    try {
      final Database db = DatabaseFactory.getDatabase();
      connection = db.createConnection(UNIT_TEST_USER);
      final EntityConnection conn = new DefaultLocalEntityConnection(DOMAIN, db, connection);
      assertTrue(conn.isConnected());
    }
    finally {
      if (connection != null) {
        try {
          connection.close();
        }
        catch (final Exception ignored) {/*ignored*/}
      }
    }
  }

  @Test
  void testConstructorInvalidConnection() throws Exception {
    assertThrows(DatabaseException.class, () -> {
      Connection connection = null;
      try {
        final Database db = DatabaseFactory.getDatabase();
        connection = db.createConnection(UNIT_TEST_USER);
        connection.close();
        new DefaultLocalEntityConnection(DOMAIN, db, connection);
      }
      finally {
        if (connection != null) {
          try {
            connection.close();
          }
          catch (final Exception ignored) {/*ignored*/}
        }
      }
    });
  }

  @Test
  void readWriteBlob() throws DatabaseException {
    final byte[] lazyBytes = new byte[1024];
    final byte[] bytes = new byte[1024];
    final Random random = new Random();
    random.nextBytes(lazyBytes);
    random.nextBytes(bytes);

    final Entity scott = connection.selectSingle(Employee.ID, 7);
    connection.writeBlob(scott.getPrimaryKey(), Employee.DATA_LAZY, lazyBytes);
    connection.writeBlob(scott.getPrimaryKey(), Employee.DATA, bytes);
    assertArrayEquals(lazyBytes, connection.readBlob(scott.getPrimaryKey(), Employee.DATA_LAZY));
    assertArrayEquals(bytes, connection.readBlob(scott.getPrimaryKey(), Employee.DATA));

    Entity scottFromDb = connection.selectSingle(scott.getPrimaryKey());
    //lazy loaded
    assertNull(scottFromDb.get(Employee.DATA_LAZY));
    assertNotNull(scottFromDb.get(Employee.DATA));

    //overrides lazy loading
    scottFromDb = connection.selectSingle(condition(scott.getPrimaryKey()).toSelectCondition().selectAttributes(Employee.DATA_LAZY));
    assertNotNull(scottFromDb.get(Employee.DATA_LAZY));
  }

  @Test
  void readWriteBlobViaEntity() throws DatabaseException {
    final byte[] lazyBytes = new byte[1024];
    final byte[] bytes = new byte[1024];
    final Random random = new Random();
    random.nextBytes(lazyBytes);
    random.nextBytes(bytes);

    final Entity scott = connection.selectSingle(Employee.ID, 7);
    scott.put(Employee.DATA_LAZY, lazyBytes);
    scott.put(Employee.DATA, bytes);
    connection.update(scott);
    scott.saveAll();

    byte[] lazyFromDb = connection.readBlob(scott.getPrimaryKey(), Employee.DATA_LAZY);
    final byte[] fromDb = connection.readBlob(scott.getPrimaryKey(), Employee.DATA);
    assertArrayEquals(lazyBytes, lazyFromDb);
    assertArrayEquals(bytes, fromDb);

    Entity scottFromDb = connection.selectSingle(scott.getPrimaryKey());
    //lazy loaded
    assertNull(scottFromDb.get(Employee.DATA_LAZY));
    assertNotNull(scottFromDb.get(Employee.DATA));
    assertArrayEquals(bytes, scottFromDb.get(Employee.DATA));

    final byte[] newLazyBytes = new byte[2048];
    final byte[] newBytes = new byte[2048];
    random.nextBytes(newLazyBytes);
    random.nextBytes(newBytes);

    scott.put(Employee.DATA_LAZY, newLazyBytes);
    scott.put(Employee.DATA, newBytes);

    connection.update(scott);

    lazyFromDb = connection.readBlob(scott.getPrimaryKey(), Employee.DATA_LAZY);
    assertArrayEquals(newLazyBytes, lazyFromDb);

    scottFromDb = connection.selectSingle(scott.getPrimaryKey());
    assertArrayEquals(newBytes, scottFromDb.get(Employee.DATA));
  }

  @Test
  void testUUIDPrimaryKeyColumnWithDefaultValue() throws DatabaseException {
    final Entity entity = ENTITIES.builder(UUIDTestDefault.TYPE)
            .with(UUIDTestDefault.DATA, "test")
            .build();
    connection.insert(entity);
    assertNotNull(entity.get(UUIDTestDefault.ID));
    assertEquals("test", entity.get(UUIDTestDefault.DATA));
  }

  @Test
  void testUUIDPrimaryKeyColumnWithoutDefaultValue() throws DatabaseException {
    final Entity entity = ENTITIES.builder(UUIDTestNoDefault.TYPE)
            .with(UUIDTestNoDefault.DATA, "test")
            .build();
    connection.insert(entity);
    assertNotNull(entity.get(UUIDTestNoDefault.ID));
    assertEquals("test", entity.get(UUIDTestNoDefault.DATA));
  }

  @Test
  void entityWithoutPrimaryKey() throws DatabaseException {
    List<Entity> entities = connection.select(condition(T_NO_PK));
    assertEquals(6, entities.size());
    entities = connection.select(where(NO_PK_COL1).equalTo(2)
                    .or(where(NO_PK_COL3).equalTo("5")));
    assertEquals(4, entities.size());
  }

  @Test
  void beans() throws DatabaseException {
    connection.beginTransaction();
    try {
      final List<Entity> departments = connection.select(condition(Department.TYPE));
      Department department = departments.get(0).castTo(Department.class);
      department.setName("New Name");

      department = connection.update(department).castTo(Department.class);

      assertEquals("New Name", department.getName());

      List<Department> departmentsCast = Entity.castTo(Department.class, connection.select(condition(Department.TYPE)));

      departmentsCast.forEach(dept -> dept.setName(dept.getName() + "N"));

      departmentsCast = Entity.castTo(Department.class, connection.update(departmentsCast));

      final Department newDept1 = ENTITIES.entity(Department.TYPE).castTo(Department.class);
      newDept1.setId(-1);
      newDept1.setName("hello1");
      newDept1.setLocation("location");

      final Department newDept2 = ENTITIES.entity(Department.TYPE).castTo(Department.class);
      newDept2.setId(-2);
      newDept2.setName("hello2");
      newDept2.setLocation("location");

      final List<Key> keys = connection.insert(asList(newDept1, newDept2));
      assertEquals(Integer.valueOf(-1), keys.get(0).get());
      assertEquals(Integer.valueOf(-2), keys.get(1).get());
    }
    finally {
      connection.rollbackTransaction();
    }
  }

  private static DefaultLocalEntityConnection initializeConnection() throws DatabaseException {
    return new DefaultLocalEntityConnection(DOMAIN, DatabaseFactory.getDatabase(), UNIT_TEST_USER);
  }
}