/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain;

import org.jminor.common.model.DateUtil;
import org.jminor.common.model.Util;
import org.jminor.framework.demos.empdept.domain.EmpDept;

import static org.junit.Assert.*;
import org.junit.Test;

import java.sql.Timestamp;
import java.sql.Types;
import java.util.Date;

public class EntityTest {

  public static Entity getDetailEntity(final int id, final Integer intValue, final Double doubleValue,
                                       final String stringValue, final Date dateValue, final Timestamp timestampValue,
                                       final Boolean booleanValue, final Entity entityValue) {
    final Entity entity = new Entity(EntityTestDomain.T_DETAIL);
    entity.setValue(EntityTestDomain.DETAIL_ID, id);
    entity.setValue(EntityTestDomain.DETAIL_INT, intValue);
    entity.setValue(EntityTestDomain.DETAIL_DOUBLE, doubleValue);
    entity.setValue(EntityTestDomain.DETAIL_STRING, stringValue);
    entity.setValue(EntityTestDomain.DETAIL_DATE, dateValue);
    entity.setValue(EntityTestDomain.DETAIL_TIMESTAMP, timestampValue);
    entity.setValue(EntityTestDomain.DETAIL_BOOLEAN, booleanValue);
    entity.setValue(EntityTestDomain.DETAIL_ENTITY_FK, entityValue);

    return entity;
  }

  public EntityTest() {
    new EntityTestDomain();
  }

  @Test
  public void entity() throws Exception {
    final int detailId = 1;
    final int detailInt = 2;
    final double detailDouble = 1.2;
    final String detailString = "string";
    final Date detailDate = new Date();
    final Timestamp detailTimestamp = new Timestamp(new Date().getTime());
    final Boolean detailBoolean = true;

    final int masterId = 2;
    final String masterName = "master";
    final int masterCode = 7;

    final Entity referencedEntityValue = new Entity(EntityTestDomain.T_MASTER);
    referencedEntityValue.setValue(EntityTestDomain.MASTER_ID, masterId);
    referencedEntityValue.setValue(EntityTestDomain.MASTER_NAME, masterName);
    referencedEntityValue.setValue(EntityTestDomain.MASTER_CODE, masterCode);

    Entity test = new Entity(EntityTestDomain.T_DETAIL);
    //assert not modified
    assertFalse(test.isModified());

    final Entity testEntity = getDetailEntity(detailId, detailInt, detailDouble,
            detailString, detailDate, detailTimestamp, detailBoolean, referencedEntityValue);
    //assert types
    assertEquals(testEntity.getPrimaryKey().getProperty(EntityTestDomain.DETAIL_ID).getType(), Types.INTEGER);
    assertEquals(testEntity.getProperty(EntityTestDomain.DETAIL_INT).getType(), Types.INTEGER);
    assertEquals(testEntity.getProperty(EntityTestDomain.DETAIL_DOUBLE).getType(), Types.DOUBLE);
    assertEquals(testEntity.getProperty(EntityTestDomain.DETAIL_STRING).getType(), Types.VARCHAR);
    assertEquals(testEntity.getProperty(EntityTestDomain.DETAIL_DATE).getType(), Types.DATE);
    assertEquals(testEntity.getProperty(EntityTestDomain.DETAIL_TIMESTAMP).getType(), Types.TIMESTAMP);
    assertEquals(testEntity.getProperty(EntityTestDomain.DETAIL_BOOLEAN).getType(), Types.BOOLEAN);
    assertEquals(testEntity.getProperty(EntityTestDomain.DETAIL_ENTITY_FK).getType(), Types.REF);
    assertEquals(testEntity.getProperty(EntityTestDomain.DETAIL_ENTITY_ID).getType(), Types.INTEGER);
    assertEquals(testEntity.getProperty(EntityTestDomain.DETAIL_MASTER_NAME).getType(), Types.VARCHAR);
    assertEquals(testEntity.getProperty(EntityTestDomain.DETAIL_MASTER_CODE).getType(), Types.INTEGER);

    //assert column names
    assertEquals(testEntity.getPrimaryKey().getProperty(EntityTestDomain.DETAIL_ID).getPropertyID(), EntityTestDomain.DETAIL_ID);
    assertEquals(testEntity.getProperty(EntityTestDomain.DETAIL_INT).getPropertyID(), EntityTestDomain.DETAIL_INT);
    assertEquals(testEntity.getProperty(EntityTestDomain.DETAIL_DOUBLE).getPropertyID(), EntityTestDomain.DETAIL_DOUBLE);
    assertEquals(testEntity.getProperty(EntityTestDomain.DETAIL_STRING).getPropertyID(), EntityTestDomain.DETAIL_STRING);
    assertEquals(testEntity.getProperty(EntityTestDomain.DETAIL_DATE).getPropertyID(), EntityTestDomain.DETAIL_DATE);
    assertEquals(testEntity.getProperty(EntityTestDomain.DETAIL_TIMESTAMP).getPropertyID(), EntityTestDomain.DETAIL_TIMESTAMP);
    assertEquals(testEntity.getProperty(EntityTestDomain.DETAIL_BOOLEAN).getPropertyID(), EntityTestDomain.DETAIL_BOOLEAN);
    assertEquals(testEntity.getProperty(EntityTestDomain.DETAIL_ENTITY_ID).getPropertyID(), EntityTestDomain.DETAIL_ENTITY_ID);
    assertEquals(testEntity.getProperty(EntityTestDomain.DETAIL_MASTER_NAME).getPropertyID(), EntityTestDomain.DETAIL_MASTER_NAME);
    assertEquals(testEntity.getProperty(EntityTestDomain.DETAIL_MASTER_CODE).getPropertyID(), EntityTestDomain.DETAIL_MASTER_CODE);

    //assert captions
    assertNull(testEntity.getPrimaryKey().getProperty(EntityTestDomain.DETAIL_ID).getCaption());
    assertEquals(testEntity.getProperty(EntityTestDomain.DETAIL_INT).getCaption(), EntityTestDomain.DETAIL_INT);
    assertEquals(testEntity.getProperty(EntityTestDomain.DETAIL_DOUBLE).getCaption(), EntityTestDomain.DETAIL_DOUBLE);
    assertEquals(testEntity.getProperty(EntityTestDomain.DETAIL_STRING).getCaption(), EntityTestDomain.DETAIL_STRING);
    assertEquals(testEntity.getProperty(EntityTestDomain.DETAIL_DATE).getCaption(), EntityTestDomain.DETAIL_DATE);
    assertEquals(testEntity.getProperty(EntityTestDomain.DETAIL_TIMESTAMP).getCaption(), EntityTestDomain.DETAIL_TIMESTAMP);
    assertEquals(testEntity.getProperty(EntityTestDomain.DETAIL_BOOLEAN).getCaption(), EntityTestDomain.DETAIL_BOOLEAN);
    assertEquals(testEntity.getProperty(EntityTestDomain.DETAIL_ENTITY_FK).getCaption(), EntityTestDomain.DETAIL_ENTITY_FK);
    assertEquals(testEntity.getProperty(EntityTestDomain.DETAIL_MASTER_NAME).getCaption(), EntityTestDomain.DETAIL_MASTER_NAME);
    assertEquals(testEntity.getProperty(EntityTestDomain.DETAIL_MASTER_CODE).getCaption(), EntityTestDomain.DETAIL_MASTER_CODE);

    //assert hidden status
    assertTrue(testEntity.getPrimaryKey().getProperty(EntityTestDomain.DETAIL_ID).isHidden());
    assertFalse(testEntity.getProperty(EntityTestDomain.DETAIL_INT).isHidden());
    assertFalse(testEntity.getProperty(EntityTestDomain.DETAIL_DOUBLE).isHidden());
    assertFalse(testEntity.getProperty(EntityTestDomain.DETAIL_STRING).isHidden());
    assertFalse(testEntity.getProperty(EntityTestDomain.DETAIL_DATE).isHidden());
    assertFalse(testEntity.getProperty(EntityTestDomain.DETAIL_TIMESTAMP).isHidden());
    assertFalse(testEntity.getProperty(EntityTestDomain.DETAIL_BOOLEAN).isHidden());
    assertFalse(testEntity.getProperty(EntityTestDomain.DETAIL_ENTITY_FK).isHidden());
    assertFalse(testEntity.getProperty(EntityTestDomain.DETAIL_MASTER_NAME).isHidden());
    assertFalse(testEntity.getProperty(EntityTestDomain.DETAIL_MASTER_CODE).isHidden());

    //assert values
    assertEquals(testEntity.getValue(EntityTestDomain.DETAIL_ID), detailId);
    assertEquals(testEntity.getValue(EntityTestDomain.DETAIL_INT), detailInt);
    assertEquals(testEntity.getValue(EntityTestDomain.DETAIL_DOUBLE), detailDouble);
    assertEquals(testEntity.getValue(EntityTestDomain.DETAIL_STRING), detailString);
    assertEquals(testEntity.getValue(EntityTestDomain.DETAIL_DATE), detailDate);
    assertEquals(testEntity.getValue(EntityTestDomain.DETAIL_TIMESTAMP), detailTimestamp);
    assertEquals(testEntity.getValue(EntityTestDomain.DETAIL_BOOLEAN), detailBoolean);
    assertEquals(testEntity.getValue(EntityTestDomain.DETAIL_ENTITY_FK), referencedEntityValue);
    assertEquals(testEntity.getValue(EntityTestDomain.DETAIL_MASTER_NAME), masterName);
    assertEquals(testEntity.getValue(EntityTestDomain.DETAIL_MASTER_CODE), masterCode);
    assertFalse(testEntity.isValueNull(EntityTestDomain.DETAIL_ENTITY_ID));
    try {
      testEntity.setValue(EntityTestDomain.DETAIL_MASTER_NAME, "hello");
      fail("Set value for a denormalized view property should cause an error");
    }
    catch (Exception e) {}
    try {
      testEntity.setValue(EntityTestDomain.DETAIL_MASTER_CODE, 2);
      fail("Set value for a denormalized property should cause an error");
    }
    catch (Exception e) {}
    //test setAs()
    test = new Entity(EntityTestDomain.T_DETAIL);
    test.setAs(testEntity);
    assertTrue("Entities should be equal after .setAs()", Util.equal(test, testEntity));
    assertTrue("Entity property values should be equal after .setAs()", test.propertyValuesEqual(testEntity));

    //test copy()
    final Entity test2 = testEntity.getCopy();
    assertFalse("Entity copy should not be == the original", test2 == testEntity);
    assertTrue("Entities should be equal after .getCopy()", Util.equal(test2, testEntity));
    assertTrue("Entity property values should be equal after .getCopy()", test2.propertyValuesEqual(testEntity));

    //test propogate entity reference/denormalized values
    testEntity.setValue(EntityTestDomain.DETAIL_ENTITY_FK, null);
    assertTrue(testEntity.isValueNull(EntityTestDomain.DETAIL_ENTITY_ID));
    assertTrue(testEntity.isValueNull(EntityTestDomain.DETAIL_MASTER_NAME));
    assertTrue(testEntity.isValueNull(EntityTestDomain.DETAIL_MASTER_CODE));

    testEntity.setValue(EntityTestDomain.DETAIL_ENTITY_FK, referencedEntityValue);
    assertFalse(testEntity.isValueNull(EntityTestDomain.DETAIL_ENTITY_ID));
    assertEquals(testEntity.getValue(EntityTestDomain.DETAIL_ENTITY_ID),
            referencedEntityValue.getValue(EntityTestDomain.MASTER_ID));
    assertEquals(testEntity.getValue(EntityTestDomain.DETAIL_MASTER_NAME),
            referencedEntityValue.getValue(EntityTestDomain.MASTER_NAME));
    assertEquals(testEntity.getValue(EntityTestDomain.DETAIL_MASTER_CODE),
            referencedEntityValue.getValue(EntityTestDomain.MASTER_CODE));

    referencedEntityValue.setValue(EntityTestDomain.MASTER_CODE, 20);
    testEntity.setValue(EntityTestDomain.DETAIL_ENTITY_FK, referencedEntityValue);
    assertEquals(testEntity.getValue(EntityTestDomain.DETAIL_MASTER_CODE),
            referencedEntityValue.getValue(EntityTestDomain.MASTER_CODE));

    try {
      test2.stateModified().setActive(true);
      fail("Should not be able to set the state of the modified state");
    }
    catch (Exception e) {}
  }

  @Test
  public void setValue() {
    new EmpDept();

    final Entity department = new Entity(EmpDept.T_DEPARTMENT);
    department.setValue(EmpDept.DEPARTMENT_ID, -10);

    final Entity employee = new Entity(EmpDept.T_EMPLOYEE);
    try {
      employee.setValue(EmpDept.EMPLOYEE_NAME, 1);
      fail();
    }
    catch (IllegalArgumentException e) {}
    try {
      employee.setValue(EmpDept.EMPLOYEE_NAME, 1d);
      fail();
    }
    catch (IllegalArgumentException e) {}
    try {
      employee.setValue(EmpDept.EMPLOYEE_NAME, false);
      fail();
    }
    catch (IllegalArgumentException e) {}
    try {
      employee.setValue(EmpDept.EMPLOYEE_NAME, 'c');
      fail();
    }
    catch (IllegalArgumentException e) {}
    try {
      employee.setValue(EmpDept.EMPLOYEE_NAME, department);
      fail();
    }
    catch (IllegalArgumentException e) {}
    try {
      employee.setValue(EmpDept.EMPLOYEE_NAME, new Timestamp(System.currentTimeMillis()));
      fail();
    }
    catch (IllegalArgumentException e) {}
    try {
      employee.setValue(EmpDept.EMPLOYEE_SALARY, "test");
      fail();
    }
    catch (IllegalArgumentException e) {}

    employee.setValue(EmpDept.EMPLOYEE_COMMISSION, 1200d);
    assertEquals(employee.getValue(EmpDept.EMPLOYEE_COMMISSION), 1200d);

    employee.setValue(EmpDept.EMPLOYEE_DEPARTMENT_FK, department);
    assertEquals(employee.getValue(EmpDept.EMPLOYEE_DEPARTMENT_FK), department);

    final Timestamp date = new Timestamp(DateUtil.floorDate(new Date()).getTime());
    employee.setValue(EmpDept.EMPLOYEE_HIREDATE, date);
    assertEquals(employee.getValue(EmpDept.EMPLOYEE_HIREDATE), date);

    employee.setValue(EmpDept.EMPLOYEE_ID, 123);
    assertEquals(employee.getValue(EmpDept.EMPLOYEE_ID), 123);

    employee.setValue(EmpDept.EMPLOYEE_NAME, "noname");
    assertEquals(employee.getValue(EmpDept.EMPLOYEE_NAME), "noname");
  }
}