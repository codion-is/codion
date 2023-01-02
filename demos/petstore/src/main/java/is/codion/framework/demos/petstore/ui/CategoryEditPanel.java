/*
 * Copyright (c) 2004 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.petstore.ui;

import is.codion.swing.common.ui.layout.Layouts;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.EntityEditPanel;

import static is.codion.framework.demos.petstore.domain.Petstore.Category;

public class CategoryEditPanel extends EntityEditPanel {

  public CategoryEditPanel(SwingEntityEditModel model) {
    super(model);
  }

  @Override
  protected void initializeUI() {
    setInitialFocusAttribute(Category.NAME);

    createTextField(Category.NAME);
    createTextField(Category.DESCRIPTION).columns(18);
    createTextField(Category.IMAGE_URL);

    setLayout(Layouts.flexibleGridLayout(2, 2));
    addInputPanel(Category.NAME);
    addInputPanel(Category.DESCRIPTION);
    addInputPanel(Category.IMAGE_URL);
  }
}
