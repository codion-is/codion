/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.petstore.beans.ui;

import org.jminor.common.swing.ui.layout.FlexibleGridLayout;
import org.jminor.framework.swing.model.EntityEditModel;
import org.jminor.framework.swing.ui.EntityEditPanel;

import javax.swing.JTextField;

import static org.jminor.framework.demos.petstore.domain.Petstore.*;

public class CategoryEditPanel extends EntityEditPanel {

  public CategoryEditPanel(final EntityEditModel model) {
    super(model);
  }

  @Override
  protected void initializeUI() {
    setLayout(new FlexibleGridLayout(2,2,5,5));
    final JTextField txtName = createTextField(CATEGORY_NAME);
    setInitialFocusComponent(txtName);
    txtName.setColumns(10);
    addPropertyPanel(CATEGORY_NAME);
    final JTextField txtDesc = createTextField(CATEGORY_DESCRIPTION);
    txtDesc.setColumns(18);
    addPropertyPanel(CATEGORY_DESCRIPTION);
    createTextField(CATEGORY_IMAGE_URL);
    addPropertyPanel(CATEGORY_IMAGE_URL);
  }
}
