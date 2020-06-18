/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui;

import is.codion.common.Configuration;
import is.codion.common.i18n.Messages;
import is.codion.common.item.Item;
import is.codion.common.state.StateObserver;
import is.codion.common.value.AbstractValue;
import is.codion.common.value.PropertyValue;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.property.ForeignKeyProperty;
import is.codion.framework.domain.property.Properties;
import is.codion.framework.domain.property.Property;
import is.codion.framework.domain.property.ValueListProperty;
import is.codion.framework.model.EntityEditModel;
import is.codion.swing.common.ui.checkbox.NullableCheckBox;
import is.codion.swing.common.ui.combobox.MaximumMatch;
import is.codion.swing.common.ui.combobox.SteppedComboBox;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.common.ui.layout.Layouts;
import is.codion.swing.common.ui.textfield.TextFields.ValueContainsLiterals;
import is.codion.swing.common.ui.textfield.TextInputPanel;
import is.codion.swing.common.ui.textfield.TextInputPanel.ButtonFocusable;
import is.codion.swing.common.ui.time.LocalDateInputPanel;
import is.codion.swing.common.ui.time.LocalDateTimeInputPanel;
import is.codion.swing.common.ui.time.TemporalInputPanel;
import is.codion.swing.common.ui.time.TemporalInputPanel.CalendarButton;
import is.codion.swing.common.ui.value.TextValues;
import is.codion.swing.common.ui.value.UpdateOn;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.EntityInputComponents.Editable;
import is.codion.swing.framework.ui.EntityInputComponents.IncludeCaption;
import is.codion.swing.framework.ui.EntityInputComponents.Sorted;

import javax.swing.ComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static is.codion.swing.common.ui.KeyEvents.transferFocusOnEnter;
import static java.util.Objects.requireNonNull;

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
          "is.codion.swing.framework.ui.EntityEditComponentPanel.transferFocusOnEnter", true);

  /**
   * The edit model these edit components are associated with
   */
  private final SwingEntityEditModel editModel;

  /**
   * Input components mapped to their respective attributes
   */
  private final Map<Attribute<?>, JComponent> components = new HashMap<>();

  /**
   * Attributes that should be excluded when presenting the component selection
   */
  private final Set<Attribute<?>> excludeFromSelection = new HashSet<>();

  /**
   * The component that should receive focus when the UI is initialized
   */
  private JComponent initialFocusComponent;

  /**
   * The attribute for which component should receive the focus when the UI is prepared
   */
  private Attribute<?> initialFocusAttribute;

  /**
   * The component that should receive focus when the UI is prepared after insert
   */
  private JComponent afterInsertFocusComponent;

  /**
   * The attribute for which component should receive the focus when the UI is prepared after insert
   */
  private Attribute<?> afterInsertFocusAttribute;

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
   * @return the attributes that have been associated with components.
   */
  public final List<Attribute<?>> getComponentAttributes() {
    return new ArrayList<>(components.keySet());
  }

  /**
   * @param attribute the attribute
   * @param <T> the attribute type
   * @return the component associated with the given attribute, null if no component has been
   * associated with the given attribute
   */
  public final <T> JComponent getComponent(final Attribute<T> attribute) {
    return components.get(attribute);
  }

  /**
   * @param component the component
   * @param <T> the attribute type
   * @return the attribute the given component is associated with, null if the component has not been
   * associated with a attribute
   */
  public final <T> Attribute<T> getAttribute(final JComponent component) {
    return (Attribute<T>) components.entrySet().stream().filter(entry -> entry.getValue() == component)
            .findFirst().map(Map.Entry::getKey).orElse(null);
  }

  /**
   * Sets the component that should receive the focus when the UI is cleared or activated.
   * Overrides the value set via {@link #setInitialFocusAttribute(Attribute)}
   * @param initialFocusComponent the component
   * @return the component
   */
  public final JComponent setInitialFocusComponent(final JComponent initialFocusComponent) {
    this.initialFocusComponent = initialFocusComponent;
    return initialFocusComponent;
  }

  /**
   * Sets the component associated with the given attribute as the component
   * that should receive the initial focus in this edit panel.
   * This is overridden by setInitialFocusComponent().
   * @param attribute the component attribute
   * @param <T> the attribute type
   * @see #setInitialFocusComponent(javax.swing.JComponent)
   */
  public final <T> void setInitialFocusAttribute(final Attribute<T> attribute) {
    this.initialFocusAttribute = attribute;
  }

  /**
   * Sets the component that should receive the focus after an insert has been performed..
   * Overrides the value set via {@link #setAfterInsertFocusAttribute(Attribute)}
   * @param afterInsertFocusComponent the component
   * @return the component
   */
  public final JComponent setAfterInsertFocusComponent(final JComponent afterInsertFocusComponent) {
    this.afterInsertFocusComponent = afterInsertFocusComponent;
    return afterInsertFocusComponent;
  }

  /**
   * Sets the component associated with the given attribute as the component
   * that should receive the focus after an insert is performed in this edit panel.
   * This is overridden by setAfterInsertFocusComponent().
   * @param <T> the attribute type
   * @param attribute the component attribute
   * @see #setAfterInsertFocusComponent(JComponent)
   */
  public final <T> void setAfterInsertFocusAttribute(final Attribute<T> attribute) {
    this.afterInsertFocusAttribute = attribute;
  }

  /**
   * Sets the initial focus, if a initial focus component or component attribute
   * has been set that component receives the focus, if not, or if that component
   * is not focusable, this panel receives the focus.
   * Note that if this panel is not visible nothing happens.
   * @see #isVisible()
   * @see #setInitialFocusAttribute
   * @see #setInitialFocusComponent(javax.swing.JComponent)
   */
  public final void requestInitialFocus() {
    if (isVisible()) {
      requestFocus(getInitialFocusComponent());
    }
  }

  /**
   * Request focus for the component associated with the given attribute
   * @param attribute the attribute of the component to select
   * @param <T> the attribute type
   */
  public final <T> void requestComponentFocus(final Attribute<T> attribute) {
    final JComponent component = getComponent(attribute);
    if (component != null) {
      component.requestFocus();
    }
  }

  /**
   * Displays a dialog allowing the user the select a input component which should receive the keyboard focus,
   * if only one input component is available then that component is selected automatically.
   * @see #excludeComponentFromSelection(Attribute)
   * @see #requestComponentFocus(Attribute)
   */
  public void selectInputComponent() {
    final List<Property<?>> properties =
            Properties.sort(getEditModel().getEntityDefinition().getProperties(getSelectComponentAttributes()));
    final Property<?> property = properties.size() == 1 ?  properties.get(0) :
            Dialogs.selectValue(this, properties, Messages.get(Messages.SELECT_INPUT_FIELD));
    if (property != null) {
      requestComponentFocus(property.getAttribute());
    }
  }

  /**
   * Specifies that the given attribute should be excluded when presenting a component selection list.
   * @param attribute the attribute to exclude from selection
   * @param <T> the attribute type
   */
  public final <T> void excludeComponentFromSelection(final Attribute<T> attribute) {
    getEditModel().getEntityDefinition().getProperty(attribute);//just validating that the attribute exists
    excludeFromSelection.add(attribute);
  }

  /**
   * Associates the given input component with the given attribute.
   * @param attribute the attribute
   * @param component the input component
   * @param <T> the attribute type
   */
  protected final <T> void setComponent(final Attribute<T> attribute, final JComponent component) {
    getEditModel().getEntityDefinition().getProperty(attribute);
    requireNonNull(component, "component");
    components.put(attribute, component);
  }

  /**
   * Adds a panel for the given attribute to this panel
   * @param attribute the attribute
   * @param <T> the attribute type
   * @see #createInputPanel(Attribute)
   */
  protected final <T> void addInputPanel(final Attribute<T> attribute) {
    add(createInputPanel(attribute));
  }

  /**
   * Creates a panel containing a label and the component associated with the given attribute.
   * The label text is the caption of the property based on {@code attribute}.
   * The default layout of the resulting panel is with the label on top and inputComponent below.
   * @param attribute the attribute from which property to retrieve the label caption
   * @param <T> the attribute type
   * @return a panel containing a label and a component
   * @throws IllegalArgumentException in case no component has been associated with the given attribute
   */
  protected final <T> JPanel createInputPanel(final Attribute<T> attribute) {
    final JComponent component = getComponent(attribute);
    if (component == null) {
      throw new IllegalArgumentException("No component associated with attribute: " + attribute);
    }

    return createInputPanel(attribute, component);
  }

  /**
   * Creates a panel containing a label and the component associated with the given attribute.
   * The label text is the caption of the property based on {@code attribute}.
   * The default layout of the resulting panel is with the label on top and {@code inputComponent} below.
   * @param attribute the attribute from which property to retrieve the label caption
   * @param inputComponent a component bound to the property with id {@code attribute}
   * @param <T> the attribute type
   * @return a panel containing a label and a component
   */
  protected final <T> JPanel createInputPanel(final Attribute<T> attribute, final JComponent inputComponent) {
    return createInputPanel(attribute, inputComponent, BorderLayout.NORTH);
  }

  /**
   * Creates a panel containing a label and the component associated with the given attribute.
   * The label text is the caption of the property based on {@code attribute}.
   * @param attribute the attribute from which property to retrieve the label caption
   * @param inputComponent a component bound to the property with id {@code attribute}
   * @param labelBorderLayoutConstraints {@link BorderLayout#NORTH}, {@link BorderLayout#SOUTH},
   * {@link BorderLayout#EAST} or {@link BorderLayout#WEST}
   * @param <T> the attribute type
   * @return a panel containing a label and a component
   */
  protected final <T> JPanel createInputPanel(final Attribute<T> attribute, final JComponent inputComponent,
                                              final String labelBorderLayoutConstraints) {
    return createInputPanel(attribute, inputComponent, labelBorderLayoutConstraints, JLabel.LEADING);
  }

  /**
   * Creates a panel containing a label and the given component.
   * The label text is the caption of {@code attribute}.
   * @param attribute the attribute from which property to retrieve the label caption
   * @param inputComponent a component bound to the property with id {@code attribute}
   * @param labelBorderLayoutConstraints {@link BorderLayout#NORTH}, {@link BorderLayout#SOUTH},
   * {@link BorderLayout#EAST} or {@link BorderLayout#WEST}
   * @param labelAlignment the label alignment
   * @param <T> the attribute type
   * @return a panel containing a label and a component
   */
  protected final <T> JPanel createInputPanel(final Attribute<T> attribute, final JComponent inputComponent,
                                              final String labelBorderLayoutConstraints, final int labelAlignment) {
    return createInputPanel(createLabel(attribute, labelAlignment), inputComponent, labelBorderLayoutConstraints);
  }

  /**
   * Creates a panel containing a label component and the {@code inputComponent} with the label
   * component positioned above the input component.
   * @param labelComponent the label component
   * @param inputComponent a input component
   * @return a panel containing a label and a component
   */
  protected final JPanel createInputPanel(final JComponent labelComponent, final JComponent inputComponent) {
    return createInputPanel(labelComponent, inputComponent, BorderLayout.NORTH);
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
  protected final JPanel createInputPanel(final JComponent labelComponent, final JComponent inputComponent,
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
   * Creates a JTextArea component bound to {@code attribute}.
   * @param attribute the attribute to bind
   * @return a JTextArea bound to the attribute
   */
  protected final JTextArea createTextArea(final Attribute<String> attribute) {
    return createTextArea(attribute, -1, -1);
  }

  /**
   * Creates a JTextArea component bound to {@code attribute}.
   * @param attribute the attribute to bind
   * @param rows the number of rows in the text area
   * @param columns the number of columns in the text area
   * @return a JTextArea bound to the attribute
   */
  protected final JTextArea createTextArea(final Attribute<String> attribute, final int rows, final int columns) {
    return createTextArea(attribute, rows, columns, UpdateOn.KEYSTROKE);
  }

  /**
   * Creates a JTextArea component bound to {@code attribute}.
   * @param attribute the attribute to bind
   * @param rows the number of rows in the text area
   * @param columns the number of columns in the text area
   * @param updateOn specifies when the underlying value should be updated
   * @return a JTextArea bound to the attribute
   */
  protected final JTextArea createTextArea(final Attribute<String> attribute, final int rows, final int columns,
                                           final UpdateOn updateOn) {
    return createTextArea(attribute, rows, columns, updateOn, null);
  }

  /**
   * Creates a JTextArea component bound to {@code attribute}.
   * @param attribute the attribute to bind
   * @param rows the number of rows in the text area
   * @param columns the number of columns in the text area
   * @param updateOn specifies when the underlying value should be updated
   * @param enabledState a state indicating when this text area should be enabled
   * @return a JTextArea bound to the attribute
   */
  protected final JTextArea createTextArea(final Attribute<String> attribute, final int rows, final int columns,
                                           final UpdateOn updateOn, final StateObserver enabledState) {
    final Property<String> property = getEditModel().getEntityDefinition().getProperty(attribute);
    final JTextArea textArea = EntityInputComponents.createTextArea(property, getEditModel().value(attribute),
            rows, columns, updateOn, enabledState);
    EntityComponentValidators.addValidator(attribute, textArea, getEditModel());
    setComponent(attribute, textArea);

    return textArea;
  }

  /**
   * Creates a TextInputPanel bound to {@code attribute}.
   * @param attribute the attribute to bind
   * @return a TextInputPanel bound to the attribute
   */
  protected final TextInputPanel createTextInputPanel(final Attribute<String> attribute) {
    return createTextInputPanel(attribute, UpdateOn.KEYSTROKE, ButtonFocusable.YES);
  }

  /**
   * Creates a TextInputPanel bound to {@code attribute}.
   * @param attribute the attribute to bind
   * @param updateOn specifies when the underlying value should be updated
   * @param buttonFocusable specifies whether the edit button should be focusable.
   * @return a TextInputPanel bound to the attribute
   */
  protected final TextInputPanel createTextInputPanel(final Attribute<String> attribute, final UpdateOn updateOn,
                                                      final ButtonFocusable buttonFocusable) {
    final TextInputPanel inputPanel = EntityInputComponents.createTextInputPanel(
            getEditModel().getEntityDefinition().getProperty(attribute),
            getEditModel().value(attribute), updateOn, buttonFocusable);
    if (TRANSFER_FOCUS_ON_ENTER.get()) {
      transferFocusOnEnter(inputPanel.getTextField());
      if (inputPanel.getButton() != null) {
        transferFocusOnEnter(inputPanel.getButton());
      }
    }
    setComponent(attribute, inputPanel);

    return inputPanel;
  }

  /**
   * Creates a new TemporalInputPanel using the default short date format, bound to the attribute
   * identified by {@code attribute}.
   * @param attribute the attribute for which to create the panel
   * @param <T> the attribute type
   * @return a TemporalInputPanel
   * @see Property#DATE_FORMAT
   */
  protected final <T extends Temporal> TemporalInputPanel<T> createTemporalInputPanel(final Attribute<T> attribute) {
    return createTemporalInputPanel(attribute, CalendarButton.YES);
  }

  /**
   * Creates a new TemporalInputPanel bound to {@code attribute}.
   * @param attribute the attribute for which to create the panel
   * @param calendarButton if yes a button for visually editing the date is included
   * @param <T> the attribute type
   * @return a TemporalInputPanel
   * @see Property#DATE_FORMAT
   */
  protected final <T extends Temporal> TemporalInputPanel<T> createTemporalInputPanel(final Attribute<T> attribute, final CalendarButton calendarButton) {
    return createTemporalInputPanel(attribute, calendarButton, null);
  }

  /**
   * Creates a new TemporalInputPanel bound to {@code attribute}.
   * @param attribute the attribute for which to create the panel
   * @param calendarButton if yes a button for visually editing the date is included
   * @param enabledState a state for controlling the enabled state of the input component
   * @param <T> the attribute type
   * @return a TemporalInputPanel bound to the attribute
   */
  protected final <T extends Temporal> TemporalInputPanel<T> createTemporalInputPanel(final Attribute<T> attribute, final CalendarButton calendarButton,
                                                                                      final StateObserver enabledState) {
    return createTemporalInputPanel(attribute, calendarButton, enabledState, UpdateOn.KEYSTROKE);
  }

  /**
   * Creates a new TemporalInputPanel bound to {@code attribute}.
   * @param attribute the attribute for which to create the panel
   * @param calendarButton if yes a button for visually editing the date is included
   * @param enabledState a state for controlling the enabled state of the input component
   * @param updateOn specifies when the underlying value should be updated
   * @param <T> the attribute type
   * @return a TemporalInputPanel bound to the attribute
   */
  protected final <T extends Temporal> TemporalInputPanel<T> createTemporalInputPanel(final Attribute<T> attribute, final CalendarButton calendarButton,
                                                                                      final StateObserver enabledState, final UpdateOn updateOn) {
    final TemporalInputPanel<T> panel = EntityInputComponents.createTemporalInputPanel(
            getEditModel().getEntityDefinition().getProperty(attribute),
            getEditModel().value(attribute), updateOn, calendarButton, enabledState);
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
    setComponent(attribute, panel);

    return panel;
  }

  /**
   * Creates a JTextField bound to {@code attribute}
   * @param attribute the attribute to bind
   * @param <T> the attribute type
   * @return a text field bound to the attribute
   */
  protected final <T> JTextField createTextField(final Attribute<T> attribute) {
    return createTextField(attribute, UpdateOn.KEYSTROKE);
  }

  /**
   * Creates a JTextField bound to {@code attribute}
   * @param attribute the attribute to bind
   * @param updateOn specifies when the underlying value should be updated
   * @param <T> the attribute type
   * @return a text field bound to the attribute
   */
  protected final <T> JTextField createTextField(final Attribute<T> attribute, final UpdateOn updateOn) {
    return createTextField(attribute, updateOn, null);
  }

  /**
   * Creates a JTextField bound to {@code attribute}
   * @param attribute the attribute to bind
   * @param updateOn specifies when the underlying value should be updated
   * @param enabledState a state for controlling the enabled state of the component
   * @param <T> the attribute type
   * @return a text field bound to the attribute
   */
  protected final <T> JTextField createTextField(final Attribute<T> attribute, final UpdateOn updateOn,
                                                 final StateObserver enabledState) {
    final Property<T> property = getEditModel().getEntityDefinition().getProperty(attribute);
    final JTextField textField = EntityInputComponents.createTextField(property,
            getEditModel().value(attribute), updateOn, enabledState);
    EntityComponentValidators.addValidator(attribute, textField, getEditModel());
    if (TRANSFER_FOCUS_ON_ENTER.get()) {
      transferFocusOnEnter(textField);
    }
    setComponent(attribute, textField);

    return textField;
  }

  /**
   * Creates a JFormattedTextField bound to {@code attribute}
   * @param attribute the attribute to bind
   * @param formatMaskString the format mask
   * @param valueContainsLiterals specifies whether or not the value should contain any literal characters
   * @return a text field bound to the attribute
   */
  protected final JFormattedTextField createMaskedTextField(final Attribute<String> attribute, final String formatMaskString,
                                                            final ValueContainsLiterals valueContainsLiterals) {
    return createMaskedTextField(attribute, formatMaskString, valueContainsLiterals, UpdateOn.KEYSTROKE);
  }

  /**
   * Creates a JFormattedTextField bound to {@code attribute}
   * @param attribute the attribute to bind
   * @param formatMaskString the format mask
   * @param valueContainsLiterals specifies whether or not the value should contain any literal characters
   * @param updateOn specifies when the underlying value should be updated
   * @return a text field bound to the attribute
   */
  protected final JFormattedTextField createMaskedTextField(final Attribute<String> attribute, final String formatMaskString,
                                                            final ValueContainsLiterals valueContainsLiterals, final UpdateOn updateOn) {
    return createMaskedTextField(attribute, formatMaskString, valueContainsLiterals, updateOn, null);
  }

  /**
   * Creates a JFormattedTextField bound to {@code attribute}
   * @param attribute the attribute to bind
   * @param formatMaskString the format mask
   * @param valueContainsLiterals specifies whether or not the value should contain any literal characters
   * @param updateOn specifies when the underlying value should be updated
   * @param enabledState a state for controlling the enabled state of the component, only applicable if {@code maskString} is specified
   * @return a text field bound to the attribute
   */
  protected final JFormattedTextField createMaskedTextField(final Attribute<String> attribute, final String formatMaskString,
                                                            final ValueContainsLiterals valueContainsLiterals, final UpdateOn updateOn,
                                                            final StateObserver enabledState) {
    requireNonNull(formatMaskString, "formatMaskString");
    final Property<String> property = getEditModel().getEntityDefinition().getProperty(attribute);
    final JFormattedTextField textField = EntityInputComponents.createMaskedTextField(property,
            getEditModel().value(attribute), formatMaskString, valueContainsLiterals, updateOn, enabledState);
    EntityComponentValidators.addFormattedValidator(attribute, textField, getEditModel());
    if (TRANSFER_FOCUS_ON_ENTER.get()) {
      transferFocusOnEnter(textField);
    }
    setComponent(attribute, textField);

    return textField;
  }

  /**
   * Creates a JCheckBox bound to {@code attribute}
   * @param attribute the attribute to bind
   * @return a JCheckBox bound to the attribute
   */
  protected final JCheckBox createCheckBox(final Attribute<Boolean> attribute) {
    return createCheckBox(attribute, (StateObserver) null);
  }

  /**
   * Creates a JCheckBox bound to {@code attribute}
   * @param attribute the attribute to bind
   * @param enabledState a state for controlling the enabled state of the component
   * @return a JCheckBox bound to the attribute
   */
  protected final JCheckBox createCheckBox(final Attribute<Boolean> attribute, final StateObserver enabledState) {
    return createCheckBox(attribute, enabledState, IncludeCaption.YES);
  }

  /**
   * Creates a JCheckBox bound to {@code attribute}
   * @param attribute the attribute to bind
   * @param includeCaption specifies whether or not the caption should be included
   * @return a JCheckBox bound to the attribute
   */
  protected final JCheckBox createCheckBox(final Attribute<Boolean> attribute, final IncludeCaption includeCaption) {
    return createCheckBox(attribute, null, includeCaption);
  }

  /**
   * Creates a JCheckBox bound to {@code attribute}
   * @param attribute the attribute to bind
   * @param enabledState a state for controlling the enabled state of the component
   * @param includeCaption specifies whether or not the caption should be included
   * @return a JCheckBox bound to the attribute
   */
  protected final JCheckBox createCheckBox(final Attribute<Boolean> attribute, final StateObserver enabledState,
                                           final IncludeCaption includeCaption) {
    final JCheckBox box = EntityInputComponents.createCheckBox(
            getEditModel().getEntityDefinition().getProperty(attribute),
            getEditModel().value(attribute), enabledState, includeCaption);
    if (TRANSFER_FOCUS_ON_ENTER.get()) {
      transferFocusOnEnter(box);
    }
    setComponent(attribute, box);

    return box;
  }

  /**
   * Creates a NullableCheckBox bound to {@code attribute}
   * @param attribute the attribute to bind
   * @return a NullableCheckBox bound to the attribute
   */
  protected final NullableCheckBox createNullableCheckBox(final Attribute<Boolean> attribute) {
    return createNullableCheckBox(attribute, null);
  }

  /**
   * Creates a NullableCheckBox bound to {@code attribute}
   * @param attribute the attribute to bind
   * @param enabledState a state for controlling the enabled state of the component
   * @return a NullableCheckBox bound to the attribute
   */
  protected final NullableCheckBox createNullableCheckBox(final Attribute<Boolean> attribute, final StateObserver enabledState) {
    return createNullableCheckBox(attribute, enabledState, IncludeCaption.YES);
  }

  /**
   * Creates a NullableCheckBox bound to {@code attribute}
   * @param attribute the attribute to bind
   * @param enabledState a state for controlling the enabled state of the component
   * @param includeCaption specifies whether or not the caption should be included
   * @return a NullableCheckBox bound to the attribute
   */
  protected final NullableCheckBox createNullableCheckBox(final Attribute<Boolean> attribute, final StateObserver enabledState,
                                                          final IncludeCaption includeCaption) {
    final NullableCheckBox box = EntityInputComponents.createNullableCheckBox(
            getEditModel().getEntityDefinition().getProperty(attribute),
            getEditModel().value(attribute), enabledState, includeCaption);
    if (TRANSFER_FOCUS_ON_ENTER.get()) {
      transferFocusOnEnter(box);
    }
    setComponent(attribute, box);

    return box;
  }

  /**
   * Create a JComboBox for {@code attribute}, containing
   * values for the boolean values: true, false, null
   * @param attribute the attribute to bind
   * @return JComboBox for the given attribute
   */
  protected final JComboBox<Item<Boolean>> createBooleanComboBox(final Attribute<Boolean> attribute) {
    return createBooleanComboBox(attribute, null);
  }

  /**
   * Create a JComboBox for {@code attribute}, containing
   * values for the boolean values: true, false, null
   * @param attribute the attribute to bind
   * @param enabledState a state for controlling the enabled state of the component
   * @return JComboBox for the given attribute
   */
  protected final JComboBox<Item<Boolean>> createBooleanComboBox(final Attribute<Boolean> attribute, final StateObserver enabledState) {
    final JComboBox<Item<Boolean>> comboBox = EntityInputComponents.createBooleanComboBox(
            getEditModel().getEntityDefinition().getProperty(attribute),
            getEditModel().value(attribute), enabledState);
    setComponent(attribute, comboBox);

    return comboBox;
  }

  /**
   * Creates a SteppedComboBox bound to {@code attribute}
   * @param attribute the attribute to bind
   * @param comboBoxModel the ComboBoxModel
   * @param maximumMatch true if maximum match should be used
   * @param <T> the attribute type
   * @return a SteppedComboBox bound the the attribute
   * @see is.codion.swing.common.ui.combobox.MaximumMatch
   */
  protected final <T> SteppedComboBox<T> createComboBox(final Attribute<T> attribute, final ComboBoxModel<T> comboBoxModel,
                                                        final boolean maximumMatch) {
    return createComboBox(attribute, comboBoxModel, maximumMatch, null);
  }

  /**
   * Creates a SteppedComboBox bound to {@code attribute}
   * @param attribute the attribute to bind
   * @param comboBoxModel the ComboBoxModel
   * @param maximumMatch true if maximum match should be used
   * @param enabledState a state for controlling the enabled state of the component
   * @param <T> the attribute type
   * @return a SteppedComboBox bound the the attribute
   * @see is.codion.swing.common.ui.combobox.MaximumMatch
   */
  protected final <T> SteppedComboBox<T> createComboBox(final Attribute<T> attribute, final ComboBoxModel<T> comboBoxModel,
                                                        final boolean maximumMatch, final StateObserver enabledState) {
    final SteppedComboBox<T> comboBox = EntityInputComponents.createComboBox(
            getEditModel().getEntityDefinition().getProperty(attribute),
            getEditModel().value(attribute), comboBoxModel, enabledState);
    if (maximumMatch) {
      MaximumMatch.enable(comboBox);
    }
    if (TRANSFER_FOCUS_ON_ENTER.get()) {
      transferFocusOnEnter((JComponent) comboBox.getEditor().getEditorComponent());
      transferFocusOnEnter(comboBox);
    }
    setComponent(attribute, comboBox);

    return comboBox;
  }

  /**
   * Creates a SteppedComboBox containing the values defined by the given value list attribute,
   * bound to the given attribute.
   * @param attribute the attribute
   * @param <T> the attribute type
   * @return a SteppedComboBox bound to the attribute
   * @throws IllegalArgumentException in case the property based on the given attribute is not a value list property
   */
  protected final <T> SteppedComboBox<Item<T>> createValueListComboBox(final Attribute<T> attribute) {
    return createValueListComboBox(attribute, Sorted.YES);
  }

  /**
   * Creates a SteppedComboBox containing the values defined by the given value list attribute,
   * bound to the given attribute.
   * @param attribute the attribute
   * @param sorted if yes the items are sorted, otherwise the original ordering is preserved
   * @param <T> the attribute type
   * @return a SteppedComboBox bound to the attribute
   * @throws IllegalArgumentException in case the property based on the given attribute is not a value list property
   */
  protected final <T> SteppedComboBox<Item<T>> createValueListComboBox(final Attribute<T> attribute, final Sorted sorted) {
    return createValueListComboBox(attribute, sorted, null);
  }

  /**
   * Creates a SteppedComboBox containing the values defined by the given value list attribute,
   * bound to the given attribute.
   * @param attribute the attribute
   * @param enabledState a state for controlling the enabled state of the component
   * @param <T> the attribute type
   * @return a SteppedComboBox bound to the attribute
   * @throws IllegalArgumentException in case the property based on the given attribute is not a value list property
   */
  protected final <T> SteppedComboBox<Item<T>> createValueListComboBox(final Attribute<T> attribute, final StateObserver enabledState) {
    return createValueListComboBox(attribute, Sorted.YES, enabledState);
  }

  /**
   * Creates a SteppedComboBox containing the values defined by the given value list attribute,
   * bound to the given attribute.
   * @param attribute the attribute
   * @param sorted if yes the items are sorted, otherwise the original ordering is preserved
   * @param enabledState a state for controlling the enabled state of the component
   * @param <T> the attribute type
   * @return a SteppedComboBox bound to the attribute
   * @throws IllegalArgumentException in case the property based on the given attribute is not a value list property
   */
  protected final <T> SteppedComboBox<Item<T>> createValueListComboBox(final Attribute<T> attribute, final Sorted sorted,
                                                                       final StateObserver enabledState) {
    final Property<T> property = getEditModel().getEntityDefinition().getProperty(attribute);
    if (!(property instanceof ValueListProperty)) {
      throw new IllegalArgumentException("Property identified by '" + attribute + "' is not a ValueListProperty");
    }

    final SteppedComboBox<Item<T>> box = EntityInputComponents.createValueListComboBox(
            (ValueListProperty<T>) property, getEditModel().value(attribute), sorted, enabledState);
    if (TRANSFER_FOCUS_ON_ENTER.get()) {
      transferFocusOnEnter((JComponent) box.getEditor().getEditorComponent());
      transferFocusOnEnter(box);
    }
    setComponent(property.getAttribute(), box);

    return box;
  }

  /**
   * Creates an editable SteppedComboBox bound to {@code attribute}
   * @param attribute the attribute to bind
   * @param comboBoxModel the ComboBoxModel
   * @param <T> the attribute type
   * @return an editable SteppedComboBox bound the the attribute
   */
  protected final <T> SteppedComboBox<T> createEditableComboBox(final Attribute<T> attribute, final ComboBoxModel<T> comboBoxModel) {
    return createEditableComboBox(attribute, comboBoxModel, null);
  }

  /**
   * Creates an editable SteppedComboBox bound to {@code attribute}
   * @param attribute the attribute to bind
   * @param comboBoxModel the ComboBoxModel
   * @param enabledState a state for controlling the enabled state of the component
   * @param <T> the attribute type
   * @return an editable SteppedComboBox bound the the attribute
   */
  protected final <T> SteppedComboBox<T> createEditableComboBox(final Attribute<T> attribute, final ComboBoxModel<T> comboBoxModel,
                                                                final StateObserver enabledState) {
    final SteppedComboBox<T> comboBox = EntityInputComponents.createComboBox(
            getEditModel().getEntityDefinition().getProperty(attribute),
            getEditModel().value(attribute), comboBoxModel, enabledState, Editable.YES);
    if (TRANSFER_FOCUS_ON_ENTER.get()) {
      transferFocusOnEnter((JComponent) comboBox.getEditor().getEditorComponent());
      transferFocusOnEnter(comboBox);
    }
    setComponent(attribute, comboBox);

    return comboBox;
  }

  /**
   * Creates a SteppedComboBox bound to {@code attribute}, the combo box
   * contains the underlying values of the attribute
   * @param attribute the attribute to bind
   * @param <T> the attribute type
   * @return a SteppedComboBox bound to the attribute
   */
  protected final <T> SteppedComboBox<T> createAttributeComboBox(final Attribute<T> attribute) {
    return createAttributeComboBox(attribute, null);
  }

  /**
   * Creates a SteppedComboBox bound to {@code attribute}, the combo box
   * contains the underlying values of the attribute
   * @param attribute the attribute to bind
   * @param enabledState a state for controlling the enabled state of the component
   * @param <T> the attribute type
   * @return a SteppedComboBox bound to the attribute
   */
  protected final <T> SteppedComboBox<T> createAttributeComboBox(final Attribute<T> attribute, final StateObserver enabledState) {
    return createAttributeComboBox(attribute, enabledState, Editable.NO);
  }

  /**
   * Creates a SteppedComboBox bound to {@code attribute}, the combo box
   * contains the underlying values of the attribute
   * @param attribute the attribute to bind
   * @param enabledState a state for controlling the enabled state of the component
   * @param editable yes if the combo box should be editable, only works with combo boxes based on String.class properties
   * @param <T> the attribute type
   * @return a SteppedComboBox bound to the attribute
   */
  protected final <T> SteppedComboBox<T> createAttributeComboBox(final Attribute<T> attribute, final StateObserver enabledState,
                                                                 final Editable editable) {
    final SteppedComboBox<T> comboBox = EntityInputComponents.createPropertyComboBox(
            getEditModel().getEntityDefinition().getColumnProperty(attribute),
            getEditModel().value(attribute),
            (ComboBoxModel<T>) getEditModel().getComboBoxModel(attribute), enabledState, editable);
    if (TRANSFER_FOCUS_ON_ENTER.get()) {
      transferFocusOnEnter((JComponent) comboBox.getEditor().getEditorComponent());
      transferFocusOnEnter(comboBox);
    }
    setComponent(attribute, comboBox);

    return comboBox;
  }

  /**
   * Creates an EntityComboBox bound to {@code foreignKeyAttribute}
   * @param foreignKeyAttribute the foreign key attribute to bind
   * combination used to create new instances of the entity this EntityComboBox is based on
   * EntityComboBox is focusable
   * @return an EntityComboBox bound to the attribute
   */
  protected final EntityComboBox createForeignKeyComboBox(final Attribute<Entity> foreignKeyAttribute) {
    return createForeignKeyComboBox(foreignKeyAttribute, null);
  }

  /**
   * Creates a EntityComboBox bound to {@code foreignKeyAttribute}
   * @param foreignKeyAttribute the attribute to bind
   * @param enabledState a state for controlling the enabled state of the component
   * @return a EntityComboBox bound to the attribute
   */
  protected final EntityComboBox createForeignKeyComboBox(final Attribute<Entity> foreignKeyAttribute, final StateObserver enabledState) {
    final EntityComboBox comboBox = EntityInputComponents.createForeignKeyComboBox(
            getEditModel().getEntityDefinition().getForeignKeyProperty(foreignKeyAttribute),
            getEditModel().value(foreignKeyAttribute),
            getEditModel().getForeignKeyComboBoxModel(foreignKeyAttribute), enabledState);
    if (TRANSFER_FOCUS_ON_ENTER.get()) {
      //getEditor().getEditorComponent() only required because the combo box is editable, due to AutoCompletion
      transferFocusOnEnter((JComponent) comboBox.getEditor().getEditorComponent());
    }
    setComponent(foreignKeyAttribute, comboBox);

    return comboBox;
  }

  /**
   * Creates an EntityLookupField bound to {@code foreignKeyattribute}
   * @param foreignKeyAttribute the foreign key attribute to bind
   * @return an EntityLookupField bound the attribute
   */
  protected final EntityLookupField createForeignKeyLookupField(final Attribute<Entity> foreignKeyAttribute) {
    return createForeignKeyLookupField(foreignKeyAttribute, null);
  }

  /**
   * Creates an EntityLookupField bound to {@code foreignKeyattribute}
   * @param foreignKeyAttribute the foreign key attribute to bind
   * @param enabledState a state for controlling the enabled state of the component
   * @return an EntityLookupField bound the attribute
   */
  protected final EntityLookupField createForeignKeyLookupField(final Attribute<Entity> foreignKeyAttribute,
                                                                final StateObserver enabledState) {
    final EntityLookupField lookupField = EntityInputComponents.createForeignKeyLookupField(
            getEditModel().getEntityDefinition().getForeignKeyProperty(foreignKeyAttribute),
            getEditModel().value(foreignKeyAttribute),
            getEditModel().getForeignKeyLookupModel(foreignKeyAttribute), enabledState);
    if (TRANSFER_FOCUS_ON_ENTER.get()) {
      lookupField.setTransferFocusOnEnter();
    }
    setComponent(foreignKeyAttribute, lookupField);

    return lookupField;
  }

  /**
   * Creates an uneditable JTextField bound to {@code foreignKeyAttribute}
   * @param foreignKeyAttribute the attribute to bind
   * @return an uneditable JTextField bound to the attribute
   */
  protected final JTextField createForeignKeyField(final Attribute<Entity> foreignKeyAttribute) {
    requireNonNull(foreignKeyAttribute, "foreignKeyAttribute");
    final ForeignKeyProperty foreignKeyProperty =
            getEditModel().getEntityDefinition().getForeignKeyProperty(foreignKeyAttribute);
    final JTextField textField = new JTextField();
    textField.setEditable(false);
    textField.setFocusable(false);
    textField.setToolTipText(foreignKeyProperty.getDescription());
    new ForeignKeyModelValue(getEditModel(), foreignKeyAttribute).link(TextValues.textValue(textField));
    if (TRANSFER_FOCUS_ON_ENTER.get()) {
      transferFocusOnEnter(textField);
    }
    setComponent(foreignKeyProperty.getAttribute(), textField);

    return textField;
  }

  /**
   * Creates a JLabel with a caption from {@code attribute}, if a input component exists
   * for the given attribute this label is associated with it via {@link JLabel#setLabelFor(Component)}.
   * @param attribute the attribute from which to retrieve the caption
   * @param <T> the attribute type
   * @return a JLabel for the given attribute
   */
  protected final <T> JLabel createLabel(final Attribute<T> attribute) {
    return createLabel(attribute, JLabel.LEFT);
  }

  /**
   * Creates a JLabel with a caption from {@code attribute}, if an input component exists
   * for the given attribute this label is associated with it via {@link JLabel#setLabelFor(Component)}.
   * @param attribute the attribute from which to retrieve the caption
   * @param horizontalAlignment the horizontal text alignment
   * @param <T> the attribute type
   * @return a JLabel for the given attribute
   */
  protected final <T> JLabel createLabel(final Attribute<T> attribute, final int horizontalAlignment) {
    return setLabelForComponent(EntityInputComponents.createLabel(getEditModel().getEntityDefinition()
            .getProperty(attribute), horizontalAlignment), getComponent(attribute));
  }

  /**
   * @return the component that should get the initial focus when the UI is cleared
   */
  protected JComponent getInitialFocusComponent() {
    if (initialFocusComponent != null) {
      return initialFocusComponent;
    }

    if (initialFocusAttribute != null) {
      return getComponent(initialFocusAttribute);
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

    if (afterInsertFocusAttribute != null) {
      return getComponent(afterInsertFocusAttribute);
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
   * @return a list of attributes to use when selecting a input component in this panel,
   * this returns all attributes that have mapped components in this panel
   * that are enabled, displayable, visible and focusable.
   * @see #excludeComponentFromSelection(String)
   * @see #setComponent(String, javax.swing.JComponent)
   */
  private List<Attribute<?>> getSelectComponentAttributes() {
    final List<Attribute<?>> attributes = getComponentAttributes();
    attributes.removeIf(attribute -> {
      final JComponent component = getComponent(attribute);

      return component == null || excludeFromSelection.contains(attribute) || !component.isDisplayable() ||
              !component.isVisible() || !component.isFocusable() || !component.isEnabled();
    });

    return attributes;
  }

  private static JLabel setLabelForComponent(final JLabel label, final JComponent component) {
    if (component != null && label.getLabelFor() != component) {
      label.setLabelFor(component);
    }

    return label;
  }

  private static final class ForeignKeyModelValue extends AbstractValue<String> {

    private final EntityEditModel editModel;
    private final Attribute<Entity> foreignKeyAttribute;

    private ForeignKeyModelValue(final EntityEditModel editModel, final Attribute<Entity> foreignKeyAttribute) {
      this.editModel = editModel;
      this.foreignKeyAttribute = foreignKeyAttribute;
      editModel.addValueListener(foreignKeyAttribute, valueChange -> notifyValueChange());
    }

    @Override
    public void set(final String value) {/*read only*/}

    @Override
    public String get() {
      final Entity value = editModel.getForeignKey(foreignKeyAttribute);

      return value == null ? "" : value.toString();
    }

    @Override
    public boolean isNullable() {
      return false;
    }
  }
}
