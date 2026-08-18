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
 * Copyright (c) 2004 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.demos.chinook.model;

import is.codion.common.utilities.version.Version;
import is.codion.framework.db.EntityConnection;
import is.codion.swing.framework.model.SwingEntityApplicationModel;

import java.util.List;

public final class ChinookAppModel extends SwingEntityApplicationModel {

	public static final Version VERSION = Version.parse(ChinookAppModel.class, "/version.properties");

	private final AnalyticsModel analytics;

	public ChinookAppModel(EntityConnection connection) {
		super(connection, List.of(
						new AlbumModel(connection),
						new PlaylistModel(connection),
						new CustomerModel(connection)));
		analytics = new AnalyticsModel(connection);
		models().get().forEach(model -> model.tableModel().items().refresh());
	}

	public AnalyticsModel analytics() {
		return analytics;
	}
}
