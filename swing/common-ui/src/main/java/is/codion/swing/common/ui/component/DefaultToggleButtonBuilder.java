/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.swing.common.ui.value.ComponentValue;
import is.codion.swing.common.ui.value.ComponentValues;

import javax.swing.JToggleButton;

class DefaultToggleButtonBuilder extends AbstractComponentBuilder<Boolean, JToggleButton, ToggleButtonBuilder> implements ToggleButtonBuilder {

  private String caption;
  private boolean includeCaption = true;

  @Override
  public ToggleButtonBuilder caption(final String caption) {
    this.caption = caption;
    return this;
  }

  @Override
  public ToggleButtonBuilder includeCaption(final boolean includeCaption) {
    this.includeCaption = includeCaption;
    return this;
  }

  @Override
  protected JToggleButton buildComponent() {
    return includeCaption ? new JToggleButton(caption) : new JToggleButton();
  }

  @Override
  protected ComponentValue<Boolean, JToggleButton> buildComponentValue(final JToggleButton component) {
    return ComponentValues.toggleButton(component);
  }

  @Override
  protected void setInitialValue(final JToggleButton component, final Boolean initialValue) {
    component.setSelected(initialValue);
  }
}
