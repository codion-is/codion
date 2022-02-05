/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.tutorial;

import is.codion.common.db.database.Database;
import is.codion.common.db.database.DatabaseFactory;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.user.User;
import is.codion.common.value.Value;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.demos.chinook.domain.impl.ChinookImpl;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.exception.ValidationException;
import is.codion.swing.common.ui.Sizes;
import is.codion.swing.common.ui.TransferFocusOnEnter;
import is.codion.swing.common.ui.component.ComponentValues;
import is.codion.swing.common.ui.component.Components;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.framework.model.SwingEntityComboBoxModel;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.EntityComboBox;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import static is.codion.framework.demos.chinook.domain.Chinook.Album;
import static is.codion.framework.demos.chinook.domain.Chinook.Artist;
import static is.codion.swing.common.ui.layout.Layouts.gridLayout;

/**
 * When running this make sure the chinook demo module directory is the
 * working directory, due to a relative path to a db init script
 */
public final class ClientUI {

  static void artistPanel(EntityConnectionProvider connectionProvider) {
    // create a EditModel based on the artist entity
    SwingEntityEditModel editModel = new SwingEntityEditModel(Artist.TYPE, connectionProvider);

    // create a field for entering an artist name
    JTextField nameField = new JTextField(10);

    // create a String Value based on the artist name in the edit model
    Value<String> editModelNameValue = editModel.value(Artist.NAME);

    // create a String Value based on the text field
    Value<String> textFieldNameValue = ComponentValues.textComponent(nameField);

    // link the two values
    textFieldNameValue.link(editModelNameValue);

    // add an insert action to the name field
    // so that we can insert by pressing Enter
    nameField.addActionListener(Control.control(() -> {
      try {
        // insert the entity
        editModel.insert();

        // clear the edit model after a successful insert
        editModel.setDefaultValues();
      }
      catch (DatabaseException | ValidationException e) {
        JOptionPane.showMessageDialog(nameField, e.getMessage(),
                "Insert error", JOptionPane.ERROR_MESSAGE);
      }
    }));

    // show a message after insert
    editModel.addAfterInsertListener(insertedEntities ->
            JOptionPane.showMessageDialog(nameField,
                    "Inserted: " + insertedEntities.get(0)));

    JPanel artistPanel = new JPanel(gridLayout(2, 1));
    artistPanel.add(new JLabel("Artist name"));
    artistPanel.add(nameField);

    // uncomment the below line to display the panel
//    Dialogs.displayInDialog(null, artistPanel, "Artist");
  }

  static void albumPanel(final EntityConnectionProvider connectionProvider) {
    // create a EditModel based on the album entity
    SwingEntityEditModel editModel = new SwingEntityEditModel(Album.TYPE, connectionProvider);

    // create a combobox for selecting the album artist
    // based on a combobox model supplied by the edit model
    final SwingEntityComboBoxModel artistComboBoxModel = editModel.getForeignKeyComboBoxModel(Album.ARTIST_FK);
    EntityComboBox artistComboBox = new EntityComboBox(artistComboBoxModel);

    // limit the combo box width, due to long artist names
    Sizes.setPreferredWidth(artistComboBox, 240);

    // move focus with Enter key
    TransferFocusOnEnter.enable(artistComboBox);

    // populate the combo box model
    artistComboBoxModel.refresh();

    // create an Entity Value based on the album artist in the edit model
    Value<Entity> editModelArtistValue = editModel.value(Album.ARTIST_FK);

    // create an Entity Value based on the combobox
    Value<Entity> comboBoxArtistValue = ComponentValues.comboBox(artistComboBox);

    // link the two values
    comboBoxArtistValue.link(editModelArtistValue);

    // create a field for entering an album title
    JTextField titleField = new JTextField(10);

    // create a String Value based on the album title in the edit model
    Value<String> editModelTitleValue = editModel.value(Album.TITLE);

    // create a String Value based on the text field
    Value<String> textFieldTitleValue = ComponentValues.textComponent(titleField);

    // link the two values
    textFieldTitleValue.link(editModelTitleValue);

    // add an insert action to the title field
    // so that we can insert by pressing Enter
    titleField.addActionListener(Control.control(() -> {
      try {
        editModel.insert();

        // clear the edit model after a successful insert
        editModel.setDefaultValues();

        // and set the focus on the combo box
        artistComboBox.requestFocusInWindow();
      }
      catch (DatabaseException | ValidationException e) {
        JOptionPane.showMessageDialog(titleField, e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
      }
    }));

    // show a message after insert
    editModel.addAfterInsertListener(insertedEntities ->
            JOptionPane.showMessageDialog(titleField,
                    "Inserted: " + insertedEntities.get(0)));

    JPanel albumPanel = Components.panel(gridLayout(4, 1))
            .add(new JLabel("Artist"))
            .add(artistComboBox)
            .add(new JLabel("Title"))
            .add(titleField)
            .build();

    // uncomment the below line to display the panel
//    Dialogs.displayInDialog(null, albumPanel, "Album");
  }

  public static void main(final String[] args) {
    // Configure the database
    Database.DATABASE_URL.set("jdbc:h2:mem:h2db");
    Database.DATABASE_INIT_SCRIPTS.set("src/main/sql/create_schema.sql");

    // initialize a connection provider, this class is responsible
    // for supplying a valid connection or throwing an exception
    // in case a connection can not be established
    EntityConnectionProvider connectionProvider =
            new LocalEntityConnectionProvider(DatabaseFactory.getDatabase())
                    .setDomainClassName(ChinookImpl.class.getName())
                    .setUser(User.parseUser("scott:tiger"));

    artistPanel(connectionProvider);
    albumPanel(connectionProvider);
  }
}
