/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.petstore.ui;

import is.codion.swing.common.ui.layout.Layouts;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.EntityEditPanel;

import static is.codion.framework.demos.petstore.domain.Petstore.Product;

public class ProductEditPanel extends EntityEditPanel {

  public ProductEditPanel(final SwingEntityEditModel model) {
    super(model);
  }

  @Override
  protected void initializeUI() {
    setInitialFocusAttribute(Product.CATEGORY_FK);

    createForeignKeyComboBox(Product.CATEGORY_FK);
    createTextField(Product.NAME);
    createTextField(Product.DESCRIPTION).setColumns(16);

    setLayout(Layouts.flexibleGridLayout(3, 1));
    addPropertyPanel(Product.CATEGORY_FK);
    addPropertyPanel(Product.NAME);
    addPropertyPanel(Product.DESCRIPTION);
  }
}