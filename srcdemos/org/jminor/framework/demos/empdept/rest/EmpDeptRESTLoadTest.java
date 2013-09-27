/*
 * Copyright (c) 2004 - 2013, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.empdept.rest;

import org.jminor.common.model.CancelException;
import org.jminor.common.model.SearchType;
import org.jminor.common.model.User;
import org.jminor.common.model.Util;
import org.jminor.common.model.tools.LoadTestModel;
import org.jminor.common.model.tools.ScenarioException;
import org.jminor.common.ui.tools.LoadTestPanel;
import org.jminor.framework.Configuration;
import org.jminor.framework.demos.empdept.domain.EmpDept;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.plugins.json.EntityJSONParser;
import org.jminor.framework.plugins.rest.EntityRESTService;

import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.protocol.RequestDefaultHeaders;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

import javax.swing.UIManager;
import javax.ws.rs.core.MediaType;
import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

public final class EmpDeptRESTLoadTest extends LoadTestModel<DefaultHttpClient> {

  private static final PoolingClientConnectionManager CONNECTION_MANAGER;
  private static final String HOSTNAME = Configuration.getStringValue(Configuration.SERVER_HOST_NAME);
  private static final int PORT = 8080;
  private static final String BASEURL = HOSTNAME + ":" + PORT + "/entities/";
  private static final String BASIC = "Basic ";
  private static final String HTTP = "http";

  private final RequestDefaultHeaders requestDefaultHeaders = new RequestDefaultHeaders() {
    @Override
    public void process(final HttpRequest request, final HttpContext context) throws HttpException, IOException {
      final User user = getUser();
      request.setHeader(EntityRESTService.AUTHORIZATION, BASIC + DatatypeConverter.printBase64Binary((user.getUsername() + ":" + user.getPassword()).getBytes()));
      request.setHeader("Content-Type", MediaType.APPLICATION_JSON);
    }
  };

  static {
    EmpDept.init();
    final SchemeRegistry schemeRegistry = new SchemeRegistry();
    schemeRegistry.register(new Scheme(HTTP, PORT, PlainSocketFactory.getSocketFactory()));
    CONNECTION_MANAGER = new PoolingClientConnectionManager(schemeRegistry);
  }

  public EmpDeptRESTLoadTest(final User user) {
    super(user, Arrays.asList(new Accounting(), new UpdateLocation(), new Employees(), new AddDepartment(), new AddEmployee()),
            5000, 2, 10, 500);
    setWeight(UpdateLocation.NAME, 2);
    setWeight(Accounting.NAME, 4);
    setWeight(Employees.NAME, 5);
    setWeight(AddDepartment.NAME, 0);
    setWeight(AddEmployee.NAME, 0);
  }

  @Override
  protected void disconnectApplication(final DefaultHttpClient client) {}

  @Override
  protected DefaultHttpClient initializeApplication() throws CancelException {
    final DefaultHttpClient ret = new DefaultHttpClient(CONNECTION_MANAGER);
    ret.addRequestInterceptor(requestDefaultHeaders);

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
    new LoadTestPanel(new EmpDeptRESTLoadTest(User.UNIT_TEST_USER)).showFrame();
  }

  private static final class UpdateLocation extends AbstractUsageScenario<DefaultHttpClient> {
    public static final String NAME = "UpdateLocation";

    private UpdateLocation() {
      super(NAME);
    }

    @Override
    protected void performScenario(final DefaultHttpClient client) throws ScenarioException {
      try {
        URIBuilder builder = createURIBuilder();
        builder.setPath(EntityRESTService.BY_VALUE_PATH)
                .addParameter("entityID", EmpDept.T_DEPARTMENT);
        HttpResponse response = client.execute(new HttpGet(builder.build()));
        final String queryResult = getContentStream(response.getEntity());
        final List<Entity> queryEntities = EntityJSONParser.deserializeEntities(queryResult);

        final Entity entity = queryEntities.get(new Random().nextInt(queryEntities.size()));
        entity.setValue(EmpDept.DEPARTMENT_LOCATION, Util.createRandomString(10, 13));
        builder = createURIBuilder();
        builder.addParameter("entities", EntityJSONParser.serializeEntities(Arrays.asList(entity), false));
        response = client.execute(new HttpPut(builder.build()));
        getContentStream(response.getEntity());
      }
      catch (Exception e) {
        e.printStackTrace();
        throw new ScenarioException(e);
      }
    }
  }

  private static final class Accounting extends AbstractUsageScenario<DefaultHttpClient> {
    public static final String NAME = "Accounting";

    private Accounting() {
      super(NAME);
    }

    @Override
    protected void performScenario(final DefaultHttpClient client) throws ScenarioException {
      try {
        final URIBuilder builder = createURIBuilder();
        builder.setPath(EntityRESTService.BY_VALUE_PATH)
                .addParameter("entityID", EmpDept.T_DEPARTMENT)
                .addParameter("searchType", SearchType.NOT_LIKE.toString())
                .addParameter("values", "{\"dname\":\"ACCOUNTING\"}");
        final HttpResponse response = client.execute(new HttpGet(builder.build()));
        final String queryResult = getContentStream(response.getEntity());
        final List<Entity> queryEntities = EntityJSONParser.deserializeEntities(queryResult);
      }
      catch (Exception e) {
        e.printStackTrace();
        throw new ScenarioException(e);
      }
    }
  }

  private static final class Employees extends AbstractUsageScenario<DefaultHttpClient> {
    public static final String NAME = "Employees";

    private Employees() {
      super(NAME);
    }

    @Override
    protected void performScenario(final DefaultHttpClient client) throws ScenarioException {
      try {
        URIBuilder builder = createURIBuilder();
        builder.setPath(EntityRESTService.BY_VALUE_PATH)
                .addParameter("entityID", EmpDept.T_DEPARTMENT)
                .addParameter("searchType", SearchType.NOT_LIKE.toString())
                .addParameter("values", "{\"dname\":\"ACCOUNTING\"}");

        HttpResponse response = client.execute(new HttpGet(builder.build()));
        String queryResult = getContentStream(response.getEntity());
        List<Entity> queryEntities = EntityJSONParser.deserializeEntities(queryResult);

        builder = createURIBuilder();
        builder.setPath(EntityRESTService.BY_VALUE_PATH)
                .addParameter("entityID", EmpDept.T_EMPLOYEE)
                .addParameter("searchType", SearchType.LIKE.toString())
                .addParameter("values", "{\"deptno\":\"" + queryEntities.get(new Random().nextInt(queryEntities.size())).getValueAsString(EmpDept.DEPARTMENT_ID) + "\"}");

        response = client.execute(new HttpGet(builder.build()));
        queryResult = getContentStream(response.getEntity());
        queryEntities = EntityJSONParser.deserializeEntities(queryResult);
      }
      catch (Exception e) {
        e.printStackTrace();
        throw new ScenarioException(e);
      }
    }
  }

  private static final class AddDepartment extends AbstractUsageScenario<DefaultHttpClient> {
    public static final String NAME = "AddDepartment";

    private AddDepartment() {
      super(NAME);
    }
    @Override
    protected void performScenario(final DefaultHttpClient client) throws ScenarioException {
      try {
        final int deptNo = new Random().nextInt(500);
        final Entity propaganda = Entities.entity(EmpDept.T_DEPARTMENT);
        propaganda.setValue(EmpDept.DEPARTMENT_ID, deptNo);
        propaganda.setValue(EmpDept.DEPARTMENT_NAME, "PROPAGANDA");
        propaganda.setValue(EmpDept.DEPARTMENT_LOCATION, "Hell");

        final URIBuilder builder = createURIBuilder();
        builder.addParameter("entities", EntityJSONParser.serializeEntities(Arrays.asList(propaganda), false));
        final HttpResponse response = client.execute(new HttpPost(builder.build()));
      }
      catch (Exception e) {
        e.printStackTrace();
        throw new ScenarioException(e);
      }
    }
  }

  private static final class AddEmployee extends AbstractUsageScenario<DefaultHttpClient> {
    public static final String NAME = "AddEmployee";

    private AddEmployee() {
      super(NAME);
    }

    @Override
    protected void performScenario(final DefaultHttpClient client) throws ScenarioException {
      try {
        final URIBuilder builder = createURIBuilder();
        builder.setPath(EntityRESTService.BY_VALUE_PATH)
                .addParameter("entityID", EmpDept.T_DEPARTMENT);
        final HttpResponse response = client.execute(new HttpGet(builder.build()));

        final String queryResult = getContentStream(response.getEntity());
        final List<Entity> queryEntities = EntityJSONParser.deserializeEntities(queryResult);
        final Entity department = queryEntities.get(0);
      }
      catch (Exception e) {
        e.printStackTrace();
        throw new ScenarioException(e);
      }
    }
  }
}
