/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.model.table;

import is.codion.common.event.EventListener;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Collections.emptyMap;
import static java.util.Collections.unmodifiableMap;
import static java.util.function.Function.identity;

final class DefaultTableConditionModel<C> implements TableConditionModel<C> {

  private final Map<C, ColumnConditionModel<C, ?>> conditionModels;

  DefaultTableConditionModel(Collection<ColumnConditionModel<C, ?>> conditionModels) {
    this.conditionModels = initializeColumnConditionModels(conditionModels);
  }

  @Override
  public void clear() {
    conditionModels.values().forEach(ColumnConditionModel::clear);
  }

  @Override
  public boolean isEnabled() {
    return conditionModels.values().stream()
            .anyMatch(ColumnConditionModel::isEnabled);
  }

  @Override
  public boolean isEnabled(C columnIdentifier) {
    return conditionModels.containsKey(columnIdentifier) && conditionModels.get(columnIdentifier).isEnabled();
  }

  @Override
  public Map<C, ColumnConditionModel<C, ?>> conditionModels() {
    return conditionModels;
  }

  @Override
  public <T> ColumnConditionModel<C, T> conditionModel(C columnIdentifier) {
    ColumnConditionModel<C, T> filterModel = (ColumnConditionModel<C, T>) conditionModels.get(columnIdentifier);
    if (filterModel == null) {
      throw new IllegalArgumentException("No filter model available for column: " + columnIdentifier);
    }

    return filterModel;
  }

  @Override
  public void addChangeListener(EventListener listener) {
    conditionModels.values().forEach(filterModel -> filterModel.addChangeListener(listener));
  }

  @Override
  public void removeChangeListener(EventListener listener) {
    conditionModels.values().forEach(filterModel -> filterModel.removeChangeListener(listener));
  }

  private Map<C, ColumnConditionModel<C, ?>> initializeColumnConditionModels(Collection<ColumnConditionModel<C, ?>> conditionModels) {
    if (conditionModels == null) {
      return emptyMap();
    }

    return unmodifiableMap(conditionModels.stream()
            .collect(Collectors.toMap(ColumnConditionModel::columnIdentifier, identity())));
  }
}
