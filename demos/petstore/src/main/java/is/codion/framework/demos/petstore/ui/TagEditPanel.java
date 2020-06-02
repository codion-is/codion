/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.petstore.ui;

import is.codion.swing.common.ui.layout.FlexibleGridLayout;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.EntityEditPanel;

import static is.codion.framework.demos.petstore.domain.Petstore.TAG_TAG;

public class TagEditPanel extends EntityEditPanel {

  public TagEditPanel(final SwingEntityEditModel model) {
    super(model);
  }

  @Override
  protected void initializeUI() {
    setInitialFocusAttribute(TAG_TAG);

    createTextField(TAG_TAG).setColumns(16);

    setLayout(new FlexibleGridLayout(1, 1, 5, 5));
    addPropertyPanel(TAG_TAG);
  }
}