/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.dialog;

import is.codion.swing.common.ui.Windows;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import java.awt.Window;

/**
 * A base class for Dialog builders.
 * @param <B> the builder type
 */
public class AbstractDialogBuilder<B extends DialogBuilder<B>> implements DialogBuilder<B> {

  protected Window owner;
  protected String title;
  protected ImageIcon icon;

  @Override
  public final B owner(Window owner) {
    this.owner = owner;
    return (B) this;
  }

  @Override
  public final B owner(JComponent owner) {
    if (this.owner != null) {
      throw new IllegalStateException("owner has alrady been set");
    }
    this.owner = owner == null ? null : Windows.getParentWindow(owner).orElse(null);
    return (B) this;
  }

  @Override
  public final B title(String title) {
    this.title = title;
    return (B) this;
  }

  @Override
  public final B icon(ImageIcon icon) {
    this.icon = icon;
    return (B) this;
  }
}
