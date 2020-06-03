/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.petstore.ui;

import is.codion.swing.common.ui.layout.Layouts;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.EntityEditPanel;

import static is.codion.framework.demos.petstore.domain.Petstore.Category;

public class CategoryEditPanel extends EntityEditPanel {

  public CategoryEditPanel(final SwingEntityEditModel model) {
    super(model);
  }

  @Override
  protected void initializeUI() {
    setInitialFocusAttribute(Category.NAME);

    createTextField(Category.NAME).setColumns(10);
    createTextField(Category.DESCRIPTION).setColumns(18);
    createTextField(Category.IMAGE_URL);

    setLayout(Layouts.flexibleGridLayout(2, 2));
    addPropertyPanel(Category.NAME);
    addPropertyPanel(Category.DESCRIPTION);
    addPropertyPanel(Category.IMAGE_URL);
  }
}
