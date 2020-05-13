/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.framework.demos.petstore.ui;

import dev.codion.swing.common.ui.layout.FlexibleGridLayout;
import dev.codion.swing.framework.model.SwingEntityEditModel;
import dev.codion.swing.framework.ui.EntityEditPanel;

import static dev.codion.framework.demos.petstore.domain.Petstore.*;

public class ProductEditPanel extends EntityEditPanel {

  public ProductEditPanel(final SwingEntityEditModel model) {
    super(model);
  }

  @Override
  protected void initializeUI() {
    setInitialFocusProperty(PRODUCT_CATEGORY_FK);

    createForeignKeyComboBox(PRODUCT_CATEGORY_FK);
    createTextField(PRODUCT_NAME);
    createTextField(PRODUCT_DESCRIPTION).setColumns(16);

    setLayout(new FlexibleGridLayout(3, 1, 5, 5));
    addPropertyPanel(PRODUCT_CATEGORY_FK);
    addPropertyPanel(PRODUCT_NAME);
    addPropertyPanel(PRODUCT_DESCRIPTION);
  }
}