/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db;

import org.jminor.common.db.Database;
import org.jminor.framework.model.Entity;
import org.jminor.framework.model.ModelTestDomain;
import org.jminor.framework.model.TestEntity;
import org.jminor.framework.model.Type;

import junit.framework.TestCase;

import java.util.Date;

/**
 * User: Björn Darri
 * Date: 31.3.2009
 * Time: 21:02:43
 */
public class TestEntityDbConnection extends TestCase {

  public TestEntityDbConnection(String name) {
    super(name);
    new ModelTestDomain();
  }

  public void testDML() throws Exception {
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

    //test with null values
    final Entity testEntity2 = TestEntity.getTestMasterEntity(idValue, intValue, null,
            stringValue, null, null, booleanValue, referencedEntityValue);
    assertEquals(EntityDbConnection.getInsertSQL(testEntity2),
            "insert into " + ModelTestDomain.T_TEST_DETAIL
                    + "(int, string, boolean, entity_id, id)"
                    + " values(2, 'string', 1, 2, 1)");

    final Entity testEntity = TestEntity.getTestMasterEntity(idValue, intValue, doubleValue,
            stringValue, shortDateValue, longDateValue, booleanValue, referencedEntityValue);
    //assert dml
    final String shortDateStringSql = Database.get().getSQLDateString(shortDateValue, false);
    final String longDateStringSql = Database.get().getSQLDateString(longDateValue, true);
    assertEquals(EntityDbConnection.getInsertSQL(testEntity),
            "insert into " + ModelTestDomain.T_TEST_DETAIL
                    + "(int, double, string, short_date, long_date, boolean, entity_id, id)"
                    + " values(2, 1.2, 'string', " + shortDateStringSql + ", " + longDateStringSql + ", 1, 2, 1)");
    assertEquals(EntityDbConnection.getDeleteSQL(testEntity),
            "delete from " + ModelTestDomain.T_TEST_DETAIL + " where (id = 1)");
    boolean exception = false;
    try {
      EntityDbConnection.getUpdateSQL(testEntity);
    }
    catch (Exception e) {
      exception = true;
    }
    assertTrue("Should get an exception when trying to get update sql of a non-modified entity", exception);

    testEntity.setValue(ModelTestDomain.TEST_DETAIL_INT, 42);
    testEntity.setValue(ModelTestDomain.TEST_DETAIL_STRING, "newString");
    assertEquals(EntityDbConnection.getUpdateSQL(testEntity), "update " + ModelTestDomain.T_TEST_DETAIL
            + " set int = 42, string = 'newString' where (id = 1)");
    testEntity.setValue(ModelTestDomain.TEST_DETAIL_STRING, "string");
    assertEquals(EntityDbConnection.getUpdateSQL(testEntity), "update " + ModelTestDomain.T_TEST_DETAIL
            + " set int = 42 where (id = 1)");
  }
}
