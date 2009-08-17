/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db;

import org.jminor.common.db.Database;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.EntityTest;
import org.jminor.framework.domain.EntityTestDomain;
import org.jminor.framework.domain.Type;

import junit.framework.TestCase;

import java.util.Date;

/**
 * User: Björn Darri
 * Date: 31.3.2009
 * Time: 21:02:43
 */
public class EntityDbConnectionTest extends TestCase {

  public EntityDbConnectionTest() {
    new EntityTestDomain();
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

    final Entity referencedEntityValue = new Entity(EntityTestDomain.T_MASTER);
    referencedEntityValue.setValue(EntityTestDomain.MASTER_ID, referenceId);
    referencedEntityValue.setValue(EntityTestDomain.MASTER_NAME, stringValue);

    //test with null values
    final Entity testEntity2 = EntityTest.getDetailEntity(idValue, intValue, null,
            stringValue, null, null, booleanValue, referencedEntityValue);
    assertEquals(EntityDbConnection.getInsertSQL(testEntity2),
            "insert into " + EntityTestDomain.T_DETAIL
                    + "(int, string, boolean, entity_id, id)"
                    + " values(2, 'string', 1, 2, 1)");

    final Entity testEntity = EntityTest.getDetailEntity(idValue, intValue, doubleValue,
            stringValue, shortDateValue, longDateValue, booleanValue, referencedEntityValue);
    //assert dml
    final String shortDateStringSql = Database.get().getSQLDateString(shortDateValue, false);
    final String longDateStringSql = Database.get().getSQLDateString(longDateValue, true);
    assertEquals(EntityDbConnection.getInsertSQL(testEntity),
            "insert into " + EntityTestDomain.T_DETAIL
                    + "(int, double, string, short_date, long_date, boolean, entity_id, id)"
                    + " values(2, 1.2, 'string', " + shortDateStringSql + ", " + longDateStringSql + ", 1, 2, 1)");
    assertEquals(EntityDbConnection.getDeleteSQL(testEntity),
            "delete from " + EntityTestDomain.T_DETAIL + " where (id = 1)");
    boolean exception = false;
    try {
      EntityDbConnection.getUpdateSQL(testEntity);
    }
    catch (Exception e) {
      exception = true;
    }
    assertTrue("Should get an exception when trying to get update sql of a non-modified entity", exception);

    testEntity.setValue(EntityTestDomain.DETAIL_INT, 42);
    testEntity.setValue(EntityTestDomain.DETAIL_STRING, "newString");
    assertEquals(EntityDbConnection.getUpdateSQL(testEntity), "update " + EntityTestDomain.T_DETAIL
            + " set int = 42, string = 'newString' where (id = 1)");
    testEntity.setValue(EntityTestDomain.DETAIL_STRING, "string");
    assertEquals(EntityDbConnection.getUpdateSQL(testEntity), "update " + EntityTestDomain.T_DETAIL
            + " set int = 42 where (id = 1)");
  }
}
