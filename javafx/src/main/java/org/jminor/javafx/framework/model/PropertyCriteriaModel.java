/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.javafx.framework.model;

import org.jminor.common.db.criteria.Criteria;
import org.jminor.common.model.Event;
import org.jminor.common.model.EventInfoListener;
import org.jminor.common.model.EventListener;
import org.jminor.common.model.EventObserver;
import org.jminor.common.model.Events;
import org.jminor.common.model.Item;
import org.jminor.common.model.SearchType;
import org.jminor.common.model.State;
import org.jminor.common.model.States;
import org.jminor.common.model.Util;
import org.jminor.common.model.Value;
import org.jminor.common.model.Values;
import org.jminor.framework.db.criteria.EntityCriteriaUtil;
import org.jminor.framework.domain.Property;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

public class PropertyCriteriaModel<T extends Property.SearchableProperty> {

  private final T property;

  private final Value upperBound = new CriteriaValue();
  private final Value lowerBound = new CriteriaValue();
  private final Value<SearchType> searchType = Values.value(SearchType.LIKE);
  private final State enabledState = States.state();
  private final State lowerBoundRequiredState = States.state();
  private final Event<String> criteriaStateEvent = Events.event();

  public PropertyCriteriaModel(final T property) {
    this.property = property;
    bindEvents();
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

  public T getProperty() {
    return property;
  }

  public final Criteria<Property.ColumnProperty> getColumnCriteria() {
    return EntityCriteriaUtil.criteria(property.getEntityID(), property.getPropertyID(),
            searchType.get(), getValues());
  }

  public final ObservableList<Item<SearchType>> getSearchTypeList() {
    final ObservableList<Item<SearchType>> items = FXCollections.observableArrayList();
    getSearchTypes(property).forEach(searchType -> items.add(new Item<>(searchType, searchType.getCaption())));

    return items;
  }

  public final boolean isLowerBoundRequired() {
    return lowerBoundRequiredState.isActive();
  }

  public final void addLowerBoundRequiredListener(final EventInfoListener<Boolean> listener) {
    lowerBoundRequiredState.addInfoListener(listener);
  }

  public final void clear() {
    upperBound.set(null);
    lowerBound.set(null);
    searchType.set(SearchType.LIKE);
    enabledState.setActive(false);
  }

  public final String getSearchStateString() {
    final StringBuilder stringBuilder = new StringBuilder(property.getPropertyID());
    if (enabledState.isActive()) {
      stringBuilder.append(searchType.get());
      stringBuilder.append(upperBound.get() != null ? toString(upperBound.get()) : "null");
      stringBuilder.append(lowerBound.get() != null ? toString(lowerBound.get()) : "null");
    }

    return stringBuilder.toString();
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
    final EventListener autoEnableListener = () -> {
      if (searchType.get().getValues().equals(SearchType.Values.TWO)) {
        enabledState.setActive(lowerBound.get() != null && upperBound.get() != null);
      }
      else {
        enabledState.setActive(upperBound.get() != null);
      }
    };
    upperBound.getObserver().addListener(autoEnableListener);
    lowerBound.getObserver().addListener(autoEnableListener);
    final EventListener criteriaStateListener = () -> criteriaStateEvent.fire(getSearchStateString());
    upperBound.getObserver().addListener(criteriaStateListener);
    lowerBound.getObserver().addListener(criteriaStateListener);
    searchType.getObserver().addListener(criteriaStateListener);
    enabledState.getObserver().addListener(criteriaStateListener);
    searchType.getObserver().addListener(() ->
            lowerBoundRequiredState.setActive(searchType.get().getValues().equals(SearchType.Values.TWO)));
  }

  private static Collection<SearchType> getSearchTypes(final Property property) {
    if (property instanceof Property.ForeignKeyProperty) {
      return Arrays.asList(SearchType.LIKE, SearchType.NOT_LIKE);
    }
    else if (property.isBoolean()) {
      return Collections.singleton(SearchType.LIKE);
    }
    else {
      return Arrays.asList(SearchType.values());
    }
  }

  private static String toString(final Object object) {
    final StringBuilder stringBuilder = new StringBuilder();
    if (object instanceof Collection) {
      for (final Object obj : ((Collection) object)) {
        stringBuilder.append(toString(obj));
      }
    }
    else {
      stringBuilder.append(object);
    }

    return stringBuilder.toString();
  }

  private static final class CriteriaValue implements Value<Object> {

    private final Event<Object> changeEvent = Events.event();

    private Object value;

    @Override
    public void set(final Object value) {
      final Object newValue = transformValue(value);
      if (!Util.equal(this.value, newValue)) {
        this.value = newValue;
        changeEvent.fire(this.value);
      }
    }

    @Override
    public Object get() {
      return value;
    }

    @Override
    public EventObserver<Object> getObserver() {
      return changeEvent.getObserver();
    }

    private static Object transformValue(final Object value) {
      if (value instanceof String && ((String) value).isEmpty()) {
        return null;
      }

      return value;
    }
  }
}
