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
 * Copyright (c) 2022 - 2023, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.text;

import is.codion.swing.common.model.component.text.DocumentAdapter;

import javax.swing.event.DocumentEvent;
import javax.swing.text.JTextComponent;
import java.util.function.Consumer;

final class OnTextChangedListener implements DocumentAdapter {

  private final Consumer<String> onTextChanged;
  private final JTextComponent textComponent;

  OnTextChangedListener(Consumer<String> onTextChanged, JTextComponent textComponent) {
    this.onTextChanged = onTextChanged;
    this.textComponent = textComponent;
  }

  @Override
  public void contentsChanged(DocumentEvent e) {
    onTextChanged.accept(textComponent.getText());
  }
}
