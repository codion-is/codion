/*
 * Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common;

import java.util.Arrays;
import java.util.UUID;

/**
 * Provides user credentials based on an authentication token
 */
public interface CredentialsProvider {

  String AUTHENTICATION_TOKEN_PREFIX = "authenticationToken";
  String AUTHENTICATION_TOKEN_DELIMITER = ":";

  /**
   * Finds and returns a authentication token in the given String array
   * @param args the array
   * @return the authentication token or null if none is found
   * @see #AUTHENTICATION_TOKEN_PREFIX
   */
  default UUID getAuthenticationToken(final String[] args) {
    if (args == null) {
      return null;
    }

    final UUID token = Arrays.stream(args).filter(CredentialsProvider::isAuthenticationToken).findFirst()
            .map(CredentialsProvider::getAuthenticationToken).orElse(null);
    //keep old WebStart method for backwards compatibility
    if (token == null && args.length > 1 && "-open".equals(args[0])) {
      return UUID.fromString(args[1]);
    }

    return token;
  }

  /**
   * Performs a authentication lookup.
   * @param authenticationToken the authentication token
   * @return the User credentials associated with the {@code authenticationToken}, null if authenticationToken
   * was null, the user credentials were not found, have expired or if no authentication service is running
   */
  User getCredentials(final UUID authenticationToken);

  /**
   * @param argument the argument
   * @return true if argument is an authentication token ('authenticationToken:123-123-123-123')
   */
  static boolean isAuthenticationToken(final String argument) {
    return argument.startsWith(AUTHENTICATION_TOKEN_PREFIX + AUTHENTICATION_TOKEN_DELIMITER);
  }

  /**
   * @param argument an argument containing an authenticationToken
   * @return the UUID parsed from the given argument
   */
  static UUID getAuthenticationToken(final String argument) {
    return UUID.fromString(argument.split(AUTHENTICATION_TOKEN_DELIMITER)[1]);
  }
}
