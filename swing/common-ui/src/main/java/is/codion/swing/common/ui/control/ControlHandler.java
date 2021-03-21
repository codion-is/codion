/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.control;

import javax.swing.Action;
import java.util.function.Consumer;

abstract class ControlHandler implements Consumer<Action> {

  @Override
  public final void accept(final Action action) {
    if (action == null) {
      onSeparator();
    }
    else if (action instanceof ControlList) {
      onControlList((ControlList) action);
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

  abstract void onControlList(ControlList controls);

  abstract void onAction(Action action);
}
