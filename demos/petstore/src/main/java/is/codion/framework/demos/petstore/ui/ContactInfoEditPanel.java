/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
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

    textField(SellerContactInfo.LAST_NAME).columns(10);
    textField(SellerContactInfo.FIRST_NAME);
    textField(SellerContactInfo.EMAIL);

    setLayout(Layouts.flexibleGridLayout(3, 1));
    addInputPanel(SellerContactInfo.LAST_NAME);
    addInputPanel(SellerContactInfo.FIRST_NAME);
    addInputPanel(SellerContactInfo.EMAIL);
  }
}