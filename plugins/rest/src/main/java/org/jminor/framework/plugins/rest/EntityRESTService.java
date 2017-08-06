/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.plugins.rest;

import org.jminor.common.Conjunction;
import org.jminor.common.User;
import org.jminor.common.Util;
import org.jminor.common.db.condition.Condition;
import org.jminor.common.db.condition.Conditions;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.server.Clients;
import org.jminor.common.server.Server;
import org.jminor.common.server.ServerException;
import org.jminor.framework.db.RemoteEntityConnection;
import org.jminor.framework.db.condition.EntityConditions;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.EntityUtil;
import org.jminor.framework.domain.Property;
import org.jminor.framework.plugins.json.EntityJSONParser;

import org.json.JSONException;
import org.json.JSONObject;

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
import javax.xml.bind.DatatypeConverter;
import java.rmi.RemoteException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A proof of concept implementation of a REST service for dealing with Entities
 */
@Path("/")
public final class EntityRESTService extends Application {

  public static final String BY_KEY_PATH = "key";
  public static final String BY_VALUE_PATH = "value";
  public static final String AUTHORIZATION = "Authorization";

  private static final String CLIENT_ID = "clientId";

  private static Server server;

  @GET
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Path(BY_KEY_PATH)
  public Response select(@Context final HttpServletRequest request, @Context final HttpHeaders headers,
                         @QueryParam("keys") final String keys) {
    final RemoteEntityConnection connection = authenticate(request, headers);
    try {
      return Response.ok(new EntityJSONParser().serialize(connection.selectMany(EntityJSONParser.deserializeKeys(keys)))).build();
    }
    catch (final Exception e) {
      return Response.serverError().entity(e.getMessage()).build();
    }
  }

  @GET
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Path(BY_VALUE_PATH)
  public Response select(@Context final HttpServletRequest request, @Context final HttpHeaders headers,
                         @QueryParam("entityID") final String entityID,
                         @QueryParam("conditionType") final Condition.Type conditionType,
                         @QueryParam("values") final String values) {
    final RemoteEntityConnection connection = authenticate(request, headers);
    try {
      return Response.ok(new EntityJSONParser().serialize(connection.selectMany(
              EntityConditions.selectCondition(entityID, createPropertyCondition(entityID, conditionType, values))))).build();
    }
    catch (final Exception e) {
      return Response.serverError().entity(e.getMessage()).build();
    }
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response insert(@Context final HttpServletRequest request, @Context final HttpHeaders headers,
                         @QueryParam("entities") final String entities) {
    final RemoteEntityConnection connection = authenticate(request, headers);
    try {
      return Response.ok(new EntityJSONParser().serializeKeys(connection.insert(EntityJSONParser.deserializeEntities(entities)))).build();
    }
    catch (final Exception e) {
      return Response.serverError().entity(e.getMessage()).build();
    }
  }

  @PUT
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response save(@Context final HttpServletRequest request, @Context final HttpHeaders headers,
                       @QueryParam("entities") final String entities) {
    final RemoteEntityConnection connection = authenticate(request, headers);
    try {
      final List<Entity> parsedEntities = EntityJSONParser.deserializeEntities(entities);
      final List<Entity> toInsert = new ArrayList<>(parsedEntities.size());
      final List<Entity> toUpdate = new ArrayList<>(parsedEntities.size());
      for (final Entity entity : parsedEntities) {
        if (EntityUtil.isEntityNew(entity)) {
          toInsert.add(entity);
        }
        else {
          toUpdate.add(entity);
        }
      }
      final List<Entity> savedEntities = saveEntities(connection, toInsert, toUpdate);

      return Response.ok(new EntityJSONParser().serialize(savedEntities)).build();
    }
    catch (final Exception e) {
      return Response.serverError().entity(e.getMessage()).build();
    }
  }

  @DELETE
  @Consumes(MediaType.APPLICATION_JSON)
  @Path(BY_KEY_PATH)
  public Response delete(@Context final HttpServletRequest request, @Context final HttpHeaders headers,
                         @QueryParam("keys") final String keys) {
    final RemoteEntityConnection connection = authenticate(request, headers);
    try {
      connection.delete(EntityJSONParser.deserializeKeys(keys));

      return Response.ok().build();
    }
    catch (final Exception e) {
      return Response.serverError().entity(e.getMessage()).build();
    }
  }

  @DELETE
  @Path(BY_VALUE_PATH)
  public Response delete(@Context final HttpServletRequest request, @Context final HttpHeaders headers,
                         @QueryParam("entityID") final String entityID,
                         @QueryParam("conditionType") final Condition.Type conditionType,
                         @QueryParam("values") final String values) {
    final RemoteEntityConnection connection = authenticate(request, headers);
    try {
      connection.delete(EntityConditions.condition(entityID, createPropertyCondition(entityID, conditionType, values)));

      return Response.ok().build();
    }
    catch (final Exception e) {
      return Response.serverError().entity(e.getMessage()).build();
    }
  }

  private RemoteEntityConnection authenticate(final HttpServletRequest request, final HttpHeaders headers) {
    if (server == null) {
      throw new IllegalStateException("EntityConnectionServer has not been set for REST service");
    }
    final HttpSession session = request.getSession();
    UUID clientId = (UUID) session.getAttribute(CLIENT_ID);
    if (clientId == null) {
      clientId = UUID.randomUUID();
      session.setAttribute(CLIENT_ID, clientId);
    }

    final List<String> basic = headers.getRequestHeader(AUTHORIZATION);
    if (Util.nullOrEmpty(basic)) {
      throw new WebApplicationException(Response.Status.UNAUTHORIZED);
    }

    String auth = basic.get(0);
    if (!auth.toLowerCase().startsWith("basic ")) {
      throw new WebApplicationException(Response.Status.UNAUTHORIZED);
    }

    auth = auth.replaceFirst("[B|b]asic ", "");
    final byte[] decodedBytes = DatatypeConverter.parseBase64Binary(auth);
    final User user = User.parseUser(new String(decodedBytes));
    try {
      return (RemoteEntityConnection) server.connect(Clients.connectionRequest(user, clientId, EntityRESTService.class.getName()));
    }
    catch (final ServerException.AuthenticationException ae) {
      throw new WebApplicationException(ae, Response.Status.UNAUTHORIZED);
    }
    catch (final Exception e) {
      throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  static void setServer(final Server server) {
    EntityRESTService.server = server;
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

  private static Condition.Set<Property.ColumnProperty> createPropertyCondition(final String entityID, final Condition.Type conditionType,
                                                                                final String values) throws JSONException, ParseException {
    if (conditionType == null || Util.nullOrEmpty(values)) {
      return null;
    }
    final JSONObject jsonObject = new JSONObject(values);
    final Condition.Set<Property.ColumnProperty> set = Conditions.conditionSet(Conjunction.AND);
    for (final String propertyID : JSONObject.getNames(jsonObject)) {
      final Property.ColumnProperty property = Entities.getColumnProperty(entityID, propertyID);
      final Condition<Property.ColumnProperty> condition = EntityConditions.propertyCondition(property,
              conditionType, EntityJSONParser.parseValue(property, jsonObject));
      set.add(condition);
    }

    return set;
  }
}