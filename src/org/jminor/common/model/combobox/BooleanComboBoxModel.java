/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model.combobox;

import org.jminor.common.i18n.Messages;
import org.jminor.common.model.Item;

/**
 * A ComboBoxModel for boolean values, true, false and null.
 */
public final class BooleanComboBoxModel extends ItemComboBoxModel<Boolean> {

  /**
   * Constructs a new BooleanComboBoxModel.
   */
  public BooleanComboBoxModel() {
    this("-", Messages.get(Messages.YES), Messages.get(Messages.NO));
  }

  public BooleanComboBoxModel(final String nullString, final String trueString, final String falseString) {
    super(new Item<Boolean>(null, nullString), new Item<Boolean>(true, trueString), new Item<Boolean>(false, falseString));
  }
}