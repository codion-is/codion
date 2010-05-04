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
 * @param <T> the type of the map keys
 * @param <V> the type of the map values
 */
public abstract class AbstractValueMapLink<T, V> extends AbstractValueLink<ValueChangeMapEditModel<T, V>, V> {

  /**
   * The linked key
   */
  private final T key;

  /**
   * @param editModel the ValueChangeMapEditModel instance
   * @param key the key of the value to link
   * @param linkType the link type
   */
  public AbstractValueMapLink(final ValueChangeMapEditModel<T, V> editModel, final T key, final LinkType linkType) {
    super(editModel, editModel.getPropertyChangeEvent(key), linkType);
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
    return getEditModel().isNullable(getKey());
  }

  /**
   * @return the linked key
   */
  protected T getKey() {
    return key;
  }

  /**
   * If the current value is invalid this method should return a string describing the nature of
   * the invalidity, if the value is valid this method should return null
   * @param editModel the underlying ValueChangeMapEditModel
   * @return a validation string if the value is invalid, null otherwise
   */
  protected String getValidationMessage(final ValueChangeMapEditModel<T, V> editModel) {
    try {
      editModel.validate(getKey(), ValueChangeMapEditModel.UNKNOWN);
      return null;
    }
    catch (ValidationException e) {
      return e.getMessage();
    }
  }

  /**
   * @return the value owner, in this case a ValueChangeMapEditModel
   */
  protected ValueChangeMapEditModel<T, V> getEditModel() {
    return super.getValueOwner();
  }
}
