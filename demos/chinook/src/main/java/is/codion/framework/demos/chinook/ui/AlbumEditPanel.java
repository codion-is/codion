/*
 * Copyright (c) 2004 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.ui;

import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.EntityEditPanel;

import javax.swing.JPanel;
import java.awt.BorderLayout;

import static is.codion.framework.demos.chinook.domain.Chinook.Album;
import static is.codion.swing.common.ui.component.Components.gridLayoutPanel;
import static is.codion.swing.common.ui.layout.Layouts.borderLayout;

public final class AlbumEditPanel extends EntityEditPanel {

  public AlbumEditPanel(SwingEntityEditModel editModel) {
    super(editModel);
    setDefaultTextFieldColumns(18);
  }

  @Override
  protected void initializeUI() {
    setInitialFocusAttribute(Album.ARTIST_FK);

    createForeignKeySearchField(Album.ARTIST_FK);
    createTextField(Album.TITLE);

    JPanel northPanel = gridLayoutPanel(2, 1)
            .add(createInputPanel(Album.ARTIST_FK))
            .add(createInputPanel(Album.TITLE))
            .build();

    setLayout(borderLayout());

    add(northPanel, BorderLayout.NORTH);
    add(new CoverArtPanel(editModel().value(Album.COVER)), BorderLayout.CENTER);
  }
}
