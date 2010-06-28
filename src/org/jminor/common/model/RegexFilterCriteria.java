package org.jminor.common.model;

import java.util.regex.Pattern;

/**
 * User: Björn Darri<br>
 * Date: 27.6.2010<br>
 * Time: 20:11:27
 */
public class RegexFilterCriteria implements FilterCriteria {

  private final Pattern pattern;

  public RegexFilterCriteria(final String patternString) {
    this(patternString, true);
  }

  public RegexFilterCriteria(final String patternString, final boolean caseSensitive) {
    pattern = initializePattern(patternString, caseSensitive);
  }

  public boolean include(final Object item) {
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
