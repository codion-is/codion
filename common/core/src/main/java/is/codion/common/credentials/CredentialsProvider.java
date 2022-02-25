/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.credentials;

import is.codion.common.user.User;

import java.util.Arrays;
import java.util.Iterator;
import java.util.ServiceLoader;
import java.util.UUID;

/**
 * Provides user credentials based on an authentication token
 */
public interface CredentialsProvider {

  String AUTHENTICATION_TOKEN_PREFIX = "authenticationToken";
  String AUTHENTICATION_TOKEN_DELIMITER = ":";

  /**
   * Finds and returns an authentication token in the given String array
   * @param args the array
   * @return the authentication token or null if none is found
   * @see #AUTHENTICATION_TOKEN_PREFIX
   */
  default UUID getAuthenticationToken(String[] args) {
    if (args == null) {
      return null;
    }

    return Arrays.stream(args).filter(CredentialsProvider::isAuthenticationToken).findFirst()
            .map(CredentialsProvider::getAuthenticationToken).orElse(null);
  }

  /**
   * Performs an authentication lookup, using the default registry port (1099).
   * @param authenticationToken the authentication token
   * @return the User credentials associated with the {@code authenticationToken}, null if authenticationToken
   * was null, the user credentials were not found or have expired
   * @throws CredentialsException in case of an exception while fetching credentials, such as no credentials provider found
   */
  User getCredentials(UUID authenticationToken) throws CredentialsException;

  /**
   * Returns the first {@link CredentialsProvider} implementation service found.
   * @return a {@link CredentialsProvider} implementation, null if none is available
   */
  static CredentialsProvider credentialsProvider() {
    ServiceLoader<CredentialsProvider> loader = ServiceLoader.load(CredentialsProvider.class);
    Iterator<CredentialsProvider> providerIterator = loader.iterator();
    if (providerIterator.hasNext()) {
      return providerIterator.next();
    }

    return null;
  }

  /**
   * @param argument the argument
   * @return true if argument is an authentication token ('authenticationToken:123-123-123-123')
   */
  static boolean isAuthenticationToken(String argument) {
    return argument.startsWith(AUTHENTICATION_TOKEN_PREFIX + AUTHENTICATION_TOKEN_DELIMITER);
  }

  /**
   * @param argument an argument containing an authenticationToken
   * @return the UUID parsed from the given argument
   */
  static UUID getAuthenticationToken(String argument) {
    return UUID.fromString(argument.split(AUTHENTICATION_TOKEN_DELIMITER)[1]);
  }
}
