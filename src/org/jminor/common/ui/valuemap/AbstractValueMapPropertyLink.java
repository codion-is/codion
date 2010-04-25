/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.valuemap;

import org.jminor.common.model.valuemap.ChangeValueMapEditModel;
import org.jminor.common.model.valuemap.exception.ValidationException;
import org.jminor.common.ui.control.AbstractPropertyLink;
import org.jminor.common.ui.control.LinkType;

/**
 * An abstract class for linking a UI component to a ChangeValueMapEditModel key value.
 */
public abstract class AbstractValueMapPropertyLink<T, V> extends AbstractPropertyLink<ChangeValueMapEditModel<T, V>, V> {

  /**
   * The linked key
   */
  private final T key;

  /**
   * @param editModel the ChangeValueMapEditModel instance
   * @param key the key of the value to link
   * @param linkType the link type
   */
  public AbstractValueMapPropertyLink(final ChangeValueMapEditModel<T, V> editModel, final T key, final LinkType linkType) {
    super(editModel, editModel.getPropertyChangeEvent(key), linkType);
    this.key = key;
  }

  /** {@inheritDoc} */
  @Override
  public V getModelPropertyValue() {
    return isModelPropertyValueNull() ? null : getEditModel().getValue(key);
  }

  /** {@inheritDoc} */
  @Override
  public void setModelPropertyValue(final V value) {
    getEditModel().setValue(key, value);
  }

  /**
   * @return true if the underlying model value associated with this key is null
   */
  protected boolean isModelPropertyValueNull() {
    return getEditModel().isValueNull(key);
  }

  /**
   * @param value the value
   * @return true it the value is or represents null
   */
  protected boolean isNull(final V value) {
    return getEditModel().isNull(getKey(), value);
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
   * @param editModel the underlying ChangeValueMapEditModel
   * @return a validation string if the value is invalid, null otherwise
   */
  protected String getValidationMessage(final ChangeValueMapEditModel<T, V> editModel) {
    try {
      editModel.validate(getKey(), ChangeValueMapEditModel.UNKNOWN);
      return null;
    }
    catch (ValidationException e) {
      return e.getMessage();
    }
  }

  /**
   * @return the property owner, in this case a ChangeValueMapEditModel
   */
  private ChangeValueMapEditModel<T, V> getEditModel() {
    return super.getPropertyOwner();
  }
}
