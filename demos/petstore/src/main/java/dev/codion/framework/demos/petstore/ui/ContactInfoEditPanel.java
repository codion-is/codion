/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.framework.demos.petstore.ui;

import dev.codion.swing.common.ui.layout.FlexibleGridLayout;
import dev.codion.swing.framework.model.SwingEntityEditModel;
import dev.codion.swing.framework.ui.EntityEditPanel;

import static dev.codion.framework.demos.petstore.domain.Petstore.*;

public class ContactInfoEditPanel extends EntityEditPanel {

  public ContactInfoEditPanel(final SwingEntityEditModel model) {
    super(model);
  }

  @Override
  protected void initializeUI() {
    setInitialFocusProperty(SELLER_CONTACT_INFO_LAST_NAME);

    createTextField(SELLER_CONTACT_INFO_LAST_NAME).setColumns(10);
    createTextField(SELLER_CONTACT_INFO_FIRST_NAME);
    createTextField(SELLER_CONTACT_INFO_EMAIL);

    setLayout(new FlexibleGridLayout(3, 1, 5, 5));
    addPropertyPanel(SELLER_CONTACT_INFO_LAST_NAME);
    addPropertyPanel(SELLER_CONTACT_INFO_FIRST_NAME);
    addPropertyPanel(SELLER_CONTACT_INFO_EMAIL);
  }
}