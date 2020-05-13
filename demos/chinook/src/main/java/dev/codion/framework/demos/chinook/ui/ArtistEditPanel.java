/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.framework.demos.chinook.ui;

import dev.codion.swing.framework.model.SwingEntityEditModel;
import dev.codion.swing.framework.ui.EntityEditPanel;

import static dev.codion.framework.demos.chinook.domain.Chinook.ARTIST_NAME;
import static dev.codion.swing.common.ui.layout.Layouts.gridLayout;

public class ArtistEditPanel extends EntityEditPanel {

  public ArtistEditPanel(final SwingEntityEditModel editModel) {
    super(editModel);
  }

  @Override
  protected void initializeUI() {
    setInitialFocusProperty(ARTIST_NAME);

    createTextField(ARTIST_NAME).setColumns(18);

    setLayout(gridLayout(1, 1));
    addPropertyPanel(ARTIST_NAME);
  }
}