/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.testing;

import is.codion.common.model.CancelException;
import is.codion.common.user.User;
import is.codion.common.user.Users;
import is.codion.framework.db.EntityConnectionProviders;
import is.codion.framework.demos.chinook.model.ChinookApplicationModel;
import is.codion.framework.demos.chinook.ui.ChinookAppPanel;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.model.EntityComboBoxModel;
import is.codion.framework.model.EntityEditModel;
import is.codion.swing.common.tools.loadtest.ScenarioException;
import is.codion.swing.common.tools.loadtest.UsageScenario;
import is.codion.swing.common.tools.ui.loadtest.LoadTestPanel;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.model.SwingEntityTableModel;
import is.codion.swing.framework.tools.loadtest.EntityLoadTestModel;

import javax.swing.SwingUtilities;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import static is.codion.framework.demos.chinook.domain.Chinook.*;
import static java.util.Arrays.asList;

public final class ChinookLoadTest extends EntityLoadTestModel<ChinookApplicationModel> {

  private static final User UNIT_TEST_USER =
          Users.parseUser(System.getProperty("codion.test.user", "scott:tiger"));

  private static final UsageScenario<ChinookApplicationModel> UPDATE_TOTALS =
          new AbstractEntityUsageScenario<ChinookApplicationModel>("updateTotals") {
            @Override
            protected void perform(final ChinookApplicationModel application) throws ScenarioException {
              try {
                final SwingEntityModel customerModel = application.getEntityModel(Customer.TYPE);
                customerModel.getTableModel().refresh();
                selectRandomRows(customerModel.getTableModel(), RANDOM.nextInt(6) + 2);
                final SwingEntityModel invoiceModel = customerModel.getDetailModel(Invoice.TYPE);
                selectRandomRows(invoiceModel.getTableModel(), RANDOM.nextInt(6) + 2);
                final SwingEntityTableModel invoiceLineTableModel = invoiceModel.getDetailModel(InvoiceLine.TYPE).getTableModel();
                final List<Entity> invoiceLines = invoiceLineTableModel.getItems();
                Entities.put(InvoiceLine.QUANTITY, RANDOM.nextInt(4) + 1, invoiceLines);

                invoiceLineTableModel.update(invoiceLines);

                application.updateInvoiceTotals();
              }
              catch (final Exception e) {
                throw new ScenarioException(e);
              }
            }
          };

  private static final UsageScenario<ChinookApplicationModel> VIEW_GENRE =
          new AbstractEntityUsageScenario<ChinookApplicationModel>("viewGenre") {
            @Override
            protected void perform(final ChinookApplicationModel application) throws ScenarioException {
              try {
                final SwingEntityModel genreModel = application.getEntityModel(Genre.TYPE);
                genreModel.getTableModel().refresh();
                selectRandomRow(genreModel.getTableModel());
                final SwingEntityModel trackModel = genreModel.getDetailModel(Track.TYPE);
                selectRandomRows(trackModel.getTableModel(), 2);
                genreModel.getConnectionProvider().getConnection().selectDependencies(trackModel.getTableModel().getSelectionModel().getSelectedItems());
              }
              catch (final Exception e) {
                throw new ScenarioException(e);
              }
            }

            @Override
            public int getDefaultWeight() {
              return 10;
            }
          };

  private static final UsageScenario<ChinookApplicationModel> VIEW_CUSTOMER_REPORT =
          new AbstractEntityUsageScenario<ChinookApplicationModel>("viewCustomerReport") {
            @Override
            protected void perform(final ChinookApplicationModel application) throws ScenarioException {
              try {
                final SwingEntityTableModel customerModel = application.getEntityModel(Customer.TYPE).getTableModel();
                customerModel.refresh();
                selectRandomRow(customerModel);

                final Collection<Long> customerIDs =
                        Entities.getDistinctValues(Customer.ID, customerModel.getSelectionModel().getSelectedItems());
                final HashMap<String, Object> reportParameters = new HashMap<>();
                reportParameters.put("CUSTOMER_IDS", customerIDs);
                customerModel.getConnectionProvider().getConnection().fillReport(Customer.CUSTOMER_REPORT, reportParameters);
              }
              catch (final Exception e) {
                throw new ScenarioException(e);
              }
            }

            @Override
            public int getDefaultWeight() {
              return 2;
            }
          };

  private static final UsageScenario<ChinookApplicationModel> VIEW_INVOICE =
          new AbstractEntityUsageScenario<ChinookApplicationModel>("viewInvoice") {
            @Override
            protected void perform(final ChinookApplicationModel application) throws ScenarioException {
              try {
                final SwingEntityModel customerModel = application.getEntityModel(Customer.TYPE);
                customerModel.getTableModel().refresh();
                selectRandomRow(customerModel.getTableModel());
                final SwingEntityModel invoiceModel = customerModel.getDetailModel(Invoice.TYPE);
                selectRandomRow(invoiceModel.getTableModel());
              }
              catch (final Exception e) {
                throw new ScenarioException(e);
              }
            }

            @Override
            public int getDefaultWeight() {
              return 10;
            }
          };

  private static final UsageScenario<ChinookApplicationModel> VIEW_ALBUM =
          new AbstractEntityUsageScenario<ChinookApplicationModel>("viewAlbum") {
            @Override
            protected void perform(final ChinookApplicationModel application) throws ScenarioException {
              try {
                final SwingEntityModel artistModel = application.getEntityModel(Artist.TYPE);
                artistModel.getTableModel().refresh();
                selectRandomRow(artistModel.getTableModel());
                final SwingEntityModel albumModel = artistModel.getDetailModel(Album.TYPE);
                selectRandomRow(albumModel.getTableModel());
              }
              catch (final Exception e) {
                throw new ScenarioException(e);
              }
            }

            @Override
            public int getDefaultWeight() {
              return 10;
            }
          };

  private static final UsageScenario<ChinookApplicationModel> INSERT_DELETE_ALBUM =
          new AbstractEntityUsageScenario<ChinookApplicationModel>("insertDeleteAlbum") {
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
                final EntityComboBoxModel mediaTypeComboBoxModel = trackEditModel.getForeignKeyComboBoxModel(Track.MEDIATYPE_FK);
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
          };

  private static final UsageScenario<ChinookApplicationModel> LOGOUT_LOGIN =
          new AbstractEntityUsageScenario<ChinookApplicationModel>("logoutLogin") {
            final Random random = new Random();
            @Override
            protected void perform(final ChinookApplicationModel application) throws ScenarioException {
              try {
                application.getConnectionProvider().disconnect();
                Thread.sleep(random.nextInt(1500));
                application.getConnectionProvider().getConnection();
              }
              catch (final InterruptedException ignored) {/*ignored*/}
            }

            @Override
            public int getDefaultWeight() {
              return 1;
            }
          };

  public ChinookLoadTest() {
    super(UNIT_TEST_USER, asList(VIEW_GENRE, VIEW_CUSTOMER_REPORT, VIEW_INVOICE, VIEW_ALBUM,
            UPDATE_TOTALS, INSERT_DELETE_ALBUM, LOGOUT_LOGIN));
  }

  @Override
  protected ChinookApplicationModel initializeApplication() throws CancelException {
    final ChinookApplicationModel applicationModel = new ChinookApplicationModel(
            EntityConnectionProviders.connectionProvider().setDomainClassName("is.codion.framework.demos.chinook.domain.impl.ChinookImpl")
                    .setClientTypeId(ChinookAppPanel.class.getName()).setUser(getUser()));
    /* ARTIST
     *   ALBUM
     *     TRACK
     * GENRE
     *   GENRETRACK
     * PLAYLIST
     *   PLAYLISTTRACK
     * CUSTOMER
     *   INVOICE
     *     INVOICELINE
     */
    final SwingEntityModel artistModel = applicationModel.getEntityModel(Artist.TYPE);
    final SwingEntityModel albumModel = artistModel.getDetailModel(Album.TYPE);
    final SwingEntityModel trackModel = albumModel.getDetailModel(Track.TYPE);
    artistModel.addLinkedDetailModel(albumModel);
    albumModel.addLinkedDetailModel(trackModel);

    final SwingEntityModel playlistModel = applicationModel.getEntityModel(Playlist.TYPE);
    final SwingEntityModel playlistTrackModel = playlistModel.getDetailModel(PlaylistTrack.TYPE);
    playlistModel.addLinkedDetailModel(playlistTrackModel);

    final SwingEntityModel customerModel = applicationModel.getEntityModel(Customer.TYPE);
    final SwingEntityModel invoiceModel = customerModel.getDetailModel(Invoice.TYPE);
    final SwingEntityModel invoicelineModel = invoiceModel.getDetailModel(InvoiceLine.TYPE);
    customerModel.addLinkedDetailModel(invoiceModel);
    invoiceModel.addLinkedDetailModel(invoicelineModel);

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
