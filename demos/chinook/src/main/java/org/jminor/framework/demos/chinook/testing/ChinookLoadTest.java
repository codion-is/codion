/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.chinook.testing;

import org.jminor.common.model.CancelException;
import org.jminor.common.user.User;
import org.jminor.common.user.Users;
import org.jminor.framework.db.EntityConnectionProviders;
import org.jminor.framework.demos.chinook.domain.Chinook;
import org.jminor.framework.demos.chinook.model.ChinookApplicationModel;
import org.jminor.framework.demos.chinook.ui.ChinookAppPanel;
import org.jminor.framework.domain.entity.Entities;
import org.jminor.framework.domain.entity.Entity;
import org.jminor.framework.model.EntityComboBoxModel;
import org.jminor.framework.model.EntityEditModel;
import org.jminor.plugin.jasperreports.model.JasperReports;
import org.jminor.swing.common.tools.loadtest.ui.LoadTestPanel;
import org.jminor.swing.framework.model.SwingEntityEditModel;
import org.jminor.swing.framework.model.SwingEntityModel;
import org.jminor.swing.framework.model.SwingEntityTableModel;
import org.jminor.swing.framework.tools.loadtest.EntityLoadTestModel;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import static java.util.Arrays.asList;
import static org.jminor.framework.demos.chinook.domain.Chinook.*;

public final class ChinookLoadTest extends EntityLoadTestModel<ChinookApplicationModel> {

  private static final User UNIT_TEST_USER =
          Users.parseUser(System.getProperty("jminor.test.user", "scott:tiger"));

  private static final UsageScenario<ChinookApplicationModel> UPDATE_TOTALS =
          new AbstractEntityUsageScenario<ChinookApplicationModel>("updateTotals") {
            @Override
            protected void performScenario(final ChinookApplicationModel application) throws ScenarioException {
              try {
                final SwingEntityModel customerModel = application.getEntityModel(T_CUSTOMER);
                customerModel.getTableModel().refresh();
                selectRandomRows(customerModel.getTableModel(), RANDOM.nextInt(6) + 2);
                final SwingEntityModel invoiceModel = customerModel.getDetailModel(T_INVOICE);
                selectRandomRows(invoiceModel.getTableModel(), RANDOM.nextInt(6) + 2);
                final SwingEntityTableModel invoiceLineTableModel = invoiceModel.getDetailModel(T_INVOICELINE).getTableModel();
                final List<Entity> invoiceLines = invoiceLineTableModel.getItems();
                Entities.put(INVOICELINE_QUANTITY, RANDOM.nextInt(4) + 1, invoiceLines);

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
            protected void performScenario(final ChinookApplicationModel application) throws ScenarioException {
              try {
                final SwingEntityModel genreModel = application.getEntityModel(T_GENRE);
                genreModel.getTableModel().refresh();
                selectRandomRow(genreModel.getTableModel());
                final SwingEntityModel trackModel = genreModel.getDetailModel(T_TRACK);
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
            protected void performScenario(final ChinookApplicationModel application) throws ScenarioException {
              try {
                final SwingEntityTableModel customerModel = application.getEntityModel(T_CUSTOMER).getTableModel();
                customerModel.refresh();
                selectRandomRow(customerModel);

                final Collection<Long> customerIDs =
                        Entities.getDistinctValues(CUSTOMER_CUSTOMERID, customerModel.getSelectionModel().getSelectedItems());
                final HashMap<String, Object> reportParameters = new HashMap<>();
                reportParameters.put("CUSTOMER_IDS", customerIDs);
                customerModel.getConnectionProvider().getConnection()
                        .fillReport(JasperReports.classPathReport(Chinook.class, "customer_report.jasper", reportParameters));
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
            protected void performScenario(final ChinookApplicationModel application) throws ScenarioException {
              try {
                final SwingEntityModel customerModel = application.getEntityModel(T_CUSTOMER);
                customerModel.getTableModel().refresh();
                selectRandomRow(customerModel.getTableModel());
                final SwingEntityModel invoiceModel = customerModel.getDetailModel(T_INVOICE);
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
            protected void performScenario(final ChinookApplicationModel application) throws ScenarioException {
              try {
                final SwingEntityModel artistModel = application.getEntityModel(T_ARTIST);
                artistModel.getTableModel().refresh();
                selectRandomRow(artistModel.getTableModel());
                final SwingEntityModel albumModel = artistModel.getDetailModel(T_ALBUM);
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
            protected void performScenario(final ChinookApplicationModel application) throws ScenarioException {
              final SwingEntityModel artistModel = application.getEntityModel(T_ARTIST);
              artistModel.getTableModel().refresh();
              selectRandomRow(artistModel.getTableModel());
              final Entity artist = artistModel.getTableModel().getSelectionModel().getSelectedItem();
              final SwingEntityModel albumModel = artistModel.getDetailModel(T_ALBUM);
              final EntityEditModel albumEditModel = albumModel.getEditModel();
              final Entity album = application.getDomain().entity(T_ALBUM);
              album.put(ALBUM_ARTIST_FK, artist);
              album.put(ALBUM_TITLE, "Title");

              albumEditModel.setEntity(album);
              try {
                final Entity insertedAlbum = albumEditModel.insert();
                final SwingEntityEditModel trackEditModel = albumModel.getDetailModel(T_TRACK).getEditModel();
                final EntityComboBoxModel genreComboBoxModel = trackEditModel.getForeignKeyComboBoxModel(TRACK_GENRE_FK);
                selectRandomItem(genreComboBoxModel);
                final EntityComboBoxModel mediaTypeComboBoxModel = trackEditModel.getForeignKeyComboBoxModel(TRACK_MEDIATYPE_FK);
                selectRandomItem(mediaTypeComboBoxModel);
                for (int i = 0; i < 10; i++) {
                  trackEditModel.put(TRACK_ALBUM_FK, insertedAlbum);
                  trackEditModel.put(TRACK_NAME, "Track " + i);
                  trackEditModel.put(TRACK_BYTES, 10000000);
                  trackEditModel.put(TRACK_COMPOSER, "Composer");
                  trackEditModel.put(TRACK_MILLISECONDS, 1000000);
                  trackEditModel.put(TRACK_UNITPRICE, BigDecimal.valueOf(2));
                  trackEditModel.put(TRACK_GENRE_FK, genreComboBoxModel.getSelectedValue());
                  trackEditModel.put(TRACK_MEDIATYPE_FK, mediaTypeComboBoxModel.getSelectedValue());
                  trackEditModel.insert();
                }

                final SwingEntityTableModel trackTableModel = albumModel.getDetailModel(T_TRACK).getTableModel();
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
            protected void performScenario(final ChinookApplicationModel application) throws ScenarioException {
              try {
                application.getConnectionProvider().disconnect();
                Thread.sleep(random.nextInt(1500));
                application.getConnectionProvider().getConnection();
              }
              catch (final InterruptedException ignored) {/*ignored*/}
            }

            @Override
            public int getDefaultWeight() {
              return 0;
            }
          };

  public ChinookLoadTest() {
    super(UNIT_TEST_USER, asList(VIEW_GENRE, VIEW_CUSTOMER_REPORT, VIEW_INVOICE, VIEW_ALBUM,
            UPDATE_TOTALS, INSERT_DELETE_ALBUM, LOGOUT_LOGIN));
  }

  @Override
  protected ChinookApplicationModel initializeApplication() throws CancelException {
    final ChinookApplicationModel applicationModel = new ChinookApplicationModel(
            EntityConnectionProviders.connectionProvider().setDomainClassName("org.jminor.framework.demos.chinook.domain.impl.ChinookImpl")
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
    final SwingEntityModel artistModel = applicationModel.getEntityModel(T_ARTIST);
    final SwingEntityModel albumModel = artistModel.getDetailModel(T_ALBUM);
    final SwingEntityModel trackModel = albumModel.getDetailModel(T_TRACK);
    artistModel.addLinkedDetailModel(albumModel);
    albumModel.addLinkedDetailModel(trackModel);

    final SwingEntityModel playlistModel = applicationModel.getEntityModel(T_PLAYLIST);
    final SwingEntityModel playlistTrackModel = playlistModel.getDetailModel(T_PLAYLISTTRACK);
    playlistModel.addLinkedDetailModel(playlistTrackModel);

    final SwingEntityModel customerModel = applicationModel.getEntityModel(T_CUSTOMER);
    final SwingEntityModel invoiceModel = customerModel.getDetailModel(T_INVOICE);
    final SwingEntityModel invoicelineModel = invoiceModel.getDetailModel(T_INVOICELINE);
    customerModel.addLinkedDetailModel(invoiceModel);
    invoiceModel.addLinkedDetailModel(invoicelineModel);

    final SwingEntityModel genreModel = new SwingEntityModel(T_GENRE, applicationModel.getConnectionProvider());
    final SwingEntityModel genreTrackModel = new SwingEntityModel(T_TRACK, applicationModel.getConnectionProvider());
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
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

        new LoadTestPanel(new ChinookLoadTest()).showFrame();
      }
      catch (final Exception e) {
        throw new RuntimeException(e);
      }
    }
  }
}
