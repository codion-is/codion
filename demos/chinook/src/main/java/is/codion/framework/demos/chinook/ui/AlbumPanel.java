/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2004 - 2023, Björn Darri Sigurðsson.
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
            new TrackEditPanel(trackModel.editModel()),
            new TrackTablePanel(trackModel.tableModel()));

    addDetailPanel(trackPanel);
  }
}
