/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.table;

import is.codion.common.db.Operator;
import is.codion.common.event.Event;
import is.codion.common.event.EventDataListener;
import is.codion.common.formats.LocaleDateTimePattern;
import is.codion.common.model.table.ColumnConditionModel;
import is.codion.common.state.State;
import is.codion.common.value.Value;
import is.codion.swing.common.model.checkbox.NullableToggleButtonModel;
import is.codion.swing.common.ui.Components;
import is.codion.swing.common.ui.Windows;
import is.codion.swing.common.ui.checkbox.NullableCheckBox;
import is.codion.swing.common.ui.combobox.SteppedComboBox;
import is.codion.swing.common.ui.control.ToggleControl;
import is.codion.swing.common.ui.layout.FlexibleGridLayout;
import is.codion.swing.common.ui.layout.FlexibleGridLayout.FixColumnWidths;
import is.codion.swing.common.ui.layout.FlexibleGridLayout.FixRowHeights;
import is.codion.swing.common.ui.textfield.BigDecimalField;
import is.codion.swing.common.ui.textfield.DoubleField;
import is.codion.swing.common.ui.textfield.IntegerField;
import is.codion.swing.common.ui.textfield.LongField;
import is.codion.swing.common.ui.textfield.TextFields;
import is.codion.swing.common.ui.value.ComponentValues;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
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
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;

import static is.codion.swing.common.ui.icons.Icons.icons;
import static java.util.Objects.requireNonNull;
import static javax.swing.SwingConstants.CENTER;

/**
 * A UI implementation for ColumnConditionModel
 * @param <C> the type of objects used to identify columns
 * @param <T> the column value type
 */
public class ColumnConditionPanel<C, T> extends JPanel {

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

  private final ColumnConditionModel<C, T> conditionModel;
  private final JToggleButton toggleEnabledButton;
  private final JToggleButton toggleAdvancedButton;
  private final SteppedComboBox<Operator> operatorCombo;
  private final JComponent equalField;
  private final JComponent upperBoundField;
  private final JComponent lowerBoundField;
  private final JPanel controlPanel = new JPanel(new BorderLayout());
  private final JPanel inputPanel = new JPanel(new BorderLayout());

  private final Event<C> focusGainedEvent = Event.event();
  private final State advancedConditionState = State.state();

  private JDialog dialog;
  private Point lastDialogPosition;
  private boolean dialogEnabled = false;
  private boolean dialogVisible = false;

  /**
   * Instantiates a new ColumnConditionPanel, with a default bound field factory and all available Operators.
   * @param conditionModel the condition model to base this panel on
   * @param toggleAdvancedButton specifies whether this condition panel should include a button for toggling advanced mode
   */
  public ColumnConditionPanel(final ColumnConditionModel<C, T> conditionModel, final ToggleAdvancedButton toggleAdvancedButton) {
    this(conditionModel, toggleAdvancedButton, Arrays.asList(Operator.values()));
  }

  /**
   * Instantiates a new ColumnConditionPanel, with a default bound field factory.
   * @param conditionModel the condition model to base this panel on
   * @param toggleAdvancedButton specifies whether this condition panel should include a button for toggling advanced mode
   * @param operators the operators available to this condition panel
   */
  public ColumnConditionPanel(final ColumnConditionModel<C, T> conditionModel, final ToggleAdvancedButton toggleAdvancedButton,
                              final List<Operator> operators) {
    this(conditionModel, toggleAdvancedButton, new DefaultBoundFieldFactory<>(conditionModel), operators);
  }

  /**
   * Instantiates a new ColumnConditionPanel.
   * @param conditionModel the condition model to base this panel on
   * @param toggleAdvancedButton specifies whether this condition panel should include a button for toggling advanced mode
   * @param boundFieldFactory the input field factory
   * @param operators the search operators available to this condition panel
   */
  public ColumnConditionPanel(final ColumnConditionModel<C, T> conditionModel, final ToggleAdvancedButton toggleAdvancedButton,
                              final BoundFieldFactory boundFieldFactory, final List<Operator> operators) {
    requireNonNull(conditionModel, "conditionModel");
    if (requireNonNull(operators, "operators").isEmpty()) {
      throw new IllegalArgumentException("One or more operators must be specified");
    }
    this.conditionModel = conditionModel;
    final boolean modelLocked = conditionModel.isLocked();
    conditionModel.setLocked(false);//otherwise the validator checking the locked state kicks in during value linking
    this.equalField = boundFieldFactory.createEqualField();
    this.upperBoundField = boundFieldFactory.createUpperBoundField();
    this.lowerBoundField = boundFieldFactory.createLowerBoundField();
    this.operatorCombo = initializeOperatorComboBox(operators);
    this.toggleEnabledButton = ToggleControl.builder()
            .state(conditionModel.getEnabledState())
            .icon(icons().filter())
            .build().createToggleButton();
    this.toggleAdvancedButton = toggleAdvancedButton == ToggleAdvancedButton.YES ? ToggleControl.builder()
            .state(advancedConditionState)
            .icon(icons().configure())
            .build().createToggleButton() : null;
    conditionModel.setLocked(modelLocked);
    initializeUI();
    bindEvents();
  }

  /**
   * @return the condition model this panel uses
   */
  public final ColumnConditionModel<C, T> getModel() {
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
      requestInputFocus();
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
      case EQUAL:
      case NOT_EQUAL:
        equalField.requestFocusInWindow();
        break;
      case GREATER_THAN:
      case GREATER_THAN_OR_EQUAL:
      case BETWEEN_EXCLUSIVE:
      case BETWEEN:
      case NOT_BETWEEN_EXCLUSIVE:
      case NOT_BETWEEN:
        lowerBoundField.requestFocusInWindow();
        break;
      case LESS_THAN:
      case LESS_THAN_OR_EQUAL:
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
   * @return the condition operator combo box
   */
  public final JComboBox<Operator> getOperatorComboBox() {
    return operatorCombo;
  }

  /**
   * @return the JComponent used to specify the equal value
   */
  public final JComponent getEqualField() {
    return equalField;
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
   * Provides a equal, upper and lower bound input fields for a ColumnConditionPanel
   */
  public interface BoundFieldFactory {

    /**
     * @return the equal value field
     */
    JComponent createEqualField();

    /**
     * @return a upper bound input field
     */
    JComponent createUpperBoundField();

    /**
     * @return a lower bound input field
     */
    JComponent createLowerBoundField();
  }

  private static final class DefaultBoundFieldFactory<T> implements BoundFieldFactory {

    private final ColumnConditionModel<?, T> columnConditionModel;

    private DefaultBoundFieldFactory(final ColumnConditionModel<?, T> columnConditionModel) {
      this.columnConditionModel = requireNonNull(columnConditionModel, "columnConditionModel");
    }

    public JComponent createEqualField() {
      return createField(columnConditionModel.getEqualValueSet().value());
    }

    @Override
    public JComponent createUpperBoundField() {
      if (columnConditionModel.getTypeClass().equals(Boolean.class)) {
        return null;//no lower bound field required for boolean values
      }

      return createField(columnConditionModel.getUpperBoundValue());
    }

    @Override
    public JComponent createLowerBoundField() {
      if (columnConditionModel.getTypeClass().equals(Boolean.class)) {
        return null;//no lower bound field required for boolean values
      }

      return createField(columnConditionModel.getLowerBoundValue());
    }

    private JComponent createField(final Value<?> value) {
      final Class<?> typeClass = columnConditionModel.getTypeClass();
      if (typeClass.equals(Boolean.class)) {
        final NullableCheckBox checkBox = new NullableCheckBox(new NullableToggleButtonModel());
        checkBox.setHorizontalAlignment(CENTER);
        ComponentValues.booleanToggleButton(checkBox).link((Value<Boolean>) value);

        return checkBox;
      }
      if (typeClass.equals(Integer.class)) {
        final IntegerField integerField = new IntegerField((NumberFormat) columnConditionModel.getFormat(), DEFAULT_FIELD_COLUMNS);
        ComponentValues.integerField(integerField).link((Value<Integer>) value);

        return integerField;
      }
      else if (typeClass.equals(Double.class)) {
        final DoubleField doubleField = new DoubleField((DecimalFormat) columnConditionModel.getFormat(), DEFAULT_FIELD_COLUMNS);
        ComponentValues.doubleField(doubleField).link((Value<Double>) value);

        return doubleField;
      }
      else if (typeClass.equals(BigDecimal.class)) {
        final BigDecimalField bigDecimalField = new BigDecimalField((DecimalFormat) columnConditionModel.getFormat(), DEFAULT_FIELD_COLUMNS);
        ComponentValues.bigDecimalField(bigDecimalField).link((Value<BigDecimal>) value);

        return bigDecimalField;
      }
      else if (typeClass.equals(Long.class)) {
        final LongField longField = new LongField((NumberFormat) columnConditionModel.getFormat(), DEFAULT_FIELD_COLUMNS);
        ComponentValues.longField(longField).link((Value<Long>) value);

        return longField;
      }
      else if (typeClass.equals(LocalTime.class)) {
        final JFormattedTextField formattedField =
                TextFields.createFormattedField(LocaleDateTimePattern.getMask(columnConditionModel.getDateTimePattern()));
        ComponentValues.localTimeFieldBuilder()
                .component(formattedField)
                .dateTimePattern(columnConditionModel.getDateTimePattern())
                .build()
                .link((Value<LocalTime>) value);

        return formattedField;
      }
      else if (typeClass.equals(LocalDate.class)) {
        final JFormattedTextField formattedField =
                TextFields.createFormattedField(LocaleDateTimePattern.getMask(columnConditionModel.getDateTimePattern()));
        ComponentValues.localDateFieldBuilder()
                .component(formattedField)
                .dateTimePattern(columnConditionModel.getDateTimePattern())
                .build()
                .link((Value<LocalDate>) value);

        return formattedField;
      }
      else if (typeClass.equals(LocalDateTime.class)) {
        final JFormattedTextField formattedField =
                TextFields.createFormattedField(LocaleDateTimePattern.getMask(columnConditionModel.getDateTimePattern()));
        ComponentValues.localDateFieldTimeBuilder()
                .component(formattedField)
                .dateTimePattern(columnConditionModel.getDateTimePattern())
                .build()
                .link((Value<LocalDateTime>) value);

        return formattedField;
      }
      else if (typeClass.equals(OffsetDateTime.class)) {
        final JFormattedTextField formattedField =
                TextFields.createFormattedField(LocaleDateTimePattern.getMask(columnConditionModel.getDateTimePattern()));
        ComponentValues.offsetDateFieldTimeBuilder()
                .component(formattedField)
                .dateTimePattern(columnConditionModel.getDateTimePattern())
                .build()
                .link((Value<OffsetDateTime>) value);

        return formattedField;
      }
      else if (typeClass.equals(String.class)) {
        final JTextField textField = new JTextField(DEFAULT_FIELD_COLUMNS);
        ComponentValues.stringTextComponent(textField).link((Value<String>) value);

        return textField;
      }

      throw new IllegalArgumentException("Unsupported type: " + typeClass);
    }
  }

  /**
   * Binds events to relevant GUI actions
   */
  private void bindEvents() {
    advancedConditionState.addDataListener(this::onAdvancedChange);
    conditionModel.getOperatorValue().addDataListener(this::onOperatorChanged);
    final FocusAdapter focusGainedListener = new FocusAdapter() {
      @Override
      public void focusGained(final FocusEvent e) {
        if (!e.isTemporary()) {
          focusGainedEvent.onEvent(conditionModel.getColumnIdentifier());
        }
      }
    };
    operatorCombo.addFocusListener(focusGainedListener);
    if (equalField != null) {
      equalField.addFocusListener(focusGainedListener);
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

  private void onOperatorChanged(final Operator operator) {
    switch (operator) {
      case EQUAL:
      case NOT_EQUAL:
        singleValuePanel(equalField);
        break;
      case GREATER_THAN:
      case GREATER_THAN_OR_EQUAL:
        singleValuePanel(lowerBoundField);
        break;
      case LESS_THAN:
      case LESS_THAN_OR_EQUAL:
        singleValuePanel(upperBoundField);
        break;
      case BETWEEN_EXCLUSIVE:
      case BETWEEN:
      case NOT_BETWEEN_EXCLUSIVE:
      case NOT_BETWEEN:
        rangePanel(lowerBoundField, upperBoundField);
        break;
      default:
        throw new IllegalArgumentException("Unknown operator: " + conditionModel.getOperator());
    }
    revalidate();
  }

  private void onAdvancedChange(final boolean advanced) {
    if (advanced) {
      setAdvanced();
    }
    else {
      setSimple();
    }
  }

  private void setSimple() {
    remove(controlPanel);
    if (toggleEnabledButton != null) {
      controlPanel.remove(toggleEnabledButton);
      inputPanel.add(toggleEnabledButton, BorderLayout.EAST);
    }
    if (toggleAdvancedButton != null) {
      controlPanel.remove(toggleAdvancedButton);
      inputPanel.add(toggleAdvancedButton, BorderLayout.WEST);
    }
    ((FlexibleGridLayout) getLayout()).setRows(1);
    add(inputPanel);
    setPreferredSize(new Dimension(getPreferredSize().width, inputPanel.getPreferredSize().height));
    revalidate();
  }

  private void setAdvanced() {
    if (toggleEnabledButton != null) {
      inputPanel.remove(toggleEnabledButton);
      controlPanel.add(toggleEnabledButton, BorderLayout.EAST);
    }
    if (toggleAdvancedButton != null) {
      inputPanel.remove(toggleAdvancedButton);
      controlPanel.add(toggleAdvancedButton, BorderLayout.WEST);
    }
    ((FlexibleGridLayout) getLayout()).setRows(2);
    add(controlPanel);
    add(inputPanel);
    setPreferredSize(new Dimension(getPreferredSize().width, controlPanel.getPreferredSize().height + inputPanel.getPreferredSize().height));
    revalidate();
  }

  private SteppedComboBox<Operator> initializeOperatorComboBox(final List<Operator> operators) {
    final DefaultComboBoxModel<Operator> comboBoxModel = new DefaultComboBoxModel<>();
    Arrays.stream(Operator.values()).filter(operators::contains).forEach(comboBoxModel::addElement);
    final SteppedComboBox<Operator> comboBox = new SteppedComboBox<>(comboBoxModel);
    ComponentValues.selectedComboBox(comboBox).link(conditionModel.getOperatorValue());
    comboBox.setRenderer(new OperatorComboBoxRenderer());

    return comboBox;
  }

  private void initializeUI() {
    Components.linkToEnabledState(conditionModel.getLockedObserver().getReversedObserver(),
            operatorCombo, equalField, upperBoundField, lowerBoundField, toggleAdvancedButton, toggleEnabledButton);
    setLayout(new FlexibleGridLayout(2, 1, 0, 0, FixRowHeights.YES, FixColumnWidths.NO));
    controlPanel.add(operatorCombo, BorderLayout.CENTER);
    if (toggleEnabledButton != null) {
      this.toggleEnabledButton.setPreferredSize(new Dimension(ENABLED_BUTTON_SIZE, ENABLED_BUTTON_SIZE));
    }
    if (toggleAdvancedButton != null) {
      this.toggleAdvancedButton.setPreferredSize(new Dimension(ENABLED_BUTTON_SIZE, ENABLED_BUTTON_SIZE));
    }
    onOperatorChanged(conditionModel.getOperator());
    onAdvancedChange(advancedConditionState.get());
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

  private void singleValuePanel(final JComponent boundField) {
    inputPanel.removeAll();
    inputPanel.add(boundField, BorderLayout.CENTER);
  }

  private void rangePanel(final JComponent lowerBoundField, final JComponent upperBoundField) {
    final JPanel panel = new JPanel(new GridLayout(1, 2));
    panel.add(lowerBoundField);
    panel.add(upperBoundField);
    inputPanel.removeAll();
    inputPanel.add(panel, BorderLayout.CENTER);
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
