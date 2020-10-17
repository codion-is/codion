/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import is.codion.framework.domain.TestDomain;
import is.codion.framework.domain.TestDomain.Department;
import is.codion.framework.domain.TestDomain.Detail;
import is.codion.framework.domain.TestDomain.Employee;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public final class DefaultKeyTest {

  private static final Entities ENTITIES = new TestDomain().getEntities();

  @Test
  public void compositeKeyNull() {
    final Entity master = ENTITIES.entity(TestDomain.T_COMPOSITE_MASTER);
    assertTrue(master.getPrimaryKey().isNull());
    assertFalse(master.getPrimaryKey().isNotNull());

    master.put(TestDomain.COMPOSITE_MASTER_ID_2, 2);
    master.put(TestDomain.COMPOSITE_MASTER_ID_3, 3);
    assertFalse(master.getPrimaryKey().isNull());
    assertTrue(master.getPrimaryKey().isNotNull());

    master.put(TestDomain.COMPOSITE_MASTER_ID, null);
    assertFalse(master.getPrimaryKey().isNull());
    assertTrue(master.getPrimaryKey().isNotNull());

    master.put(TestDomain.COMPOSITE_MASTER_ID, 2);
    master.put(TestDomain.COMPOSITE_MASTER_ID_2, null);
    assertTrue(master.getPrimaryKey().isNull());
    assertFalse(master.getPrimaryKey().isNotNull());

    master.put(TestDomain.COMPOSITE_MASTER_ID, null);
    assertTrue(master.getPrimaryKey().isNull());
    assertFalse(master.getPrimaryKey().isNotNull());
  }

  @Test
  public void singleKeyNull() {
    final Key key = ENTITIES.primaryKey(Detail.TYPE);
    assertTrue(key.isNull());
    key.put(null);
    assertTrue(key.isNull());
    key.put(1L);
    assertTrue(key.isNotNull());
  }

  @Test
  public void keyEquality() {
    final List<Key> keys = ENTITIES.primaryKeys(Employee.TYPE, 1, 2);
    final Key empKey1 = keys.get(0);
    final Key empKey2 = keys.get(1);
    assertNotEquals(empKey1, empKey2);

    empKey2.put(1);
    assertEquals(empKey1, empKey2);

    final Key deptKey = ENTITIES.primaryKey(Department.TYPE, 1);
    assertNotEquals(empKey1, deptKey);

    final Key compMasterKey = ENTITIES.primaryKey(TestDomain.T_COMPOSITE_MASTER);
    compMasterKey.put(TestDomain.COMPOSITE_MASTER_ID, 1);
    compMasterKey.put(TestDomain.COMPOSITE_MASTER_ID_2, 2);
    assertEquals(compMasterKey, compMasterKey);
    assertNotEquals(empKey1, compMasterKey);
    assertNotEquals(compMasterKey, new Object());

    final Key compMasterKey2 = ENTITIES.primaryKey(TestDomain.T_COMPOSITE_MASTER);
    compMasterKey2.put(TestDomain.COMPOSITE_MASTER_ID, 1);
    assertNotEquals(compMasterKey, compMasterKey2);

    compMasterKey2.put(TestDomain.COMPOSITE_MASTER_ID_2, 2);
    //keys are still null, since COMPOSITE_MASTER_ID_3 is null
    assertNotEquals(compMasterKey, compMasterKey2);

    compMasterKey.put(TestDomain.COMPOSITE_MASTER_ID_3, 3);
    compMasterKey2.put(TestDomain.COMPOSITE_MASTER_ID_3, 3);
    assertEquals(compMasterKey, compMasterKey2);

    compMasterKey.put(TestDomain.COMPOSITE_MASTER_ID, null);
    compMasterKey2.put(TestDomain.COMPOSITE_MASTER_ID, null);
    //not null since COMPOSITE_MASTER_ID is nullable
    assertEquals(compMasterKey, compMasterKey2);

    final Key detailKey = ENTITIES.primaryKey(Detail.TYPE, 1L);
    final Key detailKey2 = ENTITIES.primaryKey(Detail.TYPE, 2L);
    assertNotEquals(detailKey, detailKey2);

    detailKey2.put(1L);
    assertEquals(detailKey2, detailKey);

    final Entity department1 = ENTITIES.entity(Department.TYPE);
    department1.put(Department.NO, 1);
    final Entity department2 = ENTITIES.entity(Department.TYPE);
    department2.put(Department.NO, 1);

    assertEquals(department1.getPrimaryKey(), department2.getPrimaryKey());

    department2.put(Department.NO, 2);
    assertNotEquals(department1.getPrimaryKey(), department2.getPrimaryKey());

    department1.put(Department.NO, null);
    assertNotEquals(department1.getPrimaryKey(), department2.getPrimaryKey());

    department2.put(Department.NO, null);
    assertNotEquals(department1.getPrimaryKey(), department2.getPrimaryKey());

    department1.remove(Department.NO);
    assertNotEquals(department1.getPrimaryKey(), department2.getPrimaryKey());

    department2.remove(Department.NO);
    assertNotEquals(department1.getPrimaryKey(), department2.getPrimaryKey());

    final Key departmentKey = ENTITIES.primaryKey(Department.TYPE);
    departmentKey.put(42);
    final Key employeeKey = ENTITIES.primaryKey(Employee.TYPE);
    employeeKey.put(42);
    assertNotEquals(departmentKey, employeeKey);
  }

  @Test
  public void nullKeyEquals() {
    final Key nullKey = ENTITIES.primaryKey(Employee.TYPE);
    final Key zeroKey = ENTITIES.primaryKey(Employee.TYPE, 0);
    assertNotEquals(nullKey, zeroKey);
  }
}
