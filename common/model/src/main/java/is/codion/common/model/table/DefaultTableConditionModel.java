/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.model.table;

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
            .anyMatch(model -> model.enabled().get());
  }

  @Override
  public boolean isEnabled(C columnIdentifier) {
    return conditionModels.containsKey(columnIdentifier) && conditionModels.get(columnIdentifier).enabled().get();
  }

  @Override
  public Map<C, ColumnConditionModel<C, ?>> conditionModels() {
    return conditionModels;
  }

  @Override
  public <T> ColumnConditionModel<C, T> conditionModel(C columnIdentifier) {
    ColumnConditionModel<C, T> conditionModel = (ColumnConditionModel<C, T>) conditionModels.get(columnIdentifier);
    if (conditionModel == null) {
      throw new IllegalArgumentException("No condition model available for column: " + columnIdentifier);
    }

    return conditionModel;
  }

  @Override
  public void addChangeListener(Runnable listener) {
    conditionModels.values().forEach(filterModel -> filterModel.addChangeListener(listener));
  }

  @Override
  public void removeChangeListener(Runnable listener) {
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
