package org.jminor.framework.demos.chinook.tutorial;

import org.jminor.common.User;
import org.jminor.common.db.Database;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.domain.Domain;
import org.jminor.swing.common.ui.UiUtil;
import org.jminor.swing.framework.model.SwingEntityApplicationModel;
import org.jminor.swing.framework.model.SwingEntityEditModel;
import org.jminor.swing.framework.model.SwingEntityModel;
import org.jminor.swing.framework.ui.EntityApplicationPanel;
import org.jminor.swing.framework.ui.EntityComboBox;
import org.jminor.swing.framework.ui.EntityEditPanel;
import org.jminor.swing.framework.ui.EntityPanel;

import javax.swing.JTextField;
import java.awt.GridLayout;
import java.sql.Types;
import java.util.List;

import static java.util.Collections.singletonList;
import static org.jminor.framework.domain.Properties.*;
import static org.jminor.swing.common.ui.UiUtil.setPreferredWidth;

public final class ArtistAlbumTutorial {

  public static final String T_ARTIST = "chinook.artist";
  public static final String ARTIST_ID = "artistid";
  public static final String ARTIST_NAME = "name";

  public static final String T_ALBUM = "chinook.album";
  public static final String ALBUM_ALBUMID = "albumid";
  public static final String ALBUM_TITLE = "title";
  public static final String ALBUM_ARTISTID = "artistid";
  public static final String ALBUM_ARTISTID_FK = "artist_fk";

  public static final class Chinook extends Domain {
    public Chinook() {
      define(T_ARTIST,
              primaryKeyProperty(ARTIST_ID),
              columnProperty(ARTIST_NAME, Types.VARCHAR, "Name")
                      .setNullable(false).setMaxLength(120))
              .setKeyGenerator(automaticKeyGenerator(T_ARTIST))
              .setStringProvider(new StringProvider(ARTIST_NAME))
              .setSmallDataset(true)
              .setCaption("Artists");

      define(T_ALBUM,
              primaryKeyProperty(ALBUM_ALBUMID),
              foreignKeyProperty(ALBUM_ARTISTID_FK, "Artist", T_ARTIST,
                      columnProperty(ALBUM_ARTISTID))
                      .setNullable(false),
              columnProperty(ALBUM_TITLE, Types.VARCHAR, "Title")
                      .setNullable(false).setMaxLength(160))
              .setKeyGenerator(automaticKeyGenerator(T_ALBUM))
              .setStringProvider(new StringProvider()
                      .addValue(ALBUM_ARTISTID_FK).addText(" - ").addValue(ALBUM_TITLE))
              .setCaption("Albums");
    }
  }

  private static final class ArtistEditPanel extends EntityEditPanel {

    private ArtistEditPanel(final SwingEntityEditModel editModel) {
      super(editModel);
    }

    @Override
    protected void initializeUI() {
      setInitialFocusProperty(ARTIST_NAME);
      final JTextField nameField = createTextField(ARTIST_NAME);
      nameField.setColumns(15);
      addPropertyPanel(ARTIST_NAME);
    }
  }

  private static final class AlbumEditPanel extends EntityEditPanel {

    private AlbumEditPanel(final SwingEntityEditModel editModel) {
      super(editModel);
    }

    @Override
    protected void initializeUI() {
      setInitialFocusProperty(ALBUM_ARTISTID_FK);
      final EntityComboBox artistComboBox = createForeignKeyComboBox(ALBUM_ARTISTID_FK);
      setPreferredWidth(artistComboBox, 160);
      final JTextField titleField = createTextField(ALBUM_TITLE);
      titleField.setColumns(15);
      setLayout(new GridLayout(2, 1, 5, 5));
      addPropertyPanel(ALBUM_ARTISTID_FK);
      addPropertyPanel(ALBUM_TITLE);
    }
  }

  private static final class ApplicationPanel extends EntityApplicationPanel {

    @Override
    protected SwingEntityApplicationModel initializeApplicationModel(final EntityConnectionProvider connectionProvider) {
      final SwingEntityModel artistModel = new SwingEntityModel(T_ARTIST, connectionProvider);
      final SwingEntityModel albumModel = new SwingEntityModel(T_ALBUM, connectionProvider);
      artistModel.addDetailModels(albumModel);
      artistModel.refresh();

      final SwingEntityApplicationModel applicationModel = new SwingEntityApplicationModel(connectionProvider);
      applicationModel.addEntityModel(artistModel);

      return applicationModel;
    }

    @Override
    protected List<EntityPanel> initializeEntityPanels(final SwingEntityApplicationModel applicationModel) {
      final SwingEntityModel artistModel = applicationModel.getEntityModel(T_ARTIST);
      final SwingEntityModel albumModel = artistModel.getDetailModel(T_ALBUM);
      final EntityPanel artistPanel = new EntityPanel(artistModel, new ArtistEditPanel(artistModel.getEditModel()));
      final EntityPanel albumPanel = new EntityPanel(albumModel, new AlbumEditPanel(albumModel.getEditModel()));
      artistPanel.addDetailPanel(albumPanel);

      return singletonList(artistPanel);
    }
  }

  public static void main(final String[] args) {
    Database.DATABASE_TYPE.set(Database.Type.H2.toString());
    Database.DATABASE_EMBEDDED_IN_MEMORY.set(true);
    Database.DATABASE_INIT_SCRIPT.set("demos/chinook/src/main/sql/create_schema.sql");
    EntityConnectionProvider.CLIENT_DOMAIN_CLASS.set(Chinook.class.getName());
    new ApplicationPanel().startApplication("Artists and Albums", null, false,
            UiUtil.getScreenSizeRatio(0.5), new User("scott", "tiger".toCharArray()));
  }
}
