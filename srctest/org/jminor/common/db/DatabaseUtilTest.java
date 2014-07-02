/*
 * Copyright (c) 2004 - 2012, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db;

import org.junit.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class DatabaseUtilTest {

  @Test
  public void closeSilently() {
    DatabaseUtil.closeSilently((Connection[]) null);
    DatabaseUtil.closeSilently((Statement) null);
    DatabaseUtil.closeSilently((ResultSet) null);

    DatabaseUtil.closeSilently((Statement[]) null);
    DatabaseUtil.closeSilently((ResultSet[]) null);

    DatabaseUtil.closeSilently(new Statement[]{null, null});
    DatabaseUtil.closeSilently(new ResultSet[]{null, null});
  }

  @Test
  public void getDatabaseStatistics() {
    DatabaseUtil.getDatabaseStatistics();
  }
}
