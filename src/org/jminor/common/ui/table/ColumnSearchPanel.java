/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.table;

import org.jminor.common.model.DateUtil;
import org.jminor.common.model.EventAdapter;
import org.jminor.common.model.EventListener;
import org.jminor.common.model.Item;
import org.jminor.common.model.SearchType;
import org.jminor.common.model.State;
import org.jminor.common.model.StateObserver;
import org.jminor.common.model.States;
import org.jminor.common.model.Util;
import org.jminor.common.model.combobox.ItemComboBoxModel;
import org.jminor.common.model.table.ColumnSearchModel;
import org.jminor.common.ui.UiUtil;
import org.jminor.common.ui.ValueLinks;
import org.jminor.common.ui.combobox.SteppedComboBox;
import org.jminor.common.ui.control.ControlProvider;
import org.jminor.common.ui.control.Controls;
import org.jminor.common.ui.images.Images;
import org.jminor.common.ui.layout.FlexibleGridLayout;
import org.jminor.common.ui.textfield.DoubleField;
import org.jminor.common.ui.textfield.IntField;

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
 * A UI implementation for ColumnSearchModel
 */
public class ColumnSearchPanel<K> extends JPanel {

  public static final int DEFAULT_FIELD_COLUMNS = 4;

  private static final int ENABLED_BUTTON_SIZE = 20;

  /**
   * The SearchModel this AbstractSearchPanel represents
   */
  private final ColumnSearchModel<K> searchModel;

  /**
   * The search types allowed in this model
   */
  private final Collection<SearchType> searchTypes;

  /**
   * A JToggleButton for enabling/disabling the filter
   */
  private final JToggleButton toggleSearchEnabled;

  /**
   * A JToggleButton for toggling advanced/simple search
   */
  private final JToggleButton toggleSearchAdvanced;
  private final JComboBox searchTypeCombo;
  private final JComponent upperBoundField;
  private final JComponent lowerBoundField;

  private final State stAdvancedSearch = States.state();

  private JDialog dialog;
  private Point lastDialogPosition;
  private boolean dialogEnabled = false;
  private boolean dialogVisible = false;

  /**
   * Instantiates a new ColumnSearchPanel, with a default input field provider.
   * @param searchModel the search model to base this panel on
   * @param includeToggleSearchEnabledButton if true an activation button is included
   * @param includeToggleAdvancedSearchButton if true an advanced toggle button is included
   */
  public ColumnSearchPanel(final ColumnSearchModel<K> searchModel, final boolean includeToggleSearchEnabledButton,
                           final boolean includeToggleAdvancedSearchButton) {
    this(searchModel, includeToggleSearchEnabledButton, includeToggleAdvancedSearchButton, SearchType.values());
  }

  /**
   * Instantiates a new ColumnSearchPanel, with a default input field provider.
   * @param searchModel the search model to base this panel on
   * @param includeToggleSearchEnabledButton if true an activation button is include
   * @param includeToggleAdvancedSearchButton if true an advanced toggle button is include
   * @param searchTypes the search types available to this search panel
   */
  public ColumnSearchPanel(final ColumnSearchModel<K> searchModel, final boolean includeToggleSearchEnabledButton, final boolean includeToggleAdvancedSearchButton,
                           final SearchType... searchTypes) {
    this(searchModel, includeToggleSearchEnabledButton, includeToggleAdvancedSearchButton, new DefaultInputFieldProvider<K>(searchModel), searchTypes);
  }

  /**
   * Instantiates a new ColumnSearchPanel.
   * @param searchModel the search model to base this panel on
   * @param includeToggleSearchEnabledButton if true an activation button is include
   * @param includeToggleAdvancedSearchButton if true an advanced toggle button is include
   * @param inputFieldProvider the input field provider
   * @param searchTypes the search types available to this search panel
   */
  public ColumnSearchPanel(final ColumnSearchModel<K> searchModel, final boolean includeToggleSearchEnabledButton,
                           final boolean includeToggleAdvancedSearchButton, final InputFieldProvider inputFieldProvider,
                           final SearchType... searchTypes) {
    this(searchModel, includeToggleSearchEnabledButton, includeToggleAdvancedSearchButton, inputFieldProvider.initializeInputField(true),
            inputFieldProvider.initializeInputField(false), searchTypes);
  }

  /**
   * Instantiates a new ColumnSearchPanel, with a default input field provider.
   * @param searchModel the search model to base this panel on
   * @param includeToggleSearchEnabledButton if true a button for enabling this search panel is included
   * @param includeToggleAdvancedSearchButton if true an advanced toggle button is included
   * @param upperBoundField the upper bound input field
   * @param lowerBoundField the lower bound input field
   * @param searchTypes the search types available to this search panel
   */
  public ColumnSearchPanel(final ColumnSearchModel<K> searchModel, final boolean includeToggleSearchEnabledButton,
                           final boolean includeToggleAdvancedSearchButton, final JComponent upperBoundField,
                           final JComponent lowerBoundField, final SearchType... searchTypes) {
    Util.rejectNullValue(searchModel, "searchModel");
    this.searchModel = searchModel;
    this.searchTypes = searchTypes == null ? Arrays.asList(SearchType.values()) : Arrays.asList(searchTypes);
    this.searchTypeCombo = initializeSearchTypeComboBox();
    this.upperBoundField = upperBoundField;
    this.lowerBoundField = lowerBoundField;
    if (includeToggleSearchEnabledButton) {
      this.toggleSearchEnabled = ControlProvider.createToggleButton(
              Controls.toggleControl(searchModel, "enabled", null, searchModel.getEnabledObserver()));
      toggleSearchEnabled.setIcon(Images.loadImage(Images.IMG_FILTER_16));
    }
    else {
      this.toggleSearchEnabled = null;
    }
    if (includeToggleAdvancedSearchButton) {
      this.toggleSearchAdvanced = ControlProvider.createToggleButton(
              Controls.toggleControl(this, "advancedSearchOn", null, stAdvancedSearch.getObserver()));
      toggleSearchAdvanced.setIcon(Images.loadImage(Images.IMG_PREFERENCES_16));
    }
    else {
      this.toggleSearchAdvanced = null;
    }
    linkComponentsToLockedState();
    initializeUI();
    initializePanel();
    bindEvents();
  }

  /**
   * @return the search model this panel uses
   */
  public final ColumnSearchModel<K> getSearchModel() {
    return this.searchModel;
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

  public final void enableDialog(final Container dialogParent, final Point position) {
    if (!isDialogEnabled()) {
      initializeSearchDialog(dialogParent);
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
   * @param value true if advanced search should be enabled
   */
  public final void setAdvancedSearchOn(final boolean value) {
    stAdvancedSearch.setActive(value);
  }

  /**
   * @return true if the advanced search is enabled
   */
  public final boolean isAdvancedSearchOn() {
    return stAdvancedSearch.isActive();
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

  public final void addAdvancedSearchListener(final EventListener listener) {
    stAdvancedSearch.addListener(listener);
  }

  public final void removeAdvancedSearchListener(final EventListener listener) {
    stAdvancedSearch.removeListener(listener);
  }

  /**
   * Provides a upper/lower bound input fields based on a ColumnSearchModel
   * @param <K> the type of column identifiers
   */
  public interface InputFieldProvider<K> {

    /**
     * @return the search model to link the input fields to
     */
    ColumnSearchModel<K> getSearchModel();

    /**
     * @param isUpperBound if true then the returned field should be bound
     * with with upper bound value int he search model, otherwise the lower bound
     * @return a upper/lower bound input field
     */
    JComponent initializeInputField(final boolean isUpperBound);
  }

  private static final class DefaultInputFieldProvider<K> implements InputFieldProvider<K> {

    private final ColumnSearchModel<K> searchModel;

    private DefaultInputFieldProvider(final ColumnSearchModel<K> searchModel) {
      Util.rejectNullValue(searchModel, "searchModel");
      this.searchModel = searchModel;
    }

    /** {@inheritDoc} */
    @Override
    public ColumnSearchModel<K> getSearchModel() {
      return searchModel;
    }

    /**
     * @param isUpperBound true if the field should represent the upper bound, otherwise it should be the lower bound field
     * @return an input field for either the upper or lower bound
     */
    @Override
    public JComponent initializeInputField(final boolean isUpperBound) {
      if (searchModel.getType() == Types.BOOLEAN && !isUpperBound) {
        return null;//no lower bound field required for boolean values
      }
      final JComponent field = initializeField();
      if (searchModel.getType() == Types.BOOLEAN) {
        createToggleProperty((JCheckBox) field, isUpperBound);
      }
      else {
        createTextProperty(field, isUpperBound);
      }

      if (field instanceof JTextField) {//enter button toggles the filter on/off
        ((JTextField) field).addActionListener(new ActionListener() {
          /** {@inheritDoc} */
          @Override
          public void actionPerformed(final ActionEvent e) {
            searchModel.setEnabled(!searchModel.isEnabled());
          }
        });
      }

      return field;
    }

    private JComponent initializeField() {
      if (searchModel.getType() == Types.DATE || searchModel.getType() == Types.TIMESTAMP) {
        return UiUtil.createFormattedField(DateUtil.getDateMask((SimpleDateFormat) searchModel.getFormat()));
      }
      else if (searchModel.getType() == Types.DOUBLE) {
        return new DoubleField(DEFAULT_FIELD_COLUMNS);
      }
      else if (searchModel.getType() == Types.INTEGER) {
        return new IntField(DEFAULT_FIELD_COLUMNS);
      }
      else if (searchModel.getType() == Types.BOOLEAN) {
        return new JCheckBox();
      }
      else {
        return new JTextField(DEFAULT_FIELD_COLUMNS);
      }
    }

    private void createToggleProperty(final JCheckBox checkBox, final boolean isUpperBound) {
      ValueLinks.toggleValueLink(checkBox.getModel(), searchModel,
              isUpperBound ? ColumnSearchModel.UPPER_BOUND_PROPERTY : ColumnSearchModel.LOWER_BOUND_PROPERTY,
              isUpperBound ? searchModel.getUpperBoundObserver() : searchModel.getLowerBoundObserver());
    }

    private void createTextProperty(final JComponent component, final boolean isUpper) {
      if (searchModel.getType() == Types.INTEGER) {
        ValueLinks.intValueLink((IntField) component, searchModel,
                isUpper ? ColumnSearchModel.UPPER_BOUND_PROPERTY : ColumnSearchModel.LOWER_BOUND_PROPERTY,
                isUpper ? searchModel.getUpperBoundObserver() : searchModel.getLowerBoundObserver(), false);
      }
      else if (searchModel.getType() == Types.DOUBLE) {
        ValueLinks.doubleValueLink((DoubleField) component, searchModel,
                isUpper ? ColumnSearchModel.UPPER_BOUND_PROPERTY : ColumnSearchModel.LOWER_BOUND_PROPERTY,
                isUpper ? searchModel.getUpperBoundObserver() : searchModel.getLowerBoundObserver(), false);
      }
      else if (searchModel.getType() == Types.DATE) {
        ValueLinks.dateValueLink((JFormattedTextField) component, searchModel,
                isUpper ? ColumnSearchModel.UPPER_BOUND_PROPERTY : ColumnSearchModel.LOWER_BOUND_PROPERTY,
                isUpper ? searchModel.getUpperBoundObserver() : searchModel.getLowerBoundObserver(),
                false, (DateFormat) searchModel.getFormat(), false);
      }
      else if (searchModel.getType() == Types.TIMESTAMP) {
        ValueLinks.dateValueLink((JFormattedTextField) component, searchModel,
                isUpper ? ColumnSearchModel.UPPER_BOUND_PROPERTY : ColumnSearchModel.LOWER_BOUND_PROPERTY,
                isUpper ? searchModel.getUpperBoundObserver() : searchModel.getLowerBoundObserver(),
                false, (DateFormat) searchModel.getFormat(), true);
      }
      else {
        ValueLinks.textValueLink((JTextField) component, searchModel,
                isUpper ? ColumnSearchModel.UPPER_BOUND_PROPERTY : ColumnSearchModel.LOWER_BOUND_PROPERTY,
                String.class, isUpper ? searchModel.getUpperBoundObserver() : searchModel.getLowerBoundObserver());
      }
    }
  }

  /**
   * Binds events to relevant GUI actions
   */
  private void bindEvents() {
    stAdvancedSearch.addListener(new EventAdapter() {
      /** {@inheritDoc} */
      @Override
      public void eventOccurred() {
        initializePanel();
        if (toggleSearchAdvanced != null) {
          toggleSearchAdvanced.requestFocusInWindow();
        }
        else {
          upperBoundField.requestFocusInWindow();
        }
      }
    });
    searchModel.addLowerBoundRequiredListener(new EventAdapter() {
      /** {@inheritDoc} */
      @Override
      public void eventOccurred() {
        initializePanel();
        revalidate();
        searchTypeCombo.requestFocusInWindow();
      }
    });
  }

  private void initializePanel() {
    if (stAdvancedSearch.isActive()) {
      initializeAdvancedPanel();
    }
    else {
      initializeSimplePanel();
    }
  }

  private JComboBox initializeSearchTypeComboBox() {
    final ItemComboBoxModel<SearchType> comboBoxModel = new ItemComboBoxModel<SearchType>();
    for (final SearchType type : SearchType.values()) {
      if (searchTypes.contains(type)) {
        comboBoxModel.addItem(new Item<SearchType>(type, type.getCaption()));
      }
    }
    final JComboBox comboBox = new SteppedComboBox(comboBoxModel);
    ValueLinks.selectedItemValueLink(comboBox, searchModel, "searchType", SearchType.class, searchModel.getSearchTypeObserver());
    comboBox.setRenderer(new DefaultListCellRenderer() {
      /** {@inheritDoc} */
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
    if (toggleSearchEnabled != null) {
      this.toggleSearchEnabled.setPreferredSize(new Dimension(ENABLED_BUTTON_SIZE, ENABLED_BUTTON_SIZE));
    }
    if (toggleSearchAdvanced != null) {
      this.toggleSearchAdvanced.setPreferredSize(new Dimension(ENABLED_BUTTON_SIZE, ENABLED_BUTTON_SIZE));
    }
  }

  private void initializeSimplePanel() {
    removeAll();
    ((FlexibleGridLayout) getLayout()).setRows(1);
    final JPanel basePanel = new JPanel(new BorderLayout(1, 1));
    if (searchModel.isLowerBoundRequired()) {
      final JPanel fieldBase = new JPanel(new GridLayout(1, 2, 1, 1));
      fieldBase.add(lowerBoundField);
      fieldBase.add(upperBoundField);
      basePanel.add(fieldBase, BorderLayout.CENTER);
    }
    else {
      basePanel.add(upperBoundField, BorderLayout.CENTER);
    }

    if (toggleSearchEnabled != null) {
      basePanel.add(toggleSearchEnabled, BorderLayout.EAST);
    }
    if (toggleSearchAdvanced != null) {
      basePanel.add(toggleSearchAdvanced, BorderLayout.WEST);
    }

    add(basePanel);

    setPreferredSize(new Dimension(getPreferredSize().width, basePanel.getPreferredSize().height));

    revalidate();
  }

  private void initializeAdvancedPanel() {
    removeAll();
    ((FlexibleGridLayout) getLayout()).setRows(2);
    final JPanel inputPanel = new JPanel(new BorderLayout(1, 1));
    if (searchModel.isLowerBoundRequired()) {
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
    if (toggleSearchEnabled != null) {
      controlPanel.add(toggleSearchEnabled, BorderLayout.EAST);
    }
    if (toggleSearchAdvanced != null) {
      controlPanel.add(toggleSearchAdvanced, BorderLayout.WEST);
    }

    add(controlPanel);
    add(inputPanel);

    setPreferredSize(new Dimension(getPreferredSize().width, controlPanel.getPreferredSize().height + inputPanel.getPreferredSize().height));

    revalidate();
  }

  private void linkComponentsToLockedState() {
    final StateObserver stUnlocked = searchModel.getLockedObserver().getReversedObserver();
    UiUtil.linkToEnabledState(stUnlocked, searchTypeCombo);
    UiUtil.linkToEnabledState(stUnlocked, upperBoundField);
    if (lowerBoundField != null) {
      UiUtil.linkToEnabledState(stUnlocked, lowerBoundField);
    }
    if (toggleSearchAdvanced != null) {
      UiUtil.linkToEnabledState(stUnlocked, toggleSearchAdvanced);
    }
    if (toggleSearchEnabled != null) {
      UiUtil.linkToEnabledState(stUnlocked, toggleSearchEnabled);
    }
  }

  private void initializeSearchDialog(final Container parent) {
    if (dialog != null) {
      return;
    }

    final JDialog dlgParent = UiUtil.getParentDialog(parent);
    if (dlgParent != null) {
      dialog = new JDialog(dlgParent, searchModel.getColumnIdentifier().toString(), false);
    }
    else {
      dialog = new JDialog(UiUtil.getParentFrame(parent), searchModel.getColumnIdentifier().toString(), false);
    }

    final JPanel searchPanel = new JPanel(new BorderLayout());
    searchPanel.add(this, BorderLayout.NORTH);
    dialog.getContentPane().add(searchPanel);
    dialog.pack();

    addAdvancedSearchListener(new EventAdapter() {
      /** {@inheritDoc} */
      @Override
      public void eventOccurred() {
        dialog.pack();
      }
    });

    dialog.addWindowListener(new WindowAdapter() {
      /** {@inheritDoc} */
      @Override
      public void windowClosing(final WindowEvent e) {
        disableDialog();
      }
    });
  }
}
