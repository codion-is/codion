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

public class RemoteLoadTestAdapter extends UnicastRemoteObject implements RemoteLoadTest {

  private final LoadTest loadTest;
  private final ClientInfo clientInfo;
  private static final String LOAD_TEST_CLASSNAME = "jminor.loadtest.className";

  public RemoteLoadTestAdapter(final ClientInfo clientInfo, final int loadTestPort) throws RemoteException {
    super(loadTestPort, RMISocketFactory.getSocketFactory(), RMISocketFactory.getSocketFactory());
    this.clientInfo = clientInfo;
    this.loadTest = instantiateLoadTest(clientInfo);
  }

  private LoadTest instantiateLoadTest(ClientInfo clientInfo) throws RemoteException {
    final String loadTestClass = (String) clientInfo.getProperty(LOAD_TEST_CLASSNAME);
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

  ClientInfo getClientInfo() {
    return clientInfo;
  }

  public void exit() throws RemoteException {
    loadTest.exit();
  }

  public User getUser() {
    return loadTest.getUser();
  }

  public void setUser(final User user) {
    loadTest.setUser(user);
  }

  public LoadTest.UsageScenario getUsageScenario(final String scenarioName) {
    return loadTest.getUsageScenario(scenarioName);
  }

  public Collection<String> getUsageScenarios() {
    return loadTest.getUsageScenarios();
  }

  public int getWarningTime() {
    return loadTest.getWarningTime();
  }

  public void setWarningTime(int warningTime) {
    loadTest.setWarningTime(warningTime);
  }

  public int getUpdateInterval() {
    return loadTest.getUpdateInterval();
  }

  public void setUpdateInterval(int updateInterval) {
    loadTest.setUpdateInterval(updateInterval);
  }

  public int getApplicationCount() {
    return loadTest.getApplicationCount();
  }

  public int getApplicationBatchSize() {
    return loadTest.getApplicationBatchSize();
  }

  public void setApplicationBatchSize(int applicationBatchSize) {
    loadTest.setApplicationBatchSize(applicationBatchSize);
  }

  public boolean isPaused() {
    return loadTest.isPaused();
  }

  public void setPaused(boolean value) {
    loadTest.setPaused(value);
  }

  public int getMaximumThinkTime() {
    return loadTest.getMaximumThinkTime();
  }

  public void setMaximumThinkTime(int maximumThinkTime) {
    loadTest.setMaximumThinkTime(maximumThinkTime);
  }

  public int getMinimumThinkTime() {
    return loadTest.getMinimumThinkTime();
  }

  public void setMinimumThinkTime(int minimumThinkTime) {
    loadTest.setMinimumThinkTime(minimumThinkTime);
  }

  public int getLoginDelayFactor() {
    return loadTest.getLoginDelayFactor();
  }

  public void setLoginDelayFactor(int loginDelayFactor) {
    loadTest.setLoginDelayFactor(loginDelayFactor);
  }

  public boolean isCollectChartData() {
    return loadTest.isCollectChartData();
  }

  public void setCollectChartData(boolean value) {
    loadTest.setCollectChartData(value);
  }

  public void addApplicationBatch() {
    loadTest.addApplicationBatch();
  }

  public void removeApplicationBatch() {
    loadTest.removeApplicationBatch();
  }

  public void resetChartData() {
    loadTest.resetChartData();
  }

  public YIntervalSeriesCollection getScenarioDurationDataset(String name) {
    return loadTest.getScenarioDurationDataset(name);
  }

  public XYDataset getThinkTimeDataset() {
    return loadTest.getThinkTimeDataset();
  }

  public XYDataset getNumberOfApplicationsDataset() {
    return loadTest.getNumberOfApplicationsDataset();
  }

  public XYDataset getUsageScenarioDataset() {
    return loadTest.getUsageScenarioDataset();
  }

  public XYDataset getMemoryUsageDataset() {
    return loadTest.getMemoryUsageDataset();
  }

  public XYDataset getUsageScenarioFailureDataset() {
    return loadTest.getUsageScenarioFailureDataset();
  }

  public int getItemCount() {
    return loadTest.getScenarioChooser().getItemCount();
  }

  public Collection<ItemRandomizer.RandomItem<LoadTest.UsageScenario>> getItems() {
    return loadTest.getScenarioChooser().getItems();
  }

  public int getWeight(Object  item) {
    return loadTest.getScenarioChooser().getWeight((LoadTest.UsageScenario) item);
  }

  public void setWeight(Object  item, int weight) {
    loadTest.getScenarioChooser().setWeight((LoadTest.UsageScenario) item, weight);
  }

  public void addItem(Object  item) {
    loadTest.getScenarioChooser().addItem((LoadTest.UsageScenario) item);
  }

  public void addItem(Object  item, int weight) {
    loadTest.getScenarioChooser().addItem((LoadTest.UsageScenario) item, weight);
  }

  public LoadTest.UsageScenario getRandomItem() {
    return loadTest.getScenarioChooser().getRandomItem();
  }

  public double getWeightRatio(Object item) {
    return loadTest.getScenarioChooser().getWeightRatio((LoadTest.UsageScenario) item);
  }

  public void incrementWeight(Object item) {
    loadTest.getScenarioChooser().incrementWeight((LoadTest.UsageScenario) (item));
  }

  public void decrementWeight(Object item) {
    loadTest.getScenarioChooser().decrementWeight((LoadTest.UsageScenario) item);
  }
}
