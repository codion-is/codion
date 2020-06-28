/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.local;

import is.codion.common.DateFormats;
import is.codion.common.db.database.Database;
import is.codion.common.db.database.Databases;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.db.exception.MultipleRecordsFoundException;
import is.codion.common.db.exception.RecordModifiedException;
import is.codion.common.db.exception.RecordNotFoundException;
import is.codion.common.db.exception.ReferentialIntegrityException;
import is.codion.common.db.exception.UniqueConstraintException;
import is.codion.common.db.exception.UpdateException;
import is.codion.common.db.result.ResultIterator;
import is.codion.common.user.User;
import is.codion.common.user.Users;
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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static is.codion.framework.db.condition.Conditions.condition;
import static is.codion.framework.db.local.TestDomain.*;
import static is.codion.framework.domain.entity.Entities.getKeys;
import static is.codion.framework.domain.entity.OrderBy.orderBy;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;

public class DefaultLocalEntityConnectionTest {

  private static final User UNIT_TEST_USER =
          Users.parseUser(System.getProperty("codion.test.user", "scott:tiger"));

  private DefaultLocalEntityConnection connection;

  private static final TestDomain DOMAIN = new TestDomain();
  private static final Entities ENTITIES = DOMAIN.getEntities();

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
      final Key key = ENTITIES.key(Department.TYPE, 40);
      assertEquals(0, connection.delete(new ArrayList<>()));
      assertTrue(connection.delete(key));
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
      final Key key = ENTITIES.key(Department.TYPE, 40);
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
    try {
      connection.beginTransaction();
      //scott, james, adams
      assertEquals(3, connection.delete(condition(EMP_NAME).equalTo("%S%")
              .and(condition(EMP_JOB).equalTo("CLERK"))));
    }
    finally {
      connection.rollbackTransaction();
    }
  }

  @Test
  public void deleteReferentialIntegrity() {
    final Key key = ENTITIES.key(Department.TYPE, 10);
    assertThrows(ReferentialIntegrityException.class, () -> connection.delete(key));
  }

  @Test
  public void insertUniqueConstraint() {
    final Entity department = ENTITIES.entity(Department.TYPE);
    department.put(Department.DEPTNO, 1000);
    department.put(Department.DNAME, "SALES");
    assertThrows(UniqueConstraintException.class, () -> connection.insert(department));
  }

  @Test
  public void updateUniqueConstraint() throws DatabaseException {
    final Entity department = connection.selectSingle(Department.DEPTNO, 20);
    department.put(Department.DNAME, "SALES");
    assertThrows(UniqueConstraintException.class, () -> connection.update(department));
  }

  @Test
  public void insertNoParentKey() {
    final Entity emp = ENTITIES.entity(T_EMP);
    emp.put(EMP_ID, -100);
    emp.put(EMP_NAME, "Testing");
    emp.put(EMP_DEPARTMENT, -1010);//not available
    emp.put(EMP_SALARY, 2000d);
    assertThrows(ReferentialIntegrityException.class, () -> connection.insert(emp));
  }

  @Test
  public void insertNoPk() throws DatabaseException {
    final Entity noPk = ENTITIES.entity(T_NO_PK);
    noPk.put(NO_PK_COL1, 10);
    noPk.put(NO_PK_COL2, "10");
    noPk.put(NO_PK_COL3, "10");
    noPk.put(NO_PK_COL4, 10);

    final Key key = connection.insert(noPk);
    assertTrue(key.isNull());
  }

  @Test
  public void updateNoParentKey() throws DatabaseException {
    final Entity emp = connection.selectSingle(EMP_ID, 3);
    emp.put(EMP_DEPARTMENT, -1010);//not available
    assertThrows(ReferentialIntegrityException.class, () -> connection.update(emp));
  }

  @Test
  public void deleteByKeyWithForeignKeys() throws DatabaseException {
    final Entity accounting = connection.selectSingle(Department.DNAME, "ACCOUNTING");
    assertThrows(ReferentialIntegrityException.class, () -> connection.delete(accounting.getKey()));
  }

  @Test
  public void deleteByConditionWithForeignKeys() throws DatabaseException {
    assertThrows(ReferentialIntegrityException.class, () ->
            connection.delete(Conditions.condition(Department.DNAME).equalTo("ACCOUNTING")));
  }

  @Test
  public void fillReport() throws Exception {
    final Map<String, Object> reportParameters = new HashMap<>();
    reportParameters.put("DEPTNO", asList(10, 20));
    assertEquals("result", connection.fillReport(REPORT, reportParameters));
  }

  @Test
  public void selectDependencies() throws Exception {
    final Map<EntityType<?>, Collection<Entity>> empty = connection.selectDependencies(new ArrayList<>());
    assertTrue(empty.isEmpty());
    final List<Entity> accounting = connection.select(Department.DNAME, "ACCOUNTING");
    final Map<EntityType<?>, Collection<Entity>> emps = connection.selectDependencies(accounting);
    assertEquals(1, emps.size());
    assertTrue(emps.containsKey(T_EMP));
    assertEquals(7, emps.get(T_EMP).size());

    final Entity emp = connection.selectSingle(EMP_NAME, "KING");
    final Map<EntityType<?>, Collection<Entity>> deps = connection.selectDependencies(singletonList(emp));
    assertTrue(deps.isEmpty());//soft foreign key reference
  }

  @Test
  public void selectLimitOffset() throws Exception {
    final SelectCondition condition = condition(T_EMP).selectCondition()
            .setOrderBy(orderBy().ascending(EMP_NAME)).setLimit(2);
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
    connection.select(EMP_MGR_FK, (Entity) null);
    connection.select(EMP_DATA_LAZY, (byte[]) null);
  }

  @Test
  public void select() throws Exception {
    List<Entity> result = connection.select(new ArrayList<>());
    assertTrue(result.isEmpty());
    result = connection.select(Department.DEPTNO, asList(10, 20));
    assertEquals(2, result.size());
    result = connection.select(getKeys(result));
    assertEquals(2, result.size());
    result = connection.select(Conditions.customCondition(Department.DEPARTMENT_CONDITION_TYPE,
                    asList(Department.DEPTNO, Department.DEPTNO), asList(10, 20)).selectCondition());
    assertEquals(2, result.size());
    result = connection.select(Conditions.customCondition(JOINED_QUERY_CONDITION_TYPE).selectCondition());
    assertEquals(7, result.size());

    final SelectCondition condition = Conditions.customCondition(EMP_NAME_IS_BLAKE_CONDITION_ID).selectCondition();
    result = connection.select(condition);
    Entity emp = result.get(0);
    assertTrue(emp.isLoaded(EMP_DEPARTMENT_FK));
    assertTrue(emp.isLoaded(EMP_MGR_FK));
    emp = emp.getForeignKey(EMP_MGR_FK);
    assertFalse(emp.isLoaded(EMP_MGR_FK));

    result = connection.select(condition.setForeignKeyFetchDepth(EMP_DEPARTMENT_FK, 0));
    assertEquals(1, result.size());
    emp = result.get(0);
    assertFalse(emp.isLoaded(EMP_DEPARTMENT_FK));
    assertTrue(emp.isLoaded(EMP_MGR_FK));

    result = connection.select(condition.setForeignKeyFetchDepth(EMP_MGR_FK, 0));
    assertEquals(1, result.size());
    emp = result.get(0);
    assertFalse(emp.isLoaded(EMP_DEPARTMENT_FK));
    assertFalse(emp.isLoaded(EMP_MGR_FK));

    result = connection.select(condition.setForeignKeyFetchDepth(EMP_MGR_FK, 2));
    assertEquals(1, result.size());
    emp = result.get(0);
    assertFalse(emp.isLoaded(EMP_DEPARTMENT_FK));
    assertTrue(emp.isLoaded(EMP_MGR_FK));
    emp = emp.getForeignKey(EMP_MGR_FK);
    assertTrue(emp.isLoaded(EMP_MGR_FK));

    result = connection.select(condition.setForeignKeyFetchDepth(EMP_MGR_FK, -1));
    assertEquals(1, result.size());
    emp = result.get(0);
    assertFalse(emp.isLoaded(EMP_DEPARTMENT_FK));
    assertTrue(emp.isLoaded(EMP_MGR_FK));
    emp = emp.getForeignKey(EMP_MGR_FK);
    assertTrue(emp.isLoaded(EMP_MGR_FK));

    assertEquals(4, connection.rowCount(Conditions.condition(EMP_ID).equalTo(asList(1, 2, 3, 4))));
    assertEquals(0, connection.rowCount(Conditions.condition(EMP_DEPARTMENT).isNull()));
    assertEquals(0, connection.rowCount(Conditions.condition(EMP_DEPARTMENT_FK).isNull()));
    assertEquals(1, connection.rowCount(Conditions.condition(EMP_MGR).isNull()));
    assertEquals(1, connection.rowCount(Conditions.condition(EMP_MGR_FK).isNull()));
  }

  @Test
  public void selectFetchCount() throws DatabaseException {
    List<Entity> departments = connection.select(condition(Department.TYPE).selectCondition());
    assertEquals(4, departments.size());
    departments = connection.select(condition(Department.TYPE).selectCondition().setFetchCount(0));
    assertTrue(departments.isEmpty());
    departments = connection.select(condition(Department.TYPE).selectCondition().setFetchCount(2));
    assertEquals(2, departments.size());
    departments = connection.select(condition(Department.TYPE).selectCondition().setFetchCount(3));
    assertEquals(3, departments.size());
    departments = connection.select(condition(Department.TYPE).selectCondition().setFetchCount(-1));
    assertEquals(4, departments.size());
  }

  @Test
  public void selectByKey() throws DatabaseException {
    final Key deptKey = ENTITIES.key(Department.TYPE, 10);
    final Key empKey = ENTITIES.key(T_EMP, 8);

    final List<Entity> selected = connection.select(asList(deptKey, empKey));
    assertEquals(2, selected.size());
  }

  @Test
  public void selectAttributes() throws Exception {
    final List<Entity> emps = connection.select(condition(T_EMP).selectCondition()
            .setSelectAttributes(EMP_ID, EMP_JOB, EMP_DEPARTMENT));
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
  public void selectInvalidAttributes() throws Exception {
    assertThrows(IllegalArgumentException.class, () ->
            connection.select(condition(T_EMP).selectCondition()
                    .setSelectAttributes(EMP_ID, EMP_JOB, EMP_DEPARTMENT_FK)));
  }

  @Test
  public void selectInvalidColumn() throws Exception {
    assertThrows(DatabaseException.class, () -> connection.select(Conditions.customCondition(Department.DEPARTMENT_CONDITION_INVALID_COLUMN_TYPE).selectCondition()));
  }

  @Test
  public void rowCount() throws Exception {
    int rowCount = connection.rowCount(condition(Department.TYPE));
    assertEquals(4, rowCount);
    Condition deptNoCondition = Conditions.condition(Department.DEPTNO).greaterThan(30);
    rowCount = connection.rowCount(deptNoCondition);
    assertEquals(2, rowCount);

    rowCount = connection.rowCount(condition(JOINED_QUERY_ENTITY_TYPE));
    assertEquals(16, rowCount);
    deptNoCondition = Conditions.condition(JOINED_DEPTNO).greaterThan(30);
    rowCount = connection.rowCount(deptNoCondition);
    assertEquals(4, rowCount);

    rowCount = connection.rowCount(condition(GROUP_BY_QUERY_ENTITY_TYPE));
    assertEquals(4, rowCount);
  }

  @Test
  public void selectSingle() throws Exception {
    Entity sales = connection.selectSingle(Department.DNAME, "SALES");
    assertEquals(sales.get(Department.DNAME), "SALES");
    sales = connection.selectSingle(sales.getKey());
    assertEquals(sales.get(Department.DNAME), "SALES");
    sales = connection.selectSingle(Conditions.customCondition(Department.DEPARTMENT_CONDITION_SALES_TYPE).selectCondition());
    assertEquals(sales.get(Department.DNAME), "SALES");

    final Entity king = connection.selectSingle(EMP_NAME, "KING");
    assertTrue(king.containsKey(EMP_MGR_FK));
    assertNull(king.get(EMP_MGR_FK));
  }

  @Test
  public void customCondition() throws DatabaseException {
    final Condition condition = Conditions.customCondition(EMP_MGR_GREATER_THAN_CONDITION_ID,
            singletonList(EMP_MGR), singletonList(5));

    assertEquals(4, connection.select(condition.selectCondition()).size());
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
    assertThrows(RecordNotFoundException.class, () -> connection.selectSingle(Department.DNAME, "NO_NAME"));
  }

  @Test
  public void selectSingleManyFound() throws Exception {
    assertThrows(MultipleRecordsFoundException.class, () -> connection.selectSingle(EMP_JOB, "MANAGER"));
  }

  @Test
  public void insertOnlyNullValues() throws DatabaseException {
    try {
      connection.beginTransaction();
      final Entity department = ENTITIES.entity(Department.TYPE);
      assertThrows(DatabaseException.class, () -> connection.insert(department));
    }
    finally {
      connection.rollbackTransaction();
    }
  }

  @Test
  public void updateNoModifiedValues() throws DatabaseException {
    try {
      connection.beginTransaction();
      final Entity department = connection.selectSingle(Department.DEPTNO, 10);
      assertThrows(DatabaseException.class, () -> connection.update(department));
    }
    finally {
      connection.rollbackTransaction();
    }
  }

  @Test
  public void dateTime() throws DatabaseException {
    final Entity sales = connection.selectSingle(Department.DNAME, "SALES");
    final double salary = 1500;

    Entity emp = ENTITIES.entity(T_EMP);
    emp.put(EMP_DEPARTMENT_FK, sales);
    emp.put(EMP_NAME, "Nobody");
    emp.put(EMP_SALARY, salary);
    final LocalDate hiredate = LocalDate.parse("03-10-1975", DateTimeFormatter.ofPattern(DateFormats.SHORT_DASH));
    emp.put(EMP_HIREDATE, hiredate);
    final LocalDateTime hiretime = LocalDateTime.parse("03-10-1975 08:30:22", DateTimeFormatter.ofPattern(DateFormats.FULL_TIMESTAMP));
    emp.put(EMP_HIRETIME, hiretime);

    emp = connection.selectSingle(connection.insert(emp));

    assertEquals(hiredate, emp.get(EMP_HIREDATE));
    assertEquals(hiretime, emp.get(EMP_HIRETIME));
  }

  @Test
  public void insertWithNullValues() throws DatabaseException {
    final Entity sales = connection.selectSingle(Department.DNAME, "SALES");
    final String name = "Nobody";
    final double salary = 1500;
    final double defaultCommission = 200;

    Entity emp = ENTITIES.entity(T_EMP);
    emp.put(EMP_DEPARTMENT_FK, sales);
    emp.put(EMP_NAME, name);
    emp.put(EMP_SALARY, salary);

    emp = connection.selectSingle(connection.insert(emp));
    assertEquals(sales, emp.get(EMP_DEPARTMENT_FK));
    assertEquals(name, emp.get(EMP_NAME));
    assertEquals(salary, emp.get(EMP_SALARY));
    assertEquals(defaultCommission, emp.get(EMP_COMMISSION));
    connection.delete(emp.getKey());

    emp.put(EMP_COMMISSION, null);//default value should not kick in
    emp = connection.selectSingle(connection.insert(emp));
    assertEquals(sales, emp.get(EMP_DEPARTMENT_FK));
    assertEquals(name, emp.get(EMP_NAME));
    assertEquals(salary, emp.get(EMP_SALARY));
    assertNull(emp.get(EMP_COMMISSION));
    connection.delete(emp.getKey());

    emp.remove(EMP_COMMISSION);//default value should kick in
    emp = connection.selectSingle(connection.insert(emp));
    assertEquals(sales, emp.get(EMP_DEPARTMENT_FK));
    assertEquals(name, emp.get(EMP_NAME));
    assertEquals(salary, emp.get(EMP_SALARY));
    assertEquals(defaultCommission, emp.get(EMP_COMMISSION));
    connection.delete(emp.getKey());
  }

  @Test
  public void insertEmptyList() throws DatabaseException {
    final List<Key> pks = connection.insert(new ArrayList<>());
    assertTrue(pks.isEmpty());
  }

  @Test
  public void updateDifferentEntities() throws DatabaseException {
    try {
      connection.beginTransaction();
      final Entity sales = connection.selectSingle(Department.DNAME, "SALES");
      final Entity king = connection.selectSingle(EMP_NAME, "KING");
      final String newName = "New name";
      sales.put(Department.DNAME, newName);
      king.put(EMP_NAME, newName);
      final List<Entity> updated = connection.update(asList(sales, king));
      assertTrue(updated.containsAll(asList(sales, king)));
      assertEquals(newName, updated.get(updated.indexOf(sales)).get(Department.DNAME));
      assertEquals(newName, updated.get(updated.indexOf(king)).get(EMP_NAME));
    }
    finally {
      connection.rollbackTransaction();
    }
  }

  @Test
  public void updateNonExisting() throws DatabaseException {
    //otherwise the optimistic locking triggers an error
    connection.setOptimisticLockingEnabled(false);
    final Entity employee = connection.selectSingle(EMP_ID, 4);
    employee.put(EMP_ID, -888);//non existing
    employee.saveAll();
    employee.put(EMP_NAME, "New name");
    assertThrows(UpdateException.class, () -> connection.update(employee));
  }

  @Test
  public void update() throws DatabaseException {
    final List<Entity> updated = connection.update(new ArrayList<>());
    assertTrue(updated.isEmpty());
  }

  @Test
  public void updateWithConditionNoProperties() throws DatabaseException {
    final UpdateCondition condition = Conditions.condition(T_EMP).updateCondition();
    assertThrows(IllegalArgumentException.class, () -> connection.update(condition));
  }

  @Test
  public void updateWithCondition() throws DatabaseException {
    final SelectCondition selectCondition = Conditions.condition(EMP_COMMISSION).isNull().selectCondition();

    final List<Entity> entities = connection.select(selectCondition);

    final UpdateCondition updateCondition = Conditions.condition(EMP_COMMISSION).isNull().updateCondition()
            .set(EMP_COMMISSION, 500d)
            .set(EMP_SALARY, 4200d);
    try {
      connection.beginTransaction();
      connection.update(updateCondition);
      assertEquals(0, connection.rowCount(selectCondition));
      final List<Entity> afterUpdate = connection.select(Entities.getKeys(entities));
      for (final Entity entity : afterUpdate) {
        assertEquals(500d, entity.get(EMP_COMMISSION));
        assertEquals(4200d, entity.get(EMP_SALARY));
      }
    }
    finally {
      connection.rollbackTransaction();
    }
  }

  @Test
  public void updateWithConditionNoRows() throws DatabaseException {
    final UpdateCondition updateCondition = Conditions.condition(EMP_ID).isNull().updateCondition()
            .set(EMP_SALARY, 4200d);
    try {
      connection.beginTransaction();
      assertEquals(0, connection.update(updateCondition));
    }
    finally {
      connection.rollbackTransaction();
    }
  }

  @Test
  public void selectValuesNonColumnProperty() throws Exception {
    assertThrows(IllegalArgumentException.class, () -> connection.select(EMP_DEPARTMENT_LOCATION));
  }

  @Test
  public void selectValues() throws Exception {
    List<String> result = connection.select(Department.DNAME);
    assertEquals("ACCOUNTING", result.get(0));
    assertEquals("OPERATIONS", result.get(1));
    assertEquals("RESEARCH", result.get(2));
    assertEquals("SALES", result.get(3));

    result = connection.select(Department.DNAME, Conditions.condition(Department.DEPTNO).equalTo(10));
    assertTrue(result.contains("ACCOUNTING"));
    assertFalse(result.contains("SALES"));
  }

  @Test
  public void selectValuesIncorrectAttribute() throws Exception {
    assertThrows(IllegalArgumentException.class, () -> connection.select(Department.DNAME,
            condition(EMP_ID).equalTo(1)));
  }

  @Test
  public void selectForUpdateModified() throws Exception {
    final DefaultLocalEntityConnection connection = initializeConnection();
    final DefaultLocalEntityConnection connection2 = initializeConnection();
    final String originalLocation;
    try {
      final SelectCondition condition = condition(Department.DNAME).equalTo("SALES").selectCondition();
      condition.setForUpdate(true);

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

      connection.select(condition(Department.TYPE).selectCondition());//any query will do

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
      connection.disconnect();
      connection2.disconnect();
    }
  }

  @Test
  public void optimisticLockingDeleted() throws Exception {
    final DefaultLocalEntityConnection connection = initializeConnection();
    final EntityConnection connection2 = initializeConnection();
    connection.setOptimisticLockingEnabled(true);
    final Entity allen;
    try {
      final SelectCondition condition = condition(EMP_NAME).equalTo("ALLEN").selectCondition();

      allen = connection.selectSingle(condition);

      connection2.delete(allen.getKey());

      allen.put(EMP_JOB, "CLERK");
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
      connection.disconnect();
      connection2.disconnect();
    }
  }

  @Test
  public void optimisticLockingModified() throws Exception {
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
        assertTrue(((Entity) e.getModifiedRow()).valuesEqual(updatedDepartment));
        assertTrue(((Entity) e.getRow()).valuesEqual(department));
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
      baseConnection.disconnect();
      optimisticConnection.disconnect();
    }
  }

  @Test
  public void optimisticLockingBlob() throws Exception {
    final DefaultLocalEntityConnection baseConnection = initializeConnection();
    final DefaultLocalEntityConnection optimisticConnection = initializeConnection();
    optimisticConnection.setOptimisticLockingEnabled(true);
    Entity updatedEmployee = null;
    try {
      final Random random = new Random();
      final byte[] bytes = new byte[1024];
      random.nextBytes(bytes);

      final Entity employee = baseConnection.selectSingle(EMP_NAME, "BLAKE");
      employee.put(EMP_DATA, bytes);
      updatedEmployee = baseConnection.update(employee);

      random.nextBytes(bytes);
      employee.put(EMP_DATA, bytes);

      try {
        optimisticConnection.update(employee);
        fail("RecordModifiedException should have been thrown");
      }
      catch (final RecordModifiedException e) {
        assertTrue(((Entity) e.getModifiedRow()).valuesEqual(updatedEmployee));
        assertTrue(((Entity) e.getRow()).valuesEqual(employee));
      }
    }
    finally {
      baseConnection.disconnect();
      optimisticConnection.disconnect();
    }
  }

  @Test
  public void dualIterator() throws Exception {
    final DefaultLocalEntityConnection connection = initializeConnection();
    final ResultIterator<Entity> deptIterator =
            connection.iterator(condition(Department.TYPE).selectCondition());
    while (deptIterator.hasNext()) {
      final ResultIterator<Entity> empIterator =
              connection.iterator(condition(EMP_DEPARTMENT_FK).equalTo(deptIterator.next()).selectCondition());
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
  public void testConstructorInvalidConnection() throws Exception {
    assertThrows(DatabaseException.class, () -> {
      Connection connection = null;
      try {
        final Database db = Databases.getInstance();
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
  public void readWriteBlob() throws DatabaseException {
    final byte[] lazyBytes = new byte[1024];
    final byte[] bytes = new byte[1024];
    final Random random = new Random();
    random.nextBytes(lazyBytes);
    random.nextBytes(bytes);

    final Entity scott = connection.selectSingle(EMP_ID, 7);
    connection.writeBlob(scott.getKey(), EMP_DATA_LAZY, lazyBytes);
    connection.writeBlob(scott.getKey(), EMP_DATA, bytes);
    assertArrayEquals(lazyBytes, connection.readBlob(scott.getKey(), EMP_DATA_LAZY));
    assertArrayEquals(bytes, connection.readBlob(scott.getKey(), EMP_DATA));

    Entity scottFromDb = connection.selectSingle(scott.getKey());
    //lazy loaded
    assertNull(scottFromDb.get(EMP_DATA_LAZY));
    assertNotNull(scottFromDb.get(EMP_DATA));

    //overrides lazy loading
    scottFromDb = connection.selectSingle(condition(scott.getKey()).selectCondition().setSelectAttributes(EMP_DATA_LAZY));
    assertNotNull(scottFromDb.get(EMP_DATA_LAZY));
  }

  @Test
  public void readWriteBlobViaEntity() throws DatabaseException {
    final byte[] lazyBytes = new byte[1024];
    final byte[] bytes = new byte[1024];
    final Random random = new Random();
    random.nextBytes(lazyBytes);
    random.nextBytes(bytes);

    final Entity scott = connection.selectSingle(EMP_ID, 7);
    scott.put(EMP_DATA_LAZY, lazyBytes);
    scott.put(EMP_DATA, bytes);
    connection.update(scott);
    scott.saveAll();

    byte[] lazyFromDb = connection.readBlob(scott.getKey(), EMP_DATA_LAZY);
    final byte[] fromDb = connection.readBlob(scott.getKey(), EMP_DATA);
    assertArrayEquals(lazyBytes, lazyFromDb);
    assertArrayEquals(bytes, fromDb);

    Entity scottFromDb = connection.selectSingle(scott.getKey());
    //lazy loaded
    assertNull(scottFromDb.get(EMP_DATA_LAZY));
    assertNotNull(scottFromDb.get(EMP_DATA));
    assertArrayEquals(bytes, scottFromDb.get(EMP_DATA));

    final byte[] newLazyBytes = new byte[2048];
    final byte[] newBytes = new byte[2048];
    random.nextBytes(newLazyBytes);
    random.nextBytes(newBytes);

    scott.put(EMP_DATA_LAZY, newLazyBytes);
    scott.put(EMP_DATA, newBytes);

    connection.update(scott);

    lazyFromDb = connection.readBlob(scott.getKey(), EMP_DATA_LAZY);
    assertArrayEquals(newLazyBytes, lazyFromDb);

    scottFromDb = connection.selectSingle(scott.getKey());
    assertArrayEquals(newBytes, scottFromDb.get(EMP_DATA));
  }

  @Test
  public void testUUIDPrimaryKeyColumnWithDefaultValue() throws DatabaseException {
    final Entity entity = ENTITIES.entity(T_UUID_TEST_DEFAULT);
    entity.put(UUID_TEST_DEFAULT_DATA, "test");
    connection.insert(entity);
    assertNotNull(entity.get(UUID_TEST_DEFAULT_ID));
    assertEquals("test", entity.get(UUID_TEST_DEFAULT_DATA));
  }

  @Test
  public void testUUIDPrimaryKeyColumnWithoutDefaultValue() throws DatabaseException {
    final Entity entity = ENTITIES.entity(T_UUID_TEST_NO_DEFAULT);
    entity.put(UUID_TEST_NO_DEFAULT_DATA, "test");
    connection.insert(entity);
    assertNotNull(entity.get(UUID_TEST_NO_DEFAULT_ID));
    assertEquals("test", entity.get(UUID_TEST_NO_DEFAULT_DATA));
  }

  @Test
  public void entityWithoutPrimaryKey() throws DatabaseException {
    List<Entity> entities = connection.select(condition(T_NO_PK).selectCondition());
    assertEquals(6, entities.size());
    entities = connection.select(condition(NO_PK_COL1).equalTo(2)
                    .or(condition(NO_PK_COL3).equalTo("5")).selectCondition());
    assertEquals(4, entities.size());
  }

  @Test
  public void beans() throws DatabaseException {
    try {
      connection.beginTransaction();

      final List<Entity> departments = connection.select(condition(Department.TYPE).selectCondition());
      Department department = ENTITIES.castTo(Department.TYPE, departments.get(0));
      department.setName("New Name");

      department = ENTITIES.castTo(Department.TYPE, connection.update(department));

      assertEquals("New Name", department.getName());

      List<Department> departmentsCast = ENTITIES.castTo(Department.TYPE, connection.select(condition(Department.TYPE).selectCondition()));

      departmentsCast.forEach(dept -> dept.setName(dept.getName() + "N"));

      departmentsCast = ENTITIES.castTo(Department.TYPE, connection.update(departmentsCast));

      final Department newDept1 = ENTITIES.castTo(Department.TYPE, ENTITIES.entity(Department.TYPE));
      newDept1.setId(-1);
      newDept1.setName("hello1");
      newDept1.setLocation("location");

      final Department newDept2 = ENTITIES.castTo(Department.TYPE, ENTITIES.entity(Department.TYPE));
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
    return new DefaultLocalEntityConnection(DOMAIN, Databases.getInstance(), UNIT_TEST_USER);
  }
}