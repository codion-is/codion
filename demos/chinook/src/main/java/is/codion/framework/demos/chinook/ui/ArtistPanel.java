/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.ui;

import is.codion.framework.demos.chinook.domain.Chinook;
import is.codion.framework.demos.chinook.domain.Chinook.Track;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.ui.EntityPanel;

public final class ArtistPanel extends EntityPanel {

  public ArtistPanel(final SwingEntityModel artistModel) {
    super(artistModel, new ArtistEditPanel(artistModel.getEditModel()));
    setDetailSplitPanelResizeWeight(0.25);

    final SwingEntityModel albumModel = artistModel.getDetailModel(Chinook.Album.TYPE);
    final EntityPanel albumPanel = new EntityPanel(albumModel, new AlbumEditPanel(albumModel.getEditModel()));

    final SwingEntityModel trackModel = albumModel.getDetailModel(Track.TYPE);
    final EntityPanel trackPanel = new EntityPanel(trackModel,
            new TrackEditPanel(trackModel.getEditModel()),
            new TrackTablePanel(trackModel.getTableModel()));

    albumPanel.addDetailPanel(trackPanel);

    addDetailPanel(albumPanel);
  }
}
