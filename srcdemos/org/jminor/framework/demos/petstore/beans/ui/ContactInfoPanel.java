/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.petstore.beans.ui;

import org.jminor.common.ui.layout.FlexibleGridLayout;
import org.jminor.framework.client.model.EntityEditModel;
import org.jminor.framework.client.ui.EntityEditPanel;
import static org.jminor.framework.demos.petstore.domain.Petstore.*;

import javax.swing.JTextField;

public class ContactInfoPanel extends EntityEditPanel {

  public ContactInfoPanel(final EntityEditModel model) {
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