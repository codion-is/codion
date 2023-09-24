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
 * Copyright (c) 2004 - 2023, Björn Darri Sigurðsson.
 */
package is.codion.framework.demos.chinook.tutorial;

import is.codion.common.db.database.Database;
import is.codion.common.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.demos.chinook.domain.Chinook.Album;
import is.codion.framework.demos.chinook.domain.Chinook.Artist;
import is.codion.framework.demos.chinook.domain.impl.ChinookImpl;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.model.SwingEntityTableModel;
import is.codion.swing.framework.ui.EntityEditPanel;
import is.codion.swing.framework.ui.EntityPanel;
import is.codion.swing.framework.ui.EntityTablePanel;

/**
 * When running this make sure the chinook demo module directory is the
 * working directory, due to a relative path to a db init script
 */
public final class ClientArchitecture {

  // tag::entityModel[]
  /**
   * Creates a SwingEntityModel based on the {@link Artist#TYPE} entity
   * with a detail model based on {@link Album#TYPE}
   * @param connectionProvider the connection provider
   */
  static SwingEntityModel artistModel(EntityConnectionProvider connectionProvider) {
    // create a default edit model
    SwingEntityEditModel artistEditModel =
            new SwingEntityEditModel(Artist.TYPE, connectionProvider);

    // create a default table model, wrapping the edit model
    SwingEntityTableModel artistTableModel =
            new SwingEntityTableModel(artistEditModel);

    // create a default model wrapping the table model
    SwingEntityModel artistModel =
            new SwingEntityModel(artistTableModel);

    // Note that this does the same as the above, that is, creates
    // a SwingEntityModel with a default edit and table model
    SwingEntityModel albumModel =
            new SwingEntityModel(Album.TYPE, connectionProvider);

    artistModel.addDetailModel(albumModel);

    return artistModel;
  }
  // end::entityModel[]
  // tag::entityPanel[]
  /**
   * Creates a EntityPanel based on the {@link Artist#TYPE} entity
   * with a detail panel based on {@link Album#TYPE}
   * @param connectionProvider the connection provider
   */
  static EntityPanel artistPanel(EntityConnectionProvider connectionProvider) {
    // create the EntityModel to base the panel on (calling the above method)
    SwingEntityModel artistModel = artistModel(connectionProvider);

    // the edit model
    SwingEntityEditModel artistEditModel = artistModel.editModel();

    // the table model
    SwingEntityTableModel artistTableModel = artistModel.tableModel();

    // the album detail model
    SwingEntityModel albumModel = artistModel.detailModel(Album.TYPE);

    // create a EntityEditPanel instance, based on the artist edit model
    EntityEditPanel artistEditPanel = new EntityEditPanel(artistEditModel) {
      @Override
      protected void initializeUI() {
        createTextField(Artist.NAME).columns(15);
        addInputPanel(Artist.NAME);
      }
    };
    // create a EntityTablePanel instance, based on the artist table model
    EntityTablePanel artistTablePanel = new EntityTablePanel(artistTableModel);

    // create a EntityPanel instance, based on the artist model and
    // the edit and table panels from above
    EntityPanel artistPanel = new EntityPanel(artistModel, artistEditPanel, artistTablePanel);

    // create a new EntityPanel, without an edit panel and
    // with a default EntityTablePanel
    EntityPanel albumPanel = new EntityPanel(albumModel);

    artistPanel.addDetailPanel(albumPanel);

    return artistPanel;
  }
  // end::entityPanel[]

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

    EntityPanel artistPanel = artistPanel(connectionProvider);

    // lazy initialization
    artistPanel.initialize();

    // fetch data from the database
    artistPanel.model().tableModel().refresh();

    // uncomment the below line to display the panel
//    displayInDialog(null, artistPanel, "Artists");

    connectionProvider.close();
  }
}
