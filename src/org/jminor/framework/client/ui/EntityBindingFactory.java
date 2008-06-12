/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui;

import org.jminor.common.model.State;
import org.jminor.common.model.formats.AbstractDateMaskFormat;
import org.jminor.common.ui.UiUtil;
import org.jminor.common.ui.combobox.MaximumMatch;
import org.jminor.common.ui.combobox.SteppedComboBox;
import org.jminor.common.ui.control.LinkType;
import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.client.model.EntityTableModel;
import org.jminor.framework.model.EntityRepository;
import org.jminor.framework.model.Property;

import javax.swing.ComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.FlowLayout;
import java.awt.GridLayout;

public abstract class EntityBindingFactory extends JPanel {

  /**
   * @return Value for property 'model'.
   */
  public abstract EntityModel getModel();

  public JPanel getControlPanel(final String propertyID, final JComponent inputComponent) {
    return getControlPanel(propertyID, inputComponent, true);
  }

  public JPanel getControlPanel(final String propertyID, final JComponent inputComponent,
                                final boolean labelOnTop) {
    return getControlPanel(propertyID, inputComponent, labelOnTop, 0, 0);
  }

  public JPanel getControlPanel(final String propertyID, final JComponent inputComponent,
                                final boolean labelOnTop, final int hgap, final int vgap) {
    return getControlPanel(propertyID, inputComponent, labelOnTop, hgap, vgap, JLabel.LEADING);
  }

  public JPanel getControlPanel(final String propertyID, final JComponent inputComponent,
                                final boolean labelOnTop, final int hgap, final int vgap, final int labelAlignment) {
    return getControlPanel(createLabel(propertyID, labelAlignment), inputComponent, labelOnTop, hgap, vgap);
  }

  public JPanel getControlPanel(final JLabel label, final JComponent inputComponent,
                                final boolean labelOnTop, final int hgap, final int vgap) {
    final JPanel ret = new JPanel(labelOnTop ?
            new GridLayout(2, 1, hgap, vgap) : new FlowLayout(FlowLayout.LEADING, hgap, vgap));
    ret.add(label);
    ret.add(inputComponent);

    return ret;
  }

  protected final JTextArea createTextArea(final String propertyID) {
    return createTextArea(propertyID, -1, -1);
  }

  protected final JTextArea createTextArea(final String propertyID, final int rows, final int columns) {
    return FrameworkUiUtil.createTextArea(EntityRepository.get().getProperty(getModel().getEntityID(), propertyID),
            getModel(), rows, columns);
  }

  protected final UiUtil.DateInputPanel createDateFieldPanel(final String propertyID,
                                                             final AbstractDateMaskFormat dateMaskFormat) {
    return createDateFieldPanel(propertyID, dateMaskFormat, true);
  }

  protected final UiUtil.DateInputPanel createDateFieldPanel(final String propertyID,
                                                             final AbstractDateMaskFormat dateMaskFormat,
                                                             final boolean includeButton) {
    return createDateFieldPanel(propertyID, dateMaskFormat, includeButton, null);
  }

  protected final UiUtil.DateInputPanel createDateFieldPanel(final String propertyID,
                                                             final AbstractDateMaskFormat dateMaskFormat,
                                                             final boolean includeButton,
                                                             final State enabledState) {
    return createDateFieldPanel(propertyID, dateMaskFormat, includeButton, enabledState, LinkType.READ_WRITE);
  }

  protected final UiUtil.DateInputPanel createDateFieldPanel(final String propertyID,
                                                             final AbstractDateMaskFormat dateMaskFormat,
                                                             final boolean includeButton,
                                                             final State enabledState,
                                                             final LinkType linkType) {
    return createDateFieldPanel(EntityRepository.get().getProperty(getModel().getEntityID(), propertyID),
            dateMaskFormat, includeButton, enabledState, getModel(), linkType);
  }

  protected final UiUtil.DateInputPanel createDateFieldPanel(final Property property,
                                                             final AbstractDateMaskFormat dateMaskFormat) {
    return createDateFieldPanel(property, dateMaskFormat, true);
  }

  protected final UiUtil.DateInputPanel createDateFieldPanel(final Property property,
                                                             final AbstractDateMaskFormat dateMaskFormat,
                                                             final boolean includeButton) {
    return createDateFieldPanel(property, dateMaskFormat, includeButton, null);
  }

  protected final UiUtil.DateInputPanel createDateFieldPanel(final Property property,
                                                             final AbstractDateMaskFormat dateMaskFormat,
                                                             final boolean includeButton,
                                                             final State enabledState) {
    return createDateFieldPanel(property, dateMaskFormat, includeButton, enabledState, getModel());
  }

  protected final UiUtil.DateInputPanel createDateFieldPanel(final Property property,
                                                             final AbstractDateMaskFormat dateMaskFormat,
                                                             final boolean includeButton,
                                                             final State enabledState,
                                                             final EntityModel entityModel) {
    return createDateFieldPanel(property, dateMaskFormat, includeButton, enabledState,
            entityModel, LinkType.READ_WRITE);
  }

  protected final UiUtil.DateInputPanel createDateFieldPanel(final Property property,
                                                             final AbstractDateMaskFormat dateMaskFormat,
                                                             final boolean includeButton,
                                                             final State enabledState,
                                                             final EntityModel entityModel,
                                                             final LinkType linkType) {
    return FrameworkUiUtil.createDateFieldPanel(property, entityModel, dateMaskFormat,
            linkType, includeButton, enabledState);
  }

  protected final JTextField createTextField(final String propertyID) {
    return createTextField(propertyID, LinkType.READ_WRITE);
  }

  protected final JTextField createTextField(final String propertyID, final LinkType linkType) {
    return createTextField(propertyID, linkType, true);
  }

  protected final JTextField createTextField(final String propertyID, final LinkType linkType,
                                             final boolean immediateUpdate) {
    return createTextField(propertyID, linkType, immediateUpdate, null);
  }

  protected final JTextField createTextField(final String propertyID, final LinkType linkType,
                                             final boolean immediateUpdate, final String formatString) {
    return createTextField(propertyID, linkType, immediateUpdate, formatString, null);
  }

  protected final JTextField createTextField(final String propertyID, final LinkType linkType,
                                             final boolean immediateUpdate, final String formatString,
                                             final State enabledState) {
    return createTextField(propertyID, linkType, immediateUpdate, formatString, enabledState, false);
  }

  protected final JTextField createTextField(final String propertyID, final LinkType linkType,
                                             final boolean immediateUpdate, final String formatString,
                                             final State enabledState, final boolean valueIncludesLiteralCharacters) {
    return createTextField(EntityRepository.get().getProperty(getModel().getEntityID(), propertyID),
            linkType, formatString, immediateUpdate, enabledState, valueIncludesLiteralCharacters);
  }

  protected final JTextField createTextField(final Property property) {
    return createTextField(property, LinkType.READ_WRITE);
  }

  protected final JTextField createTextField(final Property property, final LinkType linkType) {
    return createTextField(property, linkType, null, true);
  }

  protected final JTextField createTextField(final Property property, final LinkType linkType,
                                             final String formatString, final boolean immediateUpdate) {
    return createTextField(property, linkType, formatString, immediateUpdate, null);
  }

  protected final JTextField createTextField(final Property property, final LinkType linkType,
                                             final String formatString, final boolean immediateUpdate,
                                             final State enabledState) {
    return createTextField(property, linkType, formatString, immediateUpdate, enabledState, false);
  }

  protected final JTextField createTextField(final Property property, final LinkType linkType,
                                             final String formatString, final boolean immediateUpdate,
                                             final State enabledState, final boolean valueContainsLiteralCharacters) {
    return FrameworkUiUtil.createTextField(property, getModel(), linkType, formatString, immediateUpdate,
            null, enabledState, valueContainsLiteralCharacters);
  }

  protected final JCheckBox createCheckBox(final String propertyID) {
    return createCheckBox(EntityRepository.get().getProperty(getModel().getEntityID(), propertyID));
  }

  protected final JCheckBox createCheckBox(final String propertyID, final State enabledState) {
    return createCheckBox(propertyID, enabledState, true);
  }

  protected final JCheckBox createCheckBox(final String propertyID, final State enabledState,
                                           final boolean includeCaption) {
    return createCheckBox(EntityRepository.get().getProperty(getModel().getEntityID(), propertyID), enabledState, includeCaption);
  }

  protected final JCheckBox createCheckBox(final Property property) {
    return createCheckBox(property, null);
  }

  protected final JCheckBox createCheckBox(final Property property, final State enabledState) {
    return createCheckBox(property, enabledState, true);
  }

  protected final JCheckBox createCheckBox(final Property property, final State enabledState,
                                           final boolean includeCaption) {
    return FrameworkUiUtil.createCheckBox(property, getModel(), enabledState, includeCaption);
  }

  protected final JComboBox createBooleanComboBox(final String propertyID) {
    return createBooleanComboBox(propertyID, null);
  }

  protected final JComboBox createBooleanComboBox(final String propertyID, final State enabledState) {
    return createBooleanComboBox(EntityRepository.get().getProperty(getModel().getEntityID(), propertyID),enabledState);
  }

  protected final JComboBox createBooleanComboBox(final Property property) {
    return createBooleanComboBox(property, null);
  }

  protected final JComboBox createBooleanComboBox(final Property property, final State enabledState) {
    return FrameworkUiUtil.createBooleanComboBox(property, getModel(), enabledState);
  }

  protected final SteppedComboBox createComboBox(final String propertyID, final ComboBoxModel comboBoxModel,
                                                 final boolean maximumMatch) {
    return createComboBox(propertyID, comboBoxModel, maximumMatch, null);
  }

  protected final SteppedComboBox createComboBox(final String propertyID, final ComboBoxModel comboBoxModel,
                                                 final boolean maximumMatch, final State enabledState) {
    return createComboBox(EntityRepository.get().getProperty(getModel().getEntityID(), propertyID),
            comboBoxModel, maximumMatch, enabledState);
  }

  protected final SteppedComboBox createComboBox(final Property property, final ComboBoxModel comboBoxModel,
                                                 final boolean maximumMatch) {
    return createComboBox(property, comboBoxModel, maximumMatch, null);
  }

  protected final SteppedComboBox createComboBox(final Property property, final ComboBoxModel comboBoxModel,
                                                 final boolean maximumMatch, final State enabledState) {
    final SteppedComboBox ret = FrameworkUiUtil.createComboBox(property, getModel(), comboBoxModel, enabledState);
    if (maximumMatch)
      MaximumMatch.enable(ret);

    return ret;
  }

  protected final SteppedComboBox createEditableComboBox(final String propertyID, final ComboBoxModel comboBoxModel) {
    return createEditableComboBox(propertyID, comboBoxModel, null);
  }

  protected final SteppedComboBox createEditableComboBox(final String propertyID, final ComboBoxModel comboBoxModel,
                                                         final State enabledState) {
    return createEditableComboBox(EntityRepository.get().getProperty(getModel().getEntityID(), propertyID),
            comboBoxModel, enabledState);
  }

  protected final SteppedComboBox createEditableComboBox(final Property property, final ComboBoxModel comboBoxModel,
                                                         final State enabledState) {
    return FrameworkUiUtil.createComboBox(property, getModel(), comboBoxModel, enabledState, true);
  }

  protected final SteppedComboBox createPropertyComboBox(final String propertyID) {
    return createPropertyComboBox(propertyID, null);
  }

  protected final SteppedComboBox createPropertyComboBox(final String propertyID, final State state) {
    return createPropertyComboBox(propertyID, state, null);
  }

  protected final SteppedComboBox createPropertyComboBox(final String propertyID, final State state,
                                                         final Object nullValue) {
    return createPropertyComboBox(propertyID, state, nullValue, false);
  }

  protected final SteppedComboBox createPropertyComboBox(final String propertyID, final State state,
                                                         final Object nullValue, final boolean editable) {
    return createPropertyComboBox(EntityRepository.get().getProperty(getModel().getEntityID(), propertyID),
            state, nullValue, editable);
  }

  protected final SteppedComboBox createPropertyComboBox(final Property property) {
    return createPropertyComboBox(property, null);
  }

  protected final SteppedComboBox createPropertyComboBox(final Property property, final State state) {
    return createPropertyComboBox(property, state, null);
  }

  protected final SteppedComboBox createPropertyComboBox(final Property property, final State state,
                                                         final Object nullValue) {
    return createPropertyComboBox(property, state, nullValue, false);
  }

  protected final SteppedComboBox createPropertyComboBox(final Property property, final State state,
                                                         final Object nullValue, final boolean editable) {
    return FrameworkUiUtil.createPropertyComboBox(property, getModel(), null, state, nullValue, editable);
  }

  protected final EntityComboBox createEntityComboBox(final String propertyID) {
    return createEntityComboBox(propertyID, null);
  }

  protected final EntityComboBox createEntityComboBox(final String propertyID, final State enabledState) {
    return createEntityComboBox((Property.EntityProperty)
            EntityRepository.get().getProperty(getModel().getEntityID(), propertyID), enabledState);
  }

  protected final JTextField createEntityField(final String propertyID) {
    return createEntityField(EntityRepository.get().getProperty(getModel().getEntityID(), propertyID));
  }

  protected final JPanel createEntityFieldPanel(final String propertyID, final EntityTableModel lookupModel) {
    return createEntityFieldPanel((Property.EntityProperty)
            EntityRepository.get().getProperty(getModel().getEntityID(), propertyID), lookupModel);
  }

  protected final EntitySearchField createEntitySearchField(final String propertyID, final String entityID, final String searchPropertyID) {
    return createEntitySearchField(EntityRepository.get().getProperty(getModel().getEntityID(), propertyID), entityID, searchPropertyID);
  }

  protected final EntitySearchField createEntitySearchField(final Property property, final String entityID, final String searchPropertyID) {
    return FrameworkUiUtil.createEntitySearchField(property, getModel(), entityID, searchPropertyID);
  }

  protected final EntityComboBox createEntityComboBox(final Property.EntityProperty property) {
    return createEntityComboBox(property, null);
  }

  protected final EntityComboBox createEntityComboBox(final Property.EntityProperty property, final State enabledState) {
    return createEntityComboBox(property, null, false, enabledState);
  }

  protected final EntityComboBox createEntityComboBox(final String propertyID,
                                                      final EntityPanel.EntityPanelInfo appInfo,
                                                      final boolean newButtonFocusable) {
    return createEntityComboBox((Property.EntityProperty) EntityRepository.get().getProperty(getModel().getEntityID(),
            propertyID), appInfo, newButtonFocusable, null);
  }

  protected final EntityComboBox createEntityComboBox(final Property.EntityProperty property,
                                                      final EntityPanel.EntityPanelInfo appInfo,
                                                      final boolean newButtonFocusable, final State enabledState) {
    return FrameworkUiUtil.createEntityComboBox(property, getModel(), appInfo, newButtonFocusable, enabledState);
  }

  protected final JTextField createEntityField(final Property property) {
    return FrameworkUiUtil.createEntityField(property, getModel());
  }

  protected final JPanel createEntityFieldPanel(final Property.EntityProperty property, final EntityTableModel lookupModel) {
    return FrameworkUiUtil.createEntityFieldPanel(property, getModel(), lookupModel);
  }

  protected final JLabel createLabel(final String propertyID) {
    return createLabel(propertyID, JLabel.LEFT);
  }

  protected final JLabel createLabel(final String propertyID, final int horizontalAlignment) {
    final String text = EntityRepository.get().getProperty(getModel().getEntityID(), propertyID).getCaption();
    if (text == null || text.length() == 0)
      throw new IllegalArgumentException("Cannot create a label for property: " + propertyID + ", no caption");

    return new JLabel(text, horizontalAlignment);
  }
}
