/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.testing;

import is.codion.common.model.CancelException;
import is.codion.common.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.demos.chinook.domain.Chinook.Album;
import is.codion.framework.demos.chinook.domain.Chinook.Artist;
import is.codion.framework.demos.chinook.domain.Chinook.Customer;
import is.codion.framework.demos.chinook.domain.Chinook.Invoice;
import is.codion.framework.demos.chinook.domain.Chinook.Playlist;
import is.codion.framework.demos.chinook.domain.Chinook.PlaylistTrack;
import is.codion.framework.demos.chinook.model.ChinookApplicationModel;
import is.codion.framework.demos.chinook.testing.scenarios.InsertDeleteAlbum;
import is.codion.framework.demos.chinook.testing.scenarios.LogoutLogin;
import is.codion.framework.demos.chinook.testing.scenarios.RaisePrices;
import is.codion.framework.demos.chinook.testing.scenarios.RandomPlaylist;
import is.codion.framework.demos.chinook.testing.scenarios.UpdateTotals;
import is.codion.framework.demos.chinook.testing.scenarios.ViewAlbum;
import is.codion.framework.demos.chinook.testing.scenarios.ViewCustomerReport;
import is.codion.framework.demos.chinook.testing.scenarios.ViewGenre;
import is.codion.framework.demos.chinook.testing.scenarios.ViewInvoice;
import is.codion.framework.demos.chinook.ui.ChinookAppPanel;
import is.codion.swing.common.tools.ui.loadtest.LoadTestPanel;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.tools.loadtest.EntityLoadTestModel;

import javax.swing.SwingUtilities;

import static is.codion.framework.demos.chinook.domain.Chinook.Genre;
import static is.codion.framework.demos.chinook.domain.Chinook.Track;
import static java.util.Arrays.asList;

public final class ChinookLoadTest extends EntityLoadTestModel<ChinookApplicationModel> {

  private static final User UNIT_TEST_USER =
          User.parseUser(System.getProperty("codion.test.user", "scott:tiger"));

  public ChinookLoadTest() {
    super(UNIT_TEST_USER, asList(new ViewGenre(), new ViewCustomerReport(), new ViewInvoice(), new ViewAlbum(),
            new UpdateTotals(), new InsertDeleteAlbum(), new LogoutLogin(), new RaisePrices(), new RandomPlaylist()));
  }

  @Override
  protected ChinookApplicationModel initializeApplication() throws CancelException {
    final ChinookApplicationModel applicationModel = new ChinookApplicationModel(
            EntityConnectionProvider.connectionProvider().setDomainClassName("is.codion.framework.demos.chinook.domain.impl.ChinookImpl")
                    .setClientTypeId(ChinookAppPanel.class.getName()).setUser(getUser()));

    final SwingEntityModel customerModel = applicationModel.getEntityModel(Customer.TYPE);
    final SwingEntityModel invoiceModel = customerModel.getDetailModel(Invoice.TYPE);
    customerModel.addLinkedDetailModel(invoiceModel);

    final SwingEntityModel artistModel = applicationModel.getEntityModel(Artist.TYPE);
    final SwingEntityModel albumModel = artistModel.getDetailModel(Album.TYPE);
    final SwingEntityModel trackModel = albumModel.getDetailModel(Track.TYPE);

    artistModel.addLinkedDetailModel(albumModel);
    albumModel.addLinkedDetailModel(trackModel);

    final SwingEntityModel playlistModel = applicationModel.getEntityModel(Playlist.TYPE);
    final SwingEntityModel playlistTrackModel = playlistModel.getDetailModel(PlaylistTrack.TYPE);
    playlistModel.addLinkedDetailModel(playlistTrackModel);

    /* Add a Genre model used in the ViewGenre scenario */
    final SwingEntityModel genreModel = new SwingEntityModel(Genre.TYPE, applicationModel.getConnectionProvider());
    final SwingEntityModel genreTrackModel = new SwingEntityModel(Track.TYPE, applicationModel.getConnectionProvider());
    genreModel.addDetailModel(genreTrackModel);
    genreModel.addLinkedDetailModel(genreTrackModel);

    applicationModel.addEntityModel(genreModel);

    return applicationModel;
  }

  public static void main(final String[] args) throws Exception {
    SwingUtilities.invokeLater(new Runner());
  }

  private static final class Runner implements Runnable {
    @Override
    public void run() {
      try {
        new LoadTestPanel<>(new ChinookLoadTest()).showFrame();
      }
      catch (final Exception e) {
        throw new RuntimeException(e);
      }
    }
  }
}
