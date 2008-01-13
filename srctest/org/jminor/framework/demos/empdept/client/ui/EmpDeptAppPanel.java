/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.empdept.client.ui;

import org.jminor.common.db.User;
import org.jminor.common.model.Util;
import org.jminor.common.ui.UiUtil;
import org.jminor.framework.FrameworkSettings;
import org.jminor.framework.client.ui.EntityApplicationPanel;
import org.jminor.framework.client.ui.EntityPanelInfo;
import org.jminor.framework.demos.empdept.beans.DepartmentModel;
import org.jminor.framework.demos.empdept.beans.ui.DepartmentPanel;
import org.jminor.framework.demos.empdept.client.EmpDeptAppModel;

import org.apache.log4j.Level;

import javax.swing.UIManager;
import java.util.Arrays;
import java.util.List;

public class EmpDeptAppPanel extends EntityApplicationPanel {

  /** {@inheritDoc} */
  protected List<EntityPanelInfo> getRootEntityPanelInfo() {
    return Arrays.asList(new EntityPanelInfo(DepartmentModel.class, DepartmentPanel.class));
  }

  public static void main(final String[] args) {
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    }
    catch (Exception e) {
      e.printStackTrace();
    }
    FrameworkSettings.get().toolbarActions = true;
    FrameworkSettings.get().propertyDebug = true;
    FrameworkSettings.get().useSmartRefresh = false;
    FrameworkSettings.get().useQueryRange = false;
    Util.setDefaultLoggingLevel(Level.DEBUG);
    startApplication("Emp-Dept", EmpDeptAppPanel.class, EmpDeptAppModel.class,
            "emp_dept.gif", false, UiUtil.getSize(0.6), new User("scott", "tiger"));
  }
}
