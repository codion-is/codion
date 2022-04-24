/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
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
