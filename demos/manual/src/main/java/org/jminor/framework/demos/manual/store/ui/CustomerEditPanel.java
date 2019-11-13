/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.manual.store.ui;

import org.jminor.framework.demos.manual.store.domain.Store;
import org.jminor.swing.framework.model.SwingEntityEditModel;
import org.jminor.swing.framework.ui.EntityEditPanel;

import java.awt.GridLayout;

public class CustomerEditPanel extends EntityEditPanel {

  public CustomerEditPanel(SwingEntityEditModel editModel) {
    super(editModel);
  }

  @Override
  protected void initializeUI() {
    //the firstName field should receive the focus whenever the panel is initialized
    setInitialFocusProperty(Store.CUSTOMER_FIRST_NAME);

    setLayout(new GridLayout(4,1));
    createTextField(Store.CUSTOMER_FIRST_NAME);
    createTextField(Store.CUSTOMER_LAST_NAME);
    createForeignKeyComboBox(Store.CUSTOMER_ADDRESS_FK);
    createCheckBox(Store.CUSTOMER_IS_ACTIVE, null, false);

    //the createControlPanel method creates a panel containing the
    //component associated with the property as well as a JLabel with the
    //property caption as defined in the domain model
    addPropertyPanel(Store.CUSTOMER_FIRST_NAME);
    addPropertyPanel(Store.CUSTOMER_LAST_NAME);
    addPropertyPanel(Store.CUSTOMER_ADDRESS_FK);
    addPropertyPanel(Store.CUSTOMER_IS_ACTIVE);
  }
}
