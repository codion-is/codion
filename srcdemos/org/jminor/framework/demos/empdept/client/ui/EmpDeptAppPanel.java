/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.empdept.client.ui;

import org.jminor.common.db.User;
import org.jminor.common.model.Util;
import org.jminor.common.ui.UiUtil;
import org.jminor.framework.FrameworkSettings;
import org.jminor.framework.client.ui.EntityApplicationPanel;
import org.jminor.framework.client.ui.EntityPanelProvider;
import org.jminor.framework.demos.empdept.beans.DepartmentModel;
import org.jminor.framework.demos.empdept.beans.ui.DepartmentPanel;
import org.jminor.framework.demos.empdept.client.EmpDeptAppModel;

import org.apache.log4j.Level;

import javax.swing.UIManager;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class EmpDeptAppPanel extends EntityApplicationPanel {

  /** {@inheritDoc} */
  protected List<EntityPanelProvider> getMainEntityPanelProviders() {
    return Arrays.asList(new EntityPanelProvider(DepartmentModel.class, DepartmentPanel.class));
  }

  protected void initializeSettings() {
    Locale.setDefault(new Locale("en"));
    FrameworkSettings.get().setProperty(FrameworkSettings.TOOLBAR_BUTTONS, true);
    FrameworkSettings.get().setProperty(FrameworkSettings.PROPERTY_DEBUG_OUTPUT, true);
    Util.setLoggingLevel(Level.DEBUG);
  }

  public static void main(final String[] args) {
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    }
    catch (Exception e) {
      e.printStackTrace();
    }
    startApplication("Emp-Dept", EmpDeptAppPanel.class, EmpDeptAppModel.class,
            null, false, UiUtil.getSize(0.6), new User("scott", "tiger"));
  }
}
