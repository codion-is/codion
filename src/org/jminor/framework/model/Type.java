/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.model;

import org.jminor.common.i18n.Messages;

import java.io.Serializable;

public enum Type implements Serializable {
  INT, DOUBLE, STRING, BOOLEAN, CHAR, SHORT_DATE, LONG_DATE, BLOB, ENTITY;

  public static enum Boolean implements Serializable {
    FALSE, TRUE, NULL;

    public static final String BOOLEAN_NULL = "-";
    public static final String BOOLEAN_TRUE = Messages.get(Messages.YES);
    public static final String BOOLEAN_FALSE = Messages.get(Messages.NO);

    /** {@inheritDoc} */
    public String toString() {
      switch(this) {
        case NULL: return BOOLEAN_NULL;
        case TRUE: return BOOLEAN_TRUE;
        case FALSE: return BOOLEAN_FALSE;
      }

      return "";
    }

    public static Boolean get(final java.lang.Boolean aBoolean) {
      return aBoolean == null ? NULL : (aBoolean ? TRUE : FALSE);
    }

    public static java.lang.Boolean get(final Boolean aBoolean) {
      return aBoolean == TRUE;
    }
  }
}
