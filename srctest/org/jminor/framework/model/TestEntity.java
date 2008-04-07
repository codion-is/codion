/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.model;

import junit.framework.TestCase;
import org.jminor.common.db.Database;
import org.jminor.common.model.Util;

import java.util.Date;

public class TestEntity extends TestCase {

  public static Entity getTestMasterEntity(final int id, final int intValue, final double doubleValue,
                                           final String stringValue, final Date shortDateValue, final Date longDateValue,
                                           final Type.Boolean booleanValue, final Entity entityValue) {
    final Entity ret = new Entity(ModelTestDomain.T_TEST_DETAIL);
    ret.setValue(ModelTestDomain.TEST_DETAIL_ID, id);
    ret.setValue(ModelTestDomain.TEST_DETAIL_INT, intValue);
    ret.setValue(ModelTestDomain.TEST_DETAIL_DOUBLE, doubleValue);
    ret.setValue(ModelTestDomain.TEST_DETAIL_STRING, stringValue);
    ret.setValue(ModelTestDomain.TEST_DETAIL_SHORT_DATE, shortDateValue);
    ret.setValue(ModelTestDomain.TEST_DETAIL_LONG_DATE, longDateValue);
    ret.setValue(ModelTestDomain.TEST_DETAIL_BOOLEAN, booleanValue);
    ret.setValue(ModelTestDomain.TEST_DETAIL_ENTITY_REF, entityValue);

    return ret;
  }

  public TestEntity(String name) {
    super(name);
    new ModelTestDomain();
  }

  public void testEntity() throws Exception {
    final int idValue = 1;
    final int intValue = 2;
    final double doubleValue = 1.2;
    final String stringValue = "string";
    final Date shortDateValue = new Date();
    final Date longDateValue = new Date();
    final Type.Boolean booleanValue = Type.Boolean.TRUE;
    final int referenceId = 2;

    final Entity referencedEntityValue = new Entity(ModelTestDomain.T_TEST_MASTER);
    referencedEntityValue.setValue(ModelTestDomain.TEST_MASTER_ID, referenceId);
    referencedEntityValue.setValue(ModelTestDomain.TEST_MASTER_NAME, stringValue);

    Entity test = new Entity(ModelTestDomain.T_TEST_DETAIL);
    //assert not modified
    assertFalse(test.isModified());
    //assert default values
    assertEquals(test.getValue(ModelTestDomain.TEST_DETAIL_ID), 420);
    assertEquals(test.getValue(ModelTestDomain.TEST_DETAIL_BOOLEAN), Type.Boolean.TRUE);

    final Entity testEntity = getTestMasterEntity(idValue, intValue, doubleValue,
            stringValue, shortDateValue, longDateValue, booleanValue, referencedEntityValue);
    //assert types
    assertEquals(testEntity.getPrimaryKey().getProperty(ModelTestDomain.TEST_DETAIL_ID).getPropertyType(), Type.INT);
    assertEquals(testEntity.getProperty(ModelTestDomain.TEST_DETAIL_INT).getPropertyType(), Type.INT);
    assertEquals(testEntity.getProperty(ModelTestDomain.TEST_DETAIL_DOUBLE).getPropertyType(), Type.DOUBLE);
    assertEquals(testEntity.getProperty(ModelTestDomain.TEST_DETAIL_STRING).getPropertyType(), Type.STRING);
    assertEquals(testEntity.getProperty(ModelTestDomain.TEST_DETAIL_SHORT_DATE).getPropertyType(), Type.SHORT_DATE);
    assertEquals(testEntity.getProperty(ModelTestDomain.TEST_DETAIL_LONG_DATE).getPropertyType(), Type.LONG_DATE);
    assertEquals(testEntity.getProperty(ModelTestDomain.TEST_DETAIL_BOOLEAN).getPropertyType(), Type.BOOLEAN);
    assertEquals(testEntity.getProperty(ModelTestDomain.TEST_DETAIL_ENTITY_REF).getPropertyType(), Type.ENTITY);
    assertEquals(testEntity.getProperty(ModelTestDomain.TEST_DETAIL_ENTITY_ID).getPropertyType(), Type.INT);
    assertEquals(testEntity.getProperty(ModelTestDomain.TEST_DETAIL_MASTER_NAME_DENORM).getPropertyType(), Type.STRING);

    //assert column names
    assertEquals(testEntity.getPrimaryKey().getProperty(ModelTestDomain.TEST_DETAIL_ID).propertyID, ModelTestDomain.TEST_DETAIL_ID);
    assertEquals(testEntity.getProperty(ModelTestDomain.TEST_DETAIL_INT).propertyID, ModelTestDomain.TEST_DETAIL_INT);
    assertEquals(testEntity.getProperty(ModelTestDomain.TEST_DETAIL_DOUBLE).propertyID, ModelTestDomain.TEST_DETAIL_DOUBLE);
    assertEquals(testEntity.getProperty(ModelTestDomain.TEST_DETAIL_STRING).propertyID, ModelTestDomain.TEST_DETAIL_STRING);
    assertEquals(testEntity.getProperty(ModelTestDomain.TEST_DETAIL_SHORT_DATE).propertyID, ModelTestDomain.TEST_DETAIL_SHORT_DATE);
    assertEquals(testEntity.getProperty(ModelTestDomain.TEST_DETAIL_LONG_DATE).propertyID, ModelTestDomain.TEST_DETAIL_LONG_DATE);
    assertEquals(testEntity.getProperty(ModelTestDomain.TEST_DETAIL_BOOLEAN).propertyID, ModelTestDomain.TEST_DETAIL_BOOLEAN);
    assertEquals(testEntity.getProperty(ModelTestDomain.TEST_DETAIL_ENTITY_ID).propertyID, ModelTestDomain.TEST_DETAIL_ENTITY_ID);
    assertEquals(testEntity.getProperty(ModelTestDomain.TEST_DETAIL_MASTER_NAME_DENORM).propertyID, ModelTestDomain.TEST_DETAIL_MASTER_NAME_DENORM);

    //assert captions
    assertNull(testEntity.getPrimaryKey().getProperty(ModelTestDomain.TEST_DETAIL_ID).getCaption());
    assertEquals(testEntity.getProperty(ModelTestDomain.TEST_DETAIL_INT).getCaption(), ModelTestDomain.TEST_DETAIL_INT);
    assertEquals(testEntity.getProperty(ModelTestDomain.TEST_DETAIL_DOUBLE).getCaption(), ModelTestDomain.TEST_DETAIL_DOUBLE);
    assertEquals(testEntity.getProperty(ModelTestDomain.TEST_DETAIL_STRING).getCaption(), ModelTestDomain.TEST_DETAIL_STRING);
    assertEquals(testEntity.getProperty(ModelTestDomain.TEST_DETAIL_SHORT_DATE).getCaption(), ModelTestDomain.TEST_DETAIL_SHORT_DATE);
    assertEquals(testEntity.getProperty(ModelTestDomain.TEST_DETAIL_LONG_DATE).getCaption(), ModelTestDomain.TEST_DETAIL_LONG_DATE);
    assertEquals(testEntity.getProperty(ModelTestDomain.TEST_DETAIL_BOOLEAN).getCaption(), ModelTestDomain.TEST_DETAIL_BOOLEAN);
    assertEquals(testEntity.getProperty(ModelTestDomain.TEST_DETAIL_ENTITY_REF).getCaption(), ModelTestDomain.TEST_DETAIL_ENTITY_REF);
    assertEquals(testEntity.getProperty(ModelTestDomain.TEST_DETAIL_MASTER_NAME_DENORM).getCaption(), ModelTestDomain.TEST_DETAIL_MASTER_NAME_DENORM);

    //assert hidden status
    assertTrue(testEntity.getPrimaryKey().getProperty(ModelTestDomain.TEST_DETAIL_ID).isHidden());
    assertFalse(testEntity.getProperty(ModelTestDomain.TEST_DETAIL_INT).isHidden());
    assertFalse(testEntity.getProperty(ModelTestDomain.TEST_DETAIL_DOUBLE).isHidden());
    assertFalse(testEntity.getProperty(ModelTestDomain.TEST_DETAIL_STRING).isHidden());
    assertFalse(testEntity.getProperty(ModelTestDomain.TEST_DETAIL_SHORT_DATE).isHidden());
    assertFalse(testEntity.getProperty(ModelTestDomain.TEST_DETAIL_LONG_DATE).isHidden());
    assertFalse(testEntity.getProperty(ModelTestDomain.TEST_DETAIL_BOOLEAN).isHidden());
    assertFalse(testEntity.getProperty(ModelTestDomain.TEST_DETAIL_ENTITY_REF).isHidden());
    assertFalse(testEntity.getProperty(ModelTestDomain.TEST_DETAIL_MASTER_NAME_DENORM).isHidden());

    //assert values
    assertEquals(testEntity.getValue(ModelTestDomain.TEST_DETAIL_ID), idValue);
    assertEquals(testEntity.getValue(ModelTestDomain.TEST_DETAIL_INT), intValue);
    assertEquals(testEntity.getValue(ModelTestDomain.TEST_DETAIL_DOUBLE), doubleValue);
    assertEquals(testEntity.getValue(ModelTestDomain.TEST_DETAIL_STRING), stringValue);
    assertEquals(testEntity.getValue(ModelTestDomain.TEST_DETAIL_SHORT_DATE), shortDateValue);
    assertEquals(testEntity.getValue(ModelTestDomain.TEST_DETAIL_LONG_DATE), longDateValue);
    assertEquals(testEntity.getValue(ModelTestDomain.TEST_DETAIL_BOOLEAN), booleanValue);
    assertEquals(testEntity.getValue(ModelTestDomain.TEST_DETAIL_ENTITY_REF), referencedEntityValue);
    assertEquals(testEntity.getValue(ModelTestDomain.TEST_DETAIL_MASTER_NAME_DENORM), stringValue);
    assertFalse(testEntity.isValueNull(ModelTestDomain.TEST_DETAIL_ENTITY_ID));

    boolean exception = false;
    try {
      testEntity.setValue(ModelTestDomain.TEST_DETAIL_MASTER_NAME_DENORM, "hello");
    }
    catch (Exception e) {
      exception = true;
    }
    assertTrue("Set value for a denormalized property should cause an error", exception);

    //assert dml
    final String shortDateStringSql = Database.getSQLDateString(shortDateValue, false);
    final String longDateStringSql = Database.getSQLDateString(longDateValue, true);
    assertEquals(EntityUtil.getInsertSQL(testEntity),
            "insert into " + ModelTestDomain.T_TEST_DETAIL
                    + "(int, double, string, short_date, long_date, boolean, entity_id, id)"
                    + " values(2, 1.2, 'string', " + shortDateStringSql + ", " + longDateStringSql + ", 1, 2, 1)");
    assertEquals(EntityUtil.getDeleteSQL(testEntity),
            "delete from " + ModelTestDomain.T_TEST_DETAIL + " where (id = 1)");
    exception = false;
    try {
      EntityUtil.getUpdateSQL(testEntity);
    }
    catch (Exception e) {
      exception = true;
    }
    assertTrue("Should get an exception when trying to get update sql of a non-modified entity", exception);

    testEntity.setValue(ModelTestDomain.TEST_DETAIL_INT, 42);
    testEntity.setValue(ModelTestDomain.TEST_DETAIL_STRING, "newString");
    assertEquals(EntityUtil.getUpdateSQL(testEntity), "update " + ModelTestDomain.T_TEST_DETAIL
            + " set int = 42, string = 'newString' where (id = 1)");
    testEntity.setValue(ModelTestDomain.TEST_DETAIL_STRING, "string");
    assertEquals(EntityUtil.getUpdateSQL(testEntity), "update " + ModelTestDomain.T_TEST_DETAIL
            + " set int = 42 where (id = 1)");

    //test setAs()
    test = new Entity(ModelTestDomain.T_TEST_DETAIL);
    test.setAs(testEntity);
    assertTrue("Entities should be equal after .setAs()", Util.equal(test, testEntity));
    assertTrue("Entity property values should be equal after .setAs()", test.propertyValuesEqual(testEntity));

    //test copy()
    final Entity test2 = testEntity.getCopy();
    assertFalse("Entity copy should not be == the original", test2 == testEntity);
    assertTrue("Entities should be equal after .getCopy()", Util.equal(test2, testEntity));
    assertTrue("Entity property values should be equal after .getCopy()", test2.propertyValuesEqual(testEntity));

    //test propogate entity reference/denormalized values
    testEntity.setValue(ModelTestDomain.TEST_DETAIL_ENTITY_REF, null);
    assertTrue(testEntity.isValueNull(ModelTestDomain.TEST_DETAIL_ENTITY_ID));
    assertTrue(testEntity.isValueNull(ModelTestDomain.TEST_DETAIL_MASTER_NAME_DENORM));

    testEntity.setValue(ModelTestDomain.TEST_DETAIL_ENTITY_REF, referencedEntityValue);
    assertFalse(testEntity.isValueNull(ModelTestDomain.TEST_DETAIL_ENTITY_ID));
    assertEquals(testEntity.getValue(ModelTestDomain.TEST_DETAIL_ENTITY_ID),
            referencedEntityValue.getValue(ModelTestDomain.TEST_MASTER_ID));
    assertEquals(testEntity.getValue(ModelTestDomain.TEST_DETAIL_MASTER_NAME_DENORM),
            referencedEntityValue.getValue(ModelTestDomain.TEST_MASTER_NAME));
  }
}