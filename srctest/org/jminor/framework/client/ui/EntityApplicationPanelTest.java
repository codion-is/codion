/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui;

import org.jminor.common.model.CancelException;
import org.jminor.common.model.User;
import org.jminor.framework.client.model.DefaultEntityApplicationModel;
import org.jminor.framework.db.EntityDbConnectionTest;
import org.jminor.framework.demos.empdept.client.ui.EmpDeptAppPanel;
import org.jminor.framework.demos.empdept.domain.EmpDept;

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
    panel.initialize(new DefaultEntityApplicationModel(EntityDbConnectionTest.DB_PROVIDER) {
      @Override
      protected void loadDomainModel() {
        new EmpDept();
      }
    });
    panel.getEntityPanel(EmpDept.T_DEPARTMENT).getTablePanel().getTableModel().setSelectedItemIndexes(Arrays.asList(0,1,2));
    panel.getModel().getDbProvider().disconnect();
  }
}
