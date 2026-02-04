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
package is.codion.demos.chinook.tutorial;

import is.codion.common.db.database.Database;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.reactive.value.Value;
import is.codion.common.utilities.user.User;
import is.codion.demos.chinook.domain.ChinookImpl;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.exception.ValidationException;
import is.codion.swing.common.ui.component.Components;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.model.component.EntityComboBoxModel;
import is.codion.swing.framework.ui.component.EntityComboBox;

import javax.swing.BorderFactory;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import static is.codion.demos.chinook.domain.api.Chinook.Album;
import static is.codion.demos.chinook.domain.api.Chinook.Artist;

/**
 * When running this make sure the chinook demo module directory is the
 * working directory, due to a relative path to a db init script
 */
public final class ClientUI {

	static void artistPanel(EntityConnectionProvider connectionProvider) {
		// create a EditModel based on the artist entity
		SwingEntityEditModel editModel = new SwingEntityEditModel(Artist.TYPE, connectionProvider);

		// fetch the Value representing the artist name from the editor
		Value<String> artistNameEditModelValue = editModel.editor().value(Artist.NAME);

		// create a Control for inserting a new Artist
		Control insertControl = Control.action(actionEvent -> {
			try {
				// insert the entity
				editModel.insert();
				// clear the edit model after a successful insert
				editModel.editor().defaults();
			}
			catch (DatabaseException | ValidationException e) {
				JOptionPane.showMessageDialog((JTextField) actionEvent.getSource(),
								e.getMessage(), "Unable to insert", JOptionPane.ERROR_MESSAGE);
			}
		});
		// create a textfield for entering an artist name
		JTextField artistNameTextField =
						// link the text field to the edit model value
						Components.stringField()
										.link(artistNameEditModelValue)
										.columns(10)
										// trigger the insert action on pressing Enter
										.action(insertControl)
										.build();

		// show a message after insert
		editModel.afterInsert().addConsumer(insertedEntities ->
						JOptionPane.showMessageDialog(artistNameTextField,
										"Inserted: " + insertedEntities.iterator().next()));

		JPanel artistPanel = Components.gridLayoutPanel(2, 1)
						.add(Components.label("Artist name"))
						.add(artistNameTextField)
						.border(BorderFactory.createEmptyBorder(10, 10, 10, 10))
						.build();

		// uncomment the below lines to display the panel
//    Dialogs.componentDialog(artistPanel)
//            .title("Artist")
//            .show();
	}

	static void albumPanel(EntityConnectionProvider connectionProvider) {
		// create a EditModel based on the album entity
		SwingEntityEditModel editModel = new SwingEntityEditModel(Album.TYPE, connectionProvider);

		// fetch the Value representing the album artist from the editor
		Value<Entity> editModelArtistValue = editModel.editor().value(Album.ARTIST_FK);

		EntityComboBoxModel artistComboBoxModel = editModel.editor().comboBoxModels().get(Album.ARTIST_FK);

		// create a combobox for selecting the album artist
		// based on a combobox model supplied by the edit model
		EntityComboBox artistComboBox =
						// link the combo box to the edit model value
						EntityComboBox.builder()
										.model(artistComboBoxModel)
										.link(editModelArtistValue)
										// limit the combo box width, due to long artist names
										.preferredWidth(240)
										// move focus with Enter key
										.transferFocusOnEnter(true)
										// populate the combo box model when shown
										.onSetVisible(comboBox -> comboBox.getModel().items().refresh())
										.build();

		// fetch the Value representing the album title from the editor
		Value<String> editModelTitleValue = editModel.editor().value(Album.TITLE);

		// create a Control for inserting a new Album row
		Control insertControl = Control.action(actionEvent -> {
			try {
				// insert the entity
				editModel.insert();
				// clear the edit model after a successful insert
				editModel.editor().defaults();
				// and transfer the focus to the combo box
				artistComboBox.requestFocusInWindow();
			}
			catch (DatabaseException | ValidationException e) {
				JOptionPane.showMessageDialog((JTextField) actionEvent.getSource(),
								e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
			}
		});
		// create a text field based on the title value
		JTextField titleTextField =
						Components.stringField()
										// link the text field to the edit model value
										.link(editModelTitleValue)
										.columns(10)
										// add an insert action to the title field
										// so that we can insert by pressing Enter
										.action(insertControl)
										.build();

		// show a message after insert
		editModel.afterInsert().addConsumer(insertedEntities ->
						JOptionPane.showMessageDialog(titleTextField,
										"Inserted: " + insertedEntities.iterator().next()));

		JPanel albumPanel = Components.gridLayoutPanel(4, 1)
						.add(Components.label("Artist"))
						.add(artistComboBox)
						.add(Components.label("Title"))
						.add(titleTextField)
						.border(BorderFactory.createEmptyBorder(10, 10, 10, 10))
						.build();

		// uncomment the below lines to display the panel
//    Dialogs.componentDialog(albumPanel)
//            .title("Album")
//            .show();
	}

	public static void main(String[] args) {
		// Configure the database
		Database.URL.set("jdbc:h2:mem:h2db");
		Database.INIT_SCRIPTS.set("src/main/sql/create_schema.sql");

		// initialize a connection provider, this class is responsible
		// for supplying a valid connection or throwing an exception
		// in case a connection can not be established
		EntityConnectionProvider connectionProvider =
						LocalEntityConnectionProvider.builder()
										.domain(new ChinookImpl())
										.user(User.parse("scott:tiger"))
										.build();

		artistPanel(connectionProvider);
		albumPanel(connectionProvider);
	}
}
