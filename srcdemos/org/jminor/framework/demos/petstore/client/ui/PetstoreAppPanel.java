/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.petstore.client.ui;

import org.jminor.common.db.User;
import org.jminor.common.model.UserCancelException;
import org.jminor.common.model.Util;
import org.jminor.common.ui.UiUtil;
import org.jminor.framework.Configuration;
import org.jminor.framework.client.model.EntityApplicationModel;
import org.jminor.framework.client.ui.EntityApplicationPanel;
import org.jminor.framework.client.ui.EntityPanelProvider;
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
import java.util.Locale;

public class PetstoreAppPanel extends EntityApplicationPanel {

  @Override
  protected List<EntityPanelProvider> getMainEntityPanelProviders() {
    return Arrays.asList(new EntityPanelProvider(CategoryModel.class, CategoryPanel.class));
  }

  @Override
  protected List<EntityPanelProvider> getSupportEntityPanelProviders() {
    return Arrays.asList(
            new EntityPanelProvider("Addresses", AddressModel.class, AddressPanel.class),
            new EntityPanelProvider("Seller info", ContactInfoModel.class, ContactInfoPanel.class),
            new EntityPanelProvider("Tags", TagModel.class, TagPanel.class));
  }

  @Override
  protected void configureApplication() {
    Locale.setDefault(new Locale("en"));
    Configuration.setValue(Configuration.TOOLBAR_BUTTONS, true);
    Configuration.setValue(Configuration.PROPERTY_DEBUG_OUTPUT, true);
    Util.setDefaultLoggingLevel(Level.DEBUG);
  }

  @Override
  protected EntityApplicationModel initializeApplicationModel(final User user) throws UserCancelException {
    return new PetstoreAppModel(user);
  }

  public static void main(final String[] args) {
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    }
    catch (Exception e) {
      e.printStackTrace();
    }
    new PetstoreAppPanel().startApplication("The Pet Store", null, false, UiUtil.getScreenSizeRatio(0.8), new User("scott", "tiger"));
  }
}