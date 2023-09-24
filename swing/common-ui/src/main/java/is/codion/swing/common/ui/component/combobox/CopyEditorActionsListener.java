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
package is.codion.swing.common.ui.component.combobox;

import javax.swing.ActionMap;
import javax.swing.ComboBoxEditor;
import javax.swing.InputMap;
import javax.swing.JComponent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Arrays;

final class CopyEditorActionsListener implements PropertyChangeListener {

  private JComponent previousEditor;

  @Override
  public void propertyChange(PropertyChangeEvent event) {
    ComboBoxEditor oldEditor = (ComboBoxEditor) event.getOldValue();
    if (oldEditor != null) {
      previousEditor = (JComponent) oldEditor.getEditorComponent();
    }
    ComboBoxEditor newEditor = (ComboBoxEditor) event.getNewValue();
    if (newEditor != null && previousEditor != null) {
      copyActions(previousEditor, (JComponent) newEditor.getEditorComponent());
      previousEditor = null;
    }
  }

  private static void copyActions(JComponent previousComponent, JComponent newComponent) {
    ActionMap previousActionMap = previousComponent.getActionMap();
    ActionMap newActionMap = newComponent.getActionMap();
    Arrays.stream(previousActionMap.allKeys()).forEach(key -> newActionMap.put(key, previousActionMap.get(key)));
    InputMap previousInputMap = previousComponent.getInputMap();
    InputMap newInputMap = newComponent.getInputMap();
    Arrays.stream(previousInputMap.allKeys()).forEach(key -> newInputMap.put(key, previousInputMap.get(key)));
  }
}
