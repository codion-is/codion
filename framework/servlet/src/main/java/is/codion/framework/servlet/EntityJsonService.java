/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.servlet;

import is.codion.common.Serializer;
import is.codion.common.db.operation.FunctionType;
import is.codion.common.db.operation.ProcedureType;
import is.codion.common.db.report.ReportType;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.condition.Condition;
import is.codion.framework.db.condition.SelectCondition;
import is.codion.framework.db.condition.UpdateCondition;
import is.codion.framework.db.rmi.RemoteEntityConnection;
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.Key;
import is.codion.plugin.jackson.json.db.ConditionObjectMapper;
import is.codion.plugin.jackson.json.domain.EntityObjectMapper;

import com.fasterxml.jackson.databind.JsonNode;

import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static is.codion.plugin.jackson.json.domain.EntityObjectMapperFactory.entityObjectMapperFactory;
import static java.util.Collections.emptyList;

/**
 * A service for dealing with entities in JSON format.
 */
@Path("/")
@Singleton
@Consumes({MediaType.APPLICATION_OCTET_STREAM, MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_OCTET_STREAM, MediaType.APPLICATION_JSON})
public final class EntityJsonService extends AbstractEntityService {

  private final Map<DomainType, EntityObjectMapper> entityObjectMappers = new ConcurrentHashMap<>();
  private final Map<DomainType, ConditionObjectMapper> conditionObjectMappers = new ConcurrentHashMap<>();

  /**
   * Returns the underlying domain entities
   * @param request the servlet request
   * @param headers the headers
   * @return a response
   */
  @POST
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  @Path("getEntities")
  public Response getEntities(@Context final HttpServletRequest request, @Context final HttpHeaders headers) {
    try {
      final RemoteEntityConnection connection = authenticate(request, headers);

      return Response.ok(Serializer.serialize(connection.getEntities())).build();
    }
    catch (final Exception e) {
      return logAndGetExceptionResponse(e);
    }
  }

  /**
   * Disconnects the underlying connection
   * @param request the servlet request
   * @param headers the headers
   * @return a response
   */
  @POST
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  @Path("close")
  public Response close(@Context final HttpServletRequest request, @Context final HttpHeaders headers) {
    try {
      final RemoteEntityConnection connection = authenticate(request, headers);
      request.getSession().invalidate();
      connection.close();

      return Response.ok().build();
    }
    catch (final Exception e) {
      return logAndGetExceptionResponse(e);
    }
  }

  /**
   * Checks if a transaction is open
   * @param request the servlet request
   * @param headers the headers
   * @return a response
   */
  @POST
  @Path("isTransactionOpen")
  public Response isTransactionOpen(@Context final HttpServletRequest request, @Context final HttpHeaders headers) {
    try {
      final RemoteEntityConnection connection = authenticate(request, headers);

      final EntityObjectMapper entityObjectMapper = getEntityObjectMapper(connection.getEntities());

      return Response.ok(entityObjectMapper.writeValueAsString(connection.isTransactionOpen())).type(MediaType.APPLICATION_JSON_TYPE).build();
    }
    catch (final Exception e) {
      return logAndGetExceptionResponse(e);
    }
  }

  /**
   * Begins a transaction
   * @param request the servlet request
   * @param headers the headers
   * @return a response
   */
  @POST
  @Path("beginTransaction")
  public Response beginTransaction(@Context final HttpServletRequest request, @Context final HttpHeaders headers) {
    try {
      final RemoteEntityConnection connection = authenticate(request, headers);

      connection.beginTransaction();

      return Response.ok().type(MediaType.APPLICATION_JSON_TYPE).build();
    }
    catch (final Exception e) {
      return logAndGetExceptionResponse(e);
    }
  }

  /**
   * Commits a transaction
   * @param request the servlet request
   * @param headers the headers
   * @return a response
   */
  @POST
  @Path("commitTransaction")
  public Response commitTransaction(@Context final HttpServletRequest request, @Context final HttpHeaders headers) {
    try {
      final RemoteEntityConnection connection = authenticate(request, headers);

      connection.commitTransaction();

      return Response.ok().type(MediaType.APPLICATION_JSON_TYPE).build();
    }
    catch (final Exception e) {
      return logAndGetExceptionResponse(e);
    }
  }

  /**
   * Rolls back an open transaction
   * @param request the servlet request
   * @param headers the headers
   * @return a response
   */
  @POST
  @Path("rollbackTransaction")
  public Response rollbackTransaction(@Context final HttpServletRequest request, @Context final HttpHeaders headers) {
    try {
      final RemoteEntityConnection connection = authenticate(request, headers);

      connection.rollbackTransaction();

      return Response.ok().type(MediaType.APPLICATION_JSON_TYPE).build();
    }
    catch (final Exception e) {
      return logAndGetExceptionResponse(e);
    }
  }

  /**
   * Executes a procedure with the given parameters
   * @param request the servlet request
   * @param headers the headers
   * @return a response
   */
  @POST
  @Path("procedure")
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  public Response procedure(@Context final HttpServletRequest request, @Context final HttpHeaders headers) {
    try {
      final RemoteEntityConnection connection = authenticate(request, headers);
      final List<Object> parameters = deserialize(request);
      final List<Object> arguments = parameters.size() > 1 ? (List<Object>) parameters.get(1) : emptyList();

      connection.executeProcedure((ProcedureType<? extends EntityConnection, Object>) parameters.get(0), arguments);

      return Response.ok().build();
    }
    catch (final Exception e) {
      return logAndGetExceptionResponse(e);
    }
  }

  /**
   * Executes a function with the given parameters
   * @param request the servlet request
   * @param headers the headers
   * @return a response
   */
  @POST
  @Path("function")
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  public Response function(@Context final HttpServletRequest request, @Context final HttpHeaders headers) {
    try {
      final RemoteEntityConnection connection = authenticate(request, headers);
      final List<Object> parameters = deserialize(request);
      final List<Object> arguments = parameters.size() > 1 ? (List<Object>) parameters.get(1) : emptyList();

      return Response.ok(Serializer.serialize(connection.executeFunction(
              (FunctionType<? extends EntityConnection, Object, Object>) parameters.get(0), arguments))).build();
    }
    catch (final Exception e) {
      return logAndGetExceptionResponse(e);
    }
  }

  /**
   * Fills the given report
   * @param request the servlet request
   * @param headers the headers
   * @return a response
   */
  @POST
  @Path("report")
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  public Response report(@Context final HttpServletRequest request, @Context final HttpHeaders headers) {
    try {
      final RemoteEntityConnection connection = authenticate(request, headers);
      final List<Object> parameters = deserialize(request);

      return Response.ok(Serializer.serialize(connection.fillReport((ReportType<?, ?, Object>) parameters.get(0), parameters.get(1)))).build();
    }
    catch (final Exception e) {
      return logAndGetExceptionResponse(e);
    }
  }

  /**
   * Returns the entities referencing the given entities via foreign keys, mapped to their respective entityTypes
   * @param request the servlet request
   * @param headers the headers
   * @return a response
   */
  @POST
  @Path("dependencies")
  public Response dependencies(@Context final HttpServletRequest request, @Context final HttpHeaders headers) {
    try {
      final RemoteEntityConnection connection = authenticate(request, headers);

      final EntityObjectMapper entityObjectMapper = getEntityObjectMapper(connection.getEntities());
      final List<Entity> entities = entityObjectMapper.deserializeEntities(request.getInputStream());

      final Map<String, Collection<Entity>> dependencies = new HashMap<>();
      connection.selectDependencies(entities).forEach((entityType, deps) -> dependencies.put(entityType.getName(), deps));

      return Response.ok(entityObjectMapper.writeValueAsString(dependencies)).type(MediaType.APPLICATION_JSON_TYPE).build();
    }
    catch (final Exception e) {
      return logAndGetExceptionResponse(e);
    }
  }

  /**
   * Returns the record count for the given condition
   * @param request the servlet request
   * @param headers the headers
   * @return a response
   */
  @POST
  @Path("count")
  public Response count(@Context final HttpServletRequest request, @Context final HttpHeaders headers) {
    try {
      final RemoteEntityConnection connection = authenticate(request, headers);

      final ConditionObjectMapper conditionObjectMapper = getConditionObjectMapper(connection.getEntities());

      return Response.ok(conditionObjectMapper.getEntityObjectMapper().writeValueAsString(connection.rowCount(conditionObjectMapper
              .readValue(request.getInputStream(), Condition.class)))).type(MediaType.APPLICATION_JSON_TYPE).build();
    }
    catch (final Exception e) {
      return logAndGetExceptionResponse(e);
    }
  }

  /**
   * Selects the values for the given attribute using the given query condition
   * @param request the servlet request
   * @param headers the headers
   * @return a response
   */
  @POST
  @Path("values")
  public Response values(@Context final HttpServletRequest request, @Context final HttpHeaders headers) {
    try {
      final RemoteEntityConnection connection = authenticate(request, headers);

      final Entities entities = connection.getEntities();
      final ConditionObjectMapper mapper = getConditionObjectMapper(entities);
      final JsonNode jsonNode = mapper.readTree(request.getInputStream());
      final EntityType<Entity> entityType = entities.getDomainType().entityType(jsonNode.get("entityType").asText());
      final Attribute<?> attribute = entities.getDefinition(entityType).getAttribute(jsonNode.get("attribute").textValue());
      Condition condition = null;
      final JsonNode conditionNode = jsonNode.get("condition");
      if (conditionNode != null) {
        condition = mapper.readValue(conditionNode.toString(), Condition.class);
      }

      return Response.ok(mapper.getEntityObjectMapper().writeValueAsString(connection.select(attribute, condition)))
              .type(MediaType.APPLICATION_JSON_TYPE).build();
    }
    catch (final Exception e) {
      return logAndGetExceptionResponse(e);
    }
  }

  /**
   * Returns the entities for the given keys
   * @param request the servlet request
   * @param headers the headers
   * @return a response
   */
  @POST
  @Path("selectByKey")
  public Response selectByKey(@Context final HttpServletRequest request, @Context final HttpHeaders headers) {
    try {
      final RemoteEntityConnection connection = authenticate(request, headers);

      final EntityObjectMapper entityObjectMapper = getEntityObjectMapper(connection.getEntities());
      final List<Key> keysFromJson = entityObjectMapper.deserializeKeys(request.getInputStream());
      final List<Entity> entities = connection.select(keysFromJson);

      return Response.ok(entityObjectMapper.writeValueAsString(entities)).type(MediaType.APPLICATION_JSON_TYPE).build();
    }
    catch (final Exception e) {
      return logAndGetExceptionResponse(e);
    }
  }

  /**
   * Returns the entities for the given query condition
   * @param request the servlet request
   * @param headers the headers
   * @return a response
   */
  @POST
  @Path("select")
  public Response select(@Context final HttpServletRequest request, @Context final HttpHeaders headers) {
    try {
      final RemoteEntityConnection connection = authenticate(request, headers);

      final ConditionObjectMapper mapper = getConditionObjectMapper(connection.getEntities());
      final SelectCondition selectConditionJson = mapper
              .readValue(request.getInputStream(), SelectCondition.class);

      return Response.ok(mapper.getEntityObjectMapper().writeValueAsString(connection.select(selectConditionJson)))
              .type(MediaType.APPLICATION_JSON_TYPE).build();
    }
    catch (final Exception e) {
      return logAndGetExceptionResponse(e);
    }
  }

  /**
   * Inserts the given entities, returning their keys
   * @param request the servlet request
   * @param headers the headers
   * @return a response
   */
  @POST
  @Path("insert")
  public Response insert(@Context final HttpServletRequest request, @Context final HttpHeaders headers) {
    try {
      final RemoteEntityConnection connection = authenticate(request, headers);

      final EntityObjectMapper mapper = getEntityObjectMapper(connection.getEntities());
      final List<Entity> entities = mapper.deserializeEntities(request.getInputStream());

      return Response.ok(mapper.writeValueAsString(connection.insert(entities))).type(MediaType.APPLICATION_JSON_TYPE).build();
    }
    catch (final Exception e) {
      return logAndGetExceptionResponse(e);
    }
  }

  /**
   * Updates the given entities
   * @param request the servlet request
   * @param headers the headers
   * @return a response
   */
  @POST
  @Path("update")
  public Response update(@Context final HttpServletRequest request, @Context final HttpHeaders headers) {
    try {
      final RemoteEntityConnection connection = authenticate(request, headers);

      final EntityObjectMapper mapper = getEntityObjectMapper(connection.getEntities());
      final List<Entity> entities = mapper.deserializeEntities(request.getInputStream());

      return Response.ok(mapper.writeValueAsString(connection.update(entities))).type(MediaType.APPLICATION_JSON_TYPE).build();
    }
    catch (final Exception e) {
      return logAndGetExceptionResponse(e);
    }
  }

  /**
   * Performs an update according to the given condition
   * @param request the servlet request
   * @param headers the headers
   * @return a response
   */
  @POST
  @Path("updateByCondition")
  public Response updateByCondition(@Context final HttpServletRequest request, @Context final HttpHeaders headers) {
    try {
      final RemoteEntityConnection connection = authenticate(request, headers);

      final ConditionObjectMapper mapper = getConditionObjectMapper(connection.getEntities());
      final UpdateCondition updateCondition = mapper.readValue(request.getInputStream(), UpdateCondition.class);

      return Response.ok(mapper.getEntityObjectMapper().writeValueAsString(connection.update(updateCondition)))
              .type(MediaType.APPLICATION_JSON_TYPE).build();
    }
    catch (final Exception e) {
      return logAndGetExceptionResponse(e);
    }
  }

  /**
   * Deletes the entities for the given condition
   * @param request the servlet request
   * @param headers the headers
   * @return a response
   */
  @POST
  @Path("delete")
  public Response delete(@Context final HttpServletRequest request, @Context final HttpHeaders headers) {
    try {
      final RemoteEntityConnection connection = authenticate(request, headers);

      final ConditionObjectMapper mapper = getConditionObjectMapper(connection.getEntities());
      final Condition deleteCondition = mapper.readValue(request.getInputStream(), Condition.class);

      return Response.ok(mapper.getEntityObjectMapper().writeValueAsString(connection.delete(deleteCondition)))
              .type(MediaType.APPLICATION_JSON_TYPE).build();
    }
    catch (final Exception e) {
      return logAndGetExceptionResponse(e);
    }
  }

  /**
   * Deletes the entities for the given keys
   * @param request the servlet request
   * @param headers the headers
   * @return a response
   */
  @POST
  @Path("deleteByKey")
  public Response deleteByKey(@Context final HttpServletRequest request, @Context final HttpHeaders headers) {
    try {
      final RemoteEntityConnection connection = authenticate(request, headers);

      final EntityObjectMapper mapper = getEntityObjectMapper(connection.getEntities());
      final List<Key> deleteKeys = mapper.deserializeKeys(request.getInputStream());
      connection.delete(deleteKeys);

      return Response.ok().type(MediaType.APPLICATION_JSON_TYPE).build();
    }
    catch (final Exception e) {
      return logAndGetExceptionResponse(e);
    }
  }

  /**
   * Writes a BLOB value
   * @param request the servlet request
   * @param headers the headers
   * @return a response
   */
  @POST
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  @Path("writeBlob")
  public Response writeBlob(@Context final HttpServletRequest request, @Context final HttpHeaders headers) {
    try {
      final RemoteEntityConnection connection = authenticate(request, headers);
      final List<Object> parameters = deserialize(request);

      connection.writeBlob((Key) parameters.get(0), (Attribute<byte[]>) parameters.get(1), (byte[]) parameters.get(2));

      return Response.ok().build();
    }
    catch (final Exception e) {
      return logAndGetExceptionResponse(e);
    }
  }

  /**
   * Reads a BLOB value
   * @param request the servlet request
   * @param headers the headers
   * @return a response
   */
  @POST
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  @Path("readBlob")
  public Response readBlob(@Context final HttpServletRequest request, @Context final HttpHeaders headers) {
    try {
      final RemoteEntityConnection connection = authenticate(request, headers);
      final List<Object> parameters = deserialize(request);

      return Response.ok(Serializer.serialize(connection.readBlob((Key) parameters.get(0), (Attribute<byte[]>) parameters.get(1)))).build();
    }
    catch (final Exception e) {
      return logAndGetExceptionResponse(e);
    }
  }

  private EntityObjectMapper getEntityObjectMapper(final Entities entities) {
    return entityObjectMappers.computeIfAbsent(entities.getDomainType(), domainType ->
            entityObjectMapperFactory(domainType).createEntityObjectMapper(entities));
  }

  private ConditionObjectMapper getConditionObjectMapper(final Entities entities) {
    return conditionObjectMappers.computeIfAbsent(entities.getDomainType(), domainType ->
            new ConditionObjectMapper(getEntityObjectMapper(entities)));
  }

  private static <T> T deserialize(final HttpServletRequest request) throws IOException, ClassNotFoundException {
    return (T) new ObjectInputStream(request.getInputStream()).readObject();
  }
}