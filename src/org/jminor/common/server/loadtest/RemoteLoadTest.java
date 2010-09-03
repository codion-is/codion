/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.server.loadtest;

import org.jminor.common.model.ItemRandomizer;
import org.jminor.common.model.LoadTest;
import org.jminor.common.model.User;

import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.YIntervalSeriesCollection;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Collection;

public interface RemoteLoadTest extends Remote {

  /**
   * Removes all applications and exits
   * @throws java.rmi.RemoteException in case of a remote exception
   */
  void exit() throws RemoteException;

  /**
   * @return the user to use when initializing new application instances
   * @throws java.rmi.RemoteException in case of a remote exception
   */
  User getUser() throws RemoteException;

  /**
   * @param user the user to use when initializing new application instances
   * @throws java.rmi.RemoteException in case of a remote exception
   */
  void setUser(User user) throws RemoteException;

  /**
   * @param scenarioName the name of the usage scenario to fetch
   * @return the usage scenario with the given name
   * @throws RuntimeException if no such scenario exists
   * @throws java.rmi.RemoteException in case of a remote exception
   */
  LoadTest.UsageScenario getUsageScenario(final String scenarioName) throws RemoteException;

  /**
   * @return the usage scenarios used by this load test
   * @throws java.rmi.RemoteException in case of a remote exception
   */
  Collection<String> getUsageScenarios() throws RemoteException;

  /**
   * @return the the maximum time in milliseconds a work request has to finish
   * @throws java.rmi.RemoteException in case of a remote exception
   */
  int getWarningTime() throws RemoteException;

  /**
   * @param warningTime the the maximum time in milliseconds a work request has to finish
   * @throws java.rmi.RemoteException in case of a remote exception
   */
  void setWarningTime(final int warningTime) throws RemoteException;

  /**
   * @return the chart data update interval
   * @throws java.rmi.RemoteException in case of a remote exception
   */
  int getUpdateInterval() throws RemoteException;

  /**
   * @param updateInterval the chart data update interval
   * @throws java.rmi.RemoteException in case of a remote exception
   */
  void setUpdateInterval(final int updateInterval) throws RemoteException;

  /**
   * @return the number of active applications
   * @throws java.rmi.RemoteException in case of a remote exception
   */
  int getApplicationCount() throws RemoteException;

  /**
   * @return the number of applications to initialize per batch
   * @throws java.rmi.RemoteException in case of a remote exception
   */
  int getApplicationBatchSize() throws RemoteException;

  /**
   * @param applicationBatchSize the number of applications to initialize per batch
   * @throws java.rmi.RemoteException in case of a remote exception
   */
  void setApplicationBatchSize(final int applicationBatchSize) throws RemoteException;

  /**
   * @return true if the load testing is paused
   * @throws java.rmi.RemoteException in case of a remote exception
   */
  boolean isPaused() throws RemoteException;

  /**
   * @param value true if load testing should be paused
   * @throws java.rmi.RemoteException in case of a remote exception
   */
  void setPaused(final boolean value) throws RemoteException;

  /**
   * @return the maximum number of milliseconds that should pass between work requests
   * @throws java.rmi.RemoteException in case of a remote exception
   */
  int getMaximumThinkTime() throws RemoteException;

  /**
   * @param maximumThinkTime the maximum number of milliseconds that should pass between work requests
   * @throws java.rmi.RemoteException in case of a remote exception
   */
  void setMaximumThinkTime(final int maximumThinkTime) throws RemoteException;

  /**
   * @return the minimum number of milliseconds that should pass between work requests
   * @throws java.rmi.RemoteException in case of a remote exception
   */
  int getMinimumThinkTime() throws RemoteException;

  /**
   * @param minimumThinkTime the minimum number of milliseconds that should pass between work requests
   * @throws java.rmi.RemoteException in case of a remote exception
   */
  void setMinimumThinkTime(final int minimumThinkTime) throws RemoteException;

  /**
   * Sets the with which to multiply the think time when logging in, this helps
   * spread the application logins when creating a batch of application.
   * @return the number with which to multiply the think time when logging in
   * @throws java.rmi.RemoteException in case of a remote exception
   */
  int getLoginDelayFactor() throws RemoteException;

  /**
   * Sets the with which to multiply the think time when logging in, this helps
   * spread the application logins when creating a batch of application.
   * @param loginDelayFactor the number with which to multiply the think time when logging in
   * @throws java.rmi.RemoteException in case of a remote exception
   */
  void setLoginDelayFactor(final int loginDelayFactor) throws RemoteException;

  /**
   * @return true if chart data is being collected
   * @throws java.rmi.RemoteException in case of a remote exception
   */
  boolean isCollectChartData() throws RemoteException;

  /**
   * @param value true if chart data should be collected
   * @throws java.rmi.RemoteException in case of a remote exception
   */
  void setCollectChartData(final boolean value) throws RemoteException;

  /**
   * Adds a batch of applications.
   * @throws java.rmi.RemoteException in case of a remote exception
   * @see #setApplicationBatchSize(int)
   */
  void addApplicationBatch() throws RemoteException;

  /**
   * Removes one batch of applications.
   * @throws java.rmi.RemoteException in case of a remote exception
   * @see #setApplicationBatchSize(int)
   */
  void removeApplicationBatch() throws RemoteException;

  /**
   * Resets the accumulated chart data
   * @throws java.rmi.RemoteException in case of a remote exception
   */
  void resetChartData() throws RemoteException;

  /**
   * @return a dataset plotting the average scenario duration
   * @param name the scenario name
   * @throws java.rmi.RemoteException in case of a remote exception
   */
  YIntervalSeriesCollection getScenarioDurationDataset(final String name) throws RemoteException;

  /**
   * @return a dataset plotting the think time
   * @throws java.rmi.RemoteException in case of a remote exception
   */
  XYDataset getThinkTimeDataset() throws RemoteException;

  /**
   * @return a dataset plotting the number of active applications
   * @throws java.rmi.RemoteException in case of a remote exception
   */
  XYDataset getNumberOfApplicationsDataset() throws RemoteException;

  /**
   * @return a dataset plotting the number of runs each usage scenario is being run per second
   * @throws java.rmi.RemoteException in case of a remote exception
   */
  XYDataset getUsageScenarioDataset() throws RemoteException;

  /**
   * @return a dataset plotting the memory usage of this load test model
   * @throws java.rmi.RemoteException in case of a remote exception
   */
  XYDataset getMemoryUsageDataset() throws RemoteException;

  /**
   * @return a dataset plotting the failure rate of each usage scenario
   * @throws java.rmi.RemoteException in case of a remote exception
   */
  XYDataset getUsageScenarioFailureDataset() throws RemoteException;
  /**
   * @return the number of items in this model.
   * @throws java.rmi.RemoteException in case of a remote exception
   */
  int getItemCount() throws RemoteException;

  /**
   * @return the items in this model.
   * @throws java.rmi.RemoteException in case of a remote exception
   */
  Collection<ItemRandomizer.RandomItem<LoadTest.UsageScenario>> getItems() throws RemoteException;

  /**
   * Returns the weight of the given item.
   * @param item the item
   * @return the item weight
   * @throws java.rmi.RemoteException in case of a remote exception
   */
  int getWeight(Object item) throws RemoteException;

  /**
   * Sets the weight of the given item
   * @param item the item
   * @param weight the value
   * @throws java.rmi.RemoteException in case of a remote exception
   */
  void setWeight(Object item, int weight) throws RemoteException;

  /**
   * Adds the given item to this model with default weight of 0.
   * @param item the item to add
   * @throws java.rmi.RemoteException in case of a remote exception
   */
  void addItem(final Object item) throws RemoteException;

  /**
   * Adds the given item to this model with the given weight value.
   * @param item the item to add
   * @param weight the initial weight to assign to the item
   * @throws java.rmi.RemoteException in case of a remote exception
   */
  void addItem(Object item, int weight) throws RemoteException;

  /**
   * Fetches a random item from this model based on the item weights.
   * @return a randomly chosen item.
   * @throws java.rmi.RemoteException in case of a remote exception
   */
  LoadTest.UsageScenario getRandomItem() throws RemoteException;

  /**
   * Returns this items share in the total weights as a floating point number between 0 and 1
   * @param item the item
   * @return the ratio of the total weights held by the given item
   * @throws java.rmi.RemoteException in case of a remote exception
   */
  double getWeightRatio(final Object item) throws RemoteException;

  /**
   * Increments the weight of the given item by one
   * @param item the item
   * @throws java.rmi.RemoteException in case of a remote exception
   */
  void incrementWeight(final Object item) throws RemoteException;

  /**
   * Decrements the weight of the given item by one
   * @param item the item
   * @throws IllegalStateException in case the weight is 0
   * @throws java.rmi.RemoteException in case of a remote exception
   */
  void decrementWeight(final Object item) throws RemoteException;
}
