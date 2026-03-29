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
 * Copyright (c) 2025 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.demos.chinook.model;

import is.codion.demos.chinook.domain.api.Chinook.Album;
import is.codion.demos.chinook.domain.api.Chinook.Artist;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.swing.common.model.worker.ProgressWorker.TaskHandler;
import is.codion.swing.framework.model.SwingEntityTableModel;

import java.util.List;

import static is.codion.framework.db.EntityConnection.Update.where;
import static is.codion.framework.db.EntityConnection.transaction;
import static is.codion.framework.domain.entity.Entity.primaryKeys;
import static is.codion.framework.model.PersistenceEvents.persistenceEvents;
import static java.util.Collections.singleton;

public final class ArtistTableModel extends SwingEntityTableModel {

	public ArtistTableModel(EntityConnectionProvider connectionProvider) {
		super(Artist.TYPE, connectionProvider);
	}

	public CombineArtistsTask combine(List<Entity> artistsToDelete, Entity artistToKeep) {
		return new CombineArtistsTask(artistsToDelete, artistToKeep);
	}

	public final class CombineArtistsTask implements TaskHandler {

		private final List<Entity> artistsToDelete;
		private final Entity artistToKeep;

		public CombineArtistsTask(List<Entity> artistsToDelete, Entity artistToKeep) {
			this.artistsToDelete = artistsToDelete;
			this.artistToKeep = artistToKeep;
		}

		@Override
		public void execute() throws Exception {
			EntityConnection connection = connection();
			transaction(connection, () -> {
				connection.update(where(Album.ARTIST_FK.in(artistsToDelete))
								.set(Album.ARTIST_ID, artistToKeep.primaryKey().value())
								.build());
				connection.delete(primaryKeys(artistsToDelete));
			});
		}

		@Override
		public void onSuccess() {
			selection().item().set(artistToKeep);
			items().remove(artistsToDelete);
			persistenceEvents(Artist.TYPE).deleted().accept(artistsToDelete);
			refresh(singleton(artistToKeep.primaryKey()));
		}
	}
}
