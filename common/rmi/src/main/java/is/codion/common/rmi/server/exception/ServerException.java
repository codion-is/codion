/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.common.rmi.server.exception;

/**
 * An exception originating from a remote server
 */
public class ServerException extends Exception {

  ServerException(final String message) {
    super(message);
  }
}
