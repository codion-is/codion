package is.codion.framework.demos.chinook.testing.scenarios;

import is.codion.framework.demos.chinook.domain.Chinook.Album;
import is.codion.framework.demos.chinook.domain.Chinook.Artist;
import is.codion.framework.demos.chinook.domain.Chinook.Track;
import is.codion.framework.demos.chinook.model.ChinookApplicationModel;
import is.codion.framework.domain.entity.Entity;
import is.codion.swing.framework.model.SwingEntityComboBoxModel;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.model.SwingEntityTableModel;
import is.codion.swing.framework.tools.loadtest.AbstractEntityUsageScenario;

import java.math.BigDecimal;

import static is.codion.swing.framework.tools.loadtest.EntityLoadTestModel.selectRandomItem;
import static is.codion.swing.framework.tools.loadtest.EntityLoadTestModel.selectRandomRow;

public final class InsertDeleteAlbum extends AbstractEntityUsageScenario<ChinookApplicationModel> {

  @Override
  protected void perform(ChinookApplicationModel application) throws Exception {
    SwingEntityModel artistModel = application.entityModel(Artist.TYPE);
    artistModel.tableModel().refresh();
    selectRandomRow(artistModel.tableModel());
    Entity artist = artistModel.tableModel().selectionModel().getSelectedItem();
    SwingEntityModel albumModel = artistModel.detailModel(Album.TYPE);
    SwingEntityEditModel albumEditModel = albumModel.editModel();
    albumEditModel.setEntity(application.entities().builder(Album.TYPE)
            .with(Album.ARTIST_FK, artist)
            .with(Album.TITLE, "Title")
            .build());
    Entity insertedAlbum = albumEditModel.insert();
    SwingEntityEditModel trackEditModel = albumModel.detailModel(Track.TYPE).editModel();
    SwingEntityComboBoxModel genreComboBoxModel = trackEditModel.foreignKeyComboBoxModel(Track.GENRE_FK);
    selectRandomItem(genreComboBoxModel);
    SwingEntityComboBoxModel mediaTypeComboBoxModel =
            trackEditModel.foreignKeyComboBoxModel(Track.MEDIATYPE_FK);
    selectRandomItem(mediaTypeComboBoxModel);
    for (int i = 0; i < 10; i++) {
      trackEditModel.put(Track.ALBUM_FK, insertedAlbum);
      trackEditModel.put(Track.NAME, "Track " + i);
      trackEditModel.put(Track.BYTES, 10000000);
      trackEditModel.put(Track.COMPOSER, "Composer");
      trackEditModel.put(Track.MILLISECONDS, 1000000);
      trackEditModel.put(Track.UNITPRICE, BigDecimal.valueOf(2));
      trackEditModel.put(Track.GENRE_FK, genreComboBoxModel.selectedValue());
      trackEditModel.put(Track.MEDIATYPE_FK, mediaTypeComboBoxModel.selectedValue());
      trackEditModel.insert();
    }

    SwingEntityTableModel trackTableModel = albumModel.detailModel(Track.TYPE).tableModel();
    trackTableModel.selectionModel().selectAll();
    trackTableModel.deleteSelected();
    albumEditModel.delete();
  }

  @Override
  public int defaultWeight() {
    return 3;
  }
}
