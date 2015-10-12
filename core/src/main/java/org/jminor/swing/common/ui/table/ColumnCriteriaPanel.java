/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.table;

import org.jminor.common.model.DateUtil;
import org.jminor.common.model.EventListener;
import org.jminor.common.model.EventObserver;
import org.jminor.common.model.Item;
import org.jminor.common.model.SearchType;
import org.jminor.common.model.State;
import org.jminor.common.model.StateObserver;
import org.jminor.common.model.States;
import org.jminor.common.model.Util;
import org.jminor.swing.common.model.combobox.ItemComboBoxModel;
import org.jminor.swing.common.model.table.ColumnCriteriaModel;
import org.jminor.swing.common.ui.UiUtil;
import org.jminor.swing.common.ui.ValueLinks;
import org.jminor.swing.common.ui.combobox.SteppedComboBox;
import org.jminor.swing.common.ui.control.ControlProvider;
import org.jminor.swing.common.ui.control.Controls;
import org.jminor.swing.common.ui.images.Images;
import org.jminor.swing.common.ui.layout.FlexibleGridLayout;
import org.jminor.swing.common.ui.textfield.DoubleField;
import org.jminor.swing.common.ui.textfield.IntField;

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
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.Types;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;

/**
 * A UI implementation for ColumnCriteriaModel
 */
public class ColumnCriteriaPanel<K> extends JPanel {

  public static final int DEFAULT_FIELD_COLUMNS = 4;

  private static final int ENABLED_BUTTON_SIZE = 20;

  /**
   * The ColumnCriteriaModel this ColumnCriteriaPanel represents
   */
  private final ColumnCriteriaModel<K> criteriaModel;

  /**
   * The search types allowed in this model
   */
  private final Collection<SearchType> searchTypes;

  /**
   * A JToggleButton for enabling/disabling the filter
   */
  private final JToggleButton toggleEnabled;

  /**
   * A JToggleButton for toggling advanced/simple search
   */
  private final JToggleButton toggleAdvancedCriteria;
  private final JComboBox searchTypeCombo;
  private final JComponent upperBoundField;
  private final JComponent lowerBoundField;

  private final State advancedCriteriaState = States.state();

  private JDialog dialog;
  private Point lastDialogPosition;
  private boolean dialogEnabled = false;
  private boolean dialogVisible = false;

  /**
   * Instantiates a new ColumnCriteriaPanel, with a default input field provider.
   * @param criteriaModel the criteria model to base this panel on
   * @param includeToggleEnabledButton if true an activation button is included
   * @param includeToggleAdvancedCriteriaButton if true an advanced toggle button is included
   */
  public ColumnCriteriaPanel(final ColumnCriteriaModel<K> criteriaModel, final boolean includeToggleEnabledButton,
                             final boolean includeToggleAdvancedCriteriaButton) {
    this(criteriaModel, includeToggleEnabledButton, includeToggleAdvancedCriteriaButton, SearchType.values());
  }

  /**
   * Instantiates a new ColumnCriteriaPanel, with a default input field provider.
   * @param criteriaModel the criteria model to base this panel on
   * @param includeToggleEnabledButton if true an activation button is include
   * @param includeToggleAdvancedCriteriaButton if true an advanced toggle button is include
   * @param searchTypes the search types available to this criteria panel
   */
  public ColumnCriteriaPanel(final ColumnCriteriaModel<K> criteriaModel, final boolean includeToggleEnabledButton,
                             final boolean includeToggleAdvancedCriteriaButton, final SearchType... searchTypes) {
    this(criteriaModel, includeToggleEnabledButton, includeToggleAdvancedCriteriaButton, new DefaultInputFieldProvider<>(criteriaModel), searchTypes);
  }

  /**
   * Instantiates a new ColumnCriteriaPanel.
   * @param criteriaModel the criteria model to base this panel on
   * @param includeToggleEnabledButton if true an activation button is include
   * @param includeToggleAdvancedCriteriaButton if true an advanced toggle button is include
   * @param inputFieldProvider the input field provider
   * @param searchTypes the search types available to this criteria panel
   */
  public ColumnCriteriaPanel(final ColumnCriteriaModel<K> criteriaModel, final boolean includeToggleEnabledButton,
                             final boolean includeToggleAdvancedCriteriaButton, final InputFieldProvider inputFieldProvider,
                             final SearchType... searchTypes) {
    this(criteriaModel, includeToggleEnabledButton, includeToggleAdvancedCriteriaButton, inputFieldProvider.initializeInputField(true),
            inputFieldProvider.initializeInputField(false), searchTypes);
  }

  /**
   * Instantiates a new ColumnCriteriaPanel, with a default input field provider.
   * @param criteriaModel the criteria model to base this panel on
   * @param includeToggleEnabledButton if true a button for enabling this criteria panel is included
   * @param includeToggleAdvancedCriteriaButton if true an advanced toggle button is included
   * @param upperBoundField the upper bound input field
   * @param lowerBoundField the lower bound input field
   * @param searchTypes the search types available to this criteria panel
   */
  public ColumnCriteriaPanel(final ColumnCriteriaModel<K> criteriaModel, final boolean includeToggleEnabledButton,
                             final boolean includeToggleAdvancedCriteriaButton, final JComponent upperBoundField,
                             final JComponent lowerBoundField, final SearchType... searchTypes) {
    Util.rejectNullValue(criteriaModel, "criteriaModel");
    this.criteriaModel = criteriaModel;
    this.searchTypes = searchTypes == null ? Arrays.asList(SearchType.values()) : Arrays.asList(searchTypes);
    this.searchTypeCombo = initializeSearchTypeComboBox();
    this.upperBoundField = upperBoundField;
    this.lowerBoundField = lowerBoundField;
    if (includeToggleEnabledButton) {
      this.toggleEnabled = ControlProvider.createToggleButton(
              Controls.toggleControl(criteriaModel, "enabled", null, criteriaModel.getEnabledObserver()));
      toggleEnabled.setIcon(Images.loadImage(Images.IMG_FILTER_16));
    }
    else {
      this.toggleEnabled = null;
    }
    if (includeToggleAdvancedCriteriaButton) {
      this.toggleAdvancedCriteria = ControlProvider.createToggleButton(
              Controls.toggleControl(this, "advancedCriteriaEnabled", null, advancedCriteriaState.getObserver()));
      toggleAdvancedCriteria.setIcon(Images.loadImage(Images.IMG_PREFERENCES_16));
    }
    else {
      this.toggleAdvancedCriteria = null;
    }
    linkComponentsToLockedState();
    initializeUI();
    initializePanel();
    bindEvents();
  }

  /**
   * @return the criteria model this panel uses
   */
  public final ColumnCriteriaModel<K> getCriteriaModel() {
    return this.criteriaModel;
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
   * Displays this criteria panel in a dialog
   * @param dialogParent the dialog parent
   * @param position the position
   */
  public final void enableDialog(final Container dialogParent, final Point position) {
    if (!isDialogEnabled()) {
      initializeCriteriaDialog(dialogParent);
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
   * Hides the dialog displaying this criteria panel
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

  public final void showDialog() {
    if (isDialogEnabled() && !isDialogVisible()) {
      dialog.setVisible(true);
      upperBoundField.requestFocusInWindow();
      dialogVisible = true;
    }
  }

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
   * @param value true if advanced criteria should be enabled
   */
  public final void setAdvancedCriteriaEnabled(final boolean value) {
    advancedCriteriaState.setActive(value);
  }

  /**
   * @return true if the advanced criteria is enabled
   */
  public final boolean isAdvancedCriteriaEnabled() {
    return advancedCriteriaState.isActive();
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

  public final void addAdvancedCriteriaListener(final EventListener listener) {
    advancedCriteriaState.addListener(listener);
  }

  public final void removeAdvancedCriteriaListener(final EventListener listener) {
    advancedCriteriaState.removeListener(listener);
  }

  /**
   * Provides a upper/lower bound input fields for a ColumnCriteriaPanel
   * @param <K> the type of column identifiers
   */
  public interface InputFieldProvider<K> {

    /**
     * @param isUpperBound if true then the returned field should be bound
     * with with upper bound value int he criteria model, otherwise the lower bound
     * @return a upper/lower bound input field
     */
    JComponent initializeInputField(final boolean isUpperBound);
  }

  private static final class DefaultInputFieldProvider<K> implements InputFieldProvider<K> {

    private final ColumnCriteriaModel<K> columnCriteriaModel;

    private DefaultInputFieldProvider(final ColumnCriteriaModel<K> columnCriteriaModel) {
      Util.rejectNullValue(columnCriteriaModel, "columnCriteriaModel");
      this.columnCriteriaModel = columnCriteriaModel;
    }

    /**
     * @param isUpperBound true if the field should represent the upper bound, otherwise it should be the lower bound field
     * @return an input field for either the upper or lower bound
     */
    @Override
    public JComponent initializeInputField(final boolean isUpperBound) {
      if (columnCriteriaModel.getType() == Types.BOOLEAN && !isUpperBound) {
        return null;//no lower bound field required for boolean values
      }
      final String property = isUpperBound ? ColumnCriteriaModel.UPPER_BOUND_PROPERTY : ColumnCriteriaModel.LOWER_BOUND_PROPERTY;
      final EventObserver changeObserver = isUpperBound ? columnCriteriaModel.getUpperBoundObserver() : columnCriteriaModel.getLowerBoundObserver();
      final JComponent field = initializeField();
      if (columnCriteriaModel.getType() == Types.BOOLEAN) {
        createToggleProperty((JCheckBox) field, property, changeObserver);
      }
      else {
        createTextProperty(field, property, changeObserver);
      }

      if (field instanceof JTextField) {//enter button toggles the filter on/off
        ((JTextField) field).addActionListener(new ActionListener() {
          @Override
          public void actionPerformed(final ActionEvent e) {
            columnCriteriaModel.setEnabled(!columnCriteriaModel.isEnabled());
          }
        });
      }

      return field;
    }

    private JComponent initializeField() {
      switch (columnCriteriaModel.getType()) {
        case Types.INTEGER:
          return new IntField(DEFAULT_FIELD_COLUMNS);
        case Types.DOUBLE:
          return new DoubleField(DEFAULT_FIELD_COLUMNS);
        case Types.BOOLEAN:
          return new JCheckBox();
        case Types.TIME:
        case Types.TIMESTAMP:
        case Types.DATE:
          return UiUtil.createFormattedField(DateUtil.getDateMask((SimpleDateFormat) columnCriteriaModel.getFormat()));
        default:
          return new JTextField(DEFAULT_FIELD_COLUMNS);
      }
    }

    private void createToggleProperty(final JCheckBox checkBox, final String property, final EventObserver changeObserver) {
      ValueLinks.toggleValueLink(checkBox.getModel(), columnCriteriaModel, property, changeObserver);
    }

    @SuppressWarnings("unchecked")
    private void createTextProperty(final JComponent component, final String property, final EventObserver changeObserver) {
      switch (columnCriteriaModel.getType()) {
        case Types.INTEGER:
          ValueLinks.intValueLink((IntField) component, columnCriteriaModel, property, changeObserver, false, true);
          break;
        case Types.DOUBLE:
          ValueLinks.doubleValueLink((DoubleField) component, columnCriteriaModel, property, changeObserver, false, true);
          break;
        case Types.TIME:
        case Types.TIMESTAMP:
        case Types.DATE:
          ValueLinks.dateValueLink((JFormattedTextField) component, columnCriteriaModel, property, changeObserver,
                  false, (DateFormat) columnCriteriaModel.getFormat(), columnCriteriaModel.getType(), true);
          break;
        default:
          ValueLinks.textValueLink((JTextField) component, columnCriteriaModel,property, changeObserver);
      }
    }
  }

  /**
   * Binds events to relevant GUI actions
   */
  private void bindEvents() {
    advancedCriteriaState.addListener(new EventListener() {
      @Override
      public void eventOccurred() {
        initializePanel();
        if (toggleAdvancedCriteria != null) {
          toggleAdvancedCriteria.requestFocusInWindow();
        }
        else {
          upperBoundField.requestFocusInWindow();
        }
      }
    });
    criteriaModel.addLowerBoundRequiredListener(new EventListener() {
      @Override
      public void eventOccurred() {
        initializePanel();
        revalidate();
        searchTypeCombo.requestFocusInWindow();
      }
    });
  }

  private void initializePanel() {
    if (advancedCriteriaState.isActive()) {
      initializeAdvancedPanel();
    }
    else {
      initializeSimplePanel();
    }
  }

  private JComboBox initializeSearchTypeComboBox() {
    final ItemComboBoxModel<SearchType> comboBoxModel = new ItemComboBoxModel<>();
    for (final SearchType type : SearchType.values()) {
      if (searchTypes.contains(type)) {
        comboBoxModel.addItem(new Item<>(type, type.getCaption()));
      }
    }
    final JComboBox<SearchType> comboBox = new SteppedComboBox(comboBoxModel);
    ValueLinks.selectedItemValueLink(comboBox, criteriaModel, "searchType", SearchType.class, (EventObserver) criteriaModel.getSearchTypeObserver());
    comboBox.setRenderer(new DefaultListCellRenderer() {
      @Override
      public Component getListCellRendererComponent(final JList list, final Object value, final int index,
                                                    final boolean isSelected, final boolean cellHasFocus) {
        final JComponent component = (JComponent) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        component.setToolTipText(((Item<SearchType>) value).getItem().getDescription());

        return component;
      }
    });

    return comboBox;
  }

  private void initializeUI() {
    final FlexibleGridLayout layout = new FlexibleGridLayout(2, 1, 1, 1, true, false);
    setLayout(layout);
    if (toggleEnabled != null) {
      this.toggleEnabled.setPreferredSize(new Dimension(ENABLED_BUTTON_SIZE, ENABLED_BUTTON_SIZE));
    }
    if (toggleAdvancedCriteria != null) {
      this.toggleAdvancedCriteria.setPreferredSize(new Dimension(ENABLED_BUTTON_SIZE, ENABLED_BUTTON_SIZE));
    }
  }

  private void initializeSimplePanel() {
    removeAll();
    ((FlexibleGridLayout) getLayout()).setRows(1);
    final JPanel basePanel = new JPanel(new BorderLayout(1, 1));
    if (criteriaModel.isLowerBoundRequired()) {
      final JPanel fieldBase = new JPanel(new GridLayout(1, 2, 1, 1));
      fieldBase.add(lowerBoundField);
      fieldBase.add(upperBoundField);
      basePanel.add(fieldBase, BorderLayout.CENTER);
    }
    else {
      basePanel.add(upperBoundField, BorderLayout.CENTER);
    }

    if (toggleEnabled != null) {
      basePanel.add(toggleEnabled, BorderLayout.EAST);
    }
    if (toggleAdvancedCriteria != null) {
      basePanel.add(toggleAdvancedCriteria, BorderLayout.WEST);
    }

    add(basePanel);

    setPreferredSize(new Dimension(getPreferredSize().width, basePanel.getPreferredSize().height));

    revalidate();
  }

  private void initializeAdvancedPanel() {
    removeAll();
    ((FlexibleGridLayout) getLayout()).setRows(2);
    final JPanel inputPanel = new JPanel(new BorderLayout(1, 1));
    if (criteriaModel.isLowerBoundRequired()) {
      final JPanel fieldBase = new JPanel(new GridLayout(1, 2, 1, 1));
      fieldBase.add(lowerBoundField);
      fieldBase.add(upperBoundField);
      inputPanel.add(fieldBase, BorderLayout.CENTER);
    }
    else {
      inputPanel.add(upperBoundField, BorderLayout.CENTER);
    }

    final JPanel controlPanel = new JPanel(new BorderLayout(1, 1));
    controlPanel.add(searchTypeCombo, BorderLayout.CENTER);
    if (toggleEnabled != null) {
      controlPanel.add(toggleEnabled, BorderLayout.EAST);
    }
    if (toggleAdvancedCriteria != null) {
      controlPanel.add(toggleAdvancedCriteria, BorderLayout.WEST);
    }

    add(controlPanel);
    add(inputPanel);

    setPreferredSize(new Dimension(getPreferredSize().width, controlPanel.getPreferredSize().height + inputPanel.getPreferredSize().height));

    revalidate();
  }

  private void linkComponentsToLockedState() {
    final StateObserver stUnlocked = criteriaModel.getLockedObserver().getReversedObserver();
    UiUtil.linkToEnabledState(stUnlocked, searchTypeCombo);
    UiUtil.linkToEnabledState(stUnlocked, upperBoundField);
    if (lowerBoundField != null) {
      UiUtil.linkToEnabledState(stUnlocked, lowerBoundField);
    }
    if (toggleAdvancedCriteria != null) {
      UiUtil.linkToEnabledState(stUnlocked, toggleAdvancedCriteria);
    }
    if (toggleEnabled != null) {
      UiUtil.linkToEnabledState(stUnlocked, toggleEnabled);
    }
  }

  private void initializeCriteriaDialog(final Container parent) {
    if (dialog != null) {
      return;
    }

    final JDialog dlgParent = UiUtil.getParentDialog(parent);
    if (dlgParent != null) {
      dialog = new JDialog(dlgParent, criteriaModel.getColumnIdentifier().toString(), false);
    }
    else {
      dialog = new JDialog(UiUtil.getParentFrame(parent), criteriaModel.getColumnIdentifier().toString(), false);
    }

    final JPanel criteriaPanel = new JPanel(new BorderLayout());
    criteriaPanel.add(this, BorderLayout.NORTH);
    dialog.getContentPane().add(criteriaPanel);
    dialog.pack();

    addAdvancedCriteriaListener(new EventListener() {
      @Override
      public void eventOccurred() {
        dialog.pack();
      }
    });

    dialog.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(final WindowEvent e) {
        disableDialog();
      }
    });
  }
}
