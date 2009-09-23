/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui.property;

import org.jminor.common.model.combobox.ItemComboBoxModel;
import org.jminor.common.ui.control.LinkType;
import org.jminor.framework.client.model.EntityEditModel;
import org.jminor.framework.client.model.combobox.EntityComboBoxModel;
import org.jminor.framework.client.model.combobox.PropertyComboBoxModel;
import org.jminor.framework.domain.Property;
import org.jminor.framework.domain.Type;

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
   * @param comboBox the combo box to link
   * @param editModel the EntityEditModel instance
   * @param property the property to link to
   */
  public ComboBoxPropertyLink(final JComboBox comboBox, final EntityEditModel editModel, final Property property) {
    this(comboBox, editModel, property, LinkType.READ_WRITE);
  }

  /**
   * Instantiate a new ComboBoxPropertyLink
   * @param comboBox the combo box to link
   * @param editModel the EntityEditModel instance
   * @param property the property to link to
   * @param linkType the link type
   */
  public ComboBoxPropertyLink(final JComboBox comboBox, final EntityEditModel editModel, final Property property,
                              final LinkType linkType) {
    super(editModel, property, linkType);
    this.boxModel = comboBox.getModel();
    updateUI();
    comboBox.addItemListener(new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED)
          updateModel();
      }
    });
    //this allows editable string based combo boxes to post their edits after each keystroke
    if (comboBox.isEditable() && property.getPropertyType() == Type.STRING) {
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
  @Override
  protected Object getUIPropertyValue() {
    Object ret;
    if (boxModel instanceof EntityComboBoxModel)
      ret = ((EntityComboBoxModel) boxModel).getSelectedEntity();
    else if (boxModel instanceof ItemComboBoxModel)
      ret = ((ItemComboBoxModel.Item) boxModel.getSelectedItem()).getItem();
    else if (boxModel instanceof PropertyComboBoxModel)
      ret = ((PropertyComboBoxModel) boxModel).isNullValueItemSelected() ? null : boxModel.getSelectedItem();
    else
      ret = boxModel.getSelectedItem();

    return ret;
  }

  /** {@inheritDoc} */
  @Override
  protected void setUIPropertyValue(final Object propertyValue) {
    boxModel.setSelectedItem(propertyValue);
  }
}
