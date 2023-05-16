/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.model;

import is.codion.common.Operator;
import is.codion.common.event.EventDataListener;
import is.codion.common.event.EventListener;
import is.codion.common.model.table.ColumnConditionModel;
import is.codion.common.state.State;
import is.codion.common.state.StateObserver;
import is.codion.common.value.Value;
import is.codion.common.value.ValueSet;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.ForeignKey;

import java.text.Format;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static java.util.Arrays.asList;

/**
 * An abstract base class for {@link ForeignKey} based {@link ColumnConditionModel}s.
 */
public abstract class AbstractForeignKeyConditionModel implements ColumnConditionModel<ForeignKey, Entity> {

  private final ColumnConditionModel<ForeignKey, Entity> conditionModel;

  protected AbstractForeignKeyConditionModel(ForeignKey foreignKey) {
    conditionModel = ColumnConditionModel.builder(foreignKey, Entity.class)
            .operators(asList(Operator.EQUAL, Operator.NOT_EQUAL))
            .build();
  }

  @Override
  public final ForeignKey columnIdentifier() {
    return conditionModel.columnIdentifier();
  }

  @Override
  public final State caseSensitiveState() {
    return conditionModel.caseSensitiveState();
  }

  @Override
  public final Format format() {
    return conditionModel.format();
  }

  @Override
  public final String dateTimePattern() {
    return conditionModel.dateTimePattern();
  }

  @Override
  public final void setLocked(boolean locked) {
    conditionModel.setLocked(locked);
  }

  @Override
  public final boolean isLocked() {
    return conditionModel.isLocked();
  }

  @Override
  public final Class<Entity> columnClass() {
    return conditionModel.columnClass();
  }

  @Override
  public final void setEqualValue(Entity value) {
    conditionModel.setEqualValue(value);
  }

  @Override
  public final Entity getEqualValue() {
    return conditionModel.getEqualValue();
  }

  @Override
  public final void setEqualValues(Collection<Entity> values) {
    conditionModel.setEqualValues(values);
  }

  @Override
  public final Collection<Entity> getEqualValues() {
    return conditionModel.getEqualValues();
  }

  @Override
  public final void setUpperBound(Entity value) {
    conditionModel.setUpperBound(value);
  }

  @Override
  public final Entity getUpperBound() {
    return conditionModel.getUpperBound();
  }

  @Override
  public final void setLowerBound(Entity value) {
    conditionModel.setLowerBound(value);
  }

  @Override
  public final Entity getLowerBound() {
    return conditionModel.getLowerBound();
  }

  @Override
  public final Operator getOperator() {
    return conditionModel.getOperator();
  }

  @Override
  public final void setOperator(Operator operator) {
    conditionModel.setOperator(operator);
  }

  @Override
  public final Value<Operator> operatorValue() {
    return conditionModel.operatorValue();
  }

  @Override
  public final List<Operator> operators() {
    return conditionModel.operators();
  }

  @Override
  public final char wildcard() {
    return conditionModel.wildcard();
  }

  @Override
  public final boolean isEnabled() {
    return conditionModel.isEnabled();
  }

  @Override
  public final void setEnabled(boolean enabled) {
    conditionModel.setEnabled(enabled);
  }

  @Override
  public final Value<AutomaticWildcard> automaticWildcardValue() {
    return conditionModel.automaticWildcardValue();
  }

  @Override
  public final State autoEnableState() {
    return conditionModel.autoEnableState();
  }

  @Override
  public final void clear() {
    conditionModel.clear();
  }

  @Override
  public final boolean accepts(Comparable<Entity> columnValue) {
    return conditionModel.accepts(columnValue);
  }

  @Override
  public final StateObserver lockedObserver() {
    return conditionModel.lockedObserver();
  }

  @Override
  public final ValueSet<Entity> equalValueSet() {
    return conditionModel.equalValueSet();
  }

  @Override
  public final Value<Entity> lowerBoundValue() {
    return conditionModel.lowerBoundValue();
  }

  @Override
  public final Value<Entity> upperBoundValue() {
    return conditionModel.upperBoundValue();
  }

  @Override
  public final State enabledState() {
    return conditionModel.enabledState();
  }

  @Override
  public final void addEnabledListener(EventListener listener) {
    conditionModel.addEnabledListener(listener);
  }

  @Override
  public final void removeEnabledListener(EventListener listener) {
    conditionModel.removeEnabledListener(listener);
  }

  @Override
  public final void addEqualValueListener(EventDataListener<Entity> listener) {
    conditionModel.addEqualValueListener(listener);
  }

  @Override
  public final void removeEqualValueListener(EventDataListener<Entity> listener) {
    conditionModel.removeEqualValueListener(listener);
  }

  @Override
  public void addEqualValuesListener(EventDataListener<Set<Entity>> listener) {
    conditionModel.addEqualValuesListener(listener);
  }

  @Override
  public void removeEqualValuesListener(EventDataListener<Set<Entity>> listener) {
    conditionModel.removeEqualValuesListener(listener);
  }

  @Override
  public final void addUpperBoundListener(EventDataListener<Entity> listener) {
    conditionModel.addUpperBoundListener(listener);
  }

  @Override
  public final void removeUpperBoundListener(EventDataListener<Entity> listener) {
    conditionModel.removeUpperBoundListener(listener);
  }

  @Override
  public final void addLowerBoundListener(EventDataListener<Entity> listener) {
    conditionModel.addLowerBoundListener(listener);
  }

  @Override
  public final void removeLowerBoundListener(EventDataListener<Entity> listener) {
    conditionModel.removeLowerBoundListener(listener);
  }

  @Override
  public final void addClearedListener(EventListener listener) {
    conditionModel.addClearedListener(listener);
  }

  @Override
  public final void removeClearedListener(EventListener listener) {
    conditionModel.removeClearedListener(listener);
  }

  @Override
  public final void addChangeListener(EventListener listener) {
    conditionModel.addChangeListener(listener);
  }

  @Override
  public final void removeChangeListener(EventListener listener) {
    conditionModel.removeChangeListener(listener);
  }

  @Override
  public final void addOperatorListener(EventDataListener<Operator> listener) {
    conditionModel.addOperatorListener(listener);
  }

  @Override
  public final void removeOperatorListener(EventDataListener<Operator> listener) {
    conditionModel.removeOperatorListener(listener);
  }
}
