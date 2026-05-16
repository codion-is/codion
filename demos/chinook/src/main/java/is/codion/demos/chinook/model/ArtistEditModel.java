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
package is.codion.demos.chinook.model;

import is.codion.demos.chinook.domain.api.Chinook.Artist;
import is.codion.demos.chinook.domain.api.Chinook.ArtistTag;
import is.codion.framework.db.EntityConnection.Select;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.model.EditorLink;
import is.codion.framework.model.EditorLink.DetailSelect;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.model.SwingEntityEditor;

import java.util.function.Predicate;

import static is.codion.framework.domain.entity.OrderBy.ascending;

// tag::artistEditModel[]
public final class ArtistEditModel extends SwingEntityEditModel {

	public static final int TAG_SLOTS = 6;
	public static final String TAG_PREFIX = "tag";

	public ArtistEditModel(EntityConnectionProvider connectionProvider) {
		super(Artist.TYPE, connectionProvider);
		TagPresent present = new TagPresent();
		for (int i = 0; i < TAG_SLOTS; i++) {
			editor().detail().add(EditorLink.builder()
							.editor(new SwingEntityEditor(ArtistTag.TYPE, connectionProvider))
							.foreignKey(ArtistTag.ARTIST_FK)
							.select(new TagSelect(i))
							.present(present)
							.name(TAG_PREFIX + i)
							.caption(String.valueOf(i + 1))
							.build());
		}
	}

	private static final class TagSelect implements DetailSelect {

		private final int index;

		private TagSelect(int index) {
			this.index = index;
		}

		@Override
		public Select get(Entity artist) {
			return Select.where(ArtistTag.ARTIST_FK.equalTo(artist))
							.orderBy(ascending(ArtistTag.TAG))
							.offset(index)
							.limit(1)
							.build();
		}
	}

	private static final class TagPresent implements Predicate<Entity> {

		@Override
		public boolean test(Entity tag) {
			return !tag.isNull(ArtistTag.TAG);
		}
	}
}
// end::artistEditModel[]