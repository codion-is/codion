/*
 * Copyright (c) 2004 - 2010, Bj�rn Darri Sigur�sson. All Rights Reserved.
 */
package org.jminor.common.db;

import java.sql.SQLException;

/**
 * User: Bj�rn Darri
 * Date: 28.3.2010
 * Time: 13:19:41
 */
public interface DbConnectionProvider {
  DbConnection createConnection(final User user) throws ClassNotFoundException, SQLException;
}
