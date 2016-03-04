/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.server;

/**
 * Responsible for validating clients when connecting to a remote server, for example a required client version
 */
public interface ClientValidator {

  /**
   * @return the String identifying the client type for which to use this validator
   */
  String getClientTypeID();

  /**
   * Validates the given client, throwing an exception in case the validation fails
   * @param connectionInfo the client to validate
   * @throws ServerException.ClientValidationException in case the validation fails
   */
  void validate(final ConnectionInfo connectionInfo) throws ServerException.ClientValidationException;
}
