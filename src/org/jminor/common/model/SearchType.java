/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

import org.jminor.common.i18n.Messages;

/**
 * Enumerating all the possible ways of searching.
 */
public enum SearchType {

  LIKE("  = ", Messages.get(Messages.LIKE)),
  NOT_LIKE("  \u2260 ", Messages.get(Messages.NOT_LIKE)),
  /** Less than or equals*/
  LESS_THAN("  \u2264 ", Messages.get(Messages.LESS_THAN)),
  /** Greater than or equals*/
  GREATER_THAN("  \u2265 ", Messages.get(Messages.GREATER_THAN)),
  WITHIN_RANGE("\u2265 \u2264", Messages.get(Messages.WITHIN_RANGE)),
  OUTSIDE_RANGE("\u2264 \u2265", Messages.get(Messages.OUTSIDE_RANGE));

  private final String caption;
  private final String description;

  SearchType(final String caption, final String description) {
    this.caption = caption;
    this.description = description;
  }

  public String getCaption() {
    return caption;
  }

  public String getDescription() {
    return description;
  }
}
