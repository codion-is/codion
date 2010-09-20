/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.chinook.testing;

import org.jminor.common.model.CancelException;
import org.jminor.common.model.LoadTest;
import org.jminor.common.model.User;
import org.jminor.common.ui.LoadTestPanel;
import org.jminor.framework.Configuration;
import org.jminor.framework.client.model.EntityApplicationModel;
import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.client.model.EntityTableModel;
import org.jminor.framework.client.model.reporting.EntityReportUtil;
import org.jminor.framework.db.provider.EntityConnectionProviders;
import org.jminor.framework.demos.chinook.client.ui.ChinookAppPanel;
import org.jminor.framework.demos.chinook.domain.Chinook;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.EntityUtil;
import org.jminor.framework.plugins.jasperreports.model.JasperReportsWrapper;
import org.jminor.framework.tools.testing.EntityLoadTestModel;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public final class ChinookLoadTest extends EntityLoadTestModel {

  private static final LoadTest.UsageScenario UPDATE_TOTALS = new AbstractEntityUsageScenario("updateTotals") {
    @Override
    protected void performScenario(final EntityApplicationModel application) throws ScenarioException {
      try {
        final EntityModel customerModel = application.getMainApplicationModel(Chinook.T_CUSTOMER);
        customerModel.getTableModel().refresh();
        selectRandomRows(customerModel.getTableModel(), RANDOM.nextInt(6) + 2);
        final EntityModel invoiceModel = customerModel.getDetailModel(Chinook.T_INVOICE);
        selectRandomRows(invoiceModel.getTableModel(), RANDOM.nextInt(6) + 2);
        final EntityTableModel invoiceLineTableModel = invoiceModel.getDetailModel(Chinook.T_INVOICELINE).getTableModel();
        final List<Entity> invoiceLines = invoiceLineTableModel.getAllItems();
        EntityUtil.setPropertyValue(Chinook.INVOICELINE_QUANTITY, RANDOM.nextInt(4) + 1, invoiceLines);

        invoiceLineTableModel.update(invoiceLines);

        ((ChinookAppPanel.ChinookApplicationModel) application).updateInvoiceTotals();
      }
      catch (Exception e) {
        throw new ScenarioException(e);
      }
    }

    @Override
    public int getDefaultWeight() {
      return 1;
    }
  };

  private static final LoadTest.UsageScenario VIEW_GENRE = new AbstractEntityUsageScenario("viewGenre") {
    @Override
    protected void performScenario(final EntityApplicationModel application) throws ScenarioException {
      try {
        final EntityModel genreModel = application.getMainApplicationModel(Chinook.T_GENRE);
        genreModel.getTableModel().refresh();
        selectRandomRow(genreModel.getTableModel());
        final EntityModel trackModel = genreModel.getDetailModel(Chinook.T_TRACK);
        selectRandomRows(trackModel.getTableModel(), 2);
        genreModel.getConnectionProvider().getConnection().selectDependentEntities(trackModel.getTableModel().getSelectedItems());
      }
      catch (Exception e) {
        throw new ScenarioException(e);
      }
    }

    @Override
    public int getDefaultWeight() {
      return 10;
    }
  };

  private static final LoadTest.UsageScenario VIEW_CUSTOMER_REPORT = new AbstractEntityUsageScenario("viewCustomerReport") {
    @Override
    protected void performScenario(final EntityApplicationModel application) throws ScenarioException {
      try {
        final EntityTableModel customerModel = application.getMainApplicationModel(Chinook.T_CUSTOMER).getTableModel();
        customerModel.refresh();
        selectRandomRow(customerModel);

        final String reportPath = Configuration.getReportPath() + "/customer_report.jasper";
        final Collection<Object> customerIDs =
                EntityUtil.getDistinctPropertyValues(Chinook.CUSTOMER_CUSTOMERID, customerModel.getSelectedItems());
        final HashMap<String, Object> reportParameters = new HashMap<String, Object>();
        reportParameters.put("CUSTOMER_IDS", customerIDs);
        EntityReportUtil.fillReport(new JasperReportsWrapper(reportPath, reportParameters),
                customerModel.getConnectionProvider());
      }
      catch (Exception e) {
        throw new ScenarioException(e);
      }
    }

    @Override
    public int getDefaultWeight() {
      return 2;
    }
  };

  private static final UsageScenario VIEW_INVOICE = new AbstractEntityUsageScenario("viewInvoice") {
    @Override
    protected void performScenario(final EntityApplicationModel application) throws ScenarioException {
      try {
        final EntityModel customerModel = application.getMainApplicationModel(Chinook.T_CUSTOMER);
        customerModel.getTableModel().refresh();
        selectRandomRow(customerModel.getTableModel());
        final EntityModel invoiceModel = customerModel.getDetailModel(Chinook.T_INVOICE);
        selectRandomRow(invoiceModel.getTableModel());
      }
      catch (Exception e) {
        throw new ScenarioException(e);
      }
    }

    @Override
    public int getDefaultWeight() {
      return 10;
    }
  };

  private static final UsageScenario VIEW_ALBUM = new AbstractEntityUsageScenario("viewAlbum") {
    @Override
    protected void performScenario(final EntityApplicationModel application) throws ScenarioException {
      try {
        final EntityModel artistModel = application.getMainApplicationModel(Chinook.T_ARTIST);
        artistModel.getTableModel().refresh();
        selectRandomRow(artistModel.getTableModel());
        final EntityModel albumModel = artistModel.getDetailModel(Chinook.T_ALBUM);
        selectRandomRow(albumModel.getTableModel());
      }
      catch (Exception e) {
        throw new ScenarioException(e);
      }
    }

    @Override
    public int getDefaultWeight() {
      return 10;
    }
  };

  public ChinookLoadTest() {
    super(User.UNIT_TEST_USER, VIEW_GENRE, VIEW_CUSTOMER_REPORT, VIEW_INVOICE, VIEW_ALBUM, UPDATE_TOTALS);
  }

  @Override
  protected EntityApplicationModel initializeApplication() throws CancelException {
    final EntityApplicationModel appModel = new ChinookAppPanel.ChinookApplicationModel(
            EntityConnectionProviders.createConnectionProvider(getUser(), ChinookLoadTest.class.getSimpleName()));
    /* ARTIST
    *   ALBUM
    *     TRACK
    * PLAYLIST
    *   PLAYLISTTRACK
    * CUSTOMER
    *   INVOICE
    *     INVOICELINE
    */
    final EntityModel artistModel = appModel.getMainApplicationModel(Chinook.T_ARTIST);
    final EntityModel albumModel = artistModel.getDetailModel(Chinook.T_ALBUM);
    final EntityModel trackModel = albumModel.getDetailModel(Chinook.T_TRACK);
    artistModel.setLinkedDetailModels(albumModel);
    albumModel.setLinkedDetailModels(trackModel);

    final EntityModel genreModel = appModel.getMainApplicationModel(Chinook.T_GENRE);
    final EntityModel genreTrackModel = genreModel.getDetailModel(Chinook.T_TRACK);
    genreModel.setLinkedDetailModels(genreTrackModel);

    final EntityModel playlistModel = appModel.getMainApplicationModel(Chinook.T_PLAYLIST);
    final EntityModel playlistTrackModel = playlistModel.getDetailModel(Chinook.T_PLAYLISTTRACK);
    playlistModel.setLinkedDetailModels(playlistTrackModel);

    final EntityModel customerModel = appModel.getMainApplicationModel(Chinook.T_CUSTOMER);
    final EntityModel invoiceModel = customerModel.getDetailModel(Chinook.T_INVOICE);
    final EntityModel invoicelineModel = invoiceModel.getDetailModel(Chinook.T_INVOICELINE);
    customerModel.setLinkedDetailModels(invoiceModel);
    invoiceModel.setLinkedDetailModels(invoicelineModel);

    return appModel;
  }

  public static void main(final String[] args) throws Exception {
    SwingUtilities.invokeLater(new Runner());
  }

  private static final class Runner implements Runnable {
    public void run() {
      try {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        final LoadTest loadTest;
        if (Configuration.getBooleanValue(Configuration.LOAD_TEST_REMOTE)) {
          final String serverHost = Configuration.getStringValue(Configuration.LOAD_TEST_REMOTE_HOSTNAME);
          final String loadTestClassName = ChinookLoadTest.class.getName();
          loadTest = getRemoteLoadTest(serverHost, loadTestClassName, User.UNIT_TEST_USER);
        }
        else {
          loadTest = new ChinookLoadTest();
        }

        new LoadTestPanel(loadTest).showFrame();
      }
      catch (Exception e) {
        e.printStackTrace();
      }
    }
  }
}
