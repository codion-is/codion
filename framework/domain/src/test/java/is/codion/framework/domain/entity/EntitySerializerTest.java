/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import is.codion.framework.domain.DefaultDomain;
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.TestDomain;
import is.codion.framework.domain.TestDomain.CompositeMaster;
import is.codion.framework.domain.TestDomain.Employee;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.time.LocalDateTime;

import static is.codion.framework.domain.entity.EntityDefinition.definition;
import static is.codion.framework.domain.property.Property.columnProperty;
import static is.codion.framework.domain.property.Property.primaryKeyProperty;
import static org.junit.jupiter.api.Assertions.*;

public final class EntitySerializerTest {

  private static final Entities ENTITIES = new TestDomain().entities();

  @org.junit.jupiter.api.Test
  void defaultSerializer() throws IOException, ClassNotFoundException {
    testSerializer(new DefaultEntitySerializer(ENTITIES));
  }

  @org.junit.jupiter.api.Test
  void legacySerializer() throws IOException, ClassNotFoundException {
    testSerializer(new LegacyEntitySerializer(ENTITIES));
  }

  @org.junit.jupiter.api.Test
  void mismatch() throws IOException, ClassNotFoundException {
    SerialDomain domain1 = new SerialDomain(definition(
            primaryKeyProperty(Test.ID),
            columnProperty(Test.NAME))
            .build());
    SerialDomain domain2 = new SerialDomain(definition(
            primaryKeyProperty(Test.ID),
            columnProperty(Test.NAME),
            columnProperty(Test.EXTRA))
            .build());

    EntitySerializer serializer1 = new DefaultEntitySerializer(domain1.entities());
    EntitySerializer serializer2 = new DefaultEntitySerializer(domain2.entities());

    DefaultEntity entity = (DefaultEntity) domain1.entities().builder(Test.TYPE)
            .with(Test.ID, 1)
            .with(Test.NAME, "name")
            .build();
    DefaultEntity deserialized = (DefaultEntity) domain2.entities().entity(Test.TYPE);
    assertFalse(deserialized.contains(Test.EXTRA));

    serializeDeserialize(serializer1, serializer2, entity, deserialized);

    entity = (DefaultEntity) domain2.entities().builder(Test.TYPE)
            .with(Test.ID, 1)
            .with(Test.NAME, "name")
            .with(Test.EXTRA, "extra")
            .build();
    deserialized = (DefaultEntity) domain1.entities().entity(Test.TYPE);

    serializeDeserialize(serializer2, serializer1, entity, deserialized);
    assertFalse(deserialized.contains(Test.EXTRA));

    deserialized = (DefaultEntity) domain2.entities().entity(Test.TYPE);
    serializeDeserialize(serializer2, serializer2, entity, deserialized);
    assertTrue(deserialized.contains(Test.EXTRA));
  }

  private void testSerializer(EntitySerializer serializer) throws IOException, ClassNotFoundException {
    DefaultEntity entity = createTestEntity();
    DefaultEntity deserializedEntity = (DefaultEntity) ENTITIES.entity(Employee.TYPE);
    serializeDeserialize(serializer, serializer, entity, deserializedEntity);
    assertTrue(deserializedEntity.isModified(Employee.NAME));
    assertTrue(Entity.valuesEqual(entity, deserializedEntity));
    assertTrue(Entity.valuesEqual(entity.referencedEntity(Employee.DEPARTMENT_FK), deserializedEntity.referencedEntity(Employee.DEPARTMENT_FK)));
    Entity manager = entity.referencedEntity(Employee.MANAGER_FK);
    Entity deserializedManager = deserializedEntity.referencedEntity(Employee.MANAGER_FK);
    assertTrue(Entity.valuesEqual(manager, deserializedManager));
    assertTrue(Entity.valuesEqual(manager.referencedEntity(Employee.DEPARTMENT_FK), deserializedManager.referencedEntity(Employee.DEPARTMENT_FK)));

    DefaultKey key = createTestKey();
    DefaultKey deserializedKey = (DefaultKey) ENTITIES.keyBuilder(CompositeMaster.TYPE).build();
    serializeDeserialize(serializer, serializer, key, deserializedKey);
    assertEquals(key.values, deserializedKey.values);
  }

  private void serializeDeserialize(EntitySerializer serializer, EntitySerializer deserializer,
                                    DefaultEntity toSerialize, DefaultEntity toDeserialize)
          throws IOException, ClassNotFoundException {
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    try (ObjectOutputStream out = new ObjectOutputStream(bos)) {
      serializer.serialize(toSerialize, out);
    }
    ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
    try (ObjectInputStream in = new ObjectInputStream(bis)) {
      deserializer.deserialize(toDeserialize, in);
    }
  }

  private void serializeDeserialize(EntitySerializer serializer, EntitySerializer deserializer,
                                    DefaultKey toSerialize, DefaultKey toDeserialize)
          throws IOException, ClassNotFoundException {
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    try (ObjectOutputStream out = new ObjectOutputStream(bos)) {
      serializer.serialize(toSerialize, out);
    }
    ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
    try (ObjectInputStream in = new ObjectInputStream(bis)) {
      deserializer.deserialize(toDeserialize, in);
    }
  }

  private static DefaultEntity createTestEntity() {
    return createTestEntity(1);
  }

  private static DefaultEntity createTestEntity(int id) {
    Entity department = ENTITIES.builder(TestDomain.Department.TYPE)
            .with(TestDomain.Department.NO, 2)
            .with(TestDomain.Department.NAME, "Dept")
            .build();

    DefaultEntity entity = (DefaultEntity) ENTITIES.builder(Employee.TYPE)
            .with(Employee.ID, id)
            .with(Employee.NAME, "Björn")
            .with(Employee.HIREDATE, LocalDateTime.now())
            .with(Employee.DEPARTMENT_FK, department)
            .with(Employee.MANAGER_FK, ENTITIES.builder(Employee.TYPE)
                    .with(Employee.ID, -id)
                    .with(Employee.NAME, "Darri")
                    .with(Employee.DEPARTMENT_FK, department)
                    .build())
            .build();
    //make it modified
    entity.put(Employee.NAME, "Björn Darri");

    return entity;
  }

  private static DefaultKey createTestKey() {
    return (DefaultKey) ENTITIES.keyBuilder(CompositeMaster.TYPE)
            .with(CompositeMaster.COMPOSITE_MASTER_ID, 1)
            .with(CompositeMaster.COMPOSITE_MASTER_ID_2, 2)
            .with(CompositeMaster.COMPOSITE_MASTER_ID_3, 3)
            .build();
  }

  private static final DomainType DOMAIN = DomainType.domainType("EntitySerializerTest");

  interface Test {
    EntityType TYPE = DOMAIN.entityType("test");

    Attribute<Integer> ID = TYPE.integerAttribute("id");
    Attribute<String> NAME = TYPE.stringAttribute("name");
    Attribute<String> EXTRA = TYPE.stringAttribute("extra");
  }

  private static final class SerialDomain extends DefaultDomain {

    private SerialDomain(EntityDefinition definition) {
      super(DOMAIN);
      add(definition);
    }
  }
}
