/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

/**
 * A generic exception directed at the user
 */
public class UserException extends Exception {

  private String[] messages;

  public UserException(final String message) {
    this(new String[] {message});
  }

  public UserException(final String[] messages) {
    this(messages[0], null);
    this.messages = messages;
  }

  public UserException(final UserException cause) {
    this(cause.getCause());
    this.messages = cause.messages;
  }

  public UserException(final Throwable cause) {
    this(cause.getMessage(), cause);
  }

  public UserException(final String message, final Throwable cause) {
    super(message, cause);
    this.messages = new String[] {message};
  }

  /**
   * @return Value for property 'messages'.
   */
  public String[] getMessages() {
    if (this.messages == null || this.messages.length == 0)
      return new String[] {getMessage()};
    else {
      final String[] ret = new String[this.messages.length];
      System.arraycopy(this.messages, 0, ret, 0, ret.length);

      return ret;
    }
  }

  @SuppressWarnings({"ThrowableInstanceNeverThrown"})
  public RuntimeException getRuntimeException() {
    return new RuntimeException(getMessage(), getCause());
  }
}
