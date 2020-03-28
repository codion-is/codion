/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.ui;

import org.jminor.common.Configuration;
import org.jminor.common.i18n.Messages;
import org.jminor.common.state.StateObserver;
import org.jminor.common.value.AbstractValue;
import org.jminor.common.value.PropertyValue;
import org.jminor.framework.domain.entity.Entity;
import org.jminor.framework.domain.property.ColumnProperty;
import org.jminor.framework.domain.property.ForeignKeyProperty;
import org.jminor.framework.domain.property.Properties;
import org.jminor.framework.domain.property.Property;
import org.jminor.framework.domain.property.ValueListProperty;
import org.jminor.framework.model.EntityEditModel;
import org.jminor.swing.common.ui.checkbox.NullableCheckBox;
import org.jminor.swing.common.ui.combobox.MaximumMatch;
import org.jminor.swing.common.ui.combobox.SteppedComboBox;
import org.jminor.swing.common.ui.dialog.Dialogs;
import org.jminor.swing.common.ui.layout.Layouts;
import org.jminor.swing.common.ui.textfield.TextInputPanel;
import org.jminor.swing.common.ui.time.LocalDateInputPanel;
import org.jminor.swing.common.ui.time.LocalDateTimeInputPanel;
import org.jminor.swing.common.ui.time.TemporalInputPanel;
import org.jminor.swing.common.ui.value.TextValues;
import org.jminor.swing.common.ui.value.UpdateOn;
import org.jminor.swing.framework.model.SwingEntityEditModel;

import javax.swing.ComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Objects.requireNonNull;
import static org.jminor.swing.common.ui.KeyEvents.transferFocusOnEnter;

/**
 * A base class for entity edit panels, providing components for editing entities.
 */
public class EntityEditComponentPanel extends JPanel {

  /**
   * Specifies whether focus should be transferred from components on enter,
   * this does not apply to text areas<br>
   * Value type: Boolean<br>
   * Default value: true
   */
  public static final PropertyValue<Boolean> TRANSFER_FOCUS_ON_ENTER = Configuration.booleanValue(
          "org.jminor.swing.framework.ui.EntityEditComponentPanel.transferFocusOnEnter", true);

  /**
   * The edit model these edit components are associated with
   */
  private final SwingEntityEditModel editModel;

  /**
   * Input components mapped to their respective propertyIds
   */
  private final Map<String, JComponent> components = new HashMap<>();

  /**
   * Properties that should be excluded when presenting the component selection
   */
  private final Set<String> excludeFromSelection = new HashSet<>();

  /**
   * The component that should receive focus when the UI is initialized
   */
  private JComponent initialFocusComponent;

  /**
   * The propertyId for which component should receive the focus when the UI is prepared
   */
  private String initialFocusPropertyId;

  /**
   * The component that should receive focus when the UI is prepared after insert
   */
  private JComponent afterInsertFocusComponent;

  /**
   * The propertyId for which component should receive the focus when the UI is prepared after insert
   */
  private String afterInsertFocusPropertyId;

  /**
   * Instantiates a new EntityEditComponentPanel
   * @param editModel the edit model
   */
  protected EntityEditComponentPanel(final SwingEntityEditModel editModel) {
    this.editModel = requireNonNull(editModel, "editModel");
  }

  /**
   * @return the edit model this panel is based on
   */
  public final SwingEntityEditModel getEditModel() {
    return editModel;
  }

  /**
   * @return the propertyIds that have been associated with components.
   */
  public final List<String> getComponentPropertyIds() {
    return new ArrayList<>(components.keySet());
  }

  /**
   * @param propertyId the propertyId
   * @return the component associated with the given propertyId, null if no component has been
   * associated with the given propertyId
   */
  public final JComponent getComponent(final String propertyId) {
    return components.get(propertyId);
  }

  /**
   * @param component the component
   * @return the propertyId the given component is associated with, null if the component has not been
   * associated with a propertyId
   */
  public final String getComponentPropertyId(final JComponent component) {
    return components.entrySet().stream().filter(entry -> entry.getValue() == component)
            .findFirst().map(Map.Entry::getKey).orElse(null);
  }

  /**
   * Sets the component that should receive the focus when the UI is cleared or activated.
   * Overrides the value set via {@link #setInitialFocusProperty(String)}
   * @param initialFocusComponent the component
   * @return the component
   */
  public final JComponent setInitialFocusComponent(final JComponent initialFocusComponent) {
    this.initialFocusComponent = initialFocusComponent;
    return initialFocusComponent;
  }

  /**
   * Sets the component associated with the given propertyId as the component
   * that should receive the initial focus in this edit panel.
   * This is overridden by setInitialFocusComponent().
   * @param propertyId the component propertyId
   * @see #setInitialFocusComponent(javax.swing.JComponent)
   */
  public final void setInitialFocusProperty(final String propertyId) {
    this.initialFocusPropertyId = propertyId;
  }

  /**
   * Sets the component that should receive the focus after an insert has been performed..
   * Overrides the value set via {@link #setAfterInsertFocusProperty(String)}
   * @param afterInsertFocusComponent the component
   * @return the component
   */
  public final JComponent setAfterInsertFocusComponent(final JComponent afterInsertFocusComponent) {
    this.afterInsertFocusComponent = afterInsertFocusComponent;
    return afterInsertFocusComponent;
  }

  /**
   * Sets the component associated with the given propertyId as the component
   * that should receive the focus after an insert is performed in this edit panel.
   * This is overridden by setAfterInsertFocusComponent().
   * @param propertyId the component propertyId
   * @see #setAfterInsertFocusComponent(JComponent)
   */
  public final void setAfterInsertFocusProperty(final String propertyId) {
    this.afterInsertFocusPropertyId = propertyId;
  }

  /**
   * Sets the initial focus, if a initial focus component or component propertyId
   * has been set that component receives the focus, if not, or if that component
   * is not focusable, this panel receives the focus.
   * Note that if this panel is not visible nothing happens.
   * @see #isVisible()
   * @see #setInitialFocusProperty
   * @see #setInitialFocusComponent(javax.swing.JComponent)
   */
  public final void requestInitialFocus() {
    if (isVisible()) {
      requestFocus(getInitialFocusComponent());
    }
  }

  /**
   * Request focus for the component associated with the given propertyId
   * @param propertyId the propertyId of the component to select
   */
  public final void requestComponentFocus(final String propertyId) {
    final JComponent component = getComponent(propertyId);
    if (component != null) {
      component.requestFocus();
    }
  }

  /**
   * Displays a dialog allowing the user the select a input component which should receive the keyboard focus,
   * if only one input component is available then that component is selected automatically.
   * @see #excludeComponentFromSelection(String)
   * @see #requestComponentFocus(String)
   */
  public void selectInputComponent() {
    final List<String> propertyIds = getSelectComponentPropertyIds();
    final List<Property> properties = Properties.sort(getEditModel().getEntityDefinition().getProperties(propertyIds));
    final Property property = properties.size() == 1 ?  properties.get(0) :
            Dialogs.selectValue(this, properties, Messages.get(Messages.SELECT_INPUT_FIELD));
    if (property != null) {
      requestComponentFocus(property.getPropertyId());
    }
  }

  /**
   * Specifies that the given property should be excluded when presenting a component selection list.
   * @param propertyId the id of the property to exclude from selection
   */
  public final void excludeComponentFromSelection(final String propertyId) {
    getEditModel().getEntityDefinition().getProperty(propertyId);//just validating that the property exists
    excludeFromSelection.add(propertyId);
  }

  /**
   * Associates the given input component with the given propertyId.
   * @param propertyId the propertyId
   * @param component the input component
   */
  protected final void setComponent(final String propertyId, final JComponent component) {
    getEditModel().getEntityDefinition().getProperty(propertyId);
    if (components.containsKey(propertyId)) {
      throw new IllegalStateException("Component already set for propertyId: " + propertyId);
    }
    components.put(propertyId, component);
  }

  /**
   * Adds a property panel for the given property to this panel
   * @param propertyId the ID of the property
   * @see #createPropertyPanel(String)
   */
  protected final void addPropertyPanel(final String propertyId) {
    add(createPropertyPanel(propertyId));
  }

  /**
   * Creates a panel containing a label and the component associated with the given property.
   * The label text is the caption of the property identified by {@code propertyId}.
   * The default layout of the resulting panel is with the label on top and inputComponent below.
   * @param propertyId the id of the property from which to retrieve the label caption
   * @return a panel containing a label and a component
   * @throws IllegalArgumentException in case no component has been associated with the given property
   */
  protected final JPanel createPropertyPanel(final String propertyId) {
    final JComponent component = getComponent(propertyId);
    if (component == null) {
      throw new IllegalArgumentException("No component associated with property: " + propertyId);
    }

    return createPropertyPanel(propertyId, component);
  }

  /**
   * Creates a panel containing a label and the given component.
   * The label text is the caption of the property identified by {@code propertyId}.
   * The default layout of the resulting panel is with the label on top and {@code inputComponent} below.
   * @param propertyId the id of the property from which to retrieve the label caption
   * @param inputComponent a component bound to the property with id {@code propertyId}
   * @return a panel containing a label and a component
   */
  protected final JPanel createPropertyPanel(final String propertyId, final JComponent inputComponent) {
    return createPropertyPanel(propertyId, inputComponent, BorderLayout.NORTH);
  }

  /**
   * Creates a panel containing a label and the given component.
   * The label text is the caption of the property identified by {@code propertyId}.
   * @param propertyId the id of the property from which to retrieve the label caption
   * @param inputComponent a component bound to the property with id {@code propertyId}
   * @param labelBorderLayoutConstraints {@link BorderLayout#NORTH}, {@link BorderLayout#SOUTH},
   * {@link BorderLayout#EAST} or {@link BorderLayout#WEST}
   * @return a panel containing a label and a component
   */
  protected final JPanel createPropertyPanel(final String propertyId, final JComponent inputComponent,
                                             final String labelBorderLayoutConstraints) {
    return createPropertyPanel(propertyId, inputComponent, labelBorderLayoutConstraints, JLabel.LEADING);
  }

  /**
   * Creates a panel containing a label and the given component.
   * The label text is the caption of the property identified by {@code propertyId}.
   * @param propertyId the id of the property from which to retrieve the label caption
   * @param inputComponent a component bound to the property with id {@code propertyId}
   * @param labelBorderLayoutConstraints {@link BorderLayout#NORTH}, {@link BorderLayout#SOUTH},
   * {@link BorderLayout#EAST} or {@link BorderLayout#WEST}
   * @param labelAlignment the label alignment
   * @return a panel containing a label and a component
   */
  protected final JPanel createPropertyPanel(final String propertyId, final JComponent inputComponent,
                                             final String labelBorderLayoutConstraints, final int labelAlignment) {
    return createPropertyPanel(createLabel(propertyId, labelAlignment), inputComponent, labelBorderLayoutConstraints);
  }

  /**
   * Creates a panel containing a label component and the {@code inputComponent} with the label
   * component positioned above the input component.
   * @param labelComponent the label component
   * @param inputComponent a input component
   * @return a panel containing a label and a component
   */
  protected final JPanel createPropertyPanel(final JComponent labelComponent, final JComponent inputComponent) {
    return createPropertyPanel(labelComponent, inputComponent, BorderLayout.NORTH);
  }

  /**
   * Creates a panel with a BorderLayout, with the {@code inputComponent} at BorderLayout.CENTER
   * and the {@code labelComponent} at a specified location.
   * @param labelComponent the label component
   * @param inputComponent a input component
   * @param labelBorderLayoutConstraints {@link BorderLayout#NORTH}, {@link BorderLayout#SOUTH},
   * {@link BorderLayout#EAST} or {@link BorderLayout#WEST}
   * @return a panel containing a label and a component
   */
  protected final JPanel createPropertyPanel(final JComponent labelComponent, final JComponent inputComponent,
                                             final String labelBorderLayoutConstraints) {
    requireNonNull(labelComponent, "labelComponent");
    requireNonNull(inputComponent, "inputComponent");
    if (labelComponent instanceof JLabel) {
      setLabelForComponent((JLabel) labelComponent, inputComponent);
    }
    final JPanel panel = new JPanel(Layouts.borderLayout());
    panel.add(inputComponent, BorderLayout.CENTER);
    panel.add(labelComponent, labelBorderLayoutConstraints);

    return panel;
  }

  /**
   * Creates a JTextArea component bound to the property identified by {@code propertyId}.
   * @param propertyId the ID of the property to bind
   * @return a JTextArea bound to the property
   */
  protected final JTextArea createTextArea(final String propertyId) {
    return createTextArea(propertyId, -1, -1);
  }

  /**
   * Creates a JTextArea component bound to the property identified by {@code propertyId}.
   * @param propertyId the ID of the property to bind
   * @param rows the number of rows in the text area
   * @param columns the number of columns in the text area
   * @return a JTextArea bound to the property
   */
  protected final JTextArea createTextArea(final String propertyId, final int rows, final int columns) {
    return createTextArea(propertyId, rows, columns, UpdateOn.KEYSTROKE);
  }

  /**
   * Creates a JTextArea component bound to the property identified by {@code propertyId}.
   * @param propertyId the ID of the property to bind
   * @param rows the number of rows in the text area
   * @param columns the number of columns in the text area
   * @param updateOn specifies when the underlying value should be updated
   * @return a JTextArea bound to the property
   */
  protected final JTextArea createTextArea(final String propertyId, final int rows, final int columns,
                                           final UpdateOn updateOn) {
    return createTextArea(propertyId, rows, columns, updateOn, null);
  }

  /**
   * Creates a JTextArea component bound to the property identified by {@code propertyId}.
   * @param propertyId the ID of the property to bind
   * @param rows the number of rows in the text area
   * @param columns the number of columns in the text area
   * @param updateOn specifies when the underlying value should be updated
   * @param enabledState a state indicating when this text area should be enabled
   * @return a JTextArea bound to the property
   */
  protected final JTextArea createTextArea(final String propertyId, final int rows, final int columns,
                                           final UpdateOn updateOn, final StateObserver enabledState) {
    final Property property = getEditModel().getEntityDefinition().getProperty(propertyId);
    final JTextArea textArea = EntityInputComponents.createTextArea(property,
            getEditModel().value(property.getPropertyId()), rows, columns, updateOn, enabledState);
    EntityComponentValidators.addValidator(property, textArea, getEditModel());
    setComponent(propertyId, textArea);

    return textArea;
  }

  /**
   * Creates a TextInputPanel bound to the property identified by {@code propertyId}.
   * @param propertyId the ID of the property to bind
   * @return a TextInputPanel bound to the property
   */
  protected final TextInputPanel createTextInputPanel(final String propertyId) {
    return createTextInputPanel(propertyId, UpdateOn.KEYSTROKE, true);
  }

  /**
   * Creates a TextInputPanel bound to the property identified by {@code propertyId}.
   * @param propertyId the ID of the property to bind
   * @param updateOn specifies when the underlying value should be updated
   * @param buttonFocusable specifies whether the edit button should be focusable.
   * @return a TextInputPanel bound to the property
   */
  protected final TextInputPanel createTextInputPanel(final String propertyId, final UpdateOn updateOn,
                                                      final boolean buttonFocusable) {
    return createTextInputPanel(getEditModel().getEntityDefinition().getProperty(propertyId),
            updateOn, buttonFocusable);
  }

  /**
   * Creates a TextInputPanel bound to the property identified by {@code propertyId}.
   * @param property the property to bind
   * @param updateOn specifies when the underlying value should be updated
   * @return a TextInputPanel bound to the property
   */
  protected final TextInputPanel createTextInputPanel(final Property property, final UpdateOn updateOn) {
    return createTextInputPanel(property, updateOn, true);
  }

  /**
   * Creates a TextInputPanel bound to the property identified by {@code propertyId}.
   * @param property the property to bind
   * @param updateOn specifies when the underlying value should be updated
   * @param buttonFocusable specifies whether the edit button should be focusable.
   * @return a TextInputPanel bound to the property
   */
  protected final TextInputPanel createTextInputPanel(final Property property, final UpdateOn updateOn,
                                                      final boolean buttonFocusable) {
    final TextInputPanel inputPanel = EntityInputComponents.createTextInputPanel(property,
            getEditModel().value(property.getPropertyId()), updateOn, buttonFocusable);
    if (TRANSFER_FOCUS_ON_ENTER.get()) {
      transferFocusOnEnter(inputPanel.getTextField());
      if (inputPanel.getButton() != null) {
        transferFocusOnEnter(inputPanel.getButton());
      }
    }
    setComponent(property.getPropertyId(), inputPanel);

    return inputPanel;
  }

  /**
   * Creates a new TemporalInputPanel using the default short date format, bound to the property
   * identified by {@code propertyId}.
   * @param propertyId the ID of the property for which to create the panel
   * @return a TemporalInputPanel using the default short date format
   * @see Property#DATE_FORMAT
   */
  protected final TemporalInputPanel createTemporalInputPanel(final String propertyId) {
    return createTemporalInputPanel(propertyId, true);
  }

  /**
   * Creates a new TemporalInputPanel bound to the property identified by {@code propertyId}.
   * @param propertyId the ID of the property for which to create the panel
   * @param includeButton if true a button for visually editing the date is included
   * @return a TemporalInputPanel using the default short date format
   * @see Property#DATE_FORMAT
   */
  protected final TemporalInputPanel createTemporalInputPanel(final String propertyId, final boolean includeButton) {
    final Property property = getEditModel().getEntityDefinition().getProperty(propertyId);
    return createTemporalInputPanel(property, includeButton, null);
  }

  /**
   * Creates a new TemporalInputPanel bound to the property identified by {@code propertyId}.
   * @param propertyId the ID of the property for which to create the panel
   * @param includeButton if true a button for visually editing the date is included
   * @param enabledState a state for controlling the enabled state of the input component
   * @return a TemporalInputPanel bound to the property
   */
  protected final TemporalInputPanel createTemporalInputPanel(final String propertyId, final boolean includeButton,
                                                              final StateObserver enabledState) {
    return createTemporalInputPanel(propertyId, includeButton, enabledState, UpdateOn.KEYSTROKE);
  }

  /**
   * Creates a new TemporalInputPanel bound to the property identified by {@code propertyId}.
   * @param propertyId the ID of the property for which to create the panel
   * @param includeButton if true a button for visually editing the date is included
   * @param enabledState a state for controlling the enabled state of the input component
   * @param updateOn specifies when the underlying value should be updated
   * @return a TemporalInputPanel bound to the property
   */
  protected final TemporalInputPanel createTemporalInputPanel(final String propertyId, final boolean includeButton,
                                                              final StateObserver enabledState, final UpdateOn updateOn) {
    return createTemporalInputPanel(getEditModel().getEntityDefinition().getProperty(propertyId),
            includeButton, enabledState, updateOn);
  }

  /**
   * Creates a new TemporalInputPanel bound to the property identified by {@code propertyId}.
   * @param property the property for which to create the panel
   * @return a TemporalInputPanel bound to the property
   */
  protected final TemporalInputPanel createTemporalInputPanel(final Property property) {
    return createTemporalInputPanel(property, true);
  }

  /**
   * Creates a new TemporalInputPanel bound to the given property.
   * @param property the property for which to create the panel
   * @param includeButton if true a button for visually editing the date is included
   * @return a TemporalInputPanel bound to the property
   */
  protected final TemporalInputPanel createTemporalInputPanel(final Property property, final boolean includeButton) {
    return createTemporalInputPanel(property, includeButton, null);
  }

  /**
   * Creates a new TemporalInputPanel bound to the given property.
   * @param property the property for which to create the panel
   * @param includeButton if true a button for visually editing the date is included
   * @param enabledState a state for controlling the enabled state of the input component
   * @return a TemporalInputPanel bound to the property
   */
  protected final TemporalInputPanel createTemporalInputPanel(final Property property, final boolean includeButton,
                                                              final StateObserver enabledState) {
    return createTemporalInputPanel(property, includeButton, enabledState, UpdateOn.KEYSTROKE);
  }

  /**
   * Creates a new TemporalInputPanel bound to the given property.
   * @param property the property for which to create the panel
   * @param includeButton if true a button for visually editing the date is included
   * @param enabledState a state for controlling the enabled state of the input component
   * @param updateOn specifies when the underlying value should be updated
   * @return a TemporalInputPanel bound to the property
   */
  protected final TemporalInputPanel createTemporalInputPanel(final Property property, final boolean includeButton,
                                                              final StateObserver enabledState, final UpdateOn updateOn) {
    final TemporalInputPanel panel = EntityInputComponents.createTemporalInputPanel(property,
            getEditModel().value(property.getPropertyId()), updateOn, includeButton, enabledState);
    if (panel instanceof LocalDateInputPanel) {
      final LocalDateInputPanel localDateInputPanel = (LocalDateInputPanel) panel;
      if (localDateInputPanel.getCalendarButton() != null && TRANSFER_FOCUS_ON_ENTER.get()) {
        transferFocusOnEnter(localDateInputPanel.getCalendarButton());
      }
    }
    if (panel instanceof LocalDateTimeInputPanel) {
      final LocalDateTimeInputPanel localDateTimeInputPanel = (LocalDateTimeInputPanel) panel;
      if (localDateTimeInputPanel.getCalendarButton() != null && TRANSFER_FOCUS_ON_ENTER.get()) {
        transferFocusOnEnter(localDateTimeInputPanel.getCalendarButton());
      }
    }
    if (TRANSFER_FOCUS_ON_ENTER.get()) {
      transferFocusOnEnter(panel.getInputField());
    }
    setComponent(property.getPropertyId(), panel);

    return panel;
  }

  /**
   * Creates a JTextField bound to the property identified by {@code propertyId}
   * @param propertyId the ID of the property to bind
   * @return a text field bound to the property
   */
  protected final JTextField createTextField(final String propertyId) {
    return createTextField(propertyId, UpdateOn.KEYSTROKE);
  }

  /**
   * Creates a JTextField bound to the property identified by {@code propertyId}
   * @param propertyId the ID of the property to bind
   * @param updateOn specifies when the underlying value should be updated
   * @return a text field bound to the property
   */
  protected final JTextField createTextField(final String propertyId, final UpdateOn updateOn) {
    return createTextField(propertyId, updateOn, null);
  }

  /**
   * Creates a JTextField bound to the property identified by {@code propertyId}
   * @param propertyId the ID of the property to bind
   * @param updateOn specifies when the underlying value should be updated
   * @param maskString if specified then a JFormattedTextField with the given mask is returned
   * @return a text field bound to the property
   */
  protected final JTextField createTextField(final String propertyId, final UpdateOn updateOn,
                                             final String maskString) {
    return createTextField(propertyId, updateOn, maskString, null);
  }

  /**
   * Creates a JTextField bound to the property identified by {@code propertyId}
   * @param propertyId the ID of the property to bind
   * @param updateOn specifies when the underlying value should be updated
   * @param maskString if specified then a JFormattedTextField with the given mask is returned
   * @param enabledState a state for controlling the enabled state of the component
   * @return a text field bound to the property
   */
  protected final JTextField createTextField(final String propertyId, final UpdateOn updateOn,
                                             final String maskString, final StateObserver enabledState) {
    return createTextField(propertyId, updateOn, maskString, enabledState, false);
  }

  /**
   * Creates a JTextField bound to the property identified by {@code propertyId}
   * @param propertyId the ID of the property to bind
   * @param updateOn specifies when the underlying value should be updated
   * @param maskString if specified then a JFormattedTextField with the given mask is returned
   * @param enabledState a state for controlling the enabled state of the component
   * @param valueIncludesLiteralCharacters only applicable if {@code maskString} is specified
   * @return a text field bound to the property
   */
  protected final JTextField createTextField(final String propertyId, final UpdateOn updateOn,
                                             final String maskString, final StateObserver enabledState,
                                             final boolean valueIncludesLiteralCharacters) {
    return createTextField(getEditModel().getEntityDefinition().getProperty(propertyId),
            updateOn, maskString, enabledState, valueIncludesLiteralCharacters);
  }

  /**
   * Creates a JTextField bound to the given property
   * @param property the property to bind
   * @return a text field bound to the property
   */
  protected final JTextField createTextField(final Property property) {
    return createTextField(property, UpdateOn.KEYSTROKE);
  }

  /**
   * Creates a JTextField bound to the given property
   * @param property the property to bind
   * @param updateOn specifies when the underlying value should be updated
   * @return a text field bound to the property
   */
  protected final JTextField createTextField(final Property property, final UpdateOn updateOn) {
    return createTextField(property, null, updateOn);
  }

  /**
   * Creates a JTextField bound to the given property
   * @param property the property to bind
   * @param maskString if specified then a JFormattedTextField with the given mask is returned
   * @param updateOn specifies when the underlying value should be updated
   * @return a text field bound to the property
   */
  protected final JTextField createTextField(final Property property, final String maskString,
                                             final UpdateOn updateOn) {
    return createTextField(property, maskString, updateOn, null);
  }

  /**
   * Creates a JTextField bound to the given property
   * @param property the ID of the property to bind
   * @param maskString if specified then a JFormattedTextField with the given mask is returned
   * @param updateOn specifies when the underlying value should be updated
   * @param enabledState a state for controlling the enabled state of the component
   * @return a text field bound to the property
   */
  protected final JTextField createTextField(final Property property, final String maskString,
                                             final UpdateOn updateOn, final StateObserver enabledState) {
    return createTextField(property, updateOn, maskString, enabledState, false);
  }

  /**
   * Creates a JTextField bound to the given property
   * @param property the property to bind
   * @param updateOn specifies when the underlying value should be updated
   * @param maskString if specified then a JFormattedTextField with the given mask is returned
   * @param enabledState a state for controlling the enabled state of the component
   * @param valueIncludesLiteralCharacters only applicable if {@code maskString} is specified
   * @return a text field bound to the property
   */
  protected final JTextField createTextField(final Property property, final UpdateOn updateOn,
                                             final String maskString, final StateObserver enabledState,
                                             final boolean valueIncludesLiteralCharacters) {
    final JTextField textField = EntityInputComponents.createTextField(property,
            getEditModel().value(property.getPropertyId()), maskString, updateOn,
            enabledState, valueIncludesLiteralCharacters);
    if (property.isString() && maskString != null) {
      EntityComponentValidators.addFormattedValidator(property, textField, getEditModel());
    }
    else {
      EntityComponentValidators.addValidator(property, textField, getEditModel());
    }
    if (TRANSFER_FOCUS_ON_ENTER.get()) {
      transferFocusOnEnter(textField);
    }
    setComponent(property.getPropertyId(), textField);

    return textField;
  }

  /**
   * Creates a JCheckBox bound to the property identified by {@code propertyId}
   * @param propertyId the ID of the property to bind
   * @return a JCheckBox bound to the property
   */
  protected final JCheckBox createCheckBox(final String propertyId) {
    return createCheckBox(propertyId, null);
  }

  /**
   * Creates a JCheckBox bound to the property identified by {@code propertyId}
   * @param propertyId the ID of the property to bind
   * @param enabledState a state for controlling the enabled state of the component
   * @return a JCheckBox bound to the property
   */
  protected final JCheckBox createCheckBox(final String propertyId, final StateObserver enabledState) {
    return createCheckBox(propertyId, enabledState, true);
  }

  /**
   * Creates a JCheckBox bound to the property identified by {@code propertyId}
   * @param propertyId the ID of the property to bind
   * @param enabledState a state for controlling the enabled state of the component
   * @param includeCaption specifies whether or not the caption should be included
   * @return a JCheckBox bound to the property
   */
  protected final JCheckBox createCheckBox(final String propertyId, final StateObserver enabledState,
                                           final boolean includeCaption) {
    return createCheckBox(getEditModel().getEntityDefinition().getProperty(propertyId), enabledState, includeCaption);
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
  protected final JCheckBox createCheckBox(final Property property, final StateObserver enabledState) {
    return createCheckBox(property, enabledState, true);
  }

  /**
   * Creates a JCheckBox bound to the given property
   * @param property the property to bind
   * @param enabledState a state for controlling the enabled state of the component
   * @param includeCaption specifies whether or not the caption should be included
   * @return a JCheckBox bound to the property
   */
  protected final JCheckBox createCheckBox(final Property property, final StateObserver enabledState,
                                           final boolean includeCaption) {
    final JCheckBox box = EntityInputComponents.createCheckBox(property,
            getEditModel().value(property.getPropertyId()), enabledState, includeCaption);
    if (TRANSFER_FOCUS_ON_ENTER.get()) {
      transferFocusOnEnter(box);
    }
    setComponent(property.getPropertyId(), box);

    return box;
  }

  /**
   * Creates a NullableCheckBox bound to the property identified by {@code propertyId}
   * @param propertyId the ID of the property to bind
   * @return a NullableCheckBox bound to the property
   */
  protected final NullableCheckBox createNullableCheckBox(final String propertyId) {
    return createNullableCheckBox(propertyId, null);
  }

  /**
   * Creates a NullableCheckBox bound to the property identified by {@code propertyId}
   * @param propertyId the ID of the property to bind
   * @param enabledState a state for controlling the enabled state of the component
   * @return a NullableCheckBox bound to the property
   */
  protected final NullableCheckBox createNullableCheckBox(final String propertyId, final StateObserver enabledState) {
    return createNullableCheckBox(propertyId, enabledState, true);
  }

  /**
   * Creates a NullableCheckBox bound to the property identified by {@code propertyId}
   * @param propertyId the ID of the property to bind
   * @param enabledState a state for controlling the enabled state of the component
   * @param includeCaption specifies whether or not the caption should be included
   * @return a NullableCheckBox bound to the property
   */
  protected final NullableCheckBox createNullableCheckBox(final String propertyId, final StateObserver enabledState,
                                                          final boolean includeCaption) {
    return createNullableCheckBox(getEditModel().getEntityDefinition().getProperty(propertyId), enabledState, includeCaption);
  }

  /**
   * Creates a NullableCheckBox bound to the given property
   * @param property the property to bind
   * @return a NullableCheckBox bound to the property
   */
  protected final NullableCheckBox createNullableCheckBox(final Property property) {
    return createNullableCheckBox(property, null);
  }

  /**
   * Creates a NullableCheckBox bound to the given property
   * @param property the property to bind
   * @param enabledState a state for controlling the enabled state of the component
   * @return a NullableCheckBox bound to the property
   */
  protected final NullableCheckBox createNullableCheckBox(final Property property, final StateObserver enabledState) {
    return createNullableCheckBox(property, enabledState, true);
  }

  /**
   * Creates a NullableCheckBox bound to the given property
   * @param property the property to bind
   * @param enabledState a state for controlling the enabled state of the component
   * @param includeCaption specifies whether or not the caption should be included
   * @return a NullableCheckBox bound to the property
   */
  protected final NullableCheckBox createNullableCheckBox(final Property property, final StateObserver enabledState,
                                                          final boolean includeCaption) {
    final NullableCheckBox box = EntityInputComponents.createNullableCheckBox(property,
            getEditModel().value(property.getPropertyId()), enabledState, includeCaption);
    if (TRANSFER_FOCUS_ON_ENTER.get()) {
      transferFocusOnEnter(box);
    }
    setComponent(property.getPropertyId(), box);

    return box;
  }

  /**
   * Create a JComboBox for the property identified by {@code propertyId}, containing
   * values for the boolean values: true, false, null
   * @param propertyId the ID of the property to bind
   * @return JComboBox for the given property
   */
  protected final JComboBox createBooleanComboBox(final String propertyId) {
    return createBooleanComboBox(propertyId, null);
  }

  /**
   * Create a JComboBox for the property identified by {@code propertyId}, containing
   * values for the boolean values: true, false, null
   * @param propertyId the ID of the property to bind
   * @param enabledState a state for controlling the enabled state of the component
   * @return JComboBox for the given property
   */
  protected final JComboBox createBooleanComboBox(final String propertyId, final StateObserver enabledState) {
    return createBooleanComboBox(getEditModel().getEntityDefinition().getProperty(propertyId), enabledState);
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
  protected final JComboBox createBooleanComboBox(final Property property, final StateObserver enabledState) {
    final JComboBox comboBox = EntityInputComponents.createBooleanComboBox(property,
            getEditModel().value(property.getPropertyId()), enabledState);
    setComponent(property.getPropertyId(), comboBox);

    return comboBox;
  }

  /**
   * Creates a SteppedComboBox bound to the property identified by {@code propertyId}
   * @param propertyId the ID of the property to bind
   * @param comboBoxModel the ComboBoxModel
   * @param maximumMatch true if maximum match should be used
   * @return a SteppedComboBox bound the the property
   * @see org.jminor.swing.common.ui.combobox.MaximumMatch
   */
  protected final SteppedComboBox createComboBox(final String propertyId, final ComboBoxModel comboBoxModel,
                                                 final boolean maximumMatch) {
    return createComboBox(propertyId, comboBoxModel, maximumMatch, null);
  }

  /**
   * Creates a SteppedComboBox bound to the property identified by {@code propertyId}
   * @param propertyId the ID of the property to bind
   * @param comboBoxModel the ComboBoxModel
   * @param maximumMatch true if maximum match should be used
   * @param enabledState a state for controlling the enabled state of the component
   * @return a SteppedComboBox bound the the property
   * @see org.jminor.swing.common.ui.combobox.MaximumMatch
   */
  protected final SteppedComboBox createComboBox(final String propertyId, final ComboBoxModel comboBoxModel,
                                                 final boolean maximumMatch, final StateObserver enabledState) {
    return createComboBox(getEditModel().getEntityDefinition().getProperty(propertyId),
            comboBoxModel, maximumMatch, enabledState);
  }

  /**
   * Creates a SteppedComboBox bound to the given property
   * @param property the property to bind
   * @param comboBoxModel the ComboBoxModel
   * @param maximumMatch true if maximum match should be used
   * @return a SteppedComboBox bound the the property
   * @see org.jminor.swing.common.ui.combobox.MaximumMatch
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
   * @see org.jminor.swing.common.ui.combobox.MaximumMatch
   */
  protected final SteppedComboBox createComboBox(final Property property, final ComboBoxModel comboBoxModel,
                                                 final boolean maximumMatch, final StateObserver enabledState) {
    final SteppedComboBox comboBox = EntityInputComponents.createComboBox(property, getEditModel().value(property.getPropertyId()),
            comboBoxModel, enabledState);
    if (maximumMatch) {
      MaximumMatch.enable(comboBox);
    }
    if (TRANSFER_FOCUS_ON_ENTER.get()) {
      transferFocusOnEnter((JComponent) comboBox.getEditor().getEditorComponent());
      transferFocusOnEnter(comboBox);
    }
    setComponent(property.getPropertyId(), comboBox);

    return comboBox;
  }

  /**
   * Creates a SteppedComboBox containing the values defined by the given value list property,
   * bound to the given property.
   * @param propertyId the propertyId
   * @return a SteppedComboBox bound to the property
   * @throws IllegalArgumentException in case the property is not a value list property
   */
  protected final SteppedComboBox createValueListComboBox(final String propertyId) {
    return createValueListComboBox(propertyId, true);
  }

  /**
   * Creates a SteppedComboBox containing the values defined by the given value list property,
   * bound to the given property.
   * @param propertyId the propertyId
   * @param sortItems if true the items are sorted, otherwise the original ordering is preserved
   * @return a SteppedComboBox bound to the property
   * @throws IllegalArgumentException in case the property is not a value list property
   */
  protected final SteppedComboBox createValueListComboBox(final String propertyId, final boolean sortItems) {
    return createValueListComboBox(propertyId, sortItems, null);
  }

  /**
   * Creates a SteppedComboBox containing the values defined by the given value list property,
   * bound to the given property.
   * @param propertyId the propertyId
   * @param enabledState a state for controlling the enabled state of the component
   * @return a SteppedComboBox bound to the property
   * @throws IllegalArgumentException in case the property is not a value list property
   */
  protected final SteppedComboBox createValueListComboBox(final String propertyId, final StateObserver enabledState) {
    return createValueListComboBox(propertyId, true, enabledState);
  }

  /**
   * Creates a SteppedComboBox containing the values defined by the given value list property,
   * bound to the given property.
   * @param propertyId the propertyId
   * @param sortItems if true the items are sorted, otherwise the original ordering is preserved
   * @param enabledState a state for controlling the enabled state of the component
   * @return a SteppedComboBox bound to the property
   * @throws IllegalArgumentException in case the property is not a value list property
   */
  protected final SteppedComboBox createValueListComboBox(final String propertyId, final boolean sortItems, final StateObserver enabledState) {
    final Property property = getEditModel().getEntityDefinition().getProperty(propertyId);
    if (!(property instanceof ValueListProperty)) {
      throw new IllegalArgumentException("Property identified by '" + propertyId + "' is not a ValueListProperty");
    }

    return createValueListComboBox((ValueListProperty) property, sortItems, enabledState);
  }

  /**
   * Creates a SteppedComboBox containing the values defined in the given value list property,
   * bound to the given property.
   * @param property the property
   * @return a SteppedComboBox bound to the property
   */
  protected final SteppedComboBox createValueListComboBox(final ValueListProperty property) {
    return createValueListComboBox(property, true);
  }

  /**
   * Creates a SteppedComboBox containing the values defined in the given value list property,
   * bound to the given property.
   * @param property the property
   * @param sortItems if true the items are sorted, otherwise the original ordering is preserved
   * @return a SteppedComboBox bound to the property
   */
  protected final SteppedComboBox createValueListComboBox(final ValueListProperty property, final boolean sortItems) {
    return createValueListComboBox(property, sortItems, null);
  }

  /**
   * Creates a SteppedComboBox containing the values defined in the given value list property,
   * bound to the given property.
   * @param property the property
   * @param enabledState a state for controlling the enabled state of the component
   * @return a SteppedComboBox bound to the property
   */
  protected final SteppedComboBox createValueListComboBox(final ValueListProperty property, final StateObserver enabledState) {
    return createValueListComboBox(property, true, enabledState);
  }

  /**
   * Creates a SteppedComboBox containing the values defined in the given value list property,
   * bound to the given property.
   * @param property the property
   * @param sortItems if true the items are sorted, otherwise the original ordering is preserved
   * @param enabledState a state for controlling the enabled state of the component
   * @return a SteppedComboBox bound to the property
   */
  protected final SteppedComboBox createValueListComboBox(final ValueListProperty property, final boolean sortItems,
                                                          final StateObserver enabledState) {
    final SteppedComboBox box = EntityInputComponents.createValueListComboBox(property, getEditModel().value(property.getPropertyId()),
            sortItems, enabledState);
    if (TRANSFER_FOCUS_ON_ENTER.get()) {
      transferFocusOnEnter((JComponent) box.getEditor().getEditorComponent());
      transferFocusOnEnter(box);
    }
    setComponent(property.getPropertyId(), box);

    return box;
  }

  /**
   * Creates an editable SteppedComboBox bound to the property identified by {@code propertyId}
   * @param propertyId the ID of the property to bind
   * @param comboBoxModel the ComboBoxModel
   * @return an editable SteppedComboBox bound the the property
   */
  protected final SteppedComboBox createEditableComboBox(final String propertyId, final ComboBoxModel comboBoxModel) {
    return createEditableComboBox(propertyId, comboBoxModel, null);
  }

  /**
   * Creates an editable SteppedComboBox bound to the property identified by {@code propertyId}
   * @param propertyId the ID of the property to bind
   * @param comboBoxModel the ComboBoxModel
   * @param enabledState a state for controlling the enabled state of the component
   * @return an editable SteppedComboBox bound the the property
   */
  protected final SteppedComboBox createEditableComboBox(final String propertyId, final ComboBoxModel comboBoxModel,
                                                         final StateObserver enabledState) {
    return createEditableComboBox(getEditModel().getEntityDefinition().getProperty(propertyId),
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
                                                         final StateObserver enabledState) {
    final SteppedComboBox comboBox = EntityInputComponents.createComboBox(property, getEditModel().value(property.getPropertyId()),
            comboBoxModel, enabledState, true);
    if (TRANSFER_FOCUS_ON_ENTER.get()) {
      transferFocusOnEnter((JComponent) comboBox.getEditor().getEditorComponent());
      transferFocusOnEnter(comboBox);
    }
    setComponent(property.getPropertyId(), comboBox);

    return comboBox;
  }

  /**
   * Creates a SteppedComboBox bound to the property identified by {@code propertyId}, the combo box
   * contains the underlying values of the property
   * @param propertyId the ID of the property to bind
   * @return a SteppedComboBox bound to the property
   */
  protected final SteppedComboBox createPropertyComboBox(final String propertyId) {
    return createPropertyComboBox(propertyId, null);
  }

  /**
   * Creates a SteppedComboBox bound to the property identified by {@code propertyId}, the combo box
   * contains the underlying values of the property
   * @param propertyId the ID of the property to bind
   * @param enabledState a state for controlling the enabled state of the component
   * @return a SteppedComboBox bound to the property
   */
  protected final SteppedComboBox createPropertyComboBox(final String propertyId, final StateObserver enabledState) {
    return createPropertyComboBox(propertyId, enabledState, false);
  }

  /**
   * Creates a SteppedComboBox bound to the property identified by {@code propertyId}, the combo box
   * contains the underlying values of the property
   * @param propertyId the ID of the property to bind
   * @param enabledState a state for controlling the enabled state of the component
   * @param editable true if the combo box should be editable, only works with combo boxes based on String.class properties
   * @return a SteppedComboBox bound to the property
   */
  protected final SteppedComboBox createPropertyComboBox(final String propertyId, final StateObserver enabledState,
                                                         final boolean editable) {
    return createPropertyComboBox(getEditModel().getEntityDefinition().getColumnProperty(propertyId),
            enabledState, editable);
  }

  /**
   * Creates a SteppedComboBox bound to the given property, the combo box
   * contains the underlying values of the property
   * @param property the property to bind
   * @return a SteppedComboBox bound to the property
   */
  protected final SteppedComboBox createPropertyComboBox(final ColumnProperty property) {
    return createPropertyComboBox(property, null);
  }

  /**
   * Creates a SteppedComboBox bound to the given property, the combo box
   * contains the underlying values of the property
   * @param property the property to bind
   * @param enabledState a state for controlling the enabled state of the component
   * @return a SteppedComboBox bound to the property
   */
  protected final SteppedComboBox createPropertyComboBox(final ColumnProperty property, final StateObserver enabledState) {
    return createPropertyComboBox(property, enabledState, false);
  }

  /**
   * Creates a SteppedComboBox bound to the given property, the combo box
   * contains the underlying values of the property
   * @param property the property to bind
   * @param enabledState a state for controlling the enabled state of the component
   * @param editable true if the combo box should be editable, only works with combo boxes based on String.class properties
   * @return a SteppedComboBox bound to the property
   */
  protected final SteppedComboBox createPropertyComboBox(final ColumnProperty property, final StateObserver enabledState,
                                                         final boolean editable) {
    final SteppedComboBox comboBox = EntityInputComponents.createPropertyComboBox(property, getEditModel().value(property.getPropertyId()),
            (ComboBoxModel) getEditModel().getComboBoxModel(property.getPropertyId()), enabledState, editable);
    if (TRANSFER_FOCUS_ON_ENTER.get()) {
      transferFocusOnEnter((JComponent) comboBox.getEditor().getEditorComponent());
      transferFocusOnEnter(comboBox);
    }
    setComponent(property.getPropertyId(), comboBox);

    return comboBox;
  }

  /**
   * Creates a EntityComboBox bound to the foreign key property identified by {@code foreignKeyPropertyId}
   * @param foreignKeyPropertyId the ID of the property to bind
   * @param enabledState a state for controlling the enabled state of the component
   * @return a EntityComboBox bound to the property
   */
  protected final EntityComboBox createForeignKeyComboBox(final String foreignKeyPropertyId, final StateObserver enabledState) {
    return createForeignKeyComboBox((ForeignKeyProperty)
            getEditModel().getEntityDefinition().getProperty(foreignKeyPropertyId), enabledState);
  }

  /**
   * Creates an EntityComboBox bound to the foreign key property identified by {@code foreignKeyPropertyId}
   * @param foreignKeyPropertyId the ID of the foreign key property to bind
   * combination used to create new instances of the entity this EntityComboBox is based on
   * EntityComboBox is focusable
   * @return an EntityComboBox bound to the property
   */
  protected final EntityComboBox createForeignKeyComboBox(final String foreignKeyPropertyId) {
    return createForeignKeyComboBox(getEditModel().getEntityDefinition().getForeignKeyProperty(
            foreignKeyPropertyId), null);
  }

  /**
   * Creates an EntityComboBox bound to the given foreign key property
   * @param foreignKeyProperty the foreign key property to bind
   * @return an EntityComboBox bound to the property
   */
  protected final EntityComboBox createForeignKeyComboBox(final ForeignKeyProperty foreignKeyProperty) {
    return createForeignKeyComboBox(foreignKeyProperty, null);
  }

  /**
   * Creates an EntityComboBox bound to the given foreign key property
   * @param foreignKeyProperty the foreign key property to bind
   * combination used to create new instances of the entity this EntityComboBox is based on
   * EntityComboBox is focusable
   * @param enabledState a state for controlling the enabled state of the component
   * @return an EntityComboBox bound to the property
   */
  protected final EntityComboBox createForeignKeyComboBox(final ForeignKeyProperty foreignKeyProperty,
                                                          final StateObserver enabledState) {
    final EntityComboBox comboBox = EntityInputComponents.createForeignKeyComboBox(foreignKeyProperty,
            getEditModel().value(foreignKeyProperty.getPropertyId()),
            getEditModel().getForeignKeyComboBoxModel(foreignKeyProperty), enabledState);
    if (TRANSFER_FOCUS_ON_ENTER.get()) {
      //getEditor().getEditorComponent() only required because the combo box is editable, due to AutoCompletion
      transferFocusOnEnter((JComponent) comboBox.getEditor().getEditorComponent());
    }
    setComponent(foreignKeyProperty.getPropertyId(), comboBox);

    return comboBox;
  }

  /**
   * Creates an EntityLookupField bound to the property identified by {@code foreignKeypropertyId}, the property
   * must be an Property.ForeignKeyProperty
   * @param foreignKeyPropertyId the ID of the foreign key property to bind
   * @return an EntityLookupField bound the property
   */
  protected final EntityLookupField createForeignKeyLookupField(final String foreignKeyPropertyId) {
    return createForeignKeyLookupField(foreignKeyPropertyId, null);
  }

  /**
   * Creates an EntityLookupField bound to the property identified by {@code foreignKeypropertyId}, the property
   * must be an Property.ForeignKeyProperty
   * @param foreignKeyPropertyId the ID of the foreign key property to bind
   * @param enabledState a state for controlling the enabled state of the component
   * @return an EntityLookupField bound the property
   */
  protected final EntityLookupField createForeignKeyLookupField(final String foreignKeyPropertyId,
                                                                final StateObserver enabledState) {
    final ForeignKeyProperty fkProperty =
            getEditModel().getEntityDefinition().getForeignKeyProperty(foreignKeyPropertyId);

    return createForeignKeyLookupField(fkProperty, enabledState);
  }

  /**
   * Creates an EntityLookupField bound to the given foreign key property
   * @param foreignKeyProperty the foreign key property to bind
   * @return an EntityLookupField bound the property
   */
  protected final EntityLookupField createForeignKeyLookupField(final ForeignKeyProperty foreignKeyProperty) {
    return createForeignKeyLookupField(foreignKeyProperty, null);
  }

  /**
   * Creates an EntityLookupField bound to the given foreign key property
   * @param foreignKeyProperty the foreign key property to bind
   * @param enabledState a state for controlling the enabled state of the component
   * @return an EntityLookupField bound the property
   */
  protected final EntityLookupField createForeignKeyLookupField(final ForeignKeyProperty foreignKeyProperty,
                                                                final StateObserver enabledState) {
    final EntityLookupField lookupField = EntityInputComponents.createForeignKeyLookupField(foreignKeyProperty,
            getEditModel().value(foreignKeyProperty.getPropertyId()),
            getEditModel().getForeignKeyLookupModel(foreignKeyProperty), enabledState);
    if (TRANSFER_FOCUS_ON_ENTER.get()) {
      lookupField.setTransferFocusOnEnter();
    }
    setComponent(foreignKeyProperty.getPropertyId(), lookupField);

    return lookupField;
  }

  /**
   * Creates an uneditable JTextField bound to the property identified by {@code propertyId}
   * @param propertyId the ID of the property to bind
   * @return an uneditable JTextField bound to the property
   */
  protected final JTextField createForeignKeyField(final String propertyId) {
    return createForeignKeyField(getEditModel().getEntityDefinition().getForeignKeyProperty(propertyId));
  }

  /**
   * Creates an uneditable JTextField bound to the given property
   * @param foreignKeyProperty the foreign key property to bind
   * @return an uneditable JTextField bound to the property
   */
  protected final JTextField createForeignKeyField(final ForeignKeyProperty foreignKeyProperty) {
    requireNonNull(foreignKeyProperty, "foreignKeyProperty");
    final JTextField textField = new JTextField();
    textField.setEditable(false);
    textField.setFocusable(false);
    textField.setToolTipText(foreignKeyProperty.getDescription());
    new ForeignKeyModelValue(getEditModel(), foreignKeyProperty.getPropertyId()).link(TextValues.textValue(textField));
    if (TRANSFER_FOCUS_ON_ENTER.get()) {
      transferFocusOnEnter(textField);
    }
    setComponent(foreignKeyProperty.getPropertyId(), textField);

    return textField;
  }

  /**
   * Creates a JLabel with a caption from the property identified by {@code propertyId}, if a input component exists
   * for the given property this label is associated with it via {@link JLabel#setLabelFor(Component)}.
   * @param propertyId the ID of the property from which to retrieve the caption
   * @return a JLabel for the given property
   */
  protected final JLabel createLabel(final String propertyId) {
    return createLabel(propertyId, JLabel.LEFT);
  }

  /**
   * Creates a JLabel with a caption from the property identified by {@code propertyId}, if an input component exists
   * for the given property this label is associated with it via {@link JLabel#setLabelFor(Component)}.
   * @param propertyId the ID of the property from which to retrieve the caption
   * @param horizontalAlignment the horizontal text alignment
   * @return a JLabel for the given property
   */
  protected final JLabel createLabel(final String propertyId, final int horizontalAlignment) {
    return setLabelForComponent(EntityInputComponents.createLabel(getEditModel().getEntityDefinition()
            .getProperty(propertyId), horizontalAlignment), getComponent(propertyId));
  }

  /**
   * @return the component that should get the initial focus when the UI is cleared
   */
  protected JComponent getInitialFocusComponent() {
    if (initialFocusComponent != null) {
      return initialFocusComponent;
    }

    if (initialFocusPropertyId != null) {
      return getComponent(initialFocusPropertyId);
    }

    return null;
  }

  /**
   * @return the component that should get the focus when the UI is prepared after insert
   */
  protected JComponent getAfterInsertFocusComponent() {
    if (afterInsertFocusComponent != null) {
      return afterInsertFocusComponent;
    }

    if (afterInsertFocusPropertyId != null) {
      return getComponent(afterInsertFocusPropertyId);
    }

    return getInitialFocusComponent();
  }

  protected final void requestAfterInsertFocus() {
    requestFocus(getAfterInsertFocusComponent());
  }

  private void requestFocus(final JComponent component) {
    if (component != null && component.isFocusable()) {
      component.requestFocus();
    }
    else {
      requestFocus();
    }
  }

  /**
   * @return a list of propertyIds to use when selecting a input component in this panel,
   * this returns all propertyIds that have mapped components in this panel
   * that are enabled, displayable, visible and focusable.
   * @see #excludeComponentFromSelection(String)
   * @see #setComponent(String, javax.swing.JComponent)
   */
  private List<String> getSelectComponentPropertyIds() {
    final List<String> propertyIds = getComponentPropertyIds();
    propertyIds.removeIf(propertyId -> {
      final JComponent component = getComponent(propertyId);

      return component == null || excludeFromSelection.contains(propertyId) || !component.isDisplayable() ||
              !component.isVisible() || !component.isFocusable() || !component.isEnabled();
    });

    return propertyIds;
  }

  private static JLabel setLabelForComponent(final JLabel label, final JComponent component) {
    if (component != null && label.getLabelFor() != component) {
      label.setLabelFor(component);
    }

    return label;
  }

  private static final class ForeignKeyModelValue extends AbstractValue<String> {

    private final EntityEditModel editModel;
    private final String foreignKeyPropertyId;

    private ForeignKeyModelValue(final EntityEditModel editModel, final String foreignKeyPropertyId) {
      this.editModel = editModel;
      this.foreignKeyPropertyId = foreignKeyPropertyId;
      editModel.addValueListener(foreignKeyPropertyId, valueChange -> notifyValueChange());
    }

    @Override
    public void set(final String value) {/*read only*/}

    @Override
    public String get() {
      final Entity value = editModel.getForeignKey(foreignKeyPropertyId);

      return value == null ? "" : value.toString();
    }

    @Override
    public boolean isNullable() {
      return false;
    }
  }
}
