/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.petstore.beans.ui;

import org.jminor.swing.common.ui.layout.FlexibleGridLayout;
import org.jminor.swing.framework.model.SwingEntityEditModel;
import org.jminor.swing.framework.ui.EntityEditPanel;

import static org.jminor.framework.demos.petstore.domain.Petstore.TAG_TAG;

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