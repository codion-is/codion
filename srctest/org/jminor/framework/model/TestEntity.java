/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.model;

import org.jminor.common.db.DbUtil;
import org.jminor.common.model.Util;
import org.jminor.common.model.formats.LongDateFormat;
import org.jminor.common.model.formats.ShortDashDateFormat;

import junit.framework.TestCase;

import java.util.Date;

public class TestEntity extends TestCase {

  static {
    new ModelTestDomain();
  }

  public static Entity getTestMasterEntity(final int id, final int intValue, final double doubleValue,
                                           final String stringValue, final Date shortDateValue, final Date longDateValue,
                                           final Type.Boolean booleanValue, final Entity entityValue,
                                           final int entityValueId) {
    final Entity ret = new Entity(ModelTestDomain.T_TEST_MASTER_ENTITY);
    ret.setValue(ModelTestDomain.ID_PROP, id);
    ret.setValue(ModelTestDomain.INT_PROP, intValue);
    ret.setValue(ModelTestDomain.DOUBLE_PROP, doubleValue);
    ret.setValue(ModelTestDomain.STRING_PROP, stringValue);
    ret.setValue(ModelTestDomain.SHORT_DATE_PROP, shortDateValue);
    ret.setValue(ModelTestDomain.LONG_DATE_PROP, longDateValue);
    ret.setValue(ModelTestDomain.BOOLEAN_PROP, booleanValue);
    ret.setValue(ModelTestDomain.ENTITY_REF, entityValue);
    ret.setValue(ModelTestDomain.ENTITY_ID_PROP, entityValueId);

    return ret;
  }

  public TestEntity(String name) {
    super(name);
  }

  public void testEntity() throws Exception {
    final int idValue = 1;
    final int intValue = 2;
    final double doubleValue = 1.2;
    final String stringValue = "string";
    final Date shortDateValue = new Date();
    final String shortDateString = ShortDashDateFormat.get().format(shortDateValue);
    final Date longDateValue = new Date();
    final String longDateString = LongDateFormat.get().format(longDateValue);
    final Type.Boolean booleanValue = Type.Boolean.TRUE;
    final int referenceId = 2;

    final Entity referencedEntityValue = new Entity(ModelTestDomain.T_TEST_DETAIL_ENTITY);
    referencedEntityValue.setValue(ModelTestDomain.ID, referenceId);
    referencedEntityValue.setValue(ModelTestDomain.NAME, stringValue);

    Entity test = new Entity(ModelTestDomain.T_TEST_MASTER_ENTITY);
    //assert not modified
    assertFalse(test.isModified());
    //assert default values
    assertEquals(test.getValue(ModelTestDomain.ID_PROP), 420);
    assertEquals(test.getValue(ModelTestDomain.BOOLEAN_PROP), Type.Boolean.TRUE);

    final Entity testEntity = getTestMasterEntity(idValue, intValue, doubleValue,
            stringValue, shortDateValue, longDateValue, booleanValue, referencedEntityValue, referenceId);
    //assert types
    assertEquals(testEntity.getPrimaryKey().getProperty(ModelTestDomain.ID_PROP).getPropertyType(), Type.INT);
    assertEquals(testEntity.getProperty(ModelTestDomain.INT_PROP).getPropertyType(), Type.INT);
    assertEquals(testEntity.getProperty(ModelTestDomain.DOUBLE_PROP).getPropertyType(), Type.DOUBLE);
    assertEquals(testEntity.getProperty(ModelTestDomain.STRING_PROP).getPropertyType(), Type.STRING);
    assertEquals(testEntity.getProperty(ModelTestDomain.SHORT_DATE_PROP).getPropertyType(), Type.SHORT_DATE);
    assertEquals(testEntity.getProperty(ModelTestDomain.LONG_DATE_PROP).getPropertyType(), Type.LONG_DATE);
    assertEquals(testEntity.getProperty(ModelTestDomain.BOOLEAN_PROP).getPropertyType(), Type.BOOLEAN);
    assertEquals(testEntity.getProperty(ModelTestDomain.ENTITY_REF).getPropertyType(), Type.ENTITY);
    assertEquals(testEntity.getProperty(ModelTestDomain.ENTITY_ID_PROP).getPropertyType(), Type.INT);
    assertEquals(testEntity.getProperty(ModelTestDomain.DENORM_PROP).getPropertyType(), Type.STRING);

    //assert column names
    assertEquals(testEntity.getPrimaryKey().getProperty(ModelTestDomain.ID_PROP).propertyID, ModelTestDomain.ID_PROP);
    assertEquals(testEntity.getProperty(ModelTestDomain.INT_PROP).propertyID, ModelTestDomain.INT_PROP);
    assertEquals(testEntity.getProperty(ModelTestDomain.DOUBLE_PROP).propertyID, ModelTestDomain.DOUBLE_PROP);
    assertEquals(testEntity.getProperty(ModelTestDomain.STRING_PROP).propertyID, ModelTestDomain.STRING_PROP);
    assertEquals(testEntity.getProperty(ModelTestDomain.SHORT_DATE_PROP).propertyID, ModelTestDomain.SHORT_DATE_PROP);
    assertEquals(testEntity.getProperty(ModelTestDomain.LONG_DATE_PROP).propertyID, ModelTestDomain.LONG_DATE_PROP);
    assertEquals(testEntity.getProperty(ModelTestDomain.BOOLEAN_PROP).propertyID, ModelTestDomain.BOOLEAN_PROP);
    assertEquals(testEntity.getProperty(ModelTestDomain.ENTITY_ID_PROP).propertyID, ModelTestDomain.ENTITY_ID_PROP);
    assertEquals(testEntity.getProperty(ModelTestDomain.DENORM_PROP).propertyID, ModelTestDomain.DENORM_PROP);

    //assert captions
    assertNull(testEntity.getPrimaryKey().getProperty(ModelTestDomain.ID_PROP).getCaption());
    assertEquals(testEntity.getProperty(ModelTestDomain.INT_PROP).getCaption(), ModelTestDomain.INT_PROP);
    assertEquals(testEntity.getProperty(ModelTestDomain.DOUBLE_PROP).getCaption(), ModelTestDomain.DOUBLE_PROP);
    assertEquals(testEntity.getProperty(ModelTestDomain.STRING_PROP).getCaption(), ModelTestDomain.STRING_PROP);
    assertEquals(testEntity.getProperty(ModelTestDomain.SHORT_DATE_PROP).getCaption(), ModelTestDomain.SHORT_DATE_PROP);
    assertEquals(testEntity.getProperty(ModelTestDomain.LONG_DATE_PROP).getCaption(), ModelTestDomain.LONG_DATE_PROP);
    assertEquals(testEntity.getProperty(ModelTestDomain.BOOLEAN_PROP).getCaption(), ModelTestDomain.BOOLEAN_PROP);
    assertEquals(testEntity.getProperty(ModelTestDomain.ENTITY_REF).getCaption(), ModelTestDomain.ENTITY_REF);
    assertEquals(testEntity.getProperty(ModelTestDomain.DENORM_PROP).getCaption(), ModelTestDomain.DENORM_PROP);

    //assert hidden status
    assertTrue(testEntity.getPrimaryKey().getProperty(ModelTestDomain.ID_PROP).isHidden());
    assertFalse(testEntity.getProperty(ModelTestDomain.INT_PROP).isHidden());
    assertFalse(testEntity.getProperty(ModelTestDomain.DOUBLE_PROP).isHidden());
    assertFalse(testEntity.getProperty(ModelTestDomain.STRING_PROP).isHidden());
    assertFalse(testEntity.getProperty(ModelTestDomain.SHORT_DATE_PROP).isHidden());
    assertFalse(testEntity.getProperty(ModelTestDomain.LONG_DATE_PROP).isHidden());
    assertFalse(testEntity.getProperty(ModelTestDomain.BOOLEAN_PROP).isHidden());
    assertFalse(testEntity.getProperty(ModelTestDomain.ENTITY_REF).isHidden());
    assertFalse(testEntity.getProperty(ModelTestDomain.DENORM_PROP).isHidden());

    //assert values
    assertEquals(testEntity.getValue(ModelTestDomain.ID_PROP), idValue);
    assertEquals(testEntity.getValue(ModelTestDomain.INT_PROP), intValue);
    assertEquals(testEntity.getValue(ModelTestDomain.DOUBLE_PROP), doubleValue);
    assertEquals(testEntity.getValue(ModelTestDomain.STRING_PROP), stringValue);
    assertEquals(testEntity.getValue(ModelTestDomain.SHORT_DATE_PROP), shortDateValue);
    assertEquals(testEntity.getValue(ModelTestDomain.LONG_DATE_PROP), longDateValue);
    assertEquals(testEntity.getValue(ModelTestDomain.BOOLEAN_PROP), booleanValue);
    assertEquals(testEntity.getValue(ModelTestDomain.ENTITY_REF), referencedEntityValue);
    assertEquals(testEntity.getValue(ModelTestDomain.DENORM_PROP), stringValue);
    assertFalse(testEntity.isValueNull(ModelTestDomain.ENTITY_ID_PROP));

    boolean exception = false;
    try {
      testEntity.setValue(ModelTestDomain.DENORM_PROP, "hello");
    }
    catch (Exception e) {
      exception = true;
    }
    assertTrue("Set value for a denormalized property should cause an error", exception);

    //assert dml
    final String shortDateStringSql = DbUtil.isMySQL()
            ? "str_to_date('" + shortDateString + "', '%d-%m-%Y')"
            : "to_date('" + shortDateString + "', 'DD-MM-YYYY')";
    final String longDateStringSql = DbUtil.isMySQL()
            ? "str_to_date('" + longDateString + "', '%d-%m-%Y %H:%i')"
            : "to_date('" +longDateString + "', 'DD-MM-YYYY HH24:MI')";
    assertEquals(EntityUtil.getInsertSQL(testEntity),
            "insert into " + ModelTestDomain.T_TEST_MASTER_ENTITY
                    + "(int, double, string, short_date, long_date, boolean, entity_id, id)"
                    + " values(2, 1.2, 'string', " + shortDateStringSql + ", " + longDateStringSql + ", 1, 2, 1)");
    assertEquals(EntityUtil.getDeleteSQL(testEntity), "delete from " + ModelTestDomain.T_TEST_MASTER_ENTITY + " where (id = 1)");
    exception = false;
    try {
      EntityUtil.getUpdateSQL(testEntity);
    }
    catch (Exception e) {
      exception = true;
    }
    assertTrue("Should get an exception when trying to get update sql of a non-modified entity", exception);

    testEntity.setValue(ModelTestDomain.INT_PROP, 42);
    testEntity.setValue(ModelTestDomain.STRING_PROP, "newString");
    assertEquals(EntityUtil.getUpdateSQL(testEntity), "update " + ModelTestDomain.T_TEST_MASTER_ENTITY + " set int = 42, string = 'newString' where (id = 1)");
    testEntity.setValue(ModelTestDomain.STRING_PROP, "string");
    assertEquals(EntityUtil.getUpdateSQL(testEntity), "update " + ModelTestDomain.T_TEST_MASTER_ENTITY + " set int = 42 where (id = 1)");

    //test setAs()
    test = new Entity(ModelTestDomain.T_TEST_MASTER_ENTITY);
    test.setAs(testEntity);
    assertTrue("Entities should be equal after .setAs()", Util.equal(test, testEntity));
    assertTrue("Entity property values should be equal after .setAs()", test.propertyValuesEqual(testEntity));

    //test copy()
    final Entity test2 = testEntity.getCopy();
    assertFalse("Entity copy should not be == the original", test2 == testEntity);
    assertTrue("Entities should be equal after .getCopy()", Util.equal(test2, testEntity));
    assertTrue("Entity property values should be equal after .getCopy()", test2.propertyValuesEqual(testEntity));
  }
}