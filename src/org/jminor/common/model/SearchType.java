/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

import java.io.Serializable;

/**
 * User: darri
 * Date: 9.3.2007
 * Time: 23:22:01
 */
public enum SearchType implements Serializable {
  LIKE, NOT_LIKE, AT_LEAST, AT_MOST, WITHIN_RANGE, OUTSIDE_RANGE
}
