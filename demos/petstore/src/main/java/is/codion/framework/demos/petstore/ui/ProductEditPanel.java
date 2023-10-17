/*
 * Copyright (c) 2004 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.petstore.ui;

import is.codion.swing.common.ui.layout.Layouts;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.EntityEditPanel;

import static is.codion.framework.demos.petstore.domain.Petstore.Product;

public class ProductEditPanel extends EntityEditPanel {

  public ProductEditPanel(SwingEntityEditModel model) {
    super(model);
  }

  @Override
  protected void initializeUI() {
    initialFocusAttribute().set(Product.CATEGORY_FK);

    createForeignKeyComboBox(Product.CATEGORY_FK);
    createTextField(Product.NAME);
    createTextField(Product.DESCRIPTION).columns(16);

    setLayout(Layouts.flexibleGridLayout(3, 1));
    addInputPanel(Product.CATEGORY_FK);
    addInputPanel(Product.NAME);
    addInputPanel(Product.DESCRIPTION);
  }
}