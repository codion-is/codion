/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.property;

/**
 * A property that does not map to an underlying database column. The value of a transient property
 * is initialized to null when entities are loaded, which means transient properties always have null as the original value.
 * The value of transient properties can be set and retrieved like normal properties but are ignored during DML operations.
 * Note that by default setting a transient value marks the entity as being modified, but trying to update an entity
 * with only transient values modified will result in an error.
 */
public interface TransientProperty<T> extends Property<T> {

  /**
   * @return true if the value of this property being modified should result in a modified entity
   */
  boolean isModifiesEntity();

  /**
   * Builds a TransientProperty instance
   */
  interface Builder<T> extends Property.Builder<T> {

    /**
     * @return the property
     */
    TransientProperty<T> get();

    /**
     * @param modifiesEntity if true then modifications to the value result in the owning entity becoming modified
     * @return this property instance
     */
    TransientProperty.Builder<T> modifiesEntity(boolean modifiesEntity);
  }
}
