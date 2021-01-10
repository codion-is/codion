/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.plugin.credentials.server;

import is.codion.common.CredentialsProvider;
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
  public User getCredentials(final UUID authenticationToken) {
    return getCredentials(authenticationToken, Registry.REGISTRY_PORT);
  }

  @Override
  public User getCredentials(final UUID authenticationToken, final int registryPort) {
    LOG.debug("DefaultCredentialsProvider.getCredentials(" + authenticationToken + ")");
    if (authenticationToken == null) {
      return null;
    }
    try {
      final Remote credentialsService = LocateRegistry.getRegistry(registryPort).lookup(CredentialsService.class.getSimpleName());
      LOG.debug("CredentialsService found: " + credentialsService);

      return ((CredentialsService) credentialsService).getUser(requireNonNull(authenticationToken, AUTHENTICATION_TOKEN_PREFIX));
    }
    catch (final NotBoundException | RemoteException e) {
      LOG.debug("No CredentialsService found", e);
      //no credential server available or not reachable
      return null;
    }
  }
}
