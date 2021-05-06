/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.dialog;

import javax.swing.AbstractAction;
import javax.swing.JDialog;
import javax.swing.JPopupMenu;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.util.Arrays;
import java.util.List;

import static java.util.stream.Collectors.toList;

final class DisposeDialogOnEscapeAction extends AbstractAction {

  private final JDialog dialog;

  DisposeDialogOnEscapeAction(final JDialog dialog) {
    super("Dialogs.disposeDialogOnEscapeAction");
    this.dialog = dialog;
  }

  @Override
  public void actionPerformed(final ActionEvent e) {
    final List<Window> heavyWeightWindows = Arrays.stream(dialog.getOwnedWindows()).filter(window ->
            window.getClass().getName().endsWith("Popup$HeavyWeightWindow") && window.isVisible()).collect(toList());
    if (!heavyWeightWindows.isEmpty()) {
      heavyWeightWindows.forEach(Window::dispose);

      return;
    }
    final List<JPopupMenu> popupMenus = DefaultDialogBuilder.getComponentsOfType(dialog.getContentPane(), JPopupMenu.class);
    if (popupMenus.isEmpty()) {
      dialog.dispose();
    }
    else {
      popupMenus.forEach(popupMenu -> popupMenu.setVisible(false));
    }
  }
}
