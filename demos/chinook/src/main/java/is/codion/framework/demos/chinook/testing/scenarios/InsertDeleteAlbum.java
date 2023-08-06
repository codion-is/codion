package is.codion.framework.demos.chinook.testing.scenarios;

import is.codion.framework.demos.chinook.domain.Chinook.Album;
import is.codion.framework.demos.chinook.domain.Chinook.Artist;
import is.codion.framework.demos.chinook.domain.Chinook.Genre;
import is.codion.framework.demos.chinook.domain.Chinook.Track;
import is.codion.framework.demos.chinook.model.ChinookAppModel;
import is.codion.framework.domain.entity.Entity;
import is.codion.swing.framework.model.EntityComboBoxModel;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.model.SwingEntityTableModel;
import is.codion.swing.framework.model.tools.loadtest.AbstractEntityUsageScenario;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import static is.codion.framework.db.condition.Condition.attribute;
import static is.codion.swing.framework.model.tools.loadtest.EntityLoadTestModel.selectRandomItem;
import static is.codion.swing.framework.model.tools.loadtest.EntityLoadTestModel.selectRandomRow;
import static java.util.Arrays.asList;

public final class InsertDeleteAlbum extends AbstractEntityUsageScenario<ChinookAppModel> {

  private static final Random RANDOM = new Random();
  private static final Collection<String> GENRES =
          asList("Classical", "Easy Listening", "Jazz", "Latin", "Reggae", "Soundtrack");

  @Override
  protected void perform(ChinookAppModel application) throws Exception {
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
    List<Entity> genres = trackEditModel.connectionProvider().connection()
            .select(attribute(Genre.NAME).in(GENRES));
    EntityComboBoxModel mediaTypeComboBoxModel =
            trackEditModel.foreignKeyComboBoxModel(Track.MEDIATYPE_FK);
    selectRandomItem(mediaTypeComboBoxModel);
    for (int i = 0; i < 10; i++) {
      trackEditModel.put(Track.ALBUM_FK, insertedAlbum);
      trackEditModel.put(Track.NAME, "Track " + i);
      trackEditModel.put(Track.BYTES, 10000000);
      trackEditModel.put(Track.COMPOSER, "Composer");
      trackEditModel.put(Track.MILLISECONDS, 1000000);
      trackEditModel.put(Track.UNITPRICE, BigDecimal.valueOf(2));
      trackEditModel.put(Track.GENRE_FK, genres.get(RANDOM.nextInt(genres.size())));
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
