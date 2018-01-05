/*
 * Chinook.Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.model.combobox;

import org.jminor.common.Item;
import org.jminor.common.i18n.Messages;

import java.util.Arrays;

/**
 * A ComboBoxModel for boolean values, true, false and null.
 */
public final class BooleanComboBoxModel extends ItemComboBoxModel<Boolean> {

  /**
   * Constructs a new BooleanComboBoxModel.
   */
  public BooleanComboBoxModel() {
    this("-");
  }

  /**
   * Constructs a new BooleanComboBoxModel.
   * @param nullString the string representing a null value
   */
  public BooleanComboBoxModel(final String nullString) {
    this(nullString, Messages.get(Messages.YES), Messages.get(Messages.NO));
  }

  /**
   * Constructs a new BooleanComboBoxModel.
   * @param nullString the string representing a null value
   * @param trueString the string representing the boolean value 'true'
   * @param falseString the string representing the boolean value 'false'
   */
  public BooleanComboBoxModel(final String nullString, final String trueString, final String falseString) {
    super(null, Arrays.asList(new Item<>(null, nullString), new Item<>(true, trueString), new Item<>(false, falseString)));
  }
}