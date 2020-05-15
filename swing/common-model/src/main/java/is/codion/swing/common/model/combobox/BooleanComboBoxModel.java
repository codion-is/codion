/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.model.combobox;

import is.codion.common.i18n.Messages;

import static is.codion.common.item.Items.item;
import static java.util.Arrays.asList;

/**
 * A ComboBoxModel for boolean values, true, false and null.
 */
public final class BooleanComboBoxModel extends ItemComboBoxModel<Boolean> {

  /**
   * Constructs a new BooleanComboBoxModel, with null as the initially selected value.
   */
  public BooleanComboBoxModel() {
    this("-");
  }

  /**
   * Constructs a new BooleanComboBoxModel, with null as the initially selected value.
   * @param nullString the string representing a null value
   */
  public BooleanComboBoxModel(final String nullString) {
    this(nullString, Messages.get(Messages.YES), Messages.get(Messages.NO));
  }

  /**
   * Constructs a new BooleanComboBoxModel, with null as the initially selected value.
   * @param nullString the string representing a null value
   * @param trueString the string representing the boolean value 'true'
   * @param falseString the string representing the boolean value 'false'
   */
  public BooleanComboBoxModel(final String nullString, final String trueString, final String falseString) {
    super(null, asList(item(null, nullString), item(true, trueString), item(false, falseString)));
    setSelectedItem(null);
  }
}