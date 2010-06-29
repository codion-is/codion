/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.petstore.client.ui;

import org.jminor.common.model.CancelException;
import org.jminor.common.model.User;
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

import javax.swing.UIManager;
import java.util.Locale;

public class PetstoreAppPanel extends EntityApplicationPanel {

  public PetstoreAppPanel() {
    addMainApplicationPanelProvider(new EntityPanelProvider(CategoryModel.class, CategoryPanel.class));
    addSupportPanelProviders(
            new EntityPanelProvider("Addresses", AddressModel.class, AddressPanel.class),
            new EntityPanelProvider("Seller info", ContactInfoModel.class, ContactInfoPanel.class),
            new EntityPanelProvider("Tags", TagModel.class, TagPanel.class));
  }

  @Override
  protected void configureApplication() {
    Locale.setDefault(new Locale("en"));
    Configuration.setValue(Configuration.TOOLBAR_BUTTONS, true);
    Configuration.setValue(Configuration.PROPERTY_DEBUG_OUTPUT, true);
  }

  @Override
  protected EntityApplicationModel initializeApplicationModel(final User user) throws CancelException {
    return new PetstoreAppModel(user);
  }

  public static void main(final String[] args) throws Exception {
    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    new PetstoreAppPanel().startApplication("The Pet Store", null, false, UiUtil.getScreenSizeRatio(0.8), new User("scott", "tiger"));
  }
}