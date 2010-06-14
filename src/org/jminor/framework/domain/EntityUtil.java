/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain;

import org.jminor.common.model.DateUtil;
import org.jminor.common.model.Deserializer;
import org.jminor.common.model.Item;
import org.jminor.common.model.Serializer;
import org.jminor.common.model.Util;
import org.jminor.common.model.valuemap.ValueProvider;
import org.jminor.framework.Configuration;

import java.sql.Timestamp;
import java.text.Collator;
import java.util.*;

/**
 * A static utility class.
 */
public class EntityUtil {

  public static final Random random = new Random();

  private EntityUtil() {}

  /**
   * @param entities the entities
   * @return a List of entities that have been modified
   */
  public static List<Entity> getModifiedEntities(final Collection<Entity> entities) {
    final List<Entity> modifiedEntities = new ArrayList<Entity>();
    for (final Entity entity : entities) {
      if (entity.isModified()) {
        modifiedEntities.add(entity);
      }
    }

    return modifiedEntities;
  }

  public static Map<Entity.Key, Entity> hashByPrimaryKey(final List<Entity> entities) {
    final Map<Entity.Key, Entity> entityMap = new HashMap<Entity.Key, Entity>();
    for (final Entity entity : entities) {
      entityMap.put(entity.getPrimaryKey(), entity);
    }

    return entityMap;
  }

  /**
   * @param entities the entities
   * @return a List containing the primary keys of the given entities
   */
  public static List<Entity.Key> getPrimaryKeys(final Collection<Entity> entities) {
    final List<Entity.Key> keys = new ArrayList<Entity.Key>(entities.size());
    for (final Entity entity : entities) {
      keys.add(entity.getPrimaryKey());
    }

    return keys;
  }

  public static List<Object> getOriginalPropertyValues(final List<Entity.Key> keys) {
    final List<Object> list = new ArrayList<Object>(keys.size());
    for (final Entity.Key key : keys) {
      list.add(key.getOriginalValue(key.getFirstKeyProperty().getPropertyID()));
    }

    return list;
  }

  /**
   * @param propertyID the ID of the property for which to retrieve the values
   * @param entities the entities from which to retrieve the property value
   * @return a List containing the values of the property with the given ID from the given entities,
   * null values are included
   */
  public static List<Object> getPropertyValues(final String propertyID, final List<Entity> entities) {
    return getPropertyValues(propertyID, entities, true);
  }

  /**
   * @param propertyID the ID of the property for which to retrieve the values
   * @param entities the entities from which to retrieve the property value
   * @param includeNullValues if true then null values are included
   * @return a List containing the values of the property with the given ID from the given entities
   */
  public static List<Object> getPropertyValues(final String propertyID, final List<Entity> entities,
                                               final boolean includeNullValues) {
    if (entities == null || entities.size() == 0) {
      return new ArrayList<Object>(0);
    }

    return getPropertyValues(entities.get(0).getProperty(propertyID), entities, includeNullValues);
  }

  /**
   * @param property the the property for which to retrieve the values
   * @param entities the entities from which to retrieve the property value
   * @return a List containing the values of the property with the given ID from the given entities,
   * null values are included
   */
  public static List<Object> getPropertyValues(final Property property, final List<Entity> entities) {
    return getPropertyValues(property, entities, true);
  }

  /**
   * @param property the the property for which to retrieve the values
   * @param entities the entities from which to retrieve the property value
   * @param includeNullValues if true then null values are included
   * @return a List containing the values of the property with the given ID from the given entities
   */
  public static List<Object> getPropertyValues(final Property property, final List<Entity> entities,
                                               final boolean includeNullValues) {
    final List<Object> values = new ArrayList<Object>(entities.size());
    for (final Entity entity : entities) {
      if (includeNullValues) {
        values.add(entity.getValue(property));
      }
      else if (!entity.isValueNull(property)) {
        values.add(entity.getValue(property));
      }
    }

    return values;
  }

  /**
   * Returns a Collection containing the distinct values of <code>propertyID</code> from the given entities.
   * If the <code>entities</code> list is null an empty Collection is returned.
   * @param entities the entities from which to retrieve the values
   * @param propertyID the ID of the property for which to retrieve the values
   * @return a Collection containing the distinct property values
   */
  public static Collection<Object> getDistinctPropertyValues(final List<Entity> entities, final String propertyID) {
    final Set<Object> values = new HashSet<Object>();
    if (entities == null) {
      return values;
    }
    for (final Entity entity : entities) {
      values.add(entity.getValue(propertyID));
    }

    return values;
  }

  /**
   * Sets the value of the property with ID <code>propertyID</code> to <code>value</code>
   * in the given entities
   * @param propertyID the ID of the property for which to set the value
   * @param value the value
   * @param entities the entities for which to set the value
   * @return the old property values mapped to their respective primary key
   */
  public static Map<Entity.Key, Object> setPropertyValue(final String propertyID, final Object value,
                                                         final List<Entity> entities) {
    final Map<Entity.Key, Object> oldValues = new HashMap<Entity.Key, Object>(entities.size());
    for (final Entity entity : entities) {
      oldValues.put(entity.getPrimaryKey(), entity.setValue(propertyID, value));
    }

    return oldValues;
  }

  /**
   * Returns a Map containing the given entities hashed by the value of the property with ID <code>propertyID</code>
   * @param entities the entities to map by property value
   * @param propertyID the ID of the property which value should be used for mapping
   * @return a Map of entities hashed by property value
   */
  public static Map<Object, Collection<Entity>> hashByPropertyValue(final List<Entity> entities, final String propertyID) {
    return Util.map(entities, new Util.HashKeyProvider<Object, Entity>() {
      public Object getKey(final Entity value) {
        return value.getValue(propertyID);
      }
    });
  }

  /**
   * Returns a Map containing the given entities hashed by their entityIDs
   * @param entities the entities to map by entityID
   * @return a Map of entities hashed by entityID
   */
  public static Map<String, Collection<Entity>> hashByEntityID(final Collection<Entity> entities) {
    return Util.map(entities, new Util.HashKeyProvider<String, Entity>() {
      public String getKey(final Entity value) {
        return value.getEntityID();
      }
    });
  }

  /**
   * Returns a Map containing the given entity keys hashed by their entityIDs
   * @param keys the entity keys to map by entityID
   * @return a Map of entity keys hashed by entityID
   */
  public static Map<String, Collection<Entity.Key>> hashKeysByEntityID(final Collection<Entity.Key> keys) {
    return Util.map(keys, new Util.HashKeyProvider<String, Entity.Key>() {
      public String getKey(final Entity.Key value) {
        return value.getEntityID();
      }
    });
  }

  public static List<Property> getProperties(final String entityID, final Collection<String> propertyIDs) {
    if (propertyIDs == null || propertyIDs.size() == 0) {
      return new ArrayList<Property>(0);
    }

    final List<Property> properties = new ArrayList<Property>(propertyIDs.size());
    for (final String propertyID : propertyIDs) {
      properties.add(EntityRepository.getProperty(entityID, propertyID));
    }

    return properties;
  }

  public static List<Property> getSortedProperties(final String entityID, final Collection<String> propertyIDs) {
    return sort(getProperties(entityID, propertyIDs));
  }

  public static List<Property> sort(final List<Property> properties) {
    final Collator collator = Collator.getInstance();
    Collections.sort(properties, new Comparator<Property>() {
      public int compare(final Property propertyOne, final Property propertyTwo) {
        return collator.compare(propertyOne.toString().toLowerCase(), propertyTwo.toString().toLowerCase());
      }
    });

    return properties;
  }

  public static List<Property> getUpdateProperties(final String entityID) {
    final List<Property> properties = EntityRepository.getDatabaseProperties(entityID,
            !EntityRepository.getIdSource(entityID).isAutoGenerated(), false, false);
    final ListIterator<Property> iterator = properties.listIterator();
    while(iterator.hasNext()) {
      final Property property = iterator.next();
      if (property.hasParentProperty() || property.isDenormalized()) {
        iterator.remove();
      }
    }
    sort(properties);

    return properties;
  }

  public static List<Entity> copyEntities(final List<Entity> entities) {
    final List<Entity> copies = new ArrayList<Entity>(entities.size());
    for (final Entity entity : entities) {
      copies.add((Entity) entity.getCopy());
    }

    return copies;
  }

  public static Entity createRandomEntity(final String entityID, final Map<String, Entity> referenceEntities) {
    return createEntity(entityID, new ValueProvider<Property, Object>() {
      public Object getValue(final Property property) {
        return getRandomValue(property, referenceEntities);
      }
    });
  }

  public static Entity createEntity(final String entityID, final ValueProvider<Property, Object> valueProvider) {
    final Entity entity = new Entity(entityID);
    final boolean autoGenID = EntityRepository.isPrimaryKeyAutoGenerated(entityID);
    for (final Property property : EntityRepository.getDatabaseProperties(entityID, !autoGenID, false, true)) {
      if (!property.hasParentProperty() && !property.isDenormalized()) {
        entity.setValue(property, valueProvider.getValue(property));
      }
    }

    return entity;
  }

  public static Entity randomize(final Entity entity, final boolean includePrimaryKey, final Map<String, Entity> referenceEntities) {
    for (final Property property : EntityRepository.getDatabaseProperties(entity.getEntityID(), includePrimaryKey, false, true)) {
      if (property instanceof Property.ForeignKeyProperty) {
        final Property.ForeignKeyProperty foreignKeyProperty = (Property.ForeignKeyProperty) property;
        if (referenceEntities != null && referenceEntities.containsKey(foreignKeyProperty.getReferencedEntityID())) {
          entity.setValue(property, getRandomValue(property, referenceEntities));
        }
      }
      else if (!property.hasParentProperty() && !property.isDenormalized()) {
        entity.setValue(property, getRandomValue(property, referenceEntities));
      }
    }

    return entity;
  }

  public static Object getRandomValue(final Property property, final Map<String, Entity> referenceEntities) {
    if (property instanceof Property.ForeignKeyProperty) {
      final String referenceEntityID = ((Property.ForeignKeyProperty) property).getReferencedEntityID();
      return referenceEntities == null ? null : referenceEntities.get(referenceEntityID);
    }
    else {
      if (property instanceof Property.ValueListProperty) {
        final List<Item<Object>> items = ((Property.ValueListProperty) property).getValues();
        final Item item = items.get(random.nextInt(items.size()));

        return item.getItem();
      }
      else if (property.isBoolean()) {
        return random.nextBoolean();
      }
      else if (property.isCharacter()) {
        return (char) random.nextInt();
      }
      else if (property.isDate()) {
        return DateUtil.floorDate(new Date());
      }
      else if (property.isTimestamp()) {
        return DateUtil.floorTimestamp(new Timestamp(System.currentTimeMillis()));
      }
      else if (property.isDouble()) {
        double min = property.getMin() == null ? -10000000 : property.getMin();
        double max = property.getMax() == null ? 10000000 : property.getMax();
        //Min + (int)(Math.random() * ((Max - Min) + 1))
        final double ret = min + (random.nextDouble() * ((max - min) + 1));
        if (property.getMaximumFractionDigits() > 0) {
          return Util.roundDouble(ret, property.getMaximumFractionDigits());
        }
        else {
          return ret;
        }
      }
      else if (property.isInteger()) {
        double min = property.getMin() == null ? -10000000 : property.getMin();
        double max = property.getMax() == null ? 10000000 : property.getMax();

        return (int) (min + (random.nextDouble() * ((max - min) + 1)));
      }
      else if (property.isString()) {
        final int minLength = property.isNullable() ? 0 : 1;
        return Util.createRandomString(minLength, property.getMaxLength() < 0 ? 10 : property.getMaxLength());
      }
    }

    return null;
  }

  public static Serializer<Entity> getEntitySerializer() {
    if (!Configuration.entitySerializerAvailable())
      throw new RuntimeException("Required configuration property is missing: " + Configuration.ENTITY_SERIALIZER_CLASS);

    try {
      final String serializerClass = Configuration.getStringValue(Configuration.ENTITY_SERIALIZER_CLASS);

      return (Serializer<Entity>) Class.forName(serializerClass).getConstructor().newInstance();
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static Deserializer<Entity> getEntityDeserializer() {
    if (!Configuration.entityDeserializerAvailable())
      throw new RuntimeException("Required configuration property is missing: " + Configuration.ENTITY_DESERIALIZER_CLASS);

    try {
      final String deserializerClass = Configuration.getStringValue(Configuration.ENTITY_DESERIALIZER_CLASS);

      return (Deserializer<Entity>) Class.forName(deserializerClass).getConstructor().newInstance();
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
