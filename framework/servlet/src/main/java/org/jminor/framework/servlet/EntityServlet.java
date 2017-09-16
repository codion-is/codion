/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.servlet;

import org.jminor.common.User;
import org.jminor.common.Util;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.db.reports.ReportResult;
import org.jminor.common.db.reports.ReportWrapper;
import org.jminor.common.server.Clients;
import org.jminor.common.server.Server;
import org.jminor.common.server.ServerException;
import org.jminor.framework.db.condition.EntityCondition;
import org.jminor.framework.db.condition.EntitySelectCondition;
import org.jminor.framework.db.remote.RemoteEntityConnection;
import org.jminor.framework.db.remote.RemoteEntityConnectionProvider;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A Servlet implementation for dealing with Entities
 */
@Path("/")
public final class EntityServlet extends Application {

  private static final Logger LOG = LoggerFactory.getLogger(EntityServlet.class);

  public static final String AUTHORIZATION = "Authorization";
  public static final String DOMAIN_ID = "domainId";
  public static final String CLIENT_TYPE_ID = "clientTypeId";
  public static final String CLIENT_ID = "clientId";

  private static Server<RemoteEntityConnection, Remote> server;

  /**
   * Disconnects the underlying connection
   * @param request the servlet request
   * @param headers the headers
   * @return a response
   */
  @GET
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
  @GET
  @Consumes(MediaType.APPLICATION_OCTET_STREAM)
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  @Path("isTransactionOpen")
  public Response isTransactionOpen(@Context final HttpServletRequest request, @Context final HttpHeaders headers) {
    final RemoteEntityConnection connection = authenticate(request, headers);
    try {
      return Response.ok(Util.serializeAndBase64Encode(connection.isTransactionOpen())).build();
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
  @GET
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
  @GET
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
  @GET
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
   * @param parameters the procedure parameters
   * @return a response
   */
  @GET
  @Consumes(MediaType.APPLICATION_OCTET_STREAM)
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  @Path("procedure")
  public Response procedure(@Context final HttpServletRequest request, @Context final HttpHeaders headers,
                            @QueryParam("procedureId") final String procedureId,
                            @QueryParam("parameters") final String parameters) {
    final RemoteEntityConnection connection = authenticate(request, headers);
    try {
      final List parameterList = Util.base64DecodeAndDeserialize(parameters);
      connection.executeProcedure(procedureId, parameterList.toArray());

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
   * @param parameters the procedure parameters
   * @return a response
   */
  @GET
  @Consumes(MediaType.APPLICATION_OCTET_STREAM)
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  @Path("function")
  public Response function(@Context final HttpServletRequest request, @Context final HttpHeaders headers,
                           @QueryParam("functionId") final String functionId,
                           @QueryParam("parameters") final String parameters) {
    final RemoteEntityConnection connection = authenticate(request, headers);
    try {
      final List parameterList = Util.base64DecodeAndDeserialize(parameters);
      final List result = connection.executeFunction(functionId, parameterList.toArray());

      return Response.ok(Util.serializeAndBase64Encode(result)).build();
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
   * @param reportWrapper the report wrapper
   * @return a response
   */
  @GET
  @Consumes(MediaType.APPLICATION_OCTET_STREAM)
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  @Path("report")
  public Response report(@Context final HttpServletRequest request, @Context final HttpHeaders headers,
                         @QueryParam("reportWrapper") final String reportWrapper) {
    final RemoteEntityConnection connection = authenticate(request, headers);
    try {
      final ReportWrapper wrapper = Util.base64DecodeAndDeserialize(reportWrapper);
      final ReportResult result = connection.fillReport(wrapper);

      return Response.ok(Util.serializeAndBase64Encode(result)).build();
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
   * @param entities the entities
   * @return a response
   */
  @GET
  @Consumes(MediaType.APPLICATION_OCTET_STREAM)
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  @Path("dependencies")
  public Response dependencies(@Context final HttpServletRequest request, @Context final HttpHeaders headers,
                               @QueryParam("entities") final String entities) {
    final RemoteEntityConnection connection = authenticate(request, headers);
    try {
      final List<Entity> entityList = Util.base64DecodeAndDeserialize(entities);
      final Map<String, Collection<Entity>> dependencies = connection.selectDependentEntities(entityList);

      return Response.ok(Util.serializeAndBase64Encode(dependencies)).build();
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
   * @param condition the query condition
   * @return a response
   */
  @GET
  @Consumes(MediaType.APPLICATION_OCTET_STREAM)
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  @Path("count")
  public Response count(@Context final HttpServletRequest request, @Context final HttpHeaders headers,
                        @QueryParam("condition") final String condition) {
    final RemoteEntityConnection connection = authenticate(request, headers);
    try {
      final EntityCondition entityCondition = Util.base64DecodeAndDeserialize(condition);

      return Response.ok(Util.serializeAndBase64Encode(connection.selectRowCount(entityCondition))).build();
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
   * @param condition the query condition
   * @return a response
   */
  @GET
  @Consumes(MediaType.APPLICATION_OCTET_STREAM)
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  @Path("values")
  public Response values(@Context final HttpServletRequest request, @Context final HttpHeaders headers,
                         @QueryParam("propertyId") final String propertyId,
                         @QueryParam("condition") final String condition) {
    final RemoteEntityConnection connection = authenticate(request, headers);
    try {
      final EntityCondition entityCondition = Util.base64DecodeAndDeserialize(condition);
      final List values = connection.selectValues(propertyId, entityCondition);

      return Response.ok(Util.serializeAndBase64Encode(values)).build();
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
   * @param condition the query condition
   * @return a response
   */
  @GET
  @Consumes(MediaType.APPLICATION_OCTET_STREAM)
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  public Response select(@Context final HttpServletRequest request, @Context final HttpHeaders headers,
                         @QueryParam("condition") final String condition) {
    final RemoteEntityConnection connection = authenticate(request, headers);
    try {
      final EntitySelectCondition selectCondition = Util.base64DecodeAndDeserialize(condition);

      return Response.ok(Util.serializeAndBase64Encode(connection.selectMany(selectCondition))).build();
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
   * @param entities the entities to insert
   * @return a response
   */
  @POST
  @Consumes(MediaType.APPLICATION_OCTET_STREAM)
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  public Response insert(@Context final HttpServletRequest request, @Context final HttpHeaders headers,
                         @QueryParam("entities") final String entities) {
    final RemoteEntityConnection connection = authenticate(request, headers);
    try {
      final List<Entity.Key> keys = connection.insert(Util.base64DecodeAndDeserialize(entities));

      return Response.ok(Util.serializeAndBase64Encode(keys)).build();
    }
    catch (final Exception e) {
      LOG.error(e.getMessage(), e);
      return getExceptionResponse(e);
    }
  }

  /**
   * Saves, as in, inserts or updates the given entities
   * @param request the servlet request
   * @param headers the headers
   * @param entities the entities to save
   * @return a response
   */
  @PUT
  @Consumes(MediaType.APPLICATION_OCTET_STREAM)
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  public Response save(@Context final HttpServletRequest request, @Context final HttpHeaders headers,
                       @QueryParam("entities") final String entities) {
    final RemoteEntityConnection connection = authenticate(request, headers);
    try {
      final List<Entity> parsedEntities = Util.base64DecodeAndDeserialize(entities);
      final List<Entity> toInsert = new ArrayList<>(parsedEntities.size());
      final List<Entity> toUpdate = new ArrayList<>(parsedEntities.size());
      for (final Entity entity : parsedEntities) {
        if (Entities.isEntityNew(entity)) {
          toInsert.add(entity);
        }
        else {
          toUpdate.add(entity);
        }
      }
      final List<Entity> savedEntities = saveEntities(connection, toInsert, toUpdate);

      return Response.ok(Util.serializeAndBase64Encode(savedEntities)).build();
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
   * @param condition the query condition
   * @return a response
   */
  @DELETE
  @Consumes(MediaType.APPLICATION_OCTET_STREAM)
  public Response delete(@Context final HttpServletRequest request, @Context final HttpHeaders headers,
                         @QueryParam("condition") final String condition) {
    final RemoteEntityConnection connection = authenticate(request, headers);
    try {
      final EntityCondition entityCondition = Util.base64DecodeAndDeserialize(condition);
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
      throw new IllegalStateException("EntityConnectionServer has not been set for EntityServlet");
    }

    final String domainId = getDomainId(headers);
    final String clientTypeId = getClientTypeId(headers);
    final UUID clientId = getClientId(request, headers);
    final User user = getUser(headers);
    try {
      final Map<String, Object> parameters = new HashMap<>(2);
      parameters.put(RemoteEntityConnectionProvider.REMOTE_CLIENT_DOMAIN_ID, domainId);
      parameters.put(Server.CLIENT_HOST_KEY, request.getRemoteHost());

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
    EntityServlet.server = server;
  }

  private static Response getExceptionResponse(final Exception exeption) {
    try {
      return Response.serverError().entity(Util.serializeAndBase64Encode(exeption)).build();
    }
    catch (final IOException e) {
      LOG.error(e.getMessage(), e);
      return Response.serverError().entity(exeption.getMessage()).build();
    }
  }

  private static String getDomainId(final HttpHeaders headers) {
    final List<String> domainIdHeaders = headers.getRequestHeader(DOMAIN_ID);
    if (Util.nullOrEmpty(domainIdHeaders)) {
      throw new WebApplicationException(DOMAIN_ID + " header parameter is missing", Response.Status.UNAUTHORIZED);
    }

    return domainIdHeaders.get(0);
  }

  private static String getClientTypeId(final HttpHeaders headers) {
    final List<String> clientTypeIdHeaders = headers.getRequestHeader(CLIENT_TYPE_ID);
    if (Util.nullOrEmpty(clientTypeIdHeaders)) {
      throw new WebApplicationException(CLIENT_TYPE_ID + " header parameter is missing", Response.Status.UNAUTHORIZED);
    }

    return clientTypeIdHeaders.get(0);
  }

  private static UUID getClientId(final HttpServletRequest request, final HttpHeaders headers) {
    final List<String> clientIdHeaders = headers.getRequestHeader(CLIENT_ID);
    if (Util.nullOrEmpty(clientIdHeaders)) {
      throw new WebApplicationException(CLIENT_ID + " header parameter is missing", Response.Status.UNAUTHORIZED);
    }
    final UUID clientId = UUID.fromString(clientIdHeaders.get(0));
    final HttpSession session = request.getSession();
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

  private static User getUser(final HttpHeaders headers) {
    final List<String> basic = headers.getRequestHeader(AUTHORIZATION);
    if (Util.nullOrEmpty(basic)) {
      throw new WebApplicationException(Response.Status.UNAUTHORIZED);
    }

    final String basicAuth = basic.get(0);
    if (!basicAuth.toLowerCase().startsWith("basic ")) {
      throw new WebApplicationException(Response.Status.UNAUTHORIZED);
    }

    final byte[] decodedBytes = Base64.getDecoder().decode(basicAuth.replaceFirst("[B|b]asic ", ""));

    return User.parseUser(new String(decodedBytes));
  }

  private static List<Entity> saveEntities(final RemoteEntityConnection connection, final List<Entity> toInsert,
                                           final List<Entity> toUpdate) throws DatabaseException, RemoteException {
    final List<Entity> savedEntities = new ArrayList<>(toInsert.size() + toUpdate.size());
    try {
      connection.beginTransaction();
      if (!toInsert.isEmpty()) {
        savedEntities.addAll(connection.selectMany(connection.insert(toInsert)));
      }
      if (!toUpdate.isEmpty()) {
        savedEntities.addAll(connection.update(toUpdate));
      }
      connection.commitTransaction();

      return savedEntities;
    }
    catch (final DatabaseException dbe) {
      connection.rollbackTransaction();
      throw dbe;
    }
  }
}