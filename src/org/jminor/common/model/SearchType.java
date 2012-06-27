/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

import org.jminor.common.i18n.Messages;

/**
 * Enumerating all the possible ways of searching.
 */
public enum SearchType {

  LIKE {
    @Override
    public String getCaption() {
      return "  = ";
    }

    @Override
    public String getDescription() {
      return Messages.get(Messages.LIKE);
    }
  },
  NOT_LIKE {
    @Override
    public String getCaption() {
      return "  \u2260 ";
    }

    @Override
    public String getDescription() {
      return Messages.get(Messages.NOT_LIKE);
    }
  },
  LESS_THAN {
    @Override
    public String getCaption() {
      return "  \u2264 ";
    }

    @Override
    public String getDescription() {
      return Messages.get(Messages.LESS_THAN);
    }
  },
  GREATER_THAN {
    @Override
    public String getCaption() {
      return "  \u2265 ";
    }

    @Override
    public String getDescription() {
      return Messages.get(Messages.GREATER_THAN);
    }
  },
  WITHIN_RANGE {
    @Override
    public String getCaption() {
      return "\u2265 \u2264";
    }

    @Override
    public String getDescription() {
      return Messages.get(Messages.WITHIN_RANGE);
    }
  },
  OUTSIDE_RANGE {
    @Override
    public String getCaption() {
      return "\u2264 \u2265";
    }

    @Override
    public String getDescription() {
      return Messages.get(Messages.OUTSIDE_RANGE);
    }
  };

  /**
   * @return the SearchType caption
   */
  public abstract String getCaption();

  /**
   * @return a description for this SearchType
   */
  public abstract String getDescription();
}
