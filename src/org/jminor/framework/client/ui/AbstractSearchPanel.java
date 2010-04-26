/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui;

import org.jminor.common.model.SearchType;
import org.jminor.common.model.State;
import org.jminor.common.model.combobox.ItemComboBoxModel;
import org.jminor.common.ui.UiUtil;
import org.jminor.common.ui.combobox.SteppedComboBox;
import org.jminor.common.ui.control.ControlFactory;
import org.jminor.common.ui.control.ControlProvider;
import org.jminor.common.ui.images.Images;
import org.jminor.common.ui.layout.FlexibleGridLayout;
import org.jminor.framework.Configuration;
import org.jminor.framework.client.model.AbstractSearchModel;
import org.jminor.framework.domain.Property;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * An abstract panel for showing search/filter configuration.<br>
 * User: Bjorn Darri<br>
 * Date: 26.12.2007<br>
 * Time: 15:17:20<br>
 */
public abstract class AbstractSearchPanel extends JPanel {

  private static final SearchType[] searchTypes = new SearchType[] {
          SearchType.LIKE, SearchType.NOT_LIKE, SearchType.AT_LEAST,
          SearchType.AT_MOST, SearchType.WITHIN_RANGE, SearchType.OUTSIDE_RANGE};

  private static final String[] searchTypeImageNames = new String[] {
          "Equals60x16.gif", "NotEquals60x16.gif", "LessThanOrEquals60x16.gif",
          "LargerThanOrEquals60x16.gif", "Inclusive60x16.gif", "Exclusive60x16.gif"};

  /**
   * The AbstractSearchModel this AbstractSearchPanel represents
   */
  private final AbstractSearchModel model;

  /**
   * A JToggleButton for enabling/disabling the filter
   */
  private final JToggleButton toggleSearchEnabled;

  /**
   * A JToggleButton for toggling advanced/simple search
   */
  private final JToggleButton toggleSearchAdvanced;

  /**
   * A JComboBox for selecting the search type
   */
  private final JComboBox searchTypeCombo;
  private final JComponent upperBoundField;
  private final JComponent lowerBoundField;

  private final State stAdvancedSearch = new State();
  private final State stTwoSearchFields = new State();

  private final boolean includeToggleSearchEnabledBtn;
  private final boolean includeToggleSearchAdvancedBtn;

  public AbstractSearchPanel(final AbstractSearchModel model, final boolean includeActivateBtn, final boolean includeToggleAdvBtn) {
    if (model == null)
      throw new IllegalArgumentException("Can not construct a AbstractSearchPanel without a AbstractSearchModel");
    this.model = model;
    this.includeToggleSearchEnabledBtn = includeActivateBtn;
    this.includeToggleSearchAdvancedBtn = includeToggleAdvBtn;
    this.searchTypeCombo = initSearchTypeComboBox();
    this.upperBoundField = getInputField(true);
    this.lowerBoundField = isLowerBoundFieldRequired(model.getProperty()) ? getInputField(false) : null;

    this.toggleSearchEnabled = ControlProvider.createToggleButton(
            ControlFactory.toggleControl(model, "searchEnabled", null, model.stateSearchEnabled().eventStateChanged()));
    toggleSearchEnabled.setIcon(Images.loadImage(Images.IMG_FILTER_16));
    this.toggleSearchAdvanced = ControlProvider.createToggleButton(
            ControlFactory.toggleControl(this, "advancedSearchOn", null, stAdvancedSearch.eventStateChanged()));
    toggleSearchAdvanced.setIcon(Images.loadImage(Images.IMG_PREFERENCES_16));
    linkComponentsToLockedState();
    initUI();
    initializePanel();
    bindEvents();
  }

  /**
   * @return the search model this panel uses
   */
  public AbstractSearchModel getModel() {
    return this.model;
  }

  /**
   * @param value true if advanced search should be enabled
   */
  public void setAdvancedSearchOn(final boolean value) {
    stAdvancedSearch.setActive(value);
  }

  /**
   * @return true if the advanced search is enabled
   */
  public boolean isAdvancedSearchOn() {
    return stAdvancedSearch.isActive();
  }

  /**
   * @return the JComponent used to specify the upper bound
   */
  public JComponent getUpperBoundField() {
    return upperBoundField;
  }

  /**
   * @return the JComponent used to specify the lower bound
   */
  public JComponent getLowerBoundField() {
    return lowerBoundField;
  }

  public State stateAdvancedSearch() {
    return stAdvancedSearch;
  }

  public State stateTwoSearchFields() {
    return stTwoSearchFields;
  }

  /**
   * Binds events to relevant GUI actions
   */
  protected void bindEvents() {
    stAdvancedSearch.eventStateChanged().addListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        initializePanel();
        if (toggleSearchAdvanced != null)
          toggleSearchAdvanced.requestFocusInWindow();
        else
          upperBoundField.requestFocusInWindow();
      }
    });
    model.eventSearchTypeChanged().addListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        stTwoSearchFields.setActive(model.getSearchType() == SearchType.WITHIN_RANGE
                || model.getSearchType() == SearchType.OUTSIDE_RANGE);
      }
    });
    stTwoSearchFields.eventStateChanged().addListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        initializePanel();
        revalidate();
        searchTypeCombo.requestFocusInWindow();
      }
    });
  }

  protected void initializePanel() {
    if (stAdvancedSearch.isActive())
      initAdvancedPanel();
    else
      initSimplePanel();
  }

  /**
   * @return an ItemComboBoxModel containing the available search types
   */
  protected ItemComboBoxModel initSearchTypeModel() {
    final ItemComboBoxModel comboBoxModel = new ItemComboBoxModel();
    for (int i = 0; i < searchTypes.length; i++)
      if (searchTypeAllowed(searchTypes[i]))
        comboBoxModel.addElement(new ItemComboBoxModel.IconItem(searchTypes[i], Images.loadImage(searchTypeImageNames[i])));

    return comboBoxModel;
  }

  /**
   * @param searchType the search type
   * @return true if the given search type is allowed given the underlying property
   */
  protected abstract boolean searchTypeAllowed(final SearchType searchType);

  /**
   * @param isUpperBound true if the field should represent the upper bound, otherwise it should be the lower bound field
   * @return an input field for either the upper or lower bound
   */
  protected abstract JComponent getInputField(final boolean isUpperBound);

  /**
   * @param property the Property
   * @return true if a lower bound field is required given the property
   */
  protected abstract boolean isLowerBoundFieldRequired(final Property property);

  protected SimpleDateFormat getInputFormat() {
    if (model.getProperty().isValueClass(Timestamp.class))
      return Configuration.getDefaultTimestampFormat();
    if (model.getProperty().isValueClass(Date.class))
      return Configuration.getDefaultDateFormat();

    return null;
  }

  private JComboBox initSearchTypeComboBox() {
    final JComboBox comboBox = new SteppedComboBox(initSearchTypeModel());
    ControlProvider.bindItemSelector(comboBox, model, "searchType", SearchType.class, model.eventSearchTypeChanged());

    return comboBox;
  }

  private void initUI() {
    setLayout(new FlexibleGridLayout(2,1,1,1,true,false));
    ((FlexibleGridLayout)getLayout()).setFixedRowHeight(new JTextField().getSize().height);

    this.toggleSearchEnabled.setPreferredSize(new Dimension(20,20));
    this.toggleSearchAdvanced.setPreferredSize(new Dimension(20,20));
  }

  private void initSimplePanel() {
    removeAll();
    ((FlexibleGridLayout)getLayout()).setRows(1);
    final JPanel basePanel = new JPanel(new BorderLayout(1,1));
    if (stTwoSearchFields.isActive()) {
      final JPanel fieldBase = new JPanel(new GridLayout(1,2,1,1));
      fieldBase.add(upperBoundField);
      fieldBase.add(lowerBoundField);
      basePanel.add(fieldBase, BorderLayout.CENTER);
    }
    else
      basePanel.add(upperBoundField, BorderLayout.CENTER);

    if (includeToggleSearchEnabledBtn)
      basePanel.add(toggleSearchEnabled, BorderLayout.EAST);
    if (includeToggleSearchAdvancedBtn)
      basePanel.add(toggleSearchAdvanced, BorderLayout.WEST);

    add(basePanel);

    setPreferredSize(new Dimension(getPreferredSize().width, UiUtil.getPreferredTextFieldHeight()));

    revalidate();
  }

  private void initAdvancedPanel() {
    removeAll();
    ((FlexibleGridLayout)getLayout()).setRows(2);
    final JPanel inputPanel = new JPanel(new BorderLayout(1,1));
    if (stTwoSearchFields.isActive()) {
      final JPanel fieldBase = new JPanel(new GridLayout(1,2,1,1));
      fieldBase.add(lowerBoundField);
      fieldBase.add(upperBoundField);
      inputPanel.add(fieldBase, BorderLayout.CENTER);
    }
    else
      inputPanel.add(upperBoundField, BorderLayout.CENTER);

    final JPanel controlPanel = new JPanel(new BorderLayout(1,1));
    controlPanel.add(searchTypeCombo, BorderLayout.CENTER);
    if (includeToggleSearchEnabledBtn)
      controlPanel.add(toggleSearchEnabled, BorderLayout.EAST);
    if (includeToggleSearchAdvancedBtn)
      controlPanel.add(toggleSearchAdvanced, BorderLayout.WEST);

    add(controlPanel);
    add(inputPanel);

    setPreferredSize(new Dimension(getPreferredSize().width, UiUtil.getPreferredTextFieldHeight() * 2));

    revalidate();
  }

  private void linkComponentsToLockedState() {
    final State stUnlocked = model.stateLocked().getReversedState();
    UiUtil.linkToEnabledState(stUnlocked, searchTypeCombo);
    UiUtil.linkToEnabledState(stUnlocked, upperBoundField);
    if (lowerBoundField != null)
      UiUtil.linkToEnabledState(stUnlocked, lowerBoundField);
    UiUtil.linkToEnabledState(stUnlocked, toggleSearchAdvanced);
    UiUtil.linkToEnabledState(stUnlocked, toggleSearchEnabled);
  }
}
