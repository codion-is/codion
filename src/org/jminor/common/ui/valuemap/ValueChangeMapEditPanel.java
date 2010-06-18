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
 * User: Björn Darri<br>
 * Date: 25.4.2010<br>
 * Time: 12:29:37<br>
 */
public abstract class ValueChangeMapEditPanel<K, V> extends JPanel {

  private final ValueChangeMapEditModel<K, V> model;

  private final Map<K, JComponent> components = new HashMap<K, JComponent>();

  /**
   * The component that should receive focus when the UI is prepared for a new record
   */
  private JComponent initialFocusComponent;

  private K initialFocusComponentKey;

  public ValueChangeMapEditPanel(final ValueChangeMapEditModel<K, V> model) {
    this.model = model;
  }

  public ValueChangeMapEditModel<K, V> getEditModel() {
    return model;
  }

  public void prepareUI(final boolean setInitialFocus, final boolean clearUI) {
    if (clearUI) {
      getEditModel().setValueMap(null);
    }
    if (setInitialFocus && isVisible()) {
      setInitialFocus();
    }
  }

  /**
   * Sets the component that should receive the focus when the UI is cleared or activated.
   * Overrides the value set via setInitialFocusComponentKey()
   * @param initialFocusComponent the component
   * @return the component
   * @see #prepareUI(boolean, boolean)
   */
  public JComponent setInitialFocusComponent(final JComponent initialFocusComponent) {
    this.initialFocusComponent = initialFocusComponent;
    return initialFocusComponent;
  }

  public K getInitialFocusComponentKey() {
    return initialFocusComponentKey;
  }

  /**
   * Defines the component associated with the given key as the component
   * that should recieve the initial focus in this edit panel.
   * This is overridden by setInitialFocusComponent().
   * @param initialFocusComponentKey the component key
   * @see #setInitialFocusComponent(javax.swing.JComponent)
   */
  public void setInitialFocusComponentKey(final K initialFocusComponentKey) {
    this.initialFocusComponentKey = initialFocusComponentKey;
  }

  public void setInitialFocus() {
    final JComponent initialFocusComponent = getInitialFocusComponent();
    if (initialFocusComponent == null) {
      requestFocusInWindow();
    }
    else {
      initialFocusComponent.requestFocusInWindow();
    }
  }

  /**
   * @return the keys that have been associated with components.
   */
  public Collection<K> getComponentKeys() {
    return new ArrayList<K>(components.keySet());
  }

  public JComponent getComponent(final K key) {
    if (!components.containsKey(key)) {
      throw new RuntimeException("No component associated with key: " + key);
    }

    return components.get(key);
  }

  public void selectComponent(final K key) {
    if (components.containsKey(key)) {
      components.get(key).requestFocusInWindow();
    }
  }

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
  protected void setComponent(final K key, final JComponent component) {
    if (components.containsKey(key)) {
      throw new RuntimeException("Component already set for key: " + key);
    }
    components.put(key, component);
  }
}
