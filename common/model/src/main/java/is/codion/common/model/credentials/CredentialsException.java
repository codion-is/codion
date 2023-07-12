/*
 * Copyright (c) 2021 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.model.credentials;

/**
 * An exception occurring while fetching credentials.
 */
public class CredentialsException extends RuntimeException {

  protected CredentialsException(String message, Throwable cause) {
    super(message, cause);
  }
}
