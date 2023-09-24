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
 * Copyright (c) 2021 - 2023, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.button;

import is.codion.swing.common.ui.component.value.AbstractComponentValue;
import is.codion.swing.common.ui.component.value.ComponentValue;

import javax.swing.Action;
import javax.swing.JButton;

final class DefaultButtonBuilder<B extends ButtonBuilder<Void, JButton, B>> extends AbstractButtonBuilder<Void, JButton, B>
        implements ButtonBuilder<Void, JButton, B> {

  DefaultButtonBuilder(Action action) {
    super(null);
    action(action);
  }

  @Override
  protected JButton createButton() {
    return new JButton();
  }

  @Override
  protected ComponentValue<Void, JButton> createComponentValue(JButton component) {
    return new AbstractComponentValue<Void, JButton>(component) {
      @Override
      protected Void getComponentValue() {
        return null;
      }

      @Override
      protected void setComponentValue(Void value) {/*Not applicable*/}
    };
  }

  @Override
  protected void setInitialValue(JButton component, Void initialValue) {/*Not applicable*/}
}
