/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

import java.util.regex.Pattern;

/**
 * A FilterCriteria implementation based on a regular expression.
 */
public final class RegexFilterCriteria<T> implements FilterCriteria<T> {

  private final Pattern pattern;

  /**
   * Instantiates a new RegexFilterCriteria.
   * @param patternString the regex pattern
   */
  public RegexFilterCriteria(final String patternString) {
    this(patternString, true);
  }

  /**
   * Instantiates a new RegexFilterCriteria.
   * @param patternString the regex pattern
   * @param caseSensitive if true then this criteria is case sensitive
   */
  public RegexFilterCriteria(final String patternString, final boolean caseSensitive) {
    pattern = initializePattern(patternString, caseSensitive);
  }

  /**
   * @return true if the pattern is valid.
   */
  public boolean isPatternValid() {
    return pattern != null;
  }

  /**
   * Returns true if the regex pattern is valid and the given item passes the criteria.
   * @param item the item
   * @return true if the item should be included
   */
  public boolean include(final T item) {
    if (item == null || pattern == null) {
      return false;
    }

    return pattern.matcher(item.toString()).find();
  }

  private Pattern initializePattern(final String patternString, final boolean caseSensitive) {
    try {
      if (caseSensitive) {
        return Pattern.compile(patternString);
      }
      else {
        return Pattern.compile(patternString, Pattern.CASE_INSENSITIVE);
      }
    }
    catch (Exception e) {
      return null;
    }
  }
}
