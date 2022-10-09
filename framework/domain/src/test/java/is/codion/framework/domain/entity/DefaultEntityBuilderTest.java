/*
 * Copyright (c) 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import is.codion.framework.domain.DefaultDomain;
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.TestDomain;
import is.codion.framework.domain.TestDomain.Department;
import is.codion.framework.domain.TestDomain.Employee;

import org.junit.jupiter.api.Test;

import static is.codion.framework.domain.DomainType.domainType;
import static is.codion.framework.domain.entity.EntityDefinition.definition;
import static is.codion.framework.domain.property.Property.*;
import static org.junit.jupiter.api.Assertions.*;

public final class DefaultEntityBuilderTest {

  private static final Entities ENTITIES = new TestDomain().entities();

  @Test
  void valueOrder() {
    Entity department = ENTITIES.builder(Department.TYPE)
            .with(Department.NO, 10)
            .with(Department.NAME, "Test")
            .build();
    Entity employee = ENTITIES.builder(Employee.TYPE)
            .with(Employee.DEPARTMENT_NO, 2)
            //overwrites the department no. value from above
            //with the one in the department entity
            .with(Employee.DEPARTMENT_FK, department)
            .build();

    assertEquals(10, employee.get(Employee.DEPARTMENT_NO));
    assertNotNull(employee.get(Employee.DEPARTMENT_FK));

    employee = ENTITIES.builder(Employee.TYPE)
            .with(Employee.DEPARTMENT_FK, department)
            //overwrites the department no. value from the department entity
            //with this one and removes the foreign key entity, since it's now invalid
            .with(Employee.DEPARTMENT_NO, 2)
            .build();

    assertEquals(2, employee.get(Employee.DEPARTMENT_NO));
    assertNull(employee.get(Employee.DEPARTMENT_FK));
    assertFalse(employee.isLoaded(Employee.DEPARTMENT_FK));
  }

  @Test
  void defaultValues() {
    DomainType domainType = domainType("domainType");
    EntityType entityType = domainType.entityType("entityWithDefaultValues");
    Attribute<Integer> id = entityType.integerAttribute("id");
    Attribute<String> name = entityType.stringAttribute("name");
    Attribute<Integer> value = entityType.integerAttribute("value");
    Attribute<Integer> derivedValue = entityType.integerAttribute("derivedValue");

    class TestDomain extends DefaultDomain {
      public TestDomain() {
        super(domainType);
        add(definition(
                primaryKeyProperty(id),
                columnProperty(name)
                        .defaultValue("DefName"),
                columnProperty(value)
                        .defaultValue(42),
                derivedProperty(derivedValue, sourceValues -> {
                  Integer sourceValue = sourceValues.get(value);

                  return sourceValue == null ? null : sourceValue + 1;
                }, value))
                .tableName("tableName"));
      }
    }
    Entities entities = new TestDomain().entities();

    assertThrows(IllegalArgumentException.class, () -> entities.builder(entityType)
            .with(derivedValue, -42));

    Entity entity = entities.builder(entityType)
            .withDefaultValues()
            .build();
    assertTrue(entity.isNull(id));
    assertEquals("DefName", entity.get(name));
    assertEquals(42, entity.get(value));
    assertEquals(43, entity.get(derivedValue));
  }
}
