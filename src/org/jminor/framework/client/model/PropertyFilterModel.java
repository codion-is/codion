/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.model.AbstractSearchModel;
import org.jminor.common.model.DateUtil;
import org.jminor.common.model.SearchType;
import org.jminor.framework.Configuration;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.Property;

import java.sql.Timestamp;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * A class for filtering a set of entities based on a property.
 */
@SuppressWarnings({"unchecked"})
public class PropertyFilterModel extends AbstractSearchModel<Property> {

  public PropertyFilterModel(final Property property) {
    super(property, property.getType(), (String) Configuration.getValue(Configuration.WILDCARD_CHARACTER));
  }

  /** {@inheritDoc} */
  @Override
  public boolean include(final Object object) {
    return include(getComparable(object));
  }

  public boolean include(final Comparable comparable) {
    if (!isSearchEnabled()) {
      return true;
    }

    Comparable toCompare = comparable;
    if (comparable instanceof Timestamp) {//ignore seconds and milliseconds
      toCompare = DateUtil.floorTimestamp((Timestamp) toCompare);
    }

    switch (getSearchType()) {
      case LIKE:
        return includeLike(toCompare);
      case NOT_LIKE:
        return includeNotLike(toCompare);
      case AT_LEAST:
        return includeMax(toCompare);
      case AT_MOST:
        return includeMin(toCompare);
      case WITHIN_RANGE:
        return includeMinMaxInside(toCompare);
      case OUTSIDE_RANGE:
        return includeMinMaxOutside(toCompare);
    }

    throw new RuntimeException("Undefined search type: " + getSearchType());
  }

  public void setLikeValue(final Comparable value) {
    setSearchType(SearchType.LIKE);
    setUpperBound(value);
    final boolean on = value != null;
    if (isSearchEnabled() != on) {
      setSearchEnabled(on);
    }
    else {
      eventUpperBoundChanged().fire();
    }
  }

  protected boolean includeLike(final Comparable comparable) {
    if (getUpperBound() == null) {
      return true;
    }

    if (comparable == null) {
      return false;
    }

    if (comparable instanceof String) {//for Entity and String values
      return includeExactWildcard((String) comparable);
    }

    return comparable.compareTo(getUpperBound()) == 0;
  }

  protected boolean includeNotLike(final Comparable comparable) {
    if (getUpperBound() == null) {
      return true;
    }

    if (comparable == null) {
      return false;
    }

    final Property property = getSearchProperty();
    if (property.isString() || property.isReference()) {
      return !includeExactWildcard((String) comparable);
    }

    return comparable.compareTo(getUpperBound()) != 0;
  }

  protected boolean includeExactWildcard(final String value) {
    String upperBound = (String) getUpperBound();
    if (upperBound.equals(getWildcard())) {
      return true;
    }
    if (value == null) {
      return false;
    }

    String realValue = value;
    if (!isCaseSensitive()) {
      upperBound = upperBound.toUpperCase(Locale.getDefault());
      realValue = realValue.toUpperCase(Locale.getDefault());
    }

    if (upperBound.indexOf(getWildcard()) < 0) {
      return realValue.compareTo(upperBound) == 0;
    }

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
    if (getLowerBound() == null && getUpperBound() == null) {
      return true;
    }

    if (comparable == null) {
      return false;
    }

    if (getLowerBound() == null) {
      return comparable.compareTo(getUpperBound()) <= 0;
    }

    if (getUpperBound() == null) {
      return comparable.compareTo(getLowerBound()) >= 0;
    }

    final int lowerCompareResult = comparable.compareTo(getLowerBound());
    final int upperCompareResult = comparable.compareTo(getUpperBound());

    return lowerCompareResult >= 0 && upperCompareResult <= 0;
  }

  protected boolean includeMinMaxOutside(final Comparable comparable) {
    if (getLowerBound() == null && getUpperBound() == null) {
      return true;
    }

    if (comparable == null) {
      return false;
    }

    if (getLowerBound() == null) {
      return comparable.compareTo(getUpperBound()) >= 0;
    }

    if (getUpperBound() == null) {
      return comparable.compareTo(getLowerBound()) <= 0;
    }

    final int lowerCompareResult = comparable.compareTo(getLowerBound());
    final int upperCompareResult = comparable.compareTo(getUpperBound());

    return lowerCompareResult <= 0 || upperCompareResult >= 0;
  }

  protected Comparable getComparable(final Object object) {
    final Entity entity = (Entity) object;
    if (entity.isValueNull(getSearchProperty().getPropertyID())) {
      return null;
    }

    final Object value = entity.getValue(getSearchProperty().getPropertyID());
    if (getSearchProperty().isReference()) {
      return value.toString();
    }
    else {
      return (Comparable) value;
    }
  }
}