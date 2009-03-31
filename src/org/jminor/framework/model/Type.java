/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.model;

import org.jminor.common.i18n.Messages;

import java.io.Serializable;

/**
 * Represents the supported data types
 */
public enum Type implements Serializable {
  INT, DOUBLE, STRING, BOOLEAN, CHAR, SHORT_DATE, LONG_DATE, BLOB, ENTITY;

  /**
   * Represents a boolean data type, including a null value
   */
  public static enum Boolean implements Serializable {
    FALSE, TRUE, NULL;

    /**
     * A string representation of a boolean 'null' value
     */
    public static final String BOOLEAN_NULL = "-";
    /**
     * A string representation of a boolean 'true' value
     */
    public static final String BOOLEAN_TRUE = Messages.get(Messages.YES);
    /**
     * A string representation of a boolean 'false' value
     */
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

    /**
     * Translates a java.lang.Boolean value into a Type.Boolean value
     * @param aBoolean a java.lang.Boolean value
     * @return a translated value
     */
    public static Boolean get(final java.lang.Boolean aBoolean) {
      return aBoolean == null ? NULL : (aBoolean ? TRUE : FALSE);
    }

    /**
     * Translates a Type.Boolean value into a java.lang.Boolean value, null translates to false
     * @param aBoolean a Type.Boolean value
     * @return a translated value
     */
    public static java.lang.Boolean get(final Boolean aBoolean) {
      return aBoolean == TRUE;
    }
  }
}
