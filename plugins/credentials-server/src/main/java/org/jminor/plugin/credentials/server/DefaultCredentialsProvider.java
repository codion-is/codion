/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.plugin.credentials.server;

import org.jminor.common.CredentialsProvider;
import org.jminor.common.User;
import org.jminor.common.remote.Servers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * A default CredentialsProvider implementation, based on {@link CredentialsServer}.
 * @see CredentialsServer
 */
public final class DefaultCredentialsProvider implements CredentialsProvider {

  private static final Logger LOG = LoggerFactory.getLogger(DefaultCredentialsProvider.class);

  /** {@inheritDoc} */
  @Override
  public User getCredentials(final UUID authenticationToken) {
    LOG.debug("DefaultCredentialsProvider.getCredentials(" + authenticationToken + ")");
    if (authenticationToken == null) {
      return null;
    }
    try {
      final Remote credentialsService = Servers.getRegistry(Registry.REGISTRY_PORT).lookup(CredentialsService.class.getSimpleName());
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
