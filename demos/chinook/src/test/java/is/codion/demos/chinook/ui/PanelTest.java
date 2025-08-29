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
package is.codion.demos.chinook.ui;

import is.codion.common.user.User;
import is.codion.demos.chinook.domain.ChinookImpl;
import is.codion.demos.chinook.domain.api.Chinook.Customer;
import is.codion.demos.chinook.domain.api.Chinook.Employee;
import is.codion.demos.chinook.domain.api.Chinook.Genre;
import is.codion.demos.chinook.domain.api.Chinook.MediaType;
import is.codion.demos.chinook.domain.api.Chinook.Preferences;
import is.codion.demos.chinook.model.AlbumModel;
import is.codion.demos.chinook.model.ArtistTableModel;
import is.codion.demos.chinook.model.InvoiceLineEditModel;
import is.codion.demos.chinook.model.InvoiceModel;
import is.codion.demos.chinook.model.PlaylistEditModel;
import is.codion.demos.chinook.model.PlaylistTrackEditModel;
import is.codion.demos.chinook.model.TrackEditModel;
import is.codion.demos.chinook.model.TrackTableModel;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.ui.EntityPanel;
import is.codion.swing.framework.ui.icon.FrameworkIcons;

import org.junit.jupiter.api.Test;
import org.kordamp.ikonli.foundation.Foundation;

import javax.swing.JTextField;

public final class PanelTest {

	static {
		FrameworkIcons.instance().add(Foundation.PLUS, Foundation.MINUS);
	}

	private final EntityConnectionProvider connectionProvider =
					LocalEntityConnectionProvider.builder()
									.domain(new ChinookImpl())
									.user(User.parse(System.getProperty("codion.test.user")))
									.build();

	@Test
	void album() {
		new AlbumPanel(new AlbumModel(connectionProvider)).initialize();
	}

	@Test
	void artist() {
		SwingEntityModel model = new SwingEntityModel(new ArtistTableModel(connectionProvider));
		new EntityPanel(model, new ArtistEditPanel(model.editModel())).initialize();
	}

	@Test
	void customer() {
		SwingEntityModel model = new SwingEntityModel(Customer.TYPE, connectionProvider);
		model.detailModels().add(new InvoiceModel(connectionProvider));
		new CustomerPanel(model).initialize();
	}

	@Test
	void employee() {
		SwingEntityModel model = new SwingEntityModel(Employee.TYPE, connectionProvider);
		new EntityPanel(model, new EmployeeEditPanel(model.editModel()), new EmployeeTablePanel(model.tableModel())).initialize();
	}

	@Test
	void genre() {
		new GenreEditPanel(new SwingEntityEditModel(Genre.TYPE, connectionProvider)).initialize();
	}

	@Test
	void invoice() {
		new InvoicePanel(new InvoiceModel(connectionProvider)).initialize();
	}

	@Test
	void invoiceLine() {
		SwingEntityModel model = new SwingEntityModel(new InvoiceLineEditModel(connectionProvider));
		new EntityPanel(model, new InvoiceLineEditPanel(model.editModel(), new JTextField())).initialize();
	}

	@Test
	void mediaType() {
		new MediaTypeEditPanel(new SwingEntityEditModel(MediaType.TYPE, connectionProvider)).initialize();
	}

	@Test
	void playlist() {
		SwingEntityModel model = new SwingEntityModel(new PlaylistEditModel(connectionProvider));
		new EntityPanel(model, new PlaylistEditPanel(model.editModel()), new PlaylistTablePanel(model.tableModel())).initialize();
	}

	@Test
	void playlistTrack() {
		SwingEntityModel model = new SwingEntityModel(new PlaylistTrackEditModel(connectionProvider));
		new EntityPanel(model, new PlaylistTrackEditPanel(model.editModel()), new PlaylistTrackTablePanel(model.tableModel())).initialize();
	}

	@Test
	void preferences() {
		new PreferencesEditPanel(new SwingEntityEditModel(Preferences.TYPE, connectionProvider)).initialize();
	}

	@Test
	void track() {
		SwingEntityModel model = new SwingEntityModel(new TrackTableModel(connectionProvider));
		TrackTableModel tableModel = (TrackTableModel) model.tableModel();
		new EntityPanel(model, new TrackEditPanel((TrackEditModel) model.editModel(), tableModel.selection()), new TrackTablePanel(tableModel)).initialize();
	}
}
