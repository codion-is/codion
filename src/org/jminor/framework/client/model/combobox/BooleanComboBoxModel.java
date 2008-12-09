/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model.combobox;

import org.jminor.common.model.Event;
import org.jminor.framework.model.Type;

import javax.swing.DefaultComboBoxModel;

public class BooleanComboBoxModel extends DefaultComboBoxModel {

  public final Event evtSelectedItemChanged = new Event("BooleanComboBoxModel.evtSelectedItemChanged");

  /** Constructs a new BooleanComboBoxModel. */
  public BooleanComboBoxModel() {
    addElement(Type.Boolean.NULL);
    addElement(Type.Boolean.TRUE);
    addElement(Type.Boolean.FALSE);
  }

  /** {@inheritDoc} */
  public void setSelectedItem(final Object item) {
    if (item == null)
      super.setSelectedItem(Type.Boolean.NULL);
    else
      super.setSelectedItem(item);

    evtSelectedItemChanged.fire();
  }
}