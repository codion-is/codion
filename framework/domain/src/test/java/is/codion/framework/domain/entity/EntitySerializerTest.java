/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import is.codion.framework.domain.TestDomain;
import is.codion.framework.domain.TestDomain.CompositeMaster;
import is.codion.framework.domain.TestDomain.Employee;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class EntitySerializerTest {

  private static final Entities ENTITIES = new TestDomain().entities();

  @Test
  void testDefaultSerializer() throws IOException, ClassNotFoundException {
    testSerializer(new DefaultEntitySerializer(ENTITIES));
  }

  @Test
  void legacySerializer() throws IOException, ClassNotFoundException {
    testSerializer(new LegacyEntitySerializer(ENTITIES));
  }

  private void testSerializer(EntitySerializer serializer) throws IOException, ClassNotFoundException {
    DefaultEntity entity = createTestEntity();
    DefaultEntity deserializedEntity = (DefaultEntity) ENTITIES.entity(Employee.TYPE);
    serializeDeserialize(serializer, entity, deserializedEntity);
    assertTrue(deserializedEntity.isModified(Employee.NAME));
    assertTrue(Entity.valuesEqual(entity, deserializedEntity));
    assertTrue(Entity.valuesEqual(entity.referencedEntity(Employee.DEPARTMENT_FK), deserializedEntity.referencedEntity(Employee.DEPARTMENT_FK)));
    Entity manager = entity.referencedEntity(Employee.MANAGER_FK);
    Entity deserializedManager = deserializedEntity.referencedEntity(Employee.MANAGER_FK);
    assertTrue(Entity.valuesEqual(manager, deserializedManager));
    assertTrue(Entity.valuesEqual(manager.referencedEntity(Employee.DEPARTMENT_FK), deserializedManager.referencedEntity(Employee.DEPARTMENT_FK)));

    DefaultKey key = createTestKey();
    DefaultKey deserializedKey = (DefaultKey) ENTITIES.keyBuilder(CompositeMaster.TYPE).build();
    serializeDeserialize(serializer, key, deserializedKey);
    assertEquals(key.values, deserializedKey.values);
  }

  private void serializeDeserialize(EntitySerializer serializer, DefaultEntity toSerialize, DefaultEntity toDeserialize)
          throws IOException, ClassNotFoundException {
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    try (ObjectOutputStream out = new ObjectOutputStream(bos)) {
      serializer.serialize(toSerialize, out);
    }
    ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
    try (ObjectInputStream in = new ObjectInputStream(bis)) {
      serializer.deserialize(toDeserialize, in);
    }
  }

  private void serializeDeserialize(EntitySerializer serializer, DefaultKey toSerialize, DefaultKey toDeserialize)
          throws IOException, ClassNotFoundException {
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    try (ObjectOutputStream out = new ObjectOutputStream(bos)) {
      serializer.serialize(toSerialize, out);
    }
    ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
    try (ObjectInputStream in = new ObjectInputStream(bis)) {
      serializer.deserialize(toDeserialize, in);
    }
  }

  private DefaultEntity createTestEntity() {
    Entity department = ENTITIES.builder(TestDomain.Department.TYPE)
            .with(TestDomain.Department.NO, 2)
            .with(TestDomain.Department.NAME, "Dept")
            .build();

    DefaultEntity entity = (DefaultEntity) ENTITIES.builder(Employee.TYPE)
            .with(Employee.ID, 1)
            .with(Employee.NAME, "Björn")
            .with(Employee.HIREDATE, LocalDateTime.now())
            .with(Employee.DEPARTMENT_FK, department)
            .with(Employee.MANAGER_FK, ENTITIES.builder(Employee.TYPE)
                    .with(Employee.ID, 2)
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
}
