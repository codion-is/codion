/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.chinook.testing;

import org.jminor.common.db.exception.DbException;
import org.jminor.common.model.CancelException;
import org.jminor.common.model.Event;
import org.jminor.common.model.Events;
import org.jminor.common.model.ItemRandomizer;
import org.jminor.common.model.LoadTest;
import org.jminor.common.model.LoadTestModel;
import org.jminor.common.model.User;
import org.jminor.common.model.Util;
import org.jminor.common.model.reports.ReportException;
import org.jminor.common.server.ClientInfo;
import org.jminor.common.server.RemoteServer;
import org.jminor.common.server.ServerUtil;
import org.jminor.common.server.loadtest.LoadTestServer;
import org.jminor.common.server.loadtest.RemoteLoadTest;
import org.jminor.common.ui.LoadTestPanel;
import org.jminor.framework.Configuration;
import org.jminor.framework.client.model.DefaultEntityApplicationModel;
import org.jminor.framework.client.model.EntityApplicationModel;
import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.client.model.EntityTableModel;
import org.jminor.framework.client.model.reporting.EntityReportUtil;
import org.jminor.framework.db.provider.EntityDbProviderFactory;
import org.jminor.framework.demos.chinook.domain.Chinook;
import org.jminor.framework.domain.EntityUtil;
import org.jminor.framework.plugins.jasperreports.model.JasperReportsWrapper;
import org.jminor.framework.tools.testing.EntityLoadTestModel;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.event.ActionListener;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

public final class ChinookLoadTest extends EntityLoadTestModel {

  public ChinookLoadTest() {
    super(User.UNIT_TEST_USER, new LoadTestModel.AbstractUsageScenario("viewGenre") {
      @Override
      protected void performScenario(final Object application) throws ScenarioException {
        final EntityApplicationModel model = (EntityApplicationModel) application;
        final EntityModel genreModel = model.getMainApplicationModel(Chinook.T_GENRE);
        selectRandomRow(genreModel.getTableModel());
        final EntityModel trackModel = genreModel.getDetailModel(Chinook.T_TRACK);
        selectRandomRows(trackModel.getTableModel(), 2);
        try {
          genreModel.getDbProvider().getEntityDb().selectDependentEntities(trackModel.getTableModel().getSelectedItems());
        }
        catch (DbException e) {
          throw new ScenarioException(e);
        }
      }

      @Override
      public int getDefaultWeight() {
        return 3;
      }
    }, new LoadTestModel.AbstractUsageScenario("viewCustomerReport") {
      @Override
      protected void performScenario(final Object application) throws ScenarioException {
        final EntityApplicationModel model = (EntityApplicationModel) application;
        final EntityTableModel customerModel = model.getMainApplicationModel(Chinook.T_CUSTOMER).getTableModel();
        selectRandomRow(customerModel);

        final String reportPath = Configuration.getReportPath() + "/customer_report.jasper";
        final Collection<Object> customerIDs =
                EntityUtil.getDistinctPropertyValues(Chinook.CUSTOMER_CUSTOMERID, customerModel.getSelectedItems());
        final HashMap<String, Object> reportParameters = new HashMap<String, Object>();
        reportParameters.put("CUSTOMER_IDS", customerIDs);
        try {
          EntityReportUtil.fillReport(new JasperReportsWrapper(reportPath, reportParameters),
                  customerModel.getDbProvider());
        }
        catch (ReportException e) {
          throw new ScenarioException(e);
        }
      }

      @Override
      public int getDefaultWeight() {
        return 1;
      }
    }, new LoadTestModel.AbstractUsageScenario("viewInvoice") {
      @Override
      protected void performScenario(final Object application) throws ScenarioException {
        final EntityApplicationModel model = (EntityApplicationModel) application;
        final EntityModel customerModel = model.getMainApplicationModel(Chinook.T_CUSTOMER);
        selectRandomRow(customerModel.getTableModel());
        final EntityModel invoiceModel = customerModel.getDetailModel(Chinook.T_INVOICE);
        selectRandomRow(invoiceModel.getTableModel());
      }

      @Override
      public int getDefaultWeight() {
        return 2;
      }
    }, new LoadTestModel.AbstractUsageScenario("viewAlbum") {
      @Override
      protected void performScenario(final Object application) throws ScenarioException {
        final EntityApplicationModel model = (EntityApplicationModel) application;
        final EntityModel artistModel = model.getMainApplicationModel(Chinook.T_ARTIST);
        selectRandomRow(artistModel.getTableModel());
        final EntityModel albumModel = artistModel.getDetailModel(Chinook.T_ALBUM);
        selectRandomRow(albumModel.getTableModel());
      }

      @Override
      public int getDefaultWeight() {
        return 5;
      }
    });
  }

  @Override
  protected EntityApplicationModel initializeApplication() throws CancelException {
    final EntityApplicationModel appModel = new DefaultEntityApplicationModel(
            EntityDbProviderFactory.createEntityDbProvider(getUser(), ChinookLoadTest.class.getSimpleName())) {
      @Override
      protected void loadDomainModel() {
        Chinook.init();
      }
    };
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
    try {
      appModel.refresh();
    }
    catch (Exception e) {}

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
          loadTest = getRemoteLoadTest();
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

  private static LoadTest getRemoteLoadTest() throws RemoteException, NotBoundException {
    final RemoteServer server = ServerUtil.getServer("97.107.136.43", LoadTestServer.SERVER_NAME);

    final String clientType = ChinookLoadTest.class.getSimpleName();
    final ClientInfo info = new ClientInfo(UUID.randomUUID(), clientType, new User("scott", "tiger"));
    info.setProperty("jminor.loadtest.className", "org.jminor.framework.demos.chinook.testing.ChinookLoadTest");

    return initializeProxy((RemoteLoadTest) server.connect(info));
  }

  private static LoadTest initializeProxy(final RemoteLoadTest loadTest) {
    final Event evtRefresh = Events.event();
    new Timer(true).schedule(new TimerTask() {
      public void run() {
        evtRefresh.fire();
      }
    }, 0, 500);
    final ItemRandomizer randomizerProxy = Util.initializeProxy(ItemRandomizer.class, new RemoteRandomizerHandler(loadTest, evtRefresh));

    return Util.initializeProxy(LoadTest.class, new RemoteLoadTestHandler(loadTest, randomizerProxy, evtRefresh));
  }

  private static final class RemoteRandomizerHandler implements InvocationHandler {
    private final Event evtRefresh;
    private final RemoteLoadTest loadTest;

    public RemoteRandomizerHandler(final RemoteLoadTest loadTest, final Event refreshEvent) {
      this.loadTest = loadTest;
      this.evtRefresh = refreshEvent;
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      if (method.getName().endsWith("Listener")) {
        evtRefresh.addListener((ActionListener) args[0]);
        return null;
      }
      else if (method.getName().endsWith("Observer")) {
        return evtRefresh.getObserver();
      }
      try {
        final Method remoteMethod = RemoteLoadTest.class.getMethod(method.getName(), method.getParameterTypes());
        return remoteMethod.invoke(loadTest, args);
      }
      catch (Exception e) {
        throw Util.unwrapAndLog(e, InvocationTargetException.class, null);
      }
    }
  }

  private static final class RemoteLoadTestHandler implements InvocationHandler {
    private final Event evtRefresh;
    private final RemoteLoadTest loadTest;
    private final ItemRandomizer randomizer;

    public RemoteLoadTestHandler(final RemoteLoadTest loadTest, final ItemRandomizer randomizer, final Event refreshEvent) {
      this.loadTest = loadTest;
      this.randomizer = randomizer;
      this.evtRefresh = refreshEvent;
    }

    public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
      if (method.getName().equals("addExitListener")) {
        return null;
      }
      else if (method.getName().endsWith("Listener")) {
        evtRefresh.addListener((ActionListener) args[0]);
        return null;
      }
      else if (method.getName().endsWith("Observer")) {
        return evtRefresh.getObserver();
      }
      else if (method.getName().equals("getScenarioChooser")) {
        return randomizer;
      }
      else {
        try {
          final Method remoteMethod = RemoteLoadTest.class.getMethod(method.getName(), method.getParameterTypes());
          return remoteMethod.invoke(loadTest, args);
        }
        catch (Exception e) {
          throw Util.unwrapAndLog(e, InvocationTargetException.class, null);
        }
      }
    }
  }
}
