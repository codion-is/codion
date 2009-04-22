/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db;

import java.sql.SQLException;

/**
 * Exception class for using when an authentication fails
 */
public class AuthenticationException extends DbException {

  public AuthenticationException(final String username) {
    this(username, null);
  }

  public AuthenticationException(final String username, final SQLException cause) {
    super(cause, username);
  }
}
