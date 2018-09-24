/*
 * Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.petstore.beans.ui;

import org.jminor.swing.common.ui.layout.FlexibleGridLayout;
import org.jminor.swing.framework.model.SwingEntityEditModel;
import org.jminor.swing.framework.ui.EntityEditPanel;

import javax.swing.JTextField;

import static org.jminor.framework.demos.petstore.domain.Petstore.*;

public class ContactInfoEditPanel extends EntityEditPanel {

  public ContactInfoEditPanel(final SwingEntityEditModel model) {
    super(model);
  }

  @Override
  protected void initializeUI() {
    setLayout(new FlexibleGridLayout(3, 1, 5, 5));
    final JTextField txt = createTextField(SELLER_CONTACT_INFO_LAST_NAME);
    setInitialFocusComponent(txt);
    txt.setColumns(10);
    addPropertyPanel(SELLER_CONTACT_INFO_LAST_NAME);
    createTextField(SELLER_CONTACT_INFO_FIRST_NAME);
    addPropertyPanel(SELLER_CONTACT_INFO_FIRST_NAME);
    createTextField(SELLER_CONTACT_INFO_EMAIL);
    addPropertyPanel(SELLER_CONTACT_INFO_EMAIL);
  }
}