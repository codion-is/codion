/*
 * Copyright (c) 2008, Bj�rn Darri Sigur�sson. All Rights Reserved.
 */
package org.jminor.framework.demos.petstore.client.ui;

import org.jminor.common.db.User;
import org.jminor.common.model.Util;
import org.jminor.common.ui.UiUtil;
import org.jminor.framework.FrameworkSettings;
import org.jminor.framework.client.ui.EntityApplicationPanel;
import org.jminor.framework.client.ui.EntityPanelInfo;
import org.jminor.framework.demos.petstore.beans.AddressModel;
import org.jminor.framework.demos.petstore.beans.CategoryModel;
import org.jminor.framework.demos.petstore.beans.ContactInfoModel;
import org.jminor.framework.demos.petstore.beans.TagModel;
import org.jminor.framework.demos.petstore.beans.ui.AddressPanel;
import org.jminor.framework.demos.petstore.beans.ui.CategoryPanel;
import org.jminor.framework.demos.petstore.beans.ui.ContactInfoPanel;
import org.jminor.framework.demos.petstore.beans.ui.TagPanel;
import org.jminor.framework.demos.petstore.client.PetstoreAppModel;

import org.apache.log4j.Level;

import javax.swing.UIManager;
import java.util.Arrays;
import java.util.List;

public class PetstoreAppPanel extends EntityApplicationPanel {

  /** {@inheritDoc} */
  protected List<EntityPanelInfo> getRootEntityPanelInfo() {
    return Arrays.asList(new EntityPanelInfo(CategoryModel.class, CategoryPanel.class));
  }

  protected List<EntityPanelInfo> getSupportEntityPanelInfo() {
    return Arrays.asList(
            new EntityPanelInfo("Addresses", AddressModel.class, AddressPanel.class),
            new EntityPanelInfo("Seller info", ContactInfoModel.class, ContactInfoPanel.class),
            new EntityPanelInfo("Tags", TagModel.class, TagPanel.class));
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
    startApplication("The Pet Store", PetstoreAppPanel.class, PetstoreAppModel.class,
            null, false, UiUtil.getSize(0.8), new User("scott", "tiger"));
  }
}