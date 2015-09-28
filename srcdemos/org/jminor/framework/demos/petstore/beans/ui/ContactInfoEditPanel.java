/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.petstore.beans.ui;

import org.jminor.common.swing.ui.layout.FlexibleGridLayout;
import org.jminor.framework.swing.model.EntityEditModel;
import org.jminor.framework.swing.ui.EntityEditPanel;

import javax.swing.JTextField;

import static org.jminor.framework.demos.petstore.domain.Petstore.*;

public class ContactInfoEditPanel extends EntityEditPanel {

  public ContactInfoEditPanel(final EntityEditModel model) {
    super(model);
  }

  @Override
  protected void initializeUI() {
    setLayout(new FlexibleGridLayout(3,1,5,5));
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