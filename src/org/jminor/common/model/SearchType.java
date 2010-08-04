/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

import java.io.Serializable;

/**
 * Enumerating the possible ways of searching.<br>
 * User: darri<br>
 * Date: 9.3.2007<br>
 * Time: 23:22:01<br>
 */
public enum SearchType implements Serializable {

  LIKE {
    @Override
    public final String getImageName() {
      return "Equals60x16.gif";
    }
  },
  NOT_LIKE {
    @Override
    public final String getImageName() {
      return "NotEquals60x16.gif";
    }
  },
  AT_LEAST {
    @Override
    public final String getImageName() {
      return "LessThanOrEquals60x16.gif";
    }
  },
  AT_MOST {
    @Override
    public final String getImageName() {
      return "LargerThanOrEquals60x16.gif";
    }
  },
  WITHIN_RANGE {
    @Override
    public final String getImageName() {
      return "Inclusive60x16.gif";
    }
  },
  OUTSIDE_RANGE {
    @Override
    public final String getImageName() {
      return "Exclusive60x16.gif";
    }
  };
  public abstract String getImageName();
}
