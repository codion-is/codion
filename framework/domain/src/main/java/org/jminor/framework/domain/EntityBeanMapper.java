/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain;

import org.jminor.common.Util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;

/**
 * A class for mapping between entities and corresponding bean classes
 */
public class EntityBeanMapper {

  private static final String BEAN_CLASS_PARAM = "beanClass";
  private static final String ENTITY_ID_PARAM = "entityId";
  private static final String PROPERTY_ID_PARAM = "propertyId";
  private static final String PROPERTY_NAME_PARAM = "propertyName";

  private final Domain domain;

  private final Map<Class, String> entityIdMap = new HashMap<>();
  private final Map<Class, Map<String, GetterSetter>> propertyMap = new HashMap<>();

  /**
   * @param domain the domain model
   */
  public EntityBeanMapper(final Domain domain) {
    this.domain = domain;
  }

  /**
   * Associates the given bean class with the given entityId
   * @param beanClass the bean class representing entities with the given entityId
   * @param entityId the id of the entity represented by the given bean class
   */
  public final void setEntityId(final Class beanClass, final String entityId) {
    requireNonNull(beanClass, BEAN_CLASS_PARAM);
    requireNonNull(entityId, ENTITY_ID_PARAM);
    entityIdMap.put(beanClass, entityId);
  }

  /**
   * @param beanClass the bean class
   * @return the entityId of the entity represented by the given bean class, null if none is specified
   */
  public final String getEntityId(final Class beanClass) {
    requireNonNull(beanClass, BEAN_CLASS_PARAM);
    return entityIdMap.get(beanClass);
  }

  /**
   * @param entityId the entityId
   * @return the class of the bean representing entities with the given entityId
   * @throws IllegalArgumentException in case no bean class has been defined for the given entityId
   */
  public final Class getBeanClass(final String entityId) {
    requireNonNull(entityId, ENTITY_ID_PARAM);
    for (final Map.Entry<Class, String> entry : entityIdMap.entrySet()) {
      if (entry.getValue().equals(entityId)) {
        return entry.getKey();
      }
    }

    throw new IllegalArgumentException("No bean class defined for entityId: " + entityId);
  }

  /**
   * Links the given bean property name to the property identified by the given propertyId in the specified bean class
   * @param beanClass the bean class
   * @param propertyId the propertyId of the entity property
   * @param propertyName the name of the bean property
   * @throws NoSuchMethodException if the required setter/getter methods are not found
   */
  public final void setProperty(final Class beanClass, final String propertyId, final String propertyName) throws NoSuchMethodException {
    requireNonNull(beanClass, BEAN_CLASS_PARAM);
    requireNonNull(propertyId, PROPERTY_ID_PARAM);
    requireNonNull(propertyName, PROPERTY_NAME_PARAM);
    final Map<String, GetterSetter> beanPropertyMap = propertyMap.computeIfAbsent(beanClass, k -> new HashMap<>());
    final Property property = domain.getProperty(getEntityId(beanClass), propertyId);
    final Method getter = Util.getGetMethod(property.getTypeClass(), propertyName, beanClass);
    final Method setter = Util.getSetMethod(property.getTypeClass(), propertyName, beanClass);
    beanPropertyMap.put(propertyId, new GetterSetter(getter, setter));
  }

  /**
   * Transforms the given bean into a Entity according to the information found in this EntityBeanMapper instance
   * @param bean the bean to transform
   * @return a Entity derived from the given bean
   * @throws java.lang.reflect.InvocationTargetException in case an exception is thrown during a bean method call
   * @throws IllegalAccessException if a required method is not accessible
   */
  public Entity toEntity(final Object bean) throws InvocationTargetException, IllegalAccessException {
    requireNonNull(bean, "bean");
    final Entity entity = domain.entity(getEntityId(bean.getClass()));
    final Map<String, GetterSetter> beanPropertyMap = propertyMap.get(bean.getClass());
    for (final Map.Entry<String, GetterSetter> propertyEntry : beanPropertyMap.entrySet()) {
      final Property property = domain.getProperty(entity.getEntityId(), propertyEntry.getKey());
      entity.put(property, propertyEntry.getValue().getter.invoke(bean));
    }

    return entity;
  }

  /**
   * Transforms the given beans into a entities according to the information found in this EntityBeanMapper instance
   * @param beans the beans to transform
   * @return a List containing the entities derived from the given beans, an empty List if {@code beans} is null or empty
   * @throws InvocationTargetException in case an exception is thrown during a bean method call
   * @throws IllegalAccessException if a required method is not accessible
   */
  public List<Entity> toEntities(final List beans) throws InvocationTargetException, IllegalAccessException {
    if (Util.nullOrEmpty(beans)) {
      return emptyList();
    }
    final List<Entity> result = new ArrayList<>(beans.size());
    for (final Object bean : beans) {
      result.add(toEntity(bean));
    }

    return result;
  }

  /**
   * Transforms the given entity into a bean according to the information found in this EntityBeanMapper instance
   * @param entity the entity to transform
   * @return a bean derived from the given entity
   * @throws NoSuchMethodException if a required setter method is not found in the bean class
   * @throws InvocationTargetException in case an exception is thrown during a bean method call
   * @throws IllegalAccessException if a required method is not accessible
   * @throws InstantiationException if the bean class can not be instantiated
   */
  public Object toBean(final Entity entity) throws NoSuchMethodException,
          InvocationTargetException, IllegalAccessException, InstantiationException {
    requireNonNull(entity, "entity");
    final Class beanClass = getBeanClass(entity.getEntityId());
    final Object bean = beanClass.getConstructor().newInstance();
    final Map<String, GetterSetter> beanPropertyMap = propertyMap.get(beanClass);
    for (final Map.Entry<String, GetterSetter> propertyEntry : beanPropertyMap.entrySet()) {
      final Property property = domain.getProperty(entity.getEntityId(), propertyEntry.getKey());
      propertyEntry.getValue().setter.invoke(bean, entity.get(property));
    }

    return bean;
  }

  /**
   * Transforms the given entities into beans according to the information found in this EntityBeanMapper instance
   * @param entities the entities to transform
   * @return a List containing the beans derived from the given entities, an empty List if {@code entities} is null or empty
   * @throws NoSuchMethodException if a required setter method is not found in the bean class
   * @throws InvocationTargetException in case an exception is thrown during a bean method call
   * @throws IllegalAccessException if a required method is not accessible
   * @throws InstantiationException if the bean class can not be instantiated
   */
  public List<Object> toBeans(final List<Entity> entities) throws InvocationTargetException,
          NoSuchMethodException, InstantiationException, IllegalAccessException {
    if (Util.nullOrEmpty(entities)) {
      return emptyList();
    }
    final List<Object> beans = new ArrayList<>(entities.size());
    for (final Entity entity : entities) {
      beans.add(toBean(entity));
    }

    return beans;
  }

  private static final class GetterSetter {
    private final Method getter;
    private final Method setter;

    private GetterSetter(final Method getter, final Method setter) {
      this.getter = getter;
      this.setter = setter;
    }
  }
}
