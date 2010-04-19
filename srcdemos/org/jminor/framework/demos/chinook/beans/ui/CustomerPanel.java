/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.chinook.beans.ui;

import org.jminor.framework.client.model.EntityEditModel;
import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.client.ui.EntityEditPanel;
import org.jminor.framework.client.ui.EntityPanel;
import org.jminor.framework.client.ui.EntityPanelProvider;
import org.jminor.framework.demos.chinook.beans.InvoiceModel;

import java.util.Arrays;
import java.util.List;

/**
 * User: Björn Darri
 * Date: 18.4.2010
 * Time: 20:05:39
 */
public class CustomerPanel extends EntityPanel {

  public CustomerPanel(final EntityModel model) {
    super(model, "Customers");
  }

  @Override
  protected EntityEditPanel initializeEditPanel(EntityEditModel editModel) {
    return null;
  }

  @Override
  protected List<EntityPanelProvider> getDetailPanelProviders() {
    return Arrays.asList(new EntityPanelProvider(InvoiceModel.class, InvoicePanel.class));
  }
}