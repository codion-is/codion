/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui;

/**
 * Describes the possible ways to link a property value to a ui component.
 */
public enum LinkType {
  /**
   * The property value is only read and displayed in the component
   */
  READ_ONLY,
  /**
   * The property value is never read but only written according to the value in the component
   */
  WRITE_ONLY,
  /**
   * The property value is read and written according to the component
   */
  READ_WRITE
}
