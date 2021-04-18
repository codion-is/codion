/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.credentials;

/**
 * A credentials service was not found.
 */
public final class ProviderNotFoundException extends CredentialsException {

  /**
   * Instantiates a new ProviderNotFoundException
   * @param message the exception message
   * @param cause the root cause
   */
  public ProviderNotFoundException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
