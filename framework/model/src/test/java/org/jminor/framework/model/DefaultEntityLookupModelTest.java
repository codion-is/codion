/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.model;

import org.jminor.common.db.Operator;
import org.jminor.common.db.database.Databases;
import org.jminor.common.user.User;
import org.jminor.common.user.Users;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.db.condition.Conditions;
import org.jminor.framework.db.local.LocalEntityConnectionProvider;
import org.jminor.framework.domain.Domain;
import org.jminor.framework.domain.entity.Entity;
import org.jminor.framework.domain.property.ColumnProperty;
import org.jminor.framework.domain.property.Property;
import org.jminor.framework.model.tests.TestDomain;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;

public final class DefaultEntityLookupModelTest {

  private static final User UNIT_TEST_USER =
          Users.parseUser(System.getProperty("jminor.test.user", "scott:tiger"));
  private static final Domain DOMAIN = new TestDomain();
  private static final EntityConnectionProvider CONNECTION_PROVIDER = new LocalEntityConnectionProvider(
          Databases.getInstance()).setDomainClassName(TestDomain.class.getName()).setUser(UNIT_TEST_USER);

  private EntityLookupModel lookupModel;
  private Collection<ColumnProperty> lookupProperties;

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
    assertThrows(IllegalStateException.class, () -> new DefaultEntityLookupModel(TestDomain.T_EMP, CONNECTION_PROVIDER, emptyList()).performQuery());
  }

  @Test
  public void constructorNonStringLookupProperty() {
    assertThrows(IllegalArgumentException.class, () -> new DefaultEntityLookupModel(TestDomain.T_EMP, CONNECTION_PROVIDER,
            singletonList(DOMAIN.getDefinition(TestDomain.T_EMP).getColumnProperty(TestDomain.EMP_COMMISSION))));
  }

  @Test
  public void constructorIncorrectEntityLookupProperty() {
    assertThrows(IllegalArgumentException.class, () -> new DefaultEntityLookupModel(TestDomain.T_EMP, CONNECTION_PROVIDER,
            singletonList(DOMAIN.getDefinition(TestDomain.T_DEPARTMENT).getColumnProperty(TestDomain.DEPARTMENT_NAME))));
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
  public void setMultipleSelectionNotEnabled() {
    lookupModel.getMultipleSelectionEnabledValue().set(false);
    final List<Entity> entities = asList(DOMAIN.entity(TestDomain.T_EMP), DOMAIN.entity(TestDomain.T_EMP));
    assertThrows(IllegalArgumentException.class, () -> lookupModel.setSelectedEntities(entities));
  }

  @Test
  public void setToStringProvider() {
    final Property job = DOMAIN.getDefinition(TestDomain.T_EMP).getProperty(TestDomain.EMP_JOB);
    lookupModel.setToStringProvider(entity -> entity.getAsString(job));
    final Entity employee = DOMAIN.entity(TestDomain.T_EMP);
    employee.put(TestDomain.EMP_NAME, "Darri");
    employee.put(TestDomain.EMP_JOB, "CLERK");
    lookupModel.setSelectedEntities(singletonList(employee));
    assertEquals(lookupModel.getSearchString(), "CLERK");
    lookupModel.setToStringProvider(null);
    lookupModel.setSelectedEntities(singletonList(employee));
    assertEquals(lookupModel.getSearchString(), "Darri");
  }

  @Test
  public void lookupModel() throws Exception {
    lookupModel.getMultipleSelectionEnabledValue().set(true);
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

    final ColumnProperty employeeNameProperty = DOMAIN.getDefinition(TestDomain.T_EMP).getColumnProperty(TestDomain.EMP_NAME);
    final ColumnProperty employeeJobProperty = DOMAIN.getDefinition(TestDomain.T_EMP).getColumnProperty(TestDomain.EMP_JOB);

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
            Conditions.propertyCondition(TestDomain.EMP_JOB, Operator.NOT_LIKE, "MANAGER"));
    result = lookupModel.performQuery();
    assertTrue(contains(result, "John"));
    assertFalse(contains(result, "johnson"));
  }

  @Test
  public void setAdditionalLookupCondition() {
    lookupModel.getMultipleSelectionEnabledValue().set(false);
    lookupModel.setWildcard("%");
    lookupModel.setSearchString("johnson");
    List<Entity> result = lookupModel.performQuery();
    assertEquals(1, result.size());
    lookupModel.setSelectedEntities(result);
    lookupModel.setAdditionalConditionProvider(() -> Conditions.customCondition(TestDomain.EMP_CONDITION_1_ID));
    assertEquals(1, lookupModel.getSelectedEntities().size());
    result = lookupModel.performQuery();
    assertTrue(result.isEmpty());
  }

  @BeforeEach
  public void setUp() throws Exception {
    lookupProperties = asList(DOMAIN.getDefinition(TestDomain.T_EMP).getColumnProperty(TestDomain.EMP_NAME),
            DOMAIN.getDefinition(TestDomain.T_EMP).getColumnProperty(TestDomain.EMP_JOB));
    lookupModel = new DefaultEntityLookupModel(TestDomain.T_EMP, CONNECTION_PROVIDER, lookupProperties);

    CONNECTION_PROVIDER.getConnection().beginTransaction();
    setupData();
  }

  @AfterEach
  public void tearDown() throws Exception {
    CONNECTION_PROVIDER.getConnection().rollbackTransaction();
  }

  private static boolean contains(final List<Entity> result, final String employeeName) {
    for (final Entity entity : result) {
      if (entity.getString(TestDomain.EMP_NAME).equals(employeeName)) {
        return true;
      }
    }

    return false;
  }

  private static void setupData() throws Exception {
    final Entity dept = DOMAIN.entity(TestDomain.T_DEPARTMENT);
    dept.put(TestDomain.DEPARTMENT_ID, 88);
    dept.put(TestDomain.DEPARTMENT_LOCATION, "TestLoc");
    dept.put(TestDomain.DEPARTMENT_NAME, "TestDept");

    final Entity emp = DOMAIN.entity(TestDomain.T_EMP);
    emp.put(TestDomain.EMP_DEPARTMENT_FK, dept);
    emp.put(TestDomain.EMP_COMMISSION, 1000d);
    emp.put(TestDomain.EMP_HIREDATE, LocalDate.now());
    emp.put(TestDomain.EMP_JOB, "CLERK");
    emp.put(TestDomain.EMP_NAME, "John");
    emp.put(TestDomain.EMP_SALARY, 1000d);

    final Entity emp2 = DOMAIN.entity(TestDomain.T_EMP);
    emp2.put(TestDomain.EMP_DEPARTMENT_FK, dept);
    emp2.put(TestDomain.EMP_COMMISSION, 1000d);
    emp2.put(TestDomain.EMP_HIREDATE, LocalDate.now());
    emp2.put(TestDomain.EMP_JOB, "MANAGER");
    emp2.put(TestDomain.EMP_NAME, "johnson");
    emp2.put(TestDomain.EMP_SALARY, 1000d);

    final Entity emp3 = DOMAIN.entity(TestDomain.T_EMP);
    emp3.put(TestDomain.EMP_DEPARTMENT_FK, dept);
    emp3.put(TestDomain.EMP_COMMISSION, 1000d);
    emp3.put(TestDomain.EMP_HIREDATE, LocalDate.now());
    emp3.put(TestDomain.EMP_JOB, "CLERK");
    emp3.put(TestDomain.EMP_NAME, "Andy");
    emp3.put(TestDomain.EMP_SALARY, 1000d);

    final Entity emp4 = DOMAIN.entity(TestDomain.T_EMP);
    emp4.put(TestDomain.EMP_DEPARTMENT_FK, dept);
    emp4.put(TestDomain.EMP_COMMISSION, 1000d);
    emp4.put(TestDomain.EMP_HIREDATE, LocalDate.now());
    emp4.put(TestDomain.EMP_JOB, "MANAGER");
    emp4.put(TestDomain.EMP_NAME, "Andrew");
    emp4.put(TestDomain.EMP_SALARY, 1000d);

    CONNECTION_PROVIDER.getConnection().insert(asList(dept, emp, emp2, emp3, emp4));
  }
}
