/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.tools.testing;

import org.jminor.common.model.CancelException;
import org.jminor.common.model.LoadTestModel;
import org.jminor.common.model.User;
import org.jminor.common.model.LoadTest;
import org.jminor.common.model.Event;
import org.jminor.common.model.Events;
import org.jminor.common.model.ItemRandomizer;
import org.jminor.common.model.Util;
import org.jminor.common.server.ServerException;
import org.jminor.common.server.RemoteServer;
import org.jminor.common.server.ServerUtil;
import org.jminor.common.server.ClientInfo;
import org.jminor.common.server.loadtest.LoadTestServer;
import org.jminor.common.server.loadtest.RemoteLoadTest;
import org.jminor.framework.Configuration;
import org.jminor.framework.client.model.EntityApplicationModel;
import org.jminor.framework.client.model.EntityTableModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.Timer;
import java.util.TimerTask;
import java.rmi.RemoteException;
import java.rmi.NotBoundException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.awt.event.ActionListener;

/**
 * A class for running multiple EntityApplicationModel instances for load testing purposes.
 */
public abstract class EntityLoadTestModel extends LoadTestModel<EntityApplicationModel> {

  private static final int DEFAULT_WARNING_TIME = 200;

  /**
   * Instantiates a new EntityLoadTestModel.
   * @param user the default user
   * @param usageScenarios the usage scenarios
   */
  public EntityLoadTestModel(final User user, final UsageScenario<EntityApplicationModel>... usageScenarios) {
    super(user, Arrays.asList(usageScenarios), Configuration.getIntValue(Configuration.LOAD_TEST_THINKTIME),
            Configuration.getIntValue(Configuration.LOAD_TEST_LOGIN_DELAY),
            Configuration.getIntValue(Configuration.LOAD_TEST_BATCH_SIZE), DEFAULT_WARNING_TIME);
  }

  /**
   * Selects a random row in the given table model
   * @param tableModel the table model
   */
  public static void selectRandomRow(final EntityTableModel tableModel) {
    if (tableModel.getRowCount() == 0) {
      return;
    }

    tableModel.setSelectedItemIndex(RANDOM.nextInt(tableModel.getRowCount()));
  }

  /**
   * Selects random rows in the given table model
   * @param tableModel the table model
   * @param count the number of rows to select
   */
  public static void selectRandomRows(final EntityTableModel tableModel, final int count) {
    if (tableModel.getRowCount() < count) {
      tableModel.selectAll();
    }
    else {
      final int startIdx = RANDOM.nextInt(tableModel.getRowCount() - count);
      final List<Integer> indexes = new ArrayList<Integer>();
      for (int i = startIdx; i < count + startIdx; i++) {
        indexes.add(i);
      }

      tableModel.setSelectedItemIndexes(indexes);
    }
  }

  /**
   * Selects random rows in the given table model
   * @param tableModel the table model
   * @param ratio the ratio of available rows to select
   */
  public static void selectRandomRows(final EntityTableModel tableModel, final double ratio) {
    if (tableModel.getRowCount() == 0) {
      return;
    }

    final int toSelect = ratio > 0 ? (int) Math.floor(tableModel.getRowCount() * ratio) : 1;
    final List<Integer> indexes = new ArrayList<Integer>();
    for (int i = 0; i < toSelect; i++) {
      indexes.add(i);
    }

    tableModel.setSelectedItemIndexes(indexes);
  }

  /** {@inheritDoc} */
  @Override
  protected final void disconnectApplication(final EntityApplicationModel application) {
    application.getDbProvider().disconnect();
  }

  /** {@inheritDoc} */
  @Override
  protected abstract EntityApplicationModel initializeApplication() throws CancelException;

  public abstract static class AbstractEntityUsageScenario extends AbstractUsageScenario<EntityApplicationModel> {

    public AbstractEntityUsageScenario() {
      super();
    }

    public AbstractEntityUsageScenario(final String name) {
      super(name);
    }
  }

  public static LoadTest getRemoteLoadTest(final String serverHost, final String loadTestClassName, final User user) throws RemoteException, NotBoundException,
          ServerException.ServerFullException, ServerException.LoginException {
    final RemoteServer server = ServerUtil.getServer(serverHost, LoadTestServer.SERVER_NAME);

    final ClientInfo info = new ClientInfo(UUID.randomUUID(), loadTestClassName, user);

    return initializeProxy((RemoteLoadTest) server.connect(info));
  }

  private static LoadTest initializeProxy(final RemoteLoadTest loadTest) {
    final Event evtRefresh = Events.event();
    new Timer(true).schedule(new TimerTask() {
      @Override
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

    private RemoteRandomizerHandler(final RemoteLoadTest loadTest, final Event refreshEvent) {
      this.loadTest = loadTest;
      this.evtRefresh = refreshEvent;
    }

    public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
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

    private RemoteLoadTestHandler(final RemoteLoadTest loadTest, final ItemRandomizer randomizer, final Event refreshEvent) {
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
