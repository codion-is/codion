/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

/**
 * Enumerating the possible ways of comparing values.
 */
public enum SearchType {

  LIKE {
    @Override
    public String getCaption() {
      return "  = ";
    }
  },
  NOT_LIKE {
    @Override
    public String getCaption() {
      return "  \u2260 ";
    }
  },
  LESS_THAN {
    @Override
    public String getCaption() {
      return "  \u2264 ";
    }
  },
  GREATER_THAN {
    @Override
    public String getCaption() {
      return "  \u2265 ";
    }
  },
  WITHIN_RANGE {
    @Override
    public String getCaption() {
      return "\u2265 \u2264";
    }
  },
  OUTSIDE_RANGE {
    @Override
    public String getCaption() {
      return "\u2264 \u2265";
    }
  };

  public abstract String getCaption();
}
