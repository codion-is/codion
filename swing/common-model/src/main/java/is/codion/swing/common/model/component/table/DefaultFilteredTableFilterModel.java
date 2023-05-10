/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.model.component.table;

import is.codion.common.Operator;
import is.codion.common.event.EventListener;
import is.codion.common.model.table.ColumnConditionModel;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.emptyMap;
import static java.util.Collections.unmodifiableMap;

final class DefaultFilteredTableFilterModel<C> implements FilteredTableFilterModel<C> {

  private final Map<C, ColumnConditionModel<C, ?>> filterModels;

  DefaultFilteredTableFilterModel(Collection<ColumnConditionModel<C, ?>> filterModels) {
    this.filterModels = initializeColumnFilterModels(filterModels);
  }

  @Override
  public void clear() {
    filterModels.values().forEach(ColumnConditionModel::clearCondition);
  }

  @Override
  public boolean isEnabled() {
    return filterModels.values().stream()
            .anyMatch(ColumnConditionModel::isEnabled);
  }

  @Override
  public boolean isEnabled(C columnIdentifier) {
    return filterModels.containsKey(columnIdentifier) && filterModels.get(columnIdentifier).isEnabled();
  }

  @Override
  public Map<C, ColumnConditionModel<C, ?>> conditionModels() {
    return filterModels;
  }

  @Override
  public <T> ColumnConditionModel<C, T> conditionModel(C columnIdentifier) {
    ColumnConditionModel<C, T> filterModel = (ColumnConditionModel<C, T>) filterModels.get(columnIdentifier);
    if (filterModel == null) {
      throw new IllegalArgumentException("No filter model available for column: " + columnIdentifier);
    }

    return filterModel;
  }

  @Override
  public <T> void setEqualFilterValue(C columnIdentifier, Comparable<T> value) {
    ColumnConditionModel<C, T> filterModel = (ColumnConditionModel<C, T>) filterModels.get(columnIdentifier);
    if (filterModel != null) {
      filterModel.setOperator(Operator.EQUAL);
      filterModel.setEqualValue((T) value);
      filterModel.setEnabled(value != null);
    }
  }

  @Override
  public void addChangeListener(EventListener listener) {
    filterModels.values().forEach(filterModel -> filterModel.addChangeListener(listener));
  }

  @Override
  public void removeChangeListener(EventListener listener) {
    filterModels.values().forEach(filterModel -> filterModel.removeChangeListener(listener));
  }

  private Map<C, ColumnConditionModel<C, ?>> initializeColumnFilterModels(Collection<ColumnConditionModel<C, ?>> filterModels) {
    if (filterModels == null) {
      return emptyMap();
    }

    Map<C, ColumnConditionModel<C, ?>> filterMap = new HashMap<>();
    for (ColumnConditionModel<C, ?> columnFilterModel : filterModels) {
      filterMap.put(columnFilterModel.columnIdentifier(), columnFilterModel);
    }

    return unmodifiableMap(filterMap);
  }
}
