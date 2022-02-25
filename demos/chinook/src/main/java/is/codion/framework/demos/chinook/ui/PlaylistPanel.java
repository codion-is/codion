/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.ui;

import is.codion.framework.demos.chinook.domain.Chinook.PlaylistTrack;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.ui.EntityPanel;

public final class PlaylistPanel extends EntityPanel {

  public PlaylistPanel(SwingEntityModel playlistModel) {
    super(playlistModel,
            new PlaylistEditPanel(playlistModel.getEditModel()),
            new PlaylistTablePanel(playlistModel.getTableModel()));
    setDetailSplitPanelResizeWeight(0.25);

    SwingEntityModel playlistTrackModel = playlistModel.getDetailModel(PlaylistTrack.TYPE);
    EntityPanel playlistTrackPanel = new EntityPanel(playlistTrackModel,
            new PlaylistTrackEditPanel(playlistTrackModel.getEditModel()),
            new PlaylistTrackTablePanel(playlistTrackModel.getTableModel()));

    addDetailPanel(playlistTrackPanel);
  }
}
