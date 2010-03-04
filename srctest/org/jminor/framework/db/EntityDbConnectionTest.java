/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db;

import org.jminor.common.db.SimpleCriteria;
import org.jminor.common.db.User;
import org.jminor.common.db.dbms.Database;
import org.jminor.common.db.dbms.DatabaseProvider;
import org.jminor.framework.db.criteria.SelectCriteria;
import org.jminor.framework.db.criteria.EntityCriteria;
import org.jminor.framework.db.exception.EntityModifiedException;
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

import junit.framework.TestCase;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * User: Björn Darri
 * Date: 31.3.2009
 * Time: 21:02:43
 */
public class EntityDbConnectionTest extends TestCase {

  public static final EntityDbProvider dbProvider =
          EntityDbProviderFactory.createEntityDbProvider(new User("scott", "tiger"), "JMinor Unit Tests");
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

  public void testSelectAll() throws Exception {
    final List<Entity> depts = dbProvider.getEntityDb().selectAll(EmpDept.T_DEPARTMENT);
    assertEquals(depts.size(), 4);
    List<Entity> emps = dbProvider.getEntityDb().selectAll(COMBINED_ENTITY_ID);
    assertTrue(emps.size() > 0);
  }

  public void testSelectDependentEntities() throws Exception {
    final List<Entity> accounting = dbProvider.getEntityDb().selectMany(EmpDept.T_DEPARTMENT, EmpDept.DEPARTMENT_NAME, "ACCOUNTING");
    final Map<String, List<Entity>> emps = dbProvider.getEntityDb().selectDependentEntities(accounting);
    assertEquals(1, emps.size());
    assertTrue(emps.containsKey(EmpDept.T_EMPLOYEE));
    assertEquals(7, emps.get(EmpDept.T_EMPLOYEE).size());
  }

  public void testSelectMany() throws Exception {
    List<Entity> result = dbProvider.getEntityDb().selectMany(EmpDept.T_DEPARTMENT, EmpDept.DEPARTMENT_ID, 10, 20);
    assertEquals(2, result.size());
    result = dbProvider.getEntityDb().selectMany(EntityUtil.getPrimaryKeys(result));
    assertEquals(2, result.size());
    result = dbProvider.getEntityDb().selectMany(new SelectCriteria(EmpDept.T_DEPARTMENT, new SimpleCriteria("deptno in (10, 20)")));
    assertEquals(2, result.size());
    result = dbProvider.getEntityDb().selectMany(new SelectCriteria(COMBINED_ENTITY_ID, new SimpleCriteria("d.deptno = 10")));
    assertTrue(result.size() > 0);
  }

  public void testSelectRowCount() throws Exception {
    int rowCount = dbProvider.getEntityDb().selectRowCount(new EntityCriteria(EmpDept.T_DEPARTMENT));
    assertEquals(4, rowCount);
    rowCount = dbProvider.getEntityDb().selectRowCount(new EntityCriteria(COMBINED_ENTITY_ID));
    assertEquals(16, rowCount);
  }

  public void testSelectSingle() throws Exception {
    Entity sales = dbProvider.getEntityDb().selectSingle(EmpDept.T_DEPARTMENT, EmpDept.DEPARTMENT_NAME, "SALES");
    assertEquals(sales.getStringValue(EmpDept.DEPARTMENT_NAME), "SALES");
    sales = dbProvider.getEntityDb().selectSingle(sales.getPrimaryKey());
    assertEquals(sales.getStringValue(EmpDept.DEPARTMENT_NAME), "SALES");
    sales = dbProvider.getEntityDb().selectSingle(new SelectCriteria(EmpDept.T_DEPARTMENT, new SimpleCriteria("dname = 'SALES'")));
    assertEquals(sales.getStringValue(EmpDept.DEPARTMENT_NAME), "SALES");
  }

  public void testSelectPropertyValues() throws Exception {
    final List<Object> result = dbProvider.getEntityDb().selectPropertyValues(EmpDept.T_DEPARTMENT, EmpDept.DEPARTMENT_NAME, false);
    assertTrue(result.contains("ACCOUNTING"));
    assertTrue(result.contains("SALES"));
    assertTrue(result.contains("RESEARCH"));
    assertTrue(result.contains("OPERATIONS"));
  }

  public void testDML() throws Exception {
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

    final Database database = DatabaseProvider.createInstance();

    //test with null values
    final Entity testEntity2 = EntityTest.getDetailEntity(idValue, intValue, null,
            stringValue, null, null, booleanValue, referencedEntityValue);
    assertEquals("insert into " + EntityTestDomain.T_DETAIL
            + "(id, int, string, boolean, entity_id)"
            + " values(1, 2, 'string', 1, 2)", EntityDbConnection.getInsertSQL(database, testEntity2));

    final Entity testEntity = EntityTest.getDetailEntity(idValue, intValue, doubleValue,
            stringValue, dateValue, timestampValue, booleanValue, referencedEntityValue);
    //assert dml
    final String shortDateStringSql = database.getSQLDateString(dateValue, false);
    final String longDateStringSql = database.getSQLDateString(timestampValue, true);
    assertEquals("insert into " + EntityTestDomain.T_DETAIL
            + "(id, int, double, string, date, timestamp, boolean, entity_id)"
            + " values(1, 2, 1.2, 'string', " + shortDateStringSql + ", " + longDateStringSql + ", 1, 2)",
            EntityDbConnection.getInsertSQL(database, testEntity));
    assertEquals("delete from " + EntityTestDomain.T_DETAIL + " where (id = 1)",
            EntityDbConnection.getDeleteSQL(database, testEntity.getPrimaryKey()));
    try {
      EntityDbConnection.getUpdateSQL(database, testEntity);
      fail("Should get an exception when trying to get update sql of a non-modified entity");
    }
    catch (Exception e) {}

    testEntity.setValue(EntityTestDomain.DETAIL_INT, 42);
    testEntity.setValue(EntityTestDomain.DETAIL_STRING, "newString");
    assertEquals( "update " + EntityTestDomain.T_DETAIL
            + " set int = 42, string = 'newString' where (id = 1)", EntityDbConnection.getUpdateSQL(database, testEntity));
    testEntity.setValue(EntityTestDomain.DETAIL_STRING, "string");
    assertEquals("update " + EntityTestDomain.T_DETAIL + " set int = 42 where (id = 1)",
            EntityDbConnection.getUpdateSQL(database, testEntity));
  }

  public void testGenerateSelectSql() throws Exception {
    final String generated = EntityDbConnection.getSelectSql("table", "col, col2", "where col = 1", "col2");
    assertEquals("Generate select should be working", "select col, col2 from table where col = 1 order by col2", generated);
  }

  public void testOptimisticLocking() throws Exception {
    final Database database = DatabaseProvider.createInstance();
    String oldLocation = null;
    Entity updatedDeparment = null;
    final EntityDbConnection baseDb = new EntityDbConnection(database, new User("scott", "tiger"));
    final EntityDbConnection optimisticDb = new EntityDbConnection(database, new User("scott", "tiger"));
    optimisticDb.setOptimisticLocking(true);
    try {
      final Entity department = baseDb.selectSingle(EmpDept.T_DEPARTMENT, EmpDept.DEPARTMENT_NAME, "SALES");
      oldLocation = (String) department.setValue(EmpDept.DEPARTMENT_LOCATION, "NEWLOC");
      updatedDeparment = baseDb.update(Arrays.asList(department)).get(0);
      try {
        optimisticDb.update(Arrays.asList(department));
        fail("EntityModifiedException should have been thrown");
      }
      catch (EntityModifiedException e) {
        assertTrue(e.getModifiedEntity().propertyValuesEqual(updatedDeparment));
        assertTrue(e.getEntity().propertyValuesEqual(department));
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
