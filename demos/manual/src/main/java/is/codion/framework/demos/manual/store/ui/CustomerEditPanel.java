/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
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
  }

  @Override
  protected void initializeUI() {
    //the firstName field should receive the focus whenever the panel is initialized
    setInitialFocusAttribute(Customer.FIRST_NAME);

    textFieldBuilder(Customer.FIRST_NAME)
            .columns(15)
            .build();
    textFieldBuilder(Customer.LAST_NAME)
            .columns(15)
            .build();
    textFieldBuilder(Customer.EMAIL)
            .columns(15)
            .build();
    checkBoxBuilder(Customer.IS_ACTIVE)
            .includeCaption(false)
            .build();

    setLayout(new GridLayout(4,1));
    //the addInputPanel method creates and adds a panel containing the
    //component associated with the property as well as a JLabel with the
    //property caption as defined in the domain model
    addInputPanel(Customer.FIRST_NAME);
    addInputPanel(Customer.LAST_NAME);
    addInputPanel(Customer.EMAIL);
    addInputPanel(Customer.IS_ACTIVE);
  }
}
// end::customerEditPanel[]