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
 * Copyright (c) 2025 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.demos.chinook.ui;

import is.codion.common.utilities.user.User;
import is.codion.demos.chinook.domain.ChinookImpl;
import is.codion.demos.chinook.domain.api.Chinook.MediaType;
import is.codion.demos.chinook.domain.api.Chinook.Preferences;
import is.codion.demos.chinook.model.AlbumModel;
import is.codion.demos.chinook.model.ArtistTableModel;
import is.codion.demos.chinook.model.CustomerModel;
import is.codion.demos.chinook.model.EmployeeModel;
import is.codion.demos.chinook.model.GenreModel;
import is.codion.demos.chinook.model.InvoiceLineEditModel;
import is.codion.demos.chinook.model.InvoiceModel;
import is.codion.demos.chinook.model.PlaylistModel;
import is.codion.demos.chinook.model.PlaylistTrackEditModel;
import is.codion.demos.chinook.model.TrackEditModel;
import is.codion.demos.chinook.model.TrackTableModel;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.local.LocalEntityConnection;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.ui.EntityPanel;
import is.codion.swing.framework.ui.icon.FrameworkIcons;

import org.junit.jupiter.api.Test;

import javax.swing.JTextField;

public final class ChinookPanelsTest {

	static {
		FrameworkIcons icons = FrameworkIcons.instance();
		icons.put("plus", ChinookAppPanel.class.getResource("plus.svg"));
		icons.put("minus", ChinookAppPanel.class.getResource("minus.svg"));
	}

	private final EntityConnection connection =
					LocalEntityConnection.builder()
									.domain(new ChinookImpl())
									.user(User.parse(System.getProperty("codion.test.user")))
									.build();

	@Test
	void album() {
		new AlbumPanel(new AlbumModel(connection)).initialize();
	}

	@Test
	void artist() {
		SwingEntityModel model = new SwingEntityModel(new ArtistTableModel(connection));
		new EntityPanel(model, new ArtistEditPanel(model.editModel())).initialize();
	}

	@Test
	void customer() {
		new CustomerPanel(new CustomerModel(connection)).initialize();
	}

	@Test
	void employee() {
		new EmployeePanel(new EmployeeModel(connection)).initialize();
	}

	@Test
	void genre() {
		new GenrePanel(new GenreModel(connection)).initialize();
	}

	@Test
	void invoice() {
		new InvoicePanel(new InvoiceModel(connection)).initialize();
	}

	@Test
	void invoiceLine() {
		SwingEntityModel model = new SwingEntityModel(new InvoiceLineEditModel(connection));
		new EntityPanel(model, new InvoiceLineEditPanel(model.editModel(), new JTextField())).initialize();
	}

	@Test
	void mediaType() {
		new MediaTypeEditPanel(new SwingEntityEditModel(MediaType.TYPE, connection)).initialize();
	}

	@Test
	void playlist() {
		SwingEntityModel model = new PlaylistModel(connection);
		new EntityPanel(model, new PlaylistEditPanel(model.editModel()),
						new PlaylistTablePanel(model.tableModel())).initialize();
	}

	@Test
	void playlistTrack() {
		SwingEntityModel model = new SwingEntityModel(new PlaylistTrackEditModel(connection));
		new EntityPanel(model, new PlaylistTrackEditPanel(model.editModel()),
						new PlaylistTrackTablePanel(model.tableModel())).initialize();
	}

	@Test
	void preferences() {
		new PreferencesEditPanel(new SwingEntityEditModel(Preferences.TYPE, connection)).initialize();
	}

	@Test
	void track() {
		SwingEntityModel model = new SwingEntityModel(new TrackTableModel(connection));
		TrackTableModel tableModel = (TrackTableModel) model.tableModel();
		new EntityPanel(model, new TrackEditPanel((TrackEditModel) model.editModel(), tableModel.selection()),
						new TrackTablePanel(tableModel)).initialize();
	}
}
