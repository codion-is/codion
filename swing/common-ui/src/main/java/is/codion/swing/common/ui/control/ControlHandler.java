/*
 * Copyright (c) 2021 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.control;

import javax.swing.Action;
import java.util.function.Consumer;

abstract class ControlHandler implements Consumer<Action> {

  @Override
  public final void accept(Action action) {
    if (action == null) {
      onSeparator();
    }
    else if (action instanceof Controls) {
      onControls((Controls) action);
    }
    else if (action instanceof Control) {
      onControl((Control) action);
    }
    else {
      onAction(action);
    }
  }

  abstract void onSeparator();

  abstract void onControl(Control control);

  abstract void onControls(Controls controls);

  abstract void onAction(Action action);
}
