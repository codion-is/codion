/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.ui;

import is.codion.framework.demos.chinook.domain.Chinook.PlaylistTrack;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.EntityEditPanel;

import static is.codion.swing.common.ui.layout.Layouts.gridLayout;

public class PlaylistTrackEditPanel extends EntityEditPanel {

  public PlaylistTrackEditPanel(final SwingEntityEditModel editModel) {
    super(editModel);
  }

  @Override
  protected void initializeUI() {
    setInitialFocusAttribute(PlaylistTrack.PLAYLIST_FK);

    createForeignKeyComboBox(PlaylistTrack.PLAYLIST_FK);
    createForeignKeySearchField(PlaylistTrack.TRACK_FK)
            .selectionProviderFactory(TrackSelectionProvider::new)
            .columns(30);

    setLayout(gridLayout(2, 1));
    addInputPanel(PlaylistTrack.PLAYLIST_FK);
    addInputPanel(PlaylistTrack.TRACK_FK);
  }
}