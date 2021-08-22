/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.plugin.jackson.json.domain;

import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.Key;
import is.codion.plugin.jackson.json.TestDomain;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;

public final class EntityObjectMapperTest {

  private final Entities entities = new TestDomain().getEntities();

  @Test
  void entity() throws JsonProcessingException {
    final EntityObjectMapper mapper = new EntityObjectMapper(entities);

    final Entity dept = entities.entity(TestDomain.T_DEPARTMENT);
    dept.put(TestDomain.DEPARTMENT_ID, 1);
    dept.put(TestDomain.DEPARTMENT_NAME, "Name");
    dept.put(TestDomain.DEPARTMENT_LOCATION, "Location");
    dept.put(TestDomain.DEPARTMENT_LOCATION, "New Location");
    final byte[] logoBytes = new byte[20];
    new Random().nextBytes(logoBytes);
    dept.put(TestDomain.DEPARTMENT_LOGO, logoBytes);

    String jsonString = mapper.writeValueAsString(dept);
    final Entity readDept = mapper.readValue(jsonString, Entity.class);
    assertTrue(dept.columnValuesEqual(readDept));

    final Entity entity = entities.entity(TestDomain.T_ENTITY);
    entity.put(TestDomain.ENTITY_DECIMAL, BigDecimal.valueOf(1234L));
    entity.put(TestDomain.ENTITY_DATE_TIME, LocalDateTime.now());
    entity.put(TestDomain.ENTITY_OFFSET_DATE_TIME, OffsetDateTime.now());
    entity.put(TestDomain.ENTITY_BLOB, logoBytes);
    entity.put(TestDomain.ENTITY_READ_ONLY, "readOnly");
    entity.put(TestDomain.ENTITY_BOOLEAN, true);
    entity.put(TestDomain.ENTITY_TIME, LocalTime.now());

    jsonString = mapper.writeValueAsString(entity);

    assertTrue(entity.columnValuesEqual(mapper.readValue(jsonString, Entity.class)));

    entity.put(TestDomain.ENTITY_BOOLEAN, false);
    jsonString = mapper.writeValueAsString(entity);
    final Entity entityModified = mapper.readValue(jsonString, Entity.class);
    assertTrue(entityModified.isModified());
    assertTrue(entityModified.isModified(TestDomain.ENTITY_BOOLEAN));
  }

  @Test
  void entityForeignKeys() throws JsonProcessingException {
    final EntityObjectMapper mapper = new EntityObjectMapper(entities).setIncludeForeignKeyValues(true);

    final Entity dept = entities.entity(TestDomain.T_DEPARTMENT);
    dept.put(TestDomain.DEPARTMENT_ID, 1);
    dept.put(TestDomain.DEPARTMENT_NAME, "Name");
    dept.put(TestDomain.DEPARTMENT_LOCATION, "Location");
    dept.put(TestDomain.DEPARTMENT_LOCATION, "New Location");

    final Entity emp = entities.entity(TestDomain.T_EMP);
    emp.put(TestDomain.EMP_ID, 2);
    emp.put(TestDomain.EMP_NAME, "Emp");
    emp.put(TestDomain.EMP_COMMISSION, 134.34);
    emp.put(TestDomain.EMP_DEPARTMENT_FK, dept);
    emp.put(TestDomain.EMP_HIREDATE, LocalDate.now());

    final String jsonString = mapper.writeValueAsString(emp);
    final Entity readEmp = mapper.readValue(jsonString, Entity.class);
    assertTrue(emp.columnValuesEqual(readEmp));

    final Entity readDept = readEmp.getForeignKey(TestDomain.EMP_DEPARTMENT_FK);
    assertTrue(dept.columnValuesEqual(readDept));
  }

  @Test
  void key() throws JsonProcessingException {
    final EntityObjectMapper mapper = new EntityObjectMapper(entities);

    final Key deptKey1 = entities.primaryKey(TestDomain.T_DEPARTMENT, 1);
    final Key deptKey2 = entities.primaryKey(TestDomain.T_DEPARTMENT, 2);

    String jsonString = mapper.serializeKeys(asList(deptKey1, deptKey2));

    final List<Key> keys = mapper.deserializeKeys(jsonString);
    assertEquals(TestDomain.T_DEPARTMENT, keys.get(0).getEntityType());
    assertEquals(Integer.valueOf(1), keys.get(0).get());
    assertEquals(Integer.valueOf(2), keys.get(1).get());

    final Key entityKey = entities.keyBuilder(TestDomain.T_ENTITY)
            .with(TestDomain.ENTITY_DECIMAL, BigDecimal.valueOf(1234L))
            .with(TestDomain.ENTITY_DATE_TIME, LocalDateTime.now())
            .build();

    jsonString = mapper.writeValueAsString(entityKey);

    final Key readKey = mapper.readValue(jsonString, Key.class);

    assertEquals(entityKey, readKey);
  }

  @Test
  void keyOld() throws Exception {
    final EntityObjectMapper mapper = new EntityObjectMapper(entities);

    final Key key = entities.primaryKey(TestDomain.T_DEPARTMENT, 42);

    final String keyJSON = mapper.writeValueAsString(singletonList(key));
    assertEquals("[{\"entityType\":\"scott.dept\",\"values\":{\"deptno\":42}}]", keyJSON);
    final Key keyParsed = mapper.deserializeKeys(keyJSON).get(0);
    assertEquals(key.getEntityType(), keyParsed.getEntityType());
    assertEquals(key.getAttribute(), keyParsed.getAttribute());
    assertEquals((Integer) key.get(), keyParsed.get());
  }

  @Test
  void entityOld() throws Exception {
    final EntityObjectMapper mapper = new EntityObjectMapper(entities);

    final DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    final LocalDate hiredate = LocalDate.parse("2001-12-20", format);

    final Entity dept10 = entities.entity(TestDomain.T_DEPARTMENT);
    dept10.put(TestDomain.DEPARTMENT_ID, -10);
    dept10.put(TestDomain.DEPARTMENT_NAME, "DEPTNAME");
    dept10.put(TestDomain.DEPARTMENT_LOCATION, "LOCATION");

    String jsonString = mapper.writeValueAsString(singletonList(dept10));
    assertTrue(dept10.columnValuesEqual(mapper.deserializeEntities(jsonString).get(0)));

    final Entity dept20 = entities.entity(TestDomain.T_DEPARTMENT);
    dept20.put(TestDomain.DEPARTMENT_ID, -20);
    dept20.put(TestDomain.DEPARTMENT_NAME, null);
    dept20.put(TestDomain.DEPARTMENT_LOCATION, "ALOC");

    jsonString = mapper.writeValueAsString(singletonList(dept20));
    assertTrue(dept20.columnValuesEqual(mapper.deserializeEntities(jsonString).get(0)));

    final String twoDepts = mapper.serializeEntities(asList(dept10, dept20));
    mapper.deserializeEntities(twoDepts);

    final Entity mgr30 = entities.entity(TestDomain.T_EMP);
    mgr30.put(TestDomain.EMP_COMMISSION, 500.5);
    mgr30.put(TestDomain.EMP_DEPARTMENT_FK, dept20);
    mgr30.put(TestDomain.EMP_HIREDATE, hiredate);
    mgr30.put(TestDomain.EMP_ID, -30);
    mgr30.put(TestDomain.EMP_JOB, "MANAGER");
    mgr30.put(TestDomain.EMP_NAME, "MGR NAME");
    mgr30.put(TestDomain.EMP_SALARY, BigDecimal.valueOf(2500.5));

    final Entity mgr50 = entities.entity(TestDomain.T_EMP);
    mgr50.put(TestDomain.EMP_COMMISSION, 500.5);
    mgr50.put(TestDomain.EMP_DEPARTMENT_FK, dept20);
    mgr50.put(TestDomain.EMP_HIREDATE, hiredate);
    mgr50.put(TestDomain.EMP_ID, -50);
    mgr50.put(TestDomain.EMP_JOB, "MANAGER");
    mgr50.put(TestDomain.EMP_NAME, "MGR2 NAME");
    mgr50.put(TestDomain.EMP_SALARY, BigDecimal.valueOf(2500.5));

    final Entity emp1 = entities.entity(TestDomain.T_EMP);
    emp1.put(TestDomain.EMP_COMMISSION, 500.5);
    emp1.put(TestDomain.EMP_DEPARTMENT_FK, dept10);
    emp1.put(TestDomain.EMP_HIREDATE, hiredate);
    emp1.put(TestDomain.EMP_ID, -500);
    emp1.put(TestDomain.EMP_JOB, "CLERK");
    emp1.put(TestDomain.EMP_MGR_FK, mgr30);
    emp1.put(TestDomain.EMP_NAME, "A NAME");
    emp1.put(TestDomain.EMP_SALARY, BigDecimal.valueOf(2500.55));

    jsonString = mapper.writeValueAsString(singletonList(emp1));
    assertTrue(emp1.columnValuesEqual(mapper.deserializeEntities(jsonString).get(0)));

    mapper.setIncludeForeignKeyValues(true);

    jsonString = mapper.writeValueAsString(singletonList(emp1));
    Entity emp1Deserialized = mapper.deserializeEntities(jsonString).get(0);
    assertTrue(emp1.columnValuesEqual(emp1Deserialized));
    assertTrue(emp1.getForeignKey(TestDomain.EMP_DEPARTMENT_FK).columnValuesEqual(emp1Deserialized.getForeignKey(TestDomain.EMP_DEPARTMENT_FK)));
    assertTrue(emp1.getForeignKey(TestDomain.EMP_MGR_FK).columnValuesEqual(emp1Deserialized.getForeignKey(TestDomain.EMP_MGR_FK)));

    final LocalDate newHiredate = LocalDate.parse("2002-11-21", format);
    emp1.put(TestDomain.EMP_COMMISSION, 550.55);
    emp1.put(TestDomain.EMP_DEPARTMENT_FK, dept20);
    emp1.put(TestDomain.EMP_JOB, "ANALYST");
    emp1.put(TestDomain.EMP_MGR_FK, mgr50);
    emp1.put(TestDomain.EMP_NAME, "ANOTHER NAME");
    emp1.put(TestDomain.EMP_SALARY, BigDecimal.valueOf(3500.5));
    emp1.put(TestDomain.EMP_HIREDATE, newHiredate);

    jsonString = mapper.writeValueAsString(singletonList(emp1));
    emp1Deserialized = mapper.deserializeEntities(jsonString).get(0);
    assertTrue(emp1.columnValuesEqual(emp1Deserialized));

    assertEquals(500.5, emp1Deserialized.getOriginal(TestDomain.EMP_COMMISSION));
    assertEquals(dept10, emp1Deserialized.getOriginal(TestDomain.EMP_DEPARTMENT_FK));
    assertEquals("CLERK", emp1Deserialized.getOriginal(TestDomain.EMP_JOB));
    assertEquals(mgr30, emp1Deserialized.getOriginal(TestDomain.EMP_MGR_FK));
    assertEquals(hiredate, emp1Deserialized.getOriginal(TestDomain.EMP_HIREDATE));
    assertEquals("A NAME", emp1Deserialized.getOriginal(TestDomain.EMP_NAME));
    assertEquals(BigDecimal.valueOf(2500.55), emp1Deserialized.getOriginal(TestDomain.EMP_SALARY));

    assertTrue(emp1Deserialized.getOriginal(TestDomain.EMP_DEPARTMENT_FK).columnValuesEqual(dept10));
    assertTrue(emp1Deserialized.getOriginal(TestDomain.EMP_MGR_FK).columnValuesEqual(mgr30));

    final Entity emp2 = entities.entity(TestDomain.T_EMP);
    emp2.put(TestDomain.EMP_COMMISSION, 300.5);
    emp2.put(TestDomain.EMP_DEPARTMENT_FK, dept10);
    emp2.put(TestDomain.EMP_HIREDATE, hiredate);
    emp2.put(TestDomain.EMP_ID, -200);
    emp2.put(TestDomain.EMP_JOB, "CLERK");
    emp2.put(TestDomain.EMP_MGR_FK, mgr50);
    emp2.put(TestDomain.EMP_NAME, "NAME");
    emp2.put(TestDomain.EMP_SALARY, BigDecimal.valueOf(3500.5));

    mapper.setIncludeForeignKeyValues(true);

    final List<Entity> entityList = asList(emp1, emp2);
    jsonString = mapper.writeValueAsString(entityList);
    final List<Entity> parsedEntities = mapper.deserializeEntities(jsonString);
    for (final Entity entity : entityList) {
      final Entity parsed = parsedEntities.get(parsedEntities.indexOf(entity));
      assertTrue(parsed.columnValuesEqual(entity));
    }

    final List<Entity> readEntities = mapper.deserializeEntities(mapper.serializeEntities(singletonList(emp1)));
    assertEquals(1, readEntities.size());
    final Entity parsedEntity = readEntities.iterator().next();
    assertTrue(emp1.columnValuesEqual(parsedEntity));
    assertTrue(parsedEntity.isModified());
    assertTrue(parsedEntity.isModified(TestDomain.EMP_COMMISSION));
    assertTrue(parsedEntity.isModified(TestDomain.EMP_DEPARTMENT));
    assertTrue(parsedEntity.isModified(TestDomain.EMP_JOB));
    assertTrue(parsedEntity.isModified(TestDomain.EMP_MGR));
    assertTrue(parsedEntity.isModified(TestDomain.EMP_NAME));
    assertTrue(parsedEntity.isModified(TestDomain.EMP_SALARY));
    assertTrue(parsedEntity.isModified(TestDomain.EMP_HIREDATE));

    final Entity emp3 = entities.entity(TestDomain.T_EMP);
    emp3.put(TestDomain.EMP_COMMISSION, 300.5);
    emp3.put(TestDomain.EMP_DEPARTMENT_FK, dept10);
    emp3.put(TestDomain.EMP_HIREDATE, null);
    emp3.put(TestDomain.EMP_ID, -200);
    emp3.put(TestDomain.EMP_JOB, "CLERK");
    emp3.put(TestDomain.EMP_MGR_FK, mgr50);
    emp3.put(TestDomain.EMP_NAME, "NAME");
    emp3.put(TestDomain.EMP_SALARY, null);

    mapper.setIncludeForeignKeyValues(false);
    mapper.setIncludeNullValues(false);

    final Entity emp3Parsed = mapper.deserializeEntities(mapper.serializeEntities(singletonList(emp3))).get(0);
    assertFalse(emp3Parsed.contains(TestDomain.EMP_HIREDATE));
    assertFalse(emp3Parsed.contains(TestDomain.EMP_SALARY));
  }

  @Test
  void dependencyMap() throws JsonProcessingException {
    final EntityObjectMapper mapper = new EntityObjectMapper(entities);

    final Entity dept = entities.entity(TestDomain.T_DEPARTMENT);
    dept.put(TestDomain.DEPARTMENT_ID, 1);
    dept.put(TestDomain.DEPARTMENT_NAME, "Name");
    dept.put(TestDomain.DEPARTMENT_LOCATION, "Location");
    dept.put(TestDomain.DEPARTMENT_LOCATION, "New Location");

    final Map<String, Collection<Entity>> map = new HashMap<>();

    map.put(TestDomain.T_DEPARTMENT.getName(), singletonList(dept));

    final String string = mapper.writeValueAsString(map);

    mapper.readValue(string, new TypeReference<Map<String, Collection<Entity>>>() {});
  }

  @Test
  void customSerializer() throws JsonProcessingException {
    final CustomEntityObjectMapperFactory mapperFactory = (CustomEntityObjectMapperFactory) EntityObjectMapperFactory.entityObjectMapperFactory(TestDomain.DOMAIN);
    final EntityObjectMapper mapper = mapperFactory.createEntityObjectMapper(entities);

    final Custom custom = new Custom("a value");
    assertEquals(custom.value, mapper.readValue(mapper.writeValueAsString(custom), Custom.class).value);
  }

  public static final class CustomEntityObjectMapperFactory extends DefaultEntityObjectMapperFactory {

    public CustomEntityObjectMapperFactory() {
      super(TestDomain.DOMAIN);
    }

    @Override
    public EntityObjectMapper createEntityObjectMapper(final Entities entities) {
      final EntityObjectMapper mapper = EntityObjectMapper.createEntityObjectMapper(entities);
      mapper.addSerializer(Custom.class, new StdSerializer<Custom>(Custom.class) {
        @Override
        public void serialize(final Custom value, final JsonGenerator gen, final SerializerProvider provider) throws IOException {
          gen.writeStartObject();
          gen.writeStringField("value", value.value);
          gen.writeEndObject();
        }
      });
      mapper.addDeserializer(Custom.class, new StdDeserializer<Custom>(Custom.class) {
        @Override
        public Custom deserialize(final JsonParser p, final DeserializationContext ctxt) throws IOException,
                JsonProcessingException {
          final JsonNode node = p.getCodec().readTree(p);

          return new Custom(node.get("value").asText());
        }
      });

      return mapper;
    }
  }

  private static final class Custom {
    private final String value;

    private Custom(final String value) {
      this.value = value;
    }
  }
}
