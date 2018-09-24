/*
 * Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public final class DefaultEntityLookupModelTest {

  private static final Entities ENTITIES = new TestDomain();
  private static final EntityConnectionProvider CONNECTION_PROVIDER = new LocalEntityConnectionProvider(ENTITIES, new User(
          System.getProperty("jminor.unittest.username", "scott"),
          System.getProperty("jminor.unittest.password", "tiger").toCharArray()), Databases.getInstance());
  private static final EntityConditions ENTITY_CONDITIONS = CONNECTION_PROVIDER.getConditions();

  private EntityLookupModel lookupModel;
  private Collection<Property.ColumnProperty> lookupProperties;

  @Test
  public void constructorNullEntityId() {
    assertThrows(NullPointerException.class, () -> new DefaultEntityLookupModel(null, CONNECTION_PROVIDER, new ArrayList<>()));
  }

  @Test
  public void constructorNullConnectionProvider() {
    assertThrows(NullPointerException.class, () -> new DefaultEntityLookupModel(TestDomain.T_EMP, null, new ArrayList<>()));
  }

  @Test
  public void constructorNullLookupProperties() {
    assertThrows(NullPointerException.class, () -> new DefaultEntityLookupModel(TestDomain.T_EMP, CONNECTION_PROVIDER, null));
  }

  @Test
  public void lookupWithNoLookupProperties() {
    assertThrows(IllegalStateException.class, () -> new DefaultEntityLookupModel(TestDomain.T_EMP, CONNECTION_PROVIDER, Collections.emptyList()).performQuery());
  }

  @Test
  public void constructorNonStringLookupProperty() {
    assertThrows(IllegalArgumentException.class, () -> new DefaultEntityLookupModel(TestDomain.T_EMP, CONNECTION_PROVIDER,
            Collections.singletonList(ENTITIES.getColumnProperty(TestDomain.T_EMP, TestDomain.EMP_COMMISSION))));
  }

  @Test
  public void constructorIncorrectEntityLookupProperty() {
    assertThrows(IllegalArgumentException.class, () -> new DefaultEntityLookupModel(TestDomain.T_EMP, CONNECTION_PROVIDER,
            Collections.singletonList(ENTITIES.getColumnProperty(TestDomain.T_DEPARTMENT, TestDomain.DEPARTMENT_NAME))));
  }

  @Test
  public void theRest() {
    lookupModel.setDescription("description");
    assertEquals("description", lookupModel.getDescription());
    assertNotNull(lookupModel.getConnectionProvider());
    assertTrue(lookupModel.getLookupProperties().containsAll(lookupProperties));
    assertNotNull(lookupModel.getWildcard());
  }

  @Test
  public void setSelectedEntitiesMultipleNotAllowed() {
    lookupModel.getMultipleSelectionAllowedValue().set(false);
    final Collection<Entity> entities = Arrays.asList(ENTITIES.entity(TestDomain.T_EMP), ENTITIES.entity(TestDomain.T_EMP));
    assertThrows(IllegalArgumentException.class, () -> lookupModel.setSelectedEntities(entities));
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
    assertTrue(result.size() > 0);
    assertTrue(contains(result, "John"));
    assertTrue(contains(result, "johnson"));
    assertFalse(contains(result, "Andy"));
    assertFalse(contains(result, "Andrew"));
    assertEquals(lookupModel.getSearchString(), "joh");
    lookupModel.setSelectedEntities(result);
    assertEquals("John" + lookupModel.getMultipleItemSeparatorValue().get() + "johnson", lookupModel.getSearchString());

    lookupModel.setSearchString("jo");
    result = lookupModel.performQuery();
    assertTrue(contains(result, "John"));
    assertTrue(contains(result, "johnson"));
    assertFalse(contains(result, "Andy"));
    assertFalse(contains(result, "Andrew"));

    lookupModel.setSearchString("le");
    result = lookupModel.performQuery();
    assertTrue(contains(result, "John"));
    assertFalse(contains(result, "johnson"));
    assertTrue(contains(result, "Andy"));
    assertFalse(contains(result, "Andrew"));

    final Property.ColumnProperty employeeNameProperty = ENTITIES.getColumnProperty(TestDomain.T_EMP, TestDomain.EMP_NAME);
    final Property.ColumnProperty employeeJobProperty = ENTITIES.getColumnProperty(TestDomain.T_EMP, TestDomain.EMP_JOB);

    lookupModel.getPropertyLookupSettings().get(employeeNameProperty).getWildcardPrefixValue().set(false);
    lookupModel.getPropertyLookupSettings().get(employeeJobProperty).getWildcardPrefixValue().set(false);
    lookupModel.setSearchString("jo,cl");
    result = lookupModel.performQuery();
    assertTrue(contains(result, "John"));
    assertTrue(contains(result, "johnson"));
    assertTrue(contains(result, "Andy"));
    assertFalse(contains(result, "Andrew"));

    lookupModel.setSearchString("Joh");
    lookupModel.getPropertyLookupSettings().get(employeeNameProperty).getCaseSensitiveValue().set(true);
    lookupModel.getPropertyLookupSettings().get(employeeJobProperty).getCaseSensitiveValue().set(true);
    result = lookupModel.performQuery();
    assertEquals(1, result.size());
    assertTrue(contains(result, "John"));
    assertFalse(contains(result, "johnson"));
    lookupModel.getPropertyLookupSettings().get(employeeNameProperty).getWildcardPrefixValue().set(false);
    lookupModel.getPropertyLookupSettings().get(employeeJobProperty).getWildcardPrefixValue().set(false);
    lookupModel.getPropertyLookupSettings().get(employeeNameProperty).getCaseSensitiveValue().set(false);
    lookupModel.getPropertyLookupSettings().get(employeeJobProperty).getCaseSensitiveValue().set(false);
    result = lookupModel.performQuery();
    assertTrue(contains(result, "John"));
    assertTrue(contains(result, "johnson"));
    assertFalse(contains(result, "Andy"));
    assertFalse(contains(result, "Andrew"));

    lookupModel.getMultipleItemSeparatorValue().set(";");
    lookupModel.setSearchString("andy ; Andrew ");//spaces should be trimmed away
    result = lookupModel.performQuery();
    assertEquals(2, result.size());
    assertTrue(contains(result, "Andy"));
    assertTrue(contains(result, "Andrew"));

    lookupModel.setSearchString("andy;Andrew");
    result = lookupModel.performQuery();
    assertEquals(2, result.size());
    assertTrue(contains(result, "Andy"));
    assertTrue(contains(result, "Andrew"));
    lookupModel.setSelectedEntities(result);
    assertTrue(lookupModel.searchStringRepresentsSelected());

    lookupModel.setSearchString("and; rew");
    lookupModel.getPropertyLookupSettings().get(employeeNameProperty).getWildcardPrefixValue().set(true);
    lookupModel.getPropertyLookupSettings().get(employeeJobProperty).getWildcardPrefixValue().set(true);
    lookupModel.getPropertyLookupSettings().get(employeeNameProperty).getWildcardPostfixValue().set(false);
    lookupModel.getPropertyLookupSettings().get(employeeJobProperty).getWildcardPostfixValue().set(false);
    result = lookupModel.performQuery();
    assertEquals(1, result.size());
    assertFalse(contains(result, "Andy"));
    assertTrue(contains(result, "Andrew"));

    lookupModel.setSearchString("Joh");
    lookupModel.getPropertyLookupSettings().get(employeeNameProperty).getCaseSensitiveValue().set(true);
    lookupModel.getPropertyLookupSettings().get(employeeJobProperty).getCaseSensitiveValue().set(true);
    lookupModel.getPropertyLookupSettings().get(employeeNameProperty).getWildcardPostfixValue().set(true);
    lookupModel.getPropertyLookupSettings().get(employeeJobProperty).getWildcardPostfixValue().set(true);
    lookupModel.setAdditionalConditionProvider(() ->
            ENTITY_CONDITIONS.propertyCondition(ENTITIES.getColumnProperty(TestDomain.T_EMP, TestDomain.EMP_JOB),
                    Condition.Type.NOT_LIKE, "MANAGER"));
    result = lookupModel.performQuery();
    assertTrue(contains(result, "John"));
    assertFalse(contains(result, "johnson"));
  }

  @Test
  public void setAdditionalLookupCondition() {
    lookupModel.getMultipleSelectionAllowedValue().set(false);
    lookupModel.setWildcard("%");
    lookupModel.setSearchString("johnson");
    List<Entity> result = lookupModel.performQuery();
    assertEquals(1, result.size());
    lookupModel.setSelectedEntities(result);
    lookupModel.setAdditionalConditionProvider(() -> Conditions.<Property.ColumnProperty>stringCondition("1 = 2"));
    assertEquals(1, lookupModel.getSelectedEntities().size());
    result = lookupModel.performQuery();
    assertTrue(result.isEmpty());
  }

  @BeforeEach
  public void setUp() throws Exception {
    lookupProperties = Arrays.asList(ENTITIES.getColumnProperty(TestDomain.T_EMP, TestDomain.EMP_NAME),
            ENTITIES.getColumnProperty(TestDomain.T_EMP, TestDomain.EMP_JOB));
    lookupModel = new DefaultEntityLookupModel(TestDomain.T_EMP, CONNECTION_PROVIDER, lookupProperties);

    CONNECTION_PROVIDER.getConnection().beginTransaction();
    setupData();
  }

  @AfterEach
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
