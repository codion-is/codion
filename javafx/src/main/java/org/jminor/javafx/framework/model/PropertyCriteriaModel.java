/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.javafx.framework.model;

import org.jminor.common.db.criteria.Criteria;
import org.jminor.common.model.Event;
import org.jminor.common.model.EventListener;
import org.jminor.common.model.Events;
import org.jminor.common.model.SearchType;
import org.jminor.common.model.State;
import org.jminor.common.model.States;
import org.jminor.common.model.Util;
import org.jminor.common.model.Value;
import org.jminor.common.model.Values;
import org.jminor.framework.db.criteria.EntityCriteriaUtil;
import org.jminor.framework.domain.Property;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

public class PropertyCriteriaModel<T extends Property.SearchableProperty> {

  private final T property;

  private final Value upperBound = Values.value();
  private final Value lowerBound = Values.value();
  private final Value<SearchType> searchType = Values.value(SearchType.LIKE);
  private final State enabledState = States.state();
  private final Event<String> criteriaStateEvent = Events.event();

  public PropertyCriteriaModel(final T property) {
    this.property = property;
    bindEvents();
  }

  public T getProperty() {
    return property;
  }

  public final Value getUpperBoundValue() {
    return upperBound;
  }

  public final Value getLowerBoundValue() {
    return lowerBound;
  }

  public final Value<SearchType> getSearchTypeValue() {
    return searchType;
  }

  public final State getEnabledState() {
    return enabledState;
  }

  public void addCriteriaStateListener(final EventListener listener) {
    criteriaStateEvent.addListener(listener);
  }

  public final Criteria<Property.ColumnProperty> getColumnCriteria() {
    return EntityCriteriaUtil.criteria(property.getEntityID(), property.getPropertyID(),
            searchType.get(), getValues());
  }

  public void clear() {
    upperBound.set(null);
    lowerBound.set(null);
    searchType.set(SearchType.LIKE);
    enabledState.setActive(false);
  }

  public final String getSearchStateString() {
    final StringBuilder builder = new StringBuilder(enabledState.isActive() ? "enabled" : "disabled")
            .append(searchType.get().toString());
    final Object upper = upperBound.get();
    if (upper instanceof Collection) {
      builder.append(Util.getCollectionContentsAsString((Collection) upper, false));
    }
    else {
      builder.append(upper == null ? "null" : upper.toString());
    }
    final Object lower = lowerBound.get();
    if (lower instanceof Collection) {
      builder.append(Util.getCollectionContentsAsString((Collection) lower, false));
    }
    else {
      builder.append(lower == null ? "null" : lower.toString());
    }

    return builder.toString();
  }

  private Collection getValues() {
    if (searchType.get().getValues().equals(SearchType.Values.TWO)) {
      return Arrays.asList(lowerBound.get(), upperBound.get());
    }
    else if (upperBound.get() instanceof Collection){
      return (Collection) upperBound.get();
    }
    else {
      return Collections.singletonList(upperBound.get());
    }
  }

  private void bindEvents() {
    final EventListener criteriaStateListener = () -> criteriaStateEvent.fire(getSearchStateString());
    upperBound.getObserver().addListener(criteriaStateListener);
    lowerBound.getObserver().addListener(criteriaStateListener);
    searchType.getObserver().addListener(criteriaStateListener);
    enabledState.getObserver().addListener(criteriaStateListener);
  }
}
