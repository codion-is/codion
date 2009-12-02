/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db;

import org.jminor.common.db.User;
import org.jminor.common.db.dbms.Database;
import org.jminor.common.db.dbms.DatabaseProvider;
import org.jminor.framework.db.provider.EntityDbProvider;
import org.jminor.framework.db.provider.EntityDbProviderFactory;
import org.jminor.framework.demos.empdept.domain.EmpDept;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.EntityTest;
import org.jminor.framework.domain.EntityTestDomain;

import junit.framework.TestCase;

import java.sql.Timestamp;
import java.util.Date;

/**
 * User: Björn Darri
 * Date: 31.3.2009
 * Time: 21:02:43
 */
public class EntityDbConnectionTest extends TestCase {

  public static final EntityDbProvider dbProvider =
          EntityDbProviderFactory.createEntityDbProvider(new User("scott", "tiger"), "JMinor Unit Tests");

  static {
    new EmpDept();
  }

  public EntityDbConnectionTest() {
    new EntityTestDomain();
  }

  public void testDML() throws Exception {
    final int idValue = 1;
    final int intValue = 2;
    final double doubleValue = 1.2;
    final String stringValue = "string";
    final Date dateValue = new Date();
    final Timestamp timestampValue = new Timestamp(new Date().getTime());
    final Boolean booleanValue = true;
    final int referenceId = 2;

    final Entity referencedEntityValue = new Entity(EntityTestDomain.T_MASTER);
    referencedEntityValue.setValue(EntityTestDomain.MASTER_ID, referenceId);
    referencedEntityValue.setValue(EntityTestDomain.MASTER_NAME, stringValue);

    final Database database = DatabaseProvider.createInstance();

    //test with null values
    final Entity testEntity2 = EntityTest.getDetailEntity(idValue, intValue, null,
            stringValue, null, null, booleanValue, referencedEntityValue);
    assertEquals("insert into " + EntityTestDomain.T_DETAIL
                    + "(id, int, string, boolean, entity_id)"
                    + " values(1, 2, 'string', 1, 2)", EntityDbConnection.getInsertSQL(database, testEntity2));

    final Entity testEntity = EntityTest.getDetailEntity(idValue, intValue, doubleValue,
            stringValue, dateValue, timestampValue, booleanValue, referencedEntityValue);
    //assert dml
    final String shortDateStringSql = database.getSQLDateString(dateValue, false);
    final String longDateStringSql = database.getSQLDateString(timestampValue, true);
    assertEquals("insert into " + EntityTestDomain.T_DETAIL
                    + "(id, int, double, string, date, timestamp, boolean, entity_id)"
                    + " values(1, 2, 1.2, 'string', " + shortDateStringSql + ", " + longDateStringSql + ", 1, 2)",
            EntityDbConnection.getInsertSQL(database, testEntity));
    assertEquals("delete from " + EntityTestDomain.T_DETAIL + " where (id = 1)",
            EntityDbConnection.getDeleteSQL(database, testEntity.getPrimaryKey()));
    try {
      EntityDbConnection.getUpdateSQL(database, testEntity);
      fail("Should get an exception when trying to get update sql of a non-modified entity");
    }
    catch (Exception e) {}

    testEntity.setValue(EntityTestDomain.DETAIL_INT, 42);
    testEntity.setValue(EntityTestDomain.DETAIL_STRING, "newString");
    assertEquals( "update " + EntityTestDomain.T_DETAIL
            + " set int = 42, string = 'newString' where (id = 1)", EntityDbConnection.getUpdateSQL(database, testEntity));
    testEntity.setValue(EntityTestDomain.DETAIL_STRING, "string");
    assertEquals("update " + EntityTestDomain.T_DETAIL + " set int = 42 where (id = 1)",
            EntityDbConnection.getUpdateSQL(database, testEntity));
  }

  public void testGenerateSelectSql() throws Exception {
    final String generated = EntityDbConnection.getSelectSql("table", "col, col2", "where col = 1", "col2");
    assertEquals("Generate select should be working", "select col, col2 from table where col = 1 order by col2", generated);
  }
}
