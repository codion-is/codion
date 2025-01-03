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
 * Copyright (c) 2004 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.demos.chinook.ui;

import is.codion.demos.chinook.domain.api.Chinook.Track;
import is.codion.framework.model.EntitySearchModel;
import is.codion.swing.framework.ui.component.EntitySearchField.Selector;
import is.codion.swing.framework.ui.component.EntitySearchField.TableSelector;

import java.awt.Dimension;
import java.util.function.Function;

import static is.codion.swing.framework.ui.component.EntitySearchField.tableSelector;

/**
 * Provides a {@link TableSelector} for selecting tracks,
 * displaying columns for the artist, album and track name.
 */
final class TrackSelectorFactory implements Function<EntitySearchModel, Selector> {

	@Override
	public TableSelector apply(EntitySearchModel searchModel) {
		TableSelector selector = tableSelector(searchModel);
		selector.table().columnModel().visible().set(Track.ARTIST, Track.ALBUM_FK, Track.NAME);
		selector.table().model().sort().ascending(Track.ARTIST, Track.ALBUM_FK, Track.NAME);
		selector.preferredSize(new Dimension(500, 300));

		return selector;
	}
}
