/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.server;

/**
 * Responsible for validating connections to a remote server, for example a required client version
 */
public interface ConnectionValidator {

  /**
   * @return the String identifying the client type for which to use this validator
   */
  String getClientTypeID();

  /**
   * Validates the given connection, throwing an exception in case the validation fails
   * @param connectionInfo the connection to validate
   * @throws ServerException.ConnectionValidationException in case the validation fails
   */
  void validate(final ConnectionInfo connectionInfo) throws ServerException.ConnectionValidationException;
}
