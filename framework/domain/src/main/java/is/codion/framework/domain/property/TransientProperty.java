/*
 * Copyright (c) 2019 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.property;

/**
 * A property that does not map to an underlying database column. The value of a transient property
 * is initialized to null when entities are loaded, which means transient properties always have null as the original value.
 * The value of transient properties can be set and retrieved like normal properties but are ignored during DML operations.
 * Note that by default setting a transient value marks the entity as being modified, but trying to update an entity
 * with only transient values modified will result in an error.
 * @param <T> the property value type
 */
public interface TransientProperty<T> extends Property<T> {

  /**
   * @return true if the value of this property being modified should result in a modified entity
   */
  boolean modifiesEntity();

  /**
   * Builds a TransientProperty instance
   * @param <T> the property value type
   */
  interface Builder<T, B extends Builder<T, B>> extends Property.Builder<T, B> {

    /**
     * @param modifiesEntity if false then modifications to the value will not result in the owning entity becoming modified
     * @return this property instance
     */
    TransientProperty.Builder<T, B> modifiesEntity(boolean modifiesEntity);
  }
}
