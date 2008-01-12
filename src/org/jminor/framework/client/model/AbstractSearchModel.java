/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 *
 */
package org.jminor.framework.client.model;

import org.jminor.common.Constants;
import org.jminor.common.model.Event;
import org.jminor.common.model.SearchType;
import org.jminor.common.model.State;
import org.jminor.common.model.Util;
import org.jminor.framework.model.Type;

import java.sql.Timestamp;

/**
 * User: Bjï¿½rn Darri
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
  public final Event evtSearchModelCleared = new Event("AbstractSearchModel.evtCleared");

  public final State stSearchEnabled = new State("AbstractSearchModel.stSearchEnabled");
  public final State stAutomaticWildcardOn = new State("AbstractSearchModel.stAutomaticWildcardOn");

  private Object upperBound = null;
  private Object lowerBound = null;
  private SearchType searchType = SearchType.EXACT;

  public AbstractSearchModel() {
    bindEvents();
  }

  /**
   * @return Value for property 'columnType'.
   */
  public abstract Type getColumnType();

  /**
   * @return Value for property 'columnName'.
   */
  public abstract String getColumnName();

  /**
   * @return Value for property 'caption'.
   */
  public abstract String getCaption();

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
  public void setUpperBound(final double value) {
    setUpperBound(new Double(value));
  }

  /**
   * @param value Value to set for property 'upperBound'.
   */
  public void setUpperBound(final int value) {
    setUpperBound(new Integer(value));
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
      this.upperBound = upper;
      evtUpperBoundChanged.fire();
    }
  }

  /**
   * @return Value for property 'upperBound'.
   */
  public Object getUpperBound() {
    if (getColumnType() == Type.STRING && stAutomaticWildcardOn.isActive())
      return Constants.WILDCARD + this.upperBound + Constants.WILDCARD;
    else
      return this.upperBound;
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
  public void setLowerBound(final double value) {
    setLowerBound(new Double(value));
  }

  /**
   * @param value Value to set for property 'lowerBound'.
   */
  public void setLowerBound(final int value) {
    setLowerBound(new Integer(value));
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
      this.lowerBound = value;
      evtLowerBoundChanged.fire();
    }
  }

  /**
   * @return Value for property 'lowerBound'.
   */
  public Object getLowerBound() {
    if (getColumnType() == Type.STRING && stAutomaticWildcardOn.isActive())
      return Constants.WILDCARD + this.lowerBound + Constants.WILDCARD;
    else
      return this.lowerBound;
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
    if (type != this.searchType) {
      this.searchType = type;
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
  public void setAutomaticWildcardOn(final boolean value) {
    stAutomaticWildcardOn.setActive(value);
  }

  public void clear() {
    setSearchEnabled(false);
    setUpperBound((Object) null);
    setLowerBound((Object) null);
    setSearchType(SearchType.EXACT);
    evtSearchModelCleared.fire();
  }

  public static int getValueCount(final SearchType searchType) {
    switch(searchType) {
      case EXACT :
      case MAX :
      case MIN :
      case NOT_EXACT :
      case IN_LIST :
        return 1;
      case MIN_MAX_INSIDE :
      case MIN_MAX_OUTSIDE :
        return 2;
    }

    throw new IllegalArgumentException("Undefined search type " + searchType);
  }

  public static boolean containsWildcard(final String value) {
    return value != null && value.length() > 0 && value.indexOf(Constants.WILDCARD) > -1;
  }

  protected void bindEvents() {
    evtUpperBoundChanged.addListener(evtSearchStateChanged);
    evtLowerBoundChanged.addListener(evtSearchStateChanged);
    evtSearchTypeChanged.addListener(evtSearchStateChanged);
    stSearchEnabled.evtStateChanged.addListener(evtSearchStateChanged);
  }
}
