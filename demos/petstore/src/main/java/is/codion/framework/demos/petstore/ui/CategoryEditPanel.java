/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
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

    textField(Category.NAME).columns(10).build();
    textField(Category.DESCRIPTION).columns(18).build();
    textField(Category.IMAGE_URL).build();

    setLayout(Layouts.flexibleGridLayout(2, 2));
    addInputPanel(Category.NAME);
    addInputPanel(Category.DESCRIPTION);
    addInputPanel(Category.IMAGE_URL);
  }
}
