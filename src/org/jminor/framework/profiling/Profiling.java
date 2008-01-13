/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.profiling;

import org.jminor.common.db.User;
import org.jminor.common.model.Event;
import org.jminor.common.model.IntArray;
import org.jminor.common.model.UserCancelException;
import org.jminor.common.model.UserException;
import org.jminor.framework.FrameworkConstants;
import org.jminor.framework.FrameworkSettings;
import org.jminor.framework.client.model.EntityApplicationModel;
import org.jminor.framework.client.model.EntityModel;

import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public abstract class Profiling {

  public final Event evtPauseChanged = new Event("Profiling.evtPauseChanged");
  public final Event evtRelentlessChanged = new Event("Profiling.evtRelentlessChanged");
  public final Event evtMaximumThinkTimeChanged = new Event("Profiling.evtDelayChanged");
  public final Event evtMinimumThinkTimeChanged = new Event("Profiling.evtMinDelayChanged");
  public final Event evtWarningTimeChanged = new Event("Profiling.evtWarningTimeChanged");
  public final Event evtClientCountChanged = new Event("Profiling.evtClientCountChanged");
  public final Event evtBatchSizeChanged = new Event("Profiling.evtBatchSizeChanged");
  public final Event evtDoneExiting = new Event("Profiling.evtDoneExiting");

  private int maximumThinkTime =
          Integer.parseInt(System.getProperty(FrameworkConstants.PROFILING_THINKTIME_PROPERTY, "20000"));
  private int minimumThinkTime = maximumThinkTime /2;
  private int loginWaitFactor =
          Integer.parseInt(System.getProperty(FrameworkConstants.PROFILING_LOGIN_WAIT_PROPERTY, "2"));
  private int batchSize =
          Integer.parseInt(System.getProperty(FrameworkConstants.PROFILING_BATCH_SIZE_PROPERTY, "10"));

  private boolean pause = false;
  private boolean stopped = false;

  protected static final Random random = new Random();

  private final List<EntityApplicationModel> activeClients = Collections.synchronizedList(new ArrayList<EntityApplicationModel>(0));
  private User user;
  private boolean working = false;
  private boolean relentless = true;

  private final XYSeries workRequestsSeries = new XYSeries("Work requests per second");
  private final XYSeries delayedWorkRequestsSeries = new XYSeries("Delayed requests per second");
  private final XYSeriesCollection workRequestsCollection = new XYSeriesCollection();
  
  private final XYSeries minimumThinkTimeSeries = new XYSeries("Minimum think time");
  private final XYSeries maximumThinkTimeSeries = new XYSeries("Maximum think time");
  private final XYSeriesCollection thinkTimeCollection = new XYSeriesCollection();

  private final XYSeries numberOfClientsSeries = new XYSeries("Client count");
  private final XYSeriesCollection numberOfClientsCollection = new XYSeriesCollection();

  private int workRequestsPerSecond = 0;
  private int workRequestsPerSecondCounter = 0;
  private int delayedWorkRequestsPerSecond = 0;
  private int delayedWorkRequestsPerSecondCounter = 0;
  private long workRequestsPerSecondTime = System.currentTimeMillis();

  private int warningTime = 200;

  /** Constructs a new Profiling.
   * @param user the user to use for the profiling
   */
  public Profiling(final User user) {
    this.user = user;
    workRequestsCollection.addSeries(workRequestsSeries);
    workRequestsCollection.addSeries(delayedWorkRequestsSeries);
    thinkTimeCollection.addSeries(minimumThinkTimeSeries);
    thinkTimeCollection.addSeries(maximumThinkTimeSeries);
    numberOfClientsCollection.addSeries(numberOfClientsSeries);
    loadDbModel();
    new Timer(true).schedule(new TimerTask() {
      public void run() {
        updateRequestsPerSecond();
        updateChart();
        if (stopped && activeClients.size() == 0)
          evtDoneExiting.fire();
      }
    }, new Date(), 2000);
  }

  public User getUser() {
    return user;
  }

  public void setUser(User user) {
    this.user = user;
  }

  public XYSeriesCollection getWorkRequestsDataset() {
    return workRequestsCollection;
  }

  public XYSeriesCollection getThinkTimeDataset() {
    return thinkTimeCollection;
  }

  public XYSeriesCollection getNumberOfClientsDataset() {
    return numberOfClientsCollection;
  }

  public int getWarningTime() {
    return warningTime;
  }

  public void setWarningTime(int warningTime) {
    if (this.warningTime != warningTime) {
      this.warningTime = warningTime;
      evtWarningTimeChanged.fire();
    }
  }

  /**
   * @return Value for property 'clientCount'.
   */
  public int getClientCount() {
    return activeClients.size();
  }

  public int getBatchSize() {
    return batchSize;
  }

  public void setBatchSize(int batchSize) {
    this.batchSize = batchSize;
    evtBatchSizeChanged.fire();
  }

  /**
   * @param clientCount Value to set for property 'clientCount'.
   * @throws org.jminor.common.model.UserException in case of an exception
   * @throws java.rmi.RemoteException in case of a remote exception
   */
  public void setClientCount(int clientCount) throws Exception {
    if (clientCount < 0)
      clientCount = 0;

    if (activeClients.size() != clientCount)
      adjustClientCount(clientCount);
  }

  public void addClients() throws Exception {
    setClientCount(activeClients.size() + batchSize);
  }

  public void removeClients() throws Exception {
    setClientCount(activeClients.size() - batchSize);
  }

  public boolean isRelentless() {
    return relentless;
  }

  public void setRelentless(boolean relentless) {
    this.relentless = relentless;
    evtRelentlessChanged.fire();
  }

  /**
   * @return Value for property 'pause'.
   */
  public boolean isPause() {
    return this.pause;
  }

  /**
   * @param value Value to set for property 'pause'.
   */
  public void setPause(final boolean value) {
    this.pause = value;
    evtPauseChanged.fire();
  }

  public void exit() {
    pause = false;
    stopped = true;
  }

  /**
   * @return Value for property 'maximumThinkTime'.
   */
  public int getMaximumThinkTime() {
    return this.maximumThinkTime;
  }

  /**
   * @param maximumThinkTime Value to set for property 'maximumThinkTime'.
   */
  public void setMaximumThinkTime(int maximumThinkTime) {
    this.maximumThinkTime = maximumThinkTime;
    evtMaximumThinkTimeChanged.fire();
  }

  /**
   * @return Value for property 'minimumThinkTime'.
   */
  public int getMinimumThinkTime() {
    return this.minimumThinkTime;
  }

  /**
   * @param minimumThinkTime Value to set for property 'minimumThinkTime'.
   */
  public void setMinimumThinkTime(int minimumThinkTime) {
    this.minimumThinkTime = minimumThinkTime;
    evtMinimumThinkTimeChanged.fire();
  }

  protected abstract void loadDbModel();

  protected abstract void performWork(EntityApplicationModel applicationModel);

  protected abstract EntityApplicationModel initializeApplicationModel() throws UserException, UserCancelException;

  protected void selectRandomRow(final EntityModel model) {
    if (model.getTableModel().getRowCount() == 0)
      return;

    model.getTableModel().setSelectedItemIdx(random.nextInt(model.getTableModel().getRowCount()));
  }

  protected void selectRandomRows(final EntityModel model, final int count) {
    if (model.getTableModel().getRowCount() == 0)
      return;

    final IntArray indexes = new IntArray();
    for (int i = 0; i < count; i++)
      indexes.addInt(random.nextInt(model.getTableModel().getRowCount()));

    model.getTableModel().setSelectedItemIndexes(indexes.toIntArray());
  }

  protected void selectRandomRows(final EntityModel model, final double ratio) {
    if (model.getTableModel().getRowCount() == 0)
      return;

    final int toSelect = ratio > 0 ? (int) Math.floor(model.getTableModel().getRowCount()/ratio) : 1;
    final IntArray indexes = new IntArray();
    for (int i = 0; i < toSelect; i++)
      indexes.addInt(i);

    model.getTableModel().setSelectedItemIndexes(indexes.toIntArray());
  }

  private synchronized void adjustClientCount(final int clientCount) throws Exception {
    final EntityApplicationModel[] models = activeClients.toArray(new EntityApplicationModel[activeClients.size()]);
    final int activeCount = activeClients.size();
    if (activeCount > clientCount)
      for (int i = 0; i < activeCount - clientCount; i++)
        removeClient(models[i]);
    else if (activeCount < clientCount)
      for (int i = 0; i < clientCount - activeCount; i++)
        addClient();
  }

  private synchronized void addClient() throws UserException {
    final Runnable clientRunner = new Runnable() {
      public void run() {
        try {
          final EntityApplicationModel applicationModel = initApplicationModel();
          activeClients.add(applicationModel);
          evtClientCountChanged.fire();
          while (activeClients.contains(applicationModel)) {
            try {
              if (!stopped)
                Thread.sleep(getClientThinkTime(false));
              if (!pause && applicationModel.getDbConnectionProvider().getEntityDb().isConnected()) {
                if (relentless || !working) {
                  if (stopped)
                    removeClient(applicationModel);
                  else {
                    final long currentTime = System.currentTimeMillis();
                    try {
                      working = true;
                      workRequestsPerSecondCounter++;
                      performWork(applicationModel);
                    }
                    finally {
                      working = false;
                      final long workTime = System.currentTimeMillis() - currentTime;
                      if (workTime > warningTime)
                        delayedWorkRequestsPerSecondCounter++;
                    }
                  }
                }
              }
            }
            catch (Exception e) {
              e.printStackTrace();
            }
          }
        }
        catch (Exception e) {
          System.out.println("Exception " + e.getMessage());
          e.printStackTrace();
        }
      }
    };
    new Thread(clientRunner).start();
  }

  private int getClientThinkTime(final boolean isLoggingIn) {
    if (isLoggingIn)
      return random.nextInt(maximumThinkTime * loginWaitFactor);
    else {
      final int time = maximumThinkTime - minimumThinkTime;
      return time > 0 ? random.nextInt(time) + minimumThinkTime : minimumThinkTime;
    }
  }

  private synchronized void removeClient(EntityApplicationModel applicationModel) throws Exception {
    activeClients.remove(applicationModel);
    evtClientCountChanged.fire();
    applicationModel.getDbConnectionProvider().getEntityDb().logout();
    System.out.println(applicationModel + " logged out and removed");
  }

  private EntityApplicationModel initApplicationModel() throws UserException, UserCancelException {
    try {
      final int sleepyTime = getClientThinkTime(true);
      System.out.println("AppModel delaying login for " + sleepyTime + " ms");
      Thread.sleep(sleepyTime);// delay login a bit so all do not try to login at the same time
    }
    catch (InterruptedException e) {
      e.printStackTrace();
    }

    FrameworkSettings.get().useQueryRange = false;
    FrameworkSettings.get().useSmartRefresh = false;

    System.out.println("Initializing a EntityApplicationModel...");

    return initializeApplicationModel();
  }

  private void updateChart() {
    final long time = System.currentTimeMillis();
    workRequestsSeries.add(time, workRequestsPerSecond);
    delayedWorkRequestsSeries.add(time, delayedWorkRequestsPerSecond);
    minimumThinkTimeSeries.add(time, minimumThinkTime);
    maximumThinkTimeSeries.add(time, maximumThinkTime);
    numberOfClientsSeries.add(time, activeClients.size());
  }

  private void updateRequestsPerSecond() {
    final long current = System.currentTimeMillis();
    final double seconds = (current - workRequestsPerSecondTime)/1000;
    if (seconds > 5) {
      workRequestsPerSecond = (int) ((double) workRequestsPerSecondCounter/seconds);
      delayedWorkRequestsPerSecond = (int) ((double) delayedWorkRequestsPerSecondCounter/seconds);
      workRequestsPerSecondCounter = 0;
      delayedWorkRequestsPerSecondCounter = 0;
      workRequestsPerSecondTime = current;
    }
  }
}
