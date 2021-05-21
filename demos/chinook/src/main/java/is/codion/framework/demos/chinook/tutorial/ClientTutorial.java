/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.tutorial;

import is.codion.common.db.database.Database;
import is.codion.common.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.DefaultDomain;
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.ForeignKey;
import is.codion.swing.framework.model.SwingEntityApplicationModel;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.ui.EntityApplicationPanel;
import is.codion.swing.framework.ui.EntityEditPanel;
import is.codion.swing.framework.ui.EntityPanel;
import is.codion.swing.framework.ui.EntityTablePanel;

import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.Color;
import java.util.List;

import static is.codion.framework.demos.chinook.tutorial.ClientTutorial.Chinook.Album;
import static is.codion.framework.demos.chinook.tutorial.ClientTutorial.Chinook.Artist;
import static is.codion.framework.domain.DomainType.domainType;
import static is.codion.framework.domain.entity.KeyGenerator.automatic;
import static is.codion.framework.domain.entity.StringFactory.stringFactory;
import static is.codion.framework.domain.property.Properties.*;
import static is.codion.swing.common.ui.Windows.getScreenSizeRatio;
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
      EntityType<Entity> TYPE = DOMAIN.entityType("chinook.artist");

      Attribute<Integer> ID = TYPE.integerAttribute("artistid");
      Attribute<String> NAME = TYPE.stringAttribute("name");
      Attribute<Integer> NUMBER_OF_ALBUMS = TYPE.integerAttribute("number_of_albums");
    }

    public interface Album {
      EntityType<Entity> TYPE = DOMAIN.entityType("chinook.album");

      Attribute<Integer> ID = TYPE.integerAttribute("albumid");
      Attribute<String> TITLE = Artist.TYPE.stringAttribute("title");
      Attribute<Integer> ARTIST_ID = Artist.TYPE.integerAttribute("artistid");

      ForeignKey ARTIST_FK = Artist.TYPE.foreignKey("artist_fk", ARTIST_ID, Artist.ID);
    }

    public Chinook() {
      super(DOMAIN);
      define(Artist.TYPE,
              primaryKeyProperty(Artist.ID),
              columnProperty(Artist.NAME, "Name")
                      .searchProperty().nullable(false).maximumLength(120),
              subqueryProperty(Artist.NUMBER_OF_ALBUMS, "Albums",
                      "select count(*) from chinook.album " +
                              "where album.artistid = artist.artistid"))
              .keyGenerator(automatic("chinook.artist"))
              .stringFactory(stringFactory(Artist.NAME))
              .caption("Artists");

      define(Album.TYPE,
              primaryKeyProperty(Album.ID),
              columnProperty(Album.ARTIST_ID)
                      .nullable(false),
              foreignKeyProperty(Album.ARTIST_FK, "Artist"),
              columnProperty(Album.TITLE, "Title")
                      .nullable(false).maximumLength(160))
              .keyGenerator(automatic("chinook.artist"))
              .stringFactory(stringFactory(Album.ARTIST_FK)
                      .text(" - ").value(Album.TITLE))
              .caption("Albums");
    }
  }

  private static final class ArtistEditPanel extends EntityEditPanel {

    private ArtistEditPanel(SwingEntityEditModel editModel) {
      super(editModel);
    }

    @Override
    protected void initializeUI() {
      setInitialFocusAttribute(Artist.NAME);
      textField(Artist.NAME)
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
      setInitialFocusAttribute(Album.ARTIST_FK);
      foreignKeySearchField(Album.ARTIST_FK)
              .columns(15);
      textField(Album.TITLE)
              .action(createInsertControl())
              .columns(15);
      setLayout(gridLayout(2, 1));
      addInputPanel(Album.ARTIST_FK);
      addInputPanel(Album.TITLE);
    }
  }

  private static final class ApplicationPanel extends EntityApplicationPanel<SwingEntityApplicationModel> {

    private ApplicationPanel() {
      super("Artists and Albums");
    }

    @Override
    protected SwingEntityApplicationModel initializeApplicationModel(final EntityConnectionProvider connectionProvider) {
      SwingEntityModel artistModel = new SwingEntityModel(Artist.TYPE, connectionProvider);
      SwingEntityModel albumModel = new SwingEntityModel(Album.TYPE, connectionProvider);
      artistModel.addDetailModel(albumModel);
      artistModel.refresh();

      SwingEntityApplicationModel applicationModel = new SwingEntityApplicationModel(connectionProvider);
      applicationModel.addEntityModel(artistModel);

      return applicationModel;
    }

    @Override
    protected List<EntityPanel> initializeEntityPanels(final SwingEntityApplicationModel applicationModel) {
      SwingEntityModel artistModel = applicationModel.getEntityModel(Artist.TYPE);
      SwingEntityModel albumModel = artistModel.getDetailModel(Album.TYPE);
      EntityPanel artistPanel = new EntityPanel(artistModel, new ArtistEditPanel(artistModel.getEditModel()));
      EntityPanel albumPanel = new EntityPanel(albumModel, new AlbumEditPanel(albumModel.getEditModel()));
      artistPanel.addDetailPanel(albumPanel);

      return singletonList(artistPanel);
    }
  }

  public static void main(final String[] args) {
    Database.DATABASE_URL.set("jdbc:h2:mem:h2db");
    Database.DATABASE_INIT_SCRIPTS.set("src/main/sql/create_schema.sql");
    EntityConnectionProvider.CLIENT_DOMAIN_CLASS.set(Chinook.class.getName());
    UIManager.put("Table.alternateRowColor", new Color(215, 215, 215));
    EntityPanel.TOOLBAR_BUTTONS.set(true);
    EntityTablePanel.TABLE_AUTO_RESIZE_MODE.set(JTable.AUTO_RESIZE_ALL_COLUMNS);
    SwingUtilities.invokeLater(() -> new ApplicationPanel().starter()
            .frameSize(getScreenSizeRatio(0.5))
            .defaultLoginUser(User.parseUser("scott:tiger"))
            .start());
  }
}
