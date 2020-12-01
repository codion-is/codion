package is.codion.framework.demos.chinook.testing.scenarios;

import is.codion.framework.demos.chinook.domain.Chinook.Album;
import is.codion.framework.demos.chinook.domain.Chinook.Artist;
import is.codion.framework.demos.chinook.domain.Chinook.Track;
import is.codion.framework.demos.chinook.model.ChinookApplicationModel;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.model.EntityComboBoxModel;
import is.codion.framework.model.EntityEditModel;
import is.codion.swing.common.tools.loadtest.ScenarioException;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.model.SwingEntityTableModel;
import is.codion.swing.framework.tools.loadtest.AbstractEntityUsageScenario;

import java.math.BigDecimal;

import static is.codion.swing.framework.tools.loadtest.EntityLoadTestModel.selectRandomItem;
import static is.codion.swing.framework.tools.loadtest.EntityLoadTestModel.selectRandomRow;

public final class InsertDeleteAlbum extends AbstractEntityUsageScenario<ChinookApplicationModel> {

  @Override
  protected void perform(final ChinookApplicationModel application) throws ScenarioException {
    final SwingEntityModel artistModel = application.getEntityModel(Artist.TYPE);
    artistModel.getTableModel().refresh();
    selectRandomRow(artistModel.getTableModel());
    final Entity artist = artistModel.getTableModel().getSelectionModel().getSelectedItem();
    final SwingEntityModel albumModel = artistModel.getDetailModel(Album.TYPE);
    final EntityEditModel albumEditModel = albumModel.getEditModel();
    final Entity album = application.getEntities().entity(Album.TYPE);
    album.put(Album.ARTIST_FK, artist);
    album.put(Album.TITLE, "Title");

    albumEditModel.setEntity(album);
    try {
      final Entity insertedAlbum = albumEditModel.insert();
      final SwingEntityEditModel trackEditModel = albumModel.getDetailModel(Track.TYPE).getEditModel();
      final EntityComboBoxModel genreComboBoxModel = trackEditModel.getForeignKeyComboBoxModel(Track.GENRE_FK);
      selectRandomItem(genreComboBoxModel);
      final EntityComboBoxModel mediaTypeComboBoxModel =
              trackEditModel.getForeignKeyComboBoxModel(Track.MEDIATYPE_FK);
      selectRandomItem(mediaTypeComboBoxModel);
      for (int i = 0; i < 10; i++) {
        trackEditModel.put(Track.ALBUM_FK, insertedAlbum);
        trackEditModel.put(Track.NAME, "Track " + i);
        trackEditModel.put(Track.BYTES, 10000000);
        trackEditModel.put(Track.COMPOSER, "Composer");
        trackEditModel.put(Track.MILLISECONDS, 1000000);
        trackEditModel.put(Track.UNITPRICE, BigDecimal.valueOf(2));
        trackEditModel.put(Track.GENRE_FK, genreComboBoxModel.getSelectedValue());
        trackEditModel.put(Track.MEDIATYPE_FK, mediaTypeComboBoxModel.getSelectedValue());
        trackEditModel.insert();
      }

      final SwingEntityTableModel trackTableModel = albumModel.getDetailModel(Track.TYPE).getTableModel();
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
