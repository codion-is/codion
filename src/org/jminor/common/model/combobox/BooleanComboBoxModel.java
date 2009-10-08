/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model.combobox;

import org.jminor.common.i18n.Messages;

/**
 * A ComboBoxModel for boolean values, true, false and null
 */
public class BooleanComboBoxModel extends ItemComboBoxModel {

  /** Constructs a new BooleanComboBoxModel. */
  public BooleanComboBoxModel() {
    super(new Item<Boolean>(null, "-"), new Item<Boolean>(true, Messages.get(Messages.YES)),
            new Item<Boolean>(false, Messages.get(Messages.NO)));
  }
}