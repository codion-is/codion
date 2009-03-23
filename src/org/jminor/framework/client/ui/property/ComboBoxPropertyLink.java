/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui.property;

import org.jminor.common.model.State;
import org.jminor.common.model.combobox.ItemComboBoxModel;
import org.jminor.common.ui.control.LinkType;
import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.client.model.combobox.EntityComboBoxModel;
import org.jminor.framework.model.EntityRepository;
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

  public ComboBoxPropertyLink(final EntityModel entityModel, final String propertyID, final JComboBox comboBox) {
    this(entityModel, EntityRepository.get().getProperty(entityModel.getEntityID(), propertyID), comboBox);
  }

  public ComboBoxPropertyLink(final EntityModel entityModel, final Property property, final JComboBox comboBox) {
    this(entityModel, property, comboBox, LinkType.READ_WRITE, null);
  }

  public ComboBoxPropertyLink(final EntityModel entityModel, final Property property, final JComboBox comboBox,
                              final LinkType linkType, final State enabledState) {
    super(entityModel, property, linkType, enabledState);
    this.boxModel = comboBox.getModel();
    refreshUI();
    comboBox.addItemListener(new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED)
          refreshProperty();
      }
    });
    //this allows editable combo boxes to post their edits after each keystroke
    if (comboBox.isEditable()) {//todo works only for string properties
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
  protected Object getUiPropertyValue() {
    Object ret;
    if (boxModel instanceof EntityComboBoxModel)
      ret = ((EntityComboBoxModel) boxModel).getSelectedEntity();
    else if (boxModel instanceof ItemComboBoxModel)
      ret = ((ItemComboBoxModel.Item) boxModel.getSelectedItem()).getItem();
    else {
      ret = boxModel.getSelectedItem();
      if (ret instanceof String && ((String) ret).length() == 0)
        ret = null;
    }

    return ret;
  }

  /** {@inheritDoc} */
  protected void setUiPropertyValue(final Object propertyValue) {
    boxModel.setSelectedItem(propertyValue);
  }
}
