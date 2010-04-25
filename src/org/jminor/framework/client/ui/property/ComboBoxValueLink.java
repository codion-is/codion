/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui.property;

import org.jminor.common.model.combobox.ItemComboBoxModel;
import org.jminor.common.model.valuemap.ChangeValueMapEditModel;
import org.jminor.common.ui.control.LinkType;
import org.jminor.common.ui.valuemap.AbstractValueMapLink;
import org.jminor.framework.client.model.EntityComboBoxModel;
import org.jminor.framework.client.model.PropertyComboBoxModel;
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
 * A class for linking a ComboBox to a EntityEditModel property value.
 */
public class ComboBoxValueLink extends AbstractValueMapLink<String, Object> {

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
  public ComboBoxValueLink(final JComboBox comboBox, final ChangeValueMapEditModel<String, Object> editModel,
                           final Property property) {
    this(comboBox, editModel, property, LinkType.READ_WRITE);
  }

  /**
   * Instantiate a new ComboBoxPropertyLink
   * @param comboBox the combo box to link
   * @param editModel the EntityEditModel instance
   * @param property the property to link to
   * @param linkType the link type
   */
  public ComboBoxValueLink(final JComboBox comboBox, final ChangeValueMapEditModel<String, Object> editModel,
                           final Property property, final LinkType linkType) {
    super(editModel, property.getPropertyID(), linkType);
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
        public void insertUpdate(DocumentEvent e) {
          boxModel.setSelectedItem(comboBox.getEditor().getItem());
        }
        public void removeUpdate(DocumentEvent e) {
          boxModel.setSelectedItem(comboBox.getEditor().getItem());
        }
        public void changedUpdate(DocumentEvent e) {}
      });
    }
  }

  /** {@inheritDoc} */
  @Override
  protected Object getUIValue() {
    Object value;
    if (boxModel instanceof EntityComboBoxModel)
      value = ((EntityComboBoxModel) boxModel).getSelectedEntity();
    else if (boxModel instanceof ItemComboBoxModel)
      value = ((ItemComboBoxModel.Item) boxModel.getSelectedItem()).getItem();
    else if (boxModel instanceof PropertyComboBoxModel)
      value = ((PropertyComboBoxModel) boxModel).isNullValueItemSelected() ? null : boxModel.getSelectedItem();
    else
      value = boxModel.getSelectedItem();

    return value;
  }

  /** {@inheritDoc} */
  @Override
  protected void setUIValue(final Object propertyValue) {
    boxModel.setSelectedItem(propertyValue);
  }
}
