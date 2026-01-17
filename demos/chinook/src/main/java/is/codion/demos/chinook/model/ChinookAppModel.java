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
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.swing.framework.model.SwingEntityApplicationModel;
import is.codion.swing.framework.model.SwingEntityModel;

import java.util.List;

public final class ChinookAppModel extends SwingEntityApplicationModel {

	public static final Version VERSION = Version.parse(ChinookAppModel.class, "/version.properties");

	private final AnalyticsModel analytics;

	public ChinookAppModel(EntityConnectionProvider connectionProvider) {
		super(connectionProvider, List.of(
						createAlbumModel(connectionProvider),
						createPlaylistModel(connectionProvider),
						createCustomerModel(connectionProvider)));
		this.analytics = new AnalyticsModel(connectionProvider);
	}

	public AnalyticsModel analytics() {
		return analytics;
	}

	private static SwingEntityModel createAlbumModel(EntityConnectionProvider connectionProvider) {
		AlbumModel albumModel = new AlbumModel(connectionProvider);
		albumModel.tableModel().items().refresh();

		return albumModel;
	}

	private static SwingEntityModel createPlaylistModel(EntityConnectionProvider connectionProvider) {
		PlaylistModel playlistModel = new PlaylistModel(connectionProvider);
		playlistModel.tableModel().items().refresh();

		return playlistModel;
	}

	private static SwingEntityModel createCustomerModel(EntityConnectionProvider connectionProvider) {
		CustomerModel customerModel = new CustomerModel(connectionProvider);
		customerModel.tableModel().items().refresh();

		return customerModel;
	}
}
