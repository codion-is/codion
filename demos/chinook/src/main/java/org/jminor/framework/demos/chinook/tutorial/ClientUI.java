/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.chinook.tutorial;

import org.jminor.common.User;
import org.jminor.common.Value;
import org.jminor.common.Values;
import org.jminor.common.db.Database;
import org.jminor.common.db.Databases;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.db.valuemap.exception.ValidationException;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.db.local.LocalEntityConnectionProvider;
import org.jminor.framework.demos.chinook.domain.impl.ChinookImpl;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.model.EntityComboBoxModel;
import org.jminor.swing.common.ui.UiUtil;
import org.jminor.swing.common.ui.UiValues;
import org.jminor.swing.common.ui.control.Controls;
import org.jminor.swing.framework.model.SwingEntityEditModel;
import org.jminor.swing.framework.ui.EntityComboBox;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.GridLayout;

import static org.jminor.framework.demos.chinook.domain.Chinook.*;

/**
 * When running this make sure the chinook demo module directory is the
 * working directory, due to a relative path to a db init script
 */
public final class ClientUI {

  static void artistPanel(EntityConnectionProvider connectionProvider) {
    //create a EditModel based on the artist entity
    SwingEntityEditModel editModel =
            new SwingEntityEditModel(T_ARTIST, connectionProvider);

    //create a field for entering a artist name
    JTextField nameField = new JTextField(10);
    //create a String Value based on the artist name in the edit model
    Value<String> editModelNameValue =
            editModel.value(ARTIST_NAME);
    //create a String Value based on the text field
    Value<String> textFieldNameValue =
            UiValues.textValue(nameField);
    //link the two values
    Values.link(editModelNameValue, textFieldNameValue);
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
    editModel.addAfterInsertListener(insertEvent ->
            JOptionPane.showMessageDialog(nameField,
                    "Inserted: " + insertEvent.getInsertedEntities().get(0)));

    JPanel artistPanel = new JPanel(new GridLayout(2, 1, 5, 5));
    artistPanel.add(new JLabel("Artist name"));
    artistPanel.add(nameField);

    //uncomment the below line to display the panel
//    UiUtil.displayInDialog(null, artistPanel, "Artist");
  }

  static void albumPanel(final EntityConnectionProvider connectionProvider) {
    //create a EditModel based on the album entity
    SwingEntityEditModel editModel =
            new SwingEntityEditModel(T_ALBUM, connectionProvider);

    //create a combobox for selecting the album artist
    //based on a combobox model supplied by the edit model
    final EntityComboBoxModel artistComboBoxModel =
            editModel.getForeignKeyComboBoxModel(ALBUM_ARTIST_FK);
    EntityComboBox artistComboBox = new EntityComboBox(artistComboBoxModel);
    //limit the combo box width, due to long artist names
    UiUtil.setPreferredWidth(artistComboBox, 240);
    //move focus with Enter key
    UiUtil.transferFocusOnEnter(artistComboBox);
    //populate the combo box model
    artistComboBoxModel.refresh();
    //create a Entity Value based on the album artist in the edit model
    Value<Entity> editModelArtistValue =
            editModel.value(ALBUM_ARTIST_FK);
    //create a Entity Value based on the combobox
    Value<Entity> comboBoxArtistValue =
            UiValues.selectedItemValue(artistComboBox);
    //link the two values
    Values.link(editModelArtistValue, comboBoxArtistValue);

    //create a field for entering a album title
    JTextField titleField = new JTextField(10);
    //create a String Value based on the album title in the edit model
    Value<String> editModelNameValue =
            editModel.value(ALBUM_TITLE);
    //create a String Value based on the text field
    Value<String> textFieldTitleValue =
            UiValues.textValue(titleField);
    //link the two values
    Values.link(editModelNameValue, textFieldTitleValue);

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
    editModel.addAfterInsertListener(insertEvent ->
            JOptionPane.showMessageDialog(titleField,
                    "Inserted: " + insertEvent.getInsertedEntities().get(0)));

    JPanel albumPanel = new JPanel(new GridLayout(4, 1, 5, 5));
    albumPanel.add(new JLabel("Artist"));
    albumPanel.add(artistComboBox);
    albumPanel.add(new JLabel("Title"));
    albumPanel.add(titleField);

    //uncomment the below line to display the panel
//    UiUtil.displayInDialog(null, albumPanel, "Album");
  }

  public static void main(final String[] args) {
    // Configure the datababase
    Database.DATABASE_TYPE.set(Database.Type.H2.toString());
    Database.DATABASE_EMBEDDED_IN_MEMORY.set(true);
    Database.DATABASE_INIT_SCRIPT.set("src/main/sql/create_schema.sql");
    //initialize a connection provider, this class is responsible
    //for supplying a valid connection or throwing an exception
    //in case a connection can not be established
    EntityConnectionProvider connectionProvider =
            new LocalEntityConnectionProvider(Databases.getInstance())
                    .setDomainClassName(ChinookImpl.class.getName())
                    .setUser(new User("scott", "tiger".toCharArray()));

    artistPanel(connectionProvider);
    albumPanel(connectionProvider);
  }
}
