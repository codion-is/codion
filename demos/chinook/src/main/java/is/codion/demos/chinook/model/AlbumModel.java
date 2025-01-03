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
 * Copyright (c) 2024 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.demos.chinook.model;

import is.codion.demos.chinook.domain.api.Chinook.Album;
import is.codion.demos.chinook.domain.api.Chinook.Track;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.swing.framework.model.SwingEntityModel;

public final class AlbumModel extends SwingEntityModel {

	public AlbumModel(EntityConnectionProvider connectionProvider) {
		super(Album.TYPE, connectionProvider);
		SwingEntityModel trackModel = new SwingEntityModel(new TrackTableModel(connectionProvider));
		detailModels().add(trackModel);
		TrackEditModel trackEditModel = trackModel.editModel();
		trackEditModel.initializeComboBoxModels(Track.MEDIATYPE_FK, Track.GENRE_FK);
		// We refresh albums which rating may have changed, due to a track rating being updated
		trackEditModel.ratingUpdated().addConsumer(tableModel()::refresh);
	}
}
