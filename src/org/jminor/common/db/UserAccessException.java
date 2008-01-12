/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 *
 */
package org.jminor.common.db;

import java.sql.SQLException;

/**
 * Exception class for using when the provided login credentials fail
 */
public class UserAccessException extends DbException {

  public UserAccessException(final String username) {
    this(username, null);
  }

  public UserAccessException(final String username, final SQLException cause) {
    super(cause, username);
  }
}
