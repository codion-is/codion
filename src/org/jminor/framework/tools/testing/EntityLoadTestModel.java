/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.tools.testing;

import org.jminor.common.model.CancelException;
import org.jminor.common.model.LoadTestModel;
import org.jminor.common.model.User;
import org.jminor.framework.Configuration;
import org.jminor.framework.client.model.EntityApplicationModel;
import org.jminor.framework.client.model.EntityTableModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A class for running multiple EntityApplicationModel instances for load testing purposes.
 */
public abstract class EntityLoadTestModel extends LoadTestModel {

  private static final int DEFAULT_WARNING_TIME = 200;

  public EntityLoadTestModel(final User user, final UsageScenario... usageScenarios) {
    super(user, Arrays.asList(usageScenarios), Integer.parseInt(System.getProperty(Configuration.LOAD_TEST_THINKTIME, "2000")),
            Integer.parseInt(System.getProperty(Configuration.LOAD_TEST_LOGIN_DELAY, "2")),
            Integer.parseInt(System.getProperty(Configuration.LOAD_TEST_BATCH_SIZE, "10")), DEFAULT_WARNING_TIME);
    loadDomainModel();
  }

  public static void selectRandomRow(final EntityTableModel model) {
    if (model.getRowCount() == 0) {
      return;
    }

    model.setSelectedItemIndex(RANDOM.nextInt(model.getRowCount()));
  }

  public static void selectRandomRows(final EntityTableModel model, final int count) {
    if (model.getRowCount() == 0) {
      return;
    }

    final int startIdx = RANDOM.nextInt(model.getRowCount() - count);
    final List<Integer> indexes = new ArrayList<Integer>();
    for (int i = startIdx; i < count + startIdx; i++) {
      indexes.add(i);
    }

    model.setSelectedItemIndexes(indexes);
  }

  public static void selectRandomRows(final EntityTableModel model, final double ratio) {
    if (model.getRowCount() == 0) {
      return;
    }

    final int toSelect = ratio > 0 ? (int) Math.floor(model.getRowCount() * ratio) : 1;
    final List<Integer> indexes = new ArrayList<Integer>();
    for (int i = 0; i < toSelect; i++) {
      indexes.add(i);
    }

    model.setSelectedItemIndexes(indexes);
  }

  @Override
  protected final void disconnectApplication(final Object application) {
    ((EntityApplicationModel) application).getDbProvider().disconnect();
  }

  @Override
  protected abstract EntityApplicationModel initializeApplication() throws CancelException;

  protected abstract void loadDomainModel();
}
