/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.remote.exception;

/**
 * An exception originating from a remote server
 */
public class ServerException extends Exception {

  ServerException(final String message) {
    super(message);
  }
}
