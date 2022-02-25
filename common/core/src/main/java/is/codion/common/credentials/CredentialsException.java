/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.credentials;

/**
 * An exception occurring while fetching credentials.
 */
public class CredentialsException extends Exception {

  protected CredentialsException(String message, Throwable cause) {
    super(message, cause);
  }
}
