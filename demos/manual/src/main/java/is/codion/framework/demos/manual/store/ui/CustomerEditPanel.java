/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.manual.store.ui;

import is.codion.framework.demos.manual.store.domain.Store;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.EntityEditPanel;
import is.codion.swing.framework.ui.EntityInputComponents.IncludeCaption;

import java.awt.GridLayout;

// tag::customerEditPanel[]
public class CustomerEditPanel extends EntityEditPanel {

  public CustomerEditPanel(SwingEntityEditModel editModel) {
    super(editModel);
  }

  @Override
  protected void initializeUI() {
    //the firstName field should receive the focus whenever the panel is initialized
    setInitialFocusProperty(Store.CUSTOMER_FIRST_NAME);

    createTextField(Store.CUSTOMER_FIRST_NAME).setColumns(15);
    createTextField(Store.CUSTOMER_LAST_NAME).setColumns(15);
    createTextField(Store.CUSTOMER_EMAIL).setColumns(15);
    createCheckBox(Store.CUSTOMER_IS_ACTIVE, null, IncludeCaption.NO);

    setLayout(new GridLayout(4,1));
    //the createControlPanel method creates a panel containing the
    //component associated with the property as well as a JLabel with the
    //property caption as defined in the domain model
    addPropertyPanel(Store.CUSTOMER_FIRST_NAME);
    addPropertyPanel(Store.CUSTOMER_LAST_NAME);
    addPropertyPanel(Store.CUSTOMER_EMAIL);
    addPropertyPanel(Store.CUSTOMER_IS_ACTIVE);
  }
}
// end::customerEditPanel[]