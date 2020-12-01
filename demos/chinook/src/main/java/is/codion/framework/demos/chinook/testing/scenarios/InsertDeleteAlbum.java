package is.codion.framework.demos.chinook.testing.scenarios;

import is.codion.framework.demos.chinook.domain.Chinook;
import is.codion.framework.demos.chinook.model.ChinookApplicationModel;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.model.EntityComboBoxModel;
import is.codion.framework.model.EntityEditModel;
import is.codion.swing.common.tools.loadtest.ScenarioException;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.model.SwingEntityTableModel;
import is.codion.swing.framework.tools.loadtest.EntityLoadTestModel;

import java.math.BigDecimal;

public final class InsertDeleteAlbum extends EntityLoadTestModel.AbstractEntityUsageScenario<ChinookApplicationModel> {

  @Override
  protected void perform(final ChinookApplicationModel application) throws ScenarioException {
    final SwingEntityModel artistModel = application.getEntityModel(Chinook.Artist.TYPE);
    artistModel.getTableModel().refresh();
    EntityLoadTestModel.selectRandomRow(artistModel.getTableModel());
    final Entity artist = artistModel.getTableModel().getSelectionModel().getSelectedItem();
    final SwingEntityModel albumModel = artistModel.getDetailModel(Chinook.Album.TYPE);
    final EntityEditModel albumEditModel = albumModel.getEditModel();
    final Entity album = application.getEntities().entity(Chinook.Album.TYPE);
    album.put(Chinook.Album.ARTIST_FK, artist);
    album.put(Chinook.Album.TITLE, "Title");

    albumEditModel.setEntity(album);
    try {
      final Entity insertedAlbum = albumEditModel.insert();
      final SwingEntityEditModel trackEditModel = albumModel.getDetailModel(Chinook.Track.TYPE).getEditModel();
      final EntityComboBoxModel genreComboBoxModel = trackEditModel.getForeignKeyComboBoxModel(Chinook.Track.GENRE_FK);
      EntityLoadTestModel.selectRandomItem(genreComboBoxModel);
      final EntityComboBoxModel mediaTypeComboBoxModel =
              trackEditModel.getForeignKeyComboBoxModel(Chinook.Track.MEDIATYPE_FK);
      EntityLoadTestModel.selectRandomItem(mediaTypeComboBoxModel);
      for (int i = 0; i < 10; i++) {
        trackEditModel.put(Chinook.Track.ALBUM_FK, insertedAlbum);
        trackEditModel.put(Chinook.Track.NAME, "Track " + i);
        trackEditModel.put(Chinook.Track.BYTES, 10000000);
        trackEditModel.put(Chinook.Track.COMPOSER, "Composer");
        trackEditModel.put(Chinook.Track.MILLISECONDS, 1000000);
        trackEditModel.put(Chinook.Track.UNITPRICE, BigDecimal.valueOf(2));
        trackEditModel.put(Chinook.Track.GENRE_FK, genreComboBoxModel.getSelectedValue());
        trackEditModel.put(Chinook.Track.MEDIATYPE_FK, mediaTypeComboBoxModel.getSelectedValue());
        trackEditModel.insert();
      }

      final SwingEntityTableModel trackTableModel = albumModel.getDetailModel(Chinook.Track.TYPE).getTableModel();
      trackTableModel.getSelectionModel().selectAll();
      trackTableModel.deleteSelected();
      albumEditModel.delete();
    }
    catch (final Exception e) {
      throw new ScenarioException(e);
    }
  }

  @Override
  public int getDefaultWeight() {
    return 3;
  }
}
