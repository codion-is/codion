/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.petstore.client.ui;

import org.jminor.common.db.User;
import org.jminor.common.model.Util;
import org.jminor.common.ui.UiUtil;
import org.jminor.framework.FrameworkSettings;
import org.jminor.framework.client.ui.EntityApplicationPanel;
import org.jminor.framework.client.ui.EntityPanel;
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
  protected List<EntityPanel.EntityPanelInfo> getMainEntityPanelInfo() {
    return Arrays.asList(new EntityPanel.EntityPanelInfo(CategoryModel.class, CategoryPanel.class));
  }

  protected List<EntityPanel.EntityPanelInfo> getSupportEntityPanelInfo() {
    return Arrays.asList(
            new EntityPanel.EntityPanelInfo("Addresses", AddressModel.class, AddressPanel.class),
            new EntityPanel.EntityPanelInfo("Seller info", ContactInfoModel.class, ContactInfoPanel.class),
            new EntityPanel.EntityPanelInfo("Tags", TagModel.class, TagPanel.class));
  }

  protected void initializeSettings() {
    FrameworkSettings.get().setProperty(FrameworkSettings.TOOLBAR_BUTTONS, true);
    FrameworkSettings.get().setProperty(FrameworkSettings.PROPERTY_DEBUG_OUTPUT, true);
    Util.setDefaultLoggingLevel(Level.DEBUG);
  }

  public static void main(final String[] args) {
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    }
    catch (Exception e) {
      e.printStackTrace();
    }
    startApplication("The Pet Store", PetstoreAppPanel.class, PetstoreAppModel.class,
            null, false, UiUtil.getSize(0.8), new User("scott", "tiger"));
  }
}