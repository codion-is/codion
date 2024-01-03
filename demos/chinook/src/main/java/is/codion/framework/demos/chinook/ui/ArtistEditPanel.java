/*
 * Copyright (c) 2004 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.ui;

import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.EntityEditPanel;

import static is.codion.framework.demos.chinook.domain.Chinook.Artist;
import static is.codion.swing.common.ui.layout.Layouts.gridLayout;

public final class ArtistEditPanel extends EntityEditPanel {

  public ArtistEditPanel(SwingEntityEditModel editModel) {
    super(editModel);
  }

  @Override
  protected void initializeUI() {
    initialFocusAttribute().set(Artist.NAME);

    createTextField(Artist.NAME)
            .columns(18);

    setLayout(gridLayout(1, 1));
    addInputPanel(Artist.NAME);
  }
}