/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.servlet;

import org.jminor.common.User;
import org.jminor.common.Util;
import org.jminor.common.server.Clients;
import org.jminor.common.server.Server;
import org.jminor.common.server.ServerException;
import org.jminor.framework.db.condition.EntityCondition;
import org.jminor.framework.db.condition.EntitySelectCondition;
import org.jminor.framework.db.remote.RemoteEntityConnection;
import org.jminor.framework.db.remote.RemoteEntityConnectionProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.rmi.Remote;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A service for dealing with Entities
 */
@Path("/")
public final class EntityService extends Application {

  private static final Logger LOG = LoggerFactory.getLogger(EntityService.class);

  public static final String AUTHORIZATION = "Authorization";
  public static final String DOMAIN_ID = "domainId";
  public static final String CLIENT_TYPE_ID = "clientTypeId";
  public static final String CLIENT_ID = "clientId";
  public static final String BASIC_PREFIX = "basic ";
  public static final String X_FORWARDED_FOR = "X-Forwarded-For";
  public static final int BASIC_PREFIX_LENGTH = BASIC_PREFIX.length();

  private static Server<RemoteEntityConnection, Remote> server;

  /**
   * Disconnects the underlying connection
   * @param request the servlet request
   * @param headers the headers
   * @return a response
   */
  @POST
  @Consumes(MediaType.APPLICATION_OCTET_STREAM)
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  @Path("disconnect")
  public Response disconnect(@Context final HttpServletRequest request, @Context final HttpHeaders headers) {
    final RemoteEntityConnection connection = authenticate(request, headers);
    try {
      request.getSession().invalidate();
      connection.disconnect();

      return Response.ok().build();
    }
    catch (final Exception e) {
      LOG.error(e.getMessage(), e);
      return getExceptionResponse(e);
    }
  }

  /**
   * Checks if a transaction is open
   * @param request the servlet request
   * @param headers the headers
   * @return a response
   */
  @POST
  @Consumes(MediaType.APPLICATION_OCTET_STREAM)
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  @Path("isTransactionOpen")
  public Response isTransactionOpen(@Context final HttpServletRequest request, @Context final HttpHeaders headers) {
    final RemoteEntityConnection connection = authenticate(request, headers);
    try {
      return Response.ok(Util.serialize(connection.isTransactionOpen())).build();
    }
    catch (final Exception e) {
      LOG.error(e.getMessage(), e);
      return getExceptionResponse(e);
    }
  }

  /**
   * Begins a transaction
   * @param request the servlet request
   * @param headers the headers
   * @return a response
   */
  @POST
  @Consumes(MediaType.APPLICATION_OCTET_STREAM)
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  @Path("beginTransaction")
  public Response beginTransaction(@Context final HttpServletRequest request, @Context final HttpHeaders headers) {
    final RemoteEntityConnection connection = authenticate(request, headers);
    try {
      connection.beginTransaction();

      return Response.ok().build();
    }
    catch (final Exception e) {
      LOG.error(e.getMessage(), e);
      return getExceptionResponse(e);
    }
  }

  /**
   * Commits a transaction
   * @param request the servlet request
   * @param headers the headers
   * @return a response
   */
  @POST
  @Consumes(MediaType.APPLICATION_OCTET_STREAM)
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  @Path("commitTransaction")
  public Response commitTransaction(@Context final HttpServletRequest request, @Context final HttpHeaders headers) {
    final RemoteEntityConnection connection = authenticate(request, headers);
    try {
      connection.commitTransaction();

      return Response.ok().build();
    }
    catch (final Exception e) {
      LOG.error(e.getMessage(), e);
      return getExceptionResponse(e);
    }
  }

  /**
   * Rolls back an open transaction
   * @param request the servlet request
   * @param headers the headers
   * @return a response
   */
  @POST
  @Consumes(MediaType.APPLICATION_OCTET_STREAM)
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  @Path("rollbackTransaction")
  public Response rollbackTransaction(@Context final HttpServletRequest request, @Context final HttpHeaders headers) {
    final RemoteEntityConnection connection = authenticate(request, headers);
    try {
      connection.rollbackTransaction();

      return Response.ok().build();
    }
    catch (final Exception e) {
      LOG.error(e.getMessage(), e);
      return getExceptionResponse(e);
    }
  }

  /**
   * Executes the procedure identified by {@code procedureId}, with the given parameters
   * @param request the servlet request
   * @param headers the headers
   * @param procedureId the procedure id
   * @return a response
   */
  @POST
  @Consumes(MediaType.APPLICATION_OCTET_STREAM)
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  @Path("procedure")
  public Response procedure(@Context final HttpServletRequest request, @Context final HttpHeaders headers,
                            @QueryParam("procedureId") final String procedureId) {
    final RemoteEntityConnection connection = authenticate(request, headers);
    try {
      connection.executeProcedure(procedureId, EntityService.<List>deserialize(request).toArray());

      return Response.ok().build();
    }
    catch (final Exception e) {
      LOG.error(e.getMessage(), e);
      return getExceptionResponse(e);
    }
  }

  /**
   * Executes the function identified by {@code functionId}, with the given parameters
   * @param request the servlet request
   * @param headers the headers
   * @param functionId the function id
   * @return a response
   */
  @POST
  @Consumes(MediaType.APPLICATION_OCTET_STREAM)
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  @Path("function")
  public Response function(@Context final HttpServletRequest request, @Context final HttpHeaders headers,
                           @QueryParam("functionId") final String functionId) {
    final RemoteEntityConnection connection = authenticate(request, headers);
    try {
      return Response.ok(Util.serialize(
              connection.executeFunction(functionId,
                      EntityService.<List>deserialize(request).toArray()))).build();
    }
    catch (final Exception e) {
      LOG.error(e.getMessage(), e);
      return getExceptionResponse(e);
    }
  }

  /**
   * Fills the given report
   * @param request the servlet request
   * @param headers the headers
   * @return a response
   */
  @POST
  @Consumes(MediaType.APPLICATION_OCTET_STREAM)
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  @Path("report")
  public Response report(@Context final HttpServletRequest request, @Context final HttpHeaders headers) {
    final RemoteEntityConnection connection = authenticate(request, headers);
    try {
      return Response.ok(Util.serialize(connection.fillReport(deserialize(request)))).build();
    }
    catch (final Exception e) {
      LOG.error(e.getMessage(), e);
      return getExceptionResponse(e);
    }
  }

  /**
   * Returns the entities referencing the given entities via foreign keys, mapped to their respective entityIds
   * @param request the servlet request
   * @param headers the headers
   * @return a response
   */
  @POST
  @Consumes(MediaType.APPLICATION_OCTET_STREAM)
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  @Path("dependencies")
  public Response dependencies(@Context final HttpServletRequest request, @Context final HttpHeaders headers) {
    final RemoteEntityConnection connection = authenticate(request, headers);
    try {
      return Response.ok(Util.serialize(
              connection.selectDependentEntities(deserialize(request)))).build();
    }
    catch (final Exception e) {
      LOG.error(e.getMessage(), e);
      return getExceptionResponse(e);
    }
  }

  /**
   * Returns the record count for the given condition
   * @param request the servlet request
   * @param headers the headers
   * @return a response
   */
  @POST
  @Consumes(MediaType.APPLICATION_OCTET_STREAM)
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  @Path("count")
  public Response count(@Context final HttpServletRequest request, @Context final HttpHeaders headers) {
    final RemoteEntityConnection connection = authenticate(request, headers);
    try {
      return Response.ok(Util.serialize(connection.selectRowCount(deserialize(request)))).build();
    }
    catch (final Exception e) {
      LOG.error(e.getMessage(), e);
      return getExceptionResponse(e);
    }
  }

  /**
   * Selects the values for the given propertyId using the given query condition
   * @param request the servlet request
   * @param headers the headers
   * @param propertyId the propertyId
   * @return a response
   */
  @POST
  @Consumes(MediaType.APPLICATION_OCTET_STREAM)
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  @Path("values")
  public Response values(@Context final HttpServletRequest request, @Context final HttpHeaders headers,
                         @QueryParam("propertyId") final String propertyId) {
    final RemoteEntityConnection connection = authenticate(request, headers);
    try {
      return Response.ok(Util.serialize(
              connection.selectValues(propertyId, deserialize(request)))).build();
    }
    catch (final Exception e) {
      LOG.error(e.getMessage(), e);
      return getExceptionResponse(e);
    }
  }

  /**
   * Returns the entities for the given query condition
   * @param request the servlet request
   * @param headers the headers
   * @return a response
   */
  @POST
  @Consumes(MediaType.APPLICATION_OCTET_STREAM)
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  @Path("select")
  public Response select(@Context final HttpServletRequest request, @Context final HttpHeaders headers) {
    final RemoteEntityConnection connection = authenticate(request, headers);
    try {
      final EntitySelectCondition selectCondition = deserialize(request);

      return Response.ok(Util.serialize(connection.selectMany(selectCondition))).build();
    }
    catch (final Exception e) {
      LOG.error(e.getMessage(), e);
      return getExceptionResponse(e);
    }
  }

  /**
   * Inserts the given entities, returning their keys
   * @param request the servlet request
   * @param headers the headers
   * @return a response
   */
  @POST
  @Consumes(MediaType.APPLICATION_OCTET_STREAM)
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  @Path("insert")
  public Response insert(@Context final HttpServletRequest request, @Context final HttpHeaders headers) {
    final RemoteEntityConnection connection = authenticate(request, headers);
    try {
      return Response.ok(Util.serialize(connection.insert(deserialize(request)))).build();
    }
    catch (final Exception e) {
      LOG.error(e.getMessage(), e);
      return getExceptionResponse(e);
    }
  }

  /**
   * Updates the given entities
   * @param request the servlet request
   * @param headers the headers
   * @return a response
   */
  @POST
  @Consumes(MediaType.APPLICATION_OCTET_STREAM)
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  @Path("update")
  public Response update(@Context final HttpServletRequest request, @Context final HttpHeaders headers) {
    final RemoteEntityConnection connection = authenticate(request, headers);
    try {
      return Response.ok(Util.serialize(connection.update(deserialize(request)))).build();
    }
    catch (final Exception e) {
      LOG.error(e.getMessage(), e);
      return getExceptionResponse(e);
    }
  }

  /**
   * Deletes the entities for the given condition
   * @param request the servlet request
   * @param headers the headers
   * @return a response
   */
  @POST
  @Consumes(MediaType.APPLICATION_OCTET_STREAM)
  @Path("delete")
  public Response delete(@Context final HttpServletRequest request, @Context final HttpHeaders headers) {
    final RemoteEntityConnection connection = authenticate(request, headers);
    try {
      final EntityCondition entityCondition = deserialize(request);
      connection.delete(entityCondition);

      return Response.ok().build();
    }
    catch (final Exception e) {
      LOG.error(e.getMessage(), e);
      return getExceptionResponse(e);
    }
  }

  private static RemoteEntityConnection authenticate(final HttpServletRequest request, final HttpHeaders headers) {
    if (server == null) {
      throw new IllegalStateException("EntityConnectionServer has not been set for EntityService");
    }

    final MultivaluedMap<String, String> headerValues = headers.getRequestHeaders();
    final String domainId = getDomainId(headerValues);
    final String clientTypeId = getClientTypeId(headerValues);
    final UUID clientId = getClientId(headerValues, request.getSession());
    final User user = getUser(headerValues);
    try {
      final Map<String, Object> parameters = new HashMap<>(2);
      parameters.put(RemoteEntityConnectionProvider.REMOTE_CLIENT_DOMAIN_ID, domainId);
      parameters.put(Server.CLIENT_HOST_KEY, getRemoteHost(request));

      return server.connect(Clients.connectionRequest(user, clientId, clientTypeId, parameters));
    }
    catch (final ServerException.AuthenticationException ae) {
      throw new WebApplicationException(ae, Response.Status.UNAUTHORIZED);
    }
    catch (final Exception e) {
      LOG.error("Error during authentication", e);
      throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  static void setServer(final Server server) {
    EntityService.server = server;
  }

  private static String getRemoteHost(final HttpServletRequest request) {
    final String forwardHeader = request.getHeader(X_FORWARDED_FOR);
    if (forwardHeader == null){
      return request.getRemoteAddr();
    }

    return forwardHeader.split(",")[0];
  }

  private static Response getExceptionResponse(final Exception exeption) {
    try {
      return Response.serverError().entity(Util.serialize(exeption)).build();
    }
    catch (final IOException e) {
      LOG.error(e.getMessage(), e);
      return Response.serverError().entity(exeption.getMessage()).build();
    }
  }

  private static String getDomainId(final MultivaluedMap<String, String> headers) {
    final List<String> domainIdHeaders = headers.get(DOMAIN_ID);
    checkHeaderParameter(domainIdHeaders, DOMAIN_ID);

    return domainIdHeaders.get(0);
  }

  private static String getClientTypeId(final MultivaluedMap<String, String> headers) {
    final List<String> clientTypeIdHeaders = headers.get(CLIENT_TYPE_ID);
    checkHeaderParameter(clientTypeIdHeaders, CLIENT_TYPE_ID);

    return clientTypeIdHeaders.get(0);
  }

  private static UUID getClientId(final MultivaluedMap<String, String> headers, final HttpSession session) {
    final List<String> clientIdHeaders = headers.get(CLIENT_ID);
    checkHeaderParameter(clientIdHeaders, CLIENT_ID);
    final UUID clientId = UUID.fromString(clientIdHeaders.get(0));
    if (session.isNew()) {
      session.setAttribute(CLIENT_ID, clientId);
    }
    else {
      final UUID sessionClientId = (UUID) session.getAttribute(CLIENT_ID);
      if (sessionClientId == null || !sessionClientId.equals(clientId)) {
        throw new WebApplicationException(CLIENT_ID + " invalid", Response.Status.UNAUTHORIZED);
      }
    }

    return clientId;
  }

  private static User getUser(final MultivaluedMap<String, String> headers) {
    final List<String> basic = headers.get(AUTHORIZATION);
    if (Util.nullOrEmpty(basic)) {
      throw new WebApplicationException(Response.Status.UNAUTHORIZED);
    }

    final String basicAuth = basic.get(0);
    if (basicAuth.length() > BASIC_PREFIX_LENGTH &&
            BASIC_PREFIX.equalsIgnoreCase(basicAuth.substring(0, BASIC_PREFIX_LENGTH + 1))) {
      throw new WebApplicationException(Response.Status.UNAUTHORIZED);
    }

    final byte[] decodedBytes = Base64.getDecoder().decode(basicAuth.substring(BASIC_PREFIX_LENGTH, basicAuth.length()));

    return User.parseUser(new String(decodedBytes));
  }

  private static <T> T deserialize(final HttpServletRequest request) throws IOException, ClassNotFoundException {
    return (T) new ObjectInputStream(request.getInputStream()).readObject();
  }

  private static void checkHeaderParameter(final List<String> headers, final String headerParameter) {
    if (Util.nullOrEmpty(headers)) {
      throw new WebApplicationException(headerParameter + " header parameter is missing", Response.Status.UNAUTHORIZED);
    }
  }
}