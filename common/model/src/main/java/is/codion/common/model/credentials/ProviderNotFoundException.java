/*
 * Copyright (c) 2021 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.model.credentials;

/**
 * A credentials service was not found.
 */
public final class ProviderNotFoundException extends CredentialsException {

  /**
   * Creates a new ProviderNotFoundException
   * @param message the exception message
   * @param cause the root cause
   */
  public ProviderNotFoundException(String message, Throwable cause) {
    super(message, cause);
  }
}
