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

import javax.swing.JTextField;
import java.util.Optional;

/**
 * Provides the user with the ability to select a value.
 * @param <T> the value type
 */
public interface SelectionProvider<T> {

  /**
   * @param textField the text field in which value should be selected
   * @return the selected value, an empty Optional if nothing was selected
   * @throws is.codion.common.model.CancelException in case the user cancelled
   */
  Optional<T> select(JTextField textField);
}
