/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.petstore.beans.ui;

import org.jminor.swing.common.ui.layout.FlexibleGridLayout;
import org.jminor.swing.framework.model.SwingEntityEditModel;
import org.jminor.swing.framework.ui.EntityEditPanel;

import static org.jminor.framework.demos.petstore.domain.Petstore.*;

public class CategoryEditPanel extends EntityEditPanel {

  public CategoryEditPanel(final SwingEntityEditModel model) {
    super(model);
  }

  @Override
  protected void initializeUI() {
    setInitialFocusProperty(CATEGORY_NAME);

    createTextField(CATEGORY_NAME).setColumns(10);
    createTextField(CATEGORY_DESCRIPTION).setColumns(18);
    createTextField(CATEGORY_IMAGE_URL);

    setLayout(new FlexibleGridLayout(2, 2, 5, 5));
    addPropertyPanel(CATEGORY_NAME);
    addPropertyPanel(CATEGORY_DESCRIPTION);
    addPropertyPanel(CATEGORY_IMAGE_URL);
  }
}
