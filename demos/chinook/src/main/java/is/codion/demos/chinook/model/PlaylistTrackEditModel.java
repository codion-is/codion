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
 * Copyright (c) 2024 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.demos.chinook.model;

import is.codion.demos.chinook.domain.api.Chinook.Playlist;
import is.codion.demos.chinook.domain.api.Chinook.PlaylistTrack;
import is.codion.demos.chinook.domain.api.Chinook.Track;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.condition.Condition;
import is.codion.swing.framework.model.SwingEntityEditModel;

public final class PlaylistTrackEditModel extends SwingEntityEditModel {

	public PlaylistTrackEditModel(EntityConnectionProvider connectionProvider) {
		super(PlaylistTrack.TYPE, connectionProvider);
		// So that the track editor value is cleared after a track is added
		editor().value(PlaylistTrack.TRACK_FK).persist().set(false);
		// Set the search model condition, so the search results
		// won't contain tracks already in the selected playlist
		searchModel(PlaylistTrack.TRACK_FK).condition().set(this::excludePlaylistTracks);
	}

	private Condition excludePlaylistTracks() {
		Entity playlist = editor().value(PlaylistTrack.PLAYLIST_FK).getOrThrow();

		// Use a custom subquery based condition, see domain model implementation
		return Track.NOT_IN_PLAYLIST.get(Playlist.ID, playlist.get(Playlist.ID));
	}
}
