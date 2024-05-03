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
import is.codion.framework.demos.chinook.domain.Chinook.Playlist;
import is.codion.framework.demos.chinook.domain.Chinook.PlaylistTrack;
import is.codion.framework.demos.chinook.domain.Chinook.Track;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.condition.Condition;
import is.codion.swing.framework.model.SwingEntityEditModel;

import static java.util.Collections.singletonList;

public final class PlaylistTrackEditModel extends SwingEntityEditModel {

	public PlaylistTrackEditModel(EntityConnectionProvider connectionProvider) {
		super(PlaylistTrack.TYPE, connectionProvider);
		persist(PlaylistTrack.TRACK_FK).set(false);
		// Filter out tracks already in the current playlist
		valueEvent(PlaylistTrack.PLAYLIST_FK).addConsumer(this::filterPlaylistTracks);
	}

	private void filterPlaylistTracks(Entity playlist) {
		foreignKeySearchModel(PlaylistTrack.TRACK_FK).condition().set(() -> playlist == null ? null :
						Condition.custom(Track.NOT_IN_PLAYLIST,
										singletonList(Playlist.ID),
										singletonList(playlist.get(Playlist.ID))));
	}
}
