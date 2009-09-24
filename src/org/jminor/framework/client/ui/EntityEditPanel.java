package org.jminor.framework.client.ui;

import org.jminor.framework.client.model.EntityEditModel;

import javax.swing.JComponent;

public abstract class EntityEditPanel extends EntityBindingPanel {

  private final EntityEditModel model;

  /**
   * The component that should receive focus when the UI is prepared for a new record
   */
  private JComponent defaultFocusComponent;

  public EntityEditPanel(final EntityEditModel editModel) {
    this.model = editModel;
    initializeUI();
    bindEvents();
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

  /**
   * @return the component which should receive focus after a record has been inserted
   * or the panel is activated
   */
  public JComponent getDefaultFocusComponent() {
    return defaultFocusComponent;
  }

  @Override
  public EntityEditModel getEditModel() {
    return model;
  }

  protected abstract void initializeUI();

  protected void bindEvents(){}
}
