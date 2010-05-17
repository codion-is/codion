/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db;

import org.jminor.common.model.User;

import java.sql.SQLException;

/**
 * User: Björn Darri
 * Date: 15.5.2010
 * Time: 15:55:22
 */
public class EntityDbPreparedConnectionTest extends EntityDbConnectionTest {

  public EntityDbPreparedConnectionTest() throws ClassNotFoundException, SQLException {
    super();
  }

  @Override
  protected EntityDbConnection initializeConnection() throws ClassNotFoundException, SQLException {
    return new EntityDbPreparedConnection(DATABASE, User.UNIT_TEST_USER);
  }
}
