/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.petstore.ui;

import is.codion.swing.common.ui.layout.FlexibleGridLayout;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.EntityEditPanel;

import static is.codion.framework.demos.petstore.domain.Petstore.*;

public class CategoryEditPanel extends EntityEditPanel {

  public CategoryEditPanel(final SwingEntityEditModel model) {
    super(model);
  }

  @Override
  protected void initializeUI() {
    setInitialFocusAttribute(CATEGORY_NAME);

    createTextField(CATEGORY_NAME).setColumns(10);
    createTextField(CATEGORY_DESCRIPTION).setColumns(18);
    createTextField(CATEGORY_IMAGE_URL);

    setLayout(new FlexibleGridLayout(2, 2, 5, 5));
    addPropertyPanel(CATEGORY_NAME);
    addPropertyPanel(CATEGORY_DESCRIPTION);
    addPropertyPanel(CATEGORY_IMAGE_URL);
  }
}
