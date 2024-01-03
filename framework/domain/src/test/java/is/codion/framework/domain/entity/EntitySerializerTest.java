/*
 * Copyright (c) 2023 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import is.codion.framework.domain.DefaultDomain;
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.TestDomain;
import is.codion.framework.domain.TestDomain.CompositeMaster;
import is.codion.framework.domain.TestDomain.Employee;
import is.codion.framework.domain.entity.attribute.Column;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public final class EntitySerializerTest {

  private static final Entities ENTITIES = new TestDomain().entities();

  @Test
  void defaultSerializer() throws IOException, ClassNotFoundException {
    testSerializer(new EntitySerializer(ENTITIES, true));
  }

  @Test
  void mismatch() throws IOException, ClassNotFoundException {
    SerialDomain domain1 = new SerialDomain(TestTable.TYPE.define(
            TestTable.ID.define()
                    .primaryKey(),
            TestTable.NAME.define()
                    .column())
            .build());
    SerialDomain domain2 = new SerialDomain(TestTable.TYPE.define(
            TestTable.ID.define()
                    .primaryKey(),
            TestTable.NAME.define()
                    .column(),
            TestTable.EXTRA.define()
                    .column())
            .build());

    EntitySerializer serializer1 = new EntitySerializer(domain1.entities(), true);
    EntitySerializer serializer2 = new EntitySerializer(domain2.entities(), true);
    EntitySerializer serializer3 = new EntitySerializer(domain1.entities(), false);

    DefaultEntity entity = (DefaultEntity) domain1.entities().builder(TestTable.TYPE)
            .with(TestTable.ID, 1)
            .with(TestTable.NAME, "name")
            .build();
    DefaultEntity deserialized = (DefaultEntity) domain2.entities().entity(TestTable.TYPE);
    assertFalse(deserialized.contains(TestTable.EXTRA));

    serializeDeserialize(serializer1, serializer2, entity, deserialized);

    entity = (DefaultEntity) domain2.entities().builder(TestTable.TYPE)
            .with(TestTable.ID, 1)
            .with(TestTable.NAME, "name")
            .with(TestTable.EXTRA, "extra")
            .build();
    deserialized = (DefaultEntity) domain1.entities().entity(TestTable.TYPE);

    DefaultEntity toSerialize = entity;
    DefaultEntity toDeserialize = deserialized;

    assertThrows(IOException.class, () -> serializeDeserialize(serializer2, serializer1, toSerialize, toDeserialize));

    serializeDeserialize(serializer2, serializer3, toSerialize, toDeserialize);
    assertFalse(deserialized.contains(TestTable.EXTRA));

    deserialized = (DefaultEntity) domain2.entities().entity(TestTable.TYPE);
    serializeDeserialize(serializer2, serializer2, entity, deserialized);
    assertTrue(deserialized.contains(TestTable.EXTRA));
  }

  private static void testSerializer(EntitySerializer serializer) throws IOException, ClassNotFoundException {
    DefaultEntity entity = createTestEntity();
    DefaultEntity deserializedEntity = (DefaultEntity) ENTITIES.entity(Employee.TYPE);
    serializeDeserialize(serializer, serializer, entity, deserializedEntity);
    assertTrue(deserializedEntity.modified(Employee.NAME));
    assertTrue(Entity.valuesEqual(entity, deserializedEntity));
    assertTrue(Entity.valuesEqual(entity.referencedEntity(Employee.DEPARTMENT_FK), deserializedEntity.referencedEntity(Employee.DEPARTMENT_FK)));
    assertFalse(deserializedEntity.get(Employee.DEPARTMENT_FK).mutable());
    Entity manager = entity.referencedEntity(Employee.MANAGER_FK);
    Entity deserializedManager = deserializedEntity.referencedEntity(Employee.MANAGER_FK);
    assertTrue(Entity.valuesEqual(manager, deserializedManager));
    assertTrue(Entity.valuesEqual(manager.referencedEntity(Employee.DEPARTMENT_FK), deserializedManager.referencedEntity(Employee.DEPARTMENT_FK)));

    DefaultKey key = createTestKey();
    DefaultKey deserializedKey = (DefaultKey) ENTITIES.keyBuilder(CompositeMaster.TYPE).build();
    serializeDeserialize(serializer, serializer, key, deserializedKey);
    assertEquals(key.values, deserializedKey.values);
  }

  private static void serializeDeserialize(EntitySerializer serializer, EntitySerializer deserializer,
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

  private static void serializeDeserialize(EntitySerializer serializer, EntitySerializer deserializer,
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
            .with(TestDomain.Department.ID, 2)
            .with(TestDomain.Department.NAME, "Dept")
            .build()
            .immutable();

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

  interface TestTable {
    EntityType TYPE = DOMAIN.entityType("test");

    Column<Integer> ID = TYPE.integerColumn("id");
    Column<String> NAME = TYPE.stringColumn("name");
    Column<String> EXTRA = TYPE.stringColumn("extra");
  }

  private static final class SerialDomain extends DefaultDomain {

    private SerialDomain(EntityDefinition definition) {
      super(DOMAIN);
      add(definition);
    }
  }
}
