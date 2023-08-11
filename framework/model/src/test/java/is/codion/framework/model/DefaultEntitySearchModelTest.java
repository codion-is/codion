/*
 * Copyright (c) 2009 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.model;

import is.codion.common.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.criteria.Criteria;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.domain.entity.Column;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.property.Property;
import is.codion.framework.model.test.TestDomain;
import is.codion.framework.model.test.TestDomain.Department;
import is.codion.framework.model.test.TestDomain.Employee;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import static is.codion.framework.db.criteria.Criteria.attribute;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;

public final class DefaultEntitySearchModelTest {

  private static final User UNIT_TEST_USER =
          User.parse(System.getProperty("codion.test.user", "scott:tiger"));

  private static final Entities ENTITIES = new TestDomain().entities();

  private static final EntityConnectionProvider CONNECTION_PROVIDER = LocalEntityConnectionProvider.builder()
          .domain(new TestDomain())
          .user(UNIT_TEST_USER)
          .build();

  private EntitySearchModel searchModel;
  private Collection<Column<String>> searchAttributes;

  @Test
  void constructorNullEntityType() {
    assertThrows(NullPointerException.class, () -> new DefaultEntitySearchModel.DefaultBuilder(null, CONNECTION_PROVIDER));
  }

  @Test
  void constructorNullConnectionProvider() {
    assertThrows(NullPointerException.class, () -> new DefaultEntitySearchModel.DefaultBuilder(Employee.TYPE, null));
  }

  @Test
  void constructorNullSearchProperties() {
    assertThrows(NullPointerException.class, () -> new DefaultEntitySearchModel.DefaultBuilder(Employee.TYPE, CONNECTION_PROVIDER).searchAttributes(null));
  }

  @Test
  void searchWithNoSearchProperties() {
    assertThrows(IllegalArgumentException.class, () -> new DefaultEntitySearchModel.DefaultBuilder(Employee.TYPE, CONNECTION_PROVIDER).searchAttributes(emptyList()));
  }

  @Test
  void constructorIncorrectEntitySearchProperty() {
    assertThrows(IllegalArgumentException.class, () -> new DefaultEntitySearchModel.DefaultBuilder(Employee.TYPE, CONNECTION_PROVIDER)
            .searchAttributes(singletonList(Department.NAME)));
  }

  @Test
  void theRest() {
    searchModel.setDescription("description");
    assertEquals("description", searchModel.getDescription());
    assertNotNull(searchModel.connectionProvider());
    assertTrue(searchModel.searchAttributes().containsAll(searchAttributes));
    assertNotNull(searchModel.wildcardValue().get());
  }

  @Test
  void wrongEntityType() {
    assertThrows(IllegalArgumentException.class, () -> searchModel.setSelectedEntity(ENTITIES.entity(Department.TYPE)));
  }

  @Test
  void setSingleSelectionEnabled() {
    searchModel.singleSelectionState().set(true);
    List<Entity> entities = asList(ENTITIES.entity(Employee.TYPE), ENTITIES.entity(Employee.TYPE));
    assertThrows(IllegalArgumentException.class, () -> searchModel.setSelectedEntities(entities));
  }

  @Test
  void setToStringProvider() {
    Property<?> job = ENTITIES.definition(Employee.TYPE).property(Employee.JOB);
    searchModel.setToStringProvider(entity -> entity.toString(job.attribute()));
    Entity employee = ENTITIES.builder(Employee.TYPE)
            .with(Employee.NAME, "Darri")
            .with(Employee.JOB, "CLERK")
            .build();
    searchModel.setSelectedEntities(singletonList(employee));
    assertEquals(searchModel.getSearchString(), "CLERK");
    searchModel.setToStringProvider(null);
    searchModel.setSelectedEntities(singletonList(employee));
    assertEquals(searchModel.getSearchString(), "Darri");
  }

  @Test
  void searchModel() {
    searchModel.singleSelectionState().set(false);
    searchModel.wildcardValue().set('%');
    searchModel.setSearchString("joh");
    assertTrue(searchModel.selectionEmptyObserver().get());
    assertFalse(searchModel.searchStringRepresentsSelected());
    List<Entity> result = searchModel.performQuery();
    assertTrue(result.size() > 0);
    assertTrue(contains(result, "John"));
    assertTrue(contains(result, "johnson"));
    assertFalse(contains(result, "Andy"));
    assertFalse(contains(result, "Andrew"));
    assertEquals(searchModel.getSearchString(), "joh");
    searchModel.setSelectedEntities(result);
    assertFalse(searchModel.selectionEmptyObserver().get());
    assertEquals("John" + searchModel.multipleItemSeparatorValue().get() + "johnson", searchModel.getSearchString());

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

    searchModel.attributeSearchSettings().get(Employee.NAME).wildcardPrefixState().set(false);
    searchModel.attributeSearchSettings().get(Employee.JOB).wildcardPrefixState().set(false);
    searchModel.setSearchString("jo,cl");
    result = searchModel.performQuery();
    assertTrue(contains(result, "John"));
    assertTrue(contains(result, "johnson"));
    assertTrue(contains(result, "Andy"));
    assertFalse(contains(result, "Andrew"));

    searchModel.setSearchString("Joh");
    searchModel.attributeSearchSettings().get(Employee.NAME).caseSensitiveState().set(true);
    searchModel.attributeSearchSettings().get(Employee.JOB).caseSensitiveState().set(true);
    result = searchModel.performQuery();
    assertEquals(1, result.size());
    assertTrue(contains(result, "John"));
    assertFalse(contains(result, "johnson"));
    searchModel.attributeSearchSettings().get(Employee.NAME).wildcardPrefixState().set(false);
    searchModel.attributeSearchSettings().get(Employee.JOB).wildcardPrefixState().set(false);
    searchModel.attributeSearchSettings().get(Employee.NAME).caseSensitiveState().set(false);
    searchModel.attributeSearchSettings().get(Employee.JOB).caseSensitiveState().set(false);
    result = searchModel.performQuery();
    assertTrue(contains(result, "John"));
    assertTrue(contains(result, "johnson"));
    assertFalse(contains(result, "Andy"));
    assertFalse(contains(result, "Andrew"));

    searchModel.multipleItemSeparatorValue().set(";");
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
    searchModel.attributeSearchSettings().get(Employee.NAME).wildcardPrefixState().set(true);
    searchModel.attributeSearchSettings().get(Employee.JOB).wildcardPrefixState().set(true);
    searchModel.attributeSearchSettings().get(Employee.NAME).wildcardPostfixState().set(false);
    searchModel.attributeSearchSettings().get(Employee.JOB).wildcardPostfixState().set(false);
    result = searchModel.performQuery();
    assertEquals(1, result.size());
    assertFalse(contains(result, "Andy"));
    assertTrue(contains(result, "Andrew"));

    searchModel.setSearchString("Joh");
    searchModel.attributeSearchSettings().get(Employee.NAME).caseSensitiveState().set(true);
    searchModel.attributeSearchSettings().get(Employee.JOB).caseSensitiveState().set(true);
    searchModel.attributeSearchSettings().get(Employee.NAME).wildcardPostfixState().set(true);
    searchModel.attributeSearchSettings().get(Employee.JOB).wildcardPostfixState().set(true);
    searchModel.setAdditionalCriteriaSupplier(() -> attribute(Employee.JOB).notEqualTo("MANAGER"));
    result = searchModel.performQuery();
    assertTrue(contains(result, "John"));
    assertFalse(contains(result, "johnson"));
  }

  @Test
  void setAdditionalCriteriaProvider() {
    searchModel.singleSelectionState().set(true);
    searchModel.wildcardValue().set('%');
    searchModel.setSearchString("johnson");
    List<Entity> result = searchModel.performQuery();
    assertEquals(1, result.size());
    searchModel.setSelectedEntities(result);
    searchModel.setAdditionalCriteriaSupplier(() ->
            Criteria.customCriteria(Employee.CRITERIA_1_TYPE));
    assertEquals(1, searchModel.getSelectedEntities().size());
    result = searchModel.performQuery();
    assertTrue(result.isEmpty());
  }

  @BeforeEach
  void setUp() throws Exception {
    searchAttributes = asList(Employee.NAME, Employee.JOB);
    searchModel = new DefaultEntitySearchModel.DefaultBuilder(Employee.TYPE, CONNECTION_PROVIDER)
            .searchAttributes(searchAttributes)
            .build();

    CONNECTION_PROVIDER.connection().beginTransaction();
    setupData();
  }

  @AfterEach
  void tearDown() {
    CONNECTION_PROVIDER.connection().rollbackTransaction();
  }

  private static boolean contains(List<Entity> result, String employeeName) {
    return result.stream().anyMatch(entity -> entity.get(Employee.NAME).equals(employeeName));
  }

  private static void setupData() throws Exception {
    Entity dept = ENTITIES.builder(Department.TYPE)
            .with(Department.ID, 88)
            .with(Department.LOCATION, "TestLoc")
            .with(Department.NAME, "TestDept")
            .build();

    Entity emp = ENTITIES.builder(Employee.TYPE)
            .with(Employee.DEPARTMENT_FK, dept)
            .with(Employee.COMMISSION, 1000d)
            .with(Employee.HIREDATE, LocalDate.now())
            .with(Employee.JOB, "CLERK")
            .with(Employee.NAME, "John")
            .with(Employee.SALARY, 1000d)
            .build();

    Entity emp2 = ENTITIES.builder(Employee.TYPE)
            .with(Employee.DEPARTMENT_FK, dept)
            .with(Employee.COMMISSION, 1000d)
            .with(Employee.HIREDATE, LocalDate.now())
            .with(Employee.JOB, "MANAGER")
            .with(Employee.NAME, "johnson")
            .with(Employee.SALARY, 1000d)
            .build();

    Entity emp3 = ENTITIES.builder(Employee.TYPE)
            .with(Employee.DEPARTMENT_FK, dept)
            .with(Employee.COMMISSION, 1000d)
            .with(Employee.HIREDATE, LocalDate.now())
            .with(Employee.JOB, "CLERK")
            .with(Employee.NAME, "Andy")
            .with(Employee.SALARY, 1000d)
            .build();

    Entity emp4 = ENTITIES.builder(Employee.TYPE)
            .with(Employee.DEPARTMENT_FK, dept)
            .with(Employee.COMMISSION, 1000d)
            .with(Employee.HIREDATE, LocalDate.now())
            .with(Employee.JOB, "MANAGER")
            .with(Employee.NAME, "Andrew")
            .with(Employee.SALARY, 1000d)
            .build();

    CONNECTION_PROVIDER.connection().insert(asList(dept, emp, emp2, emp3, emp4));
  }
}
