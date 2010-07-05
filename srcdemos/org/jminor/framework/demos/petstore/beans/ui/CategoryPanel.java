/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.petstore.beans.ui;

import org.jminor.common.ui.layout.FlexibleGridLayout;
import org.jminor.framework.client.model.EntityEditModel;
import org.jminor.framework.client.ui.EntityEditPanel;
import static org.jminor.framework.demos.petstore.domain.Petstore.*;

import javax.swing.JTextField;

/**
 * User: Bjorn Darri
 * Date: 24.12.2007
 * Time: 14:05:58
 */
public class CategoryPanel extends EntityEditPanel {

  public CategoryPanel(final EntityEditModel model) {
    super(model);
  }

  @Override
  protected void initializeUI() {
    setLayout(new FlexibleGridLayout(2,2,5,5));
    final JTextField txtName = createTextField(CATEGORY_NAME);
    setInitialFocusComponent(txtName);
    txtName.setColumns(10);
    add(createPropertyPanel(CATEGORY_NAME, txtName));
    final JTextField txtDesc = createTextField(CATEGORY_DESCRIPTION);
    txtDesc.setColumns(18);
    add(createPropertyPanel(CATEGORY_DESCRIPTION, txtDesc));
    add(createPropertyPanel(CATEGORY_IMAGE_URL, createTextField(CATEGORY_IMAGE_URL)));
  }
}
