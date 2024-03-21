/*
 * Copyright (c) 2004 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.ui;

import is.codion.framework.demos.chinook.domain.Chinook.PlaylistTrack;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.ui.EntityPanel;

import static is.codion.swing.framework.ui.TabbedDetailLayout.splitPaneResizeWeight;

public final class PlaylistPanel extends EntityPanel {

  public PlaylistPanel(SwingEntityModel playlistModel) {
    super(playlistModel,
            new PlaylistEditPanel(playlistModel.editModel()),
            new PlaylistTablePanel(playlistModel.tableModel()),
            config -> config.detailLayout(splitPaneResizeWeight(0.25)));

    SwingEntityModel playlistTrackModel = playlistModel.detailModel(PlaylistTrack.TYPE);
    EntityPanel playlistTrackPanel = new EntityPanel(playlistTrackModel,
            new PlaylistTrackEditPanel(playlistTrackModel.editModel()),
            new PlaylistTrackTablePanel(playlistTrackModel.tableModel()));

    addDetailPanel(playlistTrackPanel);
  }
}
