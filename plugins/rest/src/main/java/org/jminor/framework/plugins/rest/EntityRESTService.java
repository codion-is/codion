/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.plugins.rest;

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
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A proof of concept implementation of a REST service for dealing with Entities
 */
@Path("/")
public final class EntityRESTService extends Application {

  private static final Logger LOG = LoggerFactory.getLogger(EntityRESTService.class);

  public static final String AUTHORIZATION = "Authorization";
  public static final String CLIENT_ID = "clientId";
  public static final String ERROR_WHILE_SELECTING = "Error while selecting";

  private static Server server;

  @GET
  @Consumes(MediaType.APPLICATION_OCTET_STREAM)
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  @Path("procedure")
  public Response procedure(@Context final HttpServletRequest request, @Context final HttpHeaders headers,
                            @QueryParam("domainId") final String domainId,
                            @QueryParam("procedureId") final String procedureId,
                            @QueryParam("parameters") final String parameters) {
    final RemoteEntityConnection connection = authenticate(request, headers, domainId);
    try {
      final List parameterList = Util.base64DecodeAndDeserialize(parameters);
      connection.executeProcedure(procedureId, parameterList.toArray());
      return Response.ok().build();
    }
    catch (final Exception e) {
      LOG.error("Error while executing procedure: " + procedureId, e);
      return Response.serverError().entity(e.getMessage()).build();
    }
  }

  @GET
  @Consumes(MediaType.APPLICATION_OCTET_STREAM)
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  @Path("function")
  public Response function(@Context final HttpServletRequest request, @Context final HttpHeaders headers,
                           @QueryParam("domainId") final String domainId,
                           @QueryParam("functionId") final String functionId,
                           @QueryParam("parameters") final String parameters) {
    final RemoteEntityConnection connection = authenticate(request, headers, domainId);
    try {
      final List parameterList = Util.base64DecodeAndDeserialize(parameters);
      final List result = connection.executeFunction(functionId, parameterList.toArray());
      return Response.ok(Util.serializeAndBase64Encode(result)).build();
    }
    catch (final Exception e) {
      LOG.error("Error while executing function: " + functionId, e);
      return Response.serverError().entity(e.getMessage()).build();
    }
  }

  @GET
  @Consumes(MediaType.APPLICATION_OCTET_STREAM)
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  @Path("report")
  public Response report(@Context final HttpServletRequest request, @Context final HttpHeaders headers,
                         @QueryParam("domainId") final String domainId,
                         @QueryParam("reportWrapper") final String reportWrapper) {
    final RemoteEntityConnection connection = authenticate(request, headers, domainId);
    try {
      final List<ReportWrapper> wrapperList = Util.base64DecodeAndDeserialize(reportWrapper);
      final ReportResult result = connection.fillReport(wrapperList.get(0));
      return Response.ok(Util.serializeAndBase64Encode(Collections.singletonList(result))).build();
    }
    catch (final Exception e) {
      LOG.error("Error while filling report", e);
      return Response.serverError().entity(e.getMessage()).build();
    }
  }

  @GET
  @Consumes(MediaType.APPLICATION_OCTET_STREAM)
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  @Path("dependencies")
  public Response dependencies(@Context final HttpServletRequest request, @Context final HttpHeaders headers,
                               @QueryParam("domainId") final String domainId,
                               @QueryParam("entities") final String entities) {
    final RemoteEntityConnection connection = authenticate(request, headers, domainId);
    try {
      final List<Entity> entityList = Util.base64DecodeAndDeserialize(entities);
      final Map<String, Collection<Entity>> dependencies = connection.selectDependentEntities(entityList);
      return Response.ok(Util.serializeAndBase64Encode(Collections.singletonList(dependencies))).build();
    }
    catch (final Exception e) {
      LOG.error(ERROR_WHILE_SELECTING, e);
      return Response.serverError().entity(e.getMessage()).build();
    }
  }

  @GET
  @Consumes(MediaType.APPLICATION_OCTET_STREAM)
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  @Path("count")
  public Response count(@Context final HttpServletRequest request, @Context final HttpHeaders headers,
                        @QueryParam("domainId") final String domainId,
                        @QueryParam("condition") final String condition) {
    final RemoteEntityConnection connection = authenticate(request, headers, domainId);
    try {
      final List<EntitySelectCondition> selectConditions = Util.base64DecodeAndDeserialize(condition);
      return Response.ok(Util.serializeAndBase64Encode(
              Collections.singletonList(connection.selectRowCount(selectConditions.get(0))))).build();
    }
    catch (final Exception e) {
      LOG.error(ERROR_WHILE_SELECTING, e);
      return Response.serverError().entity(e.getMessage()).build();
    }
  }

  @GET
  @Consumes(MediaType.APPLICATION_OCTET_STREAM)
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  public Response select(@Context final HttpServletRequest request, @Context final HttpHeaders headers,
                         @QueryParam("domainId") final String domainId,
                         @QueryParam("condition") final String condition) {
    final RemoteEntityConnection connection = authenticate(request, headers, domainId);
    try {
      if (condition == null) {
        return Response.ok().build();
      }
      final List<EntitySelectCondition> selectConditions = Util.base64DecodeAndDeserialize(condition);
      return Response.ok(Util.serializeAndBase64Encode(connection.selectMany(selectConditions.get(0)))).build();
    }
    catch (final Exception e) {
      LOG.error(ERROR_WHILE_SELECTING, e);
      return Response.serverError().entity(e.getMessage()).build();
    }
  }

  @POST
  @Consumes(MediaType.APPLICATION_OCTET_STREAM)
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  public Response insert(@Context final HttpServletRequest request, @Context final HttpHeaders headers,
                         @QueryParam("domainId") final String domainId,
                         @QueryParam("entities") final String entities) {
    final RemoteEntityConnection connection = authenticate(request, headers, domainId);
    try {
      return Response.ok(Util.serializeAndBase64Encode(connection.insert(Util.base64DecodeAndDeserialize(entities)))).build();
    }
    catch (final Exception e) {
      LOG.error("Error while inserting", e);
      return Response.serverError().entity(e.getMessage()).build();
    }
  }

  @PUT
  @Consumes(MediaType.APPLICATION_OCTET_STREAM)
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  public Response save(@Context final HttpServletRequest request, @Context final HttpHeaders headers,
                       @QueryParam("domainId") final String domainId,
                       @QueryParam("entities") final String entities) {
    final RemoteEntityConnection connection = authenticate(request, headers, domainId);
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
      LOG.error("Error while saving", e);
      return Response.serverError().entity(e.getMessage()).build();
    }
  }

  @DELETE
  @Consumes(MediaType.APPLICATION_OCTET_STREAM)
  public Response delete(@Context final HttpServletRequest request, @Context final HttpHeaders headers,
                         @QueryParam("domainId") final String domainId,
                         @QueryParam("condition") final String condition) {
    final RemoteEntityConnection connection = authenticate(request, headers, domainId);
    try {
      final List<EntityCondition> conditions = Util.base64DecodeAndDeserialize(condition);
      connection.delete(conditions.get(0));

      return Response.ok().build();
    }
    catch (final Exception e) {
      LOG.error("Error while deleting", e);
      return Response.serverError().entity(e.getMessage()).build();
    }
  }

  private RemoteEntityConnection authenticate(final HttpServletRequest request, final HttpHeaders headers,
                                              final String domainId) {
    if (server == null) {
      throw new IllegalStateException("EntityConnectionServer has not been set for REST service");
    }

    final UUID clientId = getClientId(request, headers);
    final User user = getUser(headers);
    try {
      final Map<String, Object> parameters = new HashMap<>(2);
      parameters.put(RemoteEntityConnectionProvider.REMOTE_CLIENT_DOMAIN_ID, domainId);
      parameters.put(Server.CLIENT_HOST_KEY, request.getRemoteHost());

      return (RemoteEntityConnection) server.connect(Clients.connectionRequest(user, clientId,
              EntityRESTService.class.getName(), parameters));
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
    EntityRESTService.server = server;
  }

  private static UUID getClientId(final HttpServletRequest request, final HttpHeaders headers) {
    final List<String> clientIdHeaders = headers.getRequestHeader(CLIENT_ID);
    if (Util.nullOrEmpty(clientIdHeaders)) {
      throw new WebApplicationException(CLIENT_ID + " header parameter is missing", Response.Status.UNAUTHORIZED);
    }
    final UUID clientId = UUID.fromString(clientIdHeaders.get(0));
    final HttpSession session = request.getSession();
    final UUID sessionClientId = (UUID) session.getAttribute(CLIENT_ID);
    if (sessionClientId == null) {
      session.setAttribute(CLIENT_ID, clientId);
    }
    else if (!clientId.equals(sessionClientId)) {
      session.invalidate();
      session.setAttribute(CLIENT_ID, clientId);
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