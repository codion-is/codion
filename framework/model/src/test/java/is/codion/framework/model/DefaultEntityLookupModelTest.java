/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.model;

import is.codion.common.db.database.DatabaseFactory;
import is.codion.common.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.condition.Conditions;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.property.Property;
import is.codion.framework.model.tests.TestDomain;

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
          User.parseUser(System.getProperty("codion.test.user", "scott:tiger"));
  private static final Entities ENTITIES = new TestDomain().getEntities();
  private static final EntityConnectionProvider CONNECTION_PROVIDER = new LocalEntityConnectionProvider(
          DatabaseFactory.getDatabase()).setDomainClassName(TestDomain.class.getName()).setUser(UNIT_TEST_USER);

  private EntityLookupModel lookupModel;
  private Collection<Attribute<String>> lookupAttributes;

  @Test
  public void constructorNullEntityType() {
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
  public void constructorIncorrectEntityLookupProperty() {
    assertThrows(IllegalArgumentException.class, () -> new DefaultEntityLookupModel(TestDomain.T_EMP, CONNECTION_PROVIDER,
            singletonList(TestDomain.DEPARTMENT_NAME)));
  }

  @Test
  public void theRest() {
    lookupModel.setDescription("description");
    assertEquals("description", lookupModel.getDescription());
    assertNotNull(lookupModel.getConnectionProvider());
    assertTrue(lookupModel.getLookupAttributes().containsAll(lookupAttributes));
    assertNotNull(lookupModel.getWildcard());
  }

  @Test
  public void wrongEntityType() {
    assertThrows(IllegalArgumentException.class, () -> lookupModel.setSelectedEntity(ENTITIES.entity(TestDomain.T_DEPARTMENT)));
  }

  @Test
  public void setMultipleSelectionNotEnabled() {
    lookupModel.getMultipleSelectionEnabledValue().set(false);
    final List<Entity> entities = asList(ENTITIES.entity(TestDomain.T_EMP), ENTITIES.entity(TestDomain.T_EMP));
    assertThrows(IllegalArgumentException.class, () -> lookupModel.setSelectedEntities(entities));
  }

  @Test
  public void setToStringProvider() {
    final Property<?> job = ENTITIES.getDefinition(TestDomain.T_EMP).getProperty(TestDomain.EMP_JOB);
    lookupModel.setToStringProvider(entity -> entity.getAsString(job.getAttribute()));
    final Entity employee = ENTITIES.entity(TestDomain.T_EMP);
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

    lookupModel.getAttributeLookupSettings().get(TestDomain.EMP_NAME).getWildcardPrefixValue().set(false);
    lookupModel.getAttributeLookupSettings().get(TestDomain.EMP_JOB).getWildcardPrefixValue().set(false);
    lookupModel.setSearchString("jo,cl");
    result = lookupModel.performQuery();
    assertTrue(contains(result, "John"));
    assertTrue(contains(result, "johnson"));
    assertTrue(contains(result, "Andy"));
    assertFalse(contains(result, "Andrew"));

    lookupModel.setSearchString("Joh");
    lookupModel.getAttributeLookupSettings().get(TestDomain.EMP_NAME).getCaseSensitiveValue().set(true);
    lookupModel.getAttributeLookupSettings().get(TestDomain.EMP_JOB).getCaseSensitiveValue().set(true);
    result = lookupModel.performQuery();
    assertEquals(1, result.size());
    assertTrue(contains(result, "John"));
    assertFalse(contains(result, "johnson"));
    lookupModel.getAttributeLookupSettings().get(TestDomain.EMP_NAME).getWildcardPrefixValue().set(false);
    lookupModel.getAttributeLookupSettings().get(TestDomain.EMP_JOB).getWildcardPrefixValue().set(false);
    lookupModel.getAttributeLookupSettings().get(TestDomain.EMP_NAME).getCaseSensitiveValue().set(false);
    lookupModel.getAttributeLookupSettings().get(TestDomain.EMP_JOB).getCaseSensitiveValue().set(false);
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
    lookupModel.getAttributeLookupSettings().get(TestDomain.EMP_NAME).getWildcardPrefixValue().set(true);
    lookupModel.getAttributeLookupSettings().get(TestDomain.EMP_JOB).getWildcardPrefixValue().set(true);
    lookupModel.getAttributeLookupSettings().get(TestDomain.EMP_NAME).getWildcardPostfixValue().set(false);
    lookupModel.getAttributeLookupSettings().get(TestDomain.EMP_JOB).getWildcardPostfixValue().set(false);
    result = lookupModel.performQuery();
    assertEquals(1, result.size());
    assertFalse(contains(result, "Andy"));
    assertTrue(contains(result, "Andrew"));

    lookupModel.setSearchString("Joh");
    lookupModel.getAttributeLookupSettings().get(TestDomain.EMP_NAME).getCaseSensitiveValue().set(true);
    lookupModel.getAttributeLookupSettings().get(TestDomain.EMP_JOB).getCaseSensitiveValue().set(true);
    lookupModel.getAttributeLookupSettings().get(TestDomain.EMP_NAME).getWildcardPostfixValue().set(true);
    lookupModel.getAttributeLookupSettings().get(TestDomain.EMP_JOB).getWildcardPostfixValue().set(true);
    lookupModel.setAdditionalConditionProvider(() ->
            Conditions.condition(TestDomain.EMP_JOB).notEqualTo("MANAGER"));
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
    lookupModel.setAdditionalConditionProvider(() -> Conditions.customCondition(TestDomain.EMP_CONDITION_1_TYPE));
    assertEquals(1, lookupModel.getSelectedEntities().size());
    result = lookupModel.performQuery();
    assertTrue(result.isEmpty());
  }

  @BeforeEach
  public void setUp() throws Exception {
    lookupAttributes = asList(TestDomain.EMP_NAME, TestDomain.EMP_JOB);
    lookupModel = new DefaultEntityLookupModel(TestDomain.T_EMP, CONNECTION_PROVIDER, lookupAttributes);

    CONNECTION_PROVIDER.getConnection().beginTransaction();
    setupData();
  }

  @AfterEach
  public void tearDown() throws Exception {
    CONNECTION_PROVIDER.getConnection().rollbackTransaction();
  }

  private static boolean contains(final List<Entity> result, final String employeeName) {
    for (final Entity entity : result) {
      if (entity.get(TestDomain.EMP_NAME).equals(employeeName)) {
        return true;
      }
    }

    return false;
  }

  private static void setupData() throws Exception {
    final Entity dept = ENTITIES.entity(TestDomain.T_DEPARTMENT);
    dept.put(TestDomain.DEPARTMENT_ID, 88);
    dept.put(TestDomain.DEPARTMENT_LOCATION, "TestLoc");
    dept.put(TestDomain.DEPARTMENT_NAME, "TestDept");

    final Entity emp = ENTITIES.entity(TestDomain.T_EMP);
    emp.put(TestDomain.EMP_DEPARTMENT_FK, dept);
    emp.put(TestDomain.EMP_COMMISSION, 1000d);
    emp.put(TestDomain.EMP_HIREDATE, LocalDate.now());
    emp.put(TestDomain.EMP_JOB, "CLERK");
    emp.put(TestDomain.EMP_NAME, "John");
    emp.put(TestDomain.EMP_SALARY, 1000d);

    final Entity emp2 = ENTITIES.entity(TestDomain.T_EMP);
    emp2.put(TestDomain.EMP_DEPARTMENT_FK, dept);
    emp2.put(TestDomain.EMP_COMMISSION, 1000d);
    emp2.put(TestDomain.EMP_HIREDATE, LocalDate.now());
    emp2.put(TestDomain.EMP_JOB, "MANAGER");
    emp2.put(TestDomain.EMP_NAME, "johnson");
    emp2.put(TestDomain.EMP_SALARY, 1000d);

    final Entity emp3 = ENTITIES.entity(TestDomain.T_EMP);
    emp3.put(TestDomain.EMP_DEPARTMENT_FK, dept);
    emp3.put(TestDomain.EMP_COMMISSION, 1000d);
    emp3.put(TestDomain.EMP_HIREDATE, LocalDate.now());
    emp3.put(TestDomain.EMP_JOB, "CLERK");
    emp3.put(TestDomain.EMP_NAME, "Andy");
    emp3.put(TestDomain.EMP_SALARY, 1000d);

    final Entity emp4 = ENTITIES.entity(TestDomain.T_EMP);
    emp4.put(TestDomain.EMP_DEPARTMENT_FK, dept);
    emp4.put(TestDomain.EMP_COMMISSION, 1000d);
    emp4.put(TestDomain.EMP_HIREDATE, LocalDate.now());
    emp4.put(TestDomain.EMP_JOB, "MANAGER");
    emp4.put(TestDomain.EMP_NAME, "Andrew");
    emp4.put(TestDomain.EMP_SALARY, 1000d);

    CONNECTION_PROVIDER.getConnection().insert(asList(dept, emp, emp2, emp3, emp4));
  }
}
