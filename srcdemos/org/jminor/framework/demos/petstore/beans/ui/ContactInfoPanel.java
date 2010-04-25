/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.petstore.beans.ui;

import org.jminor.common.model.valuemap.ChangeValueMapEditModel;
import org.jminor.common.ui.layout.FlexibleGridLayout;
import org.jminor.framework.client.model.EntityEditModel;
import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.client.ui.EntityEditPanel;
import org.jminor.framework.client.ui.EntityPanel;
import org.jminor.framework.client.ui.EntityPanelProvider;
import org.jminor.framework.demos.petstore.beans.ItemModel;
import static org.jminor.framework.demos.petstore.domain.Petstore.*;

import javax.swing.JTextField;
import java.util.Arrays;
import java.util.List;

/**
 * User: Bjorn Darri
 * Date: 24.12.2007
 * Time: 14:05:58
 */
public class ContactInfoPanel extends EntityPanel {

  public ContactInfoPanel(final EntityModel model) {
    super(model, "Seller Contact Info");
  }

  @Override
  protected List<EntityPanelProvider> getDetailPanelProviders() {
    return Arrays.asList(new EntityPanelProvider(ItemModel.class, ItemPanel.class));
  }

  @Override
  protected EntityEditPanel initializeEditPanel(final ChangeValueMapEditModel editModel) {
    return new EntityEditPanel((EntityEditModel) editModel) {
      @Override
      protected void initializeUI() {
        setLayout(new FlexibleGridLayout(3,1,5,5));
        final JTextField txt = createTextField(SELLER_CONTACT_INFO_LAST_NAME);
        setDefaultFocusComponent(txt);
        txt.setColumns(10);
        add(createPropertyPanel(SELLER_CONTACT_INFO_LAST_NAME, txt));
        add(createPropertyPanel(SELLER_CONTACT_INFO_FIRST_NAME, createTextField(SELLER_CONTACT_INFO_FIRST_NAME)));
        add(createPropertyPanel(SELLER_CONTACT_INFO_EMAIL, createTextField(SELLER_CONTACT_INFO_EMAIL)));
      }
    };
  }
}