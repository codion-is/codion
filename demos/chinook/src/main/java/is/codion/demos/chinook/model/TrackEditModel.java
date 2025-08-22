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

import is.codion.demos.chinook.domain.api.Chinook.Track;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.swing.framework.model.SwingEntityEditModel;

public final class TrackEditModel extends SwingEntityEditModel {

	public TrackEditModel(EntityConnectionProvider connectionProvider) {
		super(Track.TYPE, connectionProvider);
		// Creates and populates the combo box models for the given foreign keys, otherwise this
		// would happen when the associated combo boxes are created, as the UI is initialized.
		initializeComboBoxModels(Track.MEDIATYPE_FK, Track.GENRE_FK);
	}
}
