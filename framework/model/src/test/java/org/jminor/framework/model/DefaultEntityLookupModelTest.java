/*
 * Chinook.Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.model;

import org.jminor.common.User;
import org.jminor.common.db.Databases;
import org.jminor.common.db.condition.Condition;
import org.jminor.common.db.condition.Conditions;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.db.condition.EntityConditions;
import org.jminor.framework.db.local.LocalEntityConnectionProvider;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.Property;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.*;

public final class DefaultEntityLookupModelTest {

  private static final Entities ENTITIES = new TestDomain();
  private static final EntityConnectionProvider CONNECTION_PROVIDER = new LocalEntityConnectionProvider(ENTITIES, new User(
          System.getProperty("jminor.unittest.username", "scott"),
          System.getProperty("jminor.unittest.password", "tiger")), Databases.getInstance());
  private static final EntityConditions ENTITY_CONDITIONS = CONNECTION_PROVIDER.getConditions();

  private EntityLookupModel lookupModel;
  private Collection<Property.ColumnProperty> lookupProperties;

  @Test(expected = NullPointerException.class)
  public void constructorNullEntityId() {
    new DefaultEntityLookupModel(null, CONNECTION_PROVIDER, new ArrayList<>());
  }

  @Test(expected = NullPointerException.class)
  public void constructorNullConnectionProvider() {
    new DefaultEntityLookupModel(TestDomain.T_EMP, null, new ArrayList<>());
  }

  @Test(expected = NullPointerException.class)
  public void constructorNullLookupProperties() {
    new DefaultEntityLookupModel(TestDomain.T_EMP, CONNECTION_PROVIDER, null);
  }

  @Test(expected = IllegalStateException.class)
  public void lookupWithNoLookupProperties() {
    new DefaultEntityLookupModel(TestDomain.T_EMP, CONNECTION_PROVIDER, Collections.emptyList()).performQuery();
  }

  @Test(expected = IllegalArgumentException.class)
  public void constructorNonStringLookupProperty() {
    new DefaultEntityLookupModel(TestDomain.T_EMP, CONNECTION_PROVIDER,
            Collections.singletonList(ENTITIES.getColumnProperty(TestDomain.T_EMP, TestDomain.EMP_COMMISSION)));
  }

  @Test(expected = IllegalArgumentException.class)
  public void constructorIncorrectEntityLookupProperty() {
    new DefaultEntityLookupModel(TestDomain.T_EMP, CONNECTION_PROVIDER,
            Collections.singletonList(ENTITIES.getColumnProperty(TestDomain.T_DEPARTMENT, TestDomain.DEPARTMENT_NAME)));
  }

  @Test
  public void theRest() {
    lookupModel.setDescription("description");
    assertEquals("description", lookupModel.getDescription());
    assertNotNull(lookupModel.getConnectionProvider());
    assertTrue(lookupModel.getLookupProperties().containsAll(lookupProperties));
    assertNotNull(lookupModel.getWildcard());
  }

  @Test(expected = IllegalArgumentException.class)
  public void setSelectedEntitiesMultipleNotAllowed() {
    lookupModel.getMultipleSelectionAllowedValue().set(false);
    final Collection<Entity> entities = Arrays.asList(ENTITIES.entity(TestDomain.T_EMP), ENTITIES.entity(TestDomain.T_EMP));
    lookupModel.setSelectedEntities(entities);
  }

  @Test
  public void setToStringProvider() {
    final Property job = ENTITIES.getProperty(TestDomain.T_EMP, TestDomain.EMP_JOB);
    lookupModel.setToStringProvider(entity -> entity.getAsString(job));
    final Entity employee = ENTITIES.entity(TestDomain.T_EMP);
    employee.put(TestDomain.EMP_NAME, "Darri");
    employee.put(TestDomain.EMP_JOB, "CLERK");
    lookupModel.setSelectedEntities(Collections.singletonList(employee));
    assertEquals(lookupModel.getSearchString(), "CLERK");
    lookupModel.setToStringProvider(null);
    lookupModel.setSelectedEntities(Collections.singletonList(employee));
    assertEquals(lookupModel.getSearchString(), "Darri");
  }

  @Test
  public void lookupModel() throws Exception {
    lookupModel.getMultipleSelectionAllowedValue().set(true);
    lookupModel.setWildcard("%");
    lookupModel.setSearchString("joh");
    assertFalse(lookupModel.searchStringRepresentsSelected());
    List<Entity> result = lookupModel.performQuery();
    assertTrue("Result should not be empty", result.size() > 0);
    assertTrue("Result should contain John", contains(result, "John"));
    assertTrue("Result should contain johnson", contains(result, "johnson"));
    assertFalse("Result should not contain Andy", contains(result, "Andy"));
    assertFalse("Result should not contain Andrew", contains(result, "Andrew"));
    assertEquals("Search string should not have changed", lookupModel.getSearchString(), "joh");
    lookupModel.setSelectedEntities(result);
    assertEquals("Search string should have been updated",//this test fails due to the toString cache, strange?
            "John" + lookupModel.getMultipleItemSeparatorValue().get() + "johnson", lookupModel.getSearchString());

    lookupModel.setSearchString("jo");
    result = lookupModel.performQuery();
    assertTrue("Result should contain John", contains(result, "John"));
    assertTrue("Result should contain johnson", contains(result, "johnson"));
    assertFalse("Result should not contain Andy", contains(result, "Andy"));
    assertFalse("Result should not contain Andrew", contains(result, "Andrew"));

    lookupModel.setSearchString("le");
    result = lookupModel.performQuery();
    assertTrue("Result should contain John", contains(result, "John"));
    assertFalse("Result should not contain johnson", contains(result, "johnson"));
    assertTrue("Result should contain Andy", contains(result, "Andy"));
    assertFalse("Result should not contain Andrew", contains(result, "Andrew"));

    final Property.ColumnProperty employeeNameProperty = ENTITIES.getColumnProperty(TestDomain.T_EMP, TestDomain.EMP_NAME);
    final Property.ColumnProperty employeeJobProperty = ENTITIES.getColumnProperty(TestDomain.T_EMP, TestDomain.EMP_JOB);

    lookupModel.getPropertyLookupSettings().get(employeeNameProperty).getWildcardPrefixValue().set(false);
    lookupModel.getPropertyLookupSettings().get(employeeJobProperty).getWildcardPrefixValue().set(false);
    lookupModel.setSearchString("jo,cl");
    result = lookupModel.performQuery();
    assertTrue("Result should contain John", contains(result, "John"));
    assertTrue("Result should contain johnson", contains(result, "johnson"));
    assertTrue("Result should contain Andy", contains(result, "Andy"));
    assertFalse("Result should not contain andrew", contains(result, "Andrew"));

    lookupModel.setSearchString("Joh");
    lookupModel.getPropertyLookupSettings().get(employeeNameProperty).getCaseSensitiveValue().set(true);
    lookupModel.getPropertyLookupSettings().get(employeeJobProperty).getCaseSensitiveValue().set(true);
    result = lookupModel.performQuery();
    assertEquals("Result count should be 1", 1, result.size());
    assertTrue("Result should contain John", contains(result, "John"));
    assertFalse("Result should not contain johnson", contains(result, "johnson"));
    lookupModel.getPropertyLookupSettings().get(employeeNameProperty).getWildcardPrefixValue().set(false);
    lookupModel.getPropertyLookupSettings().get(employeeJobProperty).getWildcardPrefixValue().set(false);
    lookupModel.getPropertyLookupSettings().get(employeeNameProperty).getCaseSensitiveValue().set(false);
    lookupModel.getPropertyLookupSettings().get(employeeJobProperty).getCaseSensitiveValue().set(false);
    result = lookupModel.performQuery();
    assertTrue("Result should contain John", contains(result, "John"));
    assertTrue("Result should contain johnson", contains(result, "johnson"));
    assertFalse("Result should not contain Andy", contains(result, "Andy"));
    assertFalse("Result should not contain Andrew", contains(result, "Andrew"));

    lookupModel.getMultipleItemSeparatorValue().set(";");
    lookupModel.setSearchString("andy ; Andrew ");//spaces should be trimmed away
    result = lookupModel.performQuery();
    assertEquals("Result count should be 2", 2, result.size());
    assertTrue("Result should contain Andy", contains(result, "Andy"));
    assertTrue("Result should contain Andrew", contains(result, "Andrew"));

    lookupModel.setSearchString("andy;Andrew");
    result = lookupModel.performQuery();
    assertEquals("Result count should be 2", 2, result.size());
    assertTrue("Result should contain Andy", contains(result, "Andy"));
    assertTrue("Result should contain Andrew", contains(result, "Andrew"));
    lookupModel.setSelectedEntities(result);
    assertTrue("Search string should represent the selected items", lookupModel.searchStringRepresentsSelected());

    lookupModel.setSearchString("and; rew");
    lookupModel.getPropertyLookupSettings().get(employeeNameProperty).getWildcardPrefixValue().set(true);
    lookupModel.getPropertyLookupSettings().get(employeeJobProperty).getWildcardPrefixValue().set(true);
    lookupModel.getPropertyLookupSettings().get(employeeNameProperty).getWildcardPostfixValue().set(false);
    lookupModel.getPropertyLookupSettings().get(employeeJobProperty).getWildcardPostfixValue().set(false);
    result = lookupModel.performQuery();
    assertEquals("Result count should be 1", 1, result.size());
    assertFalse("Result should not contain Andy", contains(result, "Andy"));
    assertTrue("Result should contain Andrew", contains(result, "Andrew"));

    lookupModel.setSearchString("Joh");
    lookupModel.getPropertyLookupSettings().get(employeeNameProperty).getCaseSensitiveValue().set(true);
    lookupModel.getPropertyLookupSettings().get(employeeJobProperty).getCaseSensitiveValue().set(true);
    lookupModel.getPropertyLookupSettings().get(employeeNameProperty).getWildcardPostfixValue().set(true);
    lookupModel.getPropertyLookupSettings().get(employeeJobProperty).getWildcardPostfixValue().set(true);
    lookupModel.setAdditionalConditionProvider(() ->
            ENTITY_CONDITIONS.propertyCondition(ENTITIES.getColumnProperty(TestDomain.T_EMP, TestDomain.EMP_JOB),
                    Condition.Type.NOT_LIKE, "MANAGER"));
    result = lookupModel.performQuery();
    assertTrue("Result should contain john", contains(result, "John"));
    assertFalse("Result should not contain johnson", contains(result, "johnson"));
  }

  @Test
  public void setAdditionalLookupCondition() {
    lookupModel.getMultipleSelectionAllowedValue().set(false);
    lookupModel.setWildcard("%");
    lookupModel.setSearchString("johnson");
    List<Entity> result = lookupModel.performQuery();
    assertTrue("A single result should be returned", result.size() == 1);
    lookupModel.setSelectedEntities(result);
    lookupModel.setAdditionalConditionProvider(() -> Conditions.<Property.ColumnProperty>stringCondition("1 = 2"));
    assertEquals(1, lookupModel.getSelectedEntities().size());
    result = lookupModel.performQuery();
    assertTrue("No result should be returned", result.isEmpty());
  }

  @Before
  public void setUp() throws Exception {
    lookupProperties = Arrays.asList(ENTITIES.getColumnProperty(TestDomain.T_EMP, TestDomain.EMP_NAME),
                    ENTITIES.getColumnProperty(TestDomain.T_EMP, TestDomain.EMP_JOB));
    lookupModel = new DefaultEntityLookupModel(TestDomain.T_EMP, CONNECTION_PROVIDER, lookupProperties);

    CONNECTION_PROVIDER.getConnection().beginTransaction();
    setupData();
  }

  @After
  public void tearDown() throws Exception {
    CONNECTION_PROVIDER.getConnection().rollbackTransaction();
  }

  private boolean contains(final List<Entity> result, final String employeeName) {
    for (final Entity entity : result) {
      if (entity.getString(TestDomain.EMP_NAME).equals(employeeName)) {
        return true;
      }
    }

    return false;
  }

  private void setupData() throws Exception {
    final Entity dept = ENTITIES.entity(TestDomain.T_DEPARTMENT);
    dept.put(TestDomain.DEPARTMENT_ID, 88);
    dept.put(TestDomain.DEPARTMENT_LOCATION, "TestLoc");
    dept.put(TestDomain.DEPARTMENT_NAME, "TestDept");

    final Entity emp = ENTITIES.entity(TestDomain.T_EMP);
    emp.put(TestDomain.EMP_DEPARTMENT_FK, dept);
    emp.put(TestDomain.EMP_COMMISSION, 1000d);
    emp.put(TestDomain.EMP_HIREDATE, new Date());
    emp.put(TestDomain.EMP_JOB, "CLERK");
    emp.put(TestDomain.EMP_NAME, "John");
    emp.put(TestDomain.EMP_SALARY, 1000d);

    final Entity emp2 = ENTITIES.entity(TestDomain.T_EMP);
    emp2.put(TestDomain.EMP_DEPARTMENT_FK, dept);
    emp2.put(TestDomain.EMP_COMMISSION, 1000d);
    emp2.put(TestDomain.EMP_HIREDATE, new Date());
    emp2.put(TestDomain.EMP_JOB, "MANAGER");
    emp2.put(TestDomain.EMP_NAME, "johnson");
    emp2.put(TestDomain.EMP_SALARY, 1000d);

    final Entity emp3 = ENTITIES.entity(TestDomain.T_EMP);
    emp3.put(TestDomain.EMP_DEPARTMENT_FK, dept);
    emp3.put(TestDomain.EMP_COMMISSION, 1000d);
    emp3.put(TestDomain.EMP_HIREDATE, new Date());
    emp3.put(TestDomain.EMP_JOB, "CLERK");
    emp3.put(TestDomain.EMP_NAME, "Andy");
    emp3.put(TestDomain.EMP_SALARY, 1000d);

    final Entity emp4 = ENTITIES.entity(TestDomain.T_EMP);
    emp4.put(TestDomain.EMP_DEPARTMENT_FK, dept);
    emp4.put(TestDomain.EMP_COMMISSION, 1000d);
    emp4.put(TestDomain.EMP_HIREDATE, new Date());
    emp4.put(TestDomain.EMP_JOB, "MANAGER");
    emp4.put(TestDomain.EMP_NAME, "Andrew");
    emp4.put(TestDomain.EMP_SALARY, 1000d);

    CONNECTION_PROVIDER.getConnection().insert(Arrays.asList(dept, emp, emp2, emp3, emp4));
  }
}
