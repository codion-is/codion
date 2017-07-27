/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.model;

import org.jminor.common.Configuration;
import org.jminor.common.User;
import org.jminor.common.Value;
import org.jminor.common.model.tools.LoadTestModel;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.domain.Entity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A class for running multiple EntityApplicationModel instances for load testing purposes.
 * @param <M> the application model type used by this load test model
 */
public abstract class EntityLoadTestModel<M extends EntityApplicationModel> extends LoadTestModel<M> {

  private static final int DEFAULT_LOAD_TEST_THINKTIME = 2000;
  private static final int DEFAULT_LOAD_TEST_BATCH_SIZE = 10;
  private static final int DEFAULT_LOAD_TEST_LOGIN_DELAY = 2;

  /**
   * Specifies the hostname of the remote load test server<br>
   * Value type: String<br>
   * Default value: localhost
   */
  public static final Value<String> LOAD_TEST_REMOTE_HOSTNAME = Configuration.stringValue("jminor.loadtest.remote.hostname", "localhost");
  /**
   * Specifies the initial think time setting for the load test client
   * (max think time = thinktime, min think time = max think time / 2)<br>
   * Value type: Integer<br>
   * Default value: 2000
   */
  public static final Value<Integer> LOAD_TEST_THINKTIME = Configuration.integerValue("jminor.loadtest.thinktime", DEFAULT_LOAD_TEST_THINKTIME);

  /**
   * Specifies the initial client batch size<br>
   * Value type: Integer<br>
   * Default value: 10
   */
  public static final Value<Integer> LOAD_TEST_BATCH_SIZE = Configuration.integerValue("jminor.loadtest.batchsize", DEFAULT_LOAD_TEST_BATCH_SIZE);

  /**
   * Specifies the number which the max think time is multiplied with when initializing the clients<br>
   * Value type: Integer<br>
   * Default value: 2
   */
  public static final Value<Integer> LOAD_TEST_LOGIN_DELAY = Configuration.integerValue("jminor.loadtest.logindelay", DEFAULT_LOAD_TEST_LOGIN_DELAY);

  /**
   * Instantiates a new EntityLoadTestModel.
   * Note that {@link EntityConnectionProvider#CONNECTION_SCHEDULE_VALIDATION} is set to false when this class is instantiated
   * @param user the default user
   * @param usageScenarios the usage scenarios
   */
  public EntityLoadTestModel(final User user, final Collection<? extends UsageScenario<M>> usageScenarios) {
    super(user, usageScenarios, LOAD_TEST_THINKTIME.get(), LOAD_TEST_LOGIN_DELAY.get(),
            LOAD_TEST_BATCH_SIZE.get());
    EntityConnectionProvider.CONNECTION_SCHEDULE_VALIDATION.set(false);
  }

  /**
   * Selects a random row in the given table model
   * @param tableModel the table model
   */
  public static void selectRandomRow(final EntityTableModel tableModel) {
    if (tableModel.getRowCount() == 0) {
      return;
    }

    tableModel.getSelectionModel().setSelectedIndex(RANDOM.nextInt(tableModel.getRowCount()));
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
    if (tableModel.getRowCount() <= count) {
      tableModel.getSelectionModel().selectAll();
    }
    else {
      final int startIdx = RANDOM.nextInt(tableModel.getRowCount() - count);
      final List<Integer> indexes = new ArrayList<>();
      for (int i = startIdx; i < count + startIdx; i++) {
        indexes.add(i);
      }

      tableModel.getSelectionModel().setSelectedIndexes(indexes);
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
    final List<Integer> indexes = new ArrayList<>();
    for (int i = 0; i < toSelect; i++) {
      indexes.add(i);
    }

    tableModel.getSelectionModel().setSelectedIndexes(indexes);
  }

  /**
   * Selects a random non-null visible item in the given combobox model, if one is available
   * @param comboBoxModel the combobox model
   */
  public static void selectRandomItem(final EntityComboBoxModel comboBoxModel) {
    if (comboBoxModel.isCleared()) {
      comboBoxModel.refresh();
    }
    final List<Entity> visibleItems = comboBoxModel.getVisibleItems();
    if (visibleItems.isEmpty() || visibleItems.size() == 1 && visibleItems.get(0) == null) {
      return;
    }
    final int fromIndex = visibleItems.get(0) == null ? 1 : 0;
    comboBoxModel.setSelectedItem(visibleItems.get(RANDOM.nextInt(visibleItems.size() - fromIndex) + fromIndex));
  }

  /** {@inheritDoc} */
  @Override
  protected final void disconnectApplication(final M application) {
    application.getConnectionProvider().disconnect();
  }

  /** {@inheritDoc} */
  @Override
  protected abstract M initializeApplication();

  /**
   * An abstract base class for usage scenarios based on EntityApplicationModel instances
   * @param <M> the application model type used by this usage scenario
   */
  public abstract static class AbstractEntityUsageScenario<M extends EntityApplicationModel>
          extends AbstractUsageScenario<M> {

    /**
     * Instantiates a new AbstractEntityUsageScenario
     */
    public AbstractEntityUsageScenario() {
      super();
    }

    /**
     * Instantiates a new AbstractEntityUsageScenario
     * @param name the scenario name
     */
    public AbstractEntityUsageScenario(final String name) {
      super(name);
    }
  }
}
