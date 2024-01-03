/*
 * Copyright (c) 2021 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui.component;

import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.swing.common.ui.component.value.ComponentValue;
import is.codion.swing.framework.model.SwingEntityEditModel;

import javax.swing.JComponent;

/**
 * A factory for {@link ComponentValue} implementations.
 * @param <T> the value type
 * @param <A> the attribute type
 * @param <C> the component type
 */
public interface EntityComponentFactory<T, A extends Attribute<T>, C extends JComponent> {

  /**
   * Provides value input components for multiple entity update, override to supply
   * specific {@link ComponentValue} implementations for attributes.
   * @param attribute the attribute for which to get the ComponentValue
   * @param editModel the edit model used to create foreign key input models
   * @param initialValue the initial value to display
   * @return a new ComponentValue instance handling input for {@code attribute}
   */
  ComponentValue<T, C> componentValue(A attribute, SwingEntityEditModel editModel, T initialValue);
}
