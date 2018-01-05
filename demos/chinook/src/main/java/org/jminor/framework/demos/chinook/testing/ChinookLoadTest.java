/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.chinook.testing;

import org.jminor.common.User;
import org.jminor.common.model.CancelException;
import org.jminor.framework.db.EntityConnectionProviders;
import org.jminor.framework.demos.chinook.client.ui.ChinookAppPanel;
import org.jminor.framework.demos.chinook.domain.Chinook;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.model.EntityApplicationModel;
import org.jminor.framework.model.EntityComboBoxModel;
import org.jminor.framework.model.EntityEditModel;
import org.jminor.framework.plugins.jasperreports.model.JasperReportsWrapper;
import org.jminor.swing.common.tools.ui.LoadTestPanel;
import org.jminor.swing.framework.model.SwingEntityEditModel;
import org.jminor.swing.framework.model.SwingEntityModel;
import org.jminor.swing.framework.model.SwingEntityTableModel;
import org.jminor.swing.framework.model.reporting.EntityReportUtil;
import org.jminor.swing.framework.tools.EntityLoadTestModel;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import static org.jminor.framework.demos.chinook.domain.Chinook.*;

public final class ChinookLoadTest extends EntityLoadTestModel<ChinookAppPanel.ChinookApplicationModel> {

  private static final User UNIT_TEST_USER = new User(
          System.getProperty("jminor.unittest.username", "scott"),
          System.getProperty("jminor.unittest.password", "tiger"));

  private static final UsageScenario<ChinookAppPanel.ChinookApplicationModel> UPDATE_TOTALS =
          new AbstractEntityUsageScenario<ChinookAppPanel.ChinookApplicationModel>("updateTotals") {
    @Override
    protected void performScenario(final ChinookAppPanel.ChinookApplicationModel application) throws ScenarioException {
      try {
        final SwingEntityModel customerModel = application.getEntityModel(T_CUSTOMER);
        customerModel.getTableModel().refresh();
        selectRandomRows(customerModel.getTableModel(), RANDOM.nextInt(6) + 2);
        final SwingEntityModel invoiceModel = customerModel.getDetailModel(T_INVOICE);
        selectRandomRows(invoiceModel.getTableModel(), RANDOM.nextInt(6) + 2);
        final SwingEntityTableModel invoiceLineTableModel = invoiceModel.getDetailModel(T_INVOICELINE).getTableModel();
        final List<Entity> invoiceLines = invoiceLineTableModel.getAllItems();
        Entities.put(Chinook.INVOICELINE_QUANTITY, RANDOM.nextInt(4) + 1, invoiceLines);

        invoiceLineTableModel.update(invoiceLines);

        ((ChinookAppPanel.ChinookApplicationModel) application).updateInvoiceTotals();
      }
      catch (final Exception e) {
        throw new ScenarioException(e);
      }
    }

    @Override
    public int getDefaultWeight() {
      return 1;
    }
  };

  private static final UsageScenario<ChinookAppPanel.ChinookApplicationModel> VIEW_GENRE =
          new AbstractEntityUsageScenario<ChinookAppPanel.ChinookApplicationModel>("viewGenre") {
    @Override
    protected void performScenario(final ChinookAppPanel.ChinookApplicationModel application) throws ScenarioException {
      try {
        final SwingEntityModel genreModel = application.getEntityModel(T_GENRE);
        genreModel.getTableModel().refresh();
        selectRandomRow(genreModel.getTableModel());
        final SwingEntityModel trackModel = genreModel.getDetailModel(T_TRACK);
        selectRandomRows(trackModel.getTableModel(), 2);
        genreModel.getConnectionProvider().getConnection().selectDependentEntities(trackModel.getTableModel().getSelectionModel().getSelectedItems());
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

  private static final UsageScenario<ChinookAppPanel.ChinookApplicationModel> VIEW_CUSTOMER_REPORT =
          new AbstractEntityUsageScenario<ChinookAppPanel.ChinookApplicationModel>("viewCustomerReport") {
    @Override
    protected void performScenario(final ChinookAppPanel.ChinookApplicationModel application) throws ScenarioException {
      try {
        final SwingEntityTableModel customerModel = application.getEntityModel(T_CUSTOMER).getTableModel();
        customerModel.refresh();
        selectRandomRow(customerModel);

        final String reportPath = EntityApplicationModel.getReportPath() + "/customer_report.jasper";
        final Collection customerIDs =
                Entities.getDistinctValues(CUSTOMER_CUSTOMERID, customerModel.getSelectionModel().getSelectedItems());
        final HashMap<String, Object> reportParameters = new HashMap<>();
        reportParameters.put("CUSTOMER_IDS", customerIDs);
        EntityReportUtil.fillReport(new JasperReportsWrapper(reportPath, reportParameters),
                customerModel.getConnectionProvider());
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

  private static final UsageScenario<ChinookAppPanel.ChinookApplicationModel> VIEW_INVOICE =
          new AbstractEntityUsageScenario<ChinookAppPanel.ChinookApplicationModel>("viewInvoice") {
    @Override
    protected void performScenario(final ChinookAppPanel.ChinookApplicationModel application) throws ScenarioException {
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

  private static final UsageScenario<ChinookAppPanel.ChinookApplicationModel> VIEW_ALBUM =
          new AbstractEntityUsageScenario<ChinookAppPanel.ChinookApplicationModel>("viewAlbum") {
    @Override
    protected void performScenario(final ChinookAppPanel.ChinookApplicationModel application) throws ScenarioException {
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

  private static final UsageScenario<ChinookAppPanel.ChinookApplicationModel> INSERT_DELETE_ALBUM =
          new AbstractEntityUsageScenario<ChinookAppPanel.ChinookApplicationModel>("insertDeleteAlbum") {
    @Override
    protected void performScenario(final ChinookAppPanel.ChinookApplicationModel application) throws ScenarioException {
      final SwingEntityModel artistModel = application.getEntityModel(T_ARTIST);
      artistModel.getTableModel().refresh();
      selectRandomRow(artistModel.getTableModel());
      final Entity artist = artistModel.getTableModel().getSelectionModel().getSelectedItem();
      final SwingEntityModel albumModel = artistModel.getDetailModel(T_ALBUM);
      final EntityEditModel albumEditModel = albumModel.getEditModel();
      final Entity album = application.getEntities().entity(T_ALBUM);
      album.put(ALBUM_ARTISTID_FK, artist);
      album.put(ALBUM_TITLE, "Title");

      albumEditModel.setEntity(album);
      try {
        final Entity insertedAlbum = albumEditModel.insert().get(0);
        final SwingEntityEditModel trackEditModel = (SwingEntityEditModel) albumModel.getDetailModel(T_TRACK).getEditModel();
        final EntityComboBoxModel genreComboBoxModel = trackEditModel.getForeignKeyComboBoxModel(TRACK_GENREID_FK);
        selectRandomItem(genreComboBoxModel);
        final EntityComboBoxModel mediaTypeComboBoxModel = trackEditModel.getForeignKeyComboBoxModel(TRACK_MEDIATYPEID_FK);
        selectRandomItem(mediaTypeComboBoxModel);
        for (int i = 0; i < 10; i++) {
          trackEditModel.setValue(TRACK_ALBUMID_FK, insertedAlbum);
          trackEditModel.setValue(TRACK_NAME, "Track " + i);
          trackEditModel.setValue(TRACK_BYTES, 10000000);
          trackEditModel.setValue(TRACK_COMPOSER, "Composer");
          trackEditModel.setValue(TRACK_MILLISECONDS, 1000000);
          trackEditModel.setValue(TRACK_UNITPRICE, 2d);
          trackEditModel.setValue(TRACK_GENREID_FK, genreComboBoxModel.getSelectedValue());
          trackEditModel.setValue(TRACK_MEDIATYPEID_FK, mediaTypeComboBoxModel.getSelectedValue());
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

  public ChinookLoadTest() {
    super(UNIT_TEST_USER, Arrays.asList(VIEW_GENRE, VIEW_CUSTOMER_REPORT, VIEW_INVOICE, VIEW_ALBUM,
            UPDATE_TOTALS, INSERT_DELETE_ALBUM));
  }

  @Override
  protected ChinookAppPanel.ChinookApplicationModel initializeApplication() throws CancelException {
    final ChinookAppPanel.ChinookApplicationModel applicationModel = new ChinookAppPanel.ChinookApplicationModel(
            EntityConnectionProviders.connectionProvider("org.jminor.framework.demos.chinook.domain.ChinookDomain",
                    getUser(), ChinookLoadTest.class.getSimpleName()));
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
