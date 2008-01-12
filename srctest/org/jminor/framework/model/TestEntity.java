/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 *
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
    new TestDb();
  }

  public static Entity getTestMasterEntity(final int id, final int intValue, final double doubleValue,
                                           final String stringValue, final Date shortDateValue, final Date longDateValue,
                                           final Type.Boolean booleanValue, final Entity entityValue,
                                           final int entityValueId) {
    final Entity ret = new Entity(TestDb.T_TEST_MASTER_ENTITY);
    ret.setValue(TestDb.ID_PROP, id);
    ret.setValue(TestDb.INT_PROP, intValue);
    ret.setValue(TestDb.DOUBLE_PROP, doubleValue);
    ret.setValue(TestDb.STRING_PROP, stringValue);
    ret.setValue(TestDb.SHORT_DATE_PROP, shortDateValue);
    ret.setValue(TestDb.LONG_DATE_PROP, longDateValue);
    ret.setValue(TestDb.BOOLEAN_PROP, booleanValue);
    ret.setValue(TestDb.ENTITY_REF, entityValue);
    ret.setValue(TestDb.ENTITY_ID_PROP, entityValueId);

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

    final Entity referencedEntityValue = new Entity(TestDb.T_TEST_DETAIL_ENTITY);
    referencedEntityValue.setValue(TestDb.ID, referenceId);
    referencedEntityValue.setValue(TestDb.NAME, stringValue);

    Entity test = new Entity(TestDb.T_TEST_MASTER_ENTITY);
    //assert not modified
    assertFalse(test.isModified());
    //assert default values
    assertEquals(test.getValue(TestDb.ID_PROP), 420);
    assertEquals(test.getValue(TestDb.BOOLEAN_PROP), Type.Boolean.TRUE);

    final Entity testEntity = getTestMasterEntity(idValue, intValue, doubleValue,
            stringValue, shortDateValue, longDateValue, booleanValue, referencedEntityValue, referenceId);
    //assert types
    assertEquals(testEntity.getPrimaryKey().getProperty(TestDb.ID_PROP).getPropertyType(), Type.INT);
    assertEquals(testEntity.getProperty(TestDb.INT_PROP).getPropertyType(), Type.INT);
    assertEquals(testEntity.getProperty(TestDb.DOUBLE_PROP).getPropertyType(), Type.DOUBLE);
    assertEquals(testEntity.getProperty(TestDb.STRING_PROP).getPropertyType(), Type.STRING);
    assertEquals(testEntity.getProperty(TestDb.SHORT_DATE_PROP).getPropertyType(), Type.SHORT_DATE);
    assertEquals(testEntity.getProperty(TestDb.LONG_DATE_PROP).getPropertyType(), Type.LONG_DATE);
    assertEquals(testEntity.getProperty(TestDb.BOOLEAN_PROP).getPropertyType(), Type.BOOLEAN);
    assertEquals(testEntity.getProperty(TestDb.ENTITY_REF).getPropertyType(), Type.ENTITY);
    assertEquals(testEntity.getProperty(TestDb.ENTITY_ID_PROP).getPropertyType(), Type.INT);
    assertEquals(testEntity.getProperty(TestDb.DENORM_PROP).getPropertyType(), Type.STRING);

    //assert column names
    assertEquals(testEntity.getPrimaryKey().getProperty(TestDb.ID_PROP).propertyID, TestDb.ID_PROP);
    assertEquals(testEntity.getProperty(TestDb.INT_PROP).propertyID, TestDb.INT_PROP);
    assertEquals(testEntity.getProperty(TestDb.DOUBLE_PROP).propertyID, TestDb.DOUBLE_PROP);
    assertEquals(testEntity.getProperty(TestDb.STRING_PROP).propertyID, TestDb.STRING_PROP);
    assertEquals(testEntity.getProperty(TestDb.SHORT_DATE_PROP).propertyID, TestDb.SHORT_DATE_PROP);
    assertEquals(testEntity.getProperty(TestDb.LONG_DATE_PROP).propertyID, TestDb.LONG_DATE_PROP);
    assertEquals(testEntity.getProperty(TestDb.BOOLEAN_PROP).propertyID, TestDb.BOOLEAN_PROP);
    assertEquals(testEntity.getProperty(TestDb.ENTITY_ID_PROP).propertyID, TestDb.ENTITY_ID_PROP);
    assertEquals(testEntity.getProperty(TestDb.DENORM_PROP).propertyID, TestDb.DENORM_PROP);

    //assert captions
    assertNull(testEntity.getPrimaryKey().getProperty(TestDb.ID_PROP).getCaption());
    assertEquals(testEntity.getProperty(TestDb.INT_PROP).getCaption(), TestDb.INT_PROP);
    assertEquals(testEntity.getProperty(TestDb.DOUBLE_PROP).getCaption(), TestDb.DOUBLE_PROP);
    assertEquals(testEntity.getProperty(TestDb.STRING_PROP).getCaption(), TestDb.STRING_PROP);
    assertEquals(testEntity.getProperty(TestDb.SHORT_DATE_PROP).getCaption(), TestDb.SHORT_DATE_PROP);
    assertEquals(testEntity.getProperty(TestDb.LONG_DATE_PROP).getCaption(), TestDb.LONG_DATE_PROP);
    assertEquals(testEntity.getProperty(TestDb.BOOLEAN_PROP).getCaption(), TestDb.BOOLEAN_PROP);
    assertEquals(testEntity.getProperty(TestDb.ENTITY_REF).getCaption(), TestDb.ENTITY_REF);
    assertEquals(testEntity.getProperty(TestDb.DENORM_PROP).getCaption(), TestDb.DENORM_PROP);

    //assert hidden status
    assertTrue(testEntity.getPrimaryKey().getProperty(TestDb.ID_PROP).isHidden());
    assertFalse(testEntity.getProperty(TestDb.INT_PROP).isHidden());
    assertFalse(testEntity.getProperty(TestDb.DOUBLE_PROP).isHidden());
    assertFalse(testEntity.getProperty(TestDb.STRING_PROP).isHidden());
    assertFalse(testEntity.getProperty(TestDb.SHORT_DATE_PROP).isHidden());
    assertFalse(testEntity.getProperty(TestDb.LONG_DATE_PROP).isHidden());
    assertFalse(testEntity.getProperty(TestDb.BOOLEAN_PROP).isHidden());
    assertFalse(testEntity.getProperty(TestDb.ENTITY_REF).isHidden());
    assertFalse(testEntity.getProperty(TestDb.DENORM_PROP).isHidden());

    //assert values
    assertEquals(testEntity.getValue(TestDb.ID_PROP), idValue);
    assertEquals(testEntity.getValue(TestDb.INT_PROP), intValue);
    assertEquals(testEntity.getValue(TestDb.DOUBLE_PROP), doubleValue);
    assertEquals(testEntity.getValue(TestDb.STRING_PROP), stringValue);
    assertEquals(testEntity.getValue(TestDb.SHORT_DATE_PROP), shortDateValue);
    assertEquals(testEntity.getValue(TestDb.LONG_DATE_PROP), longDateValue);
    assertEquals(testEntity.getValue(TestDb.BOOLEAN_PROP), booleanValue);
    assertEquals(testEntity.getValue(TestDb.ENTITY_REF), referencedEntityValue);
    assertEquals(testEntity.getValue(TestDb.DENORM_PROP), stringValue);
    assertFalse(testEntity.isValueNull(TestDb.ENTITY_ID_PROP));

    boolean exception = false;
    try {
      testEntity.setValue(TestDb.DENORM_PROP, "hello");
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
            "insert into " + TestDb.T_TEST_MASTER_ENTITY
                    + "(int, double, string, short_date, long_date, boolean, entity_id, id)"
                    + " values(2, 1.2, 'string', " + shortDateStringSql + ", " + longDateStringSql + ", 1, 2, 1)");
    assertEquals(EntityUtil.getDeleteSQL(testEntity), "delete from " + TestDb.T_TEST_MASTER_ENTITY + " where (id = 1)");
    exception = false;
    try {
      EntityUtil.getUpdateSQL(testEntity);
    }
    catch (Exception e) {
      exception = true;
    }
    assertTrue("Should get an exception when trying to get update sql of a non-modified entity", exception);

    testEntity.setValue(TestDb.INT_PROP, 42);
    testEntity.setValue(TestDb.STRING_PROP, "newString");
    assertEquals(EntityUtil.getUpdateSQL(testEntity), "update " + TestDb.T_TEST_MASTER_ENTITY + " set int = 42, string = 'newString' where (id = 1)");
    testEntity.setValue(TestDb.STRING_PROP, "string");
    assertEquals(EntityUtil.getUpdateSQL(testEntity), "update " + TestDb.T_TEST_MASTER_ENTITY + " set int = 42 where (id = 1)");
//"long_date = to_date('" + longDateString+ "', 'DD-MM-YYYY HH24:MI'), " +
//                    "short_date = to_date('" + shortDateString + "', 'DD-MM-YYYY'),
    //test setAs()
    test = new Entity(TestDb.T_TEST_MASTER_ENTITY);
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