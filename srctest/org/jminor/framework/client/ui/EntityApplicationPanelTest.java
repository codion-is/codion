/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui;

import org.jminor.common.db.User;
import org.jminor.common.model.CancelException;
import org.jminor.common.ui.UiUtil;
import org.jminor.framework.demos.empdept.beans.ui.DepartmentPanel;
import org.jminor.framework.demos.empdept.client.ui.EmpDeptAppPanel;

import org.junit.Test;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import java.awt.GraphicsEnvironment;
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
    if (GraphicsEnvironment.isHeadless())
      panel.initialize(User.UNIT_TEST_USER);
    else
      panel.startApplication("test", null, false, null);

    panel.getEntityPanel(DepartmentPanel.class).getTablePanel().getTableModel().setSelectedItemIndexes(Arrays.asList(0,1,2));
    if (!GraphicsEnvironment.isHeadless()) {      
      final JFrame frame = UiUtil.getParentFrame(panel);
      frame.setVisible(false);
      frame.dispose();
    }
    panel.getModel().getDbProvider().disconnect();
  }
}
