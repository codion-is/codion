/*
 * Copyright (c) 2008, Bj�rn Darri Sigur�sson. All Rights Reserved.
 */
package org.jminor.framework.client.ui.property;

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

/**
 * A class for linking a ComboBox to a EntityModel property value
 */
public class ComboBoxPropertyLink extends AbstractEntityPropertyLink {

  /**
   * The linked ComboBoxModel
   */
  private final ComboBoxModel boxModel;

  /**
   * Instantiate a new ComboBoxPropertyLink
   * @param entityModel the EntityModel instance
   * @param propertyID the ID of the property to link to
   * @param comboBox the combo box to link
   */
  public ComboBoxPropertyLink(final EntityModel entityModel, final String propertyID, final JComboBox comboBox) {
    this(entityModel, EntityRepository.get().getProperty(entityModel.getEntityID(), propertyID), comboBox);
  }

  /**
   * Instantiate a new ComboBoxPropertyLink
   * @param entityModel the EntityModel instance
   * @param property the property to link to
   * @param comboBox the combo box to link
   */
  public ComboBoxPropertyLink(final EntityModel entityModel, final Property property, final JComboBox comboBox) {
    this(entityModel, property, comboBox, LinkType.READ_WRITE);
  }

  /**
   * Instantiate a new ComboBoxPropertyLink
   * @param entityModel the EntityModel instance
   * @param property the property to link to
   * @param comboBox the combo box to link
   * @param linkType the link type
   */
  public ComboBoxPropertyLink(final EntityModel entityModel, final Property property, final JComboBox comboBox,
                              final LinkType linkType) {
    super(entityModel, property, linkType);
    this.boxModel = comboBox.getModel();
    updateUI();
    comboBox.addItemListener(new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED)
          updateModel();
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
  protected Object getUIPropertyValue() {
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
  protected void setUIPropertyValue(final Object propertyValue) {
    boxModel.setSelectedItem(propertyValue);
  }
}
