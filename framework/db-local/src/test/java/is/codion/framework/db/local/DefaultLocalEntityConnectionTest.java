/*
 * Copyright (c) 2009 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
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
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.condition.Condition;
import is.codion.framework.db.condition.SelectCondition;
import is.codion.framework.db.condition.UpdateCondition;
import is.codion.framework.db.local.ConfigureDb.Configured;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.Key;
import is.codion.framework.domain.entity.OrderBy;

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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.TimeZone;

import static is.codion.framework.db.condition.Condition.condition;
import static is.codion.framework.db.condition.Condition.where;
import static is.codion.framework.db.local.TestDomain.*;
import static is.codion.framework.domain.entity.Entity.primaryKeys;
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
  void setup() throws ClassNotFoundException, DatabaseException {
    connection = createConnection();
  }

  @AfterEach
  void tearDown() {
    connection.close();
  }

  @Test
  void configuraDatabase() throws DatabaseException {
    try (LocalEntityConnection connection = new DefaultLocalEntityConnection(Database.instance(), new ConfigureDb(), UNIT_TEST_USER)) {
      //throws exception if table does not exist, which is created during connection configuration
      connection.select(condition(Configured.TYPE));
    }
  }

  @Test
  void delete() throws Exception {
    connection.beginTransaction();
    try {
      Key key = ENTITIES.primaryKey(Department.TYPE, 40);
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
      Key key = ENTITIES.primaryKey(Department.TYPE, 40);
      assertEquals(1, connection.delete(Condition.condition(key)));
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
      assertEquals(3, connection.delete(where(Employee.NAME).equalTo("%S%")
              .and(where(Employee.JOB).equalTo("CLERK"))));
    }
    finally {
      connection.rollbackTransaction();
    }
  }

  @Test
  void deleteRowNumberMismatch() {
    Key key400 = ENTITIES.primaryKey(Department.TYPE, 400);
    assertThrows(DeleteException.class, () -> connection.delete(key400));
    Key key40 = ENTITIES.primaryKey(Department.TYPE, 40);
    assertThrows(DeleteException.class, () -> connection.delete(asList(key40, key400)));
  }

  @Test
  void deleteReferentialIntegrity() {
    Key key = ENTITIES.primaryKey(Department.TYPE, 10);
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
    Entity department = connection.selectSingle(Department.DEPTNO, 20);
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

    Key key = connection.insert(noPk);
    assertTrue(key.isNull());
  }

  @Test
  void updateNoParentKey() throws DatabaseException {
    Entity emp = connection.selectSingle(Employee.ID, 3);
    emp.put(Employee.DEPARTMENT, -1010);//not available
    assertThrows(ReferentialIntegrityException.class, () -> connection.update(emp));
  }

  @Test
  void deleteByKeyWithForeignKeys() throws DatabaseException {
    Entity accounting = connection.selectSingle(Department.DNAME, "ACCOUNTING");
    assertThrows(ReferentialIntegrityException.class, () -> connection.delete(accounting.primaryKey()));
  }

  @Test
  void deleteByConditionWithForeignKeys() throws DatabaseException {
    assertThrows(ReferentialIntegrityException.class, () ->
            connection.delete(where(Department.DNAME).equalTo("ACCOUNTING")));
  }

  @Test
  void fillReport() throws Exception {
    Map<String, Object> reportParameters = new HashMap<>();
    reportParameters.put("DEPTNO", asList(10, 20));
    assertEquals("result", connection.fillReport(REPORT, reportParameters));
  }

  @Test
  void selectDependencies() throws Exception {
    Map<EntityType, Collection<Entity>> empty = connection.selectDependencies(new ArrayList<>());
    assertTrue(empty.isEmpty());
    List<Entity> accounting = connection.select(Department.DNAME, "ACCOUNTING");
    Map<EntityType, Collection<Entity>> emps = connection.selectDependencies(accounting);
    assertEquals(1, emps.size());
    assertTrue(emps.containsKey(Employee.TYPE));
    assertEquals(7, emps.get(Employee.TYPE).size());
    emps.get(Employee.TYPE).forEach(entity -> assertTrue(entity.isImmutable()));

    Entity emp = connection.selectSingle(Employee.NAME, "KING");
    Map<EntityType, Collection<Entity>> deps = connection.selectDependencies(singletonList(emp));
    assertTrue(deps.isEmpty());//soft foreign key reference

    //multiple foreign keys referencing the same entity
    Entity master1 = connection.selectSingle(where(Master.ID).equalTo(1));
    Entity master2 = connection.selectSingle(where(Master.ID).equalTo(2));
    Entity master3 = connection.selectSingle(where(Master.ID).equalTo(3));
    Entity master4 = connection.selectSingle(where(Master.ID).equalTo(4));

    Entity detail1 = connection.selectSingle(where(Detail.ID).equalTo(1));
    Entity detail2 = connection.selectSingle(where(Detail.ID).equalTo(2));

    deps = connection.selectDependencies(singletonList(master1));
    Collection<Entity> dependantEntities = deps.get(Detail.TYPE);
    assertEquals(1, dependantEntities.size());
    assertTrue(dependantEntities.contains(detail1));

    deps = connection.selectDependencies(singletonList(master2));
    dependantEntities = deps.get(Detail.TYPE);
    assertEquals(2, dependantEntities.size());
    assertTrue(dependantEntities.containsAll(Arrays.asList(detail1, detail2)));

    deps = connection.selectDependencies(singletonList(master3));
    dependantEntities = deps.get(Detail.TYPE);
    assertEquals(1, dependantEntities.size());
    assertTrue(dependantEntities.contains(detail2));

    deps = connection.selectDependencies(Arrays.asList(master1, master2));
    dependantEntities = deps.get(Detail.TYPE);
    assertEquals(2, dependantEntities.size());
    assertTrue(dependantEntities.containsAll(Arrays.asList(detail1, detail2)));

    deps = connection.selectDependencies(Arrays.asList(master2, master3));
    dependantEntities = deps.get(Detail.TYPE);
    assertEquals(2, dependantEntities.size());
    assertTrue(dependantEntities.containsAll(Arrays.asList(detail1, detail2)));

    deps = connection.selectDependencies(Arrays.asList(master1, master2, master3, master4));
    dependantEntities = deps.get(Detail.TYPE);
    assertEquals(2, dependantEntities.size());
    assertTrue(dependantEntities.containsAll(Arrays.asList(detail1, detail2)));

    deps = connection.selectDependencies(singletonList(master4));
    assertFalse(deps.containsKey(Detail.TYPE));

    Entity jones = connection.selectSingle(EmployeeFk.NAME, "JONES");
    assertTrue(connection.selectDependencies(singletonList(jones)).isEmpty());//soft reference
  }

  @Test
  void selectLimitOffset() throws Exception {
    SelectCondition condition = condition(Employee.TYPE).selectBuilder()
            .orderBy(OrderBy.ascending(Employee.NAME))
            .limit(2)
            .build();
    List<Entity> result = connection.select(condition);
    assertEquals(2, result.size());
    condition = condition.selectBuilder()
            .limit(3)
            .offset(3)
            .build();
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
    Collection<Entity> result = connection.select(new ArrayList<>());
    assertTrue(result.isEmpty());
    result = connection.select(Department.DEPTNO, asList(10, 20));
    assertEquals(2, result.size());
    result = connection.select(primaryKeys(result));
    assertEquals(2, result.size());
    result = connection.select(Condition.customCondition(Department.DEPARTMENT_CONDITION_TYPE,
            asList(Department.DEPTNO, Department.DEPTNO), asList(10, 20)));
    assertEquals(2, result.size());
    result = connection.select(Condition.customCondition(EmpnoDeptno.CONDITION));
    assertEquals(7, result.size());

    SelectCondition condition = Condition.customCondition(Employee.NAME_IS_BLAKE_CONDITION_ID).selectBuilder().build();
    result = connection.select(condition);
    Entity emp = result.iterator().next();
    assertTrue(emp.isLoaded(Employee.DEPARTMENT_FK));
    assertTrue(emp.isLoaded(Employee.MGR_FK));
    emp = emp.referencedEntity(Employee.MGR_FK);
    assertFalse(emp.isLoaded(Employee.MGR_FK));

    condition = condition.selectBuilder().fetchDepth(Employee.DEPARTMENT_FK, 0).build();
    result = connection.select(condition);
    assertEquals(1, result.size());
    emp = result.iterator().next();
    assertFalse(emp.isLoaded(Employee.DEPARTMENT_FK));
    assertTrue(emp.isLoaded(Employee.MGR_FK));

    condition = condition.selectBuilder().fetchDepth(Employee.MGR_FK, 0).build();
    result = connection.select(condition);
    assertEquals(1, result.size());
    emp = result.iterator().next();
    assertFalse(emp.isLoaded(Employee.DEPARTMENT_FK));
    assertFalse(emp.isLoaded(Employee.MGR_FK));

    condition = condition.selectBuilder().fetchDepth(Employee.MGR_FK, 2).build();
    result = connection.select(condition);
    assertEquals(1, result.size());
    emp = result.iterator().next();
    assertFalse(emp.isLoaded(Employee.DEPARTMENT_FK));
    assertTrue(emp.isLoaded(Employee.MGR_FK));
    emp = emp.referencedEntity(Employee.MGR_FK);
    assertTrue(emp.isLoaded(Employee.MGR_FK));

    condition = condition.selectBuilder().fetchDepth(Employee.MGR_FK, -1).build();
    result = connection.select(condition);
    assertEquals(1, result.size());
    emp = result.iterator().next();
    assertFalse(emp.isLoaded(Employee.DEPARTMENT_FK));
    assertTrue(emp.isLoaded(Employee.MGR_FK));
    emp = emp.referencedEntity(Employee.MGR_FK);
    assertTrue(emp.isLoaded(Employee.MGR_FK));

    assertEquals(4, connection.rowCount(where(Employee.ID).equalTo(asList(1, 2, 3, 4))));
    assertEquals(0, connection.rowCount(where(Employee.DEPARTMENT).isNull()));
    assertEquals(0, connection.rowCount(where(Employee.DEPARTMENT_FK).isNull()));
    assertEquals(1, connection.rowCount(where(Employee.MGR).isNull()));
    assertEquals(1, connection.rowCount(where(Employee.MGR_FK).isNull()));
  }

  @Test
  void selectLimit() throws DatabaseException {
    Condition condition = condition(Department.TYPE);
    List<Entity> departments = connection.select(condition);
    assertEquals(4, departments.size());
    SelectCondition.Builder selectConditionBuilder = condition.selectBuilder();
    departments = connection.select(selectConditionBuilder.limit(0).build());
    assertTrue(departments.isEmpty());
    departments = connection.select(selectConditionBuilder.limit(2).build());
    assertEquals(2, departments.size());
    departments = connection.select(selectConditionBuilder.limit(3).build());
    assertEquals(3, departments.size());
    departments = connection.select(selectConditionBuilder.limit(-1).build());
    assertEquals(4, departments.size());
  }

  @Test
  void selectByKey() throws DatabaseException {
    Key deptKey = ENTITIES.primaryKey(Department.TYPE, 10);
    Key empKey = ENTITIES.primaryKey(Employee.TYPE, 8);

    Collection<Entity> selected = connection.select(asList(deptKey, empKey));
    assertEquals(2, selected.size());
  }

  @Test
  void foreignKeyAttributes() throws DatabaseException {
    List<Entity> emps = connection.select(where(EmployeeFk.MGR_FK).isNotNull()
            .selectBuilder()
            .fetchDepth(EmployeeFk.MGR_FK, 2)
            .build());
    for (Entity emp : emps) {
      Entity mgr = emp.referencedEntity(EmployeeFk.MGR_FK);
      assertTrue(mgr.isImmutable());
      assertTrue(mgr.contains(EmployeeFk.ID));//pk automatically included
      assertTrue(mgr.contains(EmployeeFk.NAME));
      assertTrue(mgr.contains(EmployeeFk.JOB));
      assertTrue(mgr.contains(EmployeeFk.DEPARTMENT));
      assertTrue(mgr.contains(EmployeeFk.DEPARTMENT_FK));
      assertTrue(mgr.get(EmployeeFk.DEPARTMENT_FK).isImmutable());
      assertFalse(mgr.contains(EmployeeFk.MGR));
      assertFalse(mgr.contains(EmployeeFk.MGR_FK));
      assertFalse(mgr.contains(EmployeeFk.COMMISSION));
      assertFalse(mgr.contains(EmployeeFk.HIREDATE));
      assertFalse(mgr.contains(EmployeeFk.SALARY));
    }
  }

  @Test
  void selectAttributes() throws Exception {
    List<Entity> emps = connection.select(condition(Employee.TYPE).selectBuilder()
            .selectAttributes(Employee.ID, Employee.JOB, Employee.DEPARTMENT)
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
    for (Entity emp : connection.select(condition(Employee.TYPE).selectBuilder()
            .selectAttributes(Employee.ID, Employee.JOB, Employee.DEPARTMENT_FK, Employee.MGR, Employee.COMMISSION)
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
  void selectInvalidColumn() throws Exception {
    assertThrows(DatabaseException.class, () -> connection.select(Condition.customCondition(Department.DEPARTMENT_CONDITION_INVALID_COLUMN_TYPE)));
  }

  @Test
  void rowCount() throws Exception {
    int rowCount = connection.rowCount(condition(Department.TYPE));
    assertEquals(4, rowCount);
    Condition deptNoCondition = where(Department.DEPTNO).greaterThanOrEqualTo(30);
    rowCount = connection.rowCount(deptNoCondition);
    assertEquals(2, rowCount);

    rowCount = connection.rowCount(condition(EmpnoDeptno.TYPE));
    assertEquals(16, rowCount);
    deptNoCondition = where(EmpnoDeptno.DEPTNO).greaterThanOrEqualTo(30);
    rowCount = connection.rowCount(deptNoCondition);
    assertEquals(4, rowCount);

    rowCount = connection.rowCount(condition(Job.TYPE));
    assertEquals(4, rowCount);
  }

  @Test
  void selectSingle() throws Exception {
    Entity sales = connection.selectSingle(Department.DNAME, "SALES");
    assertEquals(sales.get(Department.DNAME), "SALES");
    sales = connection.select(sales.primaryKey());
    assertEquals(sales.get(Department.DNAME), "SALES");
    sales = connection.selectSingle(Condition.customCondition(Department.DEPARTMENT_CONDITION_SALES_TYPE));
    assertEquals(sales.get(Department.DNAME), "SALES");

    Entity king = connection.selectSingle(Employee.NAME, "KING");
    assertTrue(king.contains(Employee.MGR_FK));
    assertNull(king.get(Employee.MGR_FK));

    king = connection.selectSingle(Employee.MGR_FK, null);
    assertNull(king.get(Employee.MGR_FK));
  }

  @Test
  void customCondition() throws DatabaseException {
    Condition condition = Condition.customCondition(Employee.MGR_GREATER_THAN_CONDITION_ID,
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
      Entity department = connection.selectSingle(Department.DEPTNO, 10);
      assertThrows(DatabaseException.class, () -> connection.update(department));
    }
    finally {
      connection.rollbackTransaction();
    }
  }

  @Test
  void dateTime() throws DatabaseException {
    Entity sales = connection.selectSingle(Department.DNAME, "SALES");
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
    Entity sales = connection.selectSingle(Department.DNAME, "SALES");
    final String name = "Nobody";
    final double salary = 1500;
    final double defaultCommission = 200;

    Entity emp = ENTITIES.builder(Employee.TYPE)
            .with(Employee.DEPARTMENT_FK, sales)
            .with(Employee.NAME, name)
            .with(Employee.SALARY, salary)
            .build();

    emp = connection.select(connection.insert(emp));
    assertEquals(sales, emp.get(Employee.DEPARTMENT_FK));
    assertEquals(name, emp.get(Employee.NAME));
    assertEquals(salary, emp.get(Employee.SALARY));
    assertEquals(defaultCommission, emp.get(Employee.COMMISSION));
    connection.delete(emp.primaryKey());

    emp.put(Employee.COMMISSION, null);//default value should not kick in
    emp = connection.select(connection.insert(emp));
    assertEquals(sales, emp.get(Employee.DEPARTMENT_FK));
    assertEquals(name, emp.get(Employee.NAME));
    assertEquals(salary, emp.get(Employee.SALARY));
    assertNull(emp.get(Employee.COMMISSION));
    connection.delete(emp.primaryKey());

    emp.remove(Employee.COMMISSION);//default value should kick in
    emp = connection.select(connection.insert(emp));
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
      Entity sales = connection.selectSingle(Department.DNAME, "SALES");
      Entity king = connection.selectSingle(Employee.NAME, "KING");
      final String newName = "New name";
      sales.put(Department.DNAME, newName);
      king.put(Employee.NAME, newName);
      List<Entity> updated = new ArrayList<>(connection.update(asList(sales, king)));
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
    Entity employee = connection.selectSingle(Employee.ID, 4);
    employee.put(Employee.ID, -888);//non existing
    employee.saveAll();
    employee.put(Employee.NAME, "New name");
    assertThrows(UpdateException.class, () -> connection.update(employee));
  }

  @Test
  void update() throws DatabaseException {
    Collection<Entity> updated = connection.update(new ArrayList<>());
    assertTrue(updated.isEmpty());
  }

  @Test
  void updateWithConditionNoProperties() throws DatabaseException {
    UpdateCondition condition = Condition.condition(Employee.TYPE).updateBuilder().build();
    assertThrows(IllegalArgumentException.class, () -> connection.update(condition));
  }

  @Test
  void updateWithCondition() throws DatabaseException {
    Condition condition = where(Employee.COMMISSION).isNull();

    List<Entity> entities = connection.select(condition);

    UpdateCondition updateCondition = where(Employee.COMMISSION).isNull().updateBuilder()
            .set(Employee.COMMISSION, 500d)
            .set(Employee.SALARY, 4200d)
            .build();
    connection.beginTransaction();
    try {
      connection.update(updateCondition);
      assertEquals(0, connection.rowCount(condition));
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
    UpdateCondition updateCondition = where(Employee.ID).isNull().updateBuilder()
            .set(Employee.SALARY, 4200d)
            .build();
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
    LocalEntityConnection connection = createConnection();
    LocalEntityConnection connection2 = createConnection();
    String originalLocation;
    try {
      SelectCondition condition = where(Department.DNAME).equalTo("SALES").selectBuilder().forUpdate().build();

      Entity sales = connection.selectSingle(condition);
      originalLocation = sales.get(Department.LOC);

      sales.put(Department.LOC, "Syracuse");
      try {
        connection2.update(sales);
        fail("Should not be able to update record selected for update by another connection");
      }
      catch (DatabaseException ignored) {
        connection2.databaseConnection().rollback();
      }

      connection.select(condition(Department.TYPE));//any query will do

      try {
        sales = connection2.update(sales);
        sales.put(Department.LOC, originalLocation);
        connection2.update(sales);//revert changes to data
      }
      catch (DatabaseException ignored) {
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
    LocalEntityConnection connection = createConnection();
    EntityConnection connection2 = createConnection();
    connection.setOptimisticLockingEnabled(true);
    Entity allen;
    try {
      Condition condition = where(Employee.NAME).equalTo("ALLEN");

      allen = connection.selectSingle(condition);

      connection2.delete(allen.primaryKey());

      allen.put(Employee.JOB, "CLERK");
      try {
        connection.update(allen);
        fail("Should not be able to update record deleted by another connection");
      }
      catch (RecordModifiedException e) {
        assertNotNull(e.row());
        assertNull(e.modifiedRow());
      }

      try {
        connection2.insert(allen);//revert changes to data
      }
      catch (DatabaseException ignored) {
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
    LocalEntityConnection baseConnection = createConnection();
    LocalEntityConnection optimisticConnection = createConnection(true);
    optimisticConnection.setOptimisticLockingEnabled(true);
    assertTrue(optimisticConnection.isOptimisticLockingEnabled());
    String oldLocation = null;
    Entity updatedDepartment = null;
    try {
      Entity department = baseConnection.selectSingle(Department.DNAME, "SALES");
      oldLocation = department.put(Department.LOC, "NEWLOC");
      updatedDepartment = baseConnection.update(department);
      try {
        optimisticConnection.update(department);
        fail("RecordModifiedException should have been thrown");
      }
      catch (RecordModifiedException e) {
        assertTrue(((Entity) e.modifiedRow()).columnValuesEqual(updatedDepartment));
        assertTrue(((Entity) e.row()).columnValuesEqual(department));
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
    optimisticConnection.setOptimisticLockingEnabled(true);
    Entity updatedEmployee = null;
    try {
      Random random = new Random();
      byte[] bytes = new byte[1024];
      random.nextBytes(bytes);

      Entity employee = baseConnection.selectSingle(Employee.NAME, "BLAKE");
      employee.put(Employee.DATA, bytes);
      updatedEmployee = baseConnection.update(employee);

      random.nextBytes(bytes);
      employee.put(Employee.DATA, bytes);

      try {
        optimisticConnection.update(employee);
        fail("RecordModifiedException should have been thrown");
      }
      catch (RecordModifiedException e) {
        assertTrue(((Entity) e.modifiedRow()).columnValuesEqual(updatedEmployee));
        assertTrue(((Entity) e.row()).columnValuesEqual(employee));
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
      Condition condition = condition(Employee.TYPE);
      Iterator<Entity> iterator = connection.iterator(condition).iterator();
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
      int rowCount = connection.rowCount(condition);
      assertEquals(rowCount, counter);
      iterator = connection.iterator(condition).iterator();
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
    try (LocalEntityConnection connection = createConnection()) {
      ResultIterator<Entity> deptIterator =
              connection.iterator(condition(Department.TYPE));
      while (deptIterator.hasNext()) {
        ResultIterator<Entity> empIterator =
                connection.iterator(where(Employee.DEPARTMENT_FK).equalTo(deptIterator.next()));
        while (empIterator.hasNext()) {
          empIterator.next();
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
      assertTrue(conn.isConnected());
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
  void testConstructorInvalidConnection() throws Exception {
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

    Entity scott = connection.selectSingle(Employee.ID, 7);
    connection.writeBlob(scott.primaryKey(), Employee.DATA_LAZY, lazyBytes);
    connection.writeBlob(scott.primaryKey(), Employee.DATA, bytes);
    assertArrayEquals(lazyBytes, connection.readBlob(scott.primaryKey(), Employee.DATA_LAZY));
    assertArrayEquals(bytes, connection.readBlob(scott.primaryKey(), Employee.DATA));

    Entity scottFromDb = connection.select(scott.primaryKey());
    //lazy loaded
    assertNull(scottFromDb.get(Employee.DATA_LAZY));
    assertNotNull(scottFromDb.get(Employee.DATA));

    //overrides lazy loading
    scottFromDb = connection.selectSingle(condition(scott.primaryKey()).selectBuilder()
            .selectAttributes(Employee.DATA_LAZY)
            .build());
    assertNotNull(scottFromDb.get(Employee.DATA_LAZY));
  }

  @Test
  void readWriteBlobViaEntity() throws DatabaseException {
    byte[] lazyBytes = new byte[1024];
    byte[] bytes = new byte[1024];
    Random random = new Random();
    random.nextBytes(lazyBytes);
    random.nextBytes(bytes);

    Entity scott = connection.selectSingle(Employee.ID, 7);
    scott.put(Employee.DATA_LAZY, lazyBytes);
    scott.put(Employee.DATA, bytes);
    connection.update(scott);
    scott.saveAll();

    byte[] lazyFromDb = connection.readBlob(scott.primaryKey(), Employee.DATA_LAZY);
    byte[] fromDb = connection.readBlob(scott.primaryKey(), Employee.DATA);
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

    lazyFromDb = connection.readBlob(scott.primaryKey(), Employee.DATA_LAZY);
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
    List<Entity> entities = connection.select(condition(NoPrimaryKey.TYPE));
    assertEquals(6, entities.size());
    entities = connection.select(where(NoPrimaryKey.COL_1).equalTo(2)
            .or(where(NoPrimaryKey.COL_3).equalTo("5")));
    assertEquals(4, entities.size());
  }

  @Test
  void beans() throws DatabaseException {
    connection.beginTransaction();
    try {
      List<Entity> departments = connection.select(condition(Department.TYPE));
      Department department = departments.get(0).castTo(Department.class);
      department.setName("New Name");

      department = connection.update(department).castTo(Department.class);

      assertEquals("New Name", department.getName());

      Collection<Department> departmentsCast = Entity.castTo(Department.class, connection.select(condition(Department.TYPE)));

      departmentsCast.forEach(dept -> dept.setName(dept.getName() + "N"));

      departmentsCast = Entity.castTo(Department.class, connection.update(new ArrayList<>(departmentsCast)));

      Department newDept1 = ENTITIES.entity(Department.TYPE).castTo(Department.class);
      newDept1.setId(-1);
      newDept1.setName("hello1");
      newDept1.setLocation("location");

      Department newDept2 = ENTITIES.entity(Department.TYPE).castTo(Department.class);
      newDept2.setId(-2);
      newDept2.setName("hello2");
      newDept2.setLocation("location");

      List<Key> keys = new ArrayList<>(connection.insert(asList(newDept1, newDept2)));
      assertEquals(Integer.valueOf(-1), keys.get(0).get());
      assertEquals(Integer.valueOf(-2), keys.get(1).get());
    }
    finally {
      connection.rollbackTransaction();
    }
  }

  @Test
  void selectQuery() throws DatabaseException {
    connection.select(condition(Query.TYPE));
    connection.select(condition(Query.TYPE).selectBuilder().forUpdate().build());
    connection.select(condition(QueryColumnsWhereClause.TYPE));
    connection.select(condition(QueryColumnsWhereClause.TYPE).selectBuilder().forUpdate().build());
    connection.select(condition(QueryFromClause.TYPE));
    connection.select(condition(QueryFromClause.TYPE).selectBuilder().forUpdate().build());
    connection.select(condition(QueryFromWhereClause.TYPE));
    connection.select(condition(QueryFromWhereClause.TYPE).selectBuilder().forUpdate().build());
  }

  @Test
  void queryCache() throws DatabaseException {
    connection.setQueryCacheEnabled(true);
    assertTrue(connection.isQueryCacheEnabled());

    List<Entity> result = connection.select(where(Department.DEPTNO)
            .greaterThanOrEqualTo(20));
    List<Entity> result2 = connection.select(where(Department.DEPTNO)
            .greaterThanOrEqualTo(20));
    assertSame(result, result2);

    result2 = connection.select(where(Department.DEPTNO)
            .greaterThanOrEqualTo(20)
            .selectBuilder()
            .orderBy(OrderBy.descending(Department.DEPTNO))
            .build());
    assertNotSame(result, result2);

    result = connection.select(where(Department.DEPTNO)
            .greaterThanOrEqualTo(20)
            .selectBuilder()
            .orderBy(OrderBy.descending(Department.DEPTNO))
            .build());
    assertSame(result, result2);

    result2 = connection.select(where(Department.DEPTNO)
            .greaterThanOrEqualTo(20)
            .selectBuilder()
            .orderBy(OrderBy.ascending(Department.DEPTNO))
            .build());
    assertNotSame(result, result2);

    result = connection.select(where(Department.DEPTNO)
            .greaterThanOrEqualTo(20)
            .selectBuilder()
            .forUpdate()
            .build());
    result2 = connection.select(where(Department.DEPTNO)
            .greaterThanOrEqualTo(20)
            .selectBuilder()
            .forUpdate()
            .build());
    assertNotSame(result, result2);

    result = connection.select(where(Department.DEPTNO)
            .equalTo(20));
    result2 = connection.select(Department.DEPTNO, 20);
    assertSame(result, result2);

    connection.setQueryCacheEnabled(false);
    assertFalse(connection.isQueryCacheEnabled());

    result = connection.select(where(Department.DEPTNO)
            .greaterThanOrEqualTo(20));
    result2 = connection.select(where(Department.DEPTNO)
            .greaterThanOrEqualTo(20));
    assertNotSame(result, result2);
  }

  @Test
  void orderByNullOrder() throws DatabaseException {
    List<Entity> result = connection.select(condition(Employee.TYPE)
            .selectBuilder()
            .orderBy(OrderBy.builder()
                    .ascendingNullsFirst(Employee.MGR)
                    .build())
            .build());
    assertEquals(result.get(0).get(Employee.NAME), "KING");

    result = connection.select(condition(Employee.TYPE)
            .selectBuilder()
            .orderBy(OrderBy.builder()
                    .ascendingNullsLast(Employee.MGR)
                    .build())
            .build());
    assertEquals(result.get(result.size() - 1).get(Employee.NAME), "KING");

    result = connection.select(condition(Employee.TYPE)
            .selectBuilder()
            .orderBy(OrderBy.builder()
                    .descendingNullsFirst(Employee.MGR)
                    .build())
            .build());
    assertEquals(result.get(0).get(Employee.NAME), "KING");

    result = connection.select(condition(Employee.TYPE)
            .selectBuilder()
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
}