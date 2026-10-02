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
 * Copyright (c) 2026, Björn Darri Sigurðsson.
 */
package is.codion.demos.chinook.javadoc;

import is.codion.demos.chinook.domain.api.Chinook.Album;
import is.codion.demos.chinook.domain.api.Chinook.Track;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityFormatter;

/**
 * The {@link EntityFormatter} javadoc snippets, each the region of the same name.
 */
final class EntityFormatterSnippets {

	void usage(EntityConnection connection) {
		Entity track = connection.selectSingle(Track.NAME.equalTo("Come As You Are")); // @start region=usage

		EntityFormatter formatter = EntityFormatter.builder()
						.text("Name=")
						.value(Track.NAME)
						.text(", Album='")
						.value(Track.ALBUM_FK, Album.TITLE)
						.text("'")
						.build();

		System.out.println(formatter.apply(track)); // Name=Come As You Are, Album='Nevermind' // @end
	}
}
