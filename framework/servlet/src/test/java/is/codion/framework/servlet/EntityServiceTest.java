/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2023 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.framework.servlet;

import is.codion.common.db.database.Database;
import is.codion.common.rmi.client.Clients;
import is.codion.common.rmi.server.ServerConfiguration;
import is.codion.common.rmi.server.exception.ServerAuthenticationException;
import is.codion.common.utilities.Serializer;
import is.codion.common.utilities.user.User;
import is.codion.common.utilities.version.Version;
import is.codion.framework.db.EntityConnection.Count;
import is.codion.framework.db.EntityConnection.Select;
import is.codion.framework.db.EntityConnection.Update;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.condition.Condition;
import is.codion.framework.json.db.DatabaseObjectMapper;
import is.codion.framework.json.db.ErrorEnvelope;
import is.codion.framework.json.db.ErrorKind;
import is.codion.framework.json.domain.EntityObjectMapper;
import is.codion.framework.server.EntityServer;
import is.codion.framework.server.EntityServerConfiguration;
import is.codion.framework.servlet.TestDomain.Department;
import is.codion.framework.servlet.TestDomain.Employee;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static is.codion.framework.domain.entity.condition.Condition.keys;
import static is.codion.framework.json.db.DatabaseObjectMapper.databaseObjectMapper;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;

public class EntityServiceTest {

	private static final Entities ENTITIES = new TestDomain().entities();

	private static final DatabaseObjectMapper OBJECT_MAPPER =
					databaseObjectMapper(new TestObjectMapperFactory().entityObjectMapper(ENTITIES));

	private static final User UNIT_TEST_USER =
					User.parse(System.getProperty("codion.test.user", "scott:tiger"));
	private static final int OK = 200;
	private static final int BAD_REQUEST = 400;
	private static final int UNAUTHORIZED = 401;
	private static final int INTERNAL_SERVER_ERROR = 500;

	private static String HOSTNAME;
	private static String SERVER_BASEURL;
	private static String SERVER_JSON_BASEURL;

	private static final String CLIENT_ID_STRING = UUID.randomUUID().toString();

	private static EntityServer server;

	private static final HttpClient HTTP_CLIENT = createHttpClient();

	@BeforeAll
	public static void setUp() throws Exception {
		EntityServerConfiguration configuration = configure();
		HOSTNAME = Clients.SERVER_HOSTNAME.get();
		SERVER_BASEURL = "http://" + HOSTNAME + ":" + EntityService.PORT.get() + "/entities/serial/";
		SERVER_JSON_BASEURL = "http://" + HOSTNAME + ":" + EntityService.PORT.get() + "/entities/json/";
		server = EntityServer.startServer(configuration);
	}

	@AfterAll
	public static void tearDown() {
		if (server != null) {
			server.shutdown();
		}
		deconfigure();
	}

	@Test
	void entities() throws Exception {
		HttpResponse<byte[]> response = HTTP_CLIENT.send(createRequest("entities"), BodyHandlers.ofByteArray());
		assertEquals(OK, response.statusCode());
		Entities entities = Serializer.deserialize(response.body());
		assertNotNull(entities);

		response = HTTP_CLIENT.send(createJsonRequest("entities"), BodyHandlers.ofByteArray());
		assertEquals(OK, response.statusCode());
		entities = Serializer.deserialize(response.body());
		assertNotNull(entities);
	}

	@Test
	void close() throws Exception {
		HttpResponse<byte[]> response = HTTP_CLIENT.send(createRequest("close"), BodyHandlers.ofByteArray());
		assertEquals(OK, response.statusCode());

		response = HTTP_CLIENT.send(createJsonRequest("close"), BodyHandlers.ofByteArray());
		assertEquals(OK, response.statusCode());
	}

	@Test
	void isTransactionOpen() throws Exception {
		HttpResponse<byte[]> response = HTTP_CLIENT.send(createRequest("isTransactionOpen"), BodyHandlers.ofByteArray());
		assertEquals(OK, response.statusCode());
		boolean value = Serializer.deserialize(response.body());
		assertFalse(value);

		response = HTTP_CLIENT.send(createJsonRequest("isTransactionOpen"), BodyHandlers.ofByteArray());
		assertEquals(OK, response.statusCode());
		value = OBJECT_MAPPER.readValue(new String(response.body(), UTF_8), Boolean.class);
		assertFalse(value);
	}

	@Test
	void startRollbackTransaction() throws Exception {
		HttpResponse<byte[]> response = HTTP_CLIENT.send(createRequest("startTransaction"), BodyHandlers.ofByteArray());
		assertEquals(OK, response.statusCode());
		response = HTTP_CLIENT.send(createRequest("rollbackTransaction"), BodyHandlers.ofByteArray());
		assertEquals(OK, response.statusCode());

		response = HTTP_CLIENT.send(createJsonRequest("startTransaction"), BodyHandlers.ofByteArray());
		assertEquals(OK, response.statusCode());
		response = HTTP_CLIENT.send(createJsonRequest("rollbackTransaction"), BodyHandlers.ofByteArray());
		assertEquals(OK, response.statusCode());
	}

	@Test
	void startCommitTransaction() throws Exception {
		HttpResponse<byte[]> response = HTTP_CLIENT.send(createRequest("startTransaction"), BodyHandlers.ofByteArray());
		assertEquals(OK, response.statusCode());
		response = HTTP_CLIENT.send(createRequest("commitTransaction"), BodyHandlers.ofByteArray());
		assertEquals(OK, response.statusCode());

		response = HTTP_CLIENT.send(createJsonRequest("startTransaction"), BodyHandlers.ofByteArray());
		assertEquals(OK, response.statusCode());
		response = HTTP_CLIENT.send(createJsonRequest("commitTransaction"), BodyHandlers.ofByteArray());
		assertEquals(OK, response.statusCode());
	}

	@Test
	void procedure() throws Exception {
		JsonNodeFactory nodeFactory = OBJECT_MAPPER.getNodeFactory();
		ObjectNode request = OBJECT_MAPPER.createObjectNode();
		request.set("procedureType", nodeFactory.textNode(TestDomain.PROCEDURE.name()));
		request.set("parameter", OBJECT_MAPPER.valueToTree(asList("one", "two")));
		// No argument field since procedure has no arguments
		HttpResponse<byte[]> response = HTTP_CLIENT.send(createJsonRequest("procedure",
						BodyPublishers.ofString(request.toString())), BodyHandlers.ofByteArray());
		assertEquals(OK, response.statusCode());
	}

	@Test
	void function() throws Exception {
		JsonNodeFactory nodeFactory = OBJECT_MAPPER.getNodeFactory();
		ObjectNode request = OBJECT_MAPPER.createObjectNode();
		request.set("functionType", nodeFactory.textNode(TestDomain.FUNCTION.name()));
		request.set("parameter", OBJECT_MAPPER.valueToTree(asList("one", "two")));
		// No argument field since function has no arguments
		HttpResponse<byte[]> response = HTTP_CLIENT.send(createJsonRequest("function",
						BodyPublishers.ofString(request.toString())), BodyHandlers.ofByteArray());
		assertEquals(OK, response.statusCode());
		//the return value is json, not a serialized object
		assertTrue(response.headers().firstValue("Content-Type").orElseThrow().startsWith("application/json"));
		List<Integer> result = OBJECT_MAPPER.readValue(new String(response.body(), UTF_8), new TypeReference<List<Integer>>() {});
		assertEquals(asList(1, 2, 3), result);
	}

	@Test
	void report() throws Exception {
		HttpResponse<byte[]> response = HTTP_CLIENT.send(createRequest("report",
						BodyPublishers.ofByteArray(Serializer.serialize(asList(TestDomain.REPORT, "Parameter")))), BodyHandlers.ofByteArray());
		assertEquals(OK, response.statusCode());

		JsonNodeFactory nodeFactory = OBJECT_MAPPER.getNodeFactory();
		ObjectNode request = OBJECT_MAPPER.createObjectNode();
		request.set("reportType", nodeFactory.textNode(TestDomain.REPORT.name()));
		request.set("parameter", nodeFactory.textNode("parameter"));

		response = HTTP_CLIENT.send(createJsonRequest("report",
						BodyPublishers.ofString(request.toString())), BodyHandlers.ofByteArray());
		assertEquals(OK, response.statusCode());
	}

	@Test
	void dependencies() throws Exception {
		Entity.Key key1 = ENTITIES.primaryKey(Department.TYPE, 10);
		Entity.Key key2 = ENTITIES.primaryKey(Department.TYPE, 20);
		List<Entity> entitiesDep = Arrays.asList(Entity.entity(key1), Entity.entity(key2));

		HttpResponse<byte[]> response = HTTP_CLIENT.send(createRequest("dependencies",
						BodyPublishers.ofByteArray(Serializer.serialize(entitiesDep))), BodyHandlers.ofByteArray());
		assertEquals(OK, response.statusCode());
		Map<String, Collection<Entity>> dependencies = Serializer.deserialize(response.body());
		assertEquals(1, dependencies.size());
		assertEquals(12, dependencies.get(Employee.TYPE).size());

		response = HTTP_CLIENT.send(createJsonRequest("dependencies",
						BodyPublishers.ofString(OBJECT_MAPPER.writeValueAsString(entitiesDep))), BodyHandlers.ofByteArray());
		assertEquals(OK, response.statusCode());
		String content = new String(response.body(), UTF_8);
		Map<EntityType, Collection<Entity>> collectionMap = OBJECT_MAPPER.readValue(content,
						new TypeReference<Map<EntityType, Collection<Entity>>>() {});
		assertEquals(1, collectionMap.size());
		assertEquals(12, collectionMap.get(Employee.TYPE).size());
	}

	@Test
	void count() throws Exception {
		Count where = Count.where(Department.ID.equalTo(10));
		HttpResponse<byte[]> response = HTTP_CLIENT.send(createRequest("count",
						BodyPublishers.ofByteArray(Serializer.serialize(where))), BodyHandlers.ofByteArray());
		assertEquals(OK, response.statusCode());
		int count = Serializer.deserialize(response.body());
		assertEquals(1, count);

		response = HTTP_CLIENT.send(createJsonRequest("count",
						BodyPublishers.ofString(OBJECT_MAPPER.writeValueAsString(where))), BodyHandlers.ofByteArray());
		assertEquals(OK, response.statusCode());
		count = OBJECT_MAPPER.readValue(new String(response.body(), UTF_8), Integer.class);
		assertEquals(1, count);
	}

	@Test
	void values() throws Exception {
		Select select = Select.where(Department.ID.equalTo(10)).build();
		HttpResponse<byte[]> response = HTTP_CLIENT.send(createRequest("values",
						BodyPublishers.ofByteArray(Serializer.serialize(asList(Department.ID, select)))), BodyHandlers.ofByteArray());
		assertEquals(OK, response.statusCode());
		List<Integer> values = Serializer.deserialize(response.body());
		assertEquals(10, values.get(0));

		ObjectNode node = OBJECT_MAPPER.createObjectNode();
		node.set("column", OBJECT_MAPPER.valueToTree(Department.ID.name()));
		node.set("entityType", OBJECT_MAPPER.valueToTree(Department.ID.entityType().name()));
		node.set("condition", OBJECT_MAPPER.valueToTree(select));
		response = HTTP_CLIENT.send(createJsonRequest("values",
						BodyPublishers.ofString(node.toString())), BodyHandlers.ofByteArray());
		assertEquals(OK, response.statusCode());
		values = OBJECT_MAPPER.readValue(new String(response.body(), UTF_8), new TypeReference<List<Integer>>() {});
		assertEquals(10, values.get(0));
	}

	@Test
	void selectByKey() throws Exception {
		List<Entity.Key> keys = new ArrayList<>();
		keys.add(ENTITIES.primaryKey(Department.TYPE, 10));
		keys.add(ENTITIES.primaryKey(Department.TYPE, 20));

		HttpResponse<byte[]> response = HTTP_CLIENT.send(createRequest("selectByKey",
						BodyPublishers.ofByteArray(Serializer.serialize(keys))), BodyHandlers.ofByteArray());
		assertEquals(OK, response.statusCode());
		List<Entity> values = Serializer.deserialize(response.body());
		assertEquals(2, values.size());

		response = HTTP_CLIENT.send(createJsonRequest("selectByKey",
						BodyPublishers.ofString(OBJECT_MAPPER.writeValueAsString(keys))), BodyHandlers.ofByteArray());
		assertEquals(OK, response.statusCode());
		values = OBJECT_MAPPER.readValue(new String(response.body(), UTF_8), EntityObjectMapper.ENTITY_LIST_REFERENCE);
		assertEquals(2, values.size());
	}

	@Test
	void select() throws Exception {
		List<Entity.Key> keys = new ArrayList<>();
		keys.add(ENTITIES.primaryKey(Department.TYPE, 10));
		keys.add(ENTITIES.primaryKey(Department.TYPE, 20));
		Select select = Select.where(keys(keys)).build();

		HttpResponse<byte[]> response = HTTP_CLIENT.send(createRequest("select",
						BodyPublishers.ofByteArray(Serializer.serialize(select))), BodyHandlers.ofByteArray());
		assertEquals(OK, response.statusCode());
		List<Entity> values = Serializer.deserialize(response.body());
		assertEquals(2, values.size());

		response = HTTP_CLIENT.send(createJsonRequest("select",
						BodyPublishers.ofString(OBJECT_MAPPER.writeValueAsString(select))), BodyHandlers.ofByteArray());
		assertEquals(OK, response.statusCode());
		values = OBJECT_MAPPER.readValue(new String(response.body(), UTF_8), EntityObjectMapper.ENTITY_LIST_REFERENCE);
		assertEquals(2, values.size());
	}

	@Test
	void insert() throws Exception {
		List<Entity> entities = new ArrayList<>();
		entities.add(ENTITIES.entity(Department.TYPE)
						.with(Department.ID, -10)
						.with(Department.NAME, "A name")
						.with(Department.LOCATION, "loc")
						.build());
		entities.add(ENTITIES.entity(Department.TYPE)
						.with(Department.ID, -20)
						.with(Department.NAME, "Another name")
						.with(Department.LOCATION, "locat")
						.build());

		HttpResponse<byte[]> response = HTTP_CLIENT.send(createRequest("insertSelect",
						BodyPublishers.ofByteArray(Serializer.serialize(entities))), BodyHandlers.ofByteArray());
		assertEquals(OK, response.statusCode());
		List<Entity> values = Serializer.deserialize(response.body());
		assertEquals(2, values.size());

		entities.forEach(entity -> entity.set(Department.ID, entity.get(Department.ID) + 1));
		response = HTTP_CLIENT.send(createJsonRequest("insert",
						BodyPublishers.ofString(OBJECT_MAPPER.writeValueAsString(entities))), BodyHandlers.ofByteArray());
		assertEquals(OK, response.statusCode());
		values = OBJECT_MAPPER.readValue(new String(response.body(), UTF_8), EntityObjectMapper.ENTITY_LIST_REFERENCE);
		assertEquals(2, values.size());
	}

	@Test
	void update() throws Exception {
		List<Entity> entities = new ArrayList<>();
		entities.add(ENTITIES.entity(Department.TYPE)
						.with(Department.ID, 10)
						.with(Department.NAME, "ACCOUNTING")
						.with(Department.LOCATION, "NEW YORK")
						.build());
		entities.add(ENTITIES.entity(Department.TYPE)
						.with(Department.ID, 20)
						.with(Department.NAME, "RESEARCH")
						.with(Department.LOCATION, "DALLAS")
						.build());
		entities.get(0).set(Department.LOCATION, "NEW YORK2");
		entities.get(1).set(Department.LOCATION, "DALLAS2");

		HttpResponse<byte[]> response = HTTP_CLIENT.send(createRequest("updateSelect",
						BodyPublishers.ofByteArray(Serializer.serialize(entities))), BodyHandlers.ofByteArray());
		assertEquals(OK, response.statusCode());
		List<Entity> values = Serializer.deserialize(response.body());
		assertEquals(2, values.size());
		assertTrue(values.containsAll(entities));
		assertEquals("NEW YORK2", values.stream().filter(entity -> entity.get(Department.ID).equals(10))
						.findFirst().orElse(null).get(Department.LOCATION));
		assertEquals("DALLAS2", values.stream().filter(entity -> entity.get(Department.ID).equals(20))
						.findFirst().orElse(null).get(Department.LOCATION));

		entities.get(0).save(Department.LOCATION);
		entities.get(0).set(Department.LOCATION, "NEW YORK");
		entities.get(1).save(Department.LOCATION);
		entities.get(1).set(Department.LOCATION, "DALLAS");
		response = HTTP_CLIENT.send(createJsonRequest("updateSelect",
						BodyPublishers.ofString(OBJECT_MAPPER.writeValueAsString(entities))), BodyHandlers.ofByteArray());
		assertEquals(OK, response.statusCode());
		values = OBJECT_MAPPER.readValue(new String(response.body(), UTF_8), EntityObjectMapper.ENTITY_LIST_REFERENCE);
		assertEquals(2, values.size());
		assertTrue(values.containsAll(entities));
		assertEquals("NEW YORK", values.stream().filter(entity -> entity.get(Department.ID).equals(10))
						.findFirst().orElse(null).get(Department.LOCATION));
		assertEquals("DALLAS", values.stream().filter(entity -> entity.get(Department.ID).equals(20))
						.findFirst().orElse(null).get(Department.LOCATION));
	}

	@Test
	void updateCondition() throws Exception {
		Update update = Update.where(Department.ID.between(10, 20))
						.set(Department.LOCATION, "aloc").build();
		HttpResponse<byte[]> response = HTTP_CLIENT.send(createRequest("updateByCondition",
						BodyPublishers.ofByteArray(Serializer.serialize(update))), BodyHandlers.ofByteArray());
		assertEquals(OK, response.statusCode());
		int count = Serializer.deserialize(response.body());
		assertEquals(2, count);

		response = HTTP_CLIENT.send(createJsonRequest("updateByCondition",
						BodyPublishers.ofString(OBJECT_MAPPER.writeValueAsString(update))), BodyHandlers.ofByteArray());
		assertEquals(OK, response.statusCode());
		count = OBJECT_MAPPER.readValue(new String(response.body(), UTF_8), Integer.class);
		assertEquals(2, count);
	}

	@Test
	void delete() throws Exception {
		Condition deleteCondition = Department.ID.equalTo(40);
		HttpResponse<byte[]> response = HTTP_CLIENT.send(createRequest("delete",
						BodyPublishers.ofByteArray(Serializer.serialize(deleteCondition))), BodyHandlers.ofByteArray());
		assertEquals(OK, response.statusCode());
		int count = Serializer.deserialize(response.body());
		assertEquals(1, count);

		response = HTTP_CLIENT.send(createJsonRequest("delete",
						BodyPublishers.ofString(OBJECT_MAPPER.writeValueAsString(deleteCondition))), BodyHandlers.ofByteArray());
		assertEquals(OK, response.statusCode());
		count = OBJECT_MAPPER.readValue(new String(response.body(), UTF_8), Integer.class);
		assertEquals(0, count);
	}

	@Test
	void deleteByKey() throws Exception {
		HttpResponse<byte[]> response = HTTP_CLIENT.send(createRequest("deleteByKey",
						BodyPublishers.ofByteArray(Serializer.serialize(singletonList(ENTITIES.primaryKey(Department.TYPE, 50))))), BodyHandlers.ofByteArray());
		assertEquals(OK, response.statusCode());

		response = HTTP_CLIENT.send(createJsonRequest("deleteByKey",
						BodyPublishers.ofString(OBJECT_MAPPER.writeValueAsString(singletonList(ENTITIES.primaryKey(Department.TYPE, 60))))), BodyHandlers.ofByteArray());
		assertEquals(OK, response.statusCode());
	}

	@Test
	void malformedAuthenticationHeaders() throws Exception {
		//a missing header is 401, so a malformed one must not be 500, the server blaming itself for the client's input
		HttpResponse<byte[]> response = HTTP_CLIENT.send(createRequest(SERVER_JSON_BASEURL, "isTransactionOpen",
						BodyPublishers.noBody(), "not a uuid", Version.version().toString(), createAuthorizationHeader()),
						BodyHandlers.ofByteArray());
		assertEquals(UNAUTHORIZED, response.statusCode());

		response = HTTP_CLIENT.send(createRequest(SERVER_JSON_BASEURL, "isTransactionOpen",
						BodyPublishers.noBody(), CLIENT_ID_STRING, Version.version().toString(), "Basic not!base64"),
						BodyHandlers.ofByteArray());
		assertEquals(UNAUTHORIZED, response.statusCode());

		//a malformed optional version header is a bad request, not an authentication failure
		response = HTTP_CLIENT.send(createRequest(SERVER_JSON_BASEURL, "isTransactionOpen",
						BodyPublishers.noBody(), CLIENT_ID_STRING, "not a version", createAuthorizationHeader()),
						BodyHandlers.ofByteArray());
		assertEquals(BAD_REQUEST, response.statusCode());
	}

	@Test
	void internalErrorIsGeneric() throws Exception {
		//no entityType node, the handler dereferences null; nothing about it is the client's business
		ObjectNode node = OBJECT_MAPPER.createObjectNode();
		node.set("column", OBJECT_MAPPER.valueToTree(Department.ID.name()));
		HttpResponse<byte[]> response = HTTP_CLIENT.send(createJsonRequest("values",
						BodyPublishers.ofString(node.toString())), BodyHandlers.ofByteArray());

		assertEquals(INTERNAL_SERVER_ERROR, response.statusCode());
		assertTrue(response.headers().firstValue("Content-Type").orElseThrow().startsWith("application/json"));

		ErrorEnvelope envelope = ErrorEnvelope.fromJson(response.body());
		assertEquals(ErrorKind.INTERNAL, envelope.errorKind().orElseThrow());
		assertEquals("Internal server error", envelope.message());
		assertFalse(envelope.correlationId().isEmpty());
		assertNull(envelope.detail());

		//neither the exception type, its message nor a stack frame crosses the wire
		String body = new String(response.body(), UTF_8);
		assertFalse(body.contains("NullPointer"), body);
		assertFalse(body.contains("is.codion.framework.servlet"), body);
	}

	@Test
	void serialErrorsAreStillSerializedExceptions() throws Exception {
		//the serial channel is a java serialization channel by definition, its error wire format is unchanged
		HttpResponse<byte[]> response = HTTP_CLIENT.send(createRequest(SERVER_BASEURL, "isTransactionOpen",
						BodyPublishers.noBody(), CLIENT_ID_STRING, Version.version().toString(), "Basic not!base64"),
						BodyHandlers.ofByteArray());
		assertEquals(UNAUTHORIZED, response.statusCode());
		Object exception = Serializer.deserialize(response.body());
		assertInstanceOf(ServerAuthenticationException.class, exception);
	}

	@Test
	void valuesForeignKey() throws Exception {
		//a foreign key is not a column, the request is malformed
		ObjectNode node = OBJECT_MAPPER.createObjectNode();
		node.set("column", OBJECT_MAPPER.valueToTree(Employee.DEPARTMENT_FK.name()));
		node.set("entityType", OBJECT_MAPPER.valueToTree(Employee.TYPE.name()));
		HttpResponse<byte[]> response = HTTP_CLIENT.send(createJsonRequest("values",
						BodyPublishers.ofString(node.toString())), BodyHandlers.ofByteArray());
		assertEquals(BAD_REQUEST, response.statusCode());
	}

	private static HttpRequest createRequest(String path) {
		return createRequest(SERVER_BASEURL, path, BodyPublishers.noBody());
	}

	private static HttpRequest createRequest(String path, BodyPublisher bodyPublisher) {
		return createRequest(SERVER_BASEURL, path, bodyPublisher);
	}

	private static HttpRequest createJsonRequest(String path) {
		return createJsonRequest(path, BodyPublishers.noBody());
	}

	private static HttpRequest createJsonRequest(String path, BodyPublisher bodyPublisher) {
		return createRequest(SERVER_JSON_BASEURL, path, bodyPublisher);
	}

	private static HttpRequest createRequest(String baseUrl, String path, BodyPublisher bodyPublisher) {
		return createRequest(baseUrl, path, bodyPublisher, CLIENT_ID_STRING,
						Version.version().toString(), createAuthorizationHeader());
	}

	private static HttpRequest createRequest(String baseUrl, String path, BodyPublisher bodyPublisher,
																					 String clientId, String clientVersion, String authorization) {
		return HttpRequest.newBuilder()
						.uri(URI.create(baseUrl + path))
						.POST(bodyPublisher)
						.headers(new String[] {
										EntityService.DOMAIN_TYPE, TestDomain.DOMAIN.name(),
										EntityService.CLIENT_TYPE, "EntityJavalinTest",
										EntityService.CLIENT_ID, clientId,
										EntityService.CLIENT_VERSION, clientVersion,
										"Authorization", authorization
						})
						.build();
	}

	private static String createAuthorizationHeader() {
		return "Basic " + createCredentials();
	}

	private static String createCredentials() {
		return Base64.getEncoder().encodeToString((UNIT_TEST_USER.username() +
						":" + String.valueOf(UNIT_TEST_USER.password())).getBytes());
	}

	private static HttpClient createHttpClient() {
		return HttpClient.newBuilder()
						.cookieHandler(new CookieManager())
						.build();
	}

	private static EntityServerConfiguration configure() {
		Clients.SERVER_HOSTNAME.set("localhost");
		Clients.resolveTrustStore();
		EntityService.SERIALIZATION.set(true);
		EntityService.SECURE.set(false);

		return EntityServerConfiguration.builder()
						.port(3223)
						.registryPort(3221)
						.adminPort(3223)
						.adminUser(User.parse("scott:tiger"))
						.domainClasses(singletonList(TestDomain.class.getName()))
						.database(Database.instance())
						.sslEnabled(false)
						.auxiliaryServerFactory(singletonList(EntityServiceFactory.class.getName()))
						.objectInputFilterFactoryRequired(false)
						.build();
	}

	private static void deconfigure() {
		System.setProperty("jdk.internal.httpclient.disableHostnameVerification", Boolean.FALSE.toString());
		Clients.SERVER_HOSTNAME.set(null);
		ServerConfiguration.AUXILIARY_SERVER_FACTORIES.set(null);
		EntityService.SECURE.set(true);
	}
}
