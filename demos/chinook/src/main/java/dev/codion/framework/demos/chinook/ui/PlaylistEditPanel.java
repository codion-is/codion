/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.framework.demos.chinook.ui;

import dev.codion.swing.framework.model.SwingEntityEditModel;
import dev.codion.swing.framework.ui.EntityEditPanel;

import static dev.codion.framework.demos.chinook.domain.Chinook.PLAYLIST_NAME;
import static dev.codion.swing.common.ui.layout.Layouts.gridLayout;

public class PlaylistEditPanel extends EntityEditPanel {

  public PlaylistEditPanel(final SwingEntityEditModel editModel) {
    super(editModel);
  }

  @Override
  protected void initializeUI() {
    setInitialFocusProperty(PLAYLIST_NAME);

    createTextField(PLAYLIST_NAME).setColumns(12);

    setLayout(gridLayout(1, 1));
    addPropertyPanel(PLAYLIST_NAME);
  }
}