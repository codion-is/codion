/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db;

import org.jminor.common.db.AbstractFunction;
import org.jminor.common.db.AbstractProcedure;
import org.jminor.common.db.Database;
import org.jminor.common.db.DatabaseConnection;
import org.jminor.common.db.Databases;
import org.jminor.common.db.criteria.Criteria;
import org.jminor.common.db.criteria.SimpleCriteria;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.db.exception.RecordModifiedException;
import org.jminor.common.db.exception.RecordNotFoundException;
import org.jminor.common.model.SearchType;
import org.jminor.common.model.User;
import org.jminor.common.model.reports.ReportResult;
import org.jminor.framework.db.criteria.EntityCriteriaUtil;
import org.jminor.framework.db.criteria.EntitySelectCriteria;
import org.jminor.framework.db.provider.EntityConnectionProvider;
import org.jminor.framework.db.provider.EntityConnectionProviders;
import org.jminor.framework.demos.empdept.domain.EmpDept;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.EntityTestDomain;
import org.jminor.framework.domain.EntityUtil;
import org.jminor.framework.domain.Properties;
import org.jminor.framework.domain.Property;
import org.jminor.framework.plugins.jasperreports.model.JasperReportsWrapper;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.Assert.*;

public class DefaultEntityConnectionTest {

  public static final EntityConnectionProvider CONNECTION_PROVIDER =
          EntityConnectionProviders.createConnectionProvider(User.UNIT_TEST_USER, "JMinor Unit Tests");

  private static final String JOINED_QUERY_ENTITY_ID = "joinedQueryEntityID";

  private static final String ENTITY_ID = "blob_test";
  private static final String ID = "id";
  private static final String DATA = "data";

  private DefaultEntityConnection connection;

  static {
    EmpDept.init();
    Entities.define(JOINED_QUERY_ENTITY_ID,
            Properties.primaryKeyProperty("e.empno"),
            Properties.columnProperty("d.deptno", Types.INTEGER))
            .setSelectQuery("select e.empno, d.deptno from scott.emp e, scott.dept d where e.deptno = d.deptno");

    Entities.define(ENTITY_ID,
            Properties.primaryKeyProperty(ID),
            Properties.columnProperty(DATA, Types.BLOB));
  }

  public DefaultEntityConnectionTest() {
    EntityTestDomain.init();
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
      final Entity.Key key = Entities.key(EmpDept.T_DEPARTMENT);
      key.setValue(EmpDept.DEPARTMENT_ID, 40);
      connection.delete(new ArrayList<Entity.Key>());
      connection.delete(Arrays.asList(key));
      try {
        connection.selectSingle(key);
        fail();
      }
      catch (final DatabaseException ignored) {}
    }
    finally {
      connection.rollbackTransaction();
    }
    try {
      connection.beginTransaction();
      final Entity.Key key = Entities.key(EmpDept.T_DEPARTMENT);
      key.setValue(EmpDept.DEPARTMENT_ID, 40);
      connection.delete(EntityCriteriaUtil.criteria(key));
      try {
        connection.selectSingle(key);
        fail();
      }
      catch (final DatabaseException ignored) {}
    }
    finally {
      connection.rollbackTransaction();
    }
  }

  @Test
  public void fillReport() throws Exception {
    final Map<String, Object> reportParameters = new HashMap<>();
    reportParameters.put("DEPTNO", Arrays.asList(10, 20));
    final ReportResult print = connection.fillReport(
            new JasperReportsWrapper("resources/demos/empdept/reports/empdept_employees.jasper", reportParameters));
    assertNotNull(print.getResult());
  }

  @Test
  public void selectAll() throws Exception {
    final List<Entity> depts = connection.selectAll(EmpDept.T_DEPARTMENT);
    assertEquals(4, depts.size());
    final List<Entity> emps = connection.selectAll(JOINED_QUERY_ENTITY_ID);
    assertEquals(16, emps.size());
  }

  @Test
  public void selectDependentEntities() throws Exception {
    final Map<String, Collection<Entity>> empty = connection.selectDependentEntities(new ArrayList<Entity>());
    assertTrue(empty.isEmpty());
    final List<Entity> accounting = connection.selectMany(EmpDept.T_DEPARTMENT, EmpDept.DEPARTMENT_NAME, "ACCOUNTING");
    final Map<String, Collection<Entity>> emps = connection.selectDependentEntities(accounting);
    assertEquals(1, emps.size());
    assertTrue(emps.containsKey(EmpDept.T_EMPLOYEE));
    assertEquals(7, emps.get(EmpDept.T_EMPLOYEE).size());

    Entity emp = connection.selectSingle(EmpDept.T_EMPLOYEE, EmpDept.EMPLOYEE_NAME, "KING");
    Map<String, Collection<Entity>> deps = connection.selectDependentEntities(Arrays.asList(emp));
    assertTrue(deps.containsKey(EmpDept.T_EMPLOYEE));
    assertEquals(3, deps.get(EmpDept.T_EMPLOYEE).size());

    emp = connection.selectSingle(EmpDept.T_EMPLOYEE, EmpDept.EMPLOYEE_NAME, "MILLER");
    deps = connection.selectDependentEntities(Arrays.asList(emp));
    assertFalse(deps.containsKey(EmpDept.T_EMPLOYEE));
  }

  @Test
  public void selectManyLimitOffset() throws Exception {
    final EntitySelectCriteria criteria = EntityCriteriaUtil.selectCriteria(EmpDept.T_EMPLOYEE)
            .setOrderByClause(EmpDept.EMPLOYEE_NAME).setLimit(2);
    List<Entity> result = connection.selectMany(criteria);
    assertEquals(2, result.size());
    criteria.setLimit(3);
    criteria.setOffset(3);
    result = connection.selectMany(criteria);
    assertEquals(3, result.size());
    assertEquals("BLAKE", result.get(0).getValue(EmpDept.EMPLOYEE_NAME));
    assertEquals("CLARK", result.get(1).getValue(EmpDept.EMPLOYEE_NAME));
    assertEquals("FORD", result.get(2).getValue(EmpDept.EMPLOYEE_NAME));
  }

  @Test
  public void selectMany() throws Exception {
    List<Entity> result = connection.selectMany(new ArrayList<Entity.Key>());
    assertTrue(result.isEmpty());
    result = connection.selectMany(EmpDept.T_DEPARTMENT, EmpDept.DEPARTMENT_ID, 10, 20);
    assertEquals(2, result.size());
    result = connection.selectMany(EntityUtil.getPrimaryKeys(result));
    assertEquals(2, result.size());
    result = connection.selectMany(EntityCriteriaUtil.selectCriteria(EmpDept.T_DEPARTMENT, new SimpleCriteria<Property.ColumnProperty>("deptno in (10, 20)")));
    assertEquals(2, result.size());
    result = connection.selectMany(EntityCriteriaUtil.selectCriteria(JOINED_QUERY_ENTITY_ID, new SimpleCriteria<Property.ColumnProperty>("d.deptno = 10")));
    assertEquals(7, result.size());

    final EntitySelectCriteria criteria = EntityCriteriaUtil.selectCriteria(EmpDept.T_EMPLOYEE, new SimpleCriteria<Property.ColumnProperty>("ename = 'BLAKE'"));
    result = connection.selectMany(criteria);
    Entity emp = result.get(0);
    assertTrue(emp.isLoaded(EmpDept.EMPLOYEE_DEPARTMENT_FK));
    assertTrue(emp.isLoaded(EmpDept.EMPLOYEE_MGR_FK));
    emp = emp.getForeignKeyValue(EmpDept.EMPLOYEE_MGR_FK);
    assertFalse(emp.isLoaded(EmpDept.EMPLOYEE_MGR_FK));

    result = connection.selectMany(criteria.setForeignKeyFetchDepthLimit(EmpDept.EMPLOYEE_DEPARTMENT_FK, 0));
    assertEquals(1, result.size());
    emp = result.get(0);
    assertFalse(emp.isLoaded(EmpDept.EMPLOYEE_DEPARTMENT_FK));
    assertTrue(emp.isLoaded(EmpDept.EMPLOYEE_MGR_FK));

    result = connection.selectMany(criteria.setForeignKeyFetchDepthLimit(EmpDept.EMPLOYEE_MGR_FK, 0));
    assertEquals(1, result.size());
    emp = result.get(0);
    assertFalse(emp.isLoaded(EmpDept.EMPLOYEE_DEPARTMENT_FK));
    assertFalse(emp.isLoaded(EmpDept.EMPLOYEE_MGR_FK));

    result = connection.selectMany(criteria.setForeignKeyFetchDepthLimit(EmpDept.EMPLOYEE_MGR_FK, 2));
    assertEquals(1, result.size());
    emp = result.get(0);
    assertFalse(emp.isLoaded(EmpDept.EMPLOYEE_DEPARTMENT_FK));
    assertTrue(emp.isLoaded(EmpDept.EMPLOYEE_MGR_FK));
    emp = emp.getForeignKeyValue(EmpDept.EMPLOYEE_MGR_FK);
    assertTrue(emp.isLoaded(EmpDept.EMPLOYEE_MGR_FK));
  }

  @Test(expected = DatabaseException.class)
  public void selectManyInvalidColumn() throws Exception {
    connection.selectMany(EntityCriteriaUtil.selectCriteria(EmpDept.T_DEPARTMENT, new SimpleCriteria<Property.ColumnProperty>("no_column is null")));
  }

  @Test
  public void selectRowCount() throws Exception {
    int rowCount = connection.selectRowCount(EntityCriteriaUtil.criteria(EmpDept.T_DEPARTMENT));
    assertEquals(4, rowCount);
    Criteria<Property.ColumnProperty> deptNoCriteria = EntityCriteriaUtil.propertyCriteria(EmpDept.T_DEPARTMENT,
            EmpDept.DEPARTMENT_ID, SearchType.GREATER_THAN, 30);
    rowCount = connection.selectRowCount(EntityCriteriaUtil.criteria(EmpDept.T_DEPARTMENT, deptNoCriteria));
    assertEquals(2, rowCount);

    rowCount = connection.selectRowCount(EntityCriteriaUtil.criteria(JOINED_QUERY_ENTITY_ID));
    assertEquals(16, rowCount);
    deptNoCriteria = EntityCriteriaUtil.propertyCriteria(JOINED_QUERY_ENTITY_ID, "d.deptno", SearchType.GREATER_THAN, 30);
    rowCount = connection.selectRowCount(EntityCriteriaUtil.criteria(JOINED_QUERY_ENTITY_ID, deptNoCriteria));
    assertEquals(4, rowCount);
  }

  @Test
  public void selectSingle() throws Exception {
    Entity sales = connection.selectSingle(EmpDept.T_DEPARTMENT, EmpDept.DEPARTMENT_NAME, "SALES");
    assertEquals(sales.getStringValue(EmpDept.DEPARTMENT_NAME), "SALES");
    sales = connection.selectSingle(sales.getPrimaryKey());
    assertEquals(sales.getStringValue(EmpDept.DEPARTMENT_NAME), "SALES");
    sales = connection.selectSingle(EntityCriteriaUtil.selectCriteria(EmpDept.T_DEPARTMENT, new SimpleCriteria<Property.ColumnProperty>("dname = 'SALES'")));
    assertEquals(sales.getStringValue(EmpDept.DEPARTMENT_NAME), "SALES");

    final Entity king = connection.selectSingle(EmpDept.T_EMPLOYEE, EmpDept.EMPLOYEE_NAME, "KING");
    assertTrue(king.containsValue(EmpDept.EMPLOYEE_MGR_FK));
    assertNull(king.getValue(EmpDept.EMPLOYEE_MGR_FK));
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
    connection.selectSingle(EmpDept.T_DEPARTMENT, EmpDept.DEPARTMENT_NAME, "NO_NAME");
  }

  @Test(expected = DatabaseException.class)
  public void selectSingleManyFound() throws Exception {
    connection.selectSingle(EmpDept.T_EMPLOYEE, EmpDept.EMPLOYEE_JOB, "MANAGER");
  }

  @Test(expected = DatabaseException.class)
  public void insertOnlyNullValues() throws DatabaseException {
    try {
      connection.beginTransaction();
      final Entity department = Entities.entity(EmpDept.T_DEPARTMENT);
      connection.insert(Arrays.asList(department));
    }
    finally {
      connection.rollbackTransaction();
    }
  }

  @Test(expected = DatabaseException.class)
  public void updateNoModifiedValues() throws DatabaseException {
    try {
      connection.beginTransaction();
      final Entity department = connection.selectSingle(EmpDept.T_DEPARTMENT, EmpDept.DEPARTMENT_ID, 10);
      connection.update(Arrays.asList(department));
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
  public void selectPropertyValuesNonColumnProperty() throws Exception {
    connection.selectPropertyValues(EmpDept.T_EMPLOYEE, EmpDept.EMPLOYEE_DEPARTMENT_LOCATION, false);
  }

  @Test
  public void selectPropertyValues() throws Exception {
    final List<Object> result = connection.selectPropertyValues(EmpDept.T_DEPARTMENT, EmpDept.DEPARTMENT_NAME, false);
    assertTrue(result.contains("ACCOUNTING"));
    assertTrue(result.contains("SALES"));
    assertTrue(result.contains("RESEARCH"));
    assertTrue(result.contains("OPERATIONS"));
  }

  @Test
  public void selectForUpdateModified() throws Exception {
    final EntityConnection connection = initializeConnection();
    final EntityConnection connection2 = initializeConnection();
    final String originalLocation;
    try {
      final EntitySelectCriteria criteria = EntityCriteriaUtil.selectCriteria(EmpDept.T_DEPARTMENT, EmpDept.DEPARTMENT_NAME, SearchType.LIKE, "SALES");
      criteria.setForUpdate(true);

      Entity sales = connection.selectSingle(criteria);
      originalLocation = sales.getStringValue(EmpDept.DEPARTMENT_LOCATION);

      sales.setValue(EmpDept.DEPARTMENT_LOCATION, "Syracuse");
      try {
        connection2.update(Arrays.asList(sales));
        fail("Should not be able to update record selected for update by another connection");
      }
      catch (final DatabaseException ignored) {}

      connection.selectAll(EmpDept.T_DEPARTMENT);//any query will do

      try {
        sales = connection2.update(Arrays.asList(sales)).get(0);
        sales.setValue(EmpDept.DEPARTMENT_LOCATION, originalLocation);
        connection2.update(Arrays.asList(sales));//revert changes to data
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
    final DefaultEntityConnection connection = initializeConnection();
    final EntityConnection connection2 = initializeConnection();
    connection.setOptimisticLocking(true);
    final Entity allen;
    try {
      final EntitySelectCriteria criteria = EntityCriteriaUtil.selectCriteria(EmpDept.T_EMPLOYEE, EmpDept.EMPLOYEE_NAME, SearchType.LIKE, "ALLEN");

      allen = connection.selectSingle(criteria);

      connection2.delete(Arrays.asList(allen.getPrimaryKey()));

      allen.setValue(EmpDept.EMPLOYEE_JOB, "A JOB");
      try {
        connection.update(Arrays.asList(allen));
        fail("Should not be able to update record deleted by another connection");
      }
      catch (final RecordModifiedException e) {
        assertNotNull(e.getRow());
        assertNull(e.getModifiedRow());
      }

      try {
        connection2.insert(Arrays.asList(allen));//revert changes to data
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
    final DefaultEntityConnection baseConnection = initializeConnection();
    final DefaultEntityConnection optimisticConnection = initializeConnection();
    optimisticConnection.setOptimisticLocking(true);
    assertTrue(optimisticConnection.isOptimisticLocking());
    String oldLocation = null;
    Entity updatedDepartment = null;
    try {
      final Entity department = baseConnection.selectSingle(EmpDept.T_DEPARTMENT, EmpDept.DEPARTMENT_NAME, "SALES");
      oldLocation = (String) department.setValue(EmpDept.DEPARTMENT_LOCATION, "NEWLOC");
      updatedDepartment = baseConnection.update(Arrays.asList(department)).get(0);
      try {
        optimisticConnection.update(Arrays.asList(department));
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
          updatedDepartment.setValue(EmpDept.DEPARTMENT_LOCATION, oldLocation);
          baseConnection.update(Arrays.asList(updatedDepartment));
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
      final Database db = Databases.createInstance();
      connection = db.createConnection(User.UNIT_TEST_USER);
      final EntityConnection conn = new DefaultEntityConnection(db, connection);
      assertTrue(conn.isConnected());
      assertTrue(conn.isValid());
    }
    finally {
      if (connection != null) {
        try {
          connection.close();
        }
        catch (final Exception ignored) {}
      }
    }
  }

  @Test (expected = IllegalArgumentException.class)
  public void testConstructorInvalidConnection() throws Exception {
    Connection connection = null;
    try {
      final Database db = Databases.createInstance();
      connection = db.createConnection(User.UNIT_TEST_USER);
      connection.close();
      new DefaultEntityConnection(db, connection);
    }
    finally {
      if (connection != null) {
        try {
          connection.close();
        }
        catch (final Exception ignored) {}
      }
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void writeBlobIncorrectType() throws DatabaseException {
    connection.writeBlob(Entities.key(EmpDept.T_DEPARTMENT), EmpDept.DEPARTMENT_NAME, new byte[0]);
  }

  @Test(expected = IllegalArgumentException.class)
  public void readBlobIncorrectType() throws DatabaseException {
    connection.readBlob(Entities.key(EmpDept.T_DEPARTMENT), EmpDept.DEPARTMENT_NAME);
  }

  @Test
  public void readWriteBlob() throws SQLException, DatabaseException {
    DatabaseConnection databaseConnection = null;
    Statement statement = null;
    try {
      databaseConnection = connection.getDatabaseConnection();
      statement = databaseConnection.getConnection().createStatement();
      statement.execute("create table blob_test(id integer, data blob)");

      final Entity blobRecord = Entities.entity(ENTITY_ID);
      blobRecord.setValue(ID, 1);

      final Entity.Key blobRecordKey = connection.insert(Arrays.asList(blobRecord)).get(0);

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
        statement = databaseConnection.getConnection().createStatement();
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
      statement = databaseConnection.getConnection().createStatement();
      statement.execute("create table blob_test(id integer, data blob)");

      final byte[] bytes = new byte[1024];
      new Random().nextBytes(bytes);

      final Entity blobRecord = Entities.entity(ENTITY_ID);
      blobRecord.setValue(ID, 1);
      blobRecord.setValue("data", bytes);

      final Entity.Key blobRecordKey = connection.insert(Arrays.asList(blobRecord)).get(0);

      byte[] fromDb = connection.readBlob(blobRecordKey, DATA);
      assertTrue(Arrays.equals(bytes, fromDb));

      final Entity blobRecordFromDb = connection.selectSingle(blobRecordKey);
      assertNotNull(blobRecordFromDb);
      assertNull(blobRecordFromDb.getValue(DATA));

      final byte[] newBytes = new byte[2048];
      new Random().nextBytes(newBytes);

      blobRecord.setValue("data", newBytes);

      connection.update(Arrays.asList(blobRecord)).get(0);

      fromDb = connection.readBlob(blobRecordKey, DATA);
      assertTrue(Arrays.equals(newBytes, fromDb));
    }
    finally {
      if (statement != null) {
        statement.close();
      }
      if (databaseConnection != null) {
        statement = databaseConnection.getConnection().createStatement();
        statement.execute("drop table blob_test");
        statement.close();
      }
    }
  }

  private static DefaultEntityConnection initializeConnection() throws DatabaseException {
    return new DefaultEntityConnection(Databases.createInstance(), User.UNIT_TEST_USER);
  }
}