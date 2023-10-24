/*
 * Copyright (c) 2009 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.model;

import is.codion.common.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.ColumnDefinition;
import is.codion.framework.domain.entity.condition.Condition;
import is.codion.framework.model.test.TestDomain;
import is.codion.framework.model.test.TestDomain.Department;
import is.codion.framework.model.test.TestDomain.Employee;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

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
  private Collection<Column<String>> searchColumns;

  @Test
  void constructorNullEntityType() {
    assertThrows(NullPointerException.class, () -> new DefaultEntitySearchModel.DefaultBuilder(null, CONNECTION_PROVIDER));
  }

  @Test
  void constructorNullConnectionProvider() {
    assertThrows(NullPointerException.class, () -> new DefaultEntitySearchModel.DefaultBuilder(Employee.TYPE, null));
  }

  @Test
  void constructorNullSearchColumns() {
    assertThrows(NullPointerException.class, () -> new DefaultEntitySearchModel.DefaultBuilder(Employee.TYPE, CONNECTION_PROVIDER).searchColumns(null));
  }

  @Test
  void searchWithNoSearchColumns() {
    assertThrows(IllegalArgumentException.class, () -> new DefaultEntitySearchModel.DefaultBuilder(Employee.TYPE, CONNECTION_PROVIDER).searchColumns(emptyList()));
  }

  @Test
  void constructorIncorrectEntitySearchColumn() {
    assertThrows(IllegalArgumentException.class, () -> new DefaultEntitySearchModel.DefaultBuilder(Employee.TYPE, CONNECTION_PROVIDER)
            .searchColumns(singletonList(Department.NAME)));
  }

  @Test
  void theRest() {
    assertNotNull(searchModel.connectionProvider());
    assertTrue(searchModel.searchColumns().containsAll(searchColumns));
    assertNotNull(searchModel.wildcard().get());
  }

  @Test
  void wrongEntityType() {
    assertThrows(IllegalArgumentException.class, () -> searchModel.selectedEntity().set(ENTITIES.entity(Department.TYPE)));
  }

  @Test
  void singleSelection() {
    searchModel.singleSelection().set(true);
    List<Entity> entities = asList(ENTITIES.entity(Employee.TYPE), ENTITIES.entity(Employee.TYPE));
    assertThrows(IllegalArgumentException.class, () -> searchModel.selectedEntities().set(entities));
  }

  @Test
  void stringFunction() {
    ColumnDefinition<?> job = ENTITIES.definition(Employee.TYPE).columns().definition(Employee.JOB);
    searchModel.stringFunction().set(entity -> entity.string(job.attribute()));
    Entity employee = ENTITIES.builder(Employee.TYPE)
            .with(Employee.NAME, "Darri")
            .with(Employee.JOB, "CLERK")
            .build();
    searchModel.selectedEntity().set(employee);
    assertEquals(searchModel.searchString().get(), "CLERK");
    searchModel.stringFunction().set(null);
    searchModel.selectedEntity().set(null);
    searchModel.selectedEntity().set(employee);
    assertEquals(searchModel.searchString().get(), "Darri");
  }

  @Test
  void searchModel() {
    searchModel.singleSelection().set(false);
    searchModel.wildcard().set('%');
    searchModel.searchString().set("joh");
    assertTrue(searchModel.selectionEmpty().get());
    assertTrue(searchModel.searchStringModified().get());
    List<Entity> result = searchModel.search();
    assertFalse(result.isEmpty());
    assertTrue(contains(result, "John"));
    assertTrue(contains(result, "johnson"));
    assertFalse(contains(result, "Andy"));
    assertFalse(contains(result, "Andrew"));
    assertEquals(searchModel.searchString().get(), "joh");
    searchModel.selectedEntities().set(result);
    assertFalse(searchModel.selectionEmpty().get());
    assertEquals("John" + searchModel.separator().get() + "johnson", searchModel.searchString().get());

    searchModel.searchString().set("jo");
    result = searchModel.search();
    assertTrue(contains(result, "John"));
    assertTrue(contains(result, "johnson"));
    assertFalse(contains(result, "Andy"));
    assertFalse(contains(result, "Andrew"));

    searchModel.searchString().set("le");
    result = searchModel.search();
    assertTrue(contains(result, "John"));
    assertFalse(contains(result, "johnson"));
    assertTrue(contains(result, "Andy"));
    assertFalse(contains(result, "Andrew"));

    searchModel.columnSearchSettings().get(Employee.NAME).wildcardPrefix().set(false);
    searchModel.columnSearchSettings().get(Employee.JOB).wildcardPrefix().set(false);
    searchModel.searchString().set("jo,cl");
    result = searchModel.search();
    assertTrue(contains(result, "John"));
    assertTrue(contains(result, "johnson"));
    assertTrue(contains(result, "Andy"));
    assertFalse(contains(result, "Andrew"));

    searchModel.searchString().set("Joh");
    searchModel.columnSearchSettings().get(Employee.NAME).caseSensitive().set(true);
    searchModel.columnSearchSettings().get(Employee.JOB).caseSensitive().set(true);
    result = searchModel.search();
    assertEquals(1, result.size());
    assertTrue(contains(result, "John"));
    assertFalse(contains(result, "johnson"));
    searchModel.columnSearchSettings().get(Employee.NAME).wildcardPrefix().set(false);
    searchModel.columnSearchSettings().get(Employee.JOB).wildcardPrefix().set(false);
    searchModel.columnSearchSettings().get(Employee.NAME).caseSensitive().set(false);
    searchModel.columnSearchSettings().get(Employee.JOB).caseSensitive().set(false);
    result = searchModel.search();
    assertTrue(contains(result, "John"));
    assertTrue(contains(result, "johnson"));
    assertFalse(contains(result, "Andy"));
    assertFalse(contains(result, "Andrew"));

    searchModel.separator().set(";");
    searchModel.searchString().set("andy ; Andrew ");//spaces should be trimmed away
    result = searchModel.search();
    assertEquals(2, result.size());
    assertTrue(contains(result, "Andy"));
    assertTrue(contains(result, "Andrew"));

    searchModel.searchString().set("andy;Andrew");
    result = searchModel.search();
    assertEquals(2, result.size());
    assertTrue(contains(result, "Andy"));
    assertTrue(contains(result, "Andrew"));
    searchModel.selectedEntities().set(result);
    assertFalse(searchModel.searchStringModified().get());

    searchModel.searchString().set("and; rew");
    searchModel.columnSearchSettings().get(Employee.NAME).wildcardPrefix().set(true);
    searchModel.columnSearchSettings().get(Employee.JOB).wildcardPrefix().set(true);
    searchModel.columnSearchSettings().get(Employee.NAME).wildcardPostfix().set(false);
    searchModel.columnSearchSettings().get(Employee.JOB).wildcardPostfix().set(false);
    result = searchModel.search();
    assertEquals(1, result.size());
    assertFalse(contains(result, "Andy"));
    assertTrue(contains(result, "Andrew"));

    searchModel.searchString().set("Joh");
    searchModel.columnSearchSettings().get(Employee.NAME).caseSensitive().set(true);
    searchModel.columnSearchSettings().get(Employee.JOB).caseSensitive().set(true);
    searchModel.columnSearchSettings().get(Employee.NAME).wildcardPostfix().set(true);
    searchModel.columnSearchSettings().get(Employee.JOB).wildcardPostfix().set(true);
    searchModel.condition().set(() -> Employee.JOB.notEqualTo("MANAGER"));
    result = searchModel.search();
    assertTrue(contains(result, "John"));
    assertFalse(contains(result, "johnson"));
  }

  @Test
  void condition() {
    searchModel.singleSelection().set(true);
    searchModel.wildcard().set('%');
    searchModel.searchString().set("johnson");
    List<Entity> result = searchModel.search();
    assertEquals(1, result.size());
    searchModel.selectedEntities().set(result);
    searchModel.condition().set(() ->
            Condition.customCondition(Employee.CONDITION_1_TYPE));
    assertEquals(1, searchModel.selectedEntities().get().size());
    result = searchModel.search();
    assertTrue(result.isEmpty());
  }

  @BeforeEach
  void setUp() throws Exception {
    searchColumns = asList(Employee.NAME, Employee.JOB);
    searchModel = new DefaultEntitySearchModel.DefaultBuilder(Employee.TYPE, CONNECTION_PROVIDER)
            .searchColumns(searchColumns)
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
