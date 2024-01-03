/*
 * Copyright (c) 2004 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.ui;

import is.codion.framework.demos.chinook.domain.Chinook.Artist;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.EntityEditPanel;

import javax.swing.JPanel;
import java.awt.BorderLayout;

import static is.codion.framework.demos.chinook.domain.Chinook.Album;
import static is.codion.swing.common.ui.component.Components.borderLayoutPanel;
import static is.codion.swing.common.ui.component.Components.gridLayoutPanel;
import static is.codion.swing.common.ui.layout.Layouts.borderLayout;

public final class AlbumEditPanel extends EntityEditPanel {

  public AlbumEditPanel(SwingEntityEditModel editModel) {
    super(editModel);
    setDefaultTextFieldColumns(15);
  }

  @Override
  protected void initializeUI() {
    initialFocusAttribute().set(Album.ARTIST_FK);

    createForeignKeySearchFieldPanel(Album.ARTIST_FK, this::createArtistEditPanel)
            .add(true)
            .edit(true);
    createTextField(Album.TITLE);
    setComponent(Album.COVER, new CoverArtPanel(editModel().value(Album.COVER)));

    JPanel centerPanel = borderLayoutPanel()
            .westComponent(borderLayoutPanel()
                    .northComponent(gridLayoutPanel(2, 1)
                            .add(createInputPanel(Album.ARTIST_FK))
                            .add(createInputPanel(Album.TITLE))
                            .build())
                    .build())
            .centerComponent(createInputPanel(Album.COVER))
            .build();

    setLayout(borderLayout());
    add(centerPanel, BorderLayout.CENTER);
  }

  private EntityEditPanel createArtistEditPanel() {
    return new ArtistEditPanel(new SwingEntityEditModel(Artist.TYPE, editModel().connectionProvider()));
  }
}
