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
 * Copyright (c) 2023 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.framework.domain.entity;

import is.codion.framework.domain.TestDomain;
import is.codion.framework.domain.TestDomain.KeyTest;
import is.codion.framework.domain.TestDomainExtended;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.AttributeDefinition;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.domain.entity.exception.ItemValidationException;
import is.codion.framework.domain.entity.exception.LengthValidationException;
import is.codion.framework.domain.entity.exception.NullValidationException;
import is.codion.framework.domain.entity.exception.RangeValidationException;
import is.codion.framework.domain.entity.exception.ValidationException;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.*;

public final class EntitiesTest {

  private final Entities entities = new TestDomain().entities();

  @Test
  void defineTypes() {
    EntityDefinition definition = entities.definition(TestDomain.Detail.TYPE);

    //assert types
    assertEquals(definition.columns().definition(TestDomain.Detail.ID).attribute().type().valueClass(), Long.class);
    assertEquals(definition.columns().definition(TestDomain.Detail.SHORT).attribute().type().valueClass(), Short.class);
    assertEquals(definition.columns().definition(TestDomain.Detail.INT).attribute().type().valueClass(), Integer.class);
    assertEquals(definition.columns().definition(TestDomain.Detail.DOUBLE).attribute().type().valueClass(), Double.class);
    assertEquals(definition.columns().definition(TestDomain.Detail.STRING).attribute().type().valueClass(), String.class);
    assertEquals(definition.columns().definition(TestDomain.Detail.DATE).attribute().type().valueClass(), LocalDate.class);
    assertEquals(definition.columns().definition(TestDomain.Detail.TIMESTAMP).attribute().type().valueClass(), LocalDateTime.class);
    assertEquals(definition.columns().definition(TestDomain.Detail.BOOLEAN).attribute().type().valueClass(), Boolean.class);
    assertEquals(definition.foreignKeys().definition(TestDomain.Detail.MASTER_FK).attribute().type().valueClass(), Entity.class);
    assertEquals(definition.columns().definition(TestDomain.Detail.MASTER_ID).attribute().type().valueClass(), Long.class);
    assertEquals(definition.attributes().definition(TestDomain.Detail.MASTER_NAME).attribute().type().valueClass(), String.class);
    assertEquals(definition.attributes().definition(TestDomain.Detail.MASTER_CODE).attribute().type().valueClass(), Integer.class);

    //assert column names
    assertEquals(definition.columns().definition(TestDomain.Detail.ID).attribute(), TestDomain.Detail.ID);
    assertEquals(definition.columns().definition(TestDomain.Detail.SHORT).attribute(), TestDomain.Detail.SHORT);
    assertEquals(definition.columns().definition(TestDomain.Detail.INT).attribute(), TestDomain.Detail.INT);
    assertEquals(definition.columns().definition(TestDomain.Detail.DOUBLE).attribute(), TestDomain.Detail.DOUBLE);
    assertEquals(definition.columns().definition(TestDomain.Detail.STRING).attribute(), TestDomain.Detail.STRING);
    assertEquals(definition.columns().definition(TestDomain.Detail.DATE).attribute(), TestDomain.Detail.DATE);
    assertEquals(definition.columns().definition(TestDomain.Detail.TIMESTAMP).attribute(), TestDomain.Detail.TIMESTAMP);
    assertEquals(definition.columns().definition(TestDomain.Detail.BOOLEAN).attribute(), TestDomain.Detail.BOOLEAN);
    assertEquals(definition.columns().definition(TestDomain.Detail.MASTER_ID).attribute(), TestDomain.Detail.MASTER_ID);
    assertEquals(definition.attributes().definition(TestDomain.Detail.MASTER_NAME).attribute(), TestDomain.Detail.MASTER_NAME);
    assertEquals(definition.attributes().definition(TestDomain.Detail.MASTER_CODE).attribute(), TestDomain.Detail.MASTER_CODE);

    //assert captions
    assertNotNull(definition.columns().definition(TestDomain.Detail.ID).caption());
    assertEquals(definition.columns().definition(TestDomain.Detail.SHORT).caption(), TestDomain.Detail.SHORT.name());
    assertEquals(definition.columns().definition(TestDomain.Detail.INT).caption(), TestDomain.Detail.INT.name());
    assertEquals(definition.columns().definition(TestDomain.Detail.DOUBLE).caption(), TestDomain.Detail.DOUBLE.name());
    assertEquals(definition.columns().definition(TestDomain.Detail.STRING).caption(), "Detail string");
    assertEquals(definition.columns().definition(TestDomain.Detail.DATE).caption(), TestDomain.Detail.DATE.name());
    assertEquals(definition.columns().definition(TestDomain.Detail.TIMESTAMP).caption(), TestDomain.Detail.TIMESTAMP.name());
    assertEquals(definition.columns().definition(TestDomain.Detail.BOOLEAN).caption(), TestDomain.Detail.BOOLEAN.name());
    assertEquals(definition.foreignKeys().definition(TestDomain.Detail.MASTER_FK).caption(), TestDomain.Detail.MASTER_FK.name());
    assertEquals(definition.attributes().definition(TestDomain.Detail.MASTER_NAME).caption(), TestDomain.Detail.MASTER_NAME.name());
    assertEquals(definition.attributes().definition(TestDomain.Detail.MASTER_CODE).caption(), TestDomain.Detail.MASTER_CODE.name());

    //assert hidden status
    assertTrue(definition.columns().definition(TestDomain.Detail.ID).hidden());
    assertFalse(definition.columns().definition(TestDomain.Detail.SHORT).hidden());
    assertFalse(definition.columns().definition(TestDomain.Detail.INT).hidden());
    assertFalse(definition.columns().definition(TestDomain.Detail.DOUBLE).hidden());
    assertFalse(definition.columns().definition(TestDomain.Detail.STRING).hidden());
    assertFalse(definition.columns().definition(TestDomain.Detail.DATE).hidden());
    assertFalse(definition.columns().definition(TestDomain.Detail.TIMESTAMP).hidden());
    assertFalse(definition.columns().definition(TestDomain.Detail.BOOLEAN).hidden());
    assertFalse(definition.foreignKeys().definition(TestDomain.Detail.MASTER_FK).hidden());
    assertFalse(definition.attributes().definition(TestDomain.Detail.MASTER_NAME).hidden());
    assertFalse(definition.attributes().definition(TestDomain.Detail.MASTER_CODE).hidden());
  }

  @Test
  void attributeWrongEntityType() {
    EntityDefinition definition = entities.definition(TestDomain.Detail.TYPE);
    assertThrows(IllegalArgumentException.class, () -> definition.columns().definition(TestDomain.Master.CODE));
  }

  @Test
  void sortAttributes() {
    EntityDefinition definition = entities.definition(TestDomain.Employee.TYPE);
    List<AttributeDefinition<?>> attributes = Stream.of(TestDomain.Employee.HIREDATE, TestDomain.Employee.COMMISSION,
                    TestDomain.Employee.SALARY, TestDomain.Employee.JOB)
            .map(definition.columns()::definition)
            .sorted(AttributeDefinition.definitionComparator())
            .collect(toList());
    assertEquals(TestDomain.Employee.COMMISSION, attributes.get(0).attribute());
    assertEquals(TestDomain.Employee.HIREDATE, attributes.get(1).attribute());
    assertEquals(TestDomain.Employee.JOB, attributes.get(2).attribute());
    assertEquals(TestDomain.Employee.SALARY, attributes.get(3).attribute());
  }

  @Test
  void updatableAttributes() {
    EntityDefinition definition = entities.definition(TestDomain.Detail.TYPE);
    Collection<AttributeDefinition<?>> attributes = definition.attributes().updatable();
    assertEquals(11, attributes.size());
    assertFalse(attributes.contains(definition.attributes().definition(TestDomain.Detail.MASTER_NAME)));
    assertFalse(attributes.contains(definition.attributes().definition(TestDomain.Detail.MASTER_CODE)));
    assertFalse(attributes.contains(definition.attributes().definition(TestDomain.Detail.INT_DERIVED)));
  }

  @Test
  void selectedAttributes() {
    List<Attribute<?>> attributes = new ArrayList<>();
    attributes.add(TestDomain.Department.ID);
    attributes.add(TestDomain.Department.NAME);

    EntityDefinition definition = entities.definition(TestDomain.Department.TYPE);
    Collection<AttributeDefinition<?>> definitions = attributes.stream()
            .map(definition.attributes()::definition)
            .collect(toList());
    assertEquals(2, definitions.size());
    assertTrue(definitions.contains(definition.columns().definition(TestDomain.Department.ID)));
    assertTrue(definitions.contains(definition.columns().definition(TestDomain.Department.NAME)));
  }

  @Test
  void key() {
    Entity.Key key = entities.keyBuilder(KeyTest.TYPE).build();
    assertEquals(0, key.hashCode());
    assertTrue(key.columns().isEmpty());
    assertTrue(key.isNull());

    assertThrows(IllegalStateException.class, () -> entities.primaryKey(KeyTest.TYPE, 1));
    assertThrows(NoSuchElementException.class, key::get);
    assertThrows(NoSuchElementException.class, key::optional);
    assertThrows(NoSuchElementException.class, key::column);

    key = key.copyBuilder()
            .with(KeyTest.ID1, 1)
            .with(KeyTest.ID2, 2)
            .with(KeyTest.ID3, 3)
            .build();
    assertTrue(key.isNotNull());
    assertEquals(6, key.hashCode());
    assertTrue(key.optional(KeyTest.ID1).isPresent());

    key = key.copyBuilder()
            .with(KeyTest.ID2, 3)
            .build();
    assertEquals(7, key.hashCode());

    key = key.copyBuilder()
            .with(KeyTest.ID3, null)
            .build();
    assertTrue(key.isNotNull());
    assertEquals(4, key.hashCode());
    key = key.copyBuilder()
            .with(KeyTest.ID2, null)
            .build();
    assertTrue(key.isNull());
    assertFalse(key.optional(KeyTest.ID2).isPresent());
    assertEquals(0, key.hashCode());
    key = key.copyBuilder()
            .with(KeyTest.ID2, 4)
            .build();
    assertTrue(key.optional(KeyTest.ID2).isPresent());
    assertTrue(key.isNotNull());
    assertEquals(5, key.hashCode());

    key = key.copyBuilder()
            .with(KeyTest.ID2, 42)
            .build();
    assertTrue(key.isNotNull());
    assertEquals(43, key.hashCode());

    assertThrows(NullPointerException.class, () -> entities.keyBuilder(null));

    assertFalse(entities.keyBuilder(TestDomain.NoPk.TYPE)
            .with(TestDomain.NoPk.COL1, 1)
            .build()
            .primaryKey());
    Entity.Key noPk = entities.keyBuilder(TestDomain.NoPk.TYPE).build();
    assertThrows(IllegalArgumentException.class, () -> noPk.get(TestDomain.NoPk.COL1));
  }

  @Test
  void keys() {
    List<Entity.Key> intKeys = entities.primaryKeys(TestDomain.Employee.TYPE, 1, 2, 3, 4);
    assertEquals(4, intKeys.size());
    assertEquals(Integer.valueOf(3), intKeys.get(2).get());
    List<Entity.Key> longKeys = entities.primaryKeys(TestDomain.Detail.TYPE, 1L, 2L, 3L, 4L);
    assertEquals(4, longKeys.size());
    assertEquals(Long.valueOf(3), longKeys.get(2).get());
  }

  @Test
  void entity() {
    Entity.Key key = entities.primaryKey(TestDomain.Master.TYPE, 10L);

    Entity master = Entity.entity(key);
    assertEquals(TestDomain.Master.TYPE, master.entityType());
    assertTrue(master.contains(TestDomain.Master.ID));
    assertEquals(10L, master.get(TestDomain.Master.ID));

    assertThrows(NullPointerException.class, () -> entities.entity(null));
  }

  @Test
  void attributes() {
    EntityDefinition definition = entities.definition(TestDomain.Department.TYPE);
    AttributeDefinition<Integer> id = definition.columns().definition(TestDomain.Department.ID);
    AttributeDefinition<String> location = definition.columns().definition(TestDomain.Department.LOCATION);
    AttributeDefinition<String> name = definition.columns().definition(TestDomain.Department.NAME);
    AttributeDefinition<Boolean> active = definition.columns().definition(TestDomain.Department.ACTIVE);
    Collection<AttributeDefinition<?>> attributes = Stream.of(TestDomain.Department.LOCATION, TestDomain.Department.NAME)
            .map(definition.columns()::definition)
            .collect(toList());
    assertEquals(2, attributes.size());
    assertFalse(attributes.contains(id));
    assertTrue(attributes.contains(location));
    assertTrue(attributes.contains(name));

    attributes = definition.attributes().definitions().stream()
            .filter(ad -> !ad.hidden())
            .collect(toList());
    assertTrue(attributes.contains(id));
    assertTrue(attributes.contains(location));
    assertTrue(attributes.contains(name));
    assertFalse(attributes.contains(active));

    List<AttributeDefinition<?>> allAttributes = definition.attributes().definitions();
    assertTrue(allAttributes.contains(id));
    assertTrue(allAttributes.contains(location));
    assertTrue(allAttributes.contains(name));
    assertTrue(allAttributes.contains(active));
  }

  @Test
  void definitionInvalid() {
    assertThrows(IllegalArgumentException.class, () -> entities.definition(TestDomain.Master.TYPE)
            .attributes().definition(TestDomain.Master.TYPE.attribute("unknown attribute", Integer.class)));
  }

  @Test
  void foreignKeys() {
    EntityDefinition definition = entities.definition(TestDomain.Detail.TYPE);
    Collection<ForeignKey> foreignKeys = definition.foreignKeys().get(TestDomain.Employee.TYPE);
    assertEquals(0, foreignKeys.size());
    foreignKeys = definition.foreignKeys().get(TestDomain.Master.TYPE);
    assertEquals(2, foreignKeys.size());
    assertTrue(foreignKeys.contains(TestDomain.Detail.MASTER_FK));
  }

  @Test
  void foreignKeyAttribute() {
    assertNotNull(entities.definition(TestDomain.Detail.TYPE).foreignKeys().definition(TestDomain.Detail.MASTER_FK));
  }

  @Test
  void foreignKeyAttributeInvalid() {
    ForeignKey foreignKey = TestDomain.Detail.TYPE.foreignKey("bla bla", TestDomain.Detail.MASTER_ID, TestDomain.Master.ID);
    assertThrows(IllegalArgumentException.class, () -> entities.definition(TestDomain.Detail.TYPE).foreignKeys().definition(foreignKey));
  }

  @Test
  void hasDerivedAttributes() {
    EntityDefinition definition = entities.definition(TestDomain.Detail.TYPE);
    assertTrue(definition.attributes().derivedFrom(TestDomain.Detail.BOOLEAN).isEmpty());
    assertFalse(definition.attributes().derivedFrom(TestDomain.Detail.INT).isEmpty());
  }

  @Test
  void derivedAttributes() {
    EntityDefinition definition = entities.definition(TestDomain.Detail.TYPE);
    Collection<Attribute<?>> derivedAttributes = definition.attributes().derivedFrom(TestDomain.Detail.BOOLEAN);
    assertTrue(derivedAttributes.isEmpty());
    derivedAttributes = definition.attributes().derivedFrom(TestDomain.Detail.INT);
    assertEquals(1, derivedAttributes.size());
    assertTrue(derivedAttributes.contains(TestDomain.Detail.INT_DERIVED));
  }

  @Test
  void isSmallDataset() {
    assertTrue(entities.definition(TestDomain.Detail.TYPE).smallDataset());
  }

  @Test
  void stringFactory() {
    assertNotNull(entities.definition(TestDomain.Department.TYPE).stringFactory());
  }

  @Test
  void nullValidation() {
    Entity emp = entities.builder(TestDomain.Employee.TYPE)
            .with(TestDomain.Employee.NAME, "Name")
            .with(TestDomain.Employee.HIREDATE, LocalDateTime.now())
            .with(TestDomain.Employee.SALARY, 1200.0)
            .build();

    DefaultEntityValidator validator = new DefaultEntityValidator();
    try {
      validator.validate(emp);
      fail();
    }
    catch (ValidationException e) {
      assertTrue(e instanceof NullValidationException);
      assertEquals(TestDomain.Employee.DEPARTMENT_FK, e.attribute());
    }
    emp.put(TestDomain.Employee.DEPARTMENT_NO, 1);
    try {
      validator.validate(emp);
    }
    catch (ValidationException e) {
      fail();
    }
    emp.put(TestDomain.Employee.SALARY, null);
    try {
      validator.validate(emp);
      fail();
    }
    catch (ValidationException e) {
      assertTrue(e instanceof NullValidationException);
      assertEquals(TestDomain.Employee.SALARY, e.attribute());
    }
  }

  @Test
  void maxLengthValidation() {
    Entity emp = entities.builder(TestDomain.Employee.TYPE)
            .with(TestDomain.Employee.DEPARTMENT_NO, 1)
            .with(TestDomain.Employee.NAME, "Name")
            .with(TestDomain.Employee.HIREDATE, LocalDateTime.now())
            .with(TestDomain.Employee.SALARY, 1200.0)
            .build();
    DefaultEntityValidator validator = new DefaultEntityValidator();
    assertDoesNotThrow(() -> validator.validate(emp));
    emp.put(TestDomain.Employee.NAME, "LooooongName");
    assertThrows(LengthValidationException.class, () -> validator.validate(emp));
  }

  @Test
  void rangeValidation() {
    Entity emp = entities.builder(TestDomain.Employee.TYPE)
            .with(TestDomain.Employee.DEPARTMENT_NO, 1)
            .with(TestDomain.Employee.NAME, "Name")
            .with(TestDomain.Employee.HIREDATE, LocalDateTime.now())
            .with(TestDomain.Employee.SALARY, 1200d)
            .with(TestDomain.Employee.COMMISSION, 300d)
            .build();
    DefaultEntityValidator validator = new DefaultEntityValidator();
    assertDoesNotThrow(() -> validator.validate(emp));
    emp.put(TestDomain.Employee.COMMISSION, 10d);
    assertThrows(RangeValidationException.class, () -> validator.validate(emp));
    emp.put(TestDomain.Employee.COMMISSION, 2100d);
    assertThrows(RangeValidationException.class, () -> validator.validate(emp));
  }

  @Test
  void itemValidation() {
    Map<Attribute<?>, Object> values = new HashMap<>();
    values.put(TestDomain.Employee.NAME, "Name");
    values.put(TestDomain.Employee.DEPARTMENT_NO, 1);
    values.put(TestDomain.Employee.JOB, "CLREK");
    Entity emp = entities.definition(TestDomain.Employee.TYPE).entity(values);
    DefaultEntityValidator validator = new DefaultEntityValidator();
    assertThrows(ItemValidationException.class, () -> validator.validate(emp));
  }

  @Test
  void strictValidation() throws ValidationException {
    Entity emp = entities.builder(TestDomain.Employee.TYPE)
            .with(TestDomain.Employee.NAME, "1234567891000")
            .with(TestDomain.Employee.DEPARTMENT_NO, 1)
            .with(TestDomain.Employee.JOB, "CLERK")
            .with(TestDomain.Employee.SALARY, 1200d)
            .with(TestDomain.Employee.HIREDATE, LocalDateTime.now())
            .build();
    DefaultEntityValidator validator = new DefaultEntityValidator();
    assertThrows(LengthValidationException.class, () -> validator.validate(emp));
    emp.put(TestDomain.Employee.NAME, "Name");
    emp.save();

    emp.put(TestDomain.Employee.ID, 10);//now it "exists"
    emp.put(TestDomain.Employee.NAME, "1234567891000");
    assertThrows(LengthValidationException.class, () -> validator.validate(emp));
    emp.save();//but not modified
    validator.validate(emp);

    DefaultEntityValidator validator2 = new DefaultEntityValidator(true);

    assertThrows(LengthValidationException.class, () -> validator2.validate(emp));
    emp.put(TestDomain.Employee.NAME, "Name");
    emp.save();

    emp.put(TestDomain.Employee.ID, 10);//now it "exists"
    emp.put(TestDomain.Employee.NAME, "1234567891000");
    assertThrows(LengthValidationException.class, () -> validator2.validate(emp));
    emp.save();//but not modified
    assertThrows(LengthValidationException.class, () -> validator2.validate(emp));//strict
  }

  @Test
  void searchable() {
    EntityDefinition definition = entities.definition(TestDomain.Employee.TYPE);
    Collection<Column<String>> searchable = definition.columns().searchable();
    assertTrue(searchable.contains(TestDomain.Employee.JOB));
    assertTrue(searchable.contains(TestDomain.Employee.NAME));

    searchable = entities.definition(TestDomain.Department.TYPE).columns().searchable();
    //should contain all string based columns
    assertTrue(searchable.contains(TestDomain.Department.NAME));
  }

  @Test
  void validateTypeEntity() {
    Entity entity = entities.entity(TestDomain.Detail.TYPE);
    Entity entity1 = entities.entity(TestDomain.Detail.TYPE);
    assertThrows(IllegalArgumentException.class, () -> entity.put(TestDomain.Detail.MASTER_FK, entity1));
  }

  @Test
  void setValueDerived() {
    Entity entity = entities.entity(TestDomain.Detail.TYPE);
    assertThrows(IllegalArgumentException.class, () -> entity.put(TestDomain.Detail.INT_DERIVED, 10));
  }

  @Test
  void setValueItem() {
    Entity entity = entities.entity(TestDomain.Detail.TYPE);
    assertThrows(IllegalArgumentException.class, () -> entity.put(TestDomain.Detail.INT_VALUE_LIST, -10));
  }

  @Test
  void copyEntities() {
    Entity dept1 = entities.builder(TestDomain.Department.TYPE)
            .with(TestDomain.Department.ID, 1)
            .with(TestDomain.Department.LOCATION, "location")
            .with(TestDomain.Department.NAME, "name")
            .build();
    Entity dept2 = entities.builder(TestDomain.Department.TYPE)
            .with(TestDomain.Department.ID, 2)
            .with(TestDomain.Department.LOCATION, "location2")
            .with(TestDomain.Department.NAME, "name2")
            .build();

    Iterator<Entity> copies = Stream.of(dept1, dept2)
            .map(Entity::deepCopy)
            .collect(toList())
            .iterator();
    Entity dept1Copy = copies.next();
    Entity dept2Copy = copies.next();
    assertNotSame(dept1Copy, dept1);
    assertTrue(dept1Copy.valuesEqual(dept1));
    assertNotSame(dept2Copy, dept2);
    assertTrue(dept2Copy.valuesEqual(dept2));

    Entity emp1 = entities.builder(TestDomain.Employee.TYPE)
            .with(TestDomain.Employee.DEPARTMENT_FK, dept1)
            .with(TestDomain.Employee.NAME, "name")
            .with(TestDomain.Employee.COMMISSION, 130.5)
            .build();

    Entity copy = emp1.copy();
    assertTrue(emp1.valuesEqual(copy));
    assertSame(emp1.get(TestDomain.Employee.DEPARTMENT_FK), copy.get(TestDomain.Employee.DEPARTMENT_FK));
    assertFalse(emp1.modified());

    copy = emp1.deepCopy();
    assertTrue(emp1.valuesEqual(copy));
    assertNotSame(emp1.get(TestDomain.Employee.DEPARTMENT_FK), copy.get(TestDomain.Employee.DEPARTMENT_FK));
    assertFalse(emp1.modified());
  }

  @Test
  void extendedDomain() {
    TestDomainExtended extended = new TestDomainExtended();
    Entities entities = extended.entities();

    entities.entity(TestDomainExtended.T_EXTENDED);

    entities.entity(TestDomain.CompositeMaster.TYPE);

    TestDomainExtended.TestDomainSecondExtension second = new TestDomainExtended.TestDomainSecondExtension();
    entities = second.entities();

    entities.entity(TestDomainExtended.TestDomainSecondExtension.T_SECOND_EXTENDED);

    entities.entity(TestDomainExtended.T_EXTENDED);

    entities.entity(TestDomain.CompositeMaster.TYPE);

    assertNotNull(second.procedure(TestDomainExtended.PROC_TYPE));
    assertNotNull(second.function(TestDomainExtended.FUNC_TYPE));
    assertNotNull(second.report(TestDomainExtended.REP_TYPE));

    //entity type name clash
    assertThrows(IllegalArgumentException.class, TestDomainExtended.TestDomainThirdExtension::new);
  }
}
