/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model.combobox;

import org.jminor.common.model.Event;
import org.jminor.framework.model.Type;

import javax.swing.DefaultComboBoxModel;

/**
 * A ComboBoxModel for boolean values, true, false and null
 * @see org.jminor.framework.model.Type.Boolean
 */
public class BooleanComboBoxModel extends DefaultComboBoxModel {

  public final Event evtSelectedItemChanged = new Event();

  /** Constructs a new BooleanComboBoxModel. */
  public BooleanComboBoxModel() {
    addElement(Type.Boolean.NULL);
    addElement(Type.Boolean.TRUE);
    addElement(Type.Boolean.FALSE);
  }

  public void setSelectedItem(final boolean item) {
    setSelectedItem(item ? Type.Boolean.TRUE : Type.Boolean.FALSE);
  }

  /** {@inheritDoc} */
  @Override
  public void setSelectedItem(final Object item) {
    if (item != null && !(item instanceof Type.Boolean))
      throw new IllegalArgumentException("BooleanComboBoxModel only accepts org.jminor.framework.model.Type.Boolean values");

    super.setSelectedItem(item == null ? Type.Boolean.NULL : item);

    evtSelectedItemChanged.fire();
  }
}