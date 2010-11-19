/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.valuemap;

import org.jminor.common.model.valuemap.ValueChangeMapEditModel;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * A UI class based on the ValueChangeMapEditModel.<br>
 */
public abstract class ValueChangeMapEditPanel<K, V> extends JPanel {

  private final ValueChangeMapEditModel<K, V> model;

  private final Map<K, JComponent> components = new HashMap<K, JComponent>();

  /**
   * The component that should receive focus when the UI is prepared for a new record
   */
  private JComponent initialFocusComponent;

  private K initialFocusComponentKey;

  /**
   * Instantiates a new ValueChangeMapEditPanel based on the given model.
   * @param model the model
   */
  public ValueChangeMapEditPanel(final ValueChangeMapEditModel<K, V> model) {
    this.model = model;
  }

  /**
   * @return the edit model this panel is based on
   */
  public final ValueChangeMapEditModel<K, V> getEditModel() {
    return model;
  }

  /**
   * Prepares the UI.
   * @param setInitialFocus if true then the initial focus is set
   * @param clearUI if true the UI is cleared.
   * @see org.jminor.common.model.valuemap.ValueChangeMapEditModel#clear()
   */
  public final void prepareUI(final boolean setInitialFocus, final boolean clearUI) {
    if (clearUI) {
      clearModelValues();
    }
    if (setInitialFocus && isVisible()) {
      setInitialFocus();
    }
  }

  /**
   * Clears the values from the underlying model
   */
  public final void clearModelValues() {
    model.setValueMap(null);
  }

  /**
   * Sets the component that should receive the focus when the UI is cleared or activated.
   * Overrides the value set via setInitialFocusComponentKey()
   * @param initialFocusComponent the component
   * @return the component
   * @see #prepareUI(boolean, boolean)
   */
  public final JComponent setInitialFocusComponent(final JComponent initialFocusComponent) {
    this.initialFocusComponent = initialFocusComponent;
    return initialFocusComponent;
  }

  /**
   * @return the key of the component to receive initial the focus
   */
  public final K getInitialFocusComponentKey() {
    return initialFocusComponentKey;
  }

  /**
   * Defines the component associated with the given key as the component
   * that should recieve the initial focus in this edit panel.
   * This is overridden by setInitialFocusComponent().
   * @param initialFocusComponentKey the component key
   * @see #setInitialFocusComponent(javax.swing.JComponent)
   */
  public final void setInitialFocusComponentKey(final K initialFocusComponentKey) {
    this.initialFocusComponentKey = initialFocusComponentKey;
  }

  /**
   * Sets the initial focus
   */
  public final void setInitialFocus() {
    final JComponent focusComponent = getInitialFocusComponent();
    if (focusComponent == null) {
      requestFocus();//InWindow();
    }
    else {
      focusComponent.requestFocus();//InWindow();
    }
  }

  /**
   * @return the keys that have been associated with components.
   */
  public final Collection<K> getComponentKeys() {
    return new ArrayList<K>(components.keySet());
  }

  /**
   * @param key the key
   * @return the component associated with the given key, null if no component has been
   * associated with the given key
   */
  public final JComponent getComponent(final K key) {
    return components.get(key);
  }

  /**
   * @param component the component
   * @return the key the given component is associated with, null if the component has not been
   * associated with a key
   */
  public final K getComponentKey(final JComponent component) {
    for (final Map.Entry<K, JComponent> entry : components.entrySet()) {
      if (entry.getValue().equals(component)) {
        return entry.getKey();
      }
    }

    return null;
  }

  /**
   * @param key the key of the component to select
   */
  public final void selectComponent(final K key) {
    if (components.containsKey(key)) {
      components.get(key).requestFocus();
    }
  }

  /**
   * @return the component that should get the initial focus
   */
  protected JComponent getInitialFocusComponent() {
    if (initialFocusComponent != null) {
      return initialFocusComponent;
    }

    if (initialFocusComponentKey != null) {
      return components.get(initialFocusComponentKey);
    }

    return null;
  }

  /**
   * Associates the given input component with the given property,
   * preferably this should be called for components associated with
   * key values.
   * @param key the propertyID
   * @param component the input component
   */
  protected final void setComponent(final K key, final JComponent component) {
    if (components.containsKey(key)) {
      throw new IllegalStateException("Component already set for key: " + key);
    }
    components.put(key, component);
  }
}
