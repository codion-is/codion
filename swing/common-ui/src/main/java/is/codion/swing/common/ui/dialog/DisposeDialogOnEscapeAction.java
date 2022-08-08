/*
 * Copyright (c) 2021 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.dialog;

import is.codion.common.event.EventDataListener;
import is.codion.common.state.State;

import javax.swing.AbstractAction;
import javax.swing.JDialog;
import javax.swing.JPopupMenu;
import java.awt.Component;
import java.awt.Container;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.util.stream.Collectors.toList;

final class DisposeDialogOnEscapeAction extends AbstractAction {

  private final JDialog dialog;
  private final EventDataListener<State> confirmCloseListener;

  DisposeDialogOnEscapeAction(JDialog dialog, EventDataListener<State> confirmCloseListener) {
    super("DisposeDialogOnEscapeAction");
    this.dialog = dialog;
    this.confirmCloseListener = confirmCloseListener;
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    List<Window> heavyWeightWindows = Arrays.stream(dialog.getOwnedWindows()).filter(window ->
            window.getClass().getName().endsWith("Popup$HeavyWeightWindow") && window.isVisible()).collect(toList());
    if (!heavyWeightWindows.isEmpty()) {
      heavyWeightWindows.forEach(Window::dispose);

      return;
    }
    List<JPopupMenu> popupMenus = componentsOfType(dialog.getContentPane(), JPopupMenu.class);
    if (popupMenus.isEmpty()) {
      DisposeDialogAction.closeIfConfirmed(dialog, confirmCloseListener);
    }
    else {
      popupMenus.forEach(popupMenu -> popupMenu.setVisible(false));
    }
  }

  static <T extends Component> List<T> componentsOfType(Container container, Class<T> clazz) {
    List<T> components = new ArrayList<>();
    for (Component component : container.getComponents()) {
      if (clazz.isAssignableFrom(component.getClass())) {
        components.add((T) component);
      }
      if (component instanceof Container) {
        components.addAll(componentsOfType((Container) component, clazz));
      }
    }

    return components;
  }
}
