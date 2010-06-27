package org.jminor.common.model;

import java.util.regex.Pattern;

/**
 * User: Björn Darri<br>
 * Date: 27.6.2010<br>
 * Time: 20:11:27
 */
public class RegexFilterCriteria implements FilterCriteria<Object> {

  private final Pattern pattern;

  public RegexFilterCriteria(final String pattern) {
    this(pattern, true);
  }

  public RegexFilterCriteria(final String patternString, final boolean caseSensitive) {
    pattern = getPattern(patternString, caseSensitive);
  }

  private Pattern getPattern(String patternString, boolean caseSensitive) {
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

  public boolean include(final Object item) {
    if (item == null || pattern == null) {
      return false;
    }

    try {
      return pattern.matcher(item.toString()).find();
    }
    catch (Exception e) {
      return false;
    }
  }
}
