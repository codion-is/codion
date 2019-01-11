/*
 * Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.remote;

/**
 * Responsible for validating connections to a remote server, for example a required client version
 */
public interface ConnectionValidator {

  /**
   * @return the String identifying the client type for which to use this validator
   */
  String getClientTypeId();

  /**
   * Validates the given connection, throwing an exception in case the validation fails
   * @param connectionRequest the connection to validate
   * @throws ServerException.ConnectionValidationException in case the validation fails
   */
  void validate(final ConnectionRequest connectionRequest) throws ServerException.ConnectionValidationException;
}
