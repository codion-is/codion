/*
 * Copyright (c) 2004 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.ui;

import is.codion.framework.demos.chinook.domain.Chinook.Artist;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.EntityEditPanel;
import is.codion.swing.framework.ui.component.EntitySearchField;

import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.util.function.Supplier;

import static is.codion.framework.demos.chinook.domain.Chinook.Album;
import static is.codion.swing.common.ui.component.Components.borderLayoutPanel;
import static is.codion.swing.common.ui.component.Components.gridLayoutPanel;
import static is.codion.swing.common.ui.component.button.ButtonPanelBuilder.createEastButtonPanel;
import static is.codion.swing.common.ui.layout.Layouts.borderLayout;

public final class AlbumEditPanel extends EntityEditPanel {

  public AlbumEditPanel(SwingEntityEditModel editModel) {
    super(editModel);
    setDefaultTextFieldColumns(15);
  }

  @Override
  protected void initializeUI() {
    initialFocusAttribute().set(Album.ARTIST_FK);

    createForeignKeySearchField(Album.ARTIST_FK);
    createTextField(Album.TITLE);
    setComponent(Album.COVER, new CoverArtPanel(editModel().value(Album.COVER)));

    JPanel centerPanel = borderLayoutPanel()
            .westComponent(borderLayoutPanel()
                    .northComponent(gridLayoutPanel(2, 1)
                            .add(createInputPanel(Album.ARTIST_FK, createArtistPanel()))
                            .add(createInputPanel(Album.TITLE))
                            .build())
                    .build())
            .centerComponent(createInputPanel(Album.COVER))
            .build();

    setLayout(borderLayout());
    add(centerPanel, BorderLayout.CENTER);
  }

  private JPanel createArtistPanel() {
    EntitySearchField artistSearchField = (EntitySearchField) component(Album.ARTIST_FK);

    Supplier<EntityEditPanel> artistEditPanelSupplier = () ->
            new ArtistEditPanel(new SwingEntityEditModel(Artist.TYPE, editModel().connectionProvider()));

    Control addArtistControl = createAddControl(artistSearchField, artistEditPanelSupplier);
    Control editArtistControl = createEditControl(artistSearchField, artistEditPanelSupplier);

    return createEastButtonPanel(artistSearchField, addArtistControl, editArtistControl);
  }
}
