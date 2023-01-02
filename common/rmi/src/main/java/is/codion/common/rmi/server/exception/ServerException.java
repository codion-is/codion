/*
 * Copyright (c) 2019 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.rmi.server.exception;

/**
 * An exception originating from a remote server
 */
public class ServerException extends Exception {

  ServerException(String message) {
    super(message);
  }
}
