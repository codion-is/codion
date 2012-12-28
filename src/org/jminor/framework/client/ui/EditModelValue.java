/*
 * Copyright (c) 2004 - 2012, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui;

import org.jminor.common.model.EventObserver;
import org.jminor.common.ui.control.AbstractValueLink;
import org.jminor.framework.client.model.EntityEditModel;

public final class EditModelValue implements AbstractValueLink.ModelValue<Object> {

  private final EntityEditModel editModel;
  private final String key;

  public EditModelValue(final EntityEditModel editModel, final String key) {
    this.editModel = editModel;
    this.key = key;
  }

  /** {@inheritDoc} */
  @Override
  public Object get() {
    return editModel.getValue(key);
  }

  /** {@inheritDoc} */
  @Override
  public void set(final Object value) {
    editModel.setValue(key, value);
  }

  /** {@inheritDoc} */
  @Override
  public EventObserver getChangeEvent() {
    return editModel.getValueChangeObserver(key);
  }
}
