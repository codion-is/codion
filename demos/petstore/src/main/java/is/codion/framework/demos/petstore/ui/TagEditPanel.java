/*
 * Copyright (c) 2004 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.petstore.ui;

import is.codion.framework.demos.petstore.domain.Petstore.Tag;
import is.codion.swing.common.ui.layout.Layouts;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.EntityEditPanel;

public class TagEditPanel extends EntityEditPanel {

  public TagEditPanel(SwingEntityEditModel model) {
    super(model);
  }

  @Override
  protected void initializeUI() {
    setInitialFocusAttribute(Tag.TAG);

    createTextField(Tag.TAG).columns(16);

    setLayout(Layouts.flexibleGridLayout(1, 1));
    addInputPanel(Tag.TAG);
  }
}