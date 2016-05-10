/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.model;

import org.jminor.common.User;
import org.jminor.common.model.tools.LoadTestModel;
import org.jminor.framework.Configuration;
import org.jminor.framework.domain.Entity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A class for running multiple EntityApplicationModel instances for load testing purposes.
 */
public abstract class EntityLoadTestModel<ApplicationModel extends EntityApplicationModel> extends LoadTestModel<ApplicationModel> {

  /**
   * Instantiates a new EntityLoadTestModel.
   * Note that {@link Configuration#CONNECTION_SCHEDULE_VALIDATION} is set to false when this class is instantiated
   * @param user the default user
   * @param usageScenarios the usage scenarios
   */
  public EntityLoadTestModel(final User user, final Collection<? extends UsageScenario<ApplicationModel>> usageScenarios) {
    super(user, usageScenarios, Configuration.getIntValue(Configuration.LOAD_TEST_THINKTIME),
            Configuration.getIntValue(Configuration.LOAD_TEST_LOGIN_DELAY),
            Configuration.getIntValue(Configuration.LOAD_TEST_BATCH_SIZE));
    Configuration.setValue(Configuration.CONNECTION_SCHEDULE_VALIDATION, false);
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
  protected final void disconnectApplication(final ApplicationModel application) {
    application.getConnectionProvider().disconnect();
  }

  /** {@inheritDoc} */
  @Override
  protected abstract ApplicationModel initializeApplication();

  /**
   * An abstract base class for usage scenarios based on EntityApplicationModel instances
   */
  public abstract static class AbstractEntityUsageScenario<ApplicationModel extends EntityApplicationModel>
          extends AbstractUsageScenario<ApplicationModel> {

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
