/*
 * Copyright (c) 2013 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.servlet;

import is.codion.common.Serializer;
import is.codion.common.db.operation.FunctionType;
import is.codion.common.db.operation.ProcedureType;
import is.codion.common.db.report.ReportType;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.condition.Condition;
import is.codion.framework.db.condition.UpdateCondition;
import is.codion.framework.db.rmi.RemoteEntityConnection;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.Key;

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
import java.util.List;

/**
 * A service for dealing with entities
 */
@Path("/")
@Singleton
@Consumes(MediaType.APPLICATION_OCTET_STREAM)
@Produces(MediaType.APPLICATION_OCTET_STREAM)
public final class EntityService extends AbstractEntityService {

  /**
   * Returns the underlying domain entities
   * @param request the servlet request
   * @param headers the headers
   * @return a response
   */
  @POST
  @Path("entities")
  public Response entities(@Context HttpServletRequest request, @Context HttpHeaders headers) {
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

      return Response.ok(Serializer.serialize(connection.isTransactionOpen())).build();
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

      return Response.ok().build();
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

      return Response.ok().build();
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

      return Response.ok().build();
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

      return Response.ok().build();
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

      return Response.ok(Serializer.serialize(connection.isQueryCacheEnabled())).build();
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

      return Response.ok(Serializer.serialize(connection.selectDependencies(deserialize(request)))).build();
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

      return Response.ok(Serializer.serialize(connection.rowCount(deserialize(request)))).build();
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
      List<Object> parameters = deserialize(request);

      return Response.ok(Serializer.serialize(connection.select((Attribute<?>) parameters.get(0), (Condition) parameters.get(1)))).build();
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
      List<Key> keys = deserialize(request);

      return Response.ok(Serializer.serialize(connection.select(keys))).build();
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
      Condition selectCondition = deserialize(request);

      return Response.ok(Serializer.serialize(connection.select(selectCondition))).build();
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

      return Response.ok(Serializer.serialize(connection.insert((List<Entity>) deserialize(request)))).build();
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

      return Response.ok(Serializer.serialize(connection.update((List<Entity>) deserialize(request)))).build();
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

      return Response.ok(Serializer.serialize(connection.update((UpdateCondition) deserialize(request)))).build();
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
      Condition condition = deserialize(request);

      return Response.ok(Serializer.serialize(connection.delete(condition))).build();
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
      List<Key> keys = deserialize(request);
      connection.delete(keys);

      return Response.ok().build();
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

  private static <T> T deserialize(HttpServletRequest request) throws IOException, ClassNotFoundException {
    return (T) new ObjectInputStream(request.getInputStream()).readObject();
  }
}