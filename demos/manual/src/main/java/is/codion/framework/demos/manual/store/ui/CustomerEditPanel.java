/*
 * Copyright (c) 2004 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.manual.store.ui;

import is.codion.framework.demos.manual.store.domain.Store.Customer;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.EntityEditPanel;

import java.awt.GridLayout;

// tag::customerEditPanel[]
public class CustomerEditPanel extends EntityEditPanel {

  public CustomerEditPanel(SwingEntityEditModel editModel) {
    super(editModel);
    setDefaultTextFieldColumns(15);
  }

  @Override
  protected void initializeUI() {
    //the firstName field should receive the focus whenever the panel is initialized
    setInitialFocusAttribute(Customer.FIRST_NAME);

    createTextField(Customer.FIRST_NAME);
    createTextField(Customer.LAST_NAME);
    createTextField(Customer.EMAIL);
    createCheckBox(Customer.ACTIVE);

    setLayout(new GridLayout(4, 1));
    //the addInputPanel method creates and adds a panel containing the
    //component associated with the attribute as well as a JLabel with the
    //property caption as defined in the domain model
    addInputPanel(Customer.FIRST_NAME);
    addInputPanel(Customer.LAST_NAME);
    addInputPanel(Customer.EMAIL);
    addInputPanel(Customer.ACTIVE);
  }
}
// end::customerEditPanel[]