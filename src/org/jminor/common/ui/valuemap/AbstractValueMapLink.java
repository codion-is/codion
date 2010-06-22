/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.valuemap;

import org.jminor.common.model.valuemap.ValueChangeMapEditModel;
import org.jminor.common.model.valuemap.exception.ValidationException;
import org.jminor.common.ui.control.AbstractValueLink;
import org.jminor.common.ui.control.LinkType;

/**
 * An abstract class for linking a UI component to a ValueChangeMapEditModel key value.
 * @param <K> the type of the map keys
 * @param <V> the type of the map values
 */
public abstract class AbstractValueMapLink<K, V> extends AbstractValueLink<ValueChangeMapEditModel<K, V>, V> {

  /**
   * The linked key
   */
  private final K key;

  /**
   * @param editModel the ValueChangeMapEditModel instance
   * @param key the key of the value to link
   * @param linkType the link type
   */
  public AbstractValueMapLink(final ValueChangeMapEditModel<K, V> editModel, final K key, final LinkType linkType) {
    super(editModel, editModel.getValueChangeEvent(key), linkType);
    this.key = key;
  }

  /** {@inheritDoc} */
  @Override
  public V getModelValue() {
    return isModelValueNull() ? null : getEditModel().getValue(key);
  }

  /** {@inheritDoc} */
  @Override
  public void setModelValue(final V value) {
    getEditModel().setValue(key, value);
  }

  /**
   * @return true if the underlying model value associated with this key is null
   */
  protected boolean isModelValueNull() {
    return getEditModel().isValueNull(key);
  }

  /**
   * @return true if this value is allowed to be null
   */
  protected boolean isNullable() {
    return getEditModel().isNullable(key);
  }

  /**
   * @return the linked key
   */
  protected K getKey() {
    return key;
  }

  /**
   * If the current value is invalid this method should return a string describing the nature of
   * the invalidity, if the value is valid this method should return null
   * @param editModel the underlying ValueChangeMapEditModel
   * @return a validation string if the value is invalid, null otherwise
   */
  protected String getValidationMessage(final ValueChangeMapEditModel<K, V> editModel) {
    try {
      editModel.validate(key, ValueChangeMapEditModel.UNKNOWN);
      return null;
    }
    catch (ValidationException e) {
      return e.getMessage();
    }
  }

  /**
   * @return the value owner, in this case a ValueChangeMapEditModel
   */
  protected ValueChangeMapEditModel<K, V> getEditModel() {
    return super.getValueOwner();
  }
}
