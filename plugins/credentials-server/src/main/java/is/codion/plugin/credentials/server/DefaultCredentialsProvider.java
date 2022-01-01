/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.plugin.credentials.server;

import is.codion.common.credentials.CredentialsException;
import is.codion.common.credentials.CredentialsProvider;
import is.codion.common.credentials.ProviderNotFoundException;
import is.codion.common.credentials.ProviderNotReachableException;
import is.codion.common.user.User;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * A default CredentialsProvider implementation, based on {@link CredentialsServer}.
 * @see CredentialsServer
 */
public final class DefaultCredentialsProvider implements CredentialsProvider {

  private static final Logger LOG = LoggerFactory.getLogger(DefaultCredentialsProvider.class);

  @Override
  public User getCredentials(final UUID authenticationToken) throws CredentialsException {
    LOG.debug("DefaultCredentialsProvider.getCredentials(" + authenticationToken + ")");
    if (authenticationToken == null) {
      return null;
    }
    final Remote credentialsService = getCredentialsService(getRegistry(CredentialsService.REGISTRY_PORT.getOrThrow()));
    try {
      return ((CredentialsService) credentialsService).getUser(requireNonNull(authenticationToken, AUTHENTICATION_TOKEN_PREFIX));
    }
    catch (final RemoteException e) {
      throw new ProviderNotReachableException(e.getMessage(), e);
    }
  }

  private static Remote getCredentialsService(final Registry registry) throws ProviderNotFoundException, ProviderNotReachableException {
    final Remote credentialsService;
    try {
      credentialsService = registry.lookup(CredentialsService.class.getSimpleName());
      LOG.debug("CredentialsService found: " + credentialsService);

      return credentialsService;
    }
    catch (final NotBoundException e) {
      throw new ProviderNotFoundException("CredentialsService not bound in registry", e);
    }
    catch (final RemoteException e) {
      throw new ProviderNotReachableException("RMI registry not reachable", e);
    }
  }

  private static Registry getRegistry(final int registryPort) throws ProviderNotFoundException {
    try {
      return LocateRegistry.getRegistry(registryPort);
    }
    catch (final RemoteException e) {
      throw new ProviderNotFoundException("No RMI registry found", e);
    }
  }
}
