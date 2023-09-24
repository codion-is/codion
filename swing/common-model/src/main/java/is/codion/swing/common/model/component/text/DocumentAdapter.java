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
 * Copyright (c) 2010 - 2023, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.model.component.text;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * A document adapter, with default implementations for the {@link #insertUpdate(DocumentEvent)} and
 * {@link #removeUpdate(DocumentEvent)} calling {@link #contentsChanged(DocumentEvent)}.
 */
public interface DocumentAdapter extends DocumentListener {

  @Override
  default void changedUpdate(DocumentEvent e) {}

  @Override
  default void insertUpdate(DocumentEvent e) {
    contentsChanged(e);
  }

  @Override
  default void removeUpdate(DocumentEvent e) {
    contentsChanged(e);
  }

  /**
   * Called when the contents of this document change, either via insertion, update or removal
   * @param e the document event
   */
  void contentsChanged(DocumentEvent e);
}
