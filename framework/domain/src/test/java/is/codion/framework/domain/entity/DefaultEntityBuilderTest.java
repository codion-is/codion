/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2022 - 2023, Björn Darri Sigurðsson.
 */
package is.codion.framework.domain.entity;

import is.codion.framework.domain.DefaultDomain;
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.TestDomain;
import is.codion.framework.domain.TestDomain.Department;
import is.codion.framework.domain.TestDomain.Employee;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.Column;

import org.junit.jupiter.api.Test;

import static is.codion.framework.domain.DomainType.domainType;
import static org.junit.jupiter.api.Assertions.*;

public final class DefaultEntityBuilderTest {

  private static final Entities ENTITIES = new TestDomain().entities();

  @Test
  void valueOrder() {
    Entity department = ENTITIES.builder(Department.TYPE)
            .with(Department.ID, 10)
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
    assertFalse(employee.loaded(Employee.DEPARTMENT_FK));
  }

  @Test
  void defaultValues() {
    DomainType domainType = domainType("domainType");
    EntityType entityType = domainType.entityType("entityWithDefaultValues");
    Column<Integer> id = entityType.integerColumn("id");
    Column<String> name = entityType.stringColumn("name");
    Column<Integer> value = entityType.integerColumn("value");
    Attribute<Integer> derivedValue = entityType.integerAttribute("derivedValue");

    class TestDomain extends DefaultDomain {
      public TestDomain() {
        super(domainType);
        add(entityType.define(
                id.define()
                        .primaryKey(),
                name.define()
                        .column()
                        .defaultValue("DefName"),
                value.define()
                        .column()
                        .defaultValue(42),
                derivedValue.define()
                        .derived(sourceValues -> {
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
    assertFalse(entity.contains(id));
    assertEquals("DefName", entity.get(name));
    assertEquals(42, entity.get(value));
    assertEquals(43, entity.get(derivedValue));
  }
}
