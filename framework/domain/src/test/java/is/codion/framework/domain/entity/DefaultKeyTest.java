/*
 * Copyright (c) 2020 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
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
  void compositeKeyNull() {
    Entity master = ENTITIES.entity(TestDomain.T_COMPOSITE_MASTER);
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
  void singleKeyNull() {
    Key key = ENTITIES.keyBuilder(Detail.TYPE).build();
    assertTrue(key.isNull());
    key = key.copyBuilder()
            .with(Detail.ID, null)
            .build();
    assertTrue(key.isNull());
    key = key.copyBuilder()
            .with(Detail.ID, 1L)
            .build();
    assertTrue(key.isNotNull());
  }

  @Test
  void keyEquality() {
    List<Key> keys = ENTITIES.primaryKeys(Employee.TYPE, 1, 2);
    Key empKey1 = keys.get(0);
    Key empKey2 = keys.get(1);
    assertNotEquals(empKey1, empKey2);

    empKey2 = ENTITIES.primaryKey(Employee.TYPE, 1);
    assertEquals(empKey1, empKey2);

    Key deptKey = ENTITIES.primaryKey(Department.TYPE, 1);
    assertNotEquals(empKey1, deptKey);

    Key compMasterKey = ENTITIES.keyBuilder(TestDomain.T_COMPOSITE_MASTER)
            .with(TestDomain.COMPOSITE_MASTER_ID, 1)
            .with(TestDomain.COMPOSITE_MASTER_ID_2, 2)
            .build();
    assertEquals(compMasterKey, compMasterKey);
    assertNotEquals(empKey1, compMasterKey);
    assertNotEquals(compMasterKey, new Object());

    Key compMasterKey2 = ENTITIES.keyBuilder(TestDomain.T_COMPOSITE_MASTER)
            .with(TestDomain.COMPOSITE_MASTER_ID, 1)
            .build();
    assertNotEquals(compMasterKey, compMasterKey2);

    compMasterKey2 = compMasterKey2.copyBuilder()
            .with(TestDomain.COMPOSITE_MASTER_ID_2, 2)
            .build();
    //keys are still null, since COMPOSITE_MASTER_ID_3 is null
    assertNotEquals(compMasterKey, compMasterKey2);

    compMasterKey = compMasterKey.copyBuilder()
            .with(TestDomain.COMPOSITE_MASTER_ID_3, 3)
            .build();
    compMasterKey2 = compMasterKey2.copyBuilder()
            .with(TestDomain.COMPOSITE_MASTER_ID_3, 3)
            .build();
    assertEquals(compMasterKey, compMasterKey2);

    compMasterKey = compMasterKey.copyBuilder()
            .with(TestDomain.COMPOSITE_MASTER_ID, null)
            .build();
    compMasterKey2 = compMasterKey2.copyBuilder()
            .with(TestDomain.COMPOSITE_MASTER_ID, null)
            .build();
    //not null since COMPOSITE_MASTER_ID is nullable
    assertEquals(compMasterKey, compMasterKey2);

    Key detailKey = ENTITIES.primaryKey(Detail.TYPE, 1L);
    Key detailKey2 = ENTITIES.primaryKey(Detail.TYPE, 2L);
    assertNotEquals(detailKey, detailKey2);

    detailKey2 = detailKey2.copyBuilder()
            .with(Detail.ID, 1L)
            .build();
    assertEquals(detailKey2, detailKey);

    Entity department1 = ENTITIES.builder(Department.TYPE)
            .with(Department.NO, 1)
            .build();
    Entity department2 = ENTITIES.builder(Department.TYPE)
            .with(Department.NO, 1)
            .build();

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

    Key departmentKey = ENTITIES.primaryKey(Department.TYPE, 42);
    Key employeeKey = ENTITIES.primaryKey(Employee.TYPE, 42);
    assertNotEquals(departmentKey, employeeKey);
  }

  @Test
  void nullKeyEquals() {
    Key nullKey = ENTITIES.keyBuilder(Employee.TYPE).build();
    Key zeroKey = ENTITIES.primaryKey(Employee.TYPE, 0);
    assertNotEquals(nullKey, zeroKey);
  }
}
