/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.model.table;

import java.text.Format;
import java.util.Locale;
import java.util.function.Function;
import java.util.regex.Pattern;

import static java.util.Objects.requireNonNull;

/**
 * A default ColumnFilterModel model implementation.
 * @param <R> the type of the rows
 * @param <C> the type of the column identifier
 * @param <T> the column value type
 */
public final class DefaultColumnFilterModel<R, C, T> extends DefaultColumnConditionModel<C, T> implements ColumnFilterModel<R, C, T> {

  private Function<R, Comparable<T>> comparableFunction = value -> (Comparable<T>) value;

  /**
   * Instantiates a DefaultColumnFilterModel.
   * @param columnIdentifier the column identifier
   * @param typeClass the data type
   * @param wildcard the string to use as wildcard
   */
  public DefaultColumnFilterModel(final C columnIdentifier, final Class<T> typeClass, final String wildcard) {
    this(columnIdentifier, typeClass, wildcard, null, null);
  }

  /**
   * Instantiates a DefaultColumnFilterModel.
   * @param columnIdentifier the column identifier
   * @param typeClass the data type
   * @param wildcard the string to use as wildcard
   * @param format the format to use when presenting the values, numbers for example
   * @param dateTimePattern the date/time format pattern to use in case of a date/time column
   */
  public DefaultColumnFilterModel(final C columnIdentifier, final Class<T> typeClass, final String wildcard,
                                  final Format format, final String dateTimePattern) {
    super(columnIdentifier, typeClass, wildcard, format, dateTimePattern, AUTOMATIC_WILDCARD.get());
  }

  @Override
  public void setComparableFunction(final Function<R, Comparable<T>> comparableFunction) {
    this.comparableFunction = requireNonNull(comparableFunction);
  }

  @Override
  public boolean include(final R row) {
    return !isEnabled() || include(comparableFunction.apply(row));
  }

  boolean include(final Comparable<T> comparable) {
    if (!isEnabled()) {
      return true;
    }

    switch (getOperator()) {
      case EQUAL:
        return includeEqual(comparable);
      case NOT_EQUAL:
        return includeNotEqual(comparable);
      case LESS_THAN:
        return includeLessThan(comparable);
      case LESS_THAN_OR_EQUAL:
        return includeLessThanOrEqual(comparable);
      case GREATER_THAN:
        return includeGreaterThan(comparable);
      case GREATER_THAN_OR_EQUAL:
        return includeGreaterThanOrEqual(comparable);
      case BETWEEN_EXCLUSIVE:
        return includeBetweenExclusive(comparable);
      case BETWEEN:
        return includeBetweenInclusive(comparable);
      case NOT_BETWEEN_EXCLUSIVE:
        return includeNotBetweenExclusive(comparable);
      case NOT_BETWEEN:
        return includeNotBetween(comparable);
      default:
        throw new IllegalArgumentException("Undefined operator: " + getOperator());
    }
  }

  private boolean includeEqual(final Comparable<T> comparable) {
    final T equalValue = getEqualValue();
    if (comparable == null) {
      return equalValue == null;
    }
    if (equalValue == null) {
      return comparable == null;
    }

    if (comparable instanceof String) {//for String values
      return includeExactWildcard((String) comparable);
    }

    return comparable.compareTo(equalValue) == 0;
  }

  private boolean includeNotEqual(final Comparable<T> comparable) {
    final T equalValue = getEqualValue();
    if (comparable == null) {
      return equalValue != null;
    }
    if (equalValue == null) {
      return comparable != null;
    }

    if (comparable instanceof String && ((String) comparable).contains(getWildcard())) {
      return !includeExactWildcard((String) comparable);
    }

    return comparable.compareTo(equalValue) != 0;
  }

  private boolean includeExactWildcard(final String value) {
    String equalsValue = (String) getEqualValue();
    if (equalsValue == null) {
      equalsValue = "";
    }
    if (equalsValue.equals(getWildcard())) {
      return true;
    }
    if (value == null) {
      return false;
    }

    String realValue = value;
    if (!isCaseSensitive()) {
      equalsValue = equalsValue.toUpperCase(Locale.getDefault());
      realValue = realValue.toUpperCase(Locale.getDefault());
    }

    if (!equalsValue.contains(getWildcard())) {
      return realValue.compareTo(equalsValue) == 0;
    }

    return Pattern.matches(prepareForRegex(equalsValue), realValue);
  }

  private String prepareForRegex(final String string) {
    //a somewhat dirty fix to get rid of the '$' sign from the pattern, since it interferes with the regular expression parsing
    return string.replace(getWildcard(), ".*").replace("\\$", ".").replace("]", "\\\\]").replace("\\[", "\\\\[");
  }

  private boolean includeLessThan(final Comparable<T> comparable) {
    final T upperBound = getUpperBound();

    return upperBound == null || comparable != null && comparable.compareTo(upperBound) < 0;
  }

  private boolean includeLessThanOrEqual(final Comparable<T> comparable) {
    final T upperBound = getUpperBound();

    return upperBound == null || comparable != null && comparable.compareTo(upperBound) <= 0;
  }

  private boolean includeGreaterThan(final Comparable<T> comparable) {
    final T lowerBound = getLowerBound();

    return lowerBound == null || comparable != null && comparable.compareTo(lowerBound) > 0;
  }

  private boolean includeGreaterThanOrEqual(final Comparable<T> comparable) {
    final T lowerBound = getLowerBound();

    return lowerBound == null || comparable != null && comparable.compareTo(lowerBound) >= 0;
  }

  private boolean includeBetweenExclusive(final Comparable<T> comparable) {
    final T lowerBound = getLowerBound();
    final T upperBound = getUpperBound();
    if (lowerBound == null && upperBound == null) {
      return true;
    }

    if (comparable == null) {
      return false;
    }

    if (lowerBound == null) {
      return comparable.compareTo(upperBound) < 0;
    }

    if (upperBound == null) {
      return comparable.compareTo(lowerBound) > 0;
    }

    final int lowerCompareResult = comparable.compareTo(lowerBound);
    final int upperCompareResult = comparable.compareTo(upperBound);

    return lowerCompareResult > 0 && upperCompareResult < 0;
  }

  private boolean includeBetweenInclusive(final Comparable<T> comparable) {
    final T lowerBound = getLowerBound();
    final T upperBound = getUpperBound();
    if (lowerBound == null && upperBound == null) {
      return true;
    }

    if (comparable == null) {
      return false;
    }

    if (lowerBound == null) {
      return comparable.compareTo(upperBound) <= 0;
    }

    if (upperBound == null) {
      return comparable.compareTo(lowerBound) >= 0;
    }

    final int lowerCompareResult = comparable.compareTo(lowerBound);
    final int upperCompareResult = comparable.compareTo(upperBound);

    return lowerCompareResult >= 0 && upperCompareResult <= 0;
  }

  private boolean includeNotBetweenExclusive(final Comparable<T> comparable) {
    final T lowerBound = getLowerBound();
    final T upperBound = getUpperBound();
    if (lowerBound == null && upperBound == null) {
      return true;
    }

    if (comparable == null) {
      return false;
    }

    if (lowerBound == null) {
      return comparable.compareTo(upperBound) > 0;
    }

    if (upperBound == null) {
      return comparable.compareTo(lowerBound) < 0;
    }

    final int lowerCompareResult = comparable.compareTo(lowerBound);
    final int upperCompareResult = comparable.compareTo(upperBound);

    return lowerCompareResult < 0 || upperCompareResult > 0;
  }

  private boolean includeNotBetween(final Comparable<T> comparable) {
    final T lowerBound = getLowerBound();
    final T upperBound = getUpperBound();
    if (lowerBound == null && upperBound == null) {
      return true;
    }

    if (comparable == null) {
      return false;
    }

    if (lowerBound == null) {
      return comparable.compareTo(upperBound) >= 0;
    }

    if (upperBound == null) {
      return comparable.compareTo(lowerBound) <= 0;
    }

    final int lowerCompareResult = comparable.compareTo(lowerBound);
    final int upperCompareResult = comparable.compareTo(upperBound);

    return lowerCompareResult <= 0 || upperCompareResult >= 0;
  }
}
