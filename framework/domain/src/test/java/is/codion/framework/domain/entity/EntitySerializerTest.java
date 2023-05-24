/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import is.codion.framework.domain.TestDomain;
import is.codion.framework.domain.TestDomain.Employee;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertTrue;

public final class EntitySerializerTest {

  private static final Entities ENTITIES = new TestDomain().entities();

  @Test
  void testDefaultSerializer() throws IOException, ClassNotFoundException {
    testSerializer(new DefaultEntitySerializer(), ENTITIES);
  }

  @Test
  void legacySerializer() throws IOException, ClassNotFoundException {
    testSerializer(new LegacyEntitySerializer(), ENTITIES);
  }

  private void testSerializer(EntitySerializer serializer, Entities entities) throws IOException, ClassNotFoundException {
    DefaultEntity entity = createTestEntity(entities);
    DefaultEntity deserialized = (DefaultEntity) entities.entity(Employee.TYPE);
    serializeDeserialize(serializer, entity, deserialized);
    assertTrue(deserialized.isModified(Employee.NAME));
    assertTrue(Entity.valuesEqual(entity, deserialized));
    assertTrue(Entity.valuesEqual(entity.referencedEntity(Employee.DEPARTMENT_FK), deserialized.referencedEntity(Employee.DEPARTMENT_FK)));
    Entity manager = entity.referencedEntity(Employee.MANAGER_FK);
    Entity deserializedManager = deserialized.referencedEntity(Employee.MANAGER_FK);
    assertTrue(Entity.valuesEqual(manager, deserializedManager));
    assertTrue(Entity.valuesEqual(manager.referencedEntity(Employee.DEPARTMENT_FK), deserializedManager.referencedEntity(Employee.DEPARTMENT_FK)));
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

  private DefaultEntity createTestEntity(Entities entities) {
    Entity department = entities.builder(TestDomain.Department.TYPE)
            .with(TestDomain.Department.NO, 2)
            .with(TestDomain.Department.NAME, "Dept")
            .build();

    DefaultEntity entity = (DefaultEntity) entities.builder(Employee.TYPE)
            .with(Employee.ID, 1)
            .with(Employee.NAME, "Björn")
            .with(Employee.HIREDATE, LocalDateTime.now())
            .with(Employee.DEPARTMENT_FK, department)
            .with(Employee.MANAGER_FK, entities.builder(Employee.TYPE)
                    .with(Employee.ID, 2)
                    .with(Employee.NAME, "Darri")
                    .with(Employee.DEPARTMENT_FK, department)
                    .build())
            .build();
    //make it modified
    entity.put(Employee.NAME, "Björn Darri");

    return entity;
  }
}
