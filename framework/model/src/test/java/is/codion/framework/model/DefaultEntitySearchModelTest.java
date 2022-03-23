/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.model;

import is.codion.common.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.condition.Conditions;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.property.Property;
import is.codion.framework.model.test.TestDomain;

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

public final class DefaultEntitySearchModelTest {

  private static final User UNIT_TEST_USER =
          User.parse(System.getProperty("codion.test.user", "scott:tiger"));

  private static final Entities ENTITIES = new TestDomain().getEntities();

  private static final EntityConnectionProvider CONNECTION_PROVIDER = LocalEntityConnectionProvider.builder()
          .domainClassName(TestDomain.class.getName())
          .user(UNIT_TEST_USER)
          .build();

  private EntitySearchModel searchModel;
  private Collection<Attribute<String>> searchAttributes;

  @Test
  void constructorNullEntityType() {
    assertThrows(NullPointerException.class, () -> new DefaultEntitySearchModel(null, CONNECTION_PROVIDER, new ArrayList<>()));
  }

  @Test
  void constructorNullConnectionProvider() {
    assertThrows(NullPointerException.class, () -> new DefaultEntitySearchModel(TestDomain.T_EMP, null, new ArrayList<>()));
  }

  @Test
  void constructorNullSearchProperties() {
    assertThrows(NullPointerException.class, () -> new DefaultEntitySearchModel(TestDomain.T_EMP, CONNECTION_PROVIDER, null));
  }

  @Test
  void searchWithNoSearchProperties() {
    assertThrows(IllegalStateException.class, () -> new DefaultEntitySearchModel(TestDomain.T_EMP, CONNECTION_PROVIDER, emptyList()).performQuery());
  }

  @Test
  void constructorIncorrectEntitySearchProperty() {
    assertThrows(IllegalArgumentException.class, () -> new DefaultEntitySearchModel(TestDomain.T_EMP, CONNECTION_PROVIDER,
            singletonList(TestDomain.DEPARTMENT_NAME)));
  }

  @Test
  void theRest() {
    searchModel.setDescription("description");
    assertEquals("description", searchModel.getDescription());
    assertNotNull(searchModel.getConnectionProvider());
    assertTrue(searchModel.getSearchAttributes().containsAll(searchAttributes));
    assertNotNull(searchModel.getWildcardValue().get());
  }

  @Test
  void wrongEntityType() {
    assertThrows(IllegalArgumentException.class, () -> searchModel.setSelectedEntity(ENTITIES.entity(TestDomain.T_DEPARTMENT)));
  }

  @Test
  void setMultipleSelectionNotEnabled() {
    searchModel.getMultipleSelectionEnabledValue().set(false);
    List<Entity> entities = asList(ENTITIES.entity(TestDomain.T_EMP), ENTITIES.entity(TestDomain.T_EMP));
    assertThrows(IllegalArgumentException.class, () -> searchModel.setSelectedEntities(entities));
  }

  @Test
  void setToStringProvider() {
    Property<?> job = ENTITIES.getDefinition(TestDomain.T_EMP).getProperty(TestDomain.EMP_JOB);
    searchModel.setToStringProvider(entity -> entity.toString(job.getAttribute()));
    Entity employee = ENTITIES.builder(TestDomain.T_EMP)
            .with(TestDomain.EMP_NAME, "Darri")
            .with(TestDomain.EMP_JOB, "CLERK")
            .build();
    searchModel.setSelectedEntities(singletonList(employee));
    assertEquals(searchModel.getSearchString(), "CLERK");
    searchModel.setToStringProvider(null);
    searchModel.setSelectedEntities(singletonList(employee));
    assertEquals(searchModel.getSearchString(), "Darri");
  }

  @Test
  void searchModel() throws Exception {
    searchModel.getMultipleSelectionEnabledValue().set(true);
    searchModel.getWildcardValue().set('%');
    searchModel.setSearchString("joh");
    assertFalse(searchModel.searchStringRepresentsSelected());
    List<Entity> result = searchModel.performQuery();
    assertTrue(result.size() > 0);
    assertTrue(contains(result, "John"));
    assertTrue(contains(result, "johnson"));
    assertFalse(contains(result, "Andy"));
    assertFalse(contains(result, "Andrew"));
    assertEquals(searchModel.getSearchString(), "joh");
    searchModel.setSelectedEntities(result);
    assertEquals("John" + searchModel.getMultipleItemSeparatorValue().get() + "johnson", searchModel.getSearchString());

    searchModel.setSearchString("jo");
    result = searchModel.performQuery();
    assertTrue(contains(result, "John"));
    assertTrue(contains(result, "johnson"));
    assertFalse(contains(result, "Andy"));
    assertFalse(contains(result, "Andrew"));

    searchModel.setSearchString("le");
    result = searchModel.performQuery();
    assertTrue(contains(result, "John"));
    assertFalse(contains(result, "johnson"));
    assertTrue(contains(result, "Andy"));
    assertFalse(contains(result, "Andrew"));

    searchModel.getAttributeSearchSettings().get(TestDomain.EMP_NAME).getWildcardPrefixValue().set(false);
    searchModel.getAttributeSearchSettings().get(TestDomain.EMP_JOB).getWildcardPrefixValue().set(false);
    searchModel.setSearchString("jo,cl");
    result = searchModel.performQuery();
    assertTrue(contains(result, "John"));
    assertTrue(contains(result, "johnson"));
    assertTrue(contains(result, "Andy"));
    assertFalse(contains(result, "Andrew"));

    searchModel.setSearchString("Joh");
    searchModel.getAttributeSearchSettings().get(TestDomain.EMP_NAME).getCaseSensitiveValue().set(true);
    searchModel.getAttributeSearchSettings().get(TestDomain.EMP_JOB).getCaseSensitiveValue().set(true);
    result = searchModel.performQuery();
    assertEquals(1, result.size());
    assertTrue(contains(result, "John"));
    assertFalse(contains(result, "johnson"));
    searchModel.getAttributeSearchSettings().get(TestDomain.EMP_NAME).getWildcardPrefixValue().set(false);
    searchModel.getAttributeSearchSettings().get(TestDomain.EMP_JOB).getWildcardPrefixValue().set(false);
    searchModel.getAttributeSearchSettings().get(TestDomain.EMP_NAME).getCaseSensitiveValue().set(false);
    searchModel.getAttributeSearchSettings().get(TestDomain.EMP_JOB).getCaseSensitiveValue().set(false);
    result = searchModel.performQuery();
    assertTrue(contains(result, "John"));
    assertTrue(contains(result, "johnson"));
    assertFalse(contains(result, "Andy"));
    assertFalse(contains(result, "Andrew"));

    searchModel.getMultipleItemSeparatorValue().set(";");
    searchModel.setSearchString("andy ; Andrew ");//spaces should be trimmed away
    result = searchModel.performQuery();
    assertEquals(2, result.size());
    assertTrue(contains(result, "Andy"));
    assertTrue(contains(result, "Andrew"));

    searchModel.setSearchString("andy;Andrew");
    result = searchModel.performQuery();
    assertEquals(2, result.size());
    assertTrue(contains(result, "Andy"));
    assertTrue(contains(result, "Andrew"));
    searchModel.setSelectedEntities(result);
    assertTrue(searchModel.searchStringRepresentsSelected());

    searchModel.setSearchString("and; rew");
    searchModel.getAttributeSearchSettings().get(TestDomain.EMP_NAME).getWildcardPrefixValue().set(true);
    searchModel.getAttributeSearchSettings().get(TestDomain.EMP_JOB).getWildcardPrefixValue().set(true);
    searchModel.getAttributeSearchSettings().get(TestDomain.EMP_NAME).getWildcardPostfixValue().set(false);
    searchModel.getAttributeSearchSettings().get(TestDomain.EMP_JOB).getWildcardPostfixValue().set(false);
    result = searchModel.performQuery();
    assertEquals(1, result.size());
    assertFalse(contains(result, "Andy"));
    assertTrue(contains(result, "Andrew"));

    searchModel.setSearchString("Joh");
    searchModel.getAttributeSearchSettings().get(TestDomain.EMP_NAME).getCaseSensitiveValue().set(true);
    searchModel.getAttributeSearchSettings().get(TestDomain.EMP_JOB).getCaseSensitiveValue().set(true);
    searchModel.getAttributeSearchSettings().get(TestDomain.EMP_NAME).getWildcardPostfixValue().set(true);
    searchModel.getAttributeSearchSettings().get(TestDomain.EMP_JOB).getWildcardPostfixValue().set(true);
    searchModel.setAdditionalConditionSupplier(() ->
            Conditions.where(TestDomain.EMP_JOB).notEqualTo("MANAGER"));
    result = searchModel.performQuery();
    assertTrue(contains(result, "John"));
    assertFalse(contains(result, "johnson"));
  }

  @Test
  void setAdditionalConditionProvider() {
    searchModel.getMultipleSelectionEnabledValue().set(false);
    searchModel.getWildcardValue().set('%');
    searchModel.setSearchString("johnson");
    List<Entity> result = searchModel.performQuery();
    assertEquals(1, result.size());
    searchModel.setSelectedEntities(result);
    searchModel.setAdditionalConditionSupplier(() -> Conditions.customCondition(TestDomain.EMP_CONDITION_1_TYPE));
    assertEquals(1, searchModel.getSelectedEntities().size());
    result = searchModel.performQuery();
    assertTrue(result.isEmpty());
  }

  @BeforeEach
  void setUp() throws Exception {
    searchAttributes = asList(TestDomain.EMP_NAME, TestDomain.EMP_JOB);
    searchModel = new DefaultEntitySearchModel(TestDomain.T_EMP, CONNECTION_PROVIDER, searchAttributes);

    CONNECTION_PROVIDER.getConnection().beginTransaction();
    setupData();
  }

  @AfterEach
  void tearDown() throws Exception {
    CONNECTION_PROVIDER.getConnection().rollbackTransaction();
  }

  private static boolean contains(List<Entity> result, String employeeName) {
    for (Entity entity : result) {
      if (entity.get(TestDomain.EMP_NAME).equals(employeeName)) {
        return true;
      }
    }

    return false;
  }

  private static void setupData() throws Exception {
    Entity dept = ENTITIES.builder(TestDomain.T_DEPARTMENT)
            .with(TestDomain.DEPARTMENT_ID, 88)
            .with(TestDomain.DEPARTMENT_LOCATION, "TestLoc")
            .with(TestDomain.DEPARTMENT_NAME, "TestDept")
            .build();

    Entity emp = ENTITIES.builder(TestDomain.T_EMP)
            .with(TestDomain.EMP_DEPARTMENT_FK, dept)
            .with(TestDomain.EMP_COMMISSION, 1000d)
            .with(TestDomain.EMP_HIREDATE, LocalDate.now())
            .with(TestDomain.EMP_JOB, "CLERK")
            .with(TestDomain.EMP_NAME, "John")
            .with(TestDomain.EMP_SALARY, 1000d)
            .build();

    Entity emp2 = ENTITIES.builder(TestDomain.T_EMP)
            .with(TestDomain.EMP_DEPARTMENT_FK, dept)
            .with(TestDomain.EMP_COMMISSION, 1000d)
            .with(TestDomain.EMP_HIREDATE, LocalDate.now())
            .with(TestDomain.EMP_JOB, "MANAGER")
            .with(TestDomain.EMP_NAME, "johnson")
            .with(TestDomain.EMP_SALARY, 1000d)
            .build();

    Entity emp3 = ENTITIES.builder(TestDomain.T_EMP)
            .with(TestDomain.EMP_DEPARTMENT_FK, dept)
            .with(TestDomain.EMP_COMMISSION, 1000d)
            .with(TestDomain.EMP_HIREDATE, LocalDate.now())
            .with(TestDomain.EMP_JOB, "CLERK")
            .with(TestDomain.EMP_NAME, "Andy")
            .with(TestDomain.EMP_SALARY, 1000d)
            .build();

    Entity emp4 = ENTITIES.builder(TestDomain.T_EMP)
            .with(TestDomain.EMP_DEPARTMENT_FK, dept)
            .with(TestDomain.EMP_COMMISSION, 1000d)
            .with(TestDomain.EMP_HIREDATE, LocalDate.now())
            .with(TestDomain.EMP_JOB, "MANAGER")
            .with(TestDomain.EMP_NAME, "Andrew")
            .with(TestDomain.EMP_SALARY, 1000d)
            .build();

    CONNECTION_PROVIDER.getConnection().insert(asList(dept, emp, emp2, emp3, emp4));
  }
}
