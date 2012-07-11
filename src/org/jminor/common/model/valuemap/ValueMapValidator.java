/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model.valuemap;

import org.jminor.common.model.valuemap.exception.ValidationException;

/**
 * User: Björn Darri
 * Date: 26.7.2010
 * Time: 22:19:14
 */
public interface ValueMapValidator<K, V> {
  /**
   * Code for the insert action, used during validation
   */
  int INSERT = 1;
  /**
   * Code for the update action, used during validation
   */
  int UPDATE = 2;
  /**
   * Code for an unknown action, used during validation
   */
  int UNKNOWN = 3;

  /**
   * @param valueMap the value map
   * @param key the key
   * @return true if this value is allowed to be null in the given value map
   */
  boolean isNullable(final ValueMap<K, V> valueMap, final K key);

  /**
   * @param valueMap the value map
   * @param action the action requiring validation
   * @return true if the given value map contains only valid values
   */
  boolean isValid(final ValueMap<K, V> valueMap, final int action);

  /**
   * Checks if the values in the given value map are valid
   * @param valueMap the value map
   * @param action the action requiring validation
   * @throws ValidationException in case of an invalid value
   */
  void validate(final ValueMap<K, V> valueMap, final int action) throws ValidationException;

  /**
   * Checks if the value associated with the give key is valid, throws a ValidationException if not
   * @param valueMap the value map to validate
   * @param key the key the value is associated with
   * @param action describes the action requiring validation,
   * ValueMapEditModel.INSERT, ValueMapEditModel.UPDATE or ValueMapEditModel.UNKNOWN
   * @throws org.jminor.common.model.valuemap.exception.ValidationException if the given value is not valid for the given key
   */
  void validate(final ValueMap<K, V> valueMap, final K key, final int action) throws ValidationException;
}
