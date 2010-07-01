/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui;

import org.jminor.common.model.CancelException;
import org.jminor.common.model.User;
import org.jminor.framework.db.EntityDbConnectionTest;
import org.jminor.framework.demos.empdept.beans.ui.DepartmentPanel;
import org.jminor.framework.demos.empdept.client.EmpDeptAppModel;
import org.jminor.framework.demos.empdept.client.ui.EmpDeptAppPanel;

import org.junit.Test;

import javax.swing.ImageIcon;
import java.util.Arrays;

public class EntityApplicationPanelTest {

  @Test
  public void test() throws Exception {
    final EmpDeptAppPanel panel = new EmpDeptAppPanel() {
      @Override
      protected User getUser(String frameCaption, User defaultUser, String applicationIdentifier, ImageIcon applicationIcon) throws CancelException {
        return User.UNIT_TEST_USER;
      }
    };
    panel.initialize(new EmpDeptAppModel(EntityDbConnectionTest.DB_PROVIDER));
    panel.getEntityPanel(DepartmentPanel.class).getTablePanel().getTableModel().setSelectedItemIndexes(Arrays.asList(0,1,2));
    panel.getModel().getDbProvider().disconnect();
  }
}
