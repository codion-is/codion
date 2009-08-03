/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.model.SearchType;
import org.jminor.framework.DateUtil;
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
   * @return the index of the column this filter model filters
   */
  public int getColumnIndex() {
    return columnIndex;
  }

  /** {@inheritDoc} */
  @Override
  public boolean include(final Object object) {
    return include(getComparable(object));
  }

  public boolean include(final Comparable comparable) {
    if (!isSearchEnabled())
      return true;

    Comparable toCompare = comparable;
    if (comparable instanceof Timestamp)//ignore seconds and milliseconds
      toCompare = DateUtil.floorLongDate((Timestamp) toCompare);

    switch (getSearchType()) {
      case LIKE:
        return includeLike(toCompare);
      case NOT_LIKE:
        return includeNotLike(toCompare);
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

  protected boolean includeLike(final Comparable comparable) {
    if (getUpperBound() == null)
      return true;

    if (comparable == null)
      return false;

    if (comparable instanceof String) //for Entity and String values
        return includeExactWildcard((String) comparable);

    return comparable.compareTo(getUpperBound()) == 0;
  }

  protected boolean includeNotLike(final Comparable comparable) {
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
    if (upperBound.equals(getWildcard()))
      return true;
    if (value == null)
      return false;

    String realValue = value;
    if (!isCaseSensitive()) {
      upperBound = upperBound.toUpperCase();
      realValue = realValue.toUpperCase();
    }

    if (upperBound.indexOf(getWildcard()) < 0)
      return realValue.compareTo(upperBound) == 0;

    return Pattern.matches(prepareForRegex(upperBound), realValue);
  }

  protected String prepareForRegex(final String string) {
    //a somewhat dirty fix to get rid of the '$' sign from the pattern, since it interferes with the regular expression parsing
    return string.replaceAll(getWildcard(), ".*").replaceAll("\\$", ".").replaceAll("\\]", "\\\\]").replaceAll("\\[", "\\\\[");
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