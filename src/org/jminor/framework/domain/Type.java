/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain;

import java.io.Serializable;

/**
 * Represents the supported data types
 */
public enum Type implements Serializable {
  INT, DOUBLE, STRING, BOOLEAN, CHAR, DATE, TIMESTAMP, BLOB, ENTITY
}
