/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.control;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.JToolBar;
import java.awt.Component;
import java.awt.Dimension;

final class ToolBarControlHandler extends ControlHandler {

  private final JToolBar toolbar;

  ToolBarControlHandler(final JToolBar owner) {
    this.toolbar = owner;
  }

  @Override
  public void onSeparator() {
    toolbar.addSeparator();
  }

  @Override
  public void onControl(final Control control) {
    if (control instanceof ToggleControl) {
      squareSize((AbstractButton) toolbar.add(((ToggleControl) control).createToggleButton()));
    }
    else {
      squareSize(toolbar.add(control));
    }
  }

  @Override
  public void onControls(final Controls controls) {
    controls.getActions().forEach(new ToolBarControlHandler(toolbar));
  }

  @Override
  public void onAction(final Action action) {
    toolbar.add(action);
  }

  private static void squareSize(final Component component) {
    final Dimension preferredSize = component.getPreferredSize();
    final int minimumDimension = Math.min(preferredSize.height, preferredSize.width);
    component.setPreferredSize(new Dimension(minimumDimension, minimumDimension));
  }
}
