/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.chinook.testing;

import org.jminor.common.model.CancelException;
import org.jminor.common.model.User;
import org.jminor.common.model.tools.LoadTest;
import org.jminor.common.ui.tools.LoadTestPanel;
import org.jminor.framework.Configuration;
import org.jminor.framework.client.model.DefaultEntityModel;
import org.jminor.framework.client.model.EntityApplicationModel;
import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.client.model.EntityTableModel;
import org.jminor.framework.client.model.reporting.EntityReportUtil;
import org.jminor.framework.db.provider.EntityConnectionProvider;
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

import static org.jminor.framework.demos.chinook.domain.Chinook.*;

public final class ChinookLoadTest extends EntityLoadTestModel {

  private static final LoadTest.UsageScenario UPDATE_TOTALS = new AbstractEntityUsageScenario("updateTotals") {
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
        final EntityModel genreModel = application.getEntityModel(T_GENRE);
        genreModel.getTableModel().refresh();
        selectRandomRow(genreModel.getTableModel());
        final EntityModel trackModel = genreModel.getDetailModel(T_TRACK);
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
        final EntityTableModel customerModel = application.getEntityModel(T_CUSTOMER).getTableModel();
        customerModel.refresh();
        selectRandomRow(customerModel);

        final String reportPath = Configuration.getReportPath() + "/customer_report.jasper";
        final Collection<Object> customerIDs =
                EntityUtil.getDistinctPropertyValues(CUSTOMER_CUSTOMERID, customerModel.getSelectedItems());
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
        final EntityModel customerModel = application.getEntityModel(T_CUSTOMER);
        customerModel.getTableModel().refresh();
        selectRandomRow(customerModel.getTableModel());
        final EntityModel invoiceModel = customerModel.getDetailModel(T_INVOICE);
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
        final EntityModel artistModel = application.getEntityModel(T_ARTIST);
        artistModel.getTableModel().refresh();
        selectRandomRow(artistModel.getTableModel());
        final EntityModel albumModel = artistModel.getDetailModel(T_ALBUM);
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
    final EntityConnectionProvider connectionProvider = EntityConnectionProviders.createConnectionProvider(getUser(), ChinookLoadTest.class.getSimpleName());
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
      catch (Exception e) {
        e.printStackTrace();
      }
    }
  }
}
