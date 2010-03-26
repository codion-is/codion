/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server.monitor.ui;

import org.jminor.framework.server.EntityDbRemoteServerTest;
import org.jminor.framework.server.monitor.MonitorModel;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class MonitorPanelTest {

  @BeforeClass
  public static void setUp() throws Exception {
    EntityDbRemoteServerTest.setUp();
  }

  @AfterClass
  public static void tearDown() throws Exception {
    EntityDbRemoteServerTest.tearDown();
  }

  @Test
  public void test() throws Exception {
    new MonitorPanel(new MonitorModel("localhost"));
  }
}
