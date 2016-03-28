/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.javafx.framework.model;

import org.jminor.common.db.criteria.Criteria;
import org.jminor.common.db.criteria.CriteriaSet;
import org.jminor.common.db.criteria.CriteriaUtil;
import org.jminor.common.model.Conjunction;
import org.jminor.common.model.EventListener;
import org.jminor.common.model.State;
import org.jminor.common.model.StateObserver;
import org.jminor.common.model.States;
import org.jminor.common.model.Util;
import org.jminor.common.model.table.ColumnCriteriaModel;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Property;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public final class EntityListCriteriaModel {

  private final String entityID;
  private final EntityConnectionProvider connectionProvider;
  private final Map<String, ColumnCriteriaModel<? extends Property.SearchableProperty>> criteriaModels = new LinkedHashMap<>();
  private final State criteriaStateChangedState = States.state();

  private String rememberedCriteriaState = "";
  private Criteria<Property.ColumnProperty> additionalCriteria;

  public EntityListCriteriaModel(final String entityID, final EntityConnectionProvider connectionProvider) {
    this.entityID = entityID;
    this.connectionProvider = connectionProvider;
    initializePropertyCriteria();
    bindEvents();
  }

  public ColumnCriteriaModel<? extends Property.SearchableProperty> getPropertyCriteriaModel(final String propertyID) {
    return criteriaModels.get(propertyID);
  }

  public void rememberCurrentCriteriaState() {
    rememberedCriteriaState = getCriteriaModelState();
    criteriaStateChangedState.setActive(false);
  }

  public StateObserver getCriteriaStateChangedObserver() {
    return criteriaStateChangedState.getObserver();
  }

  public final Criteria<Property.ColumnProperty> getTableCriteria() {
    final CriteriaSet<Property.ColumnProperty> criteriaSet = CriteriaUtil.criteriaSet(Conjunction.AND);
    criteriaModels.values().stream().filter(ColumnCriteriaModel::isEnabled).forEach(model -> {
      if (model instanceof PropertyCriteriaModel) {
        criteriaSet.add(((PropertyCriteriaModel) model).getCriteria());
      }
      else if (model instanceof ForeignKeyCriteriaModel) {
        criteriaSet.add(((ForeignKeyCriteriaModel) model).getCriteria());
      }
    });
    if (additionalCriteria != null) {
      criteriaSet.add(additionalCriteria);
    }

    return criteriaSet.getCriteriaCount() > 0 ? criteriaSet : null;
  }

  public void clear() {
    criteriaModels.values().forEach(ColumnCriteriaModel::clearCriteria);
  }

  public final boolean setCriteriaValues(final String propertyID, final Collection<?> values) {
    final String criteriaState = getCriteriaModelState();
    if (criteriaModels.containsKey(propertyID)) {
      final ColumnCriteriaModel criteriaModel = criteriaModels.get(propertyID);
      criteriaModel.setEnabled(!Util.nullOrEmpty(values));
      criteriaModel.setUpperBound((Object) null);//because the upperBound could be a reference to the active entity which changes accordingly
      criteriaModel.setUpperBound(values != null && values.isEmpty() ? null : values);//this then fails to register a changed upper bound
    }
    return !criteriaState.equals(rememberedCriteriaState);
  }

  private void initializePropertyCriteria() {
    Entities.getColumnProperties(entityID).stream().filter(columnProperty ->
            !columnProperty.isForeignKeyProperty() && !columnProperty.isAggregateColumn()).forEach(columnProperty ->
            criteriaModels.put(columnProperty.getPropertyID(), new PropertyCriteriaModel(columnProperty)));
    for (final Property.ForeignKeyProperty foreignKeyProperty : Entities.getForeignKeyProperties(entityID)) {
      criteriaModels.put(foreignKeyProperty.getPropertyID(), new ForeignKeyCriteriaModel(foreignKeyProperty, connectionProvider));
    }
  }

  private String getCriteriaModelState() {
    final StringBuilder stringBuilder = new StringBuilder();
    for (final ColumnCriteriaModel model : criteriaModels.values()) {
      stringBuilder.append(model.toString());
    }

    return stringBuilder.toString();
  }

  private void bindEvents() {
    final EventListener listener = () -> {
      criteriaStateChangedState.setActive(!rememberedCriteriaState.equals(getCriteriaModelState()));
      //todo necessary?
//      criteriaStateChangedEvent.fire();
    };
    for (final ColumnCriteriaModel model : criteriaModels.values()) {
      model.addCriteriaStateListener(listener);
    }
  }
}
