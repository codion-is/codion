/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.server.loadtest;

import org.jminor.common.model.ItemRandomizer;
import org.jminor.common.model.LoadTest;
import org.jminor.common.model.User;
import org.jminor.common.server.ClientInfo;

import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.YIntervalSeriesCollection;

import java.rmi.RemoteException;
import java.rmi.server.RMISocketFactory;
import java.rmi.server.UnicastRemoteObject;
import java.util.Collection;

/**
 * A remote load test adapter.
 * @see org.jminor.common.server.loadtest.LoadTestServer
 */
public final class RemoteLoadTestImpl extends UnicastRemoteObject implements RemoteLoadTest {

  private static final long serialVersionUID = 1;

  private final LoadTest loadTest;
  private final ClientInfo clientInfo;
  private static final String LOAD_TEST_CLASSNAME = "jminor.loadtest.className";

  /**
   * Instantiates a new RemoteLoadTestAdapter.
   * @param clientInfo the client info
   * @param loadTestPort the port on which to register the service
   * @throws RemoteException in case of an exception
   */
  public RemoteLoadTestImpl(final ClientInfo clientInfo, final int loadTestPort) throws RemoteException {
    super(loadTestPort, RMISocketFactory.getSocketFactory(), RMISocketFactory.getSocketFactory());
    this.clientInfo = clientInfo;
    this.loadTest = instantiateLoadTest(clientInfo);
  }

  /**
   * @return the client info associated with this remote test adapter
   */
  ClientInfo getClientInfo() {
    return clientInfo;
  }

  public void exit() throws RemoteException {
    loadTest.exit();
  }

  /** {@inheritDoc} */
  public User getUser() {
    return loadTest.getUser();
  }

  /** {@inheritDoc} */
  public void setUser(final User user) {
    loadTest.setUser(user);
  }

  /** {@inheritDoc} */
  public LoadTest.UsageScenario getUsageScenario(final String scenarioName) {
    return loadTest.getUsageScenario(scenarioName);
  }

  /** {@inheritDoc} */
  public Collection<String> getUsageScenarios() {
    return loadTest.getUsageScenarios();
  }

  /** {@inheritDoc} */
  public int getWarningTime() {
    return loadTest.getWarningTime();
  }

  /** {@inheritDoc} */
  public void setWarningTime(final int warningTime) {
    loadTest.setWarningTime(warningTime);
  }

  public int getUpdateInterval() {
    return loadTest.getUpdateInterval();
  }

  /** {@inheritDoc} */
  public void setUpdateInterval(final int updateInterval) {
    loadTest.setUpdateInterval(updateInterval);
  }

  /** {@inheritDoc} */
  public int getApplicationCount() {
    return loadTest.getApplicationCount();
  }

  /** {@inheritDoc} */
  public int getApplicationBatchSize() {
    return loadTest.getApplicationBatchSize();
  }

  /** {@inheritDoc} */
  public void setApplicationBatchSize(final int applicationBatchSize) {
    loadTest.setApplicationBatchSize(applicationBatchSize);
  }

  /** {@inheritDoc} */
  public boolean isPaused() {
    return loadTest.isPaused();
  }

  /** {@inheritDoc} */
  public void setPaused(final boolean value) {
    loadTest.setPaused(value);
  }

  /** {@inheritDoc} */
  public int getMaximumThinkTime() {
    return loadTest.getMaximumThinkTime();
  }

  /** {@inheritDoc} */
  public void setMaximumThinkTime(final int maximumThinkTime) {
    loadTest.setMaximumThinkTime(maximumThinkTime);
  }

  public int getMinimumThinkTime() {
    return loadTest.getMinimumThinkTime();
  }

  /** {@inheritDoc} */
  public void setMinimumThinkTime(final int minimumThinkTime) {
    loadTest.setMinimumThinkTime(minimumThinkTime);
  }

  /** {@inheritDoc} */
  public int getLoginDelayFactor() {
    return loadTest.getLoginDelayFactor();
  }

  /** {@inheritDoc} */
  public void setLoginDelayFactor(final int loginDelayFactor) {
    loadTest.setLoginDelayFactor(loginDelayFactor);
  }

  /** {@inheritDoc} */
  public boolean isCollectChartData() {
    return loadTest.isCollectChartData();
  }

  /** {@inheritDoc} */
  public void setCollectChartData(final boolean value) {
    loadTest.setCollectChartData(value);
  }

  /** {@inheritDoc} */
  public void addApplicationBatch() {
    loadTest.addApplicationBatch();
  }

  /** {@inheritDoc} */
  public void removeApplicationBatch() {
    loadTest.removeApplicationBatch();
  }

  /** {@inheritDoc} */
  public void resetChartData() {
    loadTest.resetChartData();
  }

  public YIntervalSeriesCollection getScenarioDurationDataset(final String name) {
    return loadTest.getScenarioDurationDataset(name);
  }

  /** {@inheritDoc} */
  public XYDataset getThinkTimeDataset() {
    return loadTest.getThinkTimeDataset();
  }

  /** {@inheritDoc} */
  public XYDataset getNumberOfApplicationsDataset() {
    return loadTest.getNumberOfApplicationsDataset();
  }

  /** {@inheritDoc} */
  public XYDataset getUsageScenarioDataset() {
    return loadTest.getUsageScenarioDataset();
  }

  /** {@inheritDoc} */
  public XYDataset getMemoryUsageDataset() {
    return loadTest.getMemoryUsageDataset();
  }

  /** {@inheritDoc} */
  public XYDataset getUsageScenarioFailureDataset() {
    return loadTest.getUsageScenarioFailureDataset();
  }

  /** {@inheritDoc} */
  public int getItemCount() {
    return loadTest.getScenarioChooser().getItemCount();
  }

  /** {@inheritDoc} */
  public Collection<ItemRandomizer.RandomItem<LoadTest.UsageScenario>> getItems() {
    return loadTest.getScenarioChooser().getItems();
  }

  /** {@inheritDoc} */
  public int getWeight(final Object item) {
    return loadTest.getScenarioChooser().getWeight((LoadTest.UsageScenario) item);
  }

  public void setWeight(final Object item, final int weight) {
    loadTest.getScenarioChooser().setWeight((LoadTest.UsageScenario) item, weight);
  }

  /** {@inheritDoc} */
  public void addItem(final Object item) {
    loadTest.getScenarioChooser().addItem((LoadTest.UsageScenario) item);
  }

  /** {@inheritDoc} */
  public void addItem(final Object item, final int weight) {
    loadTest.getScenarioChooser().addItem((LoadTest.UsageScenario) item, weight);
  }

  /** {@inheritDoc} */
  public LoadTest.UsageScenario getRandomItem() {
    return loadTest.getScenarioChooser().getRandomItem();
  }

  /** {@inheritDoc} */
  public double getWeightRatio(final Object item) {
    return loadTest.getScenarioChooser().getWeightRatio((LoadTest.UsageScenario) item);
  }

  /** {@inheritDoc} */
  public void incrementWeight(final Object item) {
    loadTest.getScenarioChooser().incrementWeight((LoadTest.UsageScenario) (item));
  }

  /** {@inheritDoc} */
  public void decrementWeight(final Object item) {
    loadTest.getScenarioChooser().decrementWeight((LoadTest.UsageScenario) item);
  }

  private static LoadTest instantiateLoadTest(final ClientInfo clientInfo) throws RemoteException {
    final String loadTestClass = clientInfo.getClientTypeID();
    if (loadTestClass == null) {
      throw new IllegalArgumentException(LOAD_TEST_CLASSNAME + " is missing");
    }

    try {
      return (LoadTest) Class.forName(loadTestClass).getConstructor().newInstance();
    }
    catch (Exception e) {
      throw new RemoteException(e.getMessage(), e);
    }
  }
}
