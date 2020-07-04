/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.table;

import is.codion.common.DateFormats;
import is.codion.common.db.Operator;
import is.codion.common.event.Event;
import is.codion.common.event.EventDataListener;
import is.codion.common.event.Events;
import is.codion.common.model.table.ColumnConditionModel;
import is.codion.common.state.State;
import is.codion.common.state.States;
import is.codion.common.value.Value;
import is.codion.common.value.ValueSet;
import is.codion.common.value.Values;
import is.codion.swing.common.model.checkbox.NullableToggleButtonModel;
import is.codion.swing.common.ui.Components;
import is.codion.swing.common.ui.Windows;
import is.codion.swing.common.ui.checkbox.NullableCheckBox;
import is.codion.swing.common.ui.combobox.SteppedComboBox;
import is.codion.swing.common.ui.control.ControlProvider;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.layout.FlexibleGridLayout;
import is.codion.swing.common.ui.layout.FlexibleGridLayout.FixColumnWidths;
import is.codion.swing.common.ui.layout.FlexibleGridLayout.FixRowHeights;
import is.codion.swing.common.ui.textfield.BigDecimalField;
import is.codion.swing.common.ui.textfield.DoubleField;
import is.codion.swing.common.ui.textfield.IntegerField;
import is.codion.swing.common.ui.textfield.LongField;
import is.codion.swing.common.ui.textfield.TextFields;
import is.codion.swing.common.ui.value.BooleanValues;
import is.codion.swing.common.ui.value.NumericalValues;
import is.codion.swing.common.ui.value.SelectedValues;
import is.codion.swing.common.ui.value.TemporalValues;
import is.codion.swing.common.ui.value.TextValues;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.plaf.basic.BasicComboBoxRenderer;
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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import static is.codion.swing.common.ui.icons.Icons.icons;
import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;
import static javax.swing.SwingConstants.CENTER;

/**
 * A UI implementation for ColumnConditionModel
 * @param <R> the type of rows
 * @param <C> the type of objects used to identify columns
 */
public class ColumnConditionPanel<R, C> extends JPanel {

  public static final int DEFAULT_FIELD_COLUMNS = 4;

  /**
   * Specifies whether a condition panel should include
   * a button for toggling advanced mode.
   */
  public enum ToggleAdvancedButton {
    /**
     * Include a button for toggling advancded mode.
     */
    YES,
    /**
     * Don't include a button for toggling advancded mode.
     */
    NO
  }

  private static final int ENABLED_BUTTON_SIZE = 20;

  private final ColumnConditionModel<R, C> conditionModel;
  private final Collection<Operator> operators;
  private final JToggleButton toggleEnabledButton;
  private final JToggleButton toggleAdvancedButton;
  private final SteppedComboBox<Operator> operatorCombo;
  private final JComponent equalToField;
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
   * @param toggleAdvancedButton specifies whether this condition panel should include a button for toggling advanced mode
   * @param operators the operators available to this condition panel
   */
  public ColumnConditionPanel(final ColumnConditionModel<R, C> conditionModel, final ToggleAdvancedButton toggleAdvancedButton,
                              final Operator... operators) {
    this(conditionModel, toggleAdvancedButton, new DefaultBoundFieldProvider(conditionModel), operators);
  }

  /**
   * Instantiates a new ColumnConditionPanel.
   * @param conditionModel the condition model to base this panel on
   * @param toggleAdvancedButton specifies whether this condition panel should include a button for toggling advanced mode
   * @param boundFieldProvider the input field provider
   * @param operators the search operators available to this condition panel
   */
  public ColumnConditionPanel(final ColumnConditionModel<R, C> conditionModel, final ToggleAdvancedButton toggleAdvancedButton,
                              final BoundFieldProvider boundFieldProvider, final Operator... operators) {
    this(conditionModel, toggleAdvancedButton, boundFieldProvider.initializeEqualsValueField(),
            boundFieldProvider.initializeUpperBoundField(), boundFieldProvider.initializeLowerBoundField(), operators);
  }

  /**
   * Instantiates a new ColumnConditionPanel, with a default input field provider.
   * @param conditionModel the condition model to base this panel on
   * @param toggleAdvancedButton specifies whether this condition panel should include a button for toggling advanced mode
   * @param upperBoundField the upper bound input field
   * @param lowerBoundField the lower bound input field
   * @param operators the search operators available to this condition panel
   */
  public ColumnConditionPanel(final ColumnConditionModel<R, C> conditionModel,
                              final ToggleAdvancedButton toggleAdvancedButton, final JComponent equalToField,
                              final JComponent upperBoundField, final JComponent lowerBoundField, final Operator... operators) {
    requireNonNull(conditionModel, "conditionModel");
    this.conditionModel = conditionModel;
    this.operators = operators == null ? asList(Operator.values()) : asList(operators);
    this.operatorCombo = initializeOperatorComboBox();
    this.equalToField = equalToField;
    this.upperBoundField = upperBoundField;
    this.lowerBoundField = lowerBoundField;
    this.toggleEnabledButton = ControlProvider.createToggleButton(
            Controls.toggleControl(conditionModel, "enabled", null, conditionModel.getEnabledObserver()));
    this.toggleEnabledButton.setIcon(icons().filter());
    if (toggleAdvancedButton == ToggleAdvancedButton.YES) {
      this.toggleAdvancedButton = ControlProvider.createToggleButton(Controls.toggleControl(advancedConditionState));
      this.toggleAdvancedButton.setIcon(icons().configure());
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
    switch (conditionModel.getOperator()) {
      case EQUALS:
      case NOT_EQUALS:
        equalToField.requestFocusInWindow();
        break;
      case GREATER_THAN:
      case WITHIN_RANGE:
      case OUTSIDE_RANGE:
        lowerBoundField.requestFocusInWindow();
        break;
      case LESS_THAN:
        upperBoundField.requestFocusInWindow();
        break;
      default:
        throw new IllegalArgumentException("Unknown operator: " + conditionModel.getOperator());
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
  public interface BoundFieldProvider {

    /**
     * @return the equals value field
     */
    JComponent initializeEqualsValueField();

    /**
     * @return a upper bound input field
     */
    JComponent initializeUpperBoundField();

    /**
     * @return a lower bound input field
     */
    JComponent initializeLowerBoundField();
  }

  private static final class DefaultBoundFieldProvider implements BoundFieldProvider {

    private final ColumnConditionModel<?, ?> columnConditionModel;

    private DefaultBoundFieldProvider(final ColumnConditionModel<?, ?> columnConditionModel) {
      requireNonNull(columnConditionModel, "columnConditionModel");
      this.columnConditionModel = columnConditionModel;
    }

    public JComponent initializeEqualsValueField() {
      final ValueSet<Object> valueSet = columnConditionModel.getEqualsValueSet();
      final Value<Object> value = Values.value();
      value.addDataListener(object -> valueSet.set(object == null ? Collections.emptySet() : Collections.singleton(object)));

      return initializeField(value);
    }

    @Override
    public JComponent initializeUpperBoundField() {
      if (columnConditionModel.getTypeClass().equals(Boolean.class)) {
        return null;//no lower bound field required for boolean values
      }

      return initializeField(columnConditionModel.getUpperBoundValue());
    }

    @Override
    public JComponent initializeLowerBoundField() {
      if (columnConditionModel.getTypeClass().equals(Boolean.class)) {
        return null;//no lower bound field required for boolean values
      }

      return initializeField(columnConditionModel.getLowerBoundValue());
    }

    private JComponent initializeField(final Value<?> value) {
      final Class<?> typeClass = columnConditionModel.getTypeClass();
      if (typeClass.equals(Boolean.class)) {
        final NullableCheckBox checkBox = new NullableCheckBox(new NullableToggleButtonModel());
        checkBox.setHorizontalAlignment(CENTER);
        ((Value<Boolean>) value).link(BooleanValues.booleanButtonModelValue(checkBox.getModel()));

        return checkBox;
      }
      if (typeClass.equals(Integer.class)) {
        final IntegerField integerField = new IntegerField((NumberFormat) columnConditionModel.getFormat(), DEFAULT_FIELD_COLUMNS);
        ((Value<Integer>) value).link(NumericalValues.integerValue(integerField));

        return integerField;
      }
      else if (typeClass.equals(Double.class)) {
        final DoubleField doubleField = new DoubleField((DecimalFormat) columnConditionModel.getFormat(), DEFAULT_FIELD_COLUMNS);
        ((Value<Double>) value).link(NumericalValues.doubleValue(doubleField));

        return doubleField;
      }
      else if (typeClass.equals(BigDecimal.class)) {
        final BigDecimalField bigDecimalField = new BigDecimalField((DecimalFormat) columnConditionModel.getFormat(), DEFAULT_FIELD_COLUMNS);
        ((Value<BigDecimal>) value).link(NumericalValues.bigDecimalValue(bigDecimalField));

        return bigDecimalField;
      }
      else if (typeClass.equals(Long.class)) {
        final LongField longField = new LongField((NumberFormat) columnConditionModel.getFormat(), DEFAULT_FIELD_COLUMNS);
        ((Value<Long>) value).link(NumericalValues.longValue(longField));

        return longField;
      }
      else if (typeClass.equals(LocalTime.class)) {
        final JFormattedTextField formattedField =
                TextFields.createFormattedField(DateFormats.getDateMask(columnConditionModel.getDateTimeFormatPattern()));
        ((Value<LocalTime>) value).link(TemporalValues.localTimeValue(formattedField, columnConditionModel.getDateTimeFormatPattern()));

        return formattedField;
      }
      else if (typeClass.equals(LocalDate.class)) {
        final JFormattedTextField formattedField =
                TextFields.createFormattedField(DateFormats.getDateMask(columnConditionModel.getDateTimeFormatPattern()));
        ((Value<LocalDate>) value).link(TemporalValues.localDateValue(formattedField, columnConditionModel.getDateTimeFormatPattern()));

        return formattedField;
      }
      else if (typeClass.equals(LocalDateTime.class)) {
        final JFormattedTextField formattedField =
                TextFields.createFormattedField(DateFormats.getDateMask(columnConditionModel.getDateTimeFormatPattern()));
        ((Value<LocalDateTime>) value).link(TemporalValues.localDateTimeValue(formattedField, columnConditionModel.getDateTimeFormatPattern()));

        return formattedField;
      }
      else if (typeClass.equals(String.class)) {
        final JTextField textField = new JTextField(DEFAULT_FIELD_COLUMNS);
        ((Value<String>) value).link(TextValues.textValue(textField));

        return textField;
      }

      throw new IllegalArgumentException("Unsupported type: " + typeClass);
    }
  }

  /**
   * Binds events to relevant GUI actions
   */
  private void bindEvents() {
    advancedConditionState.addListener(this::initializePanel);
    conditionModel.getOperatorObserver().addListener(() -> {
      initializePanel();
      operatorCombo.requestFocusInWindow();
    });
    final FocusAdapter focusGainedListener = new FocusAdapter() {
      @Override
      public void focusGained(final FocusEvent e) {
        if (!e.isTemporary()) {
          focusGainedEvent.onEvent(conditionModel.getColumnIdentifier());
        }
      }
    };
    operatorCombo.addFocusListener(focusGainedListener);
    if (equalToField != null) {
      equalToField.addFocusListener(focusGainedListener);
    }
    if (upperBoundField != null) {
      upperBoundField.addFocusListener(focusGainedListener);
    }
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

  private SteppedComboBox<Operator> initializeOperatorComboBox() {
    final DefaultComboBoxModel<Operator> comboBoxModel = new DefaultComboBoxModel<>();
    Arrays.stream(Operator.values()).filter(operators::contains).forEach(comboBoxModel::addElement);
    final SteppedComboBox<Operator> comboBox = new SteppedComboBox<>(comboBoxModel);
    Values.propertyValue(conditionModel, "operator", Operator.class, conditionModel.getOperatorObserver())
            .link(SelectedValues.selectedValue(comboBox));
    comboBox.setRenderer(new OperatorComboBoxRenderer());

    return comboBox;
  }

  private void initializeUI() {
    final FlexibleGridLayout layout = new FlexibleGridLayout(2, 1, 0, 0, FixRowHeights.YES, FixColumnWidths.NO);
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
    switch (conditionModel.getOperator()) {
      case EQUALS:
      case NOT_EQUALS: return singleValuePanel(equalToField);
      case GREATER_THAN: return singleValuePanel(lowerBoundField);
      case LESS_THAN: return singleValuePanel(upperBoundField);
      case WITHIN_RANGE:
      case OUTSIDE_RANGE: return rangePanel();
      default:
        throw new IllegalArgumentException("Unknown operator: " + conditionModel.getOperator());
    }
  }

  private JPanel singleValuePanel(final JComponent component) {
    final JPanel inputPanel = new JPanel(new BorderLayout());
    final JPanel panel = new JPanel(new GridLayout(1, 1));
    panel.add(component);
    inputPanel.add(panel, BorderLayout.CENTER);

    return inputPanel;
  }

  private JPanel rangePanel() {
    final JPanel inputPanel = new JPanel(new BorderLayout());
    final JPanel panel = new JPanel(new GridLayout(1, 2));
    panel.add(lowerBoundField);
    panel.add(upperBoundField);
    inputPanel.add(panel, BorderLayout.CENTER);

    return inputPanel;
  }

  private JPanel initializeControlPanel() {
    final JPanel controlPanel = new JPanel(new BorderLayout());
    controlPanel.add(operatorCombo, BorderLayout.CENTER);
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
            operatorCombo, equalToField, upperBoundField, lowerBoundField, toggleAdvancedButton, toggleEnabledButton);
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

  private static final class OperatorComboBoxRenderer extends BasicComboBoxRenderer {

    private OperatorComboBoxRenderer() {
      setHorizontalAlignment(CENTER);
    }

    @Override
    public Component getListCellRendererComponent(final JList list, final Object value, final int index,
                                                  final boolean isSelected, final boolean cellHasFocus) {
      super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
      setToolTipText(((Operator) value).getDescription());

      return this;
    }
  }
}
