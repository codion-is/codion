/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.credentials;

/**
 * A credentials service was found but not reachable.
 */
public final class ProviderNotReachableException extends CredentialsException {

  /**
   * Instantiates a new ProviderNotReachableException
   * @param message the exception message
   * @param cause the root cause
   */
  public ProviderNotReachableException(String message, Throwable cause) {
    super(message, cause);
  }
}
