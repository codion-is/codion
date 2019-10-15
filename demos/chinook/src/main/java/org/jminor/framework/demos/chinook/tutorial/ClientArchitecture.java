/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.chinook.tutorial;

import org.jminor.common.User;
import org.jminor.common.db.Database;
import org.jminor.common.db.Databases;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.db.local.LocalEntityConnectionProvider;
import org.jminor.framework.demos.chinook.domain.Chinook;
import org.jminor.framework.demos.chinook.domain.impl.ChinookImpl;
import org.jminor.swing.framework.model.SwingEntityEditModel;
import org.jminor.swing.framework.model.SwingEntityModel;
import org.jminor.swing.framework.model.SwingEntityTableModel;
import org.jminor.swing.framework.ui.EntityEditPanel;
import org.jminor.swing.framework.ui.EntityPanel;
import org.jminor.swing.framework.ui.EntityTablePanel;

/**
 * When running this make sure the chinook demo module directory is the
 * working directory, due to a relative path to a db init script
 */
public final class ClientArchitecture {

  // tag::entityModel[]
  static SwingEntityModel artistModel(EntityConnectionProvider connectionProvider) {
    //initialize a default edit model
    SwingEntityEditModel artistEditModel =
            new SwingEntityEditModel(Chinook.T_ARTIST, connectionProvider);
    //initialize a default table model
    SwingEntityTableModel artistTableModel =
            new SwingEntityTableModel(Chinook.T_ARTIST, connectionProvider);
    //initialize a default model using the edit and table models
    SwingEntityModel artistModel =
            new SwingEntityModel(artistEditModel, artistTableModel);

    //Note that this does the same as the above, that is, initialises
    //a SwingEntityModel with a default edit and table model
    SwingEntityModel albumModel =
            new SwingEntityModel(Chinook.T_ALBUM, connectionProvider);

    artistModel.addDetailModel(albumModel);

    return artistModel;
  }
  // end::entityModel[]
  // tag::entityPanel[]
  static EntityPanel artistPanel(EntityConnectionProvider connectionProvider) {
    //initialize the EntityModel to base the panel on
    SwingEntityModel artistModel = artistModel(connectionProvider);
    //fetch the edit model
    SwingEntityEditModel artistEditModel = artistModel.getEditModel();
    //fetch the table model
    SwingEntityTableModel artistTableModel = artistModel.getTableModel();
    //fetch the album detail model
    SwingEntityModel albumModel = artistModel.getDetailModel(Chinook.T_ALBUM);

    //create a EntityEditPanel instance, based on the artist edit model
    EntityEditPanel artistEditPanel = new EntityEditPanel(artistEditModel) {
      @Override
      protected void initializeUI() {
        createTextField(Chinook.ARTIST_NAME).setColumns(15);
        addPropertyPanel(Chinook.ARTIST_NAME);
      }
    };
    //create a EntityTablePanel instance, based on the artist table model
    EntityTablePanel artistTablePanel = new EntityTablePanel(artistTableModel);
    //create a EntityPanel instance, based on the artist model and
    //the edit and table panels from above
    EntityPanel artistPanel = new EntityPanel(artistModel, artistEditPanel, artistTablePanel);

    //create a new EntityPanel, without an edit panel and
    //with a default EntityTablePanel
    EntityPanel albumPanel = new EntityPanel(albumModel);

    artistPanel.addDetailPanel(albumPanel);

    return artistPanel;
  }
  // end::entityPanel[]

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

    final EntityPanel artistPanel = artistPanel(connectionProvider);
    //lazy initialization of the UI
    artistPanel.initializePanel();
    //fetch data from the database
    artistPanel.getModel().refresh();

    //uncomment the below line to display the panel
//    displayInDialog(null, artistPanel, "Artists");

    connectionProvider.disconnect();
  }
}
