/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.ui;

import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.EntityEditPanel;

import static is.codion.framework.demos.chinook.domain.Chinook.PLAYLIST_NAME;
import static is.codion.swing.common.ui.layout.Layouts.gridLayout;

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