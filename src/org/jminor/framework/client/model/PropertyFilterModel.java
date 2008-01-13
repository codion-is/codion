/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.Constants;
import org.jminor.common.model.SearchType;
import org.jminor.common.model.Util;
import org.jminor.framework.model.Entity;
import org.jminor.framework.model.Property;
import org.jminor.framework.model.Type;

import java.sql.Timestamp;
import java.util.regex.Pattern;

@SuppressWarnings({"unchecked"})
public class PropertyFilterModel extends AbstractSearchModel {

  private final Property property;
  private final int columnIndex;

  public PropertyFilterModel(final Property property, final int columnIndex) {
    this.property = property;
    this.columnIndex = columnIndex;
  }

  /** {@inheritDoc} */
  public Type getColumnType() {
    return property.getPropertyType();
  }

  /** {@inheritDoc} */
  public String getColumnName() {
    return property.propertyID;
  }

  /** {@inheritDoc} */
  public String getCaption() {
    return property.getCaption();
  }

  /**
   * @return Value for property 'columnIndex'.
   */
  public int getColumnIndex() {
    return columnIndex;
  }

  /**
   * @param value Value to set for property 'upperBound'.
   */
  public void setUpperBound(final Comparable value) {
    super.setUpperBound(value);
  }

  /** {@inheritDoc} */
  public Comparable getUpperBound() {
    return (Comparable) super.getUpperBound();
  }

  /**
   * @param value Value to set for property 'lowerBound'.
   */
  public void setLowerBound(final Comparable value) {
    super.setLowerBound(value);
  }

  /**
   * @return Value for property 'lowerBound'.
   */
  public Comparable getLowerBound() {
    return (Comparable) super.getLowerBound();
  }

  /** {@inheritDoc} */
  public boolean include(final Object object) {
    return include(getComparable(object));
  }

  public boolean include(final Comparable comparable) {
    if (!stSearchEnabled.isActive())
      return true;

    Comparable realComp = comparable;
    if (comparable instanceof Timestamp)//ignore seconds and milliseconds
      realComp = Util.floorLongDate((Timestamp) realComp);

    switch (getSearchType()) {
      case LIKE:
        return acceptExact(realComp);
      case NOT_LIKE:
        return acceptNotExact(realComp);
      case MAX:
        return acceptMax(realComp);
      case MIN:
        return acceptMin(realComp);
      case INSIDE:
        return acceptMinMaxInside(realComp);
      case OUTSIDE:
        return acceptMinMaxOutside(realComp);
    }

    throw new RuntimeException("Undefined search type: " + getSearchType());
  }

  /**
   * @param value Value to set for property 'exactValue'.
   */
  public void setExactValue(final Comparable value) {
    setSearchType(SearchType.LIKE);
    setUpperBound(value);
    final boolean on = value != null;
    if (stSearchEnabled.isActive() != on)
      stSearchEnabled.setActive(on);
    else
      evtUpperBoundChanged.fire();
  }

  /** {@inheritDoc} */
  public String toString() {
    return property.toString();
  }

  protected boolean acceptExact(final Comparable comparable) {
    if (getUpperBound() == null)
      return true;

    if (comparable == null)
      return false;

    if (comparable instanceof String) //for Entity and String values
        return acceptExactWildcard((String) comparable, true);

    return comparable.compareTo(getUpperBound()) == 0;
  }

  protected boolean acceptNotExact(final Comparable comparable) {
    if (getUpperBound() == null)
      return true;

    if (comparable == null)
      return false;

    if (getColumnType() == Type.STRING || getColumnType() == Type.ENTITY)
      return !acceptExactWildcard((String) comparable, true);

    return comparable.compareTo(getUpperBound()) != 0;
  }

  protected boolean acceptExactWildcard(final String value, final boolean caseSensitive) {
    String upperBound = (String) getUpperBound();
    if (upperBound.equals(Constants.WILDCARD))
      return true;
    if (value == null)
      return false;

    String realValue = value;
    if (!caseSensitive) {
      upperBound = upperBound.toUpperCase();
      realValue = realValue.toUpperCase();
    }

    if (upperBound.indexOf(Constants.WILDCARD) < 0)
      return realValue.compareTo(upperBound) == 0;

    return Pattern.matches(prepareForRegex(upperBound), realValue);
  }

  protected String prepareForRegex(final String string) {
    //a somewhat dirty fix to get rid of the '$' sign from the pattern, since it interferes with the regular expression parsing
    return string.replaceAll(Constants.WILDCARD, ".*").replaceAll("\\$", ".").replaceAll("\\]", "\\\\]").replaceAll("\\[", "\\\\[");
  }

  protected boolean acceptMax(final Comparable comparable) {
    return getUpperBound() == null || comparable != null && comparable.compareTo(getUpperBound()) <= 0;
  }

  protected boolean acceptMin(final Comparable comparable) {
    return getUpperBound() == null || comparable != null && comparable.compareTo(getUpperBound()) >= 0;
  }

  protected boolean acceptMinMaxInside(final Comparable comparable) {
    if (getLowerBound() == null && getUpperBound() == null)
      return true;

    if (comparable == null)
      return false;

    if (getLowerBound() == null)
      return comparable.compareTo(getUpperBound()) <= 0;

    if (getUpperBound() == null)
      return comparable.compareTo(getLowerBound()) >= 0;

    final int lowerCompareResult = comparable.compareTo(getLowerBound());
    final int upperCompareResult = comparable.compareTo(getUpperBound());

    return lowerCompareResult >= 0 && upperCompareResult <= 0;
  }

  protected boolean acceptMinMaxOutside(final Comparable comparable) {
    if (getLowerBound() == null && getUpperBound() == null)
      return true;

    if (comparable == null)
      return false;

    if (getLowerBound() == null)
      return comparable.compareTo(getUpperBound()) >= 0;

    if (getUpperBound() == null)
      return comparable.compareTo(getLowerBound()) <= 0;

    final int lowerCompareResult = comparable.compareTo(getLowerBound());
    final int upperCompareResult = comparable.compareTo(getUpperBound());

    return lowerCompareResult <= 0 || upperCompareResult >= 0;
  }

  protected Comparable getComparable(final Object object) {
    final Entity entity = (Entity) object;
    if (entity.isValueNull(getColumnName()))
      return null;

    final Object value = entity.getValue(getColumnName());
    if (value instanceof Entity)
      return value.toString();
    else
      return (Comparable) value;
  }
}