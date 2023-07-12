/*
 * Copyright (c) 2019 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.model.credentials;

import is.codion.common.user.User;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * Provides user credentials based on an authentication token
 */
public interface CredentialsProvider {

  String AUTHENTICATION_TOKEN_PREFIX = "authenticationToken";
  String AUTHENTICATION_TOKEN_DELIMITER = ":";

  /**
   * Performs an authentication lookup, using the default registry port (1099).
   * @param authenticationToken the authentication token
   * @return the User credentials associated with the {@code authenticationToken},
   * an empty Optional if the user credentials were not found or have expired
   * @throws CredentialsException in case of an exception while fetching credentials, such as no credentials provider found
   */
  Optional<User> credentials(UUID authenticationToken) throws CredentialsException;

  /**
   * Returns the first {@link CredentialsProvider} implementation service found.
   * @return a {@link CredentialsProvider} implementation, an empty Optional if none is available
   */
  static Optional<CredentialsProvider> instance() {
    ServiceLoader<CredentialsProvider> loader = ServiceLoader.load(CredentialsProvider.class);
    Iterator<CredentialsProvider> providerIterator = loader.iterator();
    if (providerIterator.hasNext()) {
      return Optional.of(providerIterator.next());
    }

    return Optional.empty();
  }

  /**
   * Finds and returns an authentication token in the given String array
   * @param args the argument array
   * @return the authentication token or an empty Optional if none is found
   * @see #AUTHENTICATION_TOKEN_PREFIX
   */
  static Optional<UUID> authenticationToken(String[] args) {
    if (args == null) {
      return Optional.empty();
    }

    return Arrays.stream(args)
            .filter(CredentialsProvider::isAuthenticationToken)
            .findFirst()
            .map(CredentialsProvider::parseAuthenticationToken);
  }

  /**
   * @param argument the argument
   * @return true if argument is an authentication token ('authenticationToken:123-123-123-123')
   */
  static boolean isAuthenticationToken(String argument) {
    return requireNonNull(argument).startsWith(AUTHENTICATION_TOKEN_PREFIX + AUTHENTICATION_TOKEN_DELIMITER);
  }

  /**
   * @param argument an argument containing an authenticationToken
   * @return the UUID parsed from the given argument
   */
  static UUID parseAuthenticationToken(String argument) {
    return UUID.fromString(requireNonNull(argument).split(AUTHENTICATION_TOKEN_DELIMITER)[1]);
  }
}
