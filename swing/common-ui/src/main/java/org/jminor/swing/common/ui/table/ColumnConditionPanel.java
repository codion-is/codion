/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.table;

import org.jminor.common.DateFormats;
import org.jminor.common.db.ConditionType;
import org.jminor.common.event.Event;
import org.jminor.common.event.EventDataListener;
import org.jminor.common.event.Events;
import org.jminor.common.item.Item;
import org.jminor.common.item.Items;
import org.jminor.common.model.table.ColumnConditionModel;
import org.jminor.common.state.State;
import org.jminor.common.state.States;
import org.jminor.common.value.Value;
import org.jminor.common.value.Values;
import org.jminor.swing.common.model.checkbox.NullableToggleButtonModel;
import org.jminor.swing.common.model.combobox.ItemComboBoxModel;
import org.jminor.swing.common.ui.Components;
import org.jminor.swing.common.ui.Windows;
import org.jminor.swing.common.ui.checkbox.NullableCheckBox;
import org.jminor.swing.common.ui.combobox.SteppedComboBox;
import org.jminor.swing.common.ui.control.ControlProvider;
import org.jminor.swing.common.ui.control.Controls;
import org.jminor.swing.common.ui.images.Images;
import org.jminor.swing.common.ui.layout.FlexibleGridLayout;
import org.jminor.swing.common.ui.textfield.DecimalField;
import org.jminor.swing.common.ui.textfield.IntegerField;
import org.jminor.swing.common.ui.textfield.LongField;
import org.jminor.swing.common.ui.textfield.TextFields;
import org.jminor.swing.common.ui.value.BooleanValues;
import org.jminor.swing.common.ui.value.NumericalValues;
import org.jminor.swing.common.ui.value.SelectedValues;
import org.jminor.swing.common.ui.value.TemporalValues;
import org.jminor.swing.common.ui.value.TextValues;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collection;

import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;

/**
 * A UI implementation for ColumnConditionModel
 * @param <R> the type of rows
 * @param <C> the type of objects used to identify columns
 */
public class ColumnConditionPanel<R, C> extends JPanel {

  public static final int DEFAULT_FIELD_COLUMNS = 4;

  private static final int ENABLED_BUTTON_SIZE = 20;

  /**
   * The ColumnConditionModel this ColumnConditionPanel represents
   */
  private final ColumnConditionModel<R, C> conditionModel;

  /**
   * The search types allowed in this model
   */
  private final Collection<ConditionType> conditionTypes;

  /**
   * A JToggleButton for enabling/disabling the filter
   */
  private final JToggleButton toggleEnabledButton;

  /**
   * A JToggleButton for toggling advanced/simple search
   */
  private final JToggleButton toggleAdvancedButton;
  private final JComboBox conditionTypeCombo;
  private final JComponent upperBoundField;
  private final JComponent lowerBoundField;

  private final Event<C> focusGainedEvent = Events.event();
  private final State advancedConditionState = States.state();

  private JDialog dialog;
  private Point lastDialogPosition;
  private boolean dialogEnabled = false;
  private boolean dialogVisible = false;

  /**
   * Instantiates a new ColumnConditionPanel, with a default input field provider.
   * @param conditionModel the condition model to base this panel on
   * @param toggleAdvancedButton if true an advanced toggle button is included
   * @param conditionTypes the search types available to this condition panel
   */
  public ColumnConditionPanel(final ColumnConditionModel<R, C> conditionModel, final boolean toggleAdvancedButton,
                              final ConditionType... conditionTypes) {
    this(conditionModel, toggleAdvancedButton, new DefaultInputFieldProvider(conditionModel), conditionTypes);
  }

  /**
   * Instantiates a new ColumnConditionPanel.
   * @param conditionModel the condition model to base this panel on
   * @param toggleAdvancedButton if true an advanced toggle button is included
   * @param inputFieldProvider the input field provider
   * @param conditionTypes the search types available to this condition panel
   */
  public ColumnConditionPanel(final ColumnConditionModel<R, C> conditionModel, final boolean toggleAdvancedButton,
                              final InputFieldProvider inputFieldProvider, final ConditionType... conditionTypes) {
    this(conditionModel, toggleAdvancedButton,
            inputFieldProvider.initializeInputField(true),
            inputFieldProvider.initializeInputField(false), conditionTypes);
  }

  /**
   * Instantiates a new ColumnConditionPanel, with a default input field provider.
   * @param conditionModel the condition model to base this panel on
   * @param toggleAdvancedButton if true an advanced toggle button is included
   * @param upperBoundField the upper bound input field
   * @param lowerBoundField the lower bound input field
   * @param conditionTypes the search types available to this condition panel
   */
  public ColumnConditionPanel(final ColumnConditionModel<R, C> conditionModel,
                              final boolean toggleAdvancedButton, final JComponent upperBoundField,
                              final JComponent lowerBoundField, final ConditionType... conditionTypes) {
    requireNonNull(conditionModel, "conditionModel");
    this.conditionModel = conditionModel;
    this.conditionTypes = conditionTypes == null ? asList(ConditionType.values()) : asList(conditionTypes);
    this.conditionTypeCombo = initializeConditionTypeComboBox();
    this.upperBoundField = upperBoundField;
    this.lowerBoundField = lowerBoundField;
    this.toggleEnabledButton = ControlProvider.createToggleButton(
            Controls.toggleControl(conditionModel, "enabled", null, conditionModel.getEnabledObserver()));
    this.toggleEnabledButton.setIcon(Images.loadImage(Images.IMG_FILTER_16));
    if (toggleAdvancedButton) {
      this.toggleAdvancedButton = ControlProvider.createToggleButton(Controls.toggleControl(advancedConditionState));
      this.toggleAdvancedButton.setIcon(Images.loadImage(Images.IMG_PREFERENCES_16));
    }
    else {
      this.toggleAdvancedButton = null;
    }
    linkComponentsToLockedState();
    initializeUI();
    initializePanel();
    bindEvents();
  }

  /**
   * @return the condition model this panel uses
   */
  public final ColumnConditionModel<R, C> getModel() {
    return this.conditionModel;
  }

  /**
   * @return the last screen position
   */
  public final Point getLastDialogPosition() {
    return lastDialogPosition;
  }

  /**
   * @return true if the dialog is enabled
   */
  public final boolean isDialogEnabled() {
    return dialogEnabled;
  }

  /**
   * @return true if the dialog is being shown
   */
  public final boolean isDialogVisible() {
    return dialogVisible;
  }

  /**
   * Displays this condition panel in a dialog
   * @param dialogParent the dialog parent
   * @param position the position
   */
  public final void enableDialog(final Container dialogParent, final Point position) {
    if (!isDialogEnabled()) {
      initializeConditionDialog(dialogParent);
      Point actualPosition = position;
      if (position == null) {
        actualPosition = lastDialogPosition;
      }
      if (actualPosition == null) {
        actualPosition = new Point(0, 0);
      }

      actualPosition.y = actualPosition.y - dialog.getHeight();
      dialog.setLocation(actualPosition);
      dialogEnabled = true;
    }

    showDialog();
  }

  /**
   * Hides the dialog displaying this condition panel
   */
  public final void disableDialog() {
    if (isDialogEnabled()) {
      if (isDialogVisible()) {
        hideDialog();
      }
      lastDialogPosition = dialog.getLocation();
      lastDialogPosition.y = lastDialogPosition.y + dialog.getHeight();
      dialog.dispose();
      dialog = null;
      dialogEnabled = false;
    }
  }

  /**
   * Displays this panel in a dialog
   */
  public final void showDialog() {
    if (isDialogEnabled() && !isDialogVisible()) {
      dialog.setVisible(true);
      upperBoundField.requestFocusInWindow();
      dialogVisible = true;
    }
  }

  /**
   * Hides the dialog showing this panel if visible
   */
  public final void hideDialog() {
    if (isDialogVisible()) {
      dialog.setVisible(false);
      dialogVisible = false;
    }
  }

  /**
   * @return the dialog used to show this filter panel
   */
  public final JDialog getDialog() {
    return dialog;
  }

  /**
   * Requests keyboard focus for this panels input field
   */
  public final void requestInputFocus() {
    if (conditionModel.isLowerBoundRequired()) {
      lowerBoundField.requestFocusInWindow();
    }
    else {
      upperBoundField.requestFocusInWindow();
    }
  }

  /**
   * @param advanced true if advanced condition should be enabled
   */
  public final void setAdvanced(final boolean advanced) {
    advancedConditionState.set(advanced);
  }

  /**
   * @return true if the advanced condition is enabled
   */
  public final boolean isAdvanced() {
    return advancedConditionState.get();
  }

  /**
   * @return the JComponent used to specify the upper bound
   */
  public final JComponent getUpperBoundField() {
    return upperBoundField;
  }

  /**
   * @return the JComponent used to specify the lower bound
   */
  public final JComponent getLowerBoundField() {
    return lowerBoundField;
  }

  /**
   * @param listener a listener notified each time the advanced condition state changes
   */
  public final void addAdvancedListener(final EventDataListener<Boolean> listener) {
    advancedConditionState.addDataListener(listener);
  }

  /**
   * @param listener the listener to remove
   */
  public final void removeAdvancedListener(final EventDataListener<Boolean> listener) {
    advancedConditionState.removeDataListener(listener);
  }

  /**
   * @param listener listener notified when a this condition panels input fields receive focus
   */
  public final void addFocusGainedListener(final EventDataListener<C> listener) {
    focusGainedEvent.addDataListener(listener);
  }

  /**
   * Provides a upper/lower bound input fields for a ColumnConditionPanel
   */
  public interface InputFieldProvider {

    /**
     * @param isUpperBound if true then the returned field should be bound
     * with with upper bound value int he condition model, otherwise the lower bound
     * @return a upper/lower bound input field
     */
    JComponent initializeInputField(boolean isUpperBound);
  }

  private static final class DefaultInputFieldProvider implements InputFieldProvider {

    private final ColumnConditionModel columnConditionModel;

    private DefaultInputFieldProvider(final ColumnConditionModel columnConditionModel) {
      requireNonNull(columnConditionModel, "columnConditionModel");
      this.columnConditionModel = columnConditionModel;
    }

    /**
     * @param isUpperBound true if the field should represent the upper bound, otherwise it should be the lower bound field
     * @return an input field for either the upper or lower bound
     */
    @Override
    public JComponent initializeInputField(final boolean isUpperBound) {
      if (columnConditionModel.getTypeClass().equals(Boolean.class) && !isUpperBound) {
        return null;//no lower bound field required for boolean values
      }
      final JComponent field = initializeField();
      if (columnConditionModel.getTypeClass().equals(Boolean.class)) {
        createToggleProperty((JCheckBox) field, isUpperBound);
      }
      else {
        createTextProperty(field, isUpperBound);
      }

      return field;
    }

    private JComponent initializeField() {
      final Class typeClass = columnConditionModel.getTypeClass();
      if (typeClass.equals(Integer.class)) {
        return new IntegerField(DEFAULT_FIELD_COLUMNS);
      }
      else if (typeClass.equals(Double.class)) {
        return new DecimalField(DEFAULT_FIELD_COLUMNS);
      }
      else if (typeClass.equals(BigDecimal.class)) {
        final DecimalFormat format = (DecimalFormat) NumberFormat.getNumberInstance();
        format.setParseBigDecimal(true);

        return new DecimalField(format, DEFAULT_FIELD_COLUMNS);
      }
      else if (typeClass.equals(Long.class)) {
        return new LongField(DEFAULT_FIELD_COLUMNS);
      }
      else if (typeClass.equals(Boolean.class)) {
        final NullableCheckBox checkBox = new NullableCheckBox(new NullableToggleButtonModel());
        checkBox.setHorizontalAlignment(SwingConstants.CENTER);

        return checkBox;
      }
      else if (typeClass.equals(LocalTime.class) || typeClass.equals(LocalDateTime.class) || typeClass.equals(LocalDate.class)) {
        return TextFields.createFormattedField(DateFormats.getDateMask(columnConditionModel.getDateTimeFormatPattern()));
      }

      return new JTextField(DEFAULT_FIELD_COLUMNS);
    }

    private void createToggleProperty(final JCheckBox checkBox, final boolean upperBound) {
      final Value<Boolean> value = upperBound ? columnConditionModel.getUpperBoundValue() : columnConditionModel.getLowerBoundValue();
      value.link(BooleanValues.booleanButtonModelValue(checkBox.getModel()));
    }

    private void createTextProperty(final JComponent component, final boolean upperBound) {
      final Value value = upperBound ? columnConditionModel.getUpperBoundValue() : columnConditionModel.getLowerBoundValue();
      final Class typeClass = columnConditionModel.getTypeClass();
      if (typeClass.equals(Integer.class)) {
        value.link(NumericalValues.integerValue((IntegerField) component));
      }
      else if (typeClass.equals(Double.class)) {
        value.link(NumericalValues.doubleValue((DecimalField) component));
      }
      else if (typeClass.equals(BigDecimal.class)) {
        value.link(NumericalValues.bigDecimalValue((DecimalField) component));
      }
      else if (typeClass.equals(Long.class)) {
        value.link(NumericalValues.longValue((LongField) component));
      }
      else if (typeClass.equals(LocalTime.class)) {
        value.link(TemporalValues.localTimeValue((JFormattedTextField) component,
                columnConditionModel.getDateTimeFormatPattern()));
      }
      else if (typeClass.equals(LocalDateTime.class)) {
        value.link(TemporalValues.localDateTimeValue((JFormattedTextField) component,
                columnConditionModel.getDateTimeFormatPattern()));
      }
      else if (typeClass.equals(LocalDate.class)) {
        value.link(TemporalValues.localDateValue((JFormattedTextField) component,
                columnConditionModel.getDateTimeFormatPattern()));
      }
      else {
        value.link(TextValues.textValue((JTextField) component));
      }
    }
  }

  /**
   * Binds events to relevant GUI actions
   */
  private void bindEvents() {
    advancedConditionState.addListener(this::initializePanel);
    conditionModel.addLowerBoundRequiredListener(() -> {
      initializePanel();
      conditionTypeCombo.requestFocusInWindow();
    });
    final FocusAdapter focusGainedListener = new FocusAdapter() {
      @Override
      public void focusGained(final FocusEvent e) {
        if (!e.isTemporary()) {
          focusGainedEvent.onEvent(conditionModel.getColumnIdentifier());
        }
      }
    };
    conditionTypeCombo.addFocusListener(focusGainedListener);
    upperBoundField.addFocusListener(focusGainedListener);
    if (lowerBoundField != null) {
      lowerBoundField.addFocusListener(focusGainedListener);
    }
    if (toggleAdvancedButton != null) {
      toggleAdvancedButton.addFocusListener(focusGainedListener);
    }
    if (toggleEnabledButton != null) {
      toggleEnabledButton.addFocusListener(focusGainedListener);
    }
  }

  private void initializePanel() {
    removeAll();
    if (advancedConditionState.get()) {
      initializeAdvancedPanel();
    }
    else {
      initializeSimplePanel();
    }
    revalidate();
  }

  private JComboBox initializeConditionTypeComboBox() {
    final ItemComboBoxModel<ConditionType> comboBoxModel = new ItemComboBoxModel<>();
    for (final ConditionType type : ConditionType.values()) {
      if (conditionTypes.contains(type)) {
        comboBoxModel.addItem(Items.item(type, type.getCaption()));
      }
    }
    final JComboBox<ConditionType> comboBox = new SteppedComboBox(comboBoxModel);
    Values.propertyValue(conditionModel, "conditionType", ConditionType.class, conditionModel.getConditionTypeObserver())
            .link(SelectedValues.selectedValue(comboBox));
    comboBox.setRenderer(new DefaultListCellRenderer() {
      @Override
      public Component getListCellRendererComponent(final JList list, final Object value, final int index,
                                                    final boolean isSelected, final boolean cellHasFocus) {
        final JComponent component = (JComponent) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        component.setToolTipText(((Item<ConditionType>) value).getValue().getDescription());

        return component;
      }
    });

    return comboBox;
  }

  private void initializeUI() {
    final FlexibleGridLayout layout = new FlexibleGridLayout(2, 1, 0, 0, true, false);
    setLayout(layout);
    if (toggleEnabledButton != null) {
      this.toggleEnabledButton.setPreferredSize(new Dimension(ENABLED_BUTTON_SIZE, ENABLED_BUTTON_SIZE));
    }
    if (toggleAdvancedButton != null) {
      this.toggleAdvancedButton.setPreferredSize(new Dimension(ENABLED_BUTTON_SIZE, ENABLED_BUTTON_SIZE));
    }
  }

  private void initializeSimplePanel() {
    ((FlexibleGridLayout) getLayout()).setRows(1);
    final JPanel inputPanel = initializeInputPanel();
    if (toggleEnabledButton != null) {
      inputPanel.add(toggleEnabledButton, BorderLayout.EAST);
    }
    if (toggleAdvancedButton != null) {
      inputPanel.add(toggleAdvancedButton, BorderLayout.WEST);
    }
    add(inputPanel);
    setPreferredSize(new Dimension(getPreferredSize().width, inputPanel.getPreferredSize().height));
  }

  private void initializeAdvancedPanel() {
    ((FlexibleGridLayout) getLayout()).setRows(2);
    final JPanel inputPanel = initializeInputPanel();
    final JPanel controlPanel = initializeControlPanel();
    add(controlPanel);
    add(inputPanel);
    setPreferredSize(new Dimension(getPreferredSize().width, controlPanel.getPreferredSize().height + inputPanel.getPreferredSize().height));
  }

  private JPanel initializeInputPanel() {
    final JPanel inputPanel = new JPanel(new BorderLayout());
    if (conditionModel.isLowerBoundRequired()) {
      final JPanel fieldBase = new JPanel(new GridLayout(1, 2));
      fieldBase.add(lowerBoundField);
      fieldBase.add(upperBoundField);
      inputPanel.add(fieldBase, BorderLayout.CENTER);
    }
    else {
      inputPanel.add(upperBoundField, BorderLayout.CENTER);
    }

    return inputPanel;
  }

  private JPanel initializeControlPanel() {
    final JPanel controlPanel = new JPanel(new BorderLayout());
    controlPanel.add(conditionTypeCombo, BorderLayout.CENTER);
    if (toggleEnabledButton != null) {
      controlPanel.add(toggleEnabledButton, BorderLayout.EAST);
    }
    if (toggleAdvancedButton != null) {
      controlPanel.add(toggleAdvancedButton, BorderLayout.WEST);
    }

    return controlPanel;
  }

  private void linkComponentsToLockedState() {
    Components.linkToEnabledState(conditionModel.getLockedObserver().getReversedObserver(),
            conditionTypeCombo, upperBoundField, lowerBoundField, toggleAdvancedButton, toggleEnabledButton);
  }

  private void initializeConditionDialog(final Container parent) {
    if (dialog != null) {
      return;
    }

    final JDialog dialogParent = Windows.getParentDialog(parent);
    if (dialogParent != null) {
      dialog = new JDialog(dialogParent, conditionModel.getColumnIdentifier().toString(), false);
    }
    else {
      dialog = new JDialog(Windows.getParentFrame(parent), conditionModel.getColumnIdentifier().toString(), false);
    }

    final JPanel conditionPanel = new JPanel(new BorderLayout());
    conditionPanel.add(this, BorderLayout.NORTH);
    dialog.getContentPane().add(conditionPanel);
    dialog.pack();

    addAdvancedListener(advanced -> dialog.pack());

    dialog.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(final WindowEvent e) {
        disableDialog();
      }
    });
  }
}
