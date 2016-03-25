/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.javafx.framework.model;

import org.jminor.common.db.criteria.Criteria;
import org.jminor.common.db.criteria.CriteriaSet;
import org.jminor.common.db.criteria.CriteriaUtil;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.model.Conjunction;
import org.jminor.common.model.EventListener;
import org.jminor.common.model.State;
import org.jminor.common.model.States;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.db.criteria.EntityCriteriaUtil;
import org.jminor.framework.db.criteria.EntitySelectCriteria;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.Property;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class EntityListCriteriaModel {

  private final String entityID;
  private final EntityConnectionProvider connectionProvider;
  private final Map<Property.SearchableProperty, PropertyCriteriaModel> criteriaModels = new LinkedHashMap<>();
  private final State criteriaStateChangedState = States.state();

  private String rememberedCriteriaState = "";
  private Criteria<Property.ColumnProperty> additionalCriteria;

  public EntityListCriteriaModel(final String entityID, final EntityConnectionProvider connectionProvider) {
    this.entityID = entityID;
    this.connectionProvider = connectionProvider;
    initializePropertyCriteria();
    bindEvents();
  }

  public final PropertyCriteriaModel getPropertyCriteriaModel(final Property.SearchableProperty property) {
    return criteriaModels.get(property);
  }

  public final void rememberCurrentCriteriaState() {
    rememberedCriteriaState = getCriteriaModelState();
    criteriaStateChangedState.setActive(false);
  }

  public void addCriteriaStateListener(final EventListener listener) {
    criteriaStateChangedState.addListener(listener);
  }

  public EntitySelectCriteria getSelectCriteria() {
    final CriteriaSet<Property.ColumnProperty> criteria = CriteriaUtil.criteriaSet(Conjunction.AND);
    criteriaModels.values().stream().filter(model ->
            model.getEnabledState().isActive()).forEach(model -> criteria.add(model.getColumnCriteria()));
    if (criteria.getCriteriaCount() > 0) {
      return EntityCriteriaUtil.selectCriteria(entityID, criteria);
    }
    else {
      return EntityCriteriaUtil.selectCriteria(entityID);
    }
  }

  public void clear() {
    criteriaModels.values().forEach(PropertyCriteriaModel::clear);
  }

  public void filterBy(final Property.ForeignKeyProperty foreignKeyProperty, final List<Entity> entities)
          throws DatabaseException {
    Objects.requireNonNull(foreignKeyProperty);
    Objects.requireNonNull(entities);
    clear();
    final ForeignKeyCriteriaModel criteriaModel = (ForeignKeyCriteriaModel) criteriaModels.get(foreignKeyProperty);
    if (criteriaModel == null) {
      throw new IllegalArgumentException("Criteria model not found for property: " + foreignKeyProperty);
    }
    criteriaModel.setCriteria(entities);
  }

  private void initializePropertyCriteria() {
    Entities.getColumnProperties(entityID).stream().filter(columnProperty ->
            !columnProperty.isForeignKeyProperty() && !columnProperty.isAggregateColumn()).forEach(columnProperty ->
            criteriaModels.put(columnProperty, new PropertyCriteriaModel<>(columnProperty)));
    for (final Property.ForeignKeyProperty foreignKeyProperty : Entities.getForeignKeyProperties(entityID)) {
      criteriaModels.put(foreignKeyProperty, new ForeignKeyCriteriaModel(foreignKeyProperty, connectionProvider));
    }
  }

  private String getCriteriaModelState() {
    final StringBuilder stringBuilder = new StringBuilder();
    for (final PropertyCriteriaModel model : criteriaModels.values()) {
      stringBuilder.append(model.getSearchStateString());
    }

    return stringBuilder.toString();
  }

  private void bindEvents() {
    final EventListener listener = () -> {
      criteriaStateChangedState.setActive(!rememberedCriteriaState.equals(getCriteriaModelState()));
      //todo necessary?
//      criteriaStateChangedEvent.fire();
    };
    for (final PropertyCriteriaModel model : criteriaModels.values()) {
      model.addCriteriaStateListener(listener);
    }
  }
}
