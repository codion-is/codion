/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model.combobox;

import org.jminor.common.i18n.Messages;
import org.jminor.common.model.Item;

/**
 * A ComboBoxModel for boolean values, true, false and null.
 */
public class BooleanComboBoxModel extends ItemComboBoxModel<Boolean> {

  /**
   * Constructs a new BooleanComboBoxModel.
   */
  public BooleanComboBoxModel() {
    super(new Item<Boolean>(null, "-"), new Item<Boolean>(true, Messages.get(Messages.YES)),
            new Item<Boolean>(false, Messages.get(Messages.NO)));
  }
}