/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.petstore.ui;

import is.codion.swing.common.ui.layout.Layouts;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.EntityEditPanel;

import static is.codion.framework.demos.petstore.domain.Petstore.SellerContactInfo;

public class ContactInfoEditPanel extends EntityEditPanel {

  public ContactInfoEditPanel(final SwingEntityEditModel model) {
    super(model);
  }

  @Override
  protected void initializeUI() {
    setInitialFocusAttribute(SellerContactInfo.LAST_NAME);

    createTextField(SellerContactInfo.LAST_NAME).setColumns(10);
    createTextField(SellerContactInfo.FIRST_NAME);
    createTextField(SellerContactInfo.EMAIL);

    setLayout(Layouts.flexibleGridLayout(3, 1));
    addPropertyPanel(SellerContactInfo.LAST_NAME);
    addPropertyPanel(SellerContactInfo.FIRST_NAME);
    addPropertyPanel(SellerContactInfo.EMAIL);
  }
}