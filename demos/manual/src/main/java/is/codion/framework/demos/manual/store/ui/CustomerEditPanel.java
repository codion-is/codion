/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.manual.store.ui;

import is.codion.framework.demos.manual.store.domain.Store.Customer;
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
    setInitialFocusAttribute(Customer.FIRST_NAME);

    createTextField(Customer.FIRST_NAME).setColumns(15);
    createTextField(Customer.LAST_NAME).setColumns(15);
    createTextField(Customer.EMAIL).setColumns(15);
    createCheckBox(Customer.IS_ACTIVE, null, IncludeCaption.NO);

    setLayout(new GridLayout(4,1));
    //the createControlPanel method creates a panel containing the
    //component associated with the property as well as a JLabel with the
    //property caption as defined in the domain model
    addInputPanel(Customer.FIRST_NAME);
    addInputPanel(Customer.LAST_NAME);
    addInputPanel(Customer.EMAIL);
    addInputPanel(Customer.IS_ACTIVE);
  }
}
// end::customerEditPanel[]