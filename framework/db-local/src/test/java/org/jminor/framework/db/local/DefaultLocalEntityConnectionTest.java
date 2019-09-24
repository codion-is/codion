/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.local;

import org.jminor.common.DateFormats;
import org.jminor.common.User;
import org.jminor.common.db.AbstractFunction;
import org.jminor.common.db.AbstractProcedure;
import org.jminor.common.db.Database;
import org.jminor.common.db.DatabaseConnection;
import org.jminor.common.db.Databases;
import org.jminor.common.db.ResultIterator;
import org.jminor.common.db.condition.Condition;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.db.exception.RecordModifiedException;
import org.jminor.common.db.exception.RecordNotFoundException;
import org.jminor.common.db.exception.ReferentialIntegrityException;
import org.jminor.common.db.exception.UniqueConstraintException;
import org.jminor.common.db.exception.UpdateException;
import org.jminor.common.db.reports.ReportDataWrapper;
import org.jminor.common.db.reports.ReportException;
import org.jminor.common.db.reports.ReportResult;
import org.jminor.common.db.reports.ReportWrapper;
import org.jminor.framework.db.EntityConnection;
import org.jminor.framework.db.condition.EntityConditions;
import org.jminor.framework.db.condition.EntitySelectCondition;
import org.jminor.framework.domain.Domain;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.Properties;
import org.jminor.framework.domain.Property;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

public class DefaultLocalEntityConnectionTest {

  private static final User UNIT_TEST_USER = new User(
          System.getProperty("jminor.unittest.username", "scott"),
          System.getProperty("jminor.unittest.password", "tiger").toCharArray());

  private static final String JOINED_QUERY_ENTITY_ID = "joinedQueryEntityID";
  private static final String GROUP_BY_QUERY_ENTITY_ID = "groupByQueryEntityID";

  private DefaultLocalEntityConnection connection;

  private static final TestDomain DOMAIN = new TestDomain();
  private static final EntityConditions ENTITY_CONDITIONS = new EntityConditions(DOMAIN);

  @BeforeAll
  public static void beforeClass() {
    DOMAIN.define(JOINED_QUERY_ENTITY_ID,
            Properties.primaryKeyProperty("e.empno"),
            Properties.columnProperty("d.deptno", Types.INTEGER))
            .setSelectQuery("select e.empno, d.deptno from scott.emp e, scott.dept d where e.deptno = d.deptno", true);

    DOMAIN.define(GROUP_BY_QUERY_ENTITY_ID,
            Properties.columnProperty("job", Types.VARCHAR)
                    .setPrimaryKeyIndex(0)
                    .setGroupingColumn(true))
            .setTableName("scott.emp")
            .setHavingClause("job <> 'PRESIDENT'");
  }

  @BeforeEach
  public void setup() throws ClassNotFoundException, DatabaseException {
    connection = initializeConnection();
  }

  @AfterEach
  public void tearDown() {
    connection.disconnect();
  }

  @Test
  public void delete() throws Exception {
    try {
      connection.beginTransaction();
      final Entity.Key key = DOMAIN.key(TestDomain.T_DEPARTMENT);
      key.put(TestDomain.DEPARTMENT_ID, 40);
      connection.delete(new ArrayList<>());
      connection.delete(Collections.singletonList(key));
      try {
        connection.selectSingle(key);
        fail();
      }
      catch (final DatabaseException ignored) {/*ignored*/}
    }
    finally {
      connection.rollbackTransaction();
    }
    try {
      connection.beginTransaction();
      final Entity.Key key = DOMAIN.key(TestDomain.T_DEPARTMENT);
      key.put(TestDomain.DEPARTMENT_ID, 40);
      connection.delete(ENTITY_CONDITIONS.condition(key));
      try {
        connection.selectSingle(key);
        fail();
      }
      catch (final DatabaseException ignored) {/*ignored*/}
    }
    finally {
      connection.rollbackTransaction();
    }
  }

  @Test
  public void deleteReferentialIntegrity() {
    final Entity.Key key = DOMAIN.key(TestDomain.T_DEPARTMENT);
    key.put(TestDomain.DEPARTMENT_ID, 10);
    assertThrows(ReferentialIntegrityException.class, () -> connection.delete(Collections.singletonList(key)));
  }

  @Test
  public void insertUniqueConstraint() {
    final Entity department = DOMAIN.entity(TestDomain.T_DEPARTMENT);
    department.put(TestDomain.DEPARTMENT_ID, 1000);
    department.put(TestDomain.DEPARTMENT_NAME, "SALES");
    assertThrows(UniqueConstraintException.class, () -> connection.insert(Collections.singletonList(department)));
  }

  @Test
  public void updateUniqueConstraint() throws DatabaseException {
    final Entity department = connection.selectSingle(TestDomain.T_DEPARTMENT, TestDomain.DEPARTMENT_ID, 20);
    department.put(TestDomain.DEPARTMENT_NAME, "SALES");
    assertThrows(UniqueConstraintException.class, () -> connection.update(Collections.singletonList(department)));
  }

  @Test
  public void insertNoParentKey() {
    final Entity emp = DOMAIN.entity(TestDomain.T_EMP);
    emp.put(TestDomain.EMP_ID, -100);
    emp.put(TestDomain.EMP_NAME, "Testing");
    emp.put(TestDomain.EMP_DEPARTMENT, -1010);//not available
    emp.put(TestDomain.EMP_SALARY, 2000d);
    assertThrows(ReferentialIntegrityException.class, () -> connection.insert(Collections.singletonList(emp)));
  }

  @Test
  public void updateNoParentKey() throws DatabaseException {
    final Entity emp = connection.selectSingle(TestDomain.T_EMP, TestDomain.EMP_ID, 3);
    emp.put(TestDomain.EMP_DEPARTMENT, -1010);//not available
    assertThrows(ReferentialIntegrityException.class, () -> connection.update(Collections.singletonList(emp)));
  }

  @Test
  public void deleteByKeyWithForeignKeys() throws DatabaseException {
    final Entity accounting = connection.selectSingle(TestDomain.T_DEPARTMENT, TestDomain.DEPARTMENT_NAME, "ACCOUNTING");
    assertThrows(DatabaseException.class, () -> connection.delete(Collections.singletonList(accounting.getKey())));
  }

  @Test
  public void deleteByConditionWithForeignKeys() throws DatabaseException {
    assertThrows(DatabaseException.class, () -> connection.delete(ENTITY_CONDITIONS.condition(TestDomain.T_DEPARTMENT, TestDomain.DEPARTMENT_NAME, Condition.Type.LIKE, "ACCOUNTING")));
  }

  @Test
  public void fillReport() throws Exception {
    final Map<String, Object> reportParameters = new HashMap<>();
    reportParameters.put("DEPTNO", Arrays.asList(10, 20));
    final ReportResult reportResult = () -> "result";
    connection.fillReport(new ReportWrapper() {
      @Override
      public String getReportName() {
        return "TestName";
      }

      @Override
      public ReportResult fillReport(final Connection connection) throws ReportException {
        return reportResult;
      }

      @Override
      public ReportResult fillReport(final ReportDataWrapper dataWrapper) throws ReportException {
        return reportResult;
      }
    });
  }

  @Test
  public void selectDependentEntities() throws Exception {
    final Map<String, Collection<Entity>> empty = connection.selectDependentEntities(new ArrayList<>());
    assertTrue(empty.isEmpty());
    final List<Entity> accounting = connection.selectMany(TestDomain.T_DEPARTMENT, TestDomain.DEPARTMENT_NAME, "ACCOUNTING");
    final Map<String, Collection<Entity>> emps = connection.selectDependentEntities(accounting);
    assertEquals(1, emps.size());
    assertTrue(emps.containsKey(TestDomain.T_EMP));
    assertEquals(7, emps.get(TestDomain.T_EMP).size());

    final Entity emp = connection.selectSingle(TestDomain.T_EMP, TestDomain.EMP_NAME, "KING");
    final Map<String, Collection<Entity>> deps = connection.selectDependentEntities(Collections.singletonList(emp));
    assertTrue(deps.isEmpty());//soft foreign key reference
  }

  @Test
  public void selectManyLimitOffset() throws Exception {
    final EntitySelectCondition condition = ENTITY_CONDITIONS.selectCondition(TestDomain.T_EMP)
            .setOrderBy(Domain.orderBy().ascending(TestDomain.EMP_NAME)).setLimit(2);
    List<Entity> result = connection.selectMany(condition);
    assertEquals(2, result.size());
    condition.setLimit(3);
    condition.setOffset(3);
    result = connection.selectMany(condition);
    assertEquals(3, result.size());
    assertEquals("BLAKE", result.get(0).get(TestDomain.EMP_NAME));
    assertEquals("CLARK", result.get(1).get(TestDomain.EMP_NAME));
    assertEquals("FORD", result.get(2).get(TestDomain.EMP_NAME));
  }

  @Test
  public void selectManyWhereNull() throws Exception {
    connection.selectMany(TestDomain.T_EMP, TestDomain.EMP_MGR_FK, (Object[]) null);
    connection.selectMany(TestDomain.T_EMP, TestDomain.EMP_DATA, (Object) null);
  }

  @Test
  public void selectMany() throws Exception {
    List<Entity> result = connection.selectMany(new ArrayList<>());
    assertTrue(result.isEmpty());
    result = connection.selectMany(TestDomain.T_DEPARTMENT, TestDomain.DEPARTMENT_ID, 10, 20);
    assertEquals(2, result.size());
    result = connection.selectMany(Entities.getKeys(result));
    assertEquals(2, result.size());
    result = connection.selectMany(ENTITY_CONDITIONS.selectCondition(TestDomain.T_DEPARTMENT, ENTITY_CONDITIONS.stringCondition("deptno in (10, 20)")));
    assertEquals(2, result.size());
    result = connection.selectMany(ENTITY_CONDITIONS.selectCondition(JOINED_QUERY_ENTITY_ID, ENTITY_CONDITIONS.stringCondition("d.deptno = 10")));
    assertEquals(7, result.size());

    final EntitySelectCondition condition = ENTITY_CONDITIONS.selectCondition(TestDomain.T_EMP, ENTITY_CONDITIONS.stringCondition("ename = 'BLAKE'"));
    result = connection.selectMany(condition);
    Entity emp = result.get(0);
    assertTrue(emp.isLoaded(TestDomain.EMP_DEPARTMENT_FK));
    assertTrue(emp.isLoaded(TestDomain.EMP_MGR_FK));
    emp = emp.getForeignKey(TestDomain.EMP_MGR_FK);
    assertFalse(emp.isLoaded(TestDomain.EMP_MGR_FK));

    result = connection.selectMany(condition.setForeignKeyFetchDepthLimit(TestDomain.EMP_DEPARTMENT_FK, 0));
    assertEquals(1, result.size());
    emp = result.get(0);
    assertFalse(emp.isLoaded(TestDomain.EMP_DEPARTMENT_FK));
    assertTrue(emp.isLoaded(TestDomain.EMP_MGR_FK));

    result = connection.selectMany(condition.setForeignKeyFetchDepthLimit(TestDomain.EMP_MGR_FK, 0));
    assertEquals(1, result.size());
    emp = result.get(0);
    assertFalse(emp.isLoaded(TestDomain.EMP_DEPARTMENT_FK));
    assertFalse(emp.isLoaded(TestDomain.EMP_MGR_FK));

    result = connection.selectMany(condition.setForeignKeyFetchDepthLimit(TestDomain.EMP_MGR_FK, 2));
    assertEquals(1, result.size());
    emp = result.get(0);
    assertFalse(emp.isLoaded(TestDomain.EMP_DEPARTMENT_FK));
    assertTrue(emp.isLoaded(TestDomain.EMP_MGR_FK));
    emp = emp.getForeignKey(TestDomain.EMP_MGR_FK);
    assertTrue(emp.isLoaded(TestDomain.EMP_MGR_FK));

    result = connection.selectMany(condition.setForeignKeyFetchDepthLimit(TestDomain.EMP_MGR_FK, -1));
    assertEquals(1, result.size());
    emp = result.get(0);
    assertFalse(emp.isLoaded(TestDomain.EMP_DEPARTMENT_FK));
    assertTrue(emp.isLoaded(TestDomain.EMP_MGR_FK));
    emp = emp.getForeignKey(TestDomain.EMP_MGR_FK);
    assertTrue(emp.isLoaded(TestDomain.EMP_MGR_FK));
  }

  @Test
  public void selectManyByKey() throws DatabaseException {
    final Entity.Key deptKey = DOMAIN.key(TestDomain.T_DEPARTMENT);
    deptKey.put(TestDomain.DEPARTMENT_ID, 10);
    final Entity.Key empKey = DOMAIN.key(TestDomain.T_EMP);
    empKey.put(TestDomain.EMP_ID, 8);

    final List<Entity> selected = connection.selectMany(Arrays.asList(deptKey, empKey));
    assertEquals(2, selected.size());
  }

  @Test
  public void selectManyInvalidColumn() throws Exception {
    assertThrows(DatabaseException.class, () -> connection.selectMany(ENTITY_CONDITIONS.selectCondition(TestDomain.T_DEPARTMENT,
            ENTITY_CONDITIONS.stringCondition("no_column is null"))));
  }

  @Test
  public void selectRowCount() throws Exception {
    int rowCount = connection.selectRowCount(ENTITY_CONDITIONS.condition(TestDomain.T_DEPARTMENT));
    assertEquals(4, rowCount);
    Condition<Property.ColumnProperty> deptNoCondition = ENTITY_CONDITIONS.propertyCondition(TestDomain.T_DEPARTMENT,
            TestDomain.DEPARTMENT_ID, Condition.Type.GREATER_THAN, 30);
    rowCount = connection.selectRowCount(ENTITY_CONDITIONS.condition(TestDomain.T_DEPARTMENT, deptNoCondition));
    assertEquals(2, rowCount);

    rowCount = connection.selectRowCount(ENTITY_CONDITIONS.condition(JOINED_QUERY_ENTITY_ID));
    assertEquals(16, rowCount);
    deptNoCondition = ENTITY_CONDITIONS.propertyCondition(JOINED_QUERY_ENTITY_ID, "d.deptno", Condition.Type.GREATER_THAN, 30);
    rowCount = connection.selectRowCount(ENTITY_CONDITIONS.condition(JOINED_QUERY_ENTITY_ID, deptNoCondition));
    assertEquals(4, rowCount);

    rowCount = connection.selectRowCount(ENTITY_CONDITIONS.condition(GROUP_BY_QUERY_ENTITY_ID));
    assertEquals(4, rowCount);
  }

  @Test
  public void selectSingle() throws Exception {
    Entity sales = connection.selectSingle(TestDomain.T_DEPARTMENT, TestDomain.DEPARTMENT_NAME, "SALES");
    assertEquals(sales.getString(TestDomain.DEPARTMENT_NAME), "SALES");
    sales = connection.selectSingle(sales.getKey());
    assertEquals(sales.getString(TestDomain.DEPARTMENT_NAME), "SALES");
    sales = connection.selectSingle(ENTITY_CONDITIONS.selectCondition(TestDomain.T_DEPARTMENT, ENTITY_CONDITIONS.stringCondition("dname = 'SALES'")));
    assertEquals(sales.getString(TestDomain.DEPARTMENT_NAME), "SALES");

    final Entity king = connection.selectSingle(TestDomain.T_EMP, TestDomain.EMP_NAME, "KING");
    assertTrue(king.containsKey(TestDomain.EMP_MGR_FK));
    assertNull(king.get(TestDomain.EMP_MGR_FK));
  }

  @Test
  public void executeFunction() throws DatabaseException {
    final DatabaseConnection.Function func = new AbstractFunction<EntityConnection>("executeFunction", "executeFunction") {
      @Override
      public List execute(final EntityConnection connection, final Object... arguments) {
        return null;
      }
    };
    DOMAIN.addOperation(func);
    connection.executeFunction(func.getId());
  }

  @Test
  public void executeProcedure() throws DatabaseException {
    final DatabaseConnection.Procedure proc = new AbstractProcedure<EntityConnection>("executeProcedure", "executeProcedure") {
      @Override
      public void execute(final EntityConnection connection, final Object... arguments) {}
    };
    DOMAIN.addOperation(proc);
    connection.executeProcedure(proc.getId());
  }

  @Test
  public void selectSingleNotFound() throws Exception {
    assertThrows(RecordNotFoundException.class, () -> connection.selectSingle(TestDomain.T_DEPARTMENT, TestDomain.DEPARTMENT_NAME, "NO_NAME"));
  }

  @Test
  public void selectSingleManyFound() throws Exception {
    assertThrows(DatabaseException.class, () -> connection.selectSingle(TestDomain.T_EMP, TestDomain.EMP_JOB, "MANAGER"));
  }

  @Test
  public void insertOnlyNullValues() throws DatabaseException {
    try {
      connection.beginTransaction();
      final Entity department = DOMAIN.entity(TestDomain.T_DEPARTMENT);
      assertThrows(DatabaseException.class, () -> connection.insert(Collections.singletonList(department)));
    }
    finally {
      connection.rollbackTransaction();
    }
  }

  @Test
  public void updateNoModifiedValues() throws DatabaseException {
    try {
      connection.beginTransaction();
      final Entity department = connection.selectSingle(TestDomain.T_DEPARTMENT, TestDomain.DEPARTMENT_ID, 10);
      assertThrows(DatabaseException.class, () -> connection.update(Collections.singletonList(department)));
    }
    finally {
      connection.rollbackTransaction();
    }
  }

  @Test
  public void dateTime() throws DatabaseException {
    final Entity sales = connection.selectSingle(TestDomain.T_DEPARTMENT, TestDomain.DEPARTMENT_NAME, "SALES");
    final double salary = 1500;

    Entity emp = DOMAIN.entity(TestDomain.T_EMP);
    emp.put(TestDomain.EMP_DEPARTMENT_FK, sales);
    emp.put(TestDomain.EMP_NAME, "Nobody");
    emp.put(TestDomain.EMP_SALARY, salary);
    final LocalDate hiredate = LocalDate.parse("03-10-1975", DateTimeFormatter.ofPattern(DateFormats.SHORT_DASH));
    emp.put(TestDomain.EMP_HIREDATE, hiredate);
    final LocalDateTime hiretime = LocalDateTime.parse("03-10-1975 08:30:22", DateTimeFormatter.ofPattern(DateFormats.FULL_TIMESTAMP));
    emp.put(TestDomain.EMP_HIRETIME, hiretime);

    emp = connection.selectSingle(connection.insert(Collections.singletonList(emp)).get(0));

    assertEquals(hiredate, emp.getDate(TestDomain.EMP_HIREDATE));
    assertEquals(hiretime, emp.getTimestamp(TestDomain.EMP_HIRETIME));
  }

  @Test
  public void insertWithNullValues() throws DatabaseException {
    final Entity sales = connection.selectSingle(TestDomain.T_DEPARTMENT, TestDomain.DEPARTMENT_NAME, "SALES");
    final String name = "Nobody";
    final double salary = 1500;
    final double defaultCommission = 200;

    Entity emp = DOMAIN.entity(TestDomain.T_EMP);
    emp.put(TestDomain.EMP_DEPARTMENT_FK, sales);
    emp.put(TestDomain.EMP_NAME, name);
    emp.put(TestDomain.EMP_SALARY, salary);

    emp = connection.selectSingle(connection.insert(Collections.singletonList(emp)).get(0));
    assertEquals(sales, emp.get(TestDomain.EMP_DEPARTMENT_FK));
    assertEquals(name, emp.get(TestDomain.EMP_NAME));
    assertEquals(salary, emp.get(TestDomain.EMP_SALARY));
    assertEquals(defaultCommission, emp.get(TestDomain.EMP_COMMISSION));
    connection.delete(Collections.singletonList(emp.getKey()));

    emp.put(TestDomain.EMP_COMMISSION, null);//default value should not kick in
    emp = connection.selectSingle(connection.insert(Collections.singletonList(emp)).get(0));
    assertEquals(sales, emp.get(TestDomain.EMP_DEPARTMENT_FK));
    assertEquals(name, emp.get(TestDomain.EMP_NAME));
    assertEquals(salary, emp.get(TestDomain.EMP_SALARY));
    assertNull(emp.get(TestDomain.EMP_COMMISSION));
    connection.delete(Collections.singletonList(emp.getKey()));

    emp.remove(TestDomain.EMP_COMMISSION);//default value should kick in
    emp = connection.selectSingle(connection.insert(Collections.singletonList(emp)).get(0));
    assertEquals(sales, emp.get(TestDomain.EMP_DEPARTMENT_FK));
    assertEquals(name, emp.get(TestDomain.EMP_NAME));
    assertEquals(salary, emp.get(TestDomain.EMP_SALARY));
    assertEquals(defaultCommission, emp.get(TestDomain.EMP_COMMISSION));
    connection.delete(Collections.singletonList(emp.getKey()));
  }

  @Test
  public void insertEmptyList() throws DatabaseException {
    final List<Entity.Key> pks = connection.insert(new ArrayList<>());
    assertTrue(pks.isEmpty());
  }

  @Test
  public void updateDifferentEntities() throws DatabaseException {
    try {
      connection.beginTransaction();
      final Entity sales = connection.selectSingle(TestDomain.T_DEPARTMENT, TestDomain.DEPARTMENT_NAME, "SALES");
      final Entity king = connection.selectSingle(TestDomain.T_EMP, TestDomain.EMP_NAME, "KING");
      final String newName = "New name";
      sales.put(TestDomain.DEPARTMENT_NAME, newName);
      king.put(TestDomain.EMP_NAME, newName);
      final List<Entity> updated = connection.update(Arrays.asList(sales, king));
      assertTrue(updated.containsAll(Arrays.asList(sales, king)));
      assertEquals(newName, updated.get(updated.indexOf(sales)).getString(TestDomain.DEPARTMENT_NAME));
      assertEquals(newName, updated.get(updated.indexOf(king)).getString(TestDomain.EMP_NAME));
    }
    finally {
      connection.rollbackTransaction();
    }
  }

  @Test
  public void updateNonExisting() throws DatabaseException {
    //otherwise the optimistic locking triggers an error
    connection.setOptimisticLocking(false);
    final Entity employee = connection.selectSingle(TestDomain.T_EMP, TestDomain.EMP_ID, 4);
    employee.put(TestDomain.EMP_ID, -888);//non existing
    employee.saveAll();
    employee.put(TestDomain.EMP_NAME, "New name");
    assertThrows(UpdateException.class, () -> connection.update(Collections.singletonList(employee)));
  }

  @Test
  public void update() throws DatabaseException {
    final List<Entity> updated = connection.update(new ArrayList<>());
    assertTrue(updated.isEmpty());
  }

  @Test
  public void selectValuesNonColumnProperty() throws Exception {
    assertThrows(IllegalArgumentException.class, () -> connection.selectValues(TestDomain.EMP_DEPARTMENT_LOCATION, ENTITY_CONDITIONS.condition(TestDomain.T_EMP)));
  }

  @Test
  public void selectValues() throws Exception {
    List<Object> result = connection.selectValues(TestDomain.DEPARTMENT_NAME, ENTITY_CONDITIONS.condition(TestDomain.T_DEPARTMENT));
    assertEquals("ACCOUNTING", result.get(0));
    assertEquals("OPERATIONS", result.get(1));
    assertEquals("RESEARCH", result.get(2));
    assertEquals("SALES", result.get(3));

    result = connection.selectValues(TestDomain.DEPARTMENT_NAME, ENTITY_CONDITIONS.condition(TestDomain.T_DEPARTMENT,
            TestDomain.DEPARTMENT_ID, Condition.Type.LIKE, 10));
    assertTrue(result.contains("ACCOUNTING"));
    assertFalse(result.contains("SALES"));
  }

  @Test
  public void selectForUpdateModified() throws Exception {
    final DefaultLocalEntityConnection connection = initializeConnection();
    final DefaultLocalEntityConnection connection2 = initializeConnection();
    final String originalLocation;
    try {
      final EntitySelectCondition condition = ENTITY_CONDITIONS.selectCondition(TestDomain.T_DEPARTMENT, TestDomain.DEPARTMENT_NAME, Condition.Type.LIKE, "SALES");
      condition.setForUpdate(true);

      Entity sales = connection.selectSingle(condition);
      originalLocation = sales.getString(TestDomain.DEPARTMENT_LOCATION);

      sales.put(TestDomain.DEPARTMENT_LOCATION, "Syracuse");
      try {
        connection2.update(Collections.singletonList(sales));
        fail("Should not be able to update record selected for update by another connection");
      }
      catch (final DatabaseException ignored) {
        connection2.getDatabaseConnection().rollback();
      }

      connection.selectMany(ENTITY_CONDITIONS.selectCondition(TestDomain.T_DEPARTMENT));//any query will do

      try {
        sales = connection2.update(Collections.singletonList(sales)).get(0);
        sales.put(TestDomain.DEPARTMENT_LOCATION, originalLocation);
        connection2.update(Collections.singletonList(sales));//revert changes to data
      }
      catch (final DatabaseException ignored) {
        fail("Should be able to update record after other connection released the select for update lock");
      }
    }
    finally {
      connection.disconnect();
      connection2.disconnect();
    }
  }

  @Test
  public void optimisticLockingDeleted() throws Exception {
    final DefaultLocalEntityConnection connection = initializeConnection();
    final EntityConnection connection2 = initializeConnection();
    connection.setOptimisticLocking(true);
    final Entity allen;
    try {
      final EntitySelectCondition condition = ENTITY_CONDITIONS.selectCondition(TestDomain.T_EMP, TestDomain.EMP_NAME, Condition.Type.LIKE, "ALLEN");

      allen = connection.selectSingle(condition);

      connection2.delete(Collections.singletonList(allen.getKey()));

      allen.put(TestDomain.EMP_JOB, "CLERK");
      try {
        connection.update(Collections.singletonList(allen));
        fail("Should not be able to update record deleted by another connection");
      }
      catch (final RecordModifiedException e) {
        assertNotNull(e.getRow());
        assertNull(e.getModifiedRow());
      }

      try {
        connection2.insert(Collections.singletonList(allen));//revert changes to data
      }
      catch (final DatabaseException ignored) {
        fail("Should be able to update record after other connection released the select for update lock");
      }
    }
    finally {
      connection.disconnect();
      connection2.disconnect();
    }
  }

  @Test
  public void optimisticLockingModified() throws Exception {
    final DefaultLocalEntityConnection baseConnection = initializeConnection();
    final DefaultLocalEntityConnection optimisticConnection = initializeConnection();
    optimisticConnection.setOptimisticLocking(true);
    assertTrue(optimisticConnection.isOptimisticLocking());
    String oldLocation = null;
    Entity updatedDepartment = null;
    try {
      final Entity department = baseConnection.selectSingle(TestDomain.T_DEPARTMENT, TestDomain.DEPARTMENT_NAME, "SALES");
      oldLocation = (String) department.put(TestDomain.DEPARTMENT_LOCATION, "NEWLOC");
      updatedDepartment = baseConnection.update(Collections.singletonList(department)).get(0);
      try {
        optimisticConnection.update(Collections.singletonList(department));
        fail("RecordModifiedException should have been thrown");
      }
      catch (final RecordModifiedException e) {
        assertTrue(((Entity) e.getModifiedRow()).valuesEqual(updatedDepartment));
        assertTrue(((Entity) e.getRow()).valuesEqual(department));
      }
    }
    finally {
      try {
        if (updatedDepartment != null && oldLocation != null) {
          updatedDepartment.put(TestDomain.DEPARTMENT_LOCATION, oldLocation);
          baseConnection.update(Collections.singletonList(updatedDepartment));
        }
      }
      catch (final DatabaseException e) {
        e.printStackTrace();
      }
      baseConnection.disconnect();
      optimisticConnection.disconnect();
    }
  }

  @Test
  public void dualIterator() throws Exception {
    final DefaultLocalEntityConnection connection = initializeConnection();
    final ResultIterator<Entity> deptIterator = connection.iterator(ENTITY_CONDITIONS.selectCondition(TestDomain.T_DEPARTMENT));
    while (deptIterator.hasNext()) {
      final ResultIterator<Entity> empIterator = connection.iterator(ENTITY_CONDITIONS.selectCondition(TestDomain.T_EMP,
              TestDomain.EMP_DEPARTMENT_FK, Condition.Type.LIKE, deptIterator.next()));
      while (empIterator.hasNext()) {
        empIterator.next();
      }
    }
  }

  @Test
  public void testConstructor() throws Exception {
    Connection connection = null;
    try {
      final Database db = Databases.getInstance();
      connection = db.createConnection(UNIT_TEST_USER);
      final EntityConnection conn = new DefaultLocalEntityConnection(DOMAIN, db, connection, true, true, 1);
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
  public void testConstructorInvalidConnection() throws Exception {
    assertThrows(DatabaseException.class, () -> {
      Connection connection = null;
      try {
        final Database db = Databases.getInstance();
        connection = db.createConnection(UNIT_TEST_USER);
        connection.close();
        new DefaultLocalEntityConnection(DOMAIN, db, connection, true, true, 1);
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
  public void writeBlobIncorrectType() throws DatabaseException {
    assertThrows(IllegalArgumentException.class, () -> connection.writeBlob(DOMAIN.key(TestDomain.T_DEPARTMENT), TestDomain.DEPARTMENT_NAME, new byte[0]));
  }

  @Test
  public void readBlobIncorrectType() throws DatabaseException {
    assertThrows(IllegalArgumentException.class, () -> connection.readBlob(DOMAIN.key(TestDomain.T_DEPARTMENT), TestDomain.DEPARTMENT_NAME));
  }

  @Test
  public void readWriteBlob() throws DatabaseException {
    final byte[] bytes = new byte[1024];
    new Random().nextBytes(bytes);

    final Entity scott = connection.selectSingle(TestDomain.T_EMP, TestDomain.EMP_ID, 7);
    connection.writeBlob(scott.getKey(), TestDomain.EMP_DATA, bytes);
    assertArrayEquals(bytes, connection.readBlob(scott.getKey(), TestDomain.EMP_DATA));

    final Entity blobRecordFromDb = connection.selectSingle(scott.getKey());
    assertNotNull(blobRecordFromDb);
    assertNull(blobRecordFromDb.get(TestDomain.EMP_DATA));
  }

  @Test
  public void readWriteBlobViaEntity() throws DatabaseException {
    final byte[] bytes = new byte[1024];
    new Random().nextBytes(bytes);

    final Entity scott = connection.selectSingle(TestDomain.T_EMP, TestDomain.EMP_ID, 7);
    scott.put(TestDomain.EMP_DATA, bytes);
    connection.update(Collections.singletonList(scott));

    byte[] fromDb = connection.readBlob(scott.getKey(), TestDomain.EMP_DATA);
    assertArrayEquals(bytes, fromDb);

    final Entity blobRecordFromDb = connection.selectSingle(scott.getKey());
    assertNotNull(blobRecordFromDb);
    assertNull(blobRecordFromDb.get(TestDomain.EMP_DATA));

    final byte[] newBytes = new byte[2048];
    new Random().nextBytes(newBytes);

    scott.put(TestDomain.EMP_DATA, newBytes);

    connection.update(Collections.singletonList(scott)).get(0);

    fromDb = connection.readBlob(scott.getKey(), TestDomain.EMP_DATA);
    assertArrayEquals(newBytes, fromDb);
  }

  private static DefaultLocalEntityConnection initializeConnection() throws DatabaseException {
    return new DefaultLocalEntityConnection(DOMAIN, Databases.getInstance(), UNIT_TEST_USER, true, true, 1);
  }
}