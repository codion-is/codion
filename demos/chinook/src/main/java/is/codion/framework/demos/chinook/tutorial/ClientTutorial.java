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
import is.codion.framework.domain.DefaultDomain;
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.StringFactory;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.swing.common.ui.component.table.FilteredTable;
import is.codion.swing.framework.model.SwingEntityApplicationModel;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.ui.EntityApplicationPanel;
import is.codion.swing.framework.ui.EntityEditPanel;
import is.codion.swing.framework.ui.EntityPanel;

import javax.swing.JTable;
import javax.swing.UIManager;
import java.awt.Color;
import java.util.List;

import static is.codion.framework.demos.chinook.tutorial.ClientTutorial.Chinook.Album;
import static is.codion.framework.demos.chinook.tutorial.ClientTutorial.Chinook.Artist;
import static is.codion.framework.domain.DomainType.domainType;
import static is.codion.framework.domain.entity.KeyGenerator.automatic;
import static is.codion.swing.common.ui.Windows.screenSizeRatio;
import static is.codion.swing.common.ui.layout.Layouts.gridLayout;
import static java.util.Collections.singletonList;

/**
 * When running this make sure the chinook demo module directory is the
 * working directory, due to a relative path to a db init script
 */
public final class ClientTutorial {

  public static final class Chinook extends DefaultDomain {

    static final DomainType DOMAIN = domainType(Chinook.class);

    public interface Artist {
      EntityType TYPE = DOMAIN.entityType("chinook.artist");

      Column<Integer> ID = TYPE.integerColumn("artistid");
      Column<String> NAME = TYPE.stringColumn("name");
      Column<Integer> NUMBER_OF_ALBUMS = TYPE.integerColumn("number_of_albums");
    }

    public interface Album {
      EntityType TYPE = DOMAIN.entityType("chinook.album");

      Column<Integer> ID = TYPE.integerColumn("albumid");
      Column<String> TITLE = TYPE.stringColumn("title");
      Column<Integer> ARTIST_ID = TYPE.integerColumn("artistid");

      ForeignKey ARTIST_FK = TYPE.foreignKey("artist_fk", ARTIST_ID, Artist.ID);
    }

    public Chinook() {
      super(DOMAIN);
      add(Artist.TYPE.define(
              Artist.ID.define()
                    .primaryKey(),
              Artist.NAME.define()
                      .column()
                      .caption("Name")
                      .searchColumn(true)
                      .nullable(false)
                      .maximumLength(120),
              Artist.NUMBER_OF_ALBUMS.define()
                      .subquery("SELECT COUNT(*) FROM chinook.album " +
                              "WHERE album.artistid = artist.artistid")
                      .caption("Albums"))
              .keyGenerator(automatic("chinook.artist"))
              .stringFactory(Artist.NAME)
              .caption("Artists"));

      add(Artist.TYPE.define(
              Album.ID.define()
                      .primaryKey(),
              Album.ARTIST_ID.define()
                      .column()
                      .nullable(false),
              Album.ARTIST_FK.define()
                      .foreignKey()
                      .caption("Artist"),
              Album.TITLE.define()
                      .column()
                      .caption("Title")
                      .nullable(false)
                      .maximumLength(160))
              .keyGenerator(automatic("chinook.artist"))
              .stringFactory(StringFactory.builder()
                      .value(Album.ARTIST_FK)
                      .text(" - ")
                      .value(Album.TITLE)
                      .build())
              .caption("Albums"));
    }
  }

  private static final class ArtistEditPanel extends EntityEditPanel {

    private ArtistEditPanel(SwingEntityEditModel editModel) {
      super(editModel);
    }

    @Override
    protected void initializeUI() {
      initialFocusAttribute().set(Artist.NAME);
      createTextField(Artist.NAME)
              .columns(15);
      addInputPanel(Artist.NAME);
    }
  }

  private static final class AlbumEditPanel extends EntityEditPanel {

    private AlbumEditPanel(SwingEntityEditModel editModel) {
      super(editModel);
    }

    @Override
    protected void initializeUI() {
      initialFocusAttribute().set(Album.ARTIST_FK);
      createForeignKeySearchField(Album.ARTIST_FK)
              .columns(15);
      createTextField(Album.TITLE)
              .action(control(EditControl.INSERT).get())
              .columns(15);
      setLayout(gridLayout(2, 1));
      addInputPanel(Album.ARTIST_FK);
      addInputPanel(Album.TITLE);
    }
  }

  private static final class ApplicationModel extends SwingEntityApplicationModel {

    private ApplicationModel(EntityConnectionProvider connectionProvider) {
      super(connectionProvider);
      SwingEntityModel artistModel = new SwingEntityModel(Artist.TYPE, connectionProvider);
      SwingEntityModel albumModel = new SwingEntityModel(Album.TYPE, connectionProvider);
      artistModel.addDetailModel(albumModel);
      artistModel.tableModel().refresh();

      addEntityModel(artistModel);
    }
  }

  private static final class ApplicationPanel extends EntityApplicationPanel<ApplicationModel> {

    private ApplicationPanel(ApplicationModel applicationModel) {
      super(applicationModel);
    }

    @Override
    protected List<EntityPanel> createEntityPanels() {
      SwingEntityModel artistModel = applicationModel().entityModel(Artist.TYPE);
      SwingEntityModel albumModel = artistModel.detailModel(Album.TYPE);
      EntityPanel artistPanel = new EntityPanel(artistModel, new ArtistEditPanel(artistModel.editModel()));
      EntityPanel albumPanel = new EntityPanel(albumModel, new AlbumEditPanel(albumModel.editModel()));
      artistPanel.addDetailPanel(albumPanel);

      return singletonList(artistPanel);
    }
  }

  public static void main(String[] args) {
    Database.DATABASE_URL.set("jdbc:h2:mem:h2db");
    Database.DATABASE_INIT_SCRIPTS.set("src/main/sql/create_schema.sql");
    UIManager.put("Table.alternateRowColor", new Color(215, 215, 215));
    EntityPanel.TOOLBAR_CONTROLS.set(true);
    FilteredTable.AUTO_RESIZE_MODE.set(JTable.AUTO_RESIZE_ALL_COLUMNS);
    EntityApplicationPanel.builder(ApplicationModel.class, ApplicationPanel.class)
            .applicationName("Artists and Albums")
            .domainType(Chinook.DOMAIN)
            .frameSize(screenSizeRatio(0.5))
            .defaultLoginUser(User.parse("scott:tiger"))
            .start();
  }
}
