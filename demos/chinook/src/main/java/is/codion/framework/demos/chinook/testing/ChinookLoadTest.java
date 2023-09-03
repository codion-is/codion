/*
 * Copyright (c) 2004 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.testing;

import is.codion.common.model.CancelException;
import is.codion.common.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.demos.chinook.domain.Chinook;
import is.codion.framework.demos.chinook.domain.Chinook.Album;
import is.codion.framework.demos.chinook.domain.Chinook.Artist;
import is.codion.framework.demos.chinook.domain.Chinook.Customer;
import is.codion.framework.demos.chinook.domain.Chinook.Invoice;
import is.codion.framework.demos.chinook.domain.Chinook.Playlist;
import is.codion.framework.demos.chinook.domain.Chinook.PlaylistTrack;
import is.codion.framework.demos.chinook.model.ChinookAppModel;
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
import is.codion.swing.common.ui.tools.loadtest.LoadTestPanel;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.model.tools.loadtest.EntityLoadTestModel;

import static is.codion.framework.demos.chinook.domain.Chinook.Genre;
import static is.codion.framework.demos.chinook.domain.Chinook.Track;
import static java.util.Arrays.asList;

public final class ChinookLoadTest extends EntityLoadTestModel<ChinookAppModel> {

  private static final User UNIT_TEST_USER =
          User.parse(System.getProperty("codion.test.user", "scott:tiger"));

  public ChinookLoadTest() {
    super(UNIT_TEST_USER, asList(new ViewGenre(), new ViewCustomerReport(), new ViewInvoice(), new ViewAlbum(),
            new UpdateTotals(), new InsertDeleteAlbum(), new LogoutLogin(), new RaisePrices(), new RandomPlaylist()));
  }

  @Override
  protected ChinookAppModel createApplication(User user) throws CancelException {
    ChinookAppModel applicationModel = new ChinookAppModel(EntityConnectionProvider.builder()
            .domainType(Chinook.DOMAIN)
            .clientTypeId(ChinookAppPanel.class.getName())
            .clientVersion(ChinookAppModel.VERSION)
            .user(user)
            .build());

    SwingEntityModel customerModel = applicationModel.entityModel(Customer.TYPE);
    SwingEntityModel invoiceModel = customerModel.detailModel(Invoice.TYPE);
    customerModel.detailModelLink(invoiceModel).active().set(true);

    SwingEntityModel artistModel = applicationModel.entityModel(Artist.TYPE);
    SwingEntityModel albumModel = artistModel.detailModel(Album.TYPE);
    SwingEntityModel trackModel = albumModel.detailModel(Track.TYPE);

    artistModel.detailModelLink(albumModel).active().set(true);
    albumModel.detailModelLink(trackModel).active().set(true);

    SwingEntityModel playlistModel = applicationModel.entityModel(Playlist.TYPE);
    SwingEntityModel playlistTrackModel = playlistModel.detailModel(PlaylistTrack.TYPE);
    playlistModel.detailModelLink(playlistTrackModel).active().set(true);

    /* Add a Genre model used in the ViewGenre scenario */
    SwingEntityModel genreModel = new SwingEntityModel(Genre.TYPE, applicationModel.connectionProvider());
    SwingEntityModel genreTrackModel = new SwingEntityModel(Track.TYPE, applicationModel.connectionProvider());
    genreModel.addDetailModel(genreTrackModel).active().set(true);

    applicationModel.addEntityModel(genreModel);

    return applicationModel;
  }

  public static void main(String[] args) {
    new LoadTestPanel<>(new ChinookLoadTest()).run();
  }
}
