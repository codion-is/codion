/*
 * Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class EntityApplicationModelTest {

  @Test
  public void getReportPathNotSpecified() {
    EntityApplicationModel.REPORT_PATH.set(null);
    assertThrows(IllegalArgumentException.class, EntityApplicationModel::getReportPath);
  }

  @Test
  public void getReportPath() {
    final String path = "test/path";
    EntityApplicationModel.REPORT_PATH.set(path);
    assertEquals(path, EntityApplicationModel.getReportPath());
  }
}
