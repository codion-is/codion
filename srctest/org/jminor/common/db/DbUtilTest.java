/*
 * Copyright (c) 2004 - 2012, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db;

import org.junit.Test;

import java.sql.ResultSet;
import java.sql.Statement;

public class DbUtilTest {

  @Test
  public void closeSilently() {
    DbUtil.closeSilently((Statement) null);
    DbUtil.closeSilently((ResultSet) null);

    DbUtil.closeSilently((Statement[]) null);
    DbUtil.closeSilently((ResultSet[]) null);

    DbUtil.closeSilently(new Statement[] {null, null});
    DbUtil.closeSilently(new ResultSet[] {null, null});
  }
}
