/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.servlet;

import is.codion.common.Serializer;
import is.codion.common.Util;
import is.codion.common.db.reports.ReportWrapper;
import is.codion.common.rmi.client.ConnectionRequest;
import is.codion.common.rmi.server.Server;
import is.codion.common.rmi.server.exception.ServerAuthenticationException;
import is.codion.common.rmi.server.exception.ServerException;
import is.codion.common.user.User;
import is.codion.common.user.Users;
import is.codion.framework.db.condition.EntityCondition;
import is.codion.framework.db.condition.EntitySelectCondition;
import is.codion.framework.db.condition.EntityUpdateCondition;
import is.codion.framework.db.rmi.RemoteEntityConnection;
import is.codion.framework.db.rmi.RemoteEntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.sql.Types;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static is.codion.framework.domain.property.Properties.attribute;
import static is.codion.framework.domain.property.Properties.blobAttribute;

/**
 * A service for dealing with entities
 */
@Path("/")
@Singleton
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
   * Returns the underlying domain entities
   * @param request the servlet request
   * @param headers the headers
   * @return a response
   */
  @POST
  @Consumes(MediaType.APPLICATION_OCTET_STREAM)
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  @Path("getEntities")
  public Response getEntities(@Context final HttpServletRequest request, @Context final HttpHeaders headers) {
    try {
      final RemoteEntityConnection connection = authenticate(request, headers);

      return Response.ok(Serializer.serialize(connection.getEntities())).build();
    }
    catch (final Exception e) {
      LOG.error(e.getMessage(), e);
      return getExceptionResponse(e);
    }
  }

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
    try {
      final RemoteEntityConnection connection = authenticate(request, headers);
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
    try {
      final RemoteEntityConnection connection = authenticate(request, headers);

      return Response.ok(Serializer.serialize(connection.isTransactionOpen())).build();
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
    try {
      final RemoteEntityConnection connection = authenticate(request, headers);
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
    try {
      final RemoteEntityConnection connection = authenticate(request, headers);
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
    try {
      final RemoteEntityConnection connection = authenticate(request, headers);
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
    try {
      final RemoteEntityConnection connection = authenticate(request, headers);
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
    try {
      final RemoteEntityConnection connection = authenticate(request, headers);

      return Response.ok(Serializer.serialize(connection.executeFunction(functionId,
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
    try {
      final RemoteEntityConnection connection = authenticate(request, headers);

      final List parameters = deserialize(request);
      return Response.ok(Serializer.serialize(connection.fillReport((ReportWrapper) parameters.get(0), parameters.get(1)))).build();
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
    try {
      final RemoteEntityConnection connection = authenticate(request, headers);

      return Response.ok(Serializer.serialize(connection.selectDependencies(deserialize(request)))).build();
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
    try {
      final RemoteEntityConnection connection = authenticate(request, headers);

      return Response.ok(Serializer.serialize(connection.selectRowCount(deserialize(request)))).build();
    }
    catch (final Exception e) {
      LOG.error(e.getMessage(), e);
      return getExceptionResponse(e);
    }
  }

  /**
   * Selects the values for the given attribute using the given query condition
   * @param request the servlet request
   * @param headers the headers
   * @param attribute the attribute
   * @return a response
   */
  @POST
  @Consumes(MediaType.APPLICATION_OCTET_STREAM)
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  @Path("values")
  public Response values(@Context final HttpServletRequest request, @Context final HttpHeaders headers,
                         @QueryParam("attribute") final String attribute) {
    try {
      final RemoteEntityConnection connection = authenticate(request, headers);

      return Response.ok(Serializer.serialize(connection.selectValues(attribute(attribute, Types.OTHER), deserialize(request)))).build();
    }
    catch (final Exception e) {
      LOG.error(e.getMessage(), e);
      return getExceptionResponse(e);
    }
  }

  /**
   * Returns the entities for the given keys
   * @param request the servlet request
   * @param headers the headers
   * @return a response
   */
  @POST
  @Consumes(MediaType.APPLICATION_OCTET_STREAM)
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  @Path("selectByKey")
  public Response selectByKey(@Context final HttpServletRequest request, @Context final HttpHeaders headers) {
    try {
      final RemoteEntityConnection connection = authenticate(request, headers);
      final List<Entity.Key> keys = deserialize(request);

      return Response.ok(Serializer.serialize(connection.select(keys))).build();
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
    try {
      final RemoteEntityConnection connection = authenticate(request, headers);
      final EntitySelectCondition selectCondition = deserialize(request);

      return Response.ok(Serializer.serialize(connection.select(selectCondition))).build();
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
    try {
      final RemoteEntityConnection connection = authenticate(request, headers);

      return Response.ok(Serializer.serialize(connection.insert((List<Entity>) deserialize(request)))).build();
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
    try {
      final RemoteEntityConnection connection = authenticate(request, headers);

      return Response.ok(Serializer.serialize(connection.update((List<Entity>) deserialize(request)))).build();
    }
    catch (final Exception e) {
      LOG.error(e.getMessage(), e);
      return getExceptionResponse(e);
    }
  }

  /**
   * Performs an update according to the given condition
   * @param request the servlet request
   * @param headers the headers
   * @return a response
   */
  @POST
  @Consumes(MediaType.APPLICATION_OCTET_STREAM)
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  @Path("updateByCondition")
  public Response updateByCondition(@Context final HttpServletRequest request, @Context final HttpHeaders headers) {
    try {
      final RemoteEntityConnection connection = authenticate(request, headers);

      return Response.ok(Serializer.serialize(connection.update((EntityUpdateCondition) deserialize(request)))).build();
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
    try {
      final RemoteEntityConnection connection = authenticate(request, headers);
      final EntityCondition entityCondition = deserialize(request);

      return Response.ok(Serializer.serialize(connection.delete(entityCondition))).build();
    }
    catch (final Exception e) {
      LOG.error(e.getMessage(), e);
      return getExceptionResponse(e);
    }
  }

  /**
   * Deletes the entities for the given keys
   * @param request the servlet request
   * @param headers the headers
   * @return a response
   */
  @POST
  @Consumes(MediaType.APPLICATION_OCTET_STREAM)
  @Path("deleteByKey")
  public Response deleteByKey(@Context final HttpServletRequest request, @Context final HttpHeaders headers) {
    try {
      final RemoteEntityConnection connection = authenticate(request, headers);
      final List<Entity.Key> keys = deserialize(request);

      return Response.ok(Serializer.serialize(connection.delete(keys))).build();
    }
    catch (final Exception e) {
      LOG.error(e.getMessage(), e);
      return getExceptionResponse(e);
    }
  }

  /**
   * Writes a BLOB value
   * @param request the servlet request
   * @param headers the headers
   * @return a response
   */
  @POST
  @Consumes(MediaType.APPLICATION_OCTET_STREAM)
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  @Path("writeBlob")
  public Response writeBlob(@Context final HttpServletRequest request, @Context final HttpHeaders headers) {
    try {
      final RemoteEntityConnection connection = authenticate(request, headers);
      final List parameters = deserialize(request);
      connection.writeBlob((Entity.Key) parameters.get(0), blobAttribute((String) parameters.get(1)), (byte[]) parameters.get(2));

      return Response.ok().build();
    }
    catch (final Exception e) {
      LOG.error(e.getMessage(), e);
      return getExceptionResponse(e);
    }
  }

  /**
   * Reads a BLOB value
   * @param request the servlet request
   * @param headers the headers
   * @return a response
   */
  @POST
  @Consumes(MediaType.APPLICATION_OCTET_STREAM)
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  @Path("readBlob")
  public Response readBlob(@Context final HttpServletRequest request, @Context final HttpHeaders headers) {
    try {
      final RemoteEntityConnection connection = authenticate(request, headers);
      final List parameters = deserialize(request);

      return Response.ok(Serializer.serialize(connection.readBlob((Entity.Key) parameters.get(0), blobAttribute((String) parameters.get(1))))).build();
    }
    catch (final Exception e) {
      LOG.error(e.getMessage(), e);
      return getExceptionResponse(e);
    }
  }

  private static RemoteEntityConnection authenticate(final HttpServletRequest request, final HttpHeaders headers)
          throws RemoteException, ServerException {
    if (server == null) {
      throw new IllegalStateException("EntityServer has not been set for EntityService");
    }

    final MultivaluedMap<String, String> headerValues = headers.getRequestHeaders();
    final String domainId = getDomainId(headerValues);
    final String clientTypeId = getClientTypeId(headerValues);
    final UUID clientId = getClientId(headerValues, request.getSession());
    final User user = getUser(headerValues);
    final Map<String, Object> parameters = new HashMap<>(2);
    parameters.put(RemoteEntityConnectionProvider.REMOTE_CLIENT_DOMAIN_ID, domainId);
    parameters.put(Server.CLIENT_HOST_KEY, getRemoteHost(request));

    return server.connect(ConnectionRequest.connectionRequest(user, clientId, clientTypeId, parameters));
  }

  static void setServer(final Server server) {
    EntityService.server = server;
  }

  private static String getRemoteHost(final HttpServletRequest request) {
    final String forwardHeader = request.getHeader(X_FORWARDED_FOR);
    if (forwardHeader == null) {
      return request.getRemoteAddr();
    }

    return forwardHeader.split(",")[0];
  }

  private static Response getExceptionResponse(final Exception exeption) {
    try {
      if (exeption instanceof ServerAuthenticationException) {
        return Response.status(Response.Status.UNAUTHORIZED).entity(Serializer.serialize(exeption)).build();
      }

      return Response.serverError().entity(Serializer.serialize(exeption)).build();
    }
    catch (final IOException e) {
      LOG.error(e.getMessage(), e);
      return Response.serverError().entity(exeption.getMessage()).build();
    }
  }

  private static String getDomainId(final MultivaluedMap<String, String> headers) throws ServerAuthenticationException {
    return checkHeaderParameter(headers.get(DOMAIN_ID), DOMAIN_ID);
  }

  private static String getClientTypeId(final MultivaluedMap<String, String> headers) throws ServerAuthenticationException {
    return checkHeaderParameter(headers.get(CLIENT_TYPE_ID), CLIENT_TYPE_ID);
  }

  private static UUID getClientId(final MultivaluedMap<String, String> headers, final HttpSession session)
          throws ServerAuthenticationException {
    final UUID headerClientId = UUID.fromString(checkHeaderParameter(headers.get(CLIENT_ID), CLIENT_ID));
    if (session.isNew()) {
      session.setAttribute(CLIENT_ID, headerClientId);
    }
    else {
      final UUID sessionClientId = (UUID) session.getAttribute(CLIENT_ID);
      if (sessionClientId == null || !sessionClientId.equals(headerClientId)) {
        session.invalidate();

        throw new ServerAuthenticationException("Invalid client id");
      }
    }

    return headerClientId;
  }

  private static User getUser(final MultivaluedMap<String, String> headers) throws ServerAuthenticationException {
    final List<String> basic = headers.get(AUTHORIZATION);
    if (Util.nullOrEmpty(basic)) {
      throw new ServerAuthenticationException("Authorization information missing");
    }

    final String basicAuth = basic.get(0);
    if (basicAuth.length() > BASIC_PREFIX_LENGTH && BASIC_PREFIX.equalsIgnoreCase(basicAuth.substring(0, BASIC_PREFIX_LENGTH))) {
      return Users.parseUser(new String(Base64.getDecoder().decode(basicAuth.substring(BASIC_PREFIX_LENGTH))));
    }

    throw new ServerAuthenticationException("Invalid authorization format");
  }

  private static <T> T deserialize(final HttpServletRequest request) throws IOException, ClassNotFoundException {
    return (T) new ObjectInputStream(request.getInputStream()).readObject();
  }

  private static String checkHeaderParameter(final List<String> headers, final String headerParameter)
          throws ServerAuthenticationException {
    if (Util.nullOrEmpty(headers)) {
      throw new ServerAuthenticationException(headerParameter + " header parameter is missing");
    }

    return headers.get(0);
  }
}