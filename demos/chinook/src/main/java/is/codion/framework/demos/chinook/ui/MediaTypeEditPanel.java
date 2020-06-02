/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.ui;

import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.EntityEditPanel;

import static is.codion.framework.demos.chinook.domain.Chinook.MEDIATYPE_NAME;
import static is.codion.swing.common.ui.layout.Layouts.gridLayout;

public class MediaTypeEditPanel extends EntityEditPanel {

  public MediaTypeEditPanel(final SwingEntityEditModel editModel) {
    super(editModel);
  }

  @Override
  protected void initializeUI() {
    setInitialFocusAttribute(MEDIATYPE_NAME);

    createTextField(MEDIATYPE_NAME).setColumns(12);

    setLayout(gridLayout(1, 1));
    addPropertyPanel(MEDIATYPE_NAME);
  }
}