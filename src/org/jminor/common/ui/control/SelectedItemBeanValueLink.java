/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.control;

import org.jminor.common.model.DocumentAdapter;
import org.jminor.common.model.Item;
import org.jminor.common.model.combobox.FilteredComboBoxModel;
import org.jminor.common.model.combobox.ItemComboBoxModel;

import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

/**
 * Binds a JComboBox to an Object based bean property.
 */
final class SelectedItemBeanValueLink extends AbstractValueLink {

  private final ComboBoxModel comboBoxModel;

  /**
   * Instantiates a new SelectedItemBeanValueLink.
   * @param box the combo box to link with the value
   * @param modelValue the model value
   * @param linkType the link type
   */
  SelectedItemBeanValueLink(final JComboBox box, final ModelValue modelValue, final LinkType linkType) {
    super(modelValue, linkType);
    this.comboBoxModel = box.getModel();
    updateUI();
    box.addItemListener(new ItemListener() {
      /** {@inheritDoc} */
      @Override
      public void itemStateChanged(final ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED) {
          updateModel();
        }
      }
    });
    //this allows editable string based combo boxes to post their edits after each keystroke
    if (box.isEditable()) {
      ((JTextField) box.getEditor().getEditorComponent()).getDocument().addDocumentListener(new DocumentAdapter() {
        /** {@inheritDoc} */
        @Override
        public void contentsChanged(final DocumentEvent e) {
          comboBoxModel.setSelectedItem(box.getEditor().getItem());
        }
      });
    }
  }

  /** {@inheritDoc} */
  @Override
  protected Object getUIValue() {
    if (comboBoxModel instanceof ItemComboBoxModel) {
      return ((Item) comboBoxModel.getSelectedItem()).getItem();
    }
    else if (comboBoxModel instanceof FilteredComboBoxModel) {
      return ((FilteredComboBoxModel) comboBoxModel).getSelectedValue();
    }

    return comboBoxModel.getSelectedItem();
  }

  /** {@inheritDoc} */
  @Override
  protected void setUIValue(final Object value) {
    comboBoxModel.setSelectedItem(value);
  }
}
