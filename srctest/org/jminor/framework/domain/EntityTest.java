/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain;

import org.jminor.common.model.Util;
import org.jminor.common.model.formats.DateFormats;
import org.jminor.framework.demos.empdept.domain.EmpDept;

import static org.junit.Assert.*;
import org.junit.Test;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
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
    assertEquals(testEntity.getPrimaryKey().getProperty(EntityTestDomain.DETAIL_ID).getPropertyType(), Type.INT);
    assertEquals(testEntity.getProperty(EntityTestDomain.DETAIL_INT).getPropertyType(), Type.INT);
    assertEquals(testEntity.getProperty(EntityTestDomain.DETAIL_DOUBLE).getPropertyType(), Type.DOUBLE);
    assertEquals(testEntity.getProperty(EntityTestDomain.DETAIL_STRING).getPropertyType(), Type.STRING);
    assertEquals(testEntity.getProperty(EntityTestDomain.DETAIL_DATE).getPropertyType(), Type.DATE);
    assertEquals(testEntity.getProperty(EntityTestDomain.DETAIL_TIMESTAMP).getPropertyType(), Type.TIMESTAMP);
    assertEquals(testEntity.getProperty(EntityTestDomain.DETAIL_BOOLEAN).getPropertyType(), Type.BOOLEAN);
    assertEquals(testEntity.getProperty(EntityTestDomain.DETAIL_ENTITY_FK).getPropertyType(), Type.ENTITY);
    assertEquals(testEntity.getProperty(EntityTestDomain.DETAIL_ENTITY_ID).getPropertyType(), Type.INT);
    assertEquals(testEntity.getProperty(EntityTestDomain.DETAIL_MASTER_NAME).getPropertyType(), Type.STRING);
    assertEquals(testEntity.getProperty(EntityTestDomain.DETAIL_MASTER_CODE).getPropertyType(), Type.INT);

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
      test2.getModifiedState().setActive(true);
      fail("Should not be able to set the state of the modified state");
    }
    catch (Exception e) {}
  }

  @Test
  public void stringProvider() {
    new EmpDept();
    final Entity department = new Entity(EmpDept.T_DEPARTMENT);
    department.setValue(EmpDept.DEPARTMENT_ID, -10);
    department.setValue(EmpDept.DEPARTMENT_LOCATION, "Reykjavik");
    department.setValue(EmpDept.DEPARTMENT_NAME, "Sales");

    final Entity employee = new Entity(EmpDept.T_EMPLOYEE);
    final Date hiredate = new Date();
    employee.setValue(EmpDept.EMPLOYEE_DEPARTMENT_FK, department);
    employee.setValue(EmpDept.EMPLOYEE_NAME, "Darri");
    employee.setValue(EmpDept.EMPLOYEE_HIREDATE, hiredate);

    final DateFormat dateFormat = new SimpleDateFormat(DateFormats.SHORT_DOT);

    final Entity.StringProvider employeeToString = new Entity.StringProvider(EmpDept.EMPLOYEE_NAME)
            .addText(" (department: ").addValue(EmpDept.EMPLOYEE_DEPARTMENT_FK).addText(", location: ")
            .addReferencedValue(EmpDept.EMPLOYEE_DEPARTMENT_FK, EmpDept.DEPARTMENT_LOCATION).addText(", hiredate: ")
            .addFormattedValue(EmpDept.EMPLOYEE_HIREDATE, dateFormat).addText(")");

    assertEquals("Darri (department: Sales, location: Reykjavik, hiredate: " + dateFormat.format(hiredate) + ")", employeeToString.toString(employee));
  }
}