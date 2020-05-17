/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.tutorial;

import is.codion.common.db.database.Database;
import is.codion.common.user.Users;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.Domain;
import is.codion.framework.domain.entity.StringProvider;
import is.codion.swing.framework.model.SwingEntityApplicationModel;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.ui.EntityApplicationPanel;
import is.codion.swing.framework.ui.EntityApplicationPanel.MaximizeFrame;
import is.codion.swing.framework.ui.EntityEditPanel;
import is.codion.swing.framework.ui.EntityLookupField;
import is.codion.swing.framework.ui.EntityPanel;
import is.codion.swing.framework.ui.EntityTablePanel;

import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.UIManager;
import java.awt.Color;
import java.sql.Types;
import java.util.List;

import static is.codion.framework.demos.chinook.tutorial.ClientTutorial.Chinook.*;
import static is.codion.framework.domain.entity.KeyGenerators.automatic;
import static is.codion.framework.domain.property.Properties.*;
import static is.codion.swing.common.ui.KeyEvents.removeTransferFocusOnEnter;
import static is.codion.swing.common.ui.Windows.getScreenSizeRatio;
import static is.codion.swing.common.ui.layout.Layouts.gridLayout;
import static java.util.Collections.singletonList;

/**
 * When running this make sure the chinook demo module directory is the
 * working directory, due to a relative path to a db init script
 */
public final class ClientTutorial {

  public static final class Chinook extends Domain {

    public static final String T_ARTIST = "chinook.artist";
    public static final String ARTIST_ID = "artistid";
    public static final String ARTIST_NAME = "name";
    public static final String ARTIST_NR_OF_ALBUMS = "nr_of_albums";

    public static final String T_ALBUM = "chinook.album";
    public static final String ALBUM_ALBUMID = "albumid";
    public static final String ALBUM_TITLE = "title";
    public static final String ALBUM_ARTISTID = "artistid";
    public static final String ALBUM_ARTIST_FK = "artist_fk";

    public Chinook() {
      define(T_ARTIST,
              primaryKeyProperty(ARTIST_ID, Types.INTEGER),
              columnProperty(ARTIST_NAME, Types.VARCHAR, "Name")
                      .searchProperty(true).nullable(false).maximumLength(120),
              subqueryProperty(ARTIST_NR_OF_ALBUMS, Types.INTEGER, "Albums",
                      "select count(*) from chinook.album " +
                              "where album.artistid = artist.artistid"))
              .keyGenerator(automatic(T_ARTIST))
              .stringProvider(new StringProvider(ARTIST_NAME))
              .caption("Artists");

      define(T_ALBUM,
              primaryKeyProperty(ALBUM_ALBUMID, Types.INTEGER),
              foreignKeyProperty(ALBUM_ARTIST_FK, "Artist", T_ARTIST,
                      columnProperty(ALBUM_ARTISTID, Types.INTEGER))
                      .nullable(false),
              columnProperty(ALBUM_TITLE, Types.VARCHAR, "Title")
                      .nullable(false).maximumLength(160))
              .keyGenerator(automatic(T_ALBUM))
              .stringProvider(new StringProvider(ALBUM_ARTIST_FK)
                      .addText(" - ").addValue(ALBUM_TITLE))
              .caption("Albums");
    }
  }

  private static final class ArtistEditPanel extends EntityEditPanel {

    private ArtistEditPanel(SwingEntityEditModel editModel) {
      super(editModel);
    }

    @Override
    protected void initializeUI() {
      setInitialFocusProperty(ARTIST_NAME);
      JTextField nameField = createTextField(ARTIST_NAME);
      nameField.setColumns(15);
      addPropertyPanel(ARTIST_NAME);
    }
  }

  private static final class AlbumEditPanel extends EntityEditPanel {

    private AlbumEditPanel(SwingEntityEditModel editModel) {
      super(editModel);
    }

    @Override
    protected void initializeUI() {
      setInitialFocusProperty(ALBUM_ARTIST_FK);
      EntityLookupField artistLookupField = createForeignKeyLookupField(ALBUM_ARTIST_FK);
      artistLookupField.setColumns(15);
      JTextField titleField = createTextField(ALBUM_TITLE);
      removeTransferFocusOnEnter(titleField);
      titleField.setAction(getInsertControl());
      titleField.setColumns(15);
      setLayout(gridLayout(2, 1));
      addPropertyPanel(ALBUM_ARTIST_FK);
      addPropertyPanel(ALBUM_TITLE);
    }
  }

  private static final class ApplicationPanel extends EntityApplicationPanel {

    @Override
    protected SwingEntityApplicationModel initializeApplicationModel(final EntityConnectionProvider connectionProvider) {
      SwingEntityModel artistModel = new SwingEntityModel(T_ARTIST, connectionProvider);
      SwingEntityModel albumModel = new SwingEntityModel(T_ALBUM, connectionProvider);
      artistModel.addDetailModel(albumModel);
      artistModel.refresh();

      SwingEntityApplicationModel applicationModel = new SwingEntityApplicationModel(connectionProvider);
      applicationModel.addEntityModel(artistModel);

      return applicationModel;
    }

    @Override
    protected List<EntityPanel> initializeEntityPanels(final SwingEntityApplicationModel applicationModel) {
      SwingEntityModel artistModel = applicationModel.getEntityModel(T_ARTIST);
      SwingEntityModel albumModel = artistModel.getDetailModel(T_ALBUM);
      EntityPanel artistPanel = new EntityPanel(artistModel, new ArtistEditPanel(artistModel.getEditModel()));
      EntityPanel albumPanel = new EntityPanel(albumModel, new AlbumEditPanel(albumModel.getEditModel()));
      artistPanel.addDetailPanel(albumPanel);

      return singletonList(artistPanel);
    }
  }

  public static void main(final String[] args) {
    Database.DATABASE_URL.set("jdbc:h2:mem:h2db");
    Database.DATABASE_INIT_SCRIPT.set("src/main/sql/create_schema.sql");
    EntityConnectionProvider.CLIENT_DOMAIN_CLASS.set(Chinook.class.getName());
    UIManager.put("Table.alternateRowColor", new Color(215, 215, 215));
    EntityPanel.TOOLBAR_BUTTONS.set(true);
    EntityTablePanel.TABLE_AUTO_RESIZE_MODE.set(JTable.AUTO_RESIZE_ALL_COLUMNS);
    new ApplicationPanel().startApplication("Artists and Albums", null, MaximizeFrame.NO,
            getScreenSizeRatio(0.5), Users.parseUser("scott:tiger"));
  }
}
