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
package is.codion.demos.chinook.model;

import is.codion.common.version.Version;
import is.codion.demos.chinook.domain.api.Chinook.Customer;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.model.ForeignKeyDetailModelLink;
import is.codion.swing.framework.model.SwingEntityApplicationModel;
import is.codion.swing.framework.model.SwingEntityModel;

public final class ChinookAppModel extends SwingEntityApplicationModel {

	public static final Version VERSION = Version.parse(ChinookAppModel.class, "/version.properties");

	public ChinookAppModel(EntityConnectionProvider connectionProvider) {
		super(connectionProvider, VERSION);
		entityModels().add(createAlbumModel(connectionProvider));
		entityModels().add(createPlaylistModel(connectionProvider));
		entityModels().add(createCustomerModel(connectionProvider));
	}

	private static SwingEntityModel createAlbumModel(EntityConnectionProvider connectionProvider) {
		AlbumModel albumModel = new AlbumModel(connectionProvider);
		albumModel.tableModel().items().refresh();

		return albumModel;
	}

	private static SwingEntityModel createPlaylistModel(EntityConnectionProvider connectionProvider) {
		SwingEntityModel playlistModel = new SwingEntityModel(new PlaylistTableModel(connectionProvider));
		SwingEntityModel playlistTrackModel = new SwingEntityModel(new PlaylistTrackEditModel(connectionProvider));

		ForeignKeyDetailModelLink<?, ?, ?> playlistTrackLink =
						playlistModel.detailModels().add(playlistTrackModel);
		playlistTrackLink.clearValueOnEmptySelection().set(true);
		playlistTrackLink.active().set(true);

		playlistModel.tableModel().items().refresh();

		return playlistModel;
	}

	private static SwingEntityModel createCustomerModel(EntityConnectionProvider connectionProvider) {
		SwingEntityModel customerModel = new SwingEntityModel(Customer.TYPE, connectionProvider);
		customerModel.editModel().initializeComboBoxModels(Customer.SUPPORTREP_FK);
		SwingEntityModel invoiceModel = new InvoiceModel(connectionProvider);
		customerModel.detailModels().add(invoiceModel);

		customerModel.tableModel().items().refresh();

		return customerModel;
	}
}
