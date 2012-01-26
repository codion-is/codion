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
 * @see RemoteLoadTestServer
 */
final class RemoteLoadTestImpl extends UnicastRemoteObject implements RemoteLoadTest {

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
  RemoteLoadTestImpl(final ClientInfo clientInfo, final int loadTestPort) throws RemoteException {
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

  @Override
  public void exit() throws RemoteException {
    loadTest.exit();
  }

  /** {@inheritDoc} */
  @Override
  public User getUser() {
    return loadTest.getUser();
  }

  /** {@inheritDoc} */
  @Override
  public void setUser(final User user) {
    loadTest.setUser(user);
  }

  /** {@inheritDoc} */
  @Override
  public LoadTest.UsageScenario getUsageScenario(final String scenarioName) {
    return loadTest.getUsageScenario(scenarioName);
  }

  /** {@inheritDoc} */
  @Override
  public Collection<String> getUsageScenarios() {
    return loadTest.getUsageScenarios();
  }

  /** {@inheritDoc} */
  @Override
  public int getWarningTime() {
    return loadTest.getWarningTime();
  }

  /** {@inheritDoc} */
  @Override
  public void setWarningTime(final int warningTime) {
    loadTest.setWarningTime(warningTime);
  }

  @Override
  public int getUpdateInterval() {
    return loadTest.getUpdateInterval();
  }

  /** {@inheritDoc} */
  @Override
  public void setUpdateInterval(final int updateInterval) {
    loadTest.setUpdateInterval(updateInterval);
  }

  /** {@inheritDoc} */
  @Override
  public int getApplicationCount() {
    return loadTest.getApplicationCount();
  }

  /** {@inheritDoc} */
  @Override
  public int getApplicationBatchSize() {
    return loadTest.getApplicationBatchSize();
  }

  /** {@inheritDoc} */
  @Override
  public void setApplicationBatchSize(final int applicationBatchSize) {
    loadTest.setApplicationBatchSize(applicationBatchSize);
  }

  /** {@inheritDoc} */
  @Override
  public boolean isPaused() {
    return loadTest.isPaused();
  }

  /** {@inheritDoc} */
  @Override
  public void setPaused(final boolean value) {
    loadTest.setPaused(value);
  }

  /** {@inheritDoc} */
  @Override
  public int getMaximumThinkTime() {
    return loadTest.getMaximumThinkTime();
  }

  /** {@inheritDoc} */
  @Override
  public void setMaximumThinkTime(final int maximumThinkTime) {
    loadTest.setMaximumThinkTime(maximumThinkTime);
  }

  @Override
  public int getMinimumThinkTime() {
    return loadTest.getMinimumThinkTime();
  }

  /** {@inheritDoc} */
  @Override
  public void setMinimumThinkTime(final int minimumThinkTime) {
    loadTest.setMinimumThinkTime(minimumThinkTime);
  }

  /** {@inheritDoc} */
  @Override
  public int getLoginDelayFactor() {
    return loadTest.getLoginDelayFactor();
  }

  /** {@inheritDoc} */
  @Override
  public void setLoginDelayFactor(final int loginDelayFactor) {
    loadTest.setLoginDelayFactor(loginDelayFactor);
  }

  /** {@inheritDoc} */
  @Override
  public boolean isCollectChartData() {
    return loadTest.isCollectChartData();
  }

  /** {@inheritDoc} */
  @Override
  public void setCollectChartData(final boolean value) {
    loadTest.setCollectChartData(value);
  }

  /** {@inheritDoc} */
  @Override
  public void addApplicationBatch() {
    loadTest.addApplicationBatch();
  }

  /** {@inheritDoc} */
  @Override
  public void removeApplicationBatch() {
    loadTest.removeApplicationBatch();
  }

  /** {@inheritDoc} */
  @Override
  public void resetChartData() {
    loadTest.resetChartData();
  }

  @Override
  public YIntervalSeriesCollection getScenarioDurationDataset(final String name) {
    return loadTest.getScenarioDurationDataset(name);
  }

  /** {@inheritDoc} */
  @Override
  public XYDataset getThinkTimeDataset() {
    return loadTest.getThinkTimeDataset();
  }

  /** {@inheritDoc} */
  @Override
  public XYDataset getNumberOfApplicationsDataset() {
    return loadTest.getNumberOfApplicationsDataset();
  }

  /** {@inheritDoc} */
  @Override
  public XYDataset getUsageScenarioDataset() {
    return loadTest.getUsageScenarioDataset();
  }

  /** {@inheritDoc} */
  @Override
  public XYDataset getMemoryUsageDataset() {
    return loadTest.getMemoryUsageDataset();
  }

  /** {@inheritDoc} */
  @Override
  public XYDataset getUsageScenarioFailureDataset() {
    return loadTest.getUsageScenarioFailureDataset();
  }

  /** {@inheritDoc} */
  @Override
  public int getItemCount() {
    return loadTest.getScenarioChooser().getItemCount();
  }

  /** {@inheritDoc} */
  @Override
  public Collection<ItemRandomizer.RandomItem<LoadTest.UsageScenario>> getItems() {
    return loadTest.getScenarioChooser().getItems();
  }

  /** {@inheritDoc} */
  @Override
  public int getWeight(final Object item) {
    return loadTest.getScenarioChooser().getWeight((LoadTest.UsageScenario) item);
  }

  @Override
  public void setWeight(final Object item, final int weight) {
    loadTest.getScenarioChooser().setWeight((LoadTest.UsageScenario) item, weight);
  }

  /** {@inheritDoc} */
  @Override
  public void addItem(final Object item) {
    loadTest.getScenarioChooser().addItem((LoadTest.UsageScenario) item);
  }

  /** {@inheritDoc} */
  @Override
  public void addItem(final Object item, final int weight) {
    loadTest.getScenarioChooser().addItem((LoadTest.UsageScenario) item, weight);
  }

  /** {@inheritDoc} */
  @Override
  public LoadTest.UsageScenario getRandomItem() {
    return loadTest.getScenarioChooser().getRandomItem();
  }

  /** {@inheritDoc} */
  @Override
  public double getWeightRatio(final Object item) {
    return loadTest.getScenarioChooser().getWeightRatio((LoadTest.UsageScenario) item);
  }

  /** {@inheritDoc} */
  @Override
  public void incrementWeight(final Object item) {
    loadTest.getScenarioChooser().incrementWeight((LoadTest.UsageScenario) (item));
  }

  /** {@inheritDoc} */
  @Override
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
