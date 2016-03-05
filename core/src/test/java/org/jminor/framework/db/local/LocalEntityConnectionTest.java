/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.local;

import org.jminor.common.db.AbstractFunction;
import org.jminor.common.db.AbstractProcedure;
import org.jminor.common.db.Database;
import org.jminor.common.db.DatabaseConnection;
import org.jminor.common.db.Databases;
import org.jminor.common.db.DatabasesTest;
import org.jminor.common.db.criteria.Criteria;
import org.jminor.common.db.criteria.CriteriaUtil;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.db.exception.RecordModifiedException;
import org.jminor.common.db.exception.RecordNotFoundException;
import org.jminor.common.model.SearchType;
import org.jminor.common.model.User;
import org.jminor.common.model.reports.ReportDataWrapper;
import org.jminor.common.model.reports.ReportException;
import org.jminor.common.model.reports.ReportResult;
import org.jminor.common.model.reports.ReportWrapper;
import org.jminor.framework.db.EntityConnection;
import org.jminor.framework.db.criteria.EntityCriteriaUtil;
import org.jminor.framework.db.criteria.EntitySelectCriteria;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.EntityUtil;
import org.jminor.framework.domain.Properties;
import org.jminor.framework.domain.Property;
import org.jminor.framework.domain.TestDomain;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.Assert.*;

public class LocalEntityConnectionTest {

  private static final String JOINED_QUERY_ENTITY_ID = "joinedQueryEntityID";

  private static final String ENTITY_ID = "blob_test";
  private static final String ID = "id";
  private static final String DATA = "data";

  private LocalEntityConnection connection;

  static {
    TestDomain.init();
    Entities.define(JOINED_QUERY_ENTITY_ID,
            Properties.primaryKeyProperty("e.empno"),
            Properties.columnProperty("d.deptno", Types.INTEGER))
            .setSelectQuery("select e.empno, d.deptno from scott.emp e, scott.dept d where e.deptno = d.deptno");

    Entities.define(ENTITY_ID,
            Properties.primaryKeyProperty(ID),
            Properties.columnProperty(DATA, Types.BLOB));
  }

  public LocalEntityConnectionTest() {
    TestDomain.init();
  }

  @Before
  public void setup() throws ClassNotFoundException, DatabaseException {
    connection = initializeConnection();
  }

  @After
  public void tearDown() {
    connection.disconnect();
  }

  @Test
  public void delete() throws Exception {
    try {
      connection.beginTransaction();
      final Entity.Key key = Entities.key(TestDomain.T_DEPARTMENT);
      key.setValue(TestDomain.DEPARTMENT_ID, 40);
      connection.delete(new ArrayList<Entity.Key>());
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
      final Entity.Key key = Entities.key(TestDomain.T_DEPARTMENT);
      key.setValue(TestDomain.DEPARTMENT_ID, 40);
      connection.delete(EntityCriteriaUtil.criteria(key));
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
  public void fillReport() throws Exception {
    final Map<String, Object> reportParameters = new HashMap<>();
    reportParameters.put("DEPTNO", Arrays.asList(10, 20));
    final ReportResult reportResult = new ReportResult() {
      @Override
      public Object getResult() {
        return "result";
      }
    };
    final ReportResult print = connection.fillReport(new ReportWrapper() {
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
    final Map<String, Collection<Entity>> empty = connection.selectDependentEntities(new ArrayList<Entity>());
    assertTrue(empty.isEmpty());
    final List<Entity> accounting = connection.selectMany(TestDomain.T_DEPARTMENT, TestDomain.DEPARTMENT_NAME, "ACCOUNTING");
    final Map<String, Collection<Entity>> emps = connection.selectDependentEntities(accounting);
    assertEquals(1, emps.size());
    assertTrue(emps.containsKey(TestDomain.T_EMP));
    assertEquals(7, emps.get(TestDomain.T_EMP).size());

    Entity emp = connection.selectSingle(TestDomain.T_EMP, TestDomain.EMP_NAME, "KING");
    Map<String, Collection<Entity>> deps = connection.selectDependentEntities(Collections.singletonList(emp));
    assertTrue(deps.containsKey(TestDomain.T_EMP));
    assertEquals(3, deps.get(TestDomain.T_EMP).size());

    emp = connection.selectSingle(TestDomain.T_EMP, TestDomain.EMP_NAME, "MILLER");
    deps = connection.selectDependentEntities(Collections.singletonList(emp));
    assertFalse(deps.containsKey(TestDomain.T_EMP));
  }

  @Test
  public void selectManyLimitOffset() throws Exception {
    final EntitySelectCriteria criteria = EntityCriteriaUtil.selectCriteria(TestDomain.T_EMP)
            .setOrderByClause(TestDomain.EMP_NAME).setLimit(2);
    List<Entity> result = connection.selectMany(criteria);
    assertEquals(2, result.size());
    criteria.setLimit(3);
    criteria.setOffset(3);
    result = connection.selectMany(criteria);
    assertEquals(3, result.size());
    assertEquals("BLAKE", result.get(0).getValue(TestDomain.EMP_NAME));
    assertEquals("CLARK", result.get(1).getValue(TestDomain.EMP_NAME));
    assertEquals("FORD", result.get(2).getValue(TestDomain.EMP_NAME));
  }

  @Test
  public void selectMany() throws Exception {
    List<Entity> result = connection.selectMany(new ArrayList<Entity.Key>());
    assertTrue(result.isEmpty());
    result = connection.selectMany(TestDomain.T_DEPARTMENT, TestDomain.DEPARTMENT_ID, 10, 20);
    assertEquals(2, result.size());
    result = connection.selectMany(EntityUtil.getPrimaryKeys(result));
    assertEquals(2, result.size());
    result = connection.selectMany(EntityCriteriaUtil.selectCriteria(TestDomain.T_DEPARTMENT, CriteriaUtil.<Property.ColumnProperty>stringCriteria("deptno in (10, 20)")));
    assertEquals(2, result.size());
    result = connection.selectMany(EntityCriteriaUtil.selectCriteria(JOINED_QUERY_ENTITY_ID, CriteriaUtil.<Property.ColumnProperty>stringCriteria("d.deptno = 10")));
    assertEquals(7, result.size());

    final EntitySelectCriteria criteria = EntityCriteriaUtil.selectCriteria(TestDomain.T_EMP, CriteriaUtil.<Property.ColumnProperty>stringCriteria("ename = 'BLAKE'"));
    result = connection.selectMany(criteria);
    Entity emp = result.get(0);
    assertTrue(emp.isLoaded(TestDomain.EMP_DEPARTMENT_FK));
    assertTrue(emp.isLoaded(TestDomain.EMP_MGR_FK));
    emp = emp.getForeignKeyValue(TestDomain.EMP_MGR_FK);
    assertFalse(emp.isLoaded(TestDomain.EMP_MGR_FK));

    result = connection.selectMany(criteria.setForeignKeyFetchDepthLimit(TestDomain.EMP_DEPARTMENT_FK, 0));
    assertEquals(1, result.size());
    emp = result.get(0);
    assertFalse(emp.isLoaded(TestDomain.EMP_DEPARTMENT_FK));
    assertTrue(emp.isLoaded(TestDomain.EMP_MGR_FK));

    result = connection.selectMany(criteria.setForeignKeyFetchDepthLimit(TestDomain.EMP_MGR_FK, 0));
    assertEquals(1, result.size());
    emp = result.get(0);
    assertFalse(emp.isLoaded(TestDomain.EMP_DEPARTMENT_FK));
    assertFalse(emp.isLoaded(TestDomain.EMP_MGR_FK));

    result = connection.selectMany(criteria.setForeignKeyFetchDepthLimit(TestDomain.EMP_MGR_FK, 2));
    assertEquals(1, result.size());
    emp = result.get(0);
    assertFalse(emp.isLoaded(TestDomain.EMP_DEPARTMENT_FK));
    assertTrue(emp.isLoaded(TestDomain.EMP_MGR_FK));
    emp = emp.getForeignKeyValue(TestDomain.EMP_MGR_FK);
    assertTrue(emp.isLoaded(TestDomain.EMP_MGR_FK));
  }

  @Test(expected = DatabaseException.class)
  public void selectManyInvalidColumn() throws Exception {
    connection.selectMany(EntityCriteriaUtil.selectCriteria(TestDomain.T_DEPARTMENT,
            CriteriaUtil.<Property.ColumnProperty>stringCriteria("no_column is null")));
  }

  @Test
  public void selectRowCount() throws Exception {
    int rowCount = connection.selectRowCount(EntityCriteriaUtil.criteria(TestDomain.T_DEPARTMENT));
    assertEquals(4, rowCount);
    Criteria<Property.ColumnProperty> deptNoCriteria = EntityCriteriaUtil.propertyCriteria(TestDomain.T_DEPARTMENT,
            TestDomain.DEPARTMENT_ID, SearchType.GREATER_THAN, 30);
    rowCount = connection.selectRowCount(EntityCriteriaUtil.criteria(TestDomain.T_DEPARTMENT, deptNoCriteria));
    assertEquals(2, rowCount);

    rowCount = connection.selectRowCount(EntityCriteriaUtil.criteria(JOINED_QUERY_ENTITY_ID));
    assertEquals(16, rowCount);
    deptNoCriteria = EntityCriteriaUtil.propertyCriteria(JOINED_QUERY_ENTITY_ID, "d.deptno", SearchType.GREATER_THAN, 30);
    rowCount = connection.selectRowCount(EntityCriteriaUtil.criteria(JOINED_QUERY_ENTITY_ID, deptNoCriteria));
    assertEquals(4, rowCount);
  }

  @Test
  public void selectSingle() throws Exception {
    Entity sales = connection.selectSingle(TestDomain.T_DEPARTMENT, TestDomain.DEPARTMENT_NAME, "SALES");
    assertEquals(sales.getStringValue(TestDomain.DEPARTMENT_NAME), "SALES");
    sales = connection.selectSingle(sales.getPrimaryKey());
    assertEquals(sales.getStringValue(TestDomain.DEPARTMENT_NAME), "SALES");
    sales = connection.selectSingle(EntityCriteriaUtil.selectCriteria(TestDomain.T_DEPARTMENT, CriteriaUtil.<Property.ColumnProperty>stringCriteria("dname = 'SALES'")));
    assertEquals(sales.getStringValue(TestDomain.DEPARTMENT_NAME), "SALES");

    final Entity king = connection.selectSingle(TestDomain.T_EMP, TestDomain.EMP_NAME, "KING");
    assertTrue(king.containsValue(TestDomain.EMP_MGR_FK));
    assertNull(king.getValue(TestDomain.EMP_MGR_FK));
  }

  @Test
  public void executeFunction() throws DatabaseException {
    final DatabaseConnection.Function func = new AbstractFunction<EntityConnection>("executeFunction", "executeFunction") {
      @Override
      public List execute(final EntityConnection connection, final Object... arguments) {
        return null;
      }
    };
    Databases.addOperation(func);
    connection.executeFunction(func.getID());
  }

  @Test
  public void executeProcedure() throws DatabaseException {
    final DatabaseConnection.Procedure proc = new AbstractProcedure<EntityConnection>("executeProcedure", "executeProcedure") {
      @Override
      public void execute(final EntityConnection connection, final Object... arguments) {}
    };
    Databases.addOperation(proc);
    connection.executeProcedure(proc.getID());
  }

  @Test(expected = RecordNotFoundException.class)
  public void selectSingleNotFound() throws Exception {
    connection.selectSingle(TestDomain.T_DEPARTMENT, TestDomain.DEPARTMENT_NAME, "NO_NAME");
  }

  @Test(expected = DatabaseException.class)
  public void selectSingleManyFound() throws Exception {
    connection.selectSingle(TestDomain.T_EMP, TestDomain.EMP_JOB, "MANAGER");
  }

  @Test(expected = DatabaseException.class)
  public void insertOnlyNullValues() throws DatabaseException {
    try {
      connection.beginTransaction();
      final Entity department = Entities.entity(TestDomain.T_DEPARTMENT);
      connection.insert(Collections.singletonList(department));
    }
    finally {
      connection.rollbackTransaction();
    }
  }

  @Test(expected = DatabaseException.class)
  public void updateNoModifiedValues() throws DatabaseException {
    try {
      connection.beginTransaction();
      final Entity department = connection.selectSingle(TestDomain.T_DEPARTMENT, TestDomain.DEPARTMENT_ID, 10);
      connection.update(Collections.singletonList(department));
    }
    finally {
      connection.rollbackTransaction();
    }
  }

  @Test
  public void insert() throws DatabaseException {
    final List<Entity.Key> pks = connection.insert(new ArrayList<Entity>());
    assertTrue(pks.isEmpty());
  }

  @Test
  public void update() throws DatabaseException {
    final List<Entity> updated = connection.update(new ArrayList<Entity>());
    assertTrue(updated.isEmpty());
  }

  @Test(expected = IllegalArgumentException.class)
  public void selectValuesNonColumnProperty() throws Exception {
    connection.selectValues(TestDomain.EMP_DEPARTMENT_LOCATION, EntityCriteriaUtil.criteria(TestDomain.T_EMP));
  }

  @Test
  public void selectValues() throws Exception {
    List<Object> result = connection.selectValues(TestDomain.DEPARTMENT_NAME, EntityCriteriaUtil.criteria(TestDomain.T_DEPARTMENT));
    assertEquals("ACCOUNTING", result.get(0));
    assertEquals("OPERATIONS", result.get(1));
    assertEquals("RESEARCH", result.get(2));
    assertEquals("SALES", result.get(3));

    result = connection.selectValues(TestDomain.DEPARTMENT_NAME, EntityCriteriaUtil.criteria(TestDomain.T_DEPARTMENT,
            TestDomain.DEPARTMENT_ID, SearchType.LIKE, 10));
    assertTrue(result.contains("ACCOUNTING"));
    assertFalse(result.contains("SALES"));
  }

  @Test
  public void selectForUpdateModified() throws Exception {
    final EntityConnection connection = initializeConnection();
    final EntityConnection connection2 = initializeConnection();
    final String originalLocation;
    try {
      final EntitySelectCriteria criteria = EntityCriteriaUtil.selectCriteria(TestDomain.T_DEPARTMENT, TestDomain.DEPARTMENT_NAME, SearchType.LIKE, "SALES");
      criteria.setForUpdate(true);

      Entity sales = connection.selectSingle(criteria);
      originalLocation = sales.getStringValue(TestDomain.DEPARTMENT_LOCATION);

      sales.setValue(TestDomain.DEPARTMENT_LOCATION, "Syracuse");
      try {
        connection2.update(Collections.singletonList(sales));
        fail("Should not be able to update record selected for update by another connection");
      }
      catch (final DatabaseException ignored) {
        connection2.getDatabaseConnection().rollback();
      }

      connection.selectMany(EntityCriteriaUtil.selectCriteria(TestDomain.T_DEPARTMENT));//any query will do

      try {
        sales = connection2.update(Collections.singletonList(sales)).get(0);
        sales.setValue(TestDomain.DEPARTMENT_LOCATION, originalLocation);
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
    final LocalEntityConnection connection = initializeConnection();
    final EntityConnection connection2 = initializeConnection();
    connection.setOptimisticLocking(true);
    final Entity allen;
    try {
      final EntitySelectCriteria criteria = EntityCriteriaUtil.selectCriteria(TestDomain.T_EMP, TestDomain.EMP_NAME, SearchType.LIKE, "ALLEN");

      allen = connection.selectSingle(criteria);

      connection2.delete(Collections.singletonList(allen.getPrimaryKey()));

      allen.setValue(TestDomain.EMP_JOB, "A JOB");
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
    final LocalEntityConnection baseConnection = initializeConnection();
    final LocalEntityConnection optimisticConnection = initializeConnection();
    optimisticConnection.setOptimisticLocking(true);
    assertTrue(optimisticConnection.isOptimisticLocking());
    String oldLocation = null;
    Entity updatedDepartment = null;
    try {
      final Entity department = baseConnection.selectSingle(TestDomain.T_DEPARTMENT, TestDomain.DEPARTMENT_NAME, "SALES");
      oldLocation = (String) department.setValue(TestDomain.DEPARTMENT_LOCATION, "NEWLOC");
      updatedDepartment = baseConnection.update(Collections.singletonList(department)).get(0);
      try {
        optimisticConnection.update(Collections.singletonList(department));
        fail("RecordModifiedException should have been thrown");
      }
      catch (final RecordModifiedException e) {
        assertTrue(((Entity) e.getModifiedRow()).propertyValuesEqual(updatedDepartment));
        assertTrue(((Entity) e.getRow()).propertyValuesEqual(department));
      }
    }
    finally {
      try {
        if (updatedDepartment != null && oldLocation != null) {
          updatedDepartment.setValue(TestDomain.DEPARTMENT_LOCATION, oldLocation);
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
  public void testConstructor() throws Exception {
    Connection connection = null;
    try {
      final Database db = DatabasesTest.createTestDatabaseInstance();
      connection = db.createConnection(User.UNIT_TEST_USER);
      final EntityConnection conn = new LocalEntityConnection(db, connection, true, true, 1);
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

  @Test (expected = IllegalArgumentException.class)
  public void testConstructorInvalidConnection() throws Exception {
    Connection connection = null;
    try {
      final Database db = DatabasesTest.createTestDatabaseInstance();
      connection = db.createConnection(User.UNIT_TEST_USER);
      connection.close();
      new LocalEntityConnection(db, connection, true, true, 1);
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

  @Test(expected = IllegalArgumentException.class)
  public void writeBlobIncorrectType() throws DatabaseException {
    connection.writeBlob(Entities.key(TestDomain.T_DEPARTMENT), TestDomain.DEPARTMENT_NAME, new byte[0]);
  }

  @Test(expected = IllegalArgumentException.class)
  public void readBlobIncorrectType() throws DatabaseException {
    connection.readBlob(Entities.key(TestDomain.T_DEPARTMENT), TestDomain.DEPARTMENT_NAME);
  }

  @Test
  public void readWriteBlob() throws SQLException, DatabaseException {
    DatabaseConnection databaseConnection = null;
    Statement statement = null;
    try {
      databaseConnection = connection.getDatabaseConnection();
      statement = databaseConnection.getConnection(false).createStatement();
      statement.execute("create table blob_test(id integer, data blob)");

      final Entity blobRecord = Entities.entity(ENTITY_ID);
      blobRecord.setValue(ID, 1);

      final Entity.Key blobRecordKey = connection.insert(Collections.singletonList(blobRecord)).get(0);

      final byte one = 1;
      final byte[] bytes = new byte[1024];
      Arrays.fill(bytes, one);

      connection.writeBlob(blobRecordKey, DATA, bytes);

      final byte[] fromDb = connection.readBlob(blobRecordKey, DATA);
      assertEquals(bytes.length, fromDb.length);

      final Entity blobRecordFromDb = connection.selectSingle(blobRecordKey);
      assertNotNull(blobRecordFromDb);
      assertNull(blobRecordFromDb.getValue(DATA));
    }
    finally {
      if (statement != null) {
        statement.close();
      }
      if (databaseConnection != null) {
        statement = databaseConnection.getConnection(false).createStatement();
        statement.execute("drop table blob_test");
        statement.close();
      }
    }
  }

  @Test
  public void readWriteBlobViaEntity() throws SQLException, DatabaseException {
    DatabaseConnection databaseConnection = null;
    Statement statement = null;
    try {
      databaseConnection = connection.getDatabaseConnection();
      statement = databaseConnection.getConnection(false).createStatement();
      statement.execute("create table blob_test(id integer, data blob)");

      final byte[] bytes = new byte[1024];
      new Random().nextBytes(bytes);

      final Entity blobRecord = Entities.entity(ENTITY_ID);
      blobRecord.setValue(ID, 1);
      blobRecord.setValue("data", bytes);

      final Entity.Key blobRecordKey = connection.insert(Collections.singletonList(blobRecord)).get(0);

      byte[] fromDb = connection.readBlob(blobRecordKey, DATA);
      assertTrue(Arrays.equals(bytes, fromDb));

      final Entity blobRecordFromDb = connection.selectSingle(blobRecordKey);
      assertNotNull(blobRecordFromDb);
      assertNull(blobRecordFromDb.getValue(DATA));

      final byte[] newBytes = new byte[2048];
      new Random().nextBytes(newBytes);

      blobRecord.setValue("data", newBytes);

      connection.update(Collections.singletonList(blobRecord)).get(0);

      fromDb = connection.readBlob(blobRecordKey, DATA);
      assertTrue(Arrays.equals(newBytes, fromDb));
    }
    finally {
      if (statement != null) {
        statement.close();
      }
      if (databaseConnection != null) {
        statement = databaseConnection.getConnection(false).createStatement();
        statement.execute("drop table blob_test");
        statement.close();
      }
    }
  }

  private static LocalEntityConnection initializeConnection() throws DatabaseException {
    return new LocalEntityConnection(DatabasesTest.createTestDatabaseInstance(), User.UNIT_TEST_USER, true, true, 1);
  }
}