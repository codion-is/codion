/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.valuemap;

import org.jminor.common.model.DocumentAdapter;
import org.jminor.common.model.Item;
import org.jminor.common.model.combobox.ItemComboBoxModel;
import org.jminor.common.model.valuemap.ValueChangeMapEditModel;
import org.jminor.common.ui.control.LinkType;

import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

/**
 * A class for linking a ComboBox to a ValueChangeMapEditModel property value.
 */
public class ComboBoxValueLink<K> extends AbstractValueMapLink<K, Object> {

  /**
   * The linked ComboBoxModel
   */
  private final ComboBoxModel boxModel;

  /**
   * Instantiate a new ComboBoxValueLink
   * @param comboBox the combo box to link
   * @param editModel the ValueChangeMapEditModel instance
   * @param property the property to link to
   */
  public ComboBoxValueLink(final JComboBox comboBox, final ValueChangeMapEditModel<K, Object> editModel,
                           final K property) {
    this(comboBox, editModel, property, LinkType.READ_WRITE, false);
  }

  /**
   * Instantiate a new ComboBoxValueLink
   * @param comboBox the combo box to link
   * @param editModel the ValueChangeMapEditModel instance
   * @param property the property to link to
   * @param linkType the link type
   * @param isString true if the underlying value is string based
   */
  public ComboBoxValueLink(final JComboBox comboBox, final ValueChangeMapEditModel<K, Object> editModel,
                           final K property, final LinkType linkType, final boolean isString) {
    super(editModel, property, linkType);
    this.boxModel = comboBox.getModel();
    updateUI();
    comboBox.addItemListener(new ItemListener() {
      /** {@inheritDoc} */
      public void itemStateChanged(final ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED) {
          updateModel();
        }
      }
    });
    //this allows editable string based combo boxes to post their edits after each keystroke
    if (comboBox.isEditable() && isString) {
      ((JTextField)comboBox.getEditor().getEditorComponent()).getDocument().addDocumentListener(new DocumentAdapter() {
        /** {@inheritDoc} */
        @Override
        public void insertOrRemoveUpdate(final DocumentEvent e) {
          boxModel.setSelectedItem(comboBox.getEditor().getItem());
        }
      });
    }
  }

  /** {@inheritDoc} */
  @Override
  protected final Object getUIValue() {
    if (boxModel instanceof ItemComboBoxModel) {
      return ((Item) boxModel.getSelectedItem()).getItem();
    }

    return boxModel.getSelectedItem();
  }

  /** {@inheritDoc} */
  @Override
  protected final void setUIValue(final Object value) {
    boxModel.setSelectedItem(value);
  }

  /**
   * @return the underlying combo box model
   */
  protected final ComboBoxModel getModel() {
    return boxModel;
  }
}
