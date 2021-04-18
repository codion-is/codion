/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.credentials;

/**
 * An exception occurring while fetching credentials.
 */
public class CredentialsException extends Exception {

  protected CredentialsException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
