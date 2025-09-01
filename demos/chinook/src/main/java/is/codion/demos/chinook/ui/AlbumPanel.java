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
 * Copyright (c) 2004 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.demos.chinook.ui;

import is.codion.demos.chinook.domain.api.Chinook.Track;
import is.codion.demos.chinook.model.AlbumModel;
import is.codion.demos.chinook.model.TrackEditModel;
import is.codion.demos.chinook.model.TrackTableModel;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.ui.EntityPanel;

public final class AlbumPanel extends EntityPanel {

	public AlbumPanel(AlbumModel albumModel) {
		super(albumModel,
						new AlbumEditPanel(albumModel.editModel()),
						new AlbumTablePanel(albumModel.tableModel()));
		SwingEntityModel trackModel = albumModel.detailModels().get(Track.TYPE);
		EntityPanel trackPanel = new EntityPanel(trackModel,
						new TrackEditPanel((TrackEditModel) trackModel.editModel(), trackModel.tableModel().selection()),
						new TrackTablePanel((TrackTableModel) trackModel.tableModel()));

		detailPanels().add(trackPanel);
	}
}
