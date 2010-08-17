/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.petstore.beans.ui;

import org.jminor.common.ui.layout.FlexibleGridLayout;
import org.jminor.framework.client.model.EntityEditModel;
import org.jminor.framework.client.ui.EntityComboBox;
import org.jminor.framework.client.ui.EntityEditPanel;
import static org.jminor.framework.demos.petstore.domain.Petstore.*;

import javax.swing.JTextField;

public class ProductPanel extends EntityEditPanel {

  public ProductPanel(final EntityEditModel model) {
    super(model);
  }

  @Override
  protected void initializeUI() {
    setLayout(new FlexibleGridLayout(3,1,5,5));
    final EntityComboBox box = createEntityComboBox(PRODUCT_CATEGORY_FK);
    setInitialFocusComponent(box);
    addPropertyPanel(PRODUCT_CATEGORY_FK);
    createTextField(PRODUCT_NAME);
    addPropertyPanel(PRODUCT_NAME);
    final JTextField txt = createTextField(PRODUCT_DESCRIPTION);
    txt.setColumns(16);
    addPropertyPanel(PRODUCT_DESCRIPTION);
  }
}