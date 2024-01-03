/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2021 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.button;

import is.codion.common.value.Value;
import is.codion.swing.common.model.component.button.NullableToggleButtonModel;

import javax.swing.JCheckBox;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;

final class DefaultCheckBoxBuilder extends DefaultToggleButtonBuilder<JCheckBox, CheckBoxBuilder> implements CheckBoxBuilder {

  private boolean nullable = false;

  DefaultCheckBoxBuilder(Value<Boolean> linkedValue) {
    super(linkedValue);
    if (linkedValue != null && linkedValue.nullable()) {
      nullable = true;
    }
    horizontalAlignment(SwingConstants.LEADING);
  }

  @Override
  public CheckBoxBuilder nullable(boolean nullable) {
    this.nullable = nullable;
    return this;
  }

  @Override
  protected JToggleButton createToggleButton() {
    return nullable ? new NullableCheckBox(new NullableToggleButtonModel()) : new JCheckBox();
  }
}
