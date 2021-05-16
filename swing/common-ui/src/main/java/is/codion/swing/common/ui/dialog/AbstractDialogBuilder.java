/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.dialog;

import is.codion.swing.common.ui.Windows;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import java.awt.Window;

abstract class AbstractDialogBuilder<T> {

  protected Window owner;
  protected String title;
  protected ImageIcon icon;

  public final T owner(final Window owner) {
    this.owner = owner;
    return (T) this;
  }

  public final T dialogParent(final JComponent dialogParent) {
    if (owner != null) {
      throw new IllegalStateException("owner has alrady been set");
    }
    this.owner = dialogParent == null ? null : Windows.getParentWindow(dialogParent);
    return (T) this;
  }

  public final T title(final String title) {
    this.title = title;
    return (T) this;
  }

  public final T icon(final ImageIcon icon) {
    this.icon = icon;
    return (T) this;
  }
}
