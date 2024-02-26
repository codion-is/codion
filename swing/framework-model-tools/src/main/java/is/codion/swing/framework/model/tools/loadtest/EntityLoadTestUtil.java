/*
 * Copyright (c) 2020 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.model.tools.loadtest;

import is.codion.framework.domain.entity.Entity;
import is.codion.framework.model.EntityTableModel;
import is.codion.swing.framework.model.component.EntityComboBoxModel;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * A utility class for load testing SwingEntityApplicationModel instances.
 */
public final class EntityLoadTestUtil{

  private static final Random RANDOM = new Random();

  private EntityLoadTestUtil() {}

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
    if (comboBoxModel.cleared()) {
      comboBoxModel.refresh();
    }
    List<Entity> visibleItems = comboBoxModel.visibleItems();
    if (visibleItems.isEmpty() || visibleItems.size() == 1 && visibleItems.get(0) == null) {
      return;
    }
    int fromIndex = visibleItems.get(0) == null ? 1 : 0;
    comboBoxModel.setSelectedItem(visibleItems.get(RANDOM.nextInt(visibleItems.size() - fromIndex) + fromIndex));
  }
}
