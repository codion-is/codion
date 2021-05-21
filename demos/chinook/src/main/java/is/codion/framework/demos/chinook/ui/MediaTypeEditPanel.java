/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.ui;

import is.codion.framework.demos.chinook.domain.Chinook.MediaType;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.EntityEditPanel;

import static is.codion.swing.common.ui.layout.Layouts.gridLayout;

public class MediaTypeEditPanel extends EntityEditPanel {

  public MediaTypeEditPanel(final SwingEntityEditModel editModel) {
    super(editModel);
  }

  @Override
  protected void initializeUI() {
    setInitialFocusAttribute(MediaType.NAME);

    textFieldBuilder(MediaType.NAME)
            .columns(12)
            .build();

    setLayout(gridLayout(1, 1));
    addInputPanel(MediaType.NAME);
  }
}