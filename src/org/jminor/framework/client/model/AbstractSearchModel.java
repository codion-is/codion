/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.model.Event;
import org.jminor.common.model.SearchType;
import org.jminor.common.model.State;
import org.jminor.common.model.Util;
import org.jminor.framework.FrameworkConstants;
import org.jminor.framework.model.Property;
import org.jminor.framework.model.Type;

import java.sql.Timestamp;

/**
 * User: Björn Darri
 * Date: 26.12.2007
 * Time: 14:48:22
 */
public abstract class AbstractSearchModel {

  public static final String UPPER_BOUND_PROPERTY = "upperBound";
  public static final String LOWER_BOUND_PROPERTY = "lowerBound";

  public final Event evtUpperBoundChanged = new Event("AbstractSearchModel.evtUpperBoundChanged");
  public final Event evtLowerBoundChanged = new Event("AbstractSearchModel.evtLowerBoundChanged");
  public final Event evtSearchTypeChanged = new Event("AbstractSearchModel.evtSearchTypeChanged");
  public final Event evtSearchStateChanged = new Event("AbstractSearchModel.evtSearchStateChanged");
  public final Event evtSearchModelCleared = new Event("AbstractSearchModel.evtSearchModelCleared");

  public final State stSearchEnabled = new State("AbstractSearchModel.stSearchEnabled");

  private final Property property;

  private SearchType searchType = SearchType.LIKE;
  private boolean automaticWildcard = false;
  private Object upperBound = null;
  private Object lowerBound = null;

  public AbstractSearchModel(final Property property) {
    this.property = property;
    bindEvents();
  }

  public Property getProperty() {
    return property;
  }

  public Type getPropertyType() {
    return property.getPropertyType();
  }

  public String getPropertyName() {
    return property.propertyID;
  }

  public String getCaption() {
    return property.getCaption();
  }

  /**
   * @param object the object
   * @return true if the object should be included
   */
  public abstract boolean include(final Object object);

  /**
   * @param value Value to set for property 'upperBound'.
   */
  public void setUpperBound(final String value) {
    setUpperBound((Object) value);
  }

  /**
   * @param value Value to set for property 'upperBound'.
   */
  public void setUpperBound(final Double value) {
    setUpperBound((Object) value);
  }

  /**
   * @param value Value to set for property 'upperBound'.
   */
  public void setUpperBound(final Integer value) {
    setUpperBound((Object) value);
  }

  /**
   * @param value Value to set for property 'upperBound'.
   */
  public void setUpperBound(final boolean value) {
    setUpperBound(Boolean.valueOf(value));
  }

  /**
   * @param value Value to set for property 'upperBound'.
   */
  public void setUpperBound(final char value) {
    setUpperBound(Character.valueOf(value));
  }

  /**
   * @param value Value to set for property 'upperBound'.
   */
  public void setUpperBound(final Boolean value) {
    setUpperBound((Object) value);
  }

  /**
   * @param value Value to set for property 'upperBound'.
   */
  public void setUpperBound(final Timestamp value) {
    setUpperBound((Object) value);
  }

  /**
   * @param upper Value to set for property 'upperBound'.
   */
  public void setUpperBound(final Object upper) {
    if (!Util.equal(upperBound, upper)) {
      upperBound = upper;
      evtUpperBoundChanged.fire();
    }
  }

  /**
   * @return Value for property 'upperBound'.
   */
  public Object getUpperBound() {
    if (getPropertyType() == Type.STRING && automaticWildcard)
      return FrameworkConstants.WILDCARD + upperBound + FrameworkConstants.WILDCARD;
    else
      return upperBound;
  }

  /**
   * @param value Value to set for property 'lowerBound'.
   */
  public void setLowerBound(final String value) {
    setLowerBound((Object) value);
  }

  /**
   * @param value Value to set for property 'lowerBound'.
   */
  public void setLowerBound(final Double value) {
    setLowerBound((Object) value);
  }

  /**
   * @param value Value to set for property 'lowerBound'.
   */
  public void setLowerBound(final Integer value) {
    setLowerBound((Object) value);
  }

  /**
   * @param value Value to set for property 'lowerBound'.
   */
  public void setLowerBound(final boolean value) {
    setLowerBound(Boolean.valueOf(value));
  }

  /**
   * @param value Value to set for property 'lowerBound'.
   */
  public void setLowerBound(final char value) {
    setLowerBound(Character.valueOf(value));
  }

  /**
   * @param value Value to set for property 'lowerBound'.
   */
  public void setLowerBound(final Boolean value) {
    setLowerBound((Object) value);
  }

  /**
   * @param value Value to set for property 'lowerBound'.
   */
  public void setLowerBound(final Timestamp value) {
    setLowerBound((Object) value);
  }

  /**
   * @param value Value to set for property 'lowerBound'.
   */
  public void setLowerBound(final Object value) {
    if (!Util.equal(lowerBound, value)) {
      lowerBound = value;
      evtLowerBoundChanged.fire();
    }
  }

  /**
   * @return Value for property 'lowerBound'.
   */
  public Object getLowerBound() {
    if (getPropertyType() == Type.STRING && automaticWildcard)
      return FrameworkConstants.WILDCARD + lowerBound + FrameworkConstants.WILDCARD;
    else
      return lowerBound;
  }

  /**
   * @return Value for property 'searchType'.
   */
  public SearchType getSearchType() {
    return searchType;
  }

  /**
   * @param type Value to set for property 'searchType'.
   */
  public void setSearchType(final SearchType type) {
    if (type != searchType) {
      searchType = type;
      evtSearchTypeChanged.fire();
    }
  }

  /**
   * @return Value for property 'searchEnabled'.
   */
  public boolean isSearchEnabled() {
    return stSearchEnabled.isActive();
  }

  /**
   * @param value Value to set for property 'searchEnabled'.
   */
  public void setSearchEnabled(final boolean value) {
    stSearchEnabled.setActive(value);
  }

  /**
   * @param value Value to set for property 'automaticWildcardOn'.
   */
  public void setAutomaticWildcard(final boolean value) {
    automaticWildcard = value;
  }

  public void clear() {
    setSearchEnabled(false);
    setUpperBound((Object) null);
    setLowerBound((Object) null);
    setSearchType(SearchType.LIKE);
    evtSearchModelCleared.fire();
  }

  public static int getValueCount(final SearchType searchType) {
    switch(searchType) {
      case LIKE:
      case MAX :
      case MIN :
      case NOT_LIKE:
      case IN:
        return 1;
      case INSIDE:
      case OUTSIDE:
        return 2;
    }

    throw new IllegalArgumentException("Undefined search type " + searchType);
  }

  protected void bindEvents() {
    evtUpperBoundChanged.addListener(evtSearchStateChanged);
    evtLowerBoundChanged.addListener(evtSearchStateChanged);
    evtSearchTypeChanged.addListener(evtSearchStateChanged);
    stSearchEnabled.evtStateChanged.addListener(evtSearchStateChanged);
  }
}
