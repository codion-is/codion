/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db;

import org.jminor.common.db.Databases;
import org.jminor.common.db.criteria.SimpleCriteria;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.db.exception.RecordModifiedException;
import org.jminor.common.db.exception.RecordNotFoundException;
import org.jminor.common.model.User;
import org.jminor.common.model.reports.ReportResult;
import org.jminor.framework.db.criteria.EntityCriteriaUtil;
import org.jminor.framework.db.criteria.EntitySelectCriteria;
import org.jminor.framework.db.provider.EntityConnectionProvider;
import org.jminor.framework.db.provider.EntityConnectionProviders;
import org.jminor.framework.demos.empdept.domain.EmpDept;
import org.jminor.framework.demos.petstore.domain.Petstore;
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

import java.sql.SQLException;
import java.sql.Types;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class EntityConnectionImplTest {

  public static final EntityConnectionProvider DB_PROVIDER =
          EntityConnectionProviders.createConnectionProvider(User.UNIT_TEST_USER, "JMinor Unit Tests");

  public static final String COMBINED_ENTITY_ID = "selectQueryEntityID";

  private EntityConnectionImpl connection;

  static {
    Petstore.init();
    EmpDept.init();
    Entities.define(COMBINED_ENTITY_ID,
            Properties.primaryKeyProperty("empno"),
            Properties.columnProperty("deptno", Types.INTEGER))
            .setSelectQuery("select e.empno, d.deptno from scott.emp e, scott.dept d where e.deptno = d.deptno");
  }

  public EntityConnectionImplTest() throws ClassNotFoundException, SQLException {
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
      connection.delete(Arrays.asList(key));
      try {
        connection.selectSingle(key);
        fail();
      }
      catch (DatabaseException e) {}
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
      catch (DatabaseException e) {}
    }
    finally {
      connection.rollbackTransaction();
    }
  }

  @Test
  public void fillReport() throws Exception {
    final Map<String, Object> reportParameters = new HashMap<String, Object>();
    reportParameters.put("DEPTNO", Arrays.asList(10, 20));
    final ReportResult print = connection.fillReport(
            new JasperReportsWrapper("resources/demos/empdept/reports/empdept_employees.jasper", reportParameters));
    assertNotNull(print.getResult());
  }

  @Test
  public void selectAll() throws Exception {
    final List<Entity> depts = connection.selectAll(EmpDept.T_DEPARTMENT);
    assertEquals(depts.size(), 4);
    final List<Entity> emps = connection.selectAll(COMBINED_ENTITY_ID);
    assertTrue(emps.size() > 0);
  }

  @Test
  public void selectDependentEntities() throws Exception {
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
  public void selectMany() throws Exception {
    List<Entity> result = connection.selectMany(EmpDept.T_DEPARTMENT, EmpDept.DEPARTMENT_ID, 10, 20);
    assertEquals(2, result.size());
    result = connection.selectMany(EntityUtil.getPrimaryKeys(result));
    assertEquals(2, result.size());
    result = connection.selectMany(EntityCriteriaUtil.selectCriteria(EmpDept.T_DEPARTMENT, new SimpleCriteria<Property.ColumnProperty>("deptno in (10, 20)")));
    assertEquals(2, result.size());
    result = connection.selectMany(EntityCriteriaUtil.selectCriteria(COMBINED_ENTITY_ID, new SimpleCriteria<Property.ColumnProperty>("d.deptno = 10")));
    assertTrue(result.size() > 0);

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
    rowCount = connection.selectRowCount(EntityCriteriaUtil.criteria(COMBINED_ENTITY_ID));
    assertEquals(16, rowCount);
  }

  @Test
  public void selectSingle() throws Exception {
    Entity sales = connection.selectSingle(EmpDept.T_DEPARTMENT, EmpDept.DEPARTMENT_NAME, "SALES");
    assertEquals(sales.getStringValue(EmpDept.DEPARTMENT_NAME), "SALES");
    sales = connection.selectSingle(sales.getPrimaryKey());
    assertEquals(sales.getStringValue(EmpDept.DEPARTMENT_NAME), "SALES");
    sales = connection.selectSingle(EntityCriteriaUtil.selectCriteria(EmpDept.T_DEPARTMENT, new SimpleCriteria<Property.ColumnProperty>("dname = 'SALES'")));
    assertEquals(sales.getStringValue(EmpDept.DEPARTMENT_NAME), "SALES");
  }

  @Test(expected = RecordNotFoundException.class)
  public void selectSingleNotFound() throws Exception {
    connection.selectSingle(EmpDept.T_DEPARTMENT, EmpDept.DEPARTMENT_NAME, "NO_NAME");
  }

  @Test(expected = DatabaseException.class)
  public void selectSingleManyFound() throws Exception {
    connection.selectSingle(EmpDept.T_EMPLOYEE, EmpDept.EMPLOYEE_JOB, "MANAGER");
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
  public void optimisticLocking() throws Exception {
    final EntityConnectionImpl baseDb = initializeConnection();
    final EntityConnectionImpl optimisticDb = initializeConnection();
    optimisticDb.setOptimisticLocking(true);
    String oldLocation = null;
    Entity updatedDepartment = null;
    try {
      final Entity department = baseDb.selectSingle(EmpDept.T_DEPARTMENT, EmpDept.DEPARTMENT_NAME, "SALES");
      oldLocation = (String) department.setValue(EmpDept.DEPARTMENT_LOCATION, "NEWLOC");
      updatedDepartment = baseDb.update(Arrays.asList(department)).get(0);
      try {
        optimisticDb.update(Arrays.asList(department));
        fail("RecordModifiedException should have been thrown");
      }
      catch (RecordModifiedException e) {
        assertTrue(((Entity) e.getModifiedRow()).propertyValuesEqual(updatedDepartment));
        assertTrue(((Entity) e.getRow()).propertyValuesEqual(department));
      }
    }
    finally {
      try {
        try {
          if (updatedDepartment != null && oldLocation != null) {
            updatedDepartment.setValue(EmpDept.DEPARTMENT_LOCATION, oldLocation);
            baseDb.update(Arrays.asList(updatedDepartment));
          }
        }
        catch (DatabaseException e) {
          e.printStackTrace();
        }
        baseDb.disconnect();
        optimisticDb.disconnect();
      }
      catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  private static EntityConnectionImpl initializeConnection() throws ClassNotFoundException, DatabaseException {
    return new EntityConnectionImpl(Databases.createInstance(), User.UNIT_TEST_USER);
  }
}