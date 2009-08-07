/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.model;

import org.jminor.common.model.Util;

import junit.framework.TestCase;

import java.util.Date;

public class EntityTest extends TestCase {

  public static Entity getDetailEntity(final int id, final Integer intValue, final Double doubleValue,
                                           final String stringValue, final Date shortDateValue, final Date longDateValue,
                                           final Type.Boolean booleanValue, final Entity entityValue) {
    final Entity ret = new Entity(EntityTestDomain.T_DETAIL);
    ret.setValue(EntityTestDomain.DETAIL_ID, id);
    ret.setValue(EntityTestDomain.DETAIL_INT, intValue);
    ret.setValue(EntityTestDomain.DETAIL_DOUBLE, doubleValue);
    ret.setValue(EntityTestDomain.DETAIL_STRING, stringValue);
    ret.setValue(EntityTestDomain.DETAIL_SHORT_DATE, shortDateValue);
    ret.setValue(EntityTestDomain.DETAIL_LONG_DATE, longDateValue);
    ret.setValue(EntityTestDomain.DETAIL_BOOLEAN, booleanValue);
    ret.setValue(EntityTestDomain.DETAIL_ENTITY_FK, entityValue);

    return ret;
  }

  public EntityTest() {
    new EntityTestDomain();
  }

  public void testEntity() throws Exception {
    final int detailId = 1;
    final int detailInt = 2;
    final double detailDouble = 1.2;
    final String detailString = "string";
    final Date detailShortDate = new Date();
    final Date detailLongDate = new Date();
    final Type.Boolean detailBoolean = Type.Boolean.TRUE;

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
            detailString, detailShortDate, detailLongDate, detailBoolean, referencedEntityValue);
    //assert types
    assertEquals(testEntity.getPrimaryKey().getProperty(EntityTestDomain.DETAIL_ID).getPropertyType(), Type.INT);
    assertEquals(testEntity.getProperty(EntityTestDomain.DETAIL_INT).getPropertyType(), Type.INT);
    assertEquals(testEntity.getProperty(EntityTestDomain.DETAIL_DOUBLE).getPropertyType(), Type.DOUBLE);
    assertEquals(testEntity.getProperty(EntityTestDomain.DETAIL_STRING).getPropertyType(), Type.STRING);
    assertEquals(testEntity.getProperty(EntityTestDomain.DETAIL_SHORT_DATE).getPropertyType(), Type.SHORT_DATE);
    assertEquals(testEntity.getProperty(EntityTestDomain.DETAIL_LONG_DATE).getPropertyType(), Type.LONG_DATE);
    assertEquals(testEntity.getProperty(EntityTestDomain.DETAIL_BOOLEAN).getPropertyType(), Type.BOOLEAN);
    assertEquals(testEntity.getProperty(EntityTestDomain.DETAIL_ENTITY_FK).getPropertyType(), Type.ENTITY);
    assertEquals(testEntity.getProperty(EntityTestDomain.DETAIL_ENTITY_ID).getPropertyType(), Type.INT);
    assertEquals(testEntity.getProperty(EntityTestDomain.DETAIL_MASTER_NAME).getPropertyType(), Type.STRING);
    assertEquals(testEntity.getProperty(EntityTestDomain.DETAIL_MASTER_CODE).getPropertyType(), Type.INT);

    //assert column names
    assertEquals(testEntity.getPrimaryKey().getProperty(EntityTestDomain.DETAIL_ID).propertyID, EntityTestDomain.DETAIL_ID);
    assertEquals(testEntity.getProperty(EntityTestDomain.DETAIL_INT).propertyID, EntityTestDomain.DETAIL_INT);
    assertEquals(testEntity.getProperty(EntityTestDomain.DETAIL_DOUBLE).propertyID, EntityTestDomain.DETAIL_DOUBLE);
    assertEquals(testEntity.getProperty(EntityTestDomain.DETAIL_STRING).propertyID, EntityTestDomain.DETAIL_STRING);
    assertEquals(testEntity.getProperty(EntityTestDomain.DETAIL_SHORT_DATE).propertyID, EntityTestDomain.DETAIL_SHORT_DATE);
    assertEquals(testEntity.getProperty(EntityTestDomain.DETAIL_LONG_DATE).propertyID, EntityTestDomain.DETAIL_LONG_DATE);
    assertEquals(testEntity.getProperty(EntityTestDomain.DETAIL_BOOLEAN).propertyID, EntityTestDomain.DETAIL_BOOLEAN);
    assertEquals(testEntity.getProperty(EntityTestDomain.DETAIL_ENTITY_ID).propertyID, EntityTestDomain.DETAIL_ENTITY_ID);
    assertEquals(testEntity.getProperty(EntityTestDomain.DETAIL_MASTER_NAME).propertyID, EntityTestDomain.DETAIL_MASTER_NAME);
    assertEquals(testEntity.getProperty(EntityTestDomain.DETAIL_MASTER_CODE).propertyID, EntityTestDomain.DETAIL_MASTER_CODE);

    //assert captions
    assertNull(testEntity.getPrimaryKey().getProperty(EntityTestDomain.DETAIL_ID).getCaption());
    assertEquals(testEntity.getProperty(EntityTestDomain.DETAIL_INT).getCaption(), EntityTestDomain.DETAIL_INT);
    assertEquals(testEntity.getProperty(EntityTestDomain.DETAIL_DOUBLE).getCaption(), EntityTestDomain.DETAIL_DOUBLE);
    assertEquals(testEntity.getProperty(EntityTestDomain.DETAIL_STRING).getCaption(), EntityTestDomain.DETAIL_STRING);
    assertEquals(testEntity.getProperty(EntityTestDomain.DETAIL_SHORT_DATE).getCaption(), EntityTestDomain.DETAIL_SHORT_DATE);
    assertEquals(testEntity.getProperty(EntityTestDomain.DETAIL_LONG_DATE).getCaption(), EntityTestDomain.DETAIL_LONG_DATE);
    assertEquals(testEntity.getProperty(EntityTestDomain.DETAIL_BOOLEAN).getCaption(), EntityTestDomain.DETAIL_BOOLEAN);
    assertEquals(testEntity.getProperty(EntityTestDomain.DETAIL_ENTITY_FK).getCaption(), EntityTestDomain.DETAIL_ENTITY_FK);
    assertEquals(testEntity.getProperty(EntityTestDomain.DETAIL_MASTER_NAME).getCaption(), EntityTestDomain.DETAIL_MASTER_NAME);
    assertEquals(testEntity.getProperty(EntityTestDomain.DETAIL_MASTER_CODE).getCaption(), EntityTestDomain.DETAIL_MASTER_CODE);

    //assert hidden status
    assertTrue(testEntity.getPrimaryKey().getProperty(EntityTestDomain.DETAIL_ID).isHidden());
    assertFalse(testEntity.getProperty(EntityTestDomain.DETAIL_INT).isHidden());
    assertFalse(testEntity.getProperty(EntityTestDomain.DETAIL_DOUBLE).isHidden());
    assertFalse(testEntity.getProperty(EntityTestDomain.DETAIL_STRING).isHidden());
    assertFalse(testEntity.getProperty(EntityTestDomain.DETAIL_SHORT_DATE).isHidden());
    assertFalse(testEntity.getProperty(EntityTestDomain.DETAIL_LONG_DATE).isHidden());
    assertFalse(testEntity.getProperty(EntityTestDomain.DETAIL_BOOLEAN).isHidden());
    assertFalse(testEntity.getProperty(EntityTestDomain.DETAIL_ENTITY_FK).isHidden());
    assertFalse(testEntity.getProperty(EntityTestDomain.DETAIL_MASTER_NAME).isHidden());
    assertFalse(testEntity.getProperty(EntityTestDomain.DETAIL_MASTER_CODE).isHidden());

    //assert values
    assertEquals(testEntity.getValue(EntityTestDomain.DETAIL_ID), detailId);
    assertEquals(testEntity.getValue(EntityTestDomain.DETAIL_INT), detailInt);
    assertEquals(testEntity.getValue(EntityTestDomain.DETAIL_DOUBLE), detailDouble);
    assertEquals(testEntity.getValue(EntityTestDomain.DETAIL_STRING), detailString);
    assertEquals(testEntity.getValue(EntityTestDomain.DETAIL_SHORT_DATE), detailShortDate);
    assertEquals(testEntity.getValue(EntityTestDomain.DETAIL_LONG_DATE), detailLongDate);
    assertEquals(testEntity.getValue(EntityTestDomain.DETAIL_BOOLEAN), detailBoolean);
    assertEquals(testEntity.getValue(EntityTestDomain.DETAIL_ENTITY_FK), referencedEntityValue);
    assertEquals(testEntity.getValue(EntityTestDomain.DETAIL_MASTER_NAME), masterName);
    assertEquals(testEntity.getValue(EntityTestDomain.DETAIL_MASTER_CODE), masterCode);
    assertFalse(testEntity.isValueNull(EntityTestDomain.DETAIL_ENTITY_ID));

    boolean exception = false;
    try {
      testEntity.setValue(EntityTestDomain.DETAIL_MASTER_NAME, "hello");
    }
    catch (Exception e) {
      exception = true;
    }
    assertTrue("Set value for a denormalized view property should cause an error", exception);

    exception = false;
    try {
      testEntity.setValue(EntityTestDomain.DETAIL_MASTER_CODE, 2);
    }
    catch (Exception e) {
      exception = true;
    }
    assertTrue("Set value for a denormalized property should cause an error", exception);

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
  }
}