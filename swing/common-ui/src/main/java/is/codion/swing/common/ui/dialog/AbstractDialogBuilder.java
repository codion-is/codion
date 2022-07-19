/*
 * Copyright (c) 2021 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.dialog;

import is.codion.common.value.Value;
import is.codion.common.value.ValueObserver;
import is.codion.swing.common.ui.Windows;

import javax.swing.ImageIcon;
import java.awt.Component;
import java.awt.Window;

/**
 * A base class for Dialog builders.
 * @param <B> the builder type
 */
public class AbstractDialogBuilder<B extends DialogBuilder<B>> implements DialogBuilder<B> {

  protected Window owner;
  protected Component locationRelativeTo;
  protected ValueObserver<String> titleProvider;
  protected ImageIcon icon;

  @Override
  public final B owner(Window owner) {
    this.owner = owner;
    return (B) this;
  }

  @Override
  public final B owner(Component owner) {
    if (this.owner != null) {
      throw new IllegalStateException("owner has alrady been set");
    }
    this.owner = owner == null ? null : Windows.getParentWindow(owner).orElse(null);
    if (locationRelativeTo == null) {
      locationRelativeTo = owner;
    }
    return (B) this;
  }

  @Override
  public final B locationRelativeTo(Component locationRelativeTo) {
    this.locationRelativeTo = locationRelativeTo;
    return (B) this;
  }

  @Override
  public final B title(String title) {
    return titleProvider(Value.value(title));
  }

  @Override
  public final B titleProvider(ValueObserver<String> titleProvider) {
    this.titleProvider = titleProvider;
    return (B) this;
  }

  @Override
  public final B icon(ImageIcon icon) {
    this.icon = icon;
    return (B) this;
  }
}
