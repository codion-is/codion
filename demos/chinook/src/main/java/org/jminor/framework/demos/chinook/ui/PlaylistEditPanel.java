/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.chinook.ui;

import org.jminor.swing.framework.model.SwingEntityEditModel;
import org.jminor.swing.framework.ui.EntityEditPanel;

import java.awt.GridLayout;

import static org.jminor.framework.demos.chinook.domain.Chinook.PLAYLIST_NAME;

public class PlaylistEditPanel extends EntityEditPanel {

  public PlaylistEditPanel(final SwingEntityEditModel editModel) {
    super(editModel);
  }

  @Override
  protected void initializeUI() {
    setInitialFocusProperty(PLAYLIST_NAME);

    createTextField(PLAYLIST_NAME).setColumns(12);

    setLayout(new GridLayout(1, 1, 5, 5));
    addPropertyPanel(PLAYLIST_NAME);
  }
}