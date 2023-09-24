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
package is.codion.swing.common.ui.control;

import is.codion.common.state.StateObserver;
import is.codion.common.value.Value;

import javax.swing.Icon;
import javax.swing.KeyStroke;

import static java.util.Objects.requireNonNull;

final class ToggleControlBuilder implements ToggleControl.Builder {

  private final Value<Boolean> value;

  private String name;
  private StateObserver enabled;
  private char mnemonic;
  private Icon smallIcon;
  private Icon largeIcon;
  private String description;
  private KeyStroke keyStroke;

  ToggleControlBuilder(Value<Boolean> value) {
    this.value = requireNonNull(value);
  }

  @Override
  public ToggleControl.Builder name(String name) {
    this.name = name;
    return this;
  }

  @Override
  public ToggleControl.Builder enabled(StateObserver enabled) {
    this.enabled = enabled;
    return this;
  }

  @Override
  public ToggleControl.Builder mnemonic(char mnemonic) {
    this.mnemonic = mnemonic;
    return this;
  }

  @Override
  public ToggleControl.Builder smallIcon(Icon smallIcon) {
    this.smallIcon = smallIcon;
    return this;
  }

  @Override
  public ToggleControl.Builder largeIcon(Icon largeIcon) {
    this.largeIcon = largeIcon;
    return this;
  }

  @Override
  public ToggleControl.Builder description(String description) {
    this.description = description;
    return this;
  }

  @Override
  public ToggleControl.Builder keyStroke(KeyStroke keyStroke) {
    this.keyStroke = keyStroke;
    return this;
  }

  @Override
  public ToggleControl build() {
    DefaultToggleControl toggleControl = new DefaultToggleControl(value, name, enabled);
    toggleControl.setMnemonic(mnemonic);
    toggleControl.setSmallIcon(smallIcon);
    toggleControl.setLargeIcon(largeIcon);
    toggleControl.setDescription(description);
    toggleControl.setKeyStroke(keyStroke);

    return toggleControl;
  }
}
