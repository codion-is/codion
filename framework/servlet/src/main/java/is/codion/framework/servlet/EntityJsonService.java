/*
 * Copyright (c) 2013 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
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
import jakarta.inject.Singleton;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static is.codion.plugin.jackson.json.domain.EntityObjectMapperFactory.entityObjectMapperFactory;

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
  public Response getEntities(@Context HttpServletRequest request, @Context HttpHeaders headers) {
    try {
      RemoteEntityConnection connection = authenticate(request, headers);

      return Response.ok(Serializer.serialize(connection.entities())).build();
    }
    catch (Exception e) {
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
  public Response close(@Context HttpServletRequest request, @Context HttpHeaders headers) {
    try {
      RemoteEntityConnection connection = authenticate(request, headers);
      request.getSession().invalidate();
      connection.close();

      return Response.ok().build();
    }
    catch (Exception e) {
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
  public Response isTransactionOpen(@Context HttpServletRequest request, @Context HttpHeaders headers) {
    try {
      RemoteEntityConnection connection = authenticate(request, headers);

      EntityObjectMapper entityObjectMapper = getEntityObjectMapper(connection.entities());

      return Response.ok(entityObjectMapper.writeValueAsString(connection.isTransactionOpen())).type(MediaType.APPLICATION_JSON_TYPE).build();
    }
    catch (Exception e) {
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
  public Response beginTransaction(@Context HttpServletRequest request, @Context HttpHeaders headers) {
    try {
      RemoteEntityConnection connection = authenticate(request, headers);

      connection.beginTransaction();

      return Response.ok().type(MediaType.APPLICATION_JSON_TYPE).build();
    }
    catch (Exception e) {
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
  public Response commitTransaction(@Context HttpServletRequest request, @Context HttpHeaders headers) {
    try {
      RemoteEntityConnection connection = authenticate(request, headers);

      connection.commitTransaction();

      return Response.ok().type(MediaType.APPLICATION_JSON_TYPE).build();
    }
    catch (Exception e) {
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
  public Response rollbackTransaction(@Context HttpServletRequest request, @Context HttpHeaders headers) {
    try {
      RemoteEntityConnection connection = authenticate(request, headers);

      connection.rollbackTransaction();

      return Response.ok().type(MediaType.APPLICATION_JSON_TYPE).build();
    }
    catch (Exception e) {
      return logAndGetExceptionResponse(e);
    }
  }

  @POST
  @Path("setQueryCacheEnabled")
  public Response setQueryCacheEnabled(@Context HttpServletRequest request, @Context HttpHeaders headers) {
    try {
      RemoteEntityConnection connection = authenticate(request, headers);
      connection.setQueryCacheEnabled(deserialize(request));

      return Response.ok().type(MediaType.APPLICATION_JSON_TYPE).build();
    }
    catch (Exception e) {
      return logAndGetExceptionResponse(e);
    }
  }

  /**
   * Checks if the query cache is enabled
   * @param request the servlet request
   * @param headers the headers
   * @return a response
   */
  @POST
  @Path("isQueryCacheEnabled")
  public Response isQueryCacheEnabled(@Context HttpServletRequest request, @Context HttpHeaders headers) {
    try {
      RemoteEntityConnection connection = authenticate(request, headers);

      EntityObjectMapper entityObjectMapper = getEntityObjectMapper(connection.entities());

      return Response.ok(entityObjectMapper.writeValueAsString(connection.isQueryCacheEnabled())).type(MediaType.APPLICATION_JSON_TYPE).build();
    }
    catch (Exception e) {
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
  public Response procedure(@Context HttpServletRequest request, @Context HttpHeaders headers) {
    try {
      RemoteEntityConnection connection = authenticate(request, headers);
      List<Object> parameters = deserialize(request);
      Object argument = parameters.size() > 1 ? parameters.get(1) : null;

      connection.executeProcedure((ProcedureType<? extends EntityConnection, Object>) parameters.get(0), argument);

      return Response.ok().build();
    }
    catch (Exception e) {
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
  public Response function(@Context HttpServletRequest request, @Context HttpHeaders headers) {
    try {
      RemoteEntityConnection connection = authenticate(request, headers);
      List<Object> parameters = deserialize(request);
      Object argument = parameters.size() > 1 ? parameters.get(1) : null;

      return Response.ok(Serializer.serialize(connection.executeFunction(
              (FunctionType<? extends EntityConnection, Object, Object>) parameters.get(0), argument))).build();
    }
    catch (Exception e) {
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
  public Response report(@Context HttpServletRequest request, @Context HttpHeaders headers) {
    try {
      RemoteEntityConnection connection = authenticate(request, headers);
      List<Object> parameters = deserialize(request);

      return Response.ok(Serializer.serialize(connection.fillReport((ReportType<?, ?, Object>) parameters.get(0), parameters.get(1)))).build();
    }
    catch (Exception e) {
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
  public Response dependencies(@Context HttpServletRequest request, @Context HttpHeaders headers) {
    try {
      RemoteEntityConnection connection = authenticate(request, headers);

      EntityObjectMapper entityObjectMapper = getEntityObjectMapper(connection.entities());
      List<Entity> entities = entityObjectMapper.deserializeEntities(request.getInputStream());

      Map<String, Collection<Entity>> dependencies = new HashMap<>();
      connection.selectDependencies(entities).forEach((entityType, deps) -> dependencies.put(entityType.name(), deps));

      return Response.ok(entityObjectMapper.writeValueAsString(dependencies)).type(MediaType.APPLICATION_JSON_TYPE).build();
    }
    catch (Exception e) {
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
  public Response count(@Context HttpServletRequest request, @Context HttpHeaders headers) {
    try {
      RemoteEntityConnection connection = authenticate(request, headers);

      ConditionObjectMapper conditionObjectMapper = getConditionObjectMapper(connection.entities());

      return Response.ok(conditionObjectMapper.entityObjectMapper().writeValueAsString(connection.rowCount(conditionObjectMapper
              .readValue(request.getInputStream(), Condition.class)))).type(MediaType.APPLICATION_JSON_TYPE).build();
    }
    catch (Exception e) {
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
  public Response values(@Context HttpServletRequest request, @Context HttpHeaders headers) {
    try {
      RemoteEntityConnection connection = authenticate(request, headers);

      Entities entities = connection.entities();
      ConditionObjectMapper mapper = getConditionObjectMapper(entities);
      JsonNode jsonNode = mapper.readTree(request.getInputStream());
      EntityType entityType = entities.domainType().entityType(jsonNode.get("entityType").asText());
      Attribute<?> attribute = entities.definition(entityType).attribute(jsonNode.get("attribute").textValue());
      Condition condition = null;
      JsonNode conditionNode = jsonNode.get("condition");
      if (conditionNode != null) {
        condition = mapper.readValue(conditionNode.toString(), Condition.class);
      }

      return Response.ok(mapper.entityObjectMapper().writeValueAsString(connection.select(attribute, condition)))
              .type(MediaType.APPLICATION_JSON_TYPE).build();
    }
    catch (Exception e) {
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
  public Response selectByKey(@Context HttpServletRequest request, @Context HttpHeaders headers) {
    try {
      RemoteEntityConnection connection = authenticate(request, headers);

      EntityObjectMapper entityObjectMapper = getEntityObjectMapper(connection.entities());
      List<Key> keysFromJson = entityObjectMapper.deserializeKeys(request.getInputStream());
      List<Entity> entities = connection.select(keysFromJson);

      return Response.ok(entityObjectMapper.writeValueAsString(entities)).type(MediaType.APPLICATION_JSON_TYPE).build();
    }
    catch (Exception e) {
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
  public Response select(@Context HttpServletRequest request, @Context HttpHeaders headers) {
    try {
      RemoteEntityConnection connection = authenticate(request, headers);

      ConditionObjectMapper mapper = getConditionObjectMapper(connection.entities());
      SelectCondition selectConditionJson = mapper
              .readValue(request.getInputStream(), SelectCondition.class);

      return Response.ok(mapper.entityObjectMapper().writeValueAsString(connection.select(selectConditionJson)))
              .type(MediaType.APPLICATION_JSON_TYPE).build();
    }
    catch (Exception e) {
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
  public Response insert(@Context HttpServletRequest request, @Context HttpHeaders headers) {
    try {
      RemoteEntityConnection connection = authenticate(request, headers);

      EntityObjectMapper mapper = getEntityObjectMapper(connection.entities());
      List<Entity> entities = mapper.deserializeEntities(request.getInputStream());

      return Response.ok(mapper.writeValueAsString(connection.insert(entities))).type(MediaType.APPLICATION_JSON_TYPE).build();
    }
    catch (Exception e) {
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
  public Response update(@Context HttpServletRequest request, @Context HttpHeaders headers) {
    try {
      RemoteEntityConnection connection = authenticate(request, headers);

      EntityObjectMapper mapper = getEntityObjectMapper(connection.entities());
      List<Entity> entities = mapper.deserializeEntities(request.getInputStream());

      return Response.ok(mapper.writeValueAsString(connection.update(entities))).type(MediaType.APPLICATION_JSON_TYPE).build();
    }
    catch (Exception e) {
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
  public Response updateByCondition(@Context HttpServletRequest request, @Context HttpHeaders headers) {
    try {
      RemoteEntityConnection connection = authenticate(request, headers);

      ConditionObjectMapper mapper = getConditionObjectMapper(connection.entities());
      UpdateCondition updateCondition = mapper.readValue(request.getInputStream(), UpdateCondition.class);

      return Response.ok(mapper.entityObjectMapper().writeValueAsString(connection.update(updateCondition)))
              .type(MediaType.APPLICATION_JSON_TYPE).build();
    }
    catch (Exception e) {
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
  public Response delete(@Context HttpServletRequest request, @Context HttpHeaders headers) {
    try {
      RemoteEntityConnection connection = authenticate(request, headers);

      ConditionObjectMapper mapper = getConditionObjectMapper(connection.entities());
      Condition deleteCondition = mapper.readValue(request.getInputStream(), Condition.class);

      return Response.ok(mapper.entityObjectMapper().writeValueAsString(connection.delete(deleteCondition)))
              .type(MediaType.APPLICATION_JSON_TYPE).build();
    }
    catch (Exception e) {
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
  public Response deleteByKey(@Context HttpServletRequest request, @Context HttpHeaders headers) {
    try {
      RemoteEntityConnection connection = authenticate(request, headers);

      EntityObjectMapper mapper = getEntityObjectMapper(connection.entities());
      List<Key> deleteKeys = mapper.deserializeKeys(request.getInputStream());
      connection.delete(deleteKeys);

      return Response.ok().type(MediaType.APPLICATION_JSON_TYPE).build();
    }
    catch (Exception e) {
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
  public Response writeBlob(@Context HttpServletRequest request, @Context HttpHeaders headers) {
    try {
      RemoteEntityConnection connection = authenticate(request, headers);
      List<Object> parameters = deserialize(request);

      connection.writeBlob((Key) parameters.get(0), (Attribute<byte[]>) parameters.get(1), (byte[]) parameters.get(2));

      return Response.ok().build();
    }
    catch (Exception e) {
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
  public Response readBlob(@Context HttpServletRequest request, @Context HttpHeaders headers) {
    try {
      RemoteEntityConnection connection = authenticate(request, headers);
      List<Object> parameters = deserialize(request);

      return Response.ok(Serializer.serialize(connection.readBlob((Key) parameters.get(0), (Attribute<byte[]>) parameters.get(1)))).build();
    }
    catch (Exception e) {
      return logAndGetExceptionResponse(e);
    }
  }

  private EntityObjectMapper getEntityObjectMapper(Entities entities) {
    return entityObjectMappers.computeIfAbsent(entities.domainType(), domainType ->
            entityObjectMapperFactory(domainType).createEntityObjectMapper(entities));
  }

  private ConditionObjectMapper getConditionObjectMapper(Entities entities) {
    return conditionObjectMappers.computeIfAbsent(entities.domainType(), domainType ->
            new ConditionObjectMapper(getEntityObjectMapper(entities)));
  }

  private static <T> T deserialize(HttpServletRequest request) throws IOException, ClassNotFoundException {
    return (T) new ObjectInputStream(request.getInputStream()).readObject();
  }
}