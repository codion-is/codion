/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 *
 */
package org.jminor.framework.client.ui.property;

import org.jminor.common.model.State;
import org.jminor.common.model.combobox.ItemComboBoxModel;
import org.jminor.common.ui.control.LinkType;
import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.client.model.combobox.EntityComboBoxModel;
import org.jminor.framework.model.Entity;
import org.jminor.framework.model.Property;

import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

public class ComboBoxPropertyLink extends AbstractEntityPropertyLink {

  private final ComboBoxModel boxModel;

  public ComboBoxPropertyLink(final EntityModel entityModel, final String propertyName, final String caption,
                              final JComboBox comboBox) {
    this(entityModel, Entity.repository.getProperty(entityModel.getEntityID(), propertyName), caption, comboBox);
  }

  public ComboBoxPropertyLink(final EntityModel entityModel, final Property property, final String caption,
                              final JComboBox comboBox) {
    this(entityModel, property, caption, comboBox, LinkType.READ_WRITE, null);
  }

  public ComboBoxPropertyLink(final EntityModel entityModel, final Property property, final String caption,
                              final JComboBox comboBox, final LinkType linkType, final State enabledState) {
    super(entityModel, property, caption, linkType, enabledState);
    this.boxModel = comboBox.getModel();
    updateUI();
    comboBox.addItemListener(new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED)
          refreshProperty();
      }
    });
    //this allows editable combo boxes to post their edits after each keystroke
    if (comboBox.isEditable()) {
      ((JTextField)comboBox.getEditor().getEditorComponent()).getDocument().addDocumentListener(new DocumentListener() {
        public void changedUpdate(DocumentEvent e) {
          boxModel.setSelectedItem(comboBox.getEditor().getItem());
        }
        public void insertUpdate(DocumentEvent e) {
          boxModel.setSelectedItem(comboBox.getEditor().getItem());
        }
        public void removeUpdate(DocumentEvent e) {
          boxModel.setSelectedItem(comboBox.getEditor().getItem());
        }
      });
    }
  }

  /** {@inheritDoc} */
  protected void updateProperty() {
    if (boxModel instanceof EntityComboBoxModel)
      setPropertyValue(((EntityComboBoxModel) boxModel).getSelectedEntity());
    else if (boxModel instanceof ItemComboBoxModel)
      setPropertyValue(((ItemComboBoxModel.Item) boxModel.getSelectedItem()).getItem());
    else {
      Object selectedItem = boxModel.getSelectedItem();
      if (selectedItem instanceof String && ((String) selectedItem).length() == 0)
        selectedItem = null;

      setPropertyValue(selectedItem);
    }
  }

  /** {@inheritDoc} */
  protected void updateUI() {
    boxModel.setSelectedItem(getPropertyValue());
  }
}
