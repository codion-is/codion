/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.empdept.rest;

import org.jminor.common.TextUtil;
import org.jminor.common.User;
import org.jminor.common.db.condition.Condition;
import org.jminor.common.model.CancelException;
import org.jminor.common.model.tools.LoadTestModel;
import org.jminor.common.server.Server;
import org.jminor.framework.demos.empdept.domain.EmpDept;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.plugins.json.EntityJSONParser;
import org.jminor.framework.plugins.rest.EntityRESTService;
import org.jminor.swing.common.ui.tools.LoadTestPanel;

import org.apache.http.HttpEntity;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;

import javax.swing.UIManager;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

public final class EmpDeptRESTLoadTest extends LoadTestModel<CloseableHttpClient> {

  private static final Entities ENTITIES = new EmpDept();

  private static final User UNIT_TEST_USER = new User(
          System.getProperty("jminor.unittest.username", "scott"),
          System.getProperty("jminor.unittest.password", "tiger"));

  private static final int PORT = 8080;
  private static final String BASEURL = Server.SERVER_HOST_NAME.get() + ":" + PORT + "/entities/";
  private static final String BASIC = "Basic ";
  private static final String HTTP = "http";
  private static final RequestConfig REQUEST_CONFIG = RequestConfig.custom()
            .setSocketTimeout(2000)
            .setConnectTimeout(2000)
            .build();

  private final HttpRequestInterceptor requestInterceptor = (request, httpContext) -> {
    final User user1 = getUser();
    request.setHeader(EntityRESTService.AUTHORIZATION, BASIC + Base64.getEncoder().encodeToString((user1.getUsername() + ":" + user1.getPassword()).getBytes()));
    request.setHeader("Content-Type", MediaType.APPLICATION_JSON);
  };

  public EmpDeptRESTLoadTest(final User user) {
    super(user, Arrays.asList(new Accounting(), new UpdateLocation(), new Employees(), new AddDepartment(), new AddEmployee()),
            5000, 2, 10, 500);
    setWeight(UpdateLocation.NAME, 2);
    setWeight(Accounting.NAME, 4);
    setWeight(Employees.NAME, 5);
    setWeight(AddDepartment.NAME, 1);
    setWeight(AddEmployee.NAME, 4);
  }

  @Override
  protected void disconnectApplication(final CloseableHttpClient client) {
    try {
      client.close();
    }
    catch (final IOException e) {
      e.printStackTrace();
    }
  }

  @Override
  protected CloseableHttpClient initializeApplication() throws CancelException {
    final CloseableHttpClient ret = HttpClientBuilder.create()
            .setDefaultRequestConfig(REQUEST_CONFIG)
            .setConnectionManager(new PoolingHttpClientConnectionManager())
            .addInterceptorFirst(requestInterceptor)
            .build();

    return ret;
  }

  private static URIBuilder createURIBuilder() {
    final URIBuilder builder = new URIBuilder();
    builder.setScheme(HTTP).setHost(BASEURL);

    return builder;
  }

  private static String getContentStream(final HttpEntity entity) throws IOException {
    Scanner scanner = null;
    try (final InputStream stream = entity.getContent()) {
      scanner = new Scanner(stream).useDelimiter("\\A");

      return scanner.hasNext() ? scanner.next() : "";
    }
    finally {
      if (scanner != null) {
        scanner.close();
      }
      EntityUtils.consume(entity);
    }
  }

  public static void main(final String[] args) throws Exception {
    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    new LoadTestPanel(new EmpDeptRESTLoadTest(UNIT_TEST_USER)).showFrame();
  }

  private static final class UpdateLocation extends AbstractUsageScenario<CloseableHttpClient> {
    public static final String NAME = "UpdateLocation";

    private UpdateLocation() {
      super(NAME);
    }

    @Override
    protected void performScenario(final CloseableHttpClient client) throws ScenarioException {
      try {
        URIBuilder builder = createURIBuilder();
        builder.setPath(EntityRESTService.BY_VALUE_PATH)
                .addParameter("domainId", ENTITIES.getDomainId())
                .addParameter("entityId", EmpDept.T_DEPARTMENT);
        HttpResponse response = client.execute(new HttpGet(builder.build()));
        checkResponse(response);
        final String queryResult = getContentStream(response.getEntity());
        final List<Entity> queryEntities = new EntityJSONParser(ENTITIES).deserializeEntities(queryResult);

        final Entity entity = queryEntities.get(new Random().nextInt(queryEntities.size()));
        entity.put(EmpDept.DEPARTMENT_LOCATION, TextUtil.createRandomString(10, 13));
        builder = createURIBuilder();
        builder.addParameter("domainId", ENTITIES.getDomainId())
                .addParameter("entities", new EntityJSONParser(ENTITIES).serialize(Collections.singletonList(entity)));
        response = client.execute(new HttpPut(builder.build()));
        checkResponse(response);
        getContentStream(response.getEntity());
      }
      catch (final Exception e) {
        e.printStackTrace();
        throw new ScenarioException(e);
      }
    }
  }

  private static final class Accounting extends AbstractUsageScenario<CloseableHttpClient> {
    public static final String NAME = "Accounting";

    private Accounting() {
      super(NAME);
    }

    @Override
    protected void performScenario(final CloseableHttpClient client) throws ScenarioException {
      try {
        final URIBuilder builder = createURIBuilder();
        builder.setPath(EntityRESTService.BY_VALUE_PATH)
                .addParameter("domainId", ENTITIES.getDomainId())
                .addParameter("entityId", EmpDept.T_DEPARTMENT)
                .addParameter("conditionType", Condition.Type.NOT_LIKE.toString())
                .addParameter("values", "{\"dname\":\"ACCOUNTING\"}");
        final HttpResponse response = client.execute(new HttpGet(builder.build()));
        checkResponse(response);
        final String queryResult = getContentStream(response.getEntity());
        new EntityJSONParser(ENTITIES).deserializeEntities(queryResult);
      }
      catch (final Exception e) {
        e.printStackTrace();
        throw new ScenarioException(e);
      }
    }
  }

  private static final class Employees extends AbstractUsageScenario<CloseableHttpClient> {
    public static final String NAME = "Employees";

    private Employees() {
      super(NAME);
    }

    @Override
    protected void performScenario(final CloseableHttpClient client) throws ScenarioException {
      final EntityJSONParser parser = new EntityJSONParser(ENTITIES);
      try {
        URIBuilder builder = createURIBuilder();
        builder.setPath(EntityRESTService.BY_VALUE_PATH)
                .addParameter("domainId", ENTITIES.getDomainId())
                .addParameter("entityId", EmpDept.T_DEPARTMENT)
                .addParameter("conditionType", Condition.Type.NOT_LIKE.toString())
                .addParameter("values", "{\"dname\":\"ACCOUNTING\"}");

        HttpResponse response = client.execute(new HttpGet(builder.build()));
        checkResponse(response);
        String queryResult = getContentStream(response.getEntity());
        List<Entity> queryEntities = parser.deserializeEntities(queryResult);

        builder = createURIBuilder();
        builder.setPath(EntityRESTService.BY_VALUE_PATH)
                .addParameter("domainId", ENTITIES.getDomainId())
                .addParameter("entityId", EmpDept.T_EMPLOYEE)
                .addParameter("conditionType", Condition.Type.LIKE.toString())
                .addParameter("values", "{\"deptno\":\"" + queryEntities.get(new Random().nextInt(queryEntities.size()))
                        .getAsString(EmpDept.DEPARTMENT_ID) + "\"}");

        response = client.execute(new HttpGet(builder.build()));
        checkResponse(response);
        queryResult = getContentStream(response.getEntity());
        queryEntities = parser.deserializeEntities(queryResult);
      }
      catch (final Exception e) {
        e.printStackTrace();
        throw new ScenarioException(e);
      }
    }
  }

  private static final class AddDepartment extends AbstractUsageScenario<CloseableHttpClient> {
    public static final String NAME = "AddDepartment";

    private AddDepartment() {
      super(NAME);
    }
    @Override
    protected void performScenario(final CloseableHttpClient client) throws ScenarioException {
      try {
        final int deptNo = new Random().nextInt(5000);
        final Entity propaganda = ENTITIES.entity(EmpDept.T_DEPARTMENT);
        propaganda.put(EmpDept.DEPARTMENT_ID, deptNo);
        propaganda.put(EmpDept.DEPARTMENT_NAME, TextUtil.createRandomString(4, 8));
        propaganda.put(EmpDept.DEPARTMENT_LOCATION, TextUtil.createRandomString(5, 10));

        final URIBuilder builder = createURIBuilder();
        builder.addParameter("domainId", ENTITIES.getDomainId())
                .addParameter("entities", new EntityJSONParser(ENTITIES).serialize(Collections.singletonList(propaganda)));
        final HttpResponse response = client.execute(new HttpPost(builder.build()));
        checkResponse(response);
        EntityUtils.consume(response.getEntity());
      }
      catch (final Exception e) {
        e.printStackTrace();
        throw new ScenarioException(e);
      }
    }
  }

  private static final class AddEmployee extends AbstractUsageScenario<CloseableHttpClient> {
    public static final String NAME = "AddEmployee";

    private final Random random = new Random();

    private AddEmployee() {
      super(NAME);
    }

    @Override
    protected void performScenario(final CloseableHttpClient client) throws ScenarioException {
      try {
        URIBuilder builder = createURIBuilder();
        builder.setPath(EntityRESTService.BY_VALUE_PATH)
                .addParameter("domainId", ENTITIES.getDomainId())
                .addParameter("entityId", EmpDept.T_DEPARTMENT);
        HttpResponse response = client.execute(new HttpGet(builder.build()));
        checkResponse(response);
        final String queryResult = getContentStream(response.getEntity());
        final List<Entity> queryEntities = new EntityJSONParser(ENTITIES).deserializeEntities(queryResult);
        final Entity department = queryEntities.get(random.nextInt(queryEntities.size()));
        final Entity employee = ENTITIES.entity(EmpDept.T_EMPLOYEE);
        employee.put(EmpDept.EMPLOYEE_DEPARTMENT_FK, department);
        employee.put(EmpDept.EMPLOYEE_NAME, TextUtil.createRandomString(5, 10));
        employee.put(EmpDept.EMPLOYEE_JOB, EmpDept.JOB_VALUES.get(random.nextInt(EmpDept.JOB_VALUES.size())).getItem());
        employee.put(EmpDept.EMPLOYEE_SALARY, (double) random.nextInt(1000) + 1000);
        employee.put(EmpDept.EMPLOYEE_HIREDATE, new Date());
        employee.put(EmpDept.EMPLOYEE_COMMISSION, random.nextDouble() * 500);

        builder = createURIBuilder();
        builder.addParameter("domainId", ENTITIES.getDomainId())
                .addParameter("entities", new EntityJSONParser(ENTITIES).serialize(Collections.singletonList(employee)));
        response = client.execute(new HttpPost(builder.build()));
        checkResponse(response);
        EntityUtils.consume(response.getEntity());
      }
      catch (final Exception e) {
        e.printStackTrace();
        throw new ScenarioException(e);
      }
    }
  }

  private static void checkResponse(final HttpResponse response) throws Exception {
    if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
      throw new Exception("Error from server: " + getContentStream(response.getEntity()));
    }
  }
}
