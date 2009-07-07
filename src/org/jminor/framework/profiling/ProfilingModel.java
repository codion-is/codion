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
import org.jminor.framework.client.model.EntityApplicationModel;
import org.jminor.framework.client.model.EntityTableModel;

import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public abstract class ProfilingModel {

  public final Event evtPauseChanged = new Event();
  public final Event evtRelentlessChanged = new Event();
  public final Event evtMaximumThinkTimeChanged = new Event();
  public final Event evtMinimumThinkTimeChanged = new Event();
  public final Event evtWarningTimeChanged = new Event();
  public final Event evtClientCountChanged = new Event();
  public final Event evtBatchSizeChanged = new Event();
  public final Event evtDoneExiting = new Event();

  protected static final Random random = new Random();

  private int maximumThinkTime =
          Integer.parseInt(System.getProperty(FrameworkConstants.PROFILING_THINKTIME_PROPERTY, "20000"));
  private int minimumThinkTime = maximumThinkTime/2;
  private int loginWaitFactor =
          Integer.parseInt(System.getProperty(FrameworkConstants.PROFILING_LOGIN_WAIT_PROPERTY, "2"));
  private int batchSize =
          Integer.parseInt(System.getProperty(FrameworkConstants.PROFILING_BATCH_SIZE_PROPERTY, "10"));

  private boolean pause = false;
  private boolean stopped = false;

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

  /** Constructs a new ProfilingModel.
   * @param user the user to use for the profiling
   */
  public ProfilingModel(final User user) {
    this.user = user;
    workRequestsCollection.addSeries(workRequestsSeries);
    workRequestsCollection.addSeries(delayedWorkRequestsSeries);
    thinkTimeCollection.addSeries(minimumThinkTimeSeries);
    thinkTimeCollection.addSeries(maximumThinkTimeSeries);
    numberOfClientsCollection.addSeries(numberOfClientsSeries);
    initializeSettings();
    loadDomainModel();
    new Timer(true).schedule(new TimerTask() {
      @Override
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
   * @return the number of active clients
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
   * @param clientCount the required number of active clients
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
   * @return true if the profiling is paused
   */
  public boolean isPause() {
    return this.pause;
  }

  /**
   * @param value true if profiling should be paused
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
   * @return the maximum number of milliseconds that should pass between work requests
   */
  public int getMaximumThinkTime() {
    return this.maximumThinkTime;
  }

  /**
   * @param maximumThinkTime the maximum number of milliseconds that should pass between work requests
   */
  public void setMaximumThinkTime(int maximumThinkTime) {
    this.maximumThinkTime = maximumThinkTime;
    evtMaximumThinkTimeChanged.fire();
  }

  /**
   * @return the minimum number of milliseconds that should pass between work requests
   */
  public int getMinimumThinkTime() {
    return this.minimumThinkTime;
  }

  /**
   * @param minimumThinkTime the minimum number of milliseconds that should pass between work requests
   */
  public void setMinimumThinkTime(int minimumThinkTime) {
    this.minimumThinkTime = minimumThinkTime;
    evtMinimumThinkTimeChanged.fire();
  }

  protected abstract void loadDomainModel();

  protected abstract void performWork(final EntityApplicationModel applicationModel);

  protected abstract EntityApplicationModel initializeApplicationModel() throws UserException, UserCancelException;

  protected void initializeSettings() {/**/}

  protected void selectRandomRow(final EntityTableModel model) {
    if (model.getRowCount() == 0)
      return;

    model.setSelectedItemIdx(random.nextInt(model.getRowCount()));
  }

  protected void selectRandomRows(final EntityTableModel model, final int count) {
    if (model.getRowCount() == 0)
      return;

    final IntArray indexes = new IntArray();
    for (int i = 0; i < count; i++)
      indexes.addInt(random.nextInt(model.getRowCount()));

    model.setSelectedItemIndexes(indexes.toIntArray());
  }

  protected void selectRandomRows(final EntityTableModel model, final double ratio) {
    if (model.getRowCount() == 0)
      return;

    final int toSelect = ratio > 0 ? (int) Math.floor(model.getRowCount()/ratio) : 1;
    final IntArray indexes = new IntArray();
    for (int i = 0; i < toSelect; i++)
      indexes.addInt(i);

    model.setSelectedItemIndexes(indexes.toIntArray());
  }

  private synchronized void adjustClientCount(final int clientCount) {
    final EntityApplicationModel[] models = activeClients.toArray(new EntityApplicationModel[activeClients.size()]);
    final int activeCount = activeClients.size();
    if (activeCount > clientCount)
      for (int i = 0; i < activeCount - clientCount; i++)
        removeClient(models[i]);
    else if (activeCount < clientCount)
      for (int i = 0; i < clientCount - activeCount; i++)
        addClient();
  }

  private synchronized void addClient() {
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
              if (!pause && applicationModel.getDbProvider().getEntityDb().isConnected()) {
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

  private synchronized void removeClient(EntityApplicationModel applicationModel) {
    try {
      activeClients.remove(applicationModel);
      evtClientCountChanged.fire();
      applicationModel.getDbProvider().getEntityDb().logout();
      System.out.println(applicationModel + " logged out and removed");
    }
    catch (Exception e) {
      System.out.println(applicationModel + " exception while logging out");
      e.printStackTrace();
    }
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
