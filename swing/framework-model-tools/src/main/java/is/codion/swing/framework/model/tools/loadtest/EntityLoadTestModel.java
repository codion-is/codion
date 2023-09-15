/*
 * Copyright (c) 2008 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.model.tools.loadtest;

import is.codion.common.Configuration;
import is.codion.common.property.PropertyValue;
import is.codion.common.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.model.EntityTableModel;
import is.codion.swing.common.model.tools.loadtest.LoadTest;
import is.codion.swing.common.model.tools.loadtest.UsageScenario;
import is.codion.swing.framework.model.SwingEntityApplicationModel;
import is.codion.swing.framework.model.component.EntityComboBoxModel;

import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * A class for running multiple EntityApplicationModel instances for load testing purposes.
 * @param <M> the application model type used by this load test model
 */
public abstract class EntityLoadTestModel<M extends SwingEntityApplicationModel> {

  private static final int DEFAULT_LOAD_TEST_THINKTIME = 2000;
  private static final int DEFAULT_LOAD_TEST_BATCH_SIZE = 10;
  private static final int DEFAULT_LOAD_TEST_LOGIN_DELAY = 2;

  /**
   * Specifies the hostname of the remote load test server<br>
   * Value type: String<br>
   * Default value: localhost
   */
  public static final PropertyValue<String> LOAD_TEST_REMOTE_HOSTNAME = Configuration.stringValue("codion.loadtest.remote.hostname", "localhost");

  /**
   * Specifies the initial think time setting for the load test client
   * (max think time = thinktime, min think time = max think time / 2)<br>
   * Value type: Integer<br>
   * Default value: 2000
   */
  public static final PropertyValue<Integer> LOAD_TEST_THINKTIME = Configuration.integerValue("codion.loadtest.thinktime", DEFAULT_LOAD_TEST_THINKTIME);

  /**
   * Specifies the initial client batch size<br>
   * Value type: Integer<br>
   * Default value: 10
   */
  public static final PropertyValue<Integer> LOAD_TEST_BATCH_SIZE = Configuration.integerValue("codion.loadtest.batchsize", DEFAULT_LOAD_TEST_BATCH_SIZE);

  /**
   * Specifies the number with which the max think time is multiplied when initializing the clients<br>
   * Value type: Integer<br>
   * Default value: 2
   */
  public static final PropertyValue<Integer> LOAD_TEST_LOGIN_DELAY = Configuration.integerValue("codion.loadtest.logindelay", DEFAULT_LOAD_TEST_LOGIN_DELAY);

  private static final Random RANDOM = new Random();

  private final LoadTest<M> loadTestModel;

  /**
   * Instantiates a new EntityLoadTestModel.
   * @param user the default user
   * @param usageScenarios the usage scenarios
   */
  protected EntityLoadTestModel(User user, Collection<? extends UsageScenario<M>> usageScenarios) {
    this.loadTestModel = LoadTest.builder(this::createApplication, this::closeApplication)
            .usageScenarios(usageScenarios)
            .user(user)
            .minimumThinkTime(LOAD_TEST_THINKTIME.get() / 2)
            .maximumThinkTime(LOAD_TEST_THINKTIME.get())
            .loginDelayFactor(LOAD_TEST_LOGIN_DELAY.get())
            .applicationBatchSize(LOAD_TEST_BATCH_SIZE.get())
            .titleFactory(loadTest -> getClass().getSimpleName() + " - " + EntityConnectionProvider.CLIENT_CONNECTION_TYPE.get())
            .build();
  }

  public final LoadTest<M> loadTestModel() {
    return loadTestModel;
  }

  /**
   * Selects a random row in the given table model
   * @param tableModel the table model
   */
  public static void selectRandomRow(EntityTableModel<?> tableModel) {
    if (tableModel.getRowCount() == 0) {
      return;
    }

    tableModel.selectionModel().setSelectedIndex(RANDOM.nextInt(tableModel.getRowCount()));
  }

  /**
   * Selects random rows in the given table model
   * @param tableModel the table model
   * @param count the number of rows to select
   */
  public static void selectRandomRows(EntityTableModel<?> tableModel, int count) {
    if (tableModel.getRowCount() == 0) {
      return;
    }
    if (tableModel.getRowCount() <= count) {
      tableModel.selectionModel().selectAll();
    }
    else {
      int startIdx = RANDOM.nextInt(tableModel.getRowCount() - count);

      tableModel.selectionModel().setSelectedIndexes(IntStream.range(startIdx, count + startIdx)
              .boxed()
              .collect(Collectors.toList()));
    }
  }

  /**
   * Selects random rows in the given table model
   * @param tableModel the table model
   * @param ratio the ratio of available rows to select
   */
  public static void selectRandomRows(EntityTableModel<?> tableModel, double ratio) {
    selectRandomRows(tableModel, ratio > 0 ? (int) Math.floor(tableModel.getRowCount() * ratio) : 1);
  }

  /**
   * Selects a random non-null visible item in the given combobox model, if one is available
   * @param comboBoxModel the combobox model
   */
  public static void selectRandomItem(EntityComboBoxModel comboBoxModel) {
    if (comboBoxModel.isCleared()) {
      comboBoxModel.refresh();
    }
    List<Entity> visibleItems = comboBoxModel.visibleItems();
    if (visibleItems.isEmpty() || visibleItems.size() == 1 && visibleItems.get(0) == null) {
      return;
    }
    int fromIndex = visibleItems.get(0) == null ? 1 : 0;
    comboBoxModel.setSelectedItem(visibleItems.get(RANDOM.nextInt(visibleItems.size() - fromIndex) + fromIndex));
  }

  protected abstract M createApplication(User user);

  private void closeApplication(M application) {
    application.connectionProvider().close();
  }
}
