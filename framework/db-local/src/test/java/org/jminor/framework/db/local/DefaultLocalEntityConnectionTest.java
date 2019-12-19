/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.local;

import org.jminor.common.Conjunction;
import org.jminor.common.DateFormats;
import org.jminor.common.User;
import org.jminor.common.db.ConditionType;
import org.jminor.common.db.Database;
import org.jminor.common.db.Databases;
import org.jminor.common.db.ResultIterator;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.db.exception.MultipleRecordsFoundException;
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
import org.jminor.framework.db.condition.Condition;
import org.jminor.framework.db.condition.Conditions;
import org.jminor.framework.db.condition.EntitySelectCondition;
import org.jminor.framework.domain.Domain;
import org.jminor.framework.domain.Entity;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.jminor.framework.db.condition.Conditions.*;
import static org.jminor.framework.db.local.TestDomain.*;
import static org.jminor.framework.domain.Entities.getKeys;
import static org.junit.jupiter.api.Assertions.*;

public class DefaultLocalEntityConnectionTest {

  private static final User UNIT_TEST_USER = new User(
          System.getProperty("jminor.unittest.username", "scott"),
          System.getProperty("jminor.unittest.password", "tiger").toCharArray());

  private DefaultLocalEntityConnection connection;

  private static final TestDomain DOMAIN = new TestDomain();

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
      final Entity.Key key = DOMAIN.key(T_DEPARTMENT, 40);
      assertEquals(0, connection.delete(new ArrayList<>()));
      assertEquals(1, connection.delete(singletonList(key)));
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
      final Entity.Key key = DOMAIN.key(T_DEPARTMENT, 40);
      assertEquals(1, connection.delete(entityCondition(key)));
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
      //scott, james, adams
      assertEquals(3, connection.delete(entityCondition(T_EMP, conditionSet(Conjunction.AND,
              propertyCondition(EMP_NAME, ConditionType.LIKE, "%S%"),
              propertyCondition(EMP_JOB, ConditionType.LIKE, "CLERK")))));
    }
    finally {
      connection.rollbackTransaction();
    }
  }

  @Test
  public void deleteReferentialIntegrity() {
    final Entity.Key key = DOMAIN.key(T_DEPARTMENT, 10);
    assertThrows(ReferentialIntegrityException.class, () -> connection.delete(singletonList(key)));
  }

  @Test
  public void insertUniqueConstraint() {
    final Entity department = DOMAIN.entity(T_DEPARTMENT);
    department.put(DEPARTMENT_ID, 1000);
    department.put(DEPARTMENT_NAME, "SALES");
    assertThrows(UniqueConstraintException.class, () -> connection.insert(singletonList(department)));
  }

  @Test
  public void updateUniqueConstraint() throws DatabaseException {
    final Entity department = connection.selectSingle(T_DEPARTMENT, DEPARTMENT_ID, 20);
    department.put(DEPARTMENT_NAME, "SALES");
    assertThrows(UniqueConstraintException.class, () -> connection.update(singletonList(department)));
  }

  @Test
  public void insertNoParentKey() {
    final Entity emp = DOMAIN.entity(T_EMP);
    emp.put(EMP_ID, -100);
    emp.put(EMP_NAME, "Testing");
    emp.put(EMP_DEPARTMENT, -1010);//not available
    emp.put(EMP_SALARY, 2000d);
    assertThrows(ReferentialIntegrityException.class, () -> connection.insert(singletonList(emp)));
  }

  @Test
  public void updateNoParentKey() throws DatabaseException {
    final Entity emp = connection.selectSingle(T_EMP, EMP_ID, 3);
    emp.put(EMP_DEPARTMENT, -1010);//not available
    assertThrows(ReferentialIntegrityException.class, () -> connection.update(singletonList(emp)));
  }

  @Test
  public void deleteByKeyWithForeignKeys() throws DatabaseException {
    final Entity accounting = connection.selectSingle(T_DEPARTMENT, DEPARTMENT_NAME, "ACCOUNTING");
    assertThrows(ReferentialIntegrityException.class, () -> connection.delete(singletonList(accounting.getKey())));
  }

  @Test
  public void deleteByConditionWithForeignKeys() throws DatabaseException {
    assertThrows(ReferentialIntegrityException.class, () -> connection.delete(entityCondition(T_DEPARTMENT,
            Conditions.propertyCondition(DEPARTMENT_NAME, ConditionType.LIKE, "ACCOUNTING"))));
  }

  @Test
  public void fillReport() throws Exception {
    final Map<String, Object> reportParameters = new HashMap<>();
    reportParameters.put("DEPTNO", asList(10, 20));
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
  public void selectDependencies() throws Exception {
    final Map<String, Collection<Entity>> empty = connection.selectDependencies(new ArrayList<>());
    assertTrue(empty.isEmpty());
    final List<Entity> accounting = connection.select(T_DEPARTMENT, DEPARTMENT_NAME, "ACCOUNTING");
    final Map<String, Collection<Entity>> emps = connection.selectDependencies(accounting);
    assertEquals(1, emps.size());
    assertTrue(emps.containsKey(T_EMP));
    assertEquals(7, emps.get(T_EMP).size());

    final Entity emp = connection.selectSingle(T_EMP, EMP_NAME, "KING");
    final Map<String, Collection<Entity>> deps = connection.selectDependencies(singletonList(emp));
    assertTrue(deps.isEmpty());//soft foreign key reference
  }

  @Test
  public void selectLimitOffset() throws Exception {
    final EntitySelectCondition condition = entitySelectCondition(T_EMP)
            .setOrderBy(Domain.orderBy().ascending(EMP_NAME)).setLimit(2);
    List<Entity> result = connection.select(condition);
    assertEquals(2, result.size());
    condition.setLimit(3);
    condition.setOffset(3);
    result = connection.select(condition);
    assertEquals(3, result.size());
    assertEquals("BLAKE", result.get(0).get(EMP_NAME));
    assertEquals("CLARK", result.get(1).get(EMP_NAME));
    assertEquals("FORD", result.get(2).get(EMP_NAME));
  }

  @Test
  public void selectWhereNull() throws Exception {
    connection.select(T_EMP, EMP_MGR_FK, (Object[]) null);
    connection.select(T_EMP, EMP_DATA_LAZY, (Object) null);
  }

  @Test
  public void select() throws Exception {
    List<Entity> result = connection.select(new ArrayList<>());
    assertTrue(result.isEmpty());
    result = connection.select(T_DEPARTMENT, DEPARTMENT_ID, 10, 20);
    assertEquals(2, result.size());
    result = connection.select(getKeys(result));
    assertEquals(2, result.size());
    result = connection.select(entitySelectCondition(T_DEPARTMENT,
            Conditions.customCondition(DEPARTMENT_CONDITION_ID,
                    asList(DEPARTMENT_ID, DEPARTMENT_ID), asList(10, 20))));
    assertEquals(2, result.size());
    result = connection.select(entitySelectCondition(JOINED_QUERY_ENTITY_ID,
            Conditions.customCondition(JOINED_QUERY_CONDITION_ID)));
    assertEquals(7, result.size());

    final EntitySelectCondition condition = entitySelectCondition(T_EMP,
            Conditions.customCondition(EMP_NAME_IS_BLAKE_CONDITION_ID));
    result = connection.select(condition);
    Entity emp = result.get(0);
    assertTrue(emp.isLoaded(EMP_DEPARTMENT_FK));
    assertTrue(emp.isLoaded(EMP_MGR_FK));
    emp = emp.getForeignKey(EMP_MGR_FK);
    assertFalse(emp.isLoaded(EMP_MGR_FK));

    result = connection.select(condition.setForeignKeyFetchDepthLimit(EMP_DEPARTMENT_FK, 0));
    assertEquals(1, result.size());
    emp = result.get(0);
    assertFalse(emp.isLoaded(EMP_DEPARTMENT_FK));
    assertTrue(emp.isLoaded(EMP_MGR_FK));

    result = connection.select(condition.setForeignKeyFetchDepthLimit(EMP_MGR_FK, 0));
    assertEquals(1, result.size());
    emp = result.get(0);
    assertFalse(emp.isLoaded(EMP_DEPARTMENT_FK));
    assertFalse(emp.isLoaded(EMP_MGR_FK));

    result = connection.select(condition.setForeignKeyFetchDepthLimit(EMP_MGR_FK, 2));
    assertEquals(1, result.size());
    emp = result.get(0);
    assertFalse(emp.isLoaded(EMP_DEPARTMENT_FK));
    assertTrue(emp.isLoaded(EMP_MGR_FK));
    emp = emp.getForeignKey(EMP_MGR_FK);
    assertTrue(emp.isLoaded(EMP_MGR_FK));

    result = connection.select(condition.setForeignKeyFetchDepthLimit(EMP_MGR_FK, -1));
    assertEquals(1, result.size());
    emp = result.get(0);
    assertFalse(emp.isLoaded(EMP_DEPARTMENT_FK));
    assertTrue(emp.isLoaded(EMP_MGR_FK));
    emp = emp.getForeignKey(EMP_MGR_FK);
    assertTrue(emp.isLoaded(EMP_MGR_FK));
  }

  @Test
  public void selectFetchCount() throws DatabaseException {
    List<Entity> departments = connection.select(entitySelectCondition(T_DEPARTMENT));
    assertEquals(4, departments.size());
    departments = connection.select(entitySelectCondition(T_DEPARTMENT).setFetchCount(0));
    assertTrue(departments.isEmpty());
    departments = connection.select(entitySelectCondition(T_DEPARTMENT).setFetchCount(2));
    assertEquals(2, departments.size());
    departments = connection.select(entitySelectCondition(T_DEPARTMENT).setFetchCount(3));
    assertEquals(3, departments.size());
    departments = connection.select(entitySelectCondition(T_DEPARTMENT).setFetchCount(-1));
    assertEquals(4, departments.size());
  }

  @Test
  public void selectByKey() throws DatabaseException {
    final Entity.Key deptKey = DOMAIN.key(T_DEPARTMENT, 10);
    final Entity.Key empKey = DOMAIN.key(T_EMP, 8);

    final List<Entity> selected = connection.select(asList(deptKey, empKey));
    assertEquals(2, selected.size());
  }

  @Test
  public void selectPropertyIds() throws Exception {
    final List<Entity> emps = connection.select(entitySelectCondition(T_EMP)
            .setSelectPropertyIds(EMP_ID, EMP_JOB, EMP_DEPARTMENT));
    for (final Entity emp : emps) {
      assertTrue(emp.containsKey(EMP_ID));
      assertTrue(emp.containsKey(EMP_JOB));
      assertTrue(emp.containsKey(EMP_DEPARTMENT_FK));
      assertFalse(emp.containsKey(EMP_COMMISSION));
      assertFalse(emp.containsKey(EMP_HIREDATE));
      assertFalse(emp.containsKey(EMP_NAME));
      assertFalse(emp.containsKey(EMP_SALARY));
    }
  }

  @Test
  public void selectInvalidPropertyIds() throws Exception {
    assertThrows(IllegalArgumentException.class, () ->
            connection.select(entitySelectCondition(T_EMP)
                    .setSelectPropertyIds(EMP_ID, EMP_JOB, EMP_DEPARTMENT_FK)));
  }

  @Test
  public void selectInvalidColumn() throws Exception {
    assertThrows(DatabaseException.class, () -> connection.select(entitySelectCondition(T_DEPARTMENT,
            Conditions.customCondition(DEPARTMENT_CONDITION_INVALID_COLUMN_ID))));
  }

  @Test
  public void selectRowCount() throws Exception {
    int rowCount = connection.selectRowCount(entityCondition(T_DEPARTMENT));
    assertEquals(4, rowCount);
    Condition deptNoCondition = Conditions.propertyCondition(DEPARTMENT_ID, ConditionType.GREATER_THAN, 30);
    rowCount = connection.selectRowCount(entityCondition(T_DEPARTMENT, deptNoCondition));
    assertEquals(2, rowCount);

    rowCount = connection.selectRowCount(entityCondition(JOINED_QUERY_ENTITY_ID));
    assertEquals(16, rowCount);
    deptNoCondition = Conditions.propertyCondition("d.deptno", ConditionType.GREATER_THAN, 30);
    rowCount = connection.selectRowCount(entityCondition(JOINED_QUERY_ENTITY_ID, deptNoCondition));
    assertEquals(4, rowCount);

    rowCount = connection.selectRowCount(entityCondition(GROUP_BY_QUERY_ENTITY_ID));
    assertEquals(4, rowCount);
  }

  @Test
  public void selectSingle() throws Exception {
    Entity sales = connection.selectSingle(T_DEPARTMENT, DEPARTMENT_NAME, "SALES");
    assertEquals(sales.getString(DEPARTMENT_NAME), "SALES");
    sales = connection.selectSingle(sales.getKey());
    assertEquals(sales.getString(DEPARTMENT_NAME), "SALES");
    sales = connection.selectSingle(entitySelectCondition(T_DEPARTMENT,
            Conditions.customCondition(DEPARTMENT_CONDITION_SALES_ID)));
    assertEquals(sales.getString(DEPARTMENT_NAME), "SALES");

    final Entity king = connection.selectSingle(T_EMP, EMP_NAME, "KING");
    assertTrue(king.containsKey(EMP_MGR_FK));
    assertNull(king.get(EMP_MGR_FK));
  }

  @Test
  public void customCondition() throws DatabaseException {
    final Condition condition = Conditions.customCondition(EMP_MGR_GREATER_THAN_CONDITION_ID,
            singletonList(EMP_MGR), singletonList(5));

    assertEquals(4, connection.select(entitySelectCondition(T_EMP, condition)).size());
  }

  @Test
  public void executeFunction() throws DatabaseException {
    connection.executeFunction(FUNCTION_ID);
  }

  @Test
  public void executeProcedure() throws DatabaseException {
    connection.executeProcedure(PROCEDURE_ID);
  }

  @Test
  public void selectSingleNotFound() throws Exception {
    assertThrows(RecordNotFoundException.class, () -> connection.selectSingle(T_DEPARTMENT, DEPARTMENT_NAME, "NO_NAME"));
  }

  @Test
  public void selectSingleManyFound() throws Exception {
    assertThrows(MultipleRecordsFoundException.class, () -> connection.selectSingle(T_EMP, EMP_JOB, "MANAGER"));
  }

  @Test
  public void insertOnlyNullValues() throws DatabaseException {
    try {
      connection.beginTransaction();
      final Entity department = DOMAIN.entity(T_DEPARTMENT);
      assertThrows(DatabaseException.class, () -> connection.insert(singletonList(department)));
    }
    finally {
      connection.rollbackTransaction();
    }
  }

  @Test
  public void updateNoModifiedValues() throws DatabaseException {
    try {
      connection.beginTransaction();
      final Entity department = connection.selectSingle(T_DEPARTMENT, DEPARTMENT_ID, 10);
      assertThrows(DatabaseException.class, () -> connection.update(singletonList(department)));
    }
    finally {
      connection.rollbackTransaction();
    }
  }

  @Test
  public void dateTime() throws DatabaseException {
    final Entity sales = connection.selectSingle(T_DEPARTMENT, DEPARTMENT_NAME, "SALES");
    final double salary = 1500;

    Entity emp = DOMAIN.entity(T_EMP);
    emp.put(EMP_DEPARTMENT_FK, sales);
    emp.put(EMP_NAME, "Nobody");
    emp.put(EMP_SALARY, salary);
    final LocalDate hiredate = LocalDate.parse("03-10-1975", DateTimeFormatter.ofPattern(DateFormats.SHORT_DASH));
    emp.put(EMP_HIREDATE, hiredate);
    final LocalDateTime hiretime = LocalDateTime.parse("03-10-1975 08:30:22", DateTimeFormatter.ofPattern(DateFormats.FULL_TIMESTAMP));
    emp.put(EMP_HIRETIME, hiretime);

    emp = connection.selectSingle(connection.insert(singletonList(emp)).get(0));

    assertEquals(hiredate, emp.getDate(EMP_HIREDATE));
    assertEquals(hiretime, emp.getTimestamp(EMP_HIRETIME));
  }

  @Test
  public void insertWithNullValues() throws DatabaseException {
    final Entity sales = connection.selectSingle(T_DEPARTMENT, DEPARTMENT_NAME, "SALES");
    final String name = "Nobody";
    final double salary = 1500;
    final double defaultCommission = 200;

    Entity emp = DOMAIN.entity(T_EMP);
    emp.put(EMP_DEPARTMENT_FK, sales);
    emp.put(EMP_NAME, name);
    emp.put(EMP_SALARY, salary);

    emp = connection.selectSingle(connection.insert(singletonList(emp)).get(0));
    assertEquals(sales, emp.get(EMP_DEPARTMENT_FK));
    assertEquals(name, emp.get(EMP_NAME));
    assertEquals(salary, emp.get(EMP_SALARY));
    assertEquals(defaultCommission, emp.get(EMP_COMMISSION));
    connection.delete(singletonList(emp.getKey()));

    emp.put(EMP_COMMISSION, null);//default value should not kick in
    emp = connection.selectSingle(connection.insert(singletonList(emp)).get(0));
    assertEquals(sales, emp.get(EMP_DEPARTMENT_FK));
    assertEquals(name, emp.get(EMP_NAME));
    assertEquals(salary, emp.get(EMP_SALARY));
    assertNull(emp.get(EMP_COMMISSION));
    connection.delete(singletonList(emp.getKey()));

    emp.remove(EMP_COMMISSION);//default value should kick in
    emp = connection.selectSingle(connection.insert(singletonList(emp)).get(0));
    assertEquals(sales, emp.get(EMP_DEPARTMENT_FK));
    assertEquals(name, emp.get(EMP_NAME));
    assertEquals(salary, emp.get(EMP_SALARY));
    assertEquals(defaultCommission, emp.get(EMP_COMMISSION));
    connection.delete(singletonList(emp.getKey()));
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
      final Entity sales = connection.selectSingle(T_DEPARTMENT, DEPARTMENT_NAME, "SALES");
      final Entity king = connection.selectSingle(T_EMP, EMP_NAME, "KING");
      final String newName = "New name";
      sales.put(DEPARTMENT_NAME, newName);
      king.put(EMP_NAME, newName);
      final List<Entity> updated = connection.update(asList(sales, king));
      assertTrue(updated.containsAll(asList(sales, king)));
      assertEquals(newName, updated.get(updated.indexOf(sales)).getString(DEPARTMENT_NAME));
      assertEquals(newName, updated.get(updated.indexOf(king)).getString(EMP_NAME));
    }
    finally {
      connection.rollbackTransaction();
    }
  }

  @Test
  public void updateNonExisting() throws DatabaseException {
    //otherwise the optimistic locking triggers an error
    connection.setOptimisticLocking(false);
    final Entity employee = connection.selectSingle(T_EMP, EMP_ID, 4);
    employee.put(EMP_ID, -888);//non existing
    employee.saveAll();
    employee.put(EMP_NAME, "New name");
    assertThrows(UpdateException.class, () -> connection.update(singletonList(employee)));
  }

  @Test
  public void update() throws DatabaseException {
    final List<Entity> updated = connection.update(new ArrayList<>());
    assertTrue(updated.isEmpty());
  }

  @Test
  public void selectValuesNonColumnProperty() throws Exception {
    assertThrows(IllegalArgumentException.class, () -> connection.selectValues(EMP_DEPARTMENT_LOCATION, entityCondition(T_EMP)));
  }

  @Test
  public void selectValues() throws Exception {
    List<Object> result = connection.selectValues(DEPARTMENT_NAME, entityCondition(T_DEPARTMENT));
    assertEquals("ACCOUNTING", result.get(0));
    assertEquals("OPERATIONS", result.get(1));
    assertEquals("RESEARCH", result.get(2));
    assertEquals("SALES", result.get(3));

    result = connection.selectValues(DEPARTMENT_NAME, entityCondition(T_DEPARTMENT,
            Conditions.propertyCondition(DEPARTMENT_ID, ConditionType.LIKE, 10)));
    assertTrue(result.contains("ACCOUNTING"));
    assertFalse(result.contains("SALES"));
  }

  @Test
  public void selectForUpdateModified() throws Exception {
    final DefaultLocalEntityConnection connection = initializeConnection();
    final DefaultLocalEntityConnection connection2 = initializeConnection();
    final String originalLocation;
    try {
      final EntitySelectCondition condition = entitySelectCondition(T_DEPARTMENT, DEPARTMENT_NAME, ConditionType.LIKE, "SALES");
      condition.setForUpdate(true);

      Entity sales = connection.selectSingle(condition);
      originalLocation = sales.getString(DEPARTMENT_LOCATION);

      sales.put(DEPARTMENT_LOCATION, "Syracuse");
      try {
        connection2.update(singletonList(sales));
        fail("Should not be able to update record selected for update by another connection");
      }
      catch (final DatabaseException ignored) {
        connection2.getDatabaseConnection().rollback();
      }

      connection.select(entitySelectCondition(T_DEPARTMENT));//any query will do

      try {
        sales = connection2.update(singletonList(sales)).get(0);
        sales.put(DEPARTMENT_LOCATION, originalLocation);
        connection2.update(singletonList(sales));//revert changes to data
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
      final EntitySelectCondition condition = entitySelectCondition(T_EMP, EMP_NAME, ConditionType.LIKE, "ALLEN");

      allen = connection.selectSingle(condition);

      connection2.delete(singletonList(allen.getKey()));

      allen.put(EMP_JOB, "CLERK");
      try {
        connection.update(singletonList(allen));
        fail("Should not be able to update record deleted by another connection");
      }
      catch (final RecordModifiedException e) {
        assertNotNull(e.getRow());
        assertNull(e.getModifiedRow());
      }

      try {
        connection2.insert(singletonList(allen));//revert changes to data
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
      final Entity department = baseConnection.selectSingle(T_DEPARTMENT, DEPARTMENT_NAME, "SALES");
      oldLocation = (String) department.put(DEPARTMENT_LOCATION, "NEWLOC");
      updatedDepartment = baseConnection.update(singletonList(department)).get(0);
      try {
        optimisticConnection.update(singletonList(department));
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
          updatedDepartment.put(DEPARTMENT_LOCATION, oldLocation);
          baseConnection.update(singletonList(updatedDepartment));
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
    final ResultIterator<Entity> deptIterator = connection.iterator(entitySelectCondition(T_DEPARTMENT));
    while (deptIterator.hasNext()) {
      final ResultIterator<Entity> empIterator = connection.iterator(entitySelectCondition(T_EMP,
              EMP_DEPARTMENT_FK, ConditionType.LIKE, deptIterator.next()));
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
      final EntityConnection conn = new DefaultLocalEntityConnection(DOMAIN, db, connection, 1);
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
        new DefaultLocalEntityConnection(DOMAIN, db, connection, 1);
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
    assertThrows(IllegalArgumentException.class, () -> connection.writeBlob(DOMAIN.key(T_DEPARTMENT), DEPARTMENT_NAME, new byte[0]));
  }

  @Test
  public void readBlobIncorrectType() throws DatabaseException {
    assertThrows(IllegalArgumentException.class, () -> connection.readBlob(DOMAIN.key(T_DEPARTMENT), DEPARTMENT_NAME));
  }

  @Test
  public void readWriteBlob() throws DatabaseException {
    final byte[] lazyBytes = new byte[1024];
    final byte[] bytes = new byte[1024];
    final Random random = new Random();
    random.nextBytes(lazyBytes);
    random.nextBytes(bytes);

    final Entity scott = connection.selectSingle(T_EMP, EMP_ID, 7);
    connection.writeBlob(scott.getKey(), EMP_DATA_LAZY, lazyBytes);
    connection.writeBlob(scott.getKey(), EMP_DATA, bytes);
    assertArrayEquals(lazyBytes, connection.readBlob(scott.getKey(), EMP_DATA_LAZY));
    assertArrayEquals(bytes, connection.readBlob(scott.getKey(), EMP_DATA));

    Entity scottFromDb = connection.selectSingle(scott.getKey());
    //lazy loaded
    assertNull(scottFromDb.get(EMP_DATA_LAZY));
    assertNotNull(scottFromDb.get(EMP_DATA));

    //overrides lazy loading
    scottFromDb = connection.selectSingle(entitySelectCondition(scott.getKey()).setSelectPropertyIds(EMP_DATA_LAZY));
    assertNotNull(scottFromDb.get(EMP_DATA_LAZY));
  }

  @Test
  public void readWriteBlobViaEntity() throws DatabaseException {
    final byte[] lazyBytes = new byte[1024];
    final byte[] bytes = new byte[1024];
    final Random random = new Random();
    random.nextBytes(lazyBytes);
    random.nextBytes(bytes);

    final Entity scott = connection.selectSingle(T_EMP, EMP_ID, 7);
    scott.put(EMP_DATA_LAZY, lazyBytes);
    scott.put(EMP_DATA, bytes);
    connection.update(singletonList(scott));

    byte[] lazyFromDb = connection.readBlob(scott.getKey(), EMP_DATA_LAZY);
    final byte[] fromDb = connection.readBlob(scott.getKey(), EMP_DATA);
    assertArrayEquals(lazyBytes, lazyFromDb);
    assertArrayEquals(bytes, fromDb);

    Entity scottFromDb = connection.selectSingle(scott.getKey());
    //lazy loaded
    assertNull(scottFromDb.get(EMP_DATA_LAZY));
    assertNotNull(scottFromDb.get(EMP_DATA));
    assertArrayEquals(bytes, scottFromDb.getBlob(EMP_DATA));

    final byte[] newLazyBytes = new byte[2048];
    final byte[] newBytes = new byte[2048];
    random.nextBytes(newLazyBytes);
    random.nextBytes(newBytes);

    scott.put(EMP_DATA_LAZY, newLazyBytes);
    scott.put(EMP_DATA, newBytes);

    connection.update(singletonList(scott)).get(0);

    lazyFromDb = connection.readBlob(scott.getKey(), EMP_DATA_LAZY);
    assertArrayEquals(newLazyBytes, lazyFromDb);

    scottFromDb = connection.selectSingle(scott.getKey());
    assertArrayEquals(newBytes, scottFromDb.getBlob(EMP_DATA));
  }

  @Test
  public void testUUIDPrimaryKeyColumnWithDefaultValue() throws DatabaseException {
    final Entity entity = DOMAIN.entity(T_UUID_TEST_DEFAULT);
    entity.put(UUID_TEST_DEFAULT_DATA, "test");
    connection.insert(singletonList(entity));
    assertNotNull(entity.get(UUID_TEST_DEFAULT_ID));
    assertEquals("test", entity.getString(UUID_TEST_DEFAULT_DATA));
  }

  @Test
  public void testUUIDPrimaryKeyColumnWithoutDefaultValue() throws DatabaseException {
    final Entity entity = DOMAIN.entity(T_UUID_TEST_NO_DEFAULT);
    entity.put(UUID_TEST_NO_DEFAULT_DATA, "test");
    connection.insert(singletonList(entity));
    assertNotNull(entity.get(UUID_TEST_NO_DEFAULT_ID));
    assertEquals("test", entity.getString(UUID_TEST_NO_DEFAULT_DATA));
  }

  private static DefaultLocalEntityConnection initializeConnection() throws DatabaseException {
    return new DefaultLocalEntityConnection(DOMAIN, Databases.getInstance(), UNIT_TEST_USER, 1);
  }
}