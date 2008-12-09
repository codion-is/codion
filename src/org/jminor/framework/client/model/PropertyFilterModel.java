/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.model.SearchType;
import org.jminor.common.model.Util;
import org.jminor.framework.FrameworkConstants;
import org.jminor.framework.model.Entity;
import org.jminor.framework.model.Property;
import org.jminor.framework.model.Type;

import java.sql.Timestamp;
import java.util.regex.Pattern;

@SuppressWarnings({"unchecked"})
public class PropertyFilterModel extends AbstractSearchModel {

  private final int columnIndex;

  public PropertyFilterModel(final Property property, final int columnIndex) {
    super(property);
    this.columnIndex = columnIndex;
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
    if (!isSearchEnabled())
      return true;

    Comparable toCompare = comparable;
    if (comparable instanceof Timestamp)//ignore seconds and milliseconds
      toCompare = Util.floorLongDate((Timestamp) toCompare);

    switch (getSearchType()) {
      case LIKE:
        return includeExact(toCompare);
      case NOT_LIKE:
        return includeNotExact(toCompare);
      case MAX:
        return includeMax(toCompare);
      case MIN:
        return includeMin(toCompare);
      case INSIDE:
        return includeMinMaxInside(toCompare);
      case OUTSIDE:
        return includeMinMaxOutside(toCompare);
    }

    throw new RuntimeException("Undefined search type: " + getSearchType());
  }

  public void setLikeValue(final Comparable value) {
    setSearchType(SearchType.LIKE);
    setUpperBound(value);
    final boolean on = value != null;
    if (isSearchEnabled() != on)
      setSearchEnabled(on);
    else
      evtUpperBoundChanged.fire();
  }

  protected boolean includeExact(final Comparable comparable) {
    if (getUpperBound() == null)
      return true;

    if (comparable == null)
      return false;

    if (comparable instanceof String) //for Entity and String values
        return includeExactWildcard((String) comparable);

    return comparable.compareTo(getUpperBound()) == 0;
  }

  protected boolean includeNotExact(final Comparable comparable) {
    if (getUpperBound() == null)
      return true;

    if (comparable == null)
      return false;

    if (getPropertyType() == Type.STRING || getPropertyType() == Type.ENTITY)
      return !includeExactWildcard((String) comparable);

    return comparable.compareTo(getUpperBound()) != 0;
  }

  protected boolean includeExactWildcard(final String value) {
    String upperBound = (String) getUpperBound();
    if (upperBound.equals(FrameworkConstants.WILDCARD))
      return true;
    if (value == null)
      return false;

    String realValue = value;
    if (!isCaseSensitive()) {
      upperBound = upperBound.toUpperCase();
      realValue = realValue.toUpperCase();
    }

    if (upperBound.indexOf(FrameworkConstants.WILDCARD) < 0)
      return realValue.compareTo(upperBound) == 0;

    return Pattern.matches(prepareForRegex(upperBound), realValue);
  }

  protected String prepareForRegex(final String string) {
    //a somewhat dirty fix to get rid of the '$' sign from the pattern, since it interferes with the regular expression parsing
    return string.replaceAll(FrameworkConstants.WILDCARD, ".*").replaceAll("\\$", ".").replaceAll("\\]", "\\\\]").replaceAll("\\[", "\\\\[");
  }

  protected boolean includeMax(final Comparable comparable) {
    return getUpperBound() == null || comparable != null && comparable.compareTo(getUpperBound()) <= 0;
  }

  protected boolean includeMin(final Comparable comparable) {
    return getUpperBound() == null || comparable != null && comparable.compareTo(getUpperBound()) >= 0;
  }

  protected boolean includeMinMaxInside(final Comparable comparable) {
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

  protected boolean includeMinMaxOutside(final Comparable comparable) {
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
    if (entity.isValueNull(getPropertyName()))
      return null;

    final Object value = entity.getValue(getPropertyName());
    if (getPropertyType() ==  Type.ENTITY)
      return value.toString();
    else if (getPropertyType() == Type.BOOLEAN)
      return Type.Boolean.get((Type.Boolean) value);
    else
      return (Comparable) value;
  }
}