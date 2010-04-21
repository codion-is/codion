/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.model.Event;
import org.jminor.common.model.SearchType;
import org.jminor.common.model.State;
import org.jminor.common.model.Util;
import org.jminor.framework.Configuration;
import org.jminor.framework.domain.Property;
import org.jminor.framework.domain.Type;

import java.sql.Timestamp;
import java.util.Date;

/**
 * Encapsulates the property search parameters search type, upper bound and lower bound,
 * as well as relevant events and states.<br>
 * User: Bjorn Darri<br>
 * Date: 26.12.2007<br>
 * Time: 14:48:22<br>
 */
public abstract class AbstractSearchModel {

  public static final String UPPER_BOUND_PROPERTY = "upperBound";
  public static final String LOWER_BOUND_PROPERTY = "lowerBound";

  private final Event evtUpperBoundChanged = new Event();
  private final Event evtLowerBoundChanged = new Event();
  private final Event evtSearchTypeChanged = new Event();
  private final Event evtSearchStateChanged = new Event();
  private final Event evtSearchModelCleared = new Event();

  private final State stLocked = new State();

  private final State stSearchEnabled = new State();
  private final Property property;

  private SearchType searchType = SearchType.LIKE;
  private boolean automaticWildcard = false;
  private boolean caseSensitive = true;
  private Object upperBound = null;
  private Object lowerBound = null;
  private String wildcard = (String) Configuration.getValue(Configuration.WILDCARD_CHARACTER);

  public AbstractSearchModel(final Property property) {
    if (property == null)
      throw new IllegalArgumentException("Search model requires a non-null property");
    this.property = property;
    bindEvents();
  }

  public Property getProperty() {
    return property;
  }

  public Type getPropertyType() {
    return property.getPropertyType();
  }

  public String getPropertyID() {
    return property.getPropertyID();
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
   * Locks/unlocks this search model, thus preventing changes to either values or search type
   * @param value the value
   */
  public void setLocked(final boolean value) {
    stLocked.setActive(value);
  }

  /**
   * @return true if this filter is be case sensitive
   */
  public boolean isCaseSensitive() {
    return caseSensitive;
  }

  /**
   * @param caseSensitive true if this search model should be case sensitive when working with strings
   */
  public void setCaseSensitive(final boolean caseSensitive) {
    this.caseSensitive = caseSensitive;
  }

  /**
   * @param value the upper bound
   */
  public void setUpperBound(final String value) {
    setUpperBound((Object) value);
  }

  /**
   * @param value the upper bound
   */
  public void setUpperBound(final Double value) {
    setUpperBound((Object) value);
  }

  /**
   * @param value the upper bound
   */
  public void setUpperBound(final Integer value) {
    setUpperBound((Object) value);
  }

  /**
   * @param value the upper bound
   */
  public void setUpperBound(final boolean value) {
    setUpperBound(Boolean.valueOf(value));
  }

  /**
   * @param value the upper bound
   */
  public void setUpperBound(final char value) {
    setUpperBound(Character.valueOf(value));
  }

  /**
   * @param value the upper bound
   */
  public void setUpperBound(final Boolean value) {
    setUpperBound((Object) value);
  }

  /**
   * @param value the upper bound
   */
  public void setUpperBound(final Timestamp value) {
    setUpperBound((Object) value);
  }

  /**
   * @param value the upper bound
   */
  public void setUpperBound(final Date value) {
    setUpperBound((Object) value);
  }

  /**
   * @param upper the upper bound
   * @throws IllegalStateException in case this model has been locked
   */
  public void setUpperBound(final Object upper) {
    if (stLocked.isActive())
      throw new IllegalStateException("Search model for property " + property + " is locked");
    if (!Util.equal(upperBound, upper)) {
      upperBound = upper;
      evtUpperBoundChanged.fire();
    }
  }

  /**
   * @return the upper bound
   */
  public Object getUpperBound() {
    if (getPropertyType() == Type.STRING && automaticWildcard)
      return getWildcard() + upperBound + getWildcard();
    else
      return upperBound;
  }

  /**
   * @param value the lower bound
   */
  public void setLowerBound(final String value) {
    setLowerBound((Object) value);
  }

  /**
   * @param value the lower bound
   */
  public void setLowerBound(final Double value) {
    setLowerBound((Object) value);
  }

  /**
   * @param value the lower bound
   */
  public void setLowerBound(final Integer value) {
    setLowerBound((Object) value);
  }

  /**
   * @param value the lower bound
   */
  public void setLowerBound(final boolean value) {
    setLowerBound(Boolean.valueOf(value));
  }

  /**
   * @param value the lower bound
   */
  public void setLowerBound(final char value) {
    setLowerBound(Character.valueOf(value));
  }

  /**
   * @param value the lower bound
   */
  public void setLowerBound(final Boolean value) {
    setLowerBound((Object) value);
  }

  /**
   * @param value the lower bound
   */
  public void setLowerBound(final Timestamp value) {
    setLowerBound((Object) value);
  }

  /**
   * @param value the lower bound
   */
  public void setLowerBound(final Date value) {
    setLowerBound((Object) value);
  }

  /**
   * @param value the lower bound
   * @throws IllegalStateException in case this model has been locked
   */
  public void setLowerBound(final Object value) {
    if (stLocked.isActive())
      throw new IllegalStateException("Search model for property " + property + " is locked");
    if (!Util.equal(lowerBound, value)) {
      lowerBound = value;
      evtLowerBoundChanged.fire();
    }
  }

  /**
   * @return the lower bound
   */
  public Object getLowerBound() {
    if (getPropertyType() == Type.STRING && automaticWildcard)
      return getWildcard() + lowerBound + getWildcard();
    else
      return lowerBound;
  }

  /**
   * @return the search type
   */
  public SearchType getSearchType() {
    return searchType;
  }

  /**
   * @param type the search type
   * @throws IllegalStateException in case this model has been locked
   */
  public void setSearchType(final SearchType type) {
    if (stLocked.isActive())
      throw new IllegalStateException("Search model for property " + property + " is locked");
    if (type != searchType) {
      searchType = type;
      evtSearchTypeChanged.fire();
    }
  }

  /**
   * @return the wildcard
   */
  public String getWildcard() {
    return wildcard;
  }

  /**
   * Sets the wildcard to use
   * @param wildcard the wildcard
   */
  public void setWildcard(final String wildcard) {
    this.wildcard = wildcard;
  }

  /**
   * @return true if this search model is enabled
   */
  public boolean isSearchEnabled() {
    return stSearchEnabled.isActive();
  }

  /**
   * @param value true if this search model should be enabled
   * @throws IllegalStateException in case this model has been locked
   */
  public void setSearchEnabled(final boolean value) {
    if (stLocked.isActive())
      throw new IllegalStateException("Search model for property " + property + " is locked");
    stSearchEnabled.setActive(value);
  }

  /**
   * @param value true if wildcard should automatically be added to strings
   */
  public void setAutomaticWildcard(final boolean value) {
    automaticWildcard = value;
  }

  public boolean isAutomaticWildcard() {
    return automaticWildcard;
  }

  public void clear() {
    setSearchEnabled(false);
    setUpperBound((Object) null);
    setLowerBound((Object) null);
    setSearchType(SearchType.LIKE);
    evtSearchModelCleared.fire();
  }

  public State stateLocked() {
    return stLocked;
  }

  public State stateSearchEnabled() {
    return stSearchEnabled;
  }

  public Event eventLowerBoundChanged() {
    return evtLowerBoundChanged;
  }

  public Event eventSearchModelCleared() {
    return evtSearchModelCleared;
  }

  public Event eventSearchStateChanged() {
    return evtSearchStateChanged;
  }

  public Event eventSearchTypeChanged() {
    return evtSearchTypeChanged;
  }

  public Event eventUpperBoundChanged() {
    return evtUpperBoundChanged;
  }

  public static int getValueCount(final SearchType searchType) {
    switch(searchType) {
      case LIKE:
      case AT_LEAST:
      case AT_MOST:
      case NOT_LIKE:
        return 1;
      case WITHIN_RANGE:
      case OUTSIDE_RANGE:
        return 2;
    }

    throw new IllegalArgumentException("Undefined search type " + searchType);
  }

  private void bindEvents() {
    evtUpperBoundChanged.addListener(evtSearchStateChanged);
    evtLowerBoundChanged.addListener(evtSearchStateChanged);
    evtSearchTypeChanged.addListener(evtSearchStateChanged);
    stSearchEnabled.eventStateChanged().addListener(evtSearchStateChanged);
  }
}
