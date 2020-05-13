/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.framework.demos.chinook.tutorial;

import dev.codion.common.db.database.Database;
import dev.codion.common.db.database.Databases;
import dev.codion.common.db.exception.DatabaseException;
import dev.codion.common.user.Users;
import dev.codion.common.value.Value;
import dev.codion.framework.db.EntityConnectionProvider;
import dev.codion.framework.db.local.LocalEntityConnectionProvider;
import dev.codion.framework.demos.chinook.domain.impl.ChinookImpl;
import dev.codion.framework.domain.entity.Entity;
import dev.codion.framework.domain.entity.exception.ValidationException;
import dev.codion.swing.common.ui.Components;
import dev.codion.swing.common.ui.KeyEvents;
import dev.codion.swing.common.ui.control.Controls;
import dev.codion.swing.common.ui.value.SelectedValues;
import dev.codion.swing.common.ui.value.TextValues;
import dev.codion.swing.framework.model.SwingEntityComboBoxModel;
import dev.codion.swing.framework.model.SwingEntityEditModel;
import dev.codion.swing.framework.ui.EntityComboBox;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import static dev.codion.framework.demos.chinook.domain.Chinook.*;
import static dev.codion.swing.common.ui.layout.Layouts.gridLayout;

/**
 * When running this make sure the chinook demo module directory is the
 * working directory, due to a relative path to a db init script
 */
public final class ClientUI {

  static void artistPanel(EntityConnectionProvider connectionProvider) {
    //create a EditModel based on the artist entity
    SwingEntityEditModel editModel = new SwingEntityEditModel(T_ARTIST, connectionProvider);

    //create a field for entering a artist name
    JTextField nameField = new JTextField(10);
    //create a String Value based on the artist name in the edit model
    Value<String> editModelNameValue = editModel.value(ARTIST_NAME);
    //create a String Value based on the text field
    Value<String> textFieldNameValue = TextValues.textValue(nameField);
    //link the two values
    editModelNameValue.link(textFieldNameValue);
    //add a insert action to the name field
    //so we can insert by pressing Enter
    nameField.addActionListener(Controls.control(() -> {
      try {
        //insert the entity
        editModel.insert();
        //clear the edit model after a successful insert
        editModel.setEntity(null);
      }
      catch (DatabaseException | ValidationException e) {
        JOptionPane.showMessageDialog(nameField, e.getMessage(),
                "Insert error", JOptionPane.ERROR_MESSAGE);
      }
    }));
    //show a message after insert
    editModel.addAfterInsertListener(insertedEntities ->
            JOptionPane.showMessageDialog(nameField,
                    "Inserted: " + insertedEntities.get(0)));

    JPanel artistPanel = new JPanel(gridLayout(2, 1));
    artistPanel.add(new JLabel("Artist name"));
    artistPanel.add(nameField);

    //uncomment the below line to display the panel
//    Dialogs.displayInDialog(null, artistPanel, "Artist");
  }

  static void albumPanel(final EntityConnectionProvider connectionProvider) {
    //create a EditModel based on the album entity
    SwingEntityEditModel editModel = new SwingEntityEditModel(T_ALBUM, connectionProvider);

    //create a combobox for selecting the album artist
    //based on a combobox model supplied by the edit model
    final SwingEntityComboBoxModel artistComboBoxModel = editModel.getForeignKeyComboBoxModel(ALBUM_ARTIST_FK);
    EntityComboBox artistComboBox = new EntityComboBox(artistComboBoxModel);
    //limit the combo box width, due to long artist names
    Components.setPreferredWidth(artistComboBox, 240);
    //move focus with Enter key
    KeyEvents.transferFocusOnEnter(artistComboBox);
    //populate the combo box model
    artistComboBoxModel.refresh();
    //create a Entity Value based on the album artist in the edit model
    Value<Entity> editModelArtistValue = editModel.value(ALBUM_ARTIST_FK);
    //create a Entity Value based on the combobox
    Value<Entity> comboBoxArtistValue = SelectedValues.selectedValue(artistComboBox);
    //link the two values
    editModelArtistValue.link(comboBoxArtistValue);

    //create a field for entering a album title
    JTextField titleField = new JTextField(10);
    //create a String Value based on the album title in the edit model
    Value<String> editModelNameValue = editModel.value(ALBUM_TITLE);
    //create a String Value based on the text field
    Value<String> textFieldTitleValue = TextValues.textValue(titleField);
    //link the two values
    editModelNameValue.link(textFieldTitleValue);

    //add a insert action to the title field
    //so we can insert by pressing Enter
    titleField.addActionListener(Controls.control(() -> {
      try {
        editModel.insert();
        //clear the edit model after a successful insert
        editModel.setEntity(null);
        //and set the focus on the combo box
        artistComboBox.requestFocusInWindow();
      }
      catch (DatabaseException | ValidationException e) {
        JOptionPane.showMessageDialog(titleField, e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
      }
    }));
    //show a message after insert
    editModel.addAfterInsertListener(insertedEntities ->
            JOptionPane.showMessageDialog(titleField,
                    "Inserted: " + insertedEntities.get(0)));

    JPanel albumPanel = new JPanel(gridLayout(4, 1));
    albumPanel.add(new JLabel("Artist"));
    albumPanel.add(artistComboBox);
    albumPanel.add(new JLabel("Title"));
    albumPanel.add(titleField);

    //uncomment the below line to display the panel
//    Dialogs.displayInDialog(null, albumPanel, "Album");
  }

  public static void main(final String[] args) {
    // Configure the database
    Database.DATABASE_URL.set("jdbc:h2:mem:h2db");
    Database.DATABASE_INIT_SCRIPT.set("src/main/sql/create_schema.sql");
    //initialize a connection provider, this class is responsible
    //for supplying a valid connection or throwing an exception
    //in case a connection can not be established
    EntityConnectionProvider connectionProvider =
            new LocalEntityConnectionProvider(Databases.getInstance())
                    .setDomainClassName(ChinookImpl.class.getName())
                    .setUser(Users.parseUser("scott:tiger"));

    artistPanel(connectionProvider);
    albumPanel(connectionProvider);
  }
}
