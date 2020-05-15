/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.framework.demos.petstore.ui;

import dev.codion.swing.common.ui.layout.FlexibleGridLayout;
import dev.codion.swing.framework.model.SwingEntityEditModel;
import dev.codion.swing.framework.ui.EntityEditPanel;

import static dev.codion.framework.demos.petstore.domain.Petstore.TAG_TAG;

public class TagEditPanel extends EntityEditPanel {

  public TagEditPanel(final SwingEntityEditModel model) {
    super(model);
  }

  @Override
  protected void initializeUI() {
    setInitialFocusProperty(TAG_TAG);

    createTextField(TAG_TAG).setColumns(16);

    setLayout(new FlexibleGridLayout(1, 1, 5, 5));
    addPropertyPanel(TAG_TAG);
  }
}