/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui;

import org.jminor.common.db.User;
import org.jminor.framework.demos.empdept.client.ui.EmpDeptAppPanel;

import org.junit.Test;

public class EntityApplicationPanelTest {

  @Test
  public void test() throws Exception {
    final EmpDeptAppPanel panel = new EmpDeptAppPanel();
    panel.initialize(new User("scott", "tiger"));
    panel.getModel().getDbProvider().disconnect();
  }
}
