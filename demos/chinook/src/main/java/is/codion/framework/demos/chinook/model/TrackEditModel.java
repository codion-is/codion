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

import is.codion.common.db.exception.DatabaseException;
import is.codion.common.event.Event;
import is.codion.common.observer.Observer;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.demos.chinook.domain.Chinook.Track;
import is.codion.framework.domain.entity.Entity;
import is.codion.swing.framework.model.SwingEntityEditModel;

import java.util.Collection;
import java.util.List;

public final class TrackEditModel extends SwingEntityEditModel {

	private final Event<Collection<Entity.Key>> ratingUpdated = Event.event();

	public TrackEditModel(EntityConnectionProvider connectionProvider) {
		super(Track.TYPE, connectionProvider);
	}

	Observer<Collection<Entity.Key>> ratingUpdated() {
		return ratingUpdated.observer();
	}

	@Override
	protected Collection<Entity> update(Collection<Entity> entities, EntityConnection connection) throws DatabaseException {
		List<Entity.Key> albumKeys = entities.stream()
						.filter(entity -> entity.entityType().equals(Track.TYPE))
						.filter(track -> track.modified(Track.RATING))
						.map(track -> track.key(Track.ALBUM_FK))
						.toList();
		Collection<Entity> updated = super.update(entities, connection);
		ratingUpdated.accept(albumKeys);

		return updated;
	}
}
