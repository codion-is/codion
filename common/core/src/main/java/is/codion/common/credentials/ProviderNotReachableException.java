/*
 * Copyright (c) 2021 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.credentials;

/**
 * A credentials service was found but not reachable.
 */
public final class ProviderNotReachableException extends CredentialsException {

  /**
   * Creates a new ProviderNotReachableException
   * @param message the exception message
   * @param cause the root cause
   */
  public ProviderNotReachableException(String message, Throwable cause) {
    super(message, cause);
  }
}
