/*
 * Copyright (c) 2008, Bj�rn Darri Sigur�sson. All Rights Reserved.
 */
package org.jminor.framework.model;

import org.jminor.common.i18n.Messages;

import java.io.Serializable;
import java.util.Date;

public enum Type implements Serializable {
  INT, DOUBLE, STRING, BOOLEAN, CHAR, SHORT_DATE, LONG_DATE, BLOB, ENTITY;

  public static Class getValueClass(final Type type, final Object value) {
    if (type == INT)
      return Integer.class;
    if (type == DOUBLE)
      return Double.class;
    if (type == BOOLEAN)
      return Boolean.class;
    if (type == SHORT_DATE || type == LONG_DATE)
      return Date.class;
    if (type == CHAR)
      return Character.class;
    if (type == ENTITY)
      return Entity.class;

    return value == null ? Object.class : value.getClass();
  }

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
  }
}
