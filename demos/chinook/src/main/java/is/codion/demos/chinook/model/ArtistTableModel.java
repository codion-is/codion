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
 * Copyright (c) 2025, Björn Darri Sigurðsson.
 */
package is.codion.demos.chinook.model;

import is.codion.demos.chinook.domain.api.Chinook.Album;
import is.codion.demos.chinook.domain.api.Chinook.Artist;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.model.EntityEditModel;
import is.codion.swing.framework.model.SwingEntityTableModel;

import java.util.List;

import static is.codion.framework.db.EntityConnection.Update.where;
import static is.codion.framework.db.EntityConnection.transaction;
import static is.codion.framework.domain.entity.Entity.primaryKeys;

public final class ArtistTableModel extends SwingEntityTableModel {

	public ArtistTableModel(EntityConnectionProvider connectionProvider) {
		super(Artist.TYPE, connectionProvider);
	}

	public void combine(List<Entity> artistsToDelete, Entity artistToKeep) {
		EntityConnection connection = connection();
		transaction(connection, () -> {
			connection.update(where(Album.ARTIST_FK.in(artistsToDelete))
							.set(Album.ARTIST_ID, artistToKeep.primaryKey().value())
							.build());
			connection.delete(primaryKeys(artistsToDelete));
		});
	}

	public void onCombined(List<Entity> artistsToDelete, Entity artistToKeep) {
		selection().item().set(artistToKeep);
		items().remove(artistsToDelete);
		EntityEditModel.events().deleted(Artist.TYPE).accept(artistsToDelete);
	}
}
