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

import is.codion.demos.chinook.domain.api.Chinook.Playlist;
import is.codion.demos.chinook.domain.api.Chinook.PlaylistTrack;
import is.codion.demos.chinook.domain.api.Chinook.Track;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.swing.framework.model.SwingEntityEditModel;

public final class PlaylistTrackEditModel extends SwingEntityEditModel {

	public PlaylistTrackEditModel(EntityConnectionProvider connectionProvider) {
		super(PlaylistTrack.TYPE, connectionProvider);
		editor().value(PlaylistTrack.TRACK_FK).persist().set(false);
		// Set the search model condition, so the search results
		// won't contain tracks already in the currently selected playlist
		editor().value(PlaylistTrack.PLAYLIST_FK).addConsumer(this::excludePlaylistTracks);
	}

	private void excludePlaylistTracks(Entity playlist) {
		searchModel(PlaylistTrack.TRACK_FK).condition().set(() -> playlist == null ? null :
						Track.NOT_IN_PLAYLIST.get(Playlist.ID, playlist.get(Playlist.ID)));
	}
}
