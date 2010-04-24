/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db;

import org.jminor.common.db.criteria.SimpleCriteria;
import org.jminor.common.db.dbms.Database;
import org.jminor.common.db.dbms.DatabaseProvider;
import org.jminor.common.db.exception.DbException;
import org.jminor.common.db.exception.RecordModifiedException;
import org.jminor.common.db.exception.RecordNotFoundException;
import org.jminor.common.model.User;
import org.jminor.framework.db.criteria.EntityCriteria;
import org.jminor.framework.db.criteria.EntityKeyCriteria;
import org.jminor.framework.db.criteria.EntitySelectCriteria;
import org.jminor.framework.db.provider.EntityDbProvider;
import org.jminor.framework.db.provider.EntityDbProviderFactory;
import org.jminor.framework.demos.empdept.domain.EmpDept;
import org.jminor.framework.demos.petstore.domain.Petstore;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.EntityDefinition;
import org.jminor.framework.domain.EntityRepository;
import org.jminor.framework.domain.EntityTest;
import org.jminor.framework.domain.EntityTestDomain;
import org.jminor.framework.domain.EntityUtil;
import org.jminor.framework.domain.Property;
import org.jminor.framework.domain.Type;

import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.util.JRLoader;
import static org.junit.Assert.*;
import org.junit.Test;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User: Bjorn Darri
 * Date: 31.3.2009
 * Time: 21:02:43
 */
public class EntityDbConnectionTest {

  public static final EntityDbProvider DB_PROVIDER =
          EntityDbProviderFactory.createEntityDbProvider(User.UNIT_TEST_USER, "JMinor Unit Tests");
  public static final Database DATABASE = DatabaseProvider.createInstance();

  public static final String COMBINED_ENTITY_ID = "selectQueryEntityID";

  static {
    new Petstore();
    new EmpDept();
    EntityRepository.add(new EntityDefinition(COMBINED_ENTITY_ID,
            new Property.PrimaryKeyProperty("empno"),
            new Property("deptno", Type.INT))
            .setSelectQuery("select e.empno, d.deptno from scott.emp e, scott.dept d where e.deptno = d.deptno"));
  }

  public EntityDbConnectionTest() {
    new EntityTestDomain();
  }

  @Test
  public void fillReport() throws Exception {
    final JasperReport report = (JasperReport) JRLoader.loadObject("resources/demos/empdept/reports/empdept_employees.jasper");
    final HashMap<String, Object> reportParameters = new HashMap<String, Object>();
    reportParameters.put("DEPTNO", Arrays.asList(10, 20));
    final JasperPrint print = DB_PROVIDER.getEntityDb().fillReport(report, reportParameters);
    assertNotNull(print);
  }

  @Test
  public void selectAll() throws Exception {
    final List<Entity> depts = DB_PROVIDER.getEntityDb().selectAll(EmpDept.T_DEPARTMENT);
    assertEquals(depts.size(), 4);
    List<Entity> emps = DB_PROVIDER.getEntityDb().selectAll(COMBINED_ENTITY_ID);
    assertTrue(emps.size() > 0);
  }

  @Test
  public void selectDependentEntities() throws Exception {
    final List<Entity> accounting = DB_PROVIDER.getEntityDb().selectMany(EmpDept.T_DEPARTMENT, EmpDept.DEPARTMENT_NAME, "ACCOUNTING");
    final Map<String, List<Entity>> emps = DB_PROVIDER.getEntityDb().selectDependentEntities(accounting);
    assertEquals(1, emps.size());
    assertTrue(emps.containsKey(EmpDept.T_EMPLOYEE));
    assertEquals(7, emps.get(EmpDept.T_EMPLOYEE).size());
  }

  @Test
  public void selectMany() throws Exception {
    List<Entity> result = DB_PROVIDER.getEntityDb().selectMany(EmpDept.T_DEPARTMENT, EmpDept.DEPARTMENT_ID, 10, 20);
    assertEquals(2, result.size());
    result = DB_PROVIDER.getEntityDb().selectMany(EntityUtil.getPrimaryKeys(result));
    assertEquals(2, result.size());
    result = DB_PROVIDER.getEntityDb().selectMany(new EntitySelectCriteria(EmpDept.T_DEPARTMENT, new SimpleCriteria("deptno in (10, 20)")));
    assertEquals(2, result.size());
    result = DB_PROVIDER.getEntityDb().selectMany(new EntitySelectCriteria(COMBINED_ENTITY_ID, new SimpleCriteria("d.deptno = 10")));
    assertTrue(result.size() > 0);

    final EntitySelectCriteria criteria = new EntitySelectCriteria(EmpDept.T_EMPLOYEE, new SimpleCriteria("ename = 'BLAKE'"));
    result = DB_PROVIDER.getEntityDb().selectMany(criteria);
    Entity emp = result.get(0);
    assertTrue(emp.isLoaded(EmpDept.EMPLOYEE_DEPARTMENT_FK));
    assertTrue(emp.isLoaded(EmpDept.EMPLOYEE_MGR_FK));
    emp = emp.getEntityValue(EmpDept.EMPLOYEE_MGR_FK);
    assertFalse(emp.isLoaded(EmpDept.EMPLOYEE_MGR_FK));

    result = DB_PROVIDER.getEntityDb().selectMany(criteria.setFetchDepth(EmpDept.EMPLOYEE_DEPARTMENT_FK, 0));
    assertEquals(1, result.size());
    emp = result.get(0);
    assertFalse(emp.isLoaded(EmpDept.EMPLOYEE_DEPARTMENT_FK));
    assertTrue(emp.isLoaded(EmpDept.EMPLOYEE_MGR_FK));

    result = DB_PROVIDER.getEntityDb().selectMany(criteria.setFetchDepth(EmpDept.EMPLOYEE_MGR_FK, 0));
    assertEquals(1, result.size());
    emp = result.get(0);
    assertFalse(emp.isLoaded(EmpDept.EMPLOYEE_DEPARTMENT_FK));
    assertFalse(emp.isLoaded(EmpDept.EMPLOYEE_MGR_FK));

    result = DB_PROVIDER.getEntityDb().selectMany(criteria.setFetchDepth(EmpDept.EMPLOYEE_MGR_FK, 2));
    assertEquals(1, result.size());
    emp = result.get(0);
    assertFalse(emp.isLoaded(EmpDept.EMPLOYEE_DEPARTMENT_FK));
    assertTrue(emp.isLoaded(EmpDept.EMPLOYEE_MGR_FK));
    emp = emp.getEntityValue(EmpDept.EMPLOYEE_MGR_FK);
    assertTrue(emp.isLoaded(EmpDept.EMPLOYEE_MGR_FK));
  }

  @Test(expected = DbException.class)
  public void selectManyInvalidColumn() throws Exception {
    DB_PROVIDER.getEntityDb().selectMany(new EntitySelectCriteria(EmpDept.T_DEPARTMENT,
            new SimpleCriteria("no_column is null")));
  }

  @Test
  public void selectRowCount() throws Exception {
    int rowCount = DB_PROVIDER.getEntityDb().selectRowCount(new EntityCriteria(EmpDept.T_DEPARTMENT));
    assertEquals(4, rowCount);
    rowCount = DB_PROVIDER.getEntityDb().selectRowCount(new EntityCriteria(COMBINED_ENTITY_ID));
    assertEquals(16, rowCount);
  }

  @Test
  public void selectSingle() throws Exception {
    Entity sales = DB_PROVIDER.getEntityDb().selectSingle(EmpDept.T_DEPARTMENT, EmpDept.DEPARTMENT_NAME, "SALES");
    assertEquals(sales.getStringValue(EmpDept.DEPARTMENT_NAME), "SALES");
    sales = DB_PROVIDER.getEntityDb().selectSingle(sales.getPrimaryKey());
    assertEquals(sales.getStringValue(EmpDept.DEPARTMENT_NAME), "SALES");
    sales = DB_PROVIDER.getEntityDb().selectSingle(new EntitySelectCriteria(EmpDept.T_DEPARTMENT, new SimpleCriteria("dname = 'SALES'")));
    assertEquals(sales.getStringValue(EmpDept.DEPARTMENT_NAME), "SALES");
  }

  @Test(expected = RecordNotFoundException.class)
  public void selectSingleNotFound() throws Exception {
    DB_PROVIDER.getEntityDb().selectSingle(EmpDept.T_DEPARTMENT, EmpDept.DEPARTMENT_NAME, "NO_NAME");
  }

  @Test(expected = DbException.class)
  public void selectSingleManyFound() throws Exception {
    DB_PROVIDER.getEntityDb().selectSingle(EmpDept.T_EMPLOYEE, EmpDept.EMPLOYEE_JOB, "MANAGER");
  }

  @Test
  public void selectPropertyValues() throws Exception {
    final List<Object> result = DB_PROVIDER.getEntityDb().selectPropertyValues(EmpDept.T_DEPARTMENT, EmpDept.DEPARTMENT_NAME, false);
    assertTrue(result.contains("ACCOUNTING"));
    assertTrue(result.contains("SALES"));
    assertTrue(result.contains("RESEARCH"));
    assertTrue(result.contains("OPERATIONS"));
  }

  @Test
  public void getInsertSQL() throws Exception {
    final int idValue = 1;
    final int intValue = 2;
    final double doubleValue = 1.2;
    final String stringValue = "string";
    final Date dateValue = new Date();
    final Timestamp timestampValue = new Timestamp(new Date().getTime());
    final Boolean booleanValue = true;
    final int referenceId = 2;

    final Entity referencedEntityValue = new Entity(EntityTestDomain.T_MASTER);
    referencedEntityValue.setValue(EntityTestDomain.MASTER_ID, referenceId);
    referencedEntityValue.setValue(EntityTestDomain.MASTER_NAME, stringValue);

    final Entity testEntity = EntityTest.getDetailEntity(idValue, intValue, null,
            stringValue, null, null, booleanValue, referencedEntityValue);

    assertEquals("insert into " + EntityTestDomain.T_DETAIL
            + "(id, int, string, boolean, entity_id)"
            + " values(1, 2, 'string', 1, 2)", EntityDbConnection.getInsertSQL(DATABASE, testEntity));

    testEntity.setValue(EntityTestDomain.DETAIL_DOUBLE, doubleValue);
    testEntity.setValue(EntityTestDomain.DETAIL_DATE, dateValue);
    testEntity.setValue(EntityTestDomain.DETAIL_TIMESTAMP, timestampValue);

    final String shortDateStringSql = DATABASE.getSQLDateString(dateValue, false);
    final String longDateStringSql = DATABASE.getSQLDateString(timestampValue, true);
    assertEquals("insert into " + EntityTestDomain.T_DETAIL
            + "(id, int, double, string, date, timestamp, boolean, entity_id)"
            + " values(1, 2, 1.2, 'string', " + shortDateStringSql + ", " + longDateStringSql + ", 1, 2)",
            EntityDbConnection.getInsertSQL(DATABASE, testEntity));
  }

  @Test
  public void getDeleteSQL() throws Exception {
    final Entity testEntity = new Entity(EntityTestDomain.T_DETAIL);
    testEntity.setValue(EntityTestDomain.DETAIL_ID, 1);

    assertEquals("delete from " + EntityTestDomain.T_DETAIL + " where (id = 1)",
            EntityDbConnection.getDeleteSQL(DATABASE, testEntity.getPrimaryKey()));

    assertEquals("delete from " + EntityTestDomain.T_DETAIL + " where (id = 1)",
            EntityDbConnection.getDeleteSQL(DATABASE, new EntityCriteria(testEntity.getEntityID(),
                    new EntityKeyCriteria(testEntity.getPrimaryKey()))));
  }

  @Test
  public void getUpdateSQL() throws Exception {
    final int idValue = 1;
    final int intValue = 2;
    final double doubleValue = 1.2;
    final String stringValue = "string";
    final Date dateValue = new Date();
    final Timestamp timestampValue = new Timestamp(new Date().getTime());
    final Boolean booleanValue = true;
    final int referenceId = 2;

    final Entity referencedEntityValue = new Entity(EntityTestDomain.T_MASTER);
    referencedEntityValue.setValue(EntityTestDomain.MASTER_ID, referenceId);
    referencedEntityValue.setValue(EntityTestDomain.MASTER_NAME, stringValue);

    final Entity testEntity = EntityTest.getDetailEntity(idValue, intValue, doubleValue,
            stringValue, dateValue, timestampValue, booleanValue, referencedEntityValue);

    try {
      EntityDbConnection.getUpdateSQL(DATABASE, testEntity);
      fail("Should get an exception when trying to get update sql of a non-modified entity");
    }
    catch (Exception e) {}

    testEntity.setValue(EntityTestDomain.DETAIL_INT, 42);
    testEntity.setValue(EntityTestDomain.DETAIL_STRING, "newString");
    assertEquals( "update " + EntityTestDomain.T_DETAIL
            + " set int = 42, string = 'newString' where (id = 1)",
            EntityDbConnection.getUpdateSQL(DATABASE, testEntity));
    testEntity.setValue(EntityTestDomain.DETAIL_STRING, "string");
    assertEquals("update " + EntityTestDomain.T_DETAIL + " set int = 42 where (id = 1)",
            EntityDbConnection.getUpdateSQL(DATABASE, testEntity));
  }

  @Test
  public void getSelectSql() throws Exception {
    final String generated = EntityDbConnection.getSelectSQL("table", "col, col2", "where col = 1", "col2");
    assertEquals("Generate select should be working", "select col, col2 from table where col = 1 order by col2", generated);
  }

  @Test
  public void optimisticLocking() throws Exception {
    final Database database = DatabaseProvider.createInstance();
    String oldLocation = null;
    Entity updatedDeparment = null;
    final EntityDbConnection baseDb = new EntityDbConnection(database, User.UNIT_TEST_USER);
    final EntityDbConnection optimisticDb = new EntityDbConnection(database, User.UNIT_TEST_USER);
    optimisticDb.setOptimisticLocking(true);
    try {
      final Entity department = baseDb.selectSingle(EmpDept.T_DEPARTMENT, EmpDept.DEPARTMENT_NAME, "SALES");
      oldLocation = (String) department.setValue(EmpDept.DEPARTMENT_LOCATION, "NEWLOC");
      updatedDeparment = baseDb.update(Arrays.asList(department)).get(0);
      try {
        optimisticDb.update(Arrays.asList(department));
        fail("RecordModifiedException should have been thrown");
      }
      catch (RecordModifiedException e) {
        assertTrue(((Entity) e.getModifiedRow()).propertyValuesEqual(updatedDeparment));
        assertTrue(((Entity) e.getRow()).propertyValuesEqual(department));
      }
    }
    finally {
      try {
        if (updatedDeparment != null && oldLocation != null) {
          updatedDeparment.setValue(EmpDept.DEPARTMENT_LOCATION, oldLocation);
          baseDb.update(Arrays.asList(updatedDeparment));
        }
        baseDb.disconnect();
        optimisticDb.disconnect();
      }
      catch (Exception e) {
        e.printStackTrace();
      }
    }
  }
}
