/*
 * Copyright (c) 2023 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.button;

import is.codion.common.value.Value;
import is.codion.swing.common.ui.component.value.ComponentValue;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.ToggleControl;

import javax.swing.JMenuItem;
import javax.swing.SwingConstants;

import static java.util.Objects.requireNonNull;

abstract class AbstractToggleMenuItemBuilder<C extends JMenuItem, B extends ToggleMenuItemBuilder<C, B>> extends AbstractButtonBuilder<Boolean, C, B>
        implements ToggleMenuItemBuilder<C, B> {

  private ToggleControl toggleControl;
  private PersistMenu persistMenu = PERSIST_MENU.get();

  AbstractToggleMenuItemBuilder(Value<Boolean> linkedValue) {
    super(linkedValue);
    horizontalAlignment(SwingConstants.LEADING);
  }

  @Override
  public final B toggleControl(ToggleControl toggleControl) {
    if (requireNonNull(toggleControl).value().nullable()) {
      throw new IllegalArgumentException("A toggle menu item does not support a nullable value");
    }
    this.toggleControl = toggleControl;
    action(toggleControl);
    return (B) this;
  }

  @Override
  public final B toggleControl(Control.Builder<ToggleControl, ?> toggleControlBuilder) {
    return toggleControl(requireNonNull(toggleControlBuilder).build());
  }

  @Override
  public final B persistMenu(PersistMenu persistMenu) {
    this.persistMenu = requireNonNull(persistMenu);
    return (B) this;
  }

  protected abstract JMenuItem createMenuItem(PersistMenu persistMenu);

  @Override
  protected final C createButton() {
    JMenuItem menuItem = createMenuItem(persistMenu);
    if (toggleControl != null) {
      menuItem.setModel(createButtonModel(toggleControl));
    }

    return (C) menuItem;
  }

  @Override
  protected final ComponentValue<Boolean, C> createComponentValue(C component) {
    return new BooleanToggleButtonValue<>(component);
  }

  @Override
  protected final void setInitialValue(C component, Boolean initialValue) {
    component.setSelected(initialValue);
  }

  @Override
  protected final boolean supportsNull() {
    return false;
  }
}
