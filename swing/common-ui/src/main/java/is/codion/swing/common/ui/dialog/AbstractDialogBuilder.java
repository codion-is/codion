/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.dialog;

import is.codion.swing.common.ui.Windows;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import java.awt.Window;

class AbstractDialogBuilder<B extends DialogBuilder<B>> implements DialogBuilder<B> {

  protected Window owner;
  protected String title;
  protected ImageIcon icon;

  public final B owner(final Window owner) {
    this.owner = owner;
    return (B) this;
  }

  public final B owner(final JComponent owner) {
    if (this.owner != null) {
      throw new IllegalStateException("owner has alrady been set");
    }
    this.owner = owner == null ? null : Windows.getParentWindow(owner);
    return (B) this;
  }

  public final B title(final String title) {
    this.title = title;
    return (B) this;
  }

  public final B icon(final ImageIcon icon) {
    this.icon = icon;
    return (B) this;
  }
}
