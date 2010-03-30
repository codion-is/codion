/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.empdept.client.ui;

import org.jminor.common.db.User;
import org.jminor.common.model.CancelException;
import org.jminor.common.model.Util;
import org.jminor.common.ui.UiUtil;
import org.jminor.common.ui.control.ControlFactory;
import org.jminor.common.ui.control.ControlSet;
import org.jminor.framework.Configuration;
import org.jminor.framework.client.model.EntityApplicationModel;
import org.jminor.framework.client.ui.EntityApplicationPanel;
import org.jminor.framework.client.ui.EntityPanel;
import org.jminor.framework.client.ui.EntityPanelProvider;
import org.jminor.framework.demos.empdept.beans.DepartmentModel;
import org.jminor.framework.demos.empdept.beans.ui.DepartmentPanel;
import org.jminor.framework.demos.empdept.client.EmpDeptAppModel;
import org.jminor.framework.demos.empdept.domain.EmpDept;
import org.jminor.framework.domain.EntityUtil;

import org.apache.log4j.Level;

import javax.swing.UIManager;
import java.io.File;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;

public class EmpDeptAppPanel extends EntityApplicationPanel {

  public void importJSON() throws Exception {
    final File file = UiUtil.selectFile(this, null);
    UiUtil.showInDialog(this, EntityPanel.createStaticEntityPanel(EntityUtil.parseJSONString(
            Util.getTextFileContents(file.getAbsolutePath(), Charset.defaultCharset())), getModel().getDbProvider()),
            true, "Import", null, null, null);
  }

  @Override
  protected ControlSet getToolsControlSet() {
    final ControlSet toolsSet = super.getToolsControlSet();
    toolsSet.add(ControlFactory.methodControl(this, "importJSON", EmpDept.getString(EmpDept.IMPORT_JSON)));

    return toolsSet;
  }

  @Override
  protected List<EntityPanelProvider> getMainEntityPanelProviders() {
    return Arrays.asList(new EntityPanelProvider(DepartmentModel.class, DepartmentPanel.class));
  }

  @Override
  protected void configureApplication() {
    Configuration.setValue(Configuration.TOOLBAR_BUTTONS, true);
    Configuration.setValue(Configuration.PROPERTY_DEBUG_OUTPUT, true);
    Configuration.setValue(Configuration.COMPACT_ENTITY_PANEL_LAYOUT, true);
    Configuration.setValue(Configuration.USE_OPTIMISTIC_LOCKING, true);
    Util.setLoggingLevel(Level.DEBUG);
  }

  @Override
  protected EntityApplicationModel initializeApplicationModel(final User user) throws CancelException {
    return new EmpDeptAppModel(user);
  }

  public static void main(final String[] args) {
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    }
    catch (Exception e) {
      e.printStackTrace();
    }
    new EmpDeptAppPanel().startApplication("Emp-Dept", null, false, UiUtil.getScreenSizeRatio(0.6), new User("scott", "tiger"));
  }
}
