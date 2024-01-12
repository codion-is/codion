/*
 * Copyright (c) 2004 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.ui;

import is.codion.framework.demos.chinook.domain.Chinook.Track;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.ui.EntityPanel;

public final class AlbumPanel extends EntityPanel {

  public AlbumPanel(SwingEntityModel albumModel) {
    super(albumModel, new AlbumEditPanel(albumModel.editModel()), new AlbumTablePanel(albumModel.tableModel()));
    SwingEntityModel trackModel = albumModel.detailModel(Track.TYPE);
    EntityPanel trackPanel = new EntityPanel(trackModel,
            new TrackEditPanel(trackModel.editModel(), trackModel.tableModel()),
            new TrackTablePanel(trackModel.tableModel()));

    addDetailPanel(trackPanel);
  }
}
