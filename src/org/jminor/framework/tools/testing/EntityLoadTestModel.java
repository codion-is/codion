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
public abstract class EntityLoadTestModel extends LoadTestModel<EntityApplicationModel> {

  /**
   * Instantiates a new EntityLoadTestModel.
   * @param user the default user
   * @param usageScenarios the usage scenarios
   */
  public EntityLoadTestModel(final User user, final UsageScenario<EntityApplicationModel>... usageScenarios) {
    super(user, Arrays.asList(usageScenarios), Configuration.getIntValue(Configuration.LOAD_TEST_THINKTIME),
            Configuration.getIntValue(Configuration.LOAD_TEST_LOGIN_DELAY),
            Configuration.getIntValue(Configuration.LOAD_TEST_BATCH_SIZE));
  }

  /**
   * Selects a random row in the given table model
   * @param tableModel the table model
   */
  public static void selectRandomRow(final EntityTableModel tableModel) {
    if (tableModel.getRowCount() == 0) {
      return;
    }

    tableModel.setSelectedIndex(RANDOM.nextInt(tableModel.getRowCount()));
  }

  /**
   * Selects random rows in the given table model
   * @param tableModel the table model
   * @param count the number of rows to select
   */
  public static void selectRandomRows(final EntityTableModel tableModel, final int count) {
    if (tableModel.getRowCount() == 0) {
      return;
    }
    if (tableModel.getRowCount() < count) {
      tableModel.selectAll();
    }
    else {
      final int startIdx = RANDOM.nextInt(tableModel.getRowCount() - count);
      final List<Integer> indexes = new ArrayList<Integer>();
      for (int i = startIdx; i < count + startIdx; i++) {
        indexes.add(i);
      }

      tableModel.setSelectedIndexes(indexes);
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

    tableModel.setSelectedIndexes(indexes);
  }

  /** {@inheritDoc} */
  @Override
  protected final void disconnectApplication(final EntityApplicationModel application) {
    application.getConnectionProvider().disconnect();
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
}
