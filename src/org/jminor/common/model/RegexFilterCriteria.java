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
    pattern = caseSensitive ? Pattern.compile(patternString) : Pattern.compile(patternString, Pattern.CASE_INSENSITIVE);
  }

  /**
   * Returns true if the regex pattern is valid and the given item passes the criteria.
   * @param item the item
   * @return true if the item should be included
   */
  public boolean include(final T item) {
    return item != null && pattern.matcher(item.toString()).find();
  }
}
