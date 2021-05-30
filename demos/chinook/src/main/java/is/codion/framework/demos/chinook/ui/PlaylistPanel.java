/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.ui;

import is.codion.framework.demos.chinook.domain.Chinook.PlaylistTrack;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.ui.EntityPanel;

public final class PlaylistPanel extends EntityPanel {

  public PlaylistPanel(final SwingEntityModel playlistModel) {
    super(playlistModel, new PlaylistEditPanel(playlistModel.getEditModel()));
    setDetailSplitPanelResizeWeight(0.25);

    final SwingEntityModel playlistTrackModel = playlistModel.getDetailModel(PlaylistTrack.TYPE);
    final EntityPanel playlistTrackPanel = new EntityPanel(playlistTrackModel,
            new PlaylistTrackEditPanel(playlistTrackModel.getEditModel()),
            new PlaylistTrackTablePanel(playlistTrackModel.getTableModel()));

    addDetailPanel(playlistTrackPanel);
  }
}
