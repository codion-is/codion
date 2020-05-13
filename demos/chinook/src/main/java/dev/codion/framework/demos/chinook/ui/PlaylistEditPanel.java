/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.chinook.ui;

import org.jminor.swing.framework.model.SwingEntityEditModel;
import org.jminor.swing.framework.ui.EntityEditPanel;

import static org.jminor.framework.demos.chinook.domain.Chinook.PLAYLIST_NAME;
import static org.jminor.swing.common.ui.layout.Layouts.gridLayout;

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