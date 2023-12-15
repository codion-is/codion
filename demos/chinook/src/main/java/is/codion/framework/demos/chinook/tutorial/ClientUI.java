/*
 * Copyright (c) 2004 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.tutorial;

import is.codion.common.db.database.Database;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.user.User;
import is.codion.common.value.Value;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.demos.chinook.domain.impl.ChinookImpl;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.exception.ValidationException;
import is.codion.swing.common.ui.component.Components;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.model.component.EntityComboBoxModel;
import is.codion.swing.framework.ui.component.EntityComboBox;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import static is.codion.framework.demos.chinook.domain.Chinook.Album;
import static is.codion.framework.demos.chinook.domain.Chinook.Artist;

/**
 * When running this make sure the chinook demo module directory is the
 * working directory, due to a relative path to a db init script
 */
public final class ClientUI {

  static void artistPanel(EntityConnectionProvider connectionProvider) {
    // create a EditModel based on the artist entity
    SwingEntityEditModel editModel = new SwingEntityEditModel(Artist.TYPE, connectionProvider);

    // fetch a Value based on the artist name from the edit model
    Value<String> artistNameEditModelValue = editModel.value(Artist.NAME);

    // create a Control for inserting a new Artist
    Control insertControl = Control.actionControl(actionEvent -> {
      try {
        // insert the entity
        editModel.insert();
        // clear the edit model after a successful insert
        editModel.setDefaults();
      }
      catch (DatabaseException | ValidationException e) {
        JOptionPane.showMessageDialog((JTextField) actionEvent.getSource(),
                e.getMessage(), "Unable to insert", JOptionPane.ERROR_MESSAGE);
      }
    });
    // create a textfield for entering an artist name
    JTextField artistNameTextField =
            // link the text field to the edit model value
            Components.stringField(artistNameEditModelValue)
                    .columns(10)
                    // trigger the insert action on pressing Enter
                    .action(insertControl)
                    .build();

    // show a message after insert
    editModel.addAfterInsertListener(insertedEntities ->
            JOptionPane.showMessageDialog(artistNameTextField,
                    "Inserted: " + insertedEntities.iterator().next()));

    JPanel artistPanel = Components.gridLayoutPanel(2, 1)
            .add(new JLabel("Artist name"))
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

    // fetch Value based on the album artist in the edit model
    Value<Entity> editModelArtistValue = editModel.value(Album.ARTIST_FK);

    EntityComboBoxModel artistComboBoxModel = editModel.foreignKeyComboBoxModel(Album.ARTIST_FK);

    // create a combobox for selecting the album artist
    // based on a combobox model supplied by the edit model
    EntityComboBox artistComboBox =
            // link the combo box to the edit model value
            EntityComboBox.builder(artistComboBoxModel, editModelArtistValue)
                    // limit the combo box width, due to long artist names
                    .preferredWidth(240)
                    // move focus with Enter key
                    .transferFocusOnEnter(true)
                    // populate the combo box model when shown
                    .onSetVisible(comboBox -> comboBox.getModel().refresh())
                    .build();

    // fetch a String Value based on the album title from the edit model
    Value<String> editModelTitleValue = editModel.value(Album.TITLE);

    // create a Control for inserting a new Album row
    Control insertControl = Control.actionControl(actionEvent -> {
      try {
        // insert the entity
        editModel.insert();
        // clear the edit model after a successful insert
        editModel.setDefaults();
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
            // link the text field to the edit model value
            Components.stringField(editModelTitleValue)
                    .columns(10)
                    // add an insert action to the title field
                    // so that we can insert by pressing Enter
                    .action(insertControl)
                    .build();

    // show a message after insert
    editModel.addAfterInsertListener(insertedEntities ->
            JOptionPane.showMessageDialog(titleTextField,
                    "Inserted: " + insertedEntities.iterator().next()));

    JPanel albumPanel = Components.gridLayoutPanel(4, 1)
            .add(new JLabel("Artist"))
            .add(artistComboBox)
            .add(new JLabel("Title"))
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
    Database.DATABASE_URL.set("jdbc:h2:mem:h2db");
    Database.DATABASE_INIT_SCRIPTS.set("src/main/sql/create_schema.sql");

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
