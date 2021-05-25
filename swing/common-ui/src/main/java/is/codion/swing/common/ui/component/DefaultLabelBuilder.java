/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.swing.common.ui.value.AbstractComponentValue;
import is.codion.swing.common.ui.value.ComponentValue;

import javax.swing.JLabel;

class DefaultLabelBuilder extends AbstractComponentBuilder<String, JLabel, LabelBuilder> implements LabelBuilder {

  private String text;

  @Override
  public LabelBuilder text(final String text) {
    this.text = text;
    return this;
  }

  @Override
  protected JLabel buildComponent() {
    return new JLabel(text);
  }

  @Override
  protected ComponentValue<String, JLabel> buildComponentValue(final JLabel component) {
    return new AbstractComponentValue<String, JLabel>(component) {
      @Override
      protected String getComponentValue(final JLabel component) {
        return null;
      }

      @Override
      protected void setComponentValue(final JLabel component, final String value) {
        component.setText(value);
      }
    };
  }

  @Override
  protected void setInitialValue(final JLabel component, final String initialValue) {
    component.setText(initialValue);
  }
}
