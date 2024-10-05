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
 * Copyright (c) 2024, Björn Darri Sigurðsson.
 */
package is.codion.framework.demos.chinook.model;

import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.demos.chinook.domain.Chinook.Album;
import is.codion.framework.demos.chinook.domain.Chinook.Track;
import is.codion.swing.framework.model.SwingEntityModel;

public final class AlbumModel extends SwingEntityModel {

	public AlbumModel(EntityConnectionProvider connectionProvider) {
		super(Album.TYPE, connectionProvider);
		SwingEntityModel trackModel = new SwingEntityModel(new TrackTableModel(connectionProvider));
		addDetailModel(trackModel);
		TrackEditModel trackEditModel = trackModel.editModel();
		trackEditModel.initializeComboBoxModels(Track.MEDIATYPE_FK, Track.GENRE_FK);
		trackEditModel.ratingUpdated().addConsumer(tableModel()::refresh);
	}
}
