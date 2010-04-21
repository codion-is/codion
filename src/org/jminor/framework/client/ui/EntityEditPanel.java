/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui;

import org.jminor.common.model.State;
import org.jminor.common.ui.DateInputPanel;
import org.jminor.common.ui.TextInputPanel;
import org.jminor.common.ui.combobox.MaximumMatch;
import org.jminor.common.ui.combobox.SteppedComboBox;
import org.jminor.common.ui.control.LinkType;
import org.jminor.common.ui.layout.FlexibleGridLayout;
import org.jminor.framework.Configuration;
import org.jminor.framework.client.model.EntityEditModel;
import org.jminor.framework.client.model.EntityTableModel;
import org.jminor.framework.domain.EntityRepository;
import org.jminor.framework.domain.Property;

import javax.swing.ComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.FlowLayout;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

/**
 * A UI component based on the EntityEditModel.
 * @see EntityEditModel
 */
public abstract class EntityEditPanel extends JPanel {

  private final EntityEditModel model;
  private final Map<String, JComponent> controls = new HashMap<String, JComponent>();

  /**
   * The component that should receive focus when the UI is prepared for a new record
   */
  private JComponent defaultFocusComponent;

  /**
   * Instantiates a new EntityEditPanel based on the provided EntityEditModel
   * @param editModel the EntityEditModel instance to base this EntityEditPanel on
   */
  public EntityEditPanel(final EntityEditModel editModel) {
    this.model = editModel;
    initializeUI();
    bindEvents();
  }

  /**
   * @return the EntityEditModel instance to bind controls to
   */
  public EntityEditModel getEditModel() {
    return model;
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

  public void setDefaultFocus() {
    final JComponent defaultFocusComponent = getDefaultFocusComponent();
    if (defaultFocusComponent == null)
      requestFocusInWindow();
    else
      defaultFocusComponent.requestFocusInWindow();
  }

  public void prepareUI(final boolean requestDefaultFocus, final boolean clearUI) {
    if (clearUI)
      getEditModel().clear();
    if (requestDefaultFocus && isVisible())
      setDefaultFocus();
  }

  public void selectControl(final Property property) {
    if (controls.containsKey(property.getPropertyID()))
      controls.get(property.getPropertyID()).requestFocusInWindow();
  }

  /**
   * Associates the given input component with the given property, this should
   * be called for all controls
   * @param propertyID the propertyID
   * @param component the input control
   */
  protected void setControl(final String propertyID, final JComponent component) {
    if (controls.containsKey(propertyID))
      throw new RuntimeException("Control already set for property: " + propertyID);
    controls.put(propertyID, component);
  }

  /**
   * Initializes this EntityEditPanel UI
   */
  protected abstract void initializeUI();

  /**
   * Performs event binding, for overriding
   */
  protected void bindEvents() {}

  protected JComponent getDefaultFocusComponent() {
    return defaultFocusComponent;
  }

  /**
   * Creates a panel containing a label and the given component.
   * The label text is the caption of the property identified by <code>propertyID</code>.
   * The default layout of the resulting panel is with the label on top and <code>inputComponent</code> below.
   * @param propertyID the id of the property from which to retrieve the label caption
   * @param inputComponent a component bound to the property with id <code>propertyID</code>
   * @return a panel containing a label and a component
   */
  protected final JPanel createPropertyPanel(final String propertyID, final JComponent inputComponent) {
    return createPropertyPanel(propertyID, inputComponent, true);
  }

  /**
   * Creates a panel containing a label and the given component.
   * The label text is the caption of the property identified by <code>propertyID</code>.
   * @param propertyID the id of the property from which to retrieve the label caption
   * @param inputComponent a component bound to the property with id <code>propertyID</code>
   * @param labelOnTop if true then the label is positioned above <code>inputComponent</code>,
   * otherwise it uses FlowLayout.LEADING in a FlowLayout.
   * @return a panel containing a label and a component
   */
  protected final JPanel createPropertyPanel(final String propertyID, final JComponent inputComponent,
                                             final boolean labelOnTop) {
    return createPropertyPanel(propertyID, inputComponent, labelOnTop, 5, 5);
  }

  /**
   * Creates a panel containing a label and the given component.
   * The label text is the caption of the property identified by <code>propertyID</code>.
   * @param propertyID the id of the property from which to retrieve the label caption
   * @param inputComponent a component bound to the property with id <code>propertyID</code>
   * @param labelOnTop if true then the label is positioned above <code>inputComponent</code>,
   * otherwise it uses FlowLayout.LEADING in a FlowLayout.
   * @param hgap the horizontal gap between components
   * @param vgap the vertical gap between components
   * @return a panel containing a label and a component
   */
  protected final JPanel createPropertyPanel(final String propertyID, final JComponent inputComponent,
                                             final boolean labelOnTop, final int hgap, final int vgap) {
    return createPropertyPanel(propertyID, inputComponent, labelOnTop, hgap, vgap, JLabel.LEADING);
  }

  /**
   * Creates a panel containing a label and the given component.
   * The label text is the caption of the property identified by <code>propertyID</code>.
   * @param propertyID the id of the property from which to retrieve the label caption
   * @param inputComponent a component bound to the property with id <code>propertyID</code>
   * @param labelOnTop if true then the label is positioned above <code>inputComponent</code>,
   * otherwise it uses FlowLayout.LEADING in a FlowLayout.
   * @param hgap the horizontal gap between components
   * @param vgap the vertical gap between components
   * @param labelAlignment the text alignment to use for the label
   * @return a panel containing a label and a component
   */
  protected final JPanel createPropertyPanel(final String propertyID, final JComponent inputComponent,
                                             final boolean labelOnTop, final int hgap, final int vgap,
                                             final int labelAlignment) {
    return createPropertyPanel(EntityUiUtil.createLabel(EntityRepository.getProperty(getEditModel().getEntityID(),
            propertyID), labelAlignment), inputComponent, labelOnTop, hgap, vgap);
  }

  /**
   * Creates a panel containing a label component and the <code>inputComponent</code>.
   * @param labelComponent the label component
   * @param inputComponent a component bound to the property with id <code>propertyID</code>
   * @param labelOnTop if true then the label is positioned above <code>inputComponent</code>,
   * otherwise it uses FlowLayout.LEADING in a FlowLayout.
   * @param hgap the horizontal gap between components
   * @param vgap the vertical gap between components
   * @return a panel containing a label and a component
   */
  protected final JPanel createPropertyPanel(final JComponent labelComponent, final JComponent inputComponent,
                                             final boolean labelOnTop, final int hgap, final int vgap) {
    final JPanel panel = new JPanel(labelOnTop ?
            new FlexibleGridLayout(2, 1, hgap, vgap) : new FlowLayout(FlowLayout.LEADING, hgap, vgap));
    if (labelComponent instanceof JLabel)
      ((JLabel) labelComponent).setLabelFor(inputComponent);
    panel.add(labelComponent);
    panel.add(inputComponent);

    return panel;
  }

  /**
   * Creates a JTextArea component bound to the property identified by <code>propertyID </code>.
   * @param propertyID the ID of the property to bind
   * @return a JTextArea bound to the property
   */
  protected final JTextArea createTextArea(final String propertyID) {
    return createTextArea(propertyID, -1, -1);
  }

  /**
   * Creates a JTextArea component bound to the property identified by <code>propertyID </code>.
   * @param propertyID the ID of the property to bind
   * @param rows the number of rows in the text area
   * @param columns the number of columns in the text area
   * @return a JTextArea bound to the property
   */
  protected final JTextArea createTextArea(final String propertyID, final int rows, final int columns) {
    final JTextArea textArea = EntityUiUtil.createTextArea(EntityRepository.getProperty(getEditModel().getEntityID(), propertyID),
            getEditModel(), rows, columns);
    setControl(propertyID, textArea);

    return textArea;
  }

  /**
   * Creates a TextInputPanel bound to the property identified by <code>propertyID</code>.
   * @param propertyID the ID of the property to bind
   * @return a TextInputPanel bound to the property
   */
  protected final TextInputPanel createTextInputPanel(final String propertyID) {
    return createTextInputPanel(propertyID, LinkType.READ_WRITE);
  }

  /**
   * Creates a TextInputPanel bound to the property identified by <code>propertyID</code>.
   * @param propertyID the ID of the property to bind
   * @param linkType the property link type
   * @return a TextInputPanel bound to the property
   */
  protected final TextInputPanel createTextInputPanel(final String propertyID, final LinkType linkType) {
    return createTextInputPanel(propertyID, linkType, true);
  }

  /**
   * Creates a TextInputPanel bound to the property identified by <code>propertyID</code>.
   * @param propertyID the ID of the property to bind
   * @param linkType the property LinkType
   * @param immediateUpdate if true then the underlying property value is updated on each keystroke,
   * otherwise it is updated when the component looses focus.
   * @return a TextInputPanel bound to the property
   */
  protected final TextInputPanel createTextInputPanel(final String propertyID, final LinkType linkType,
                                                      final boolean immediateUpdate) {
    return createTextInputPanel(propertyID, linkType, immediateUpdate, true);
  }

  /**
   * Creates a TextInputPanel bound to the property identified by <code>propertyID</code>.
   * @param propertyID the ID of the property to bind
   * @param linkType the property link type
   * @param immediateUpdate if true then the underlying property value is updated on each keystroke,
   * otherwise it is updated when the component looses focus.
   * @param buttonFocusable specifies whether the edit button should be focusable.
   * @return a TextInputPanel bound to the property
   */
  protected final TextInputPanel createTextInputPanel(final String propertyID, final LinkType linkType,
                                                      final boolean immediateUpdate, final boolean buttonFocusable) {
    return createTextInputPanel(EntityRepository.getProperty(getEditModel().getEntityID(), propertyID), linkType,
            immediateUpdate, buttonFocusable);
  }

  /**
   * Creates a TextInputPanel bound to the property identified by <code>propertyID</code>.
   * @param property the property to bind
   * @param linkType the property link type
   * @param immediateUpdate if true then the underlying property value is updated on each keystroke,
   * otherwise it is updated when the component looses focus.
   * @return a TextInputPanel bound to the property
   */
  protected final TextInputPanel createTextInputPanel(final Property property, final LinkType linkType,
                                                      final boolean immediateUpdate) {
    return createTextInputPanel(property, linkType, immediateUpdate, true);
  }

  /**
   * Creates a TextInputPanel bound to the property identified by <code>propertyID</code>.
   * @param property the property to bind
   * @param linkType the property link type
   * @param immediateUpdate if true then the underlying property value is updated on each keystroke,
   * otherwise it is updated when the component looses focus.
   * @param buttonFocusable specifies whether the edit button should be focusable.
   * @return a TextInputPanel bound to the property
   */
  protected final TextInputPanel createTextInputPanel(final Property property, final LinkType linkType,
                                                      final boolean immediateUpdate, final boolean buttonFocusable) {
    final TextInputPanel ret = new TextInputPanel(createTextField(property, linkType, null, immediateUpdate),
            property.getCaption(), null, buttonFocusable);
    ret.setMaxLength(property.getMaxLength());

    return ret;
  }

  /**
   * Creates a new DateInputPanel using the default short date format, bound to the property
   * identified by <code>propertyID</code>.
   * @param propertyID the ID of the property for which to create the panel
   * @return a DateInputPanel using the default short date format
   * @see org.jminor.framework.Configuration#DEFAULT_DATE_FORMAT
   */
  protected final DateInputPanel createDateInputPanel(final String propertyID) {
    return createDateInputPanel(propertyID, true);
  }

  /**
   * Creates a new DateInputPanel bound to the property identified by <code>propertyID</code>.
   * @param propertyID the ID of the property for which to create the panel
   * @param dateFormat the format to use for masking the input field
   * @return a DateInputPanel bound to the property
   */
  protected final DateInputPanel createDateInputPanel(final String propertyID, final SimpleDateFormat dateFormat) {
    return createDateInputPanel(propertyID, dateFormat, true);
  }

  /**
   * Creates a new DateInputPanel bound to the property identified by <code>propertyID</code>.
   * @param propertyID the ID of the property for which to create the panel
   * @param includeButton if true a button for visually editing the date is included
   * @return a DateInputPanel using the default short date format
   * @see org.jminor.framework.Configuration#DEFAULT_DATE_FORMAT
   */
  protected final DateInputPanel createDateInputPanel(final String propertyID, final boolean includeButton) {
    return createDateInputPanel(propertyID, Configuration.getDefaultDateFormat(), includeButton, null);
  }

  /**
   * Creates a new DateInputPanel bound to the property identified by <code>propertyID</code>.
   * @param propertyID the ID of the property for which to create the panel
   * @param dateFormat the format to use for masking the input field
   * @param includeButton if true a button for visually editing the date is included
   * @return a DateInputPanel bound to the property
   */
  protected final DateInputPanel createDateInputPanel(final String propertyID, final SimpleDateFormat dateFormat,
                                                      final boolean includeButton) {
    return createDateInputPanel(propertyID, dateFormat, includeButton, null);
  }

  /**
   * Creates a new DateInputPanel bound to the property identified by <code>propertyID</code>.
   * @param propertyID the ID of the property for which to create the panel
   * @param dateFormat the format to use for masking the input field
   * @param includeButton if true a button for visually editing the date is included
   * @param enabledState a state for controlling the enabled state of the input component
   * @return a DateInputPanel bound to the property
   */
  protected final DateInputPanel createDateInputPanel(final String propertyID, final SimpleDateFormat dateFormat,
                                                      final boolean includeButton, final State enabledState) {
    return createDateInputPanel(propertyID, dateFormat, includeButton, enabledState, LinkType.READ_WRITE);
  }

  /**
   * Creates a new DateInputPanel bound to the property identified by <code>propertyID</code>.
   * @param propertyID the ID of the property for which to create the panel
   * @param dateFormat the format to use for masking the input field
   * @param includeButton if true a button for visually editing the date is included
   * @param enabledState a state for controlling the enabled state of the input component
   * @param linkType the property link type
   * @return a DateInputPanel bound to the property
   */
  protected final DateInputPanel createDateInputPanel(final String propertyID, final SimpleDateFormat dateFormat,
                                                      final boolean includeButton, final State enabledState,
                                                      final LinkType linkType) {
    return createDateInputPanel(EntityRepository.getProperty(getEditModel().getEntityID(), propertyID),
            dateFormat, includeButton, enabledState, linkType);
  }

  /**
   * Creates a new DateInputPanel bound to the property identified by <code>propertyID</code>.
   * @param property the property for which to create the panel
   * @param dateFormat the format to use for masking the input field
   * @return a DateInputPanel bound to the property
   */
  protected final DateInputPanel createDateInputPanel(final Property property, final SimpleDateFormat dateFormat) {
    return createDateInputPanel(property, dateFormat, true);
  }

  /**
   * Creates a new DateInputPanel bound to the property identified by <code>propertyID</code>.
   * @param property the property for which to create the panel
   * @param dateFormat the format to use for masking the input field
   * @param includeButton if true a button for visually editing the date is included
   * @return a DateInputPanel bound to the property
   */
  protected final DateInputPanel createDateInputPanel(final Property property, final SimpleDateFormat dateFormat,
                                                      final boolean includeButton) {
    return createDateInputPanel(property, dateFormat, includeButton, null);
  }

  /**
   * Creates a new DateInputPanel bound to the property identified by <code>propertyID</code>.
   * @param property the property for which to create the panel
   * @param dateFormat the format to use for masking the input field
   * @param includeButton if true a button for visually editing the date is included
   * @param enabledState a state for controlling the enabled state of the input component
   * @return a DateInputPanel bound to the property
   */
  protected final DateInputPanel createDateInputPanel(final Property property, final SimpleDateFormat dateFormat,
                                                      final boolean includeButton, final State enabledState) {
    return createDateInputPanel(property, dateFormat, includeButton, enabledState, LinkType.READ_WRITE);
  }

  /**
   * Creates a new DateInputPanel bound to the property identified by <code>propertyID</code>.
   * @param property the property for which to create the panel
   * @param dateFormat the format to use for masking the input field
   * @param includeButton if true a button for visually editing the date is included
   * @param enabledState a state for controlling the enabled state of the input component
   * @param linkType the property link type
   * @return a DateInputPanel bound to the property
   */
  protected final DateInputPanel createDateInputPanel(final Property property, final SimpleDateFormat dateFormat,
                                                      final boolean includeButton, final State enabledState,
                                                      final LinkType linkType) {
    final DateInputPanel panel = EntityUiUtil.createDateInputPanel(property, getEditModel(), dateFormat, linkType, includeButton, enabledState);
    setControl(property.getPropertyID(), panel.getInputField());

    return panel;
  }

  /**
   * Creates a JTextField bound to the property identified by <code>propertyID</code>
   * @param propertyID the ID of the property to bind
   * @return a text field bound to the property
   */
  protected final JTextField createTextField(final String propertyID) {
    return createTextField(propertyID, LinkType.READ_WRITE);
  }

  /**
   * Creates a JTextField bound to the property identified by <code>propertyID</code>
   * @param propertyID the ID of the property to bind
   * @param linkType the property link type
   * @return a text field bound to the property
   */
  protected final JTextField createTextField(final String propertyID, final LinkType linkType) {
    return createTextField(propertyID, linkType, true);
  }

  /**
   * Creates a JTextField bound to the property identified by <code>propertyID</code>
   * @param propertyID the ID of the property to bind
   * @param linkType the property link type
   * @param immediateUpdate if true then the underlying property value is updated on each keystroke,
   * otherwise it is updated when the component looses focus.
   * @return a text field bound to the property
   */
  protected final JTextField createTextField(final String propertyID, final LinkType linkType,
                                             final boolean immediateUpdate) {
    return createTextField(propertyID, linkType, immediateUpdate, null);
  }

  /**
   * Creates a JTextField bound to the property identified by <code>propertyID</code>
   * @param propertyID the ID of the property to bind
   * @param linkType the property link type
   * @param immediateUpdate if true then the underlying property value is updated on each keystroke,
   * otherwise it is updated when the component looses focus.
   * @param maskString if specified then a JFormattedTextField with the given mask is returned
   * @return a text field bound to the property
   */
  protected final JTextField createTextField(final String propertyID, final LinkType linkType,
                                             final boolean immediateUpdate, final String maskString) {
    return createTextField(propertyID, linkType, immediateUpdate, maskString, null);
  }

  /**
   * Creates a JTextField bound to the property identified by <code>propertyID</code>
   * @param propertyID the ID of the property to bind
   * @param linkType the property link type
   * @param immediateUpdate if true then the underlying property value is updated on each keystroke,
   * otherwise it is updated when the component looses focus.
   * @param maskString if specified then a JFormattedTextField with the given mask is returned
   * @param enabledState a state for controlling the enabled state of the component
   * @return a text field bound to the property
   */
  protected final JTextField createTextField(final String propertyID, final LinkType linkType,
                                             final boolean immediateUpdate, final String maskString,
                                             final State enabledState) {
    return createTextField(propertyID, linkType, immediateUpdate, maskString, enabledState, false);
  }

  /**
   * Creates a JTextField bound to the property identified by <code>propertyID</code>
   * @param propertyID the ID of the property to bind
   * @param linkType the property link type
   * @param immediateUpdate if true then the underlying property value is updated on each keystroke,
   * otherwise it is updated when the component looses focus.
   * @param maskString if specified then a JFormattedTextField with the given mask is returned
   * @param enabledState a state for controlling the enabled state of the component
   * @param valueIncludesLiteralCharacters only applicable if <code>maskString</code> is specified
   * @return a text field bound to the property
   */
  protected final JTextField createTextField(final String propertyID, final LinkType linkType,
                                             final boolean immediateUpdate, final String maskString,
                                             final State enabledState, final boolean valueIncludesLiteralCharacters) {
    return createTextField(EntityRepository.getProperty(getEditModel().getEntityID(), propertyID),
            linkType, maskString, immediateUpdate, enabledState, valueIncludesLiteralCharacters);
  }

  /**
   * Creates a JTextField bound to the given property
   * @param property the property to bind
   * @return a text field bound to the property
   */
  protected final JTextField createTextField(final Property property) {
    return createTextField(property, LinkType.READ_WRITE);
  }

  /**
   * Creates a JTextField bound to the given property
   * @param property the property to bind
   * @param linkType the property link type
   * @return a text field bound to the property
   */
  protected final JTextField createTextField(final Property property, final LinkType linkType) {
    return createTextField(property, linkType, null, true);
  }

  /**
   * Creates a JTextField bound to the given property
   * @param property the property to bind
   * @param linkType the property link type
   * @param immediateUpdate if true then the underlying property value is updated on each keystroke,
   * otherwise it is updated when the component looses focus.
   * @param maskString if specified then a JFormattedTextField with the given mask is returned
   * @return a text field bound to the property
   */
  protected final JTextField createTextField(final Property property, final LinkType linkType,
                                             final String maskString, final boolean immediateUpdate) {
    return createTextField(property, linkType, maskString, immediateUpdate, null);
  }

  /**
   * Creates a JTextField bound to the given property
   * @param property the ID of the property to bind
   * @param linkType the property link type
   * @param immediateUpdate if true then the underlying property value is updated on each keystroke,
   * otherwise it is updated when the component looses focus.
   * @param maskString if specified then a JFormattedTextField with the given mask is returned
   * @param enabledState a state for controlling the enabled state of the component
   * @return a text field bound to the property
   */
  protected final JTextField createTextField(final Property property, final LinkType linkType,
                                             final String maskString, final boolean immediateUpdate,
                                             final State enabledState) {
    return createTextField(property, linkType, maskString, immediateUpdate, enabledState, false);
  }

  /**
   * Creates a JTextField bound to the given property
   * @param property the property to bind
   * @param linkType the property link type
   * @param immediateUpdate if true then the underlying property value is updated on each keystroke,
   * otherwise it is updated when the component looses focus.
   * @param maskString if specified then a JFormattedTextField with the given mask is returned
   * @param enabledState a state for controlling the enabled state of the component
   * @param valueIncludesLiteralCharacters only applicable if <code>maskString</code> is specified
   * @return a text field bound to the property
   */
  protected final JTextField createTextField(final Property property, final LinkType linkType,
                                             final String maskString, final boolean immediateUpdate,
                                             final State enabledState, final boolean valueIncludesLiteralCharacters) {
    final JTextField txt = EntityUiUtil.createTextField(property, getEditModel(), linkType, maskString, immediateUpdate,
            null, enabledState, valueIncludesLiteralCharacters);
    setControl(property.getPropertyID(), txt);

    return txt;
  }

  /**
   * Creates a JCheckBox bound to the property identified by <code>propertyID</code>
   * @param propertyID the ID of the property to bind
   * @return a JCheckBox bound to the property
   */
  protected final JCheckBox createCheckBox(final String propertyID) {
    return createCheckBox(propertyID, null);
  }

  /**
   * Creates a JCheckBox bound to the property identified by <code>propertyID</code>
   * @param propertyID the ID of the property to bind
   * @param enabledState a state for controlling the enabled state of the component
   * @return a JCheckBox bound to the property
   */
  protected final JCheckBox createCheckBox(final String propertyID, final State enabledState) {
    return createCheckBox(propertyID, enabledState, true);
  }

  /**
   * Creates a JCheckBox bound to the property identified by <code>propertyID</code>
   * @param propertyID the ID of the property to bind
   * @param enabledState a state for controlling the enabled state of the component
   * @param includeCaption specifies whether or not the caption should be included
   * @return a JCheckBox bound to the property
   */
  protected final JCheckBox createCheckBox(final String propertyID, final State enabledState,
                                           final boolean includeCaption) {
    return createCheckBox(EntityRepository.getProperty(getEditModel().getEntityID(), propertyID), enabledState, includeCaption);
  }

  /**
   * Creates a JCheckBox bound to the given property
   * @param property the property to bind
   * @return a JCheckBox bound to the property
   */
  protected final JCheckBox createCheckBox(final Property property) {
    return createCheckBox(property, null);
  }

  /**
   * Creates a JCheckBox bound to the given property
   * @param property the property to bind
   * @param enabledState a state for controlling the enabled state of the component
   * @return a JCheckBox bound to the property
   */
  protected final JCheckBox createCheckBox(final Property property, final State enabledState) {
    return createCheckBox(property, enabledState, true);
  }

  /**
   * Creates a JCheckBox bound to the given property
   * @param property the property to bind
   * @param enabledState a state for controlling the enabled state of the component
   * @param includeCaption specifies whether or not the caption should be included
   * @return a JCheckBox bound to the property
   */
  protected final JCheckBox createCheckBox(final Property property, final State enabledState,
                                           final boolean includeCaption) {
    final JCheckBox box = EntityUiUtil.createCheckBox(property, getEditModel(), enabledState, includeCaption);
    setControl(property.getPropertyID(), box);

    return box;
  }

  /**
   * Create a JComboBox for the property identified by <code>propertyID</code>, containing
   * values for the boolean values: true, false, null
   * @param propertyID the ID of the property to bind
   * @return JComboBox for the given property
   */
  protected final JComboBox createBooleanComboBox(final String propertyID) {
    return createBooleanComboBox(propertyID, null);
  }

  /**
   * Create a JComboBox for the property identified by <code>propertyID</code>, containing
   * values for the boolean values: true, false, null
   * @param propertyID the ID of the property to bind
   * @param enabledState a state for controlling the enabled state of the component
   * @return JComboBox for the given property
   */
  protected final JComboBox createBooleanComboBox(final String propertyID, final State enabledState) {
    return createBooleanComboBox(EntityRepository.getProperty(getEditModel().getEntityID(), propertyID),enabledState);
  }

  /**
   * Create a JComboBox for the given property, containing
   * values for the boolean values: true, false, null
   * @param property the property to bind
   * @return JComboBox for the given property
   */
  protected final JComboBox createBooleanComboBox(final Property property) {
    return createBooleanComboBox(property, null);
  }

  /**
   * Create a JComboBox for the given property, containing
   * values for the boolean values: true, false, null
   * @param property the property to bind
   * @param enabledState a state for controlling the enabled state of the component
   * @return JComboBox for the given property
   */
  protected final JComboBox createBooleanComboBox(final Property property, final State enabledState) {
    final JComboBox ret = EntityUiUtil.createBooleanComboBox(property, getEditModel(), enabledState);
    setControl(property.getPropertyID(), ret);

    return ret;
  }

  /**
   * Creates a SteppedComboBox bound to the property identified by <code>propertyID</code>
   * @param propertyID the ID of the property to bind
   * @param comboBoxModel the ComboBoxModel
   * @param maximumMatch true if maximum match should be used
   * @return a SteppedComboBox bound the the property
   * @see org.jminor.common.ui.combobox.MaximumMatch
   */
  protected final SteppedComboBox createComboBox(final String propertyID, final ComboBoxModel comboBoxModel,
                                                 final boolean maximumMatch) {
    return createComboBox(propertyID, comboBoxModel, maximumMatch, null);
  }

  /**
   * Creates a SteppedComboBox bound to the property identified by <code>propertyID</code>
   * @param propertyID the ID of the property to bind
   * @param comboBoxModel the ComboBoxModel
   * @param maximumMatch true if maximum match should be used
   * @param enabledState a state for controlling the enabled state of the component
   * @return a SteppedComboBox bound the the property
   * @see org.jminor.common.ui.combobox.MaximumMatch
   */
  protected final SteppedComboBox createComboBox(final String propertyID, final ComboBoxModel comboBoxModel,
                                                 final boolean maximumMatch, final State enabledState) {
    return createComboBox(EntityRepository.getProperty(getEditModel().getEntityID(), propertyID),
            comboBoxModel, maximumMatch, enabledState);
  }

  /**
   * Creates a SteppedComboBox bound to the given property
   * @param property the property to bind
   * @param comboBoxModel the ComboBoxModel
   * @param maximumMatch true if maximum match should be used
   * @return a SteppedComboBox bound the the property
   * @see org.jminor.common.ui.combobox.MaximumMatch
   */
  protected final SteppedComboBox createComboBox(final Property property, final ComboBoxModel comboBoxModel,
                                                 final boolean maximumMatch) {
    return createComboBox(property, comboBoxModel, maximumMatch, null);
  }

  /**
   * Creates a SteppedComboBox bound to the given property
   * @param property the property to bind
   * @param comboBoxModel the ComboBoxModel
   * @param maximumMatch true if maximum match should be used
   * @param enabledState a state for controlling the enabled state of the component
   * @return a SteppedComboBox bound the the property
   * @see org.jminor.common.ui.combobox.MaximumMatch
   */
  protected final SteppedComboBox createComboBox(final Property property, final ComboBoxModel comboBoxModel,
                                                 final boolean maximumMatch, final State enabledState) {
    final SteppedComboBox comboBox = EntityUiUtil.createComboBox(property, getEditModel(), comboBoxModel, enabledState);
    if (maximumMatch)
      MaximumMatch.enable(comboBox);
    setControl(property.getPropertyID(), comboBox);

    return comboBox;
  }

  /**
   * Creates an editable SteppedComboBox bound to the property identified by <code>propertyID</code>
   * @param propertyID the ID of the property to bind
   * @param comboBoxModel the ComboBoxModel
   * @return an editable SteppedComboBox bound the the property
   */
  protected final SteppedComboBox createEditableComboBox(final String propertyID, final ComboBoxModel comboBoxModel) {
    return createEditableComboBox(propertyID, comboBoxModel, null);
  }

  /**
   * Creates an editable SteppedComboBox bound to the property identified by <code>propertyID</code>
   * @param propertyID the ID of the property to bind
   * @param comboBoxModel the ComboBoxModel
   * @param enabledState a state for controlling the enabled state of the component
   * @return an editable SteppedComboBox bound the the property
   */
  protected final SteppedComboBox createEditableComboBox(final String propertyID, final ComboBoxModel comboBoxModel,
                                                         final State enabledState) {
    return createEditableComboBox(EntityRepository.getProperty(getEditModel().getEntityID(), propertyID),
            comboBoxModel, enabledState);
  }

  /**
   * Creates an editable SteppedComboBox bound to the given property
   * @param property the property to bind
   * @param comboBoxModel the ComboBoxModel
   * @param enabledState a state for controlling the enabled state of the component
   * @return an editable SteppedComboBox bound the the property
   */
  protected final SteppedComboBox createEditableComboBox(final Property property, final ComboBoxModel comboBoxModel,
                                                         final State enabledState) {
    final SteppedComboBox ret = EntityUiUtil.createComboBox(property, getEditModel(), comboBoxModel, enabledState, true);
    setControl(property.getPropertyID(), ret);

    return ret;
  }

  /**
   * Creates a SteppedComboBox bound to the property identified by <code>propertyID</code>, the combo box
   * contains the underlying values of the property
   * @param propertyID the ID of the property to bind
   * @return a SteppedComboBox bound to the property
   */
  protected final SteppedComboBox createPropertyComboBox(final String propertyID) {
    return createPropertyComboBox(propertyID, null);
  }

  /**
   * Creates a SteppedComboBox bound to the property identified by <code>propertyID</code>, the combo box
   * contains the underlying values of the property
   * @param propertyID the ID of the property to bind
   * @param enabledState a state for controlling the enabled state of the component
   * @return a SteppedComboBox bound to the property
   */
  protected final SteppedComboBox createPropertyComboBox(final String propertyID, final State enabledState) {
    return createPropertyComboBox(propertyID, enabledState, null);
  }

  /**
   * Creates a SteppedComboBox bound to the property identified by <code>propertyID</code>, the combo box
   * contains the underlying values of the property
   * @param propertyID the ID of the property to bind
   * @param enabledState a state for controlling the enabled state of the component
   * @param nullValueString the value used to represent a null value, shown at the top of the combo box value list
   * @return a SteppedComboBox bound to the property
   */
  protected final SteppedComboBox createPropertyComboBox(final String propertyID, final State enabledState,
                                                         final String nullValueString) {
    return createPropertyComboBox(propertyID, enabledState, nullValueString, false);
  }

  /**
   * Creates a SteppedComboBox bound to the property identified by <code>propertyID</code>, the combo box
   * contains the underlying values of the property
   * @param propertyID the ID of the property to bind
   * @param enabledState a state for controlling the enabled state of the component
   * @param nullValueString the value used to represent a null value, shown at the top of the combo box value list
   * @param editable true if the combo box should be editable, only works with combo boxes based on Type.STRING properties
   * @return a SteppedComboBox bound to the property
   */
  protected final SteppedComboBox createPropertyComboBox(final String propertyID, final State enabledState,
                                                         final String nullValueString, final boolean editable) {
    return createPropertyComboBox(EntityRepository.getProperty(getEditModel().getEntityID(), propertyID),
            enabledState, nullValueString, editable);
  }

  /**
   * Creates a SteppedComboBox bound to the given property, the combo box
   * contains the underlying values of the property
   * @param property the property to bind
   * @return a SteppedComboBox bound to the property
   */
  protected final SteppedComboBox createPropertyComboBox(final Property property) {
    return createPropertyComboBox(property, null);
  }

  /**
   * Creates a SteppedComboBox bound to the given property, the combo box
   * contains the underlying values of the property
   * @param property the property to bind
   * @param enabledState a state for controlling the enabled state of the component
   * @return a SteppedComboBox bound to the property
   */
  protected final SteppedComboBox createPropertyComboBox(final Property property, final State enabledState) {
    return createPropertyComboBox(property, enabledState, null);
  }

  /**
   * Creates a SteppedComboBox bound to the given property, the combo box
   * contains the underlying values of the property
   * @param property the property to bind
   * @param enabledState a state for controlling the enabled state of the component
   * @param nullValueString the value used to represent a null value, shown at the top of the combo box value list
   * @return a SteppedComboBox bound to the property
   */
  protected final SteppedComboBox createPropertyComboBox(final Property property, final State enabledState,
                                                         final String nullValueString) {
    return createPropertyComboBox(property, enabledState, nullValueString, false);
  }

  /**
   * Creates a SteppedComboBox bound to the given property, the combo box
   * contains the underlying values of the property
   * @param property the property to bind
   * @param enabledState a state for controlling the enabled state of the component
   * @param nullValueString the value used to represent a null value, shown at the top of the combo box value list
   * @param editable true if the combo box should be editable, only works with combo boxes based on Type.STRING properties
   * @return a SteppedComboBox bound to the property
   */
  protected final SteppedComboBox createPropertyComboBox(final Property property, final State enabledState,
                                                         final String nullValueString, final boolean editable) {
    final SteppedComboBox ret = EntityUiUtil.createPropertyComboBox(property, getEditModel(), null, enabledState, nullValueString, editable);
    setControl(property.getPropertyID(), ret);

    return ret;
  }

  /**
   * Creates a EntityComboBox bound to the property identified by <code>propertyID</code>
   * @param foreignKeyPropertyID the ID of the property to bind
   * @param enabledState a state for controlling the enabled state of the component
   * @return a EntityComboBox bound to the property
   */
  protected final EntityComboBox createEntityComboBox(final String foreignKeyPropertyID, final State enabledState) {
    return createEntityComboBox((Property.ForeignKeyProperty)
            EntityRepository.getProperty(getEditModel().getEntityID(), foreignKeyPropertyID), enabledState);
  }

  /**
   * Creates an EntityComboBox bound to the property identified by <code>propertyID</code>
   * @param foreignKeyPropertyID the ID of the foreign key property to bind
   * combination used to create new instances of the entity this EntityComboBox is based on
   * EntityComboBox is focusable
   * @return an EntityComboBox bound to the property
   */
  protected final EntityComboBox createEntityComboBox(final String foreignKeyPropertyID) {
    return createEntityComboBox(EntityRepository.getForeignKeyProperty(getEditModel().getEntityID(),
            foreignKeyPropertyID), null);
  }

  /**
   * Creates an EntityComboBox bound to the given foreign key property
   * @param foreignKeyProperty the foreign key property to bind
   * @return an EntityComboBox bound to the property
   */
  protected final EntityComboBox createEntityComboBox(final Property.ForeignKeyProperty foreignKeyProperty) {
    return createEntityComboBox(foreignKeyProperty, null);
  }

  /**
   * Creates an EntityComboBox bound to the given foreign key property
   * @param foreignKeyProperty the foreign key property to bind
   * combination used to create new instances of the entity this EntityComboBox is based on
   * EntityComboBox is focusable
   * @param enabledState a state for controlling the enabled state of the component
   * @return an EntityComboBox bound to the property
   */
  protected final EntityComboBox createEntityComboBox(final Property.ForeignKeyProperty foreignKeyProperty,
                                                      final State enabledState) {
    final EntityComboBox ret = EntityUiUtil.createEntityComboBox(foreignKeyProperty, getEditModel(), enabledState);
    setControl(foreignKeyProperty.getPropertyID(), ret);

    return ret;
  }

  /**
   * Creates an EntityLookupField bound to the property identified by <code>propertyID</code>, the property
   * must be an Property.ForeignKeyProperty
   * @param foreignKeyPropertyID the ID of the foreign key property to bind
   * @return an EntityLookupField bound the property
   */
  protected final EntityLookupField createEntityLookupField(final String foreignKeyPropertyID) {
    final Property.ForeignKeyProperty fkProperty = EntityRepository.getForeignKeyProperty(getEditModel().getEntityID(),
            foreignKeyPropertyID);
    return createEntityLookupField(fkProperty, EntityRepository.getEntitySearchPropertyIDs(fkProperty.getReferencedEntityID()));
  }

  /**
   * Creates an EntityLookupField bound to the property identified by <code>propertyID</code>, the property
   * must be an Property.ForeignKeyProperty
   * @param foreignKeyPropertyID the ID of the foreign key property to bind
   * @param searchPropertyIDs the IDs of the properties to use in the lookup
   * @return an EntityLookupField bound the property
   */
  protected final EntityLookupField createEntityLookupField(final String foreignKeyPropertyID,
                                                            final String... searchPropertyIDs) {
    final Property.ForeignKeyProperty fkProperty = EntityRepository.getForeignKeyProperty(getEditModel().getEntityID(),
            foreignKeyPropertyID);
    return createEntityLookupField(fkProperty, searchPropertyIDs == null || searchPropertyIDs.length == 0 ?
            EntityRepository.getEntitySearchPropertyIDs(fkProperty.getReferencedEntityID()) : searchPropertyIDs);
  }

  /**
   * Creates an EntityLookupField bound to the given foreign key property
   * @param foreignKeyProperty the foreign key property to bind
   * @param searchPropertyIDs the IDs of the properties to use in the lookup
   * @return an EntityLookupField bound the property
   */
  protected final EntityLookupField createEntityLookupField(final Property.ForeignKeyProperty foreignKeyProperty,
                                                            final String... searchPropertyIDs) {
    final EntityLookupField ret = EntityUiUtil.createEntityLookupField(foreignKeyProperty, getEditModel(), searchPropertyIDs);
    setControl(foreignKeyProperty.getPropertyID(), ret);

    return ret;
  }

  /**
   * Creates an uneditable JTextField bound to the property identified by <code>propertyID</code>
   * @param propertyID the ID of the property to bind
   * @return an uneditable JTextField bound to the property
   */
  protected final JTextField createEntityField(final String propertyID) {
    return createEntityField(EntityRepository.getForeignKeyProperty(getEditModel().getEntityID(), propertyID));
  }

  /**
   * Creates an uneditable JTextField bound to the given property
   * @param foreignKeyProperty the foreign key property to bind
   * @return an uneditable JTextField bound to the property
   */
  protected final JTextField createEntityField(final Property.ForeignKeyProperty foreignKeyProperty) {
    final JTextField ret = EntityUiUtil.createEntityField(foreignKeyProperty, getEditModel());
    setControl(foreignKeyProperty.getPropertyID(), ret);

    return ret;
  }

  /**
   * Creates a JPanel containing an uneditable JTextField bound to the property identified by <code>propertyID</code>
   * and a button for selecting an Entity to set as the property value
   * @param propertyID the ID of the property to bind
   * @param lookupModel an EntityTableModel to use when looking up entities
   * @return an uneditable JTextField bound to the property
   */
  protected final JPanel createEntityFieldPanel(final String propertyID, final EntityTableModel lookupModel) {
    return createEntityFieldPanel((Property.ForeignKeyProperty)
            EntityRepository.getProperty(getEditModel().getEntityID(), propertyID), lookupModel);
  }

  /**
   * Creates a JPanel containing an uneditable JTextField bound to the given property identified
   * and a button for selecting an Entity to set as the property value
   * @param foreignKeyProperty the foreign key property to bind
   * @param lookupModel an EntityTableModel to use when looking up entities
   * @return an uneditable JTextField bound to the property
   */
  protected final JPanel createEntityFieldPanel(final Property.ForeignKeyProperty foreignKeyProperty,
                                                final EntityTableModel lookupModel) {
    final JPanel ret = EntityUiUtil.createEntityFieldPanel(foreignKeyProperty, getEditModel(), lookupModel);
    setControl(foreignKeyProperty.getPropertyID(), ret);//todo get my hands on the text field

    return ret;
  }

  /**
   * Creates a JLabel with a caption from the property identified by <code>propertyID</code>
   * @param propertyID the ID of the property from which to retrieve the caption
   * @return a JLabel for the given property
   */
  protected final JLabel createLabel(final String propertyID) {
    return createLabel(propertyID, JLabel.LEFT);
  }

  /**
   * Creates a JLabel with a caption from the given property identified by <code>propertyID</code>
   * @param propertyID the ID of the property from which to retrieve the caption
   * @param horizontalAlignment the horizontal text alignment
   * @return a JLabel for the given property
   */
  protected final JLabel createLabel(final String propertyID, final int horizontalAlignment) {
    return EntityUiUtil.createLabel(EntityRepository.getProperty(getEditModel().getEntityID(), propertyID), horizontalAlignment);
  }
}
