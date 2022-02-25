/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.servlet;

import is.codion.common.Serializer;
import is.codion.common.Util;
import is.codion.common.rmi.client.ConnectionRequest;
import is.codion.common.rmi.server.Server;
import is.codion.common.rmi.server.exception.ServerAuthenticationException;
import is.codion.common.rmi.server.exception.ServerException;
import is.codion.common.user.User;
import is.codion.framework.db.rmi.RemoteEntityConnection;
import is.codion.framework.db.rmi.RemoteEntityConnectionProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

/**
 * A base class for a http entity service.
 */
abstract class AbstractEntityService extends Application {

  private static final Logger LOG = LoggerFactory.getLogger(AbstractEntityService.class);

  public static final String AUTHORIZATION = "Authorization";
  public static final String DOMAIN_TYPE_NAME = "domainTypeName";
  public static final String CLIENT_TYPE_ID = "clientTypeId";
  public static final String CLIENT_ID = "clientId";
  public static final String BASIC_PREFIX = "basic ";
  public static final String X_FORWARDED_FOR = "X-Forwarded-For";
  public static final int BASIC_PREFIX_LENGTH = BASIC_PREFIX.length();

  private static Server<RemoteEntityConnection, ? extends Remote> server;

  protected final RemoteEntityConnection authenticate(HttpServletRequest request, HttpHeaders headers) throws RemoteException, ServerException {
    if (server == null) {
      throw new IllegalStateException("EntityServer has not been set for EntityService");
    }

    MultivaluedMap<String, String> headerValues = headers.getRequestHeaders();
    String domainTypeName = getDomainTypeName(headerValues);
    String clientTypeId = getClientTypeId(headerValues);
    UUID clientId = getClientId(headerValues, request.getSession());
    User user = getUser(headerValues);

    return server.connect(ConnectionRequest.builder()
            .user(user)
            .clientId(clientId)
            .clientTypeId(clientTypeId)
            .parameter(RemoteEntityConnectionProvider.REMOTE_CLIENT_DOMAIN_TYPE, domainTypeName)
            .parameter(Server.CLIENT_HOST_KEY, getRemoteHost(request))
            .build());
  }

  static Response logAndGetExceptionResponse(Exception exception) {
    LOG.error(exception.getMessage(), exception);
    try {
      if (exception instanceof ServerAuthenticationException) {
        return Response.status(Response.Status.UNAUTHORIZED).entity(Serializer.serialize(exception)).build();
      }

      return Response.serverError().entity(Serializer.serialize(exception)).build();
    }
    catch (IOException e) {
      LOG.error(e.getMessage(), e);
      return Response.serverError().entity(exception.getMessage()).build();
    }
  }

  static void setServer(Server<RemoteEntityConnection, ? extends Remote> server) {
    AbstractEntityService.server = server;
  }

  private static String getRemoteHost(HttpServletRequest request) {
    String forwardHeader = request.getHeader(X_FORWARDED_FOR);
    if (forwardHeader == null) {
      return request.getRemoteAddr();
    }

    return forwardHeader.split(",")[0];
  }

  private static String getDomainTypeName(MultivaluedMap<String, String> headers) throws ServerAuthenticationException {
    return checkHeaderParameter(headers.get(DOMAIN_TYPE_NAME), DOMAIN_TYPE_NAME);
  }

  private static String getClientTypeId(MultivaluedMap<String, String> headers) throws ServerAuthenticationException {
    return checkHeaderParameter(headers.get(CLIENT_TYPE_ID), CLIENT_TYPE_ID);
  }

  private static UUID getClientId(MultivaluedMap<String, String> headers, HttpSession session)
          throws ServerAuthenticationException {
    UUID headerClientId = UUID.fromString(checkHeaderParameter(headers.get(CLIENT_ID), CLIENT_ID));
    if (session.isNew()) {
      session.setAttribute(CLIENT_ID, headerClientId);
    }
    else {
      UUID sessionClientId = (UUID) session.getAttribute(CLIENT_ID);
      if (sessionClientId == null || !sessionClientId.equals(headerClientId)) {
        session.invalidate();

        throw new ServerAuthenticationException("Invalid client id");
      }
    }

    return headerClientId;
  }

  private static User getUser(MultivaluedMap<String, String> headers) throws ServerAuthenticationException {
    List<String> basic = headers.get(AUTHORIZATION);
    if (Util.nullOrEmpty(basic)) {
      throw new ServerAuthenticationException("Authorization information missing");
    }

    String basicAuth = basic.get(0);
    if (basicAuth.length() > BASIC_PREFIX_LENGTH && BASIC_PREFIX.equalsIgnoreCase(basicAuth.substring(0, BASIC_PREFIX_LENGTH))) {
      return User.parseUser(new String(Base64.getDecoder().decode(basicAuth.substring(BASIC_PREFIX_LENGTH))));
    }

    throw new ServerAuthenticationException("Invalid authorization format");
  }

  private static String checkHeaderParameter(List<String> headers, String headerParameter)
          throws ServerAuthenticationException {
    if (Util.nullOrEmpty(headers)) {
      throw new ServerAuthenticationException(headerParameter + " header parameter is missing");
    }

    return headers.get(0);
  }
}
