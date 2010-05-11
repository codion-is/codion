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

  private final Map<K, JComponent> controls = new HashMap<K, JComponent>();

  /**
   * The component that should receive focus when the UI is prepared for a new record
   */
  private JComponent defaultFocusComponent;

  public ValueChangeMapEditPanel(final ValueChangeMapEditModel<K, V> model) {
    this.model = model;
  }

  public ValueChangeMapEditModel getEditModel() {
    return model;
  }

  public void prepareUI(final boolean requestDefaultFocus, final boolean clearUI) {
    if (clearUI)
      getEditModel().clear();
    if (requestDefaultFocus && isVisible())
      setDefaultFocus();
  }

  /**
   * Sets the component that should receive the focus when the UI is initialized after
   * a new record has been inserted or the panel is activated
   * @param defaultFocusComponent the component
   * @return the component
   */
  public JComponent setDefaultFocusComponent(final JComponent defaultFocusComponent) {
    return this.defaultFocusComponent = defaultFocusComponent;
  }

  protected JComponent getDefaultFocusComponent() {
    return defaultFocusComponent;
  }

  public void setDefaultFocus() {
    final JComponent defaultFocusComponent = getDefaultFocusComponent();
    if (defaultFocusComponent == null)
      requestFocusInWindow();
    else
      defaultFocusComponent.requestFocusInWindow();
  }

  /**
   * @return the keys that have been associated with controls.
   */
  public Collection<K> getControlKeys() {
    return new ArrayList<K>(controls.keySet());
  }

  public void selectControl(final K key) {
    if (controls.containsKey(key))
      controls.get(key).requestFocusInWindow();
  }

  /**
   * Associates the given input component with the given property, this should
   * be called for all controls
   * @param key the propertyID
   * @param component the input control
   */
  protected void setControl(final K key, final JComponent component) {
    if (controls.containsKey(key))
      throw new RuntimeException("Control already set for key: " + key);
    controls.put(key, component);
  }
}
