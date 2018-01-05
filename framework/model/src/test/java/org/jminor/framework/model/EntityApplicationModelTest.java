/*
 * Chinook.Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.model;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class EntityApplicationModelTest {

  @Test(expected = IllegalArgumentException.class)
  public void getReportPathNotSpecified() {
    EntityApplicationModel.REPORT_PATH.set(null);
    EntityApplicationModel.getReportPath();
  }

  @Test
  public void getReportPath() {
    final String path = "test/path";
    EntityApplicationModel.REPORT_PATH.set(path);
    assertEquals(path, EntityApplicationModel.getReportPath());
  }
}
