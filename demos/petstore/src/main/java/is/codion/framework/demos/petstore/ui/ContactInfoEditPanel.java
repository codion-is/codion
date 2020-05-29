/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.petstore.ui;

import is.codion.swing.common.ui.layout.FlexibleGridLayout;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.EntityEditPanel;

import static is.codion.framework.demos.petstore.domain.Petstore.*;

public class ContactInfoEditPanel extends EntityEditPanel {

  public ContactInfoEditPanel(final SwingEntityEditModel model) {
    super(model);
  }

  @Override
  protected void initializeUI() {
    setInitialFocusAttribute(SELLER_CONTACT_INFO_LAST_NAME);

    createTextField(SELLER_CONTACT_INFO_LAST_NAME).setColumns(10);
    createTextField(SELLER_CONTACT_INFO_FIRST_NAME);
    createTextField(SELLER_CONTACT_INFO_EMAIL);

    setLayout(new FlexibleGridLayout(3, 1, 5, 5));
    addPropertyPanel(SELLER_CONTACT_INFO_LAST_NAME);
    addPropertyPanel(SELLER_CONTACT_INFO_FIRST_NAME);
    addPropertyPanel(SELLER_CONTACT_INFO_EMAIL);
  }
}