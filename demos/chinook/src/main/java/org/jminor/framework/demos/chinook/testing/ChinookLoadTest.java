/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.chinook.testing;

import org.jminor.common.model.CancelException;
import org.jminor.common.model.User;
import org.jminor.common.model.tools.ScenarioException;
import org.jminor.framework.Configuration;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.db.EntityConnectionProviders;
import org.jminor.framework.demos.chinook.client.ui.ChinookAppPanel;
import org.jminor.framework.demos.chinook.domain.Chinook;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.EntityUtil;
import org.jminor.framework.plugins.jasperreports.model.JasperReportsWrapper;
import org.jminor.swing.common.ui.tools.LoadTestPanel;
import org.jminor.swing.framework.model.DefaultEntityModel;
import org.jminor.swing.framework.model.EntityApplicationModel;
import org.jminor.swing.framework.model.EntityComboBoxModel;
import org.jminor.swing.framework.model.EntityEditModel;
import org.jminor.swing.framework.model.EntityModel;
import org.jminor.swing.framework.model.EntityTableModel;
import org.jminor.swing.framework.model.reporting.EntityReportUtil;
import org.jminor.swing.framework.testing.EntityLoadTestModel;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import static org.jminor.framework.demos.chinook.domain.Chinook.*;

public final class ChinookLoadTest extends EntityLoadTestModel {

  private static final UsageScenario<EntityApplicationModel> UPDATE_TOTALS = new AbstractEntityUsageScenario("updateTotals") {
    @Override
    protected void performScenario(final EntityApplicationModel application) throws ScenarioException {
      try {
        final EntityModel customerModel = application.getEntityModel(T_CUSTOMER);
        customerModel.getTableModel().refresh();
        selectRandomRows(customerModel.getTableModel(), RANDOM.nextInt(6) + 2);
        final EntityModel invoiceModel = customerModel.getDetailModel(T_INVOICE);
        selectRandomRows(invoiceModel.getTableModel(), RANDOM.nextInt(6) + 2);
        final EntityTableModel invoiceLineTableModel = invoiceModel.getDetailModel(T_INVOICELINE).getTableModel();
        final List<Entity> invoiceLines = invoiceLineTableModel.getAllItems();
        EntityUtil.setPropertyValue(Chinook.INVOICELINE_QUANTITY, RANDOM.nextInt(4) + 1, invoiceLines);

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

  private static final UsageScenario<EntityApplicationModel> VIEW_GENRE = new AbstractEntityUsageScenario("viewGenre") {
    @Override
    protected void performScenario(final EntityApplicationModel application) throws ScenarioException {
      try {
        final EntityModel genreModel = application.getEntityModel(T_GENRE);
        genreModel.getTableModel().refresh();
        selectRandomRow(genreModel.getTableModel());
        final EntityModel trackModel = genreModel.getDetailModel(T_TRACK);
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

  private static final UsageScenario<EntityApplicationModel> VIEW_CUSTOMER_REPORT = new AbstractEntityUsageScenario("viewCustomerReport") {
    @Override
    protected void performScenario(final EntityApplicationModel application) throws ScenarioException {
      try {
        final EntityTableModel customerModel = application.getEntityModel(T_CUSTOMER).getTableModel();
        customerModel.refresh();
        selectRandomRow(customerModel);

        final String reportPath = Configuration.getReportPath() + "/customer_report.jasper";
        final Collection customerIDs =
                EntityUtil.getDistinctPropertyValues(CUSTOMER_CUSTOMERID, customerModel.getSelectionModel().getSelectedItems());
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

  private static final UsageScenario<EntityApplicationModel> VIEW_INVOICE = new AbstractEntityUsageScenario("viewInvoice") {
    @Override
    protected void performScenario(final EntityApplicationModel application) throws ScenarioException {
      try {
        final EntityModel customerModel = application.getEntityModel(T_CUSTOMER);
        customerModel.getTableModel().refresh();
        selectRandomRow(customerModel.getTableModel());
        final EntityModel invoiceModel = customerModel.getDetailModel(T_INVOICE);
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

  private static final UsageScenario<EntityApplicationModel> VIEW_ALBUM = new AbstractEntityUsageScenario("viewAlbum") {
    @Override
    protected void performScenario(final EntityApplicationModel application) throws ScenarioException {
      try {
        final EntityModel artistModel = application.getEntityModel(T_ARTIST);
        artistModel.getTableModel().refresh();
        selectRandomRow(artistModel.getTableModel());
        final EntityModel albumModel = artistModel.getDetailModel(T_ALBUM);
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

  private static final UsageScenario<EntityApplicationModel> INSERT_DELETE_ALBUM = new AbstractEntityUsageScenario("insertDeleteAlbum") {
    @Override
    protected void performScenario(final EntityApplicationModel application) throws ScenarioException {
      final EntityModel artistModel = application.getEntityModel(T_ARTIST);
      artistModel.getTableModel().refresh();
      selectRandomRow(artistModel.getTableModel());
      final Entity artist = artistModel.getTableModel().getSelectionModel().getSelectedItem();
      final EntityModel albumModel = artistModel.getDetailModel(T_ALBUM);
      final EntityEditModel albumEditModel = albumModel.getEditModel();
      final Entity album = Entities.entity(T_ALBUM);
      album.setValue(ALBUM_ARTISTID_FK, artist);
      album.setValue(ALBUM_TITLE, "Title");

      albumEditModel.setEntity(album);
      try {
        final Entity insertedAlbum = albumEditModel.insert().get(0);
        final EntityEditModel trackEditModel = albumModel.getDetailModel(T_TRACK).getEditModel();
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

        final EntityTableModel trackTableModel = albumModel.getDetailModel(T_TRACK).getTableModel();
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
    super(User.UNIT_TEST_USER, Arrays.asList(VIEW_GENRE, VIEW_CUSTOMER_REPORT, VIEW_INVOICE, VIEW_ALBUM,
            UPDATE_TOTALS, INSERT_DELETE_ALBUM));
  }

  @Override
  protected EntityApplicationModel initializeApplication() throws CancelException {
    final EntityConnectionProvider connectionProvider = EntityConnectionProviders.connectionProvider(getUser(), ChinookLoadTest.class.getSimpleName());
    final EntityApplicationModel appModel = new ChinookAppPanel.ChinookApplicationModel(connectionProvider);
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
    final EntityModel artistModel = appModel.getEntityModel(T_ARTIST);
    final EntityModel albumModel = artistModel.getDetailModel(T_ALBUM);
    final EntityModel trackModel = albumModel.getDetailModel(T_TRACK);
    artistModel.addLinkedDetailModel(albumModel);
    albumModel.addLinkedDetailModel(trackModel);

    final EntityModel playlistModel = appModel.getEntityModel(T_PLAYLIST);
    final EntityModel playlistTrackModel = playlistModel.getDetailModel(T_PLAYLISTTRACK);
    playlistModel.addLinkedDetailModel(playlistTrackModel);

    final EntityModel customerModel = appModel.getEntityModel(T_CUSTOMER);
    final EntityModel invoiceModel = customerModel.getDetailModel(T_INVOICE);
    final EntityModel invoicelineModel = invoiceModel.getDetailModel(T_INVOICELINE);
    customerModel.addLinkedDetailModel(invoiceModel);
    invoiceModel.addLinkedDetailModel(invoicelineModel);

    final EntityModel genreModel = new DefaultEntityModel(T_GENRE, connectionProvider);
    final EntityModel genreTrackModel = new DefaultEntityModel(T_TRACK, connectionProvider);
    genreModel.addDetailModel(genreTrackModel);
    genreModel.addLinkedDetailModel(genreTrackModel);

    appModel.addEntityModel(genreModel);

    return appModel;
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
