/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui;

import org.jminor.common.model.SearchType;
import org.jminor.common.model.State;
import org.jminor.common.model.combobox.ItemComboBoxModel;
import org.jminor.common.ui.ControlProvider;
import org.jminor.common.ui.UiUtil;
import org.jminor.common.ui.combobox.SteppedComboBox;
import org.jminor.common.ui.control.ControlFactory;
import org.jminor.common.ui.images.Images;
import org.jminor.common.ui.layout.FlexibleGridLayout;
import org.jminor.framework.client.model.AbstractSearchModel;
import org.jminor.framework.client.model.combobox.BooleanComboBoxModel;
import org.jminor.framework.model.Type;

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

/**
 * User: Björn Darri
 * Date: 26.12.2007
 * Time: 15:17:20
 */
public abstract class AbstractSearchPanel extends JPanel {

  public final State stAdvancedSearch = new State("AbstractSearchPanel.stAdvancedSearch");

  public final State stTwoSearchFields = new State("AbstractSearchPanel.stTwoSearchFields", false);

  protected static final SearchType[] searchTypes = new SearchType[] {
          SearchType.LIKE, SearchType.NOT_LIKE, SearchType.MAX, SearchType.MIN,
          SearchType.INSIDE, SearchType.OUTSIDE};

  protected static final String[] searchTypeImageNames = new String[] {
          "Equals60x16.gif", "NotEquals60x16.gif", "LessThanOrEquals60x16.gif",
          "LargerThanOrEquals60x16.gif", "Inclusive60x16.gif", "Exclusive60x16.gif"};

  /**
   * The AbstractSearchModel this AbstractSearchPanel represents
   */
  protected final AbstractSearchModel model;

  /**
   * A JToggleButton for enabling/disabling the filter
   */
  protected final JToggleButton toggleSearchEnabled;

  /**
   * A JToggleButton for toggling advanced/simple search
   */
  protected final JToggleButton toggleSearchAdvanced;

  /**
   * A JComboBox for selecting the search type
   */
  protected final JComboBox searchTypeCombo;
  protected final BooleanComboBoxModel upperBooleanComboBoxModel;
  protected final BooleanComboBoxModel lowerBooleanComboBoxModel;
  protected final JComponent upperField;
  protected final JComponent lowerField;

  private boolean includeToggleSearchEnabledBtn = false;
  private boolean includeToggleSearchAdvancedBtn = false;

  public AbstractSearchPanel(final AbstractSearchModel model, final boolean includeActivateBtn, final boolean includeToggleAdvBtn) {
    try {
      this.model = model;
      this.includeToggleSearchEnabledBtn = includeActivateBtn;
      this.includeToggleSearchAdvancedBtn = includeToggleAdvBtn;
      this.upperBooleanComboBoxModel = model.getColumnType() == Type.BOOLEAN ? new BooleanComboBoxModel() : null;
      this.lowerBooleanComboBoxModel = model.getColumnType() == Type.BOOLEAN ? new BooleanComboBoxModel() : null;
      this.searchTypeCombo = initSearchTypeComboBox();
      this.upperField = getInputField(true);
      this.lowerField = isLowerFieldRequired(model.getColumnType()) ? getInputField(false) : null;

      this.toggleSearchEnabled = ControlProvider.createToggleButton(
              ControlFactory.toggleControl(model, "searchEnabled", null, model.stSearchEnabled.evtStateChanged));
      toggleSearchEnabled.setIcon(Images.loadImage("Filter16.gif"));
      this.toggleSearchAdvanced = ControlProvider.createToggleButton(
              ControlFactory.toggleControl(this, "advancedSearchOn", null, stAdvancedSearch.evtStateChanged));
      toggleSearchAdvanced.setIcon(Images.loadImage(Images.IMG_PREFERENCES_16));

      initUI();
      initializePanel();
      bindEvents();
    }
    catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e.getMessage());
    }
  }

  /**
   * @return Value for property 'model'.
   */
  public AbstractSearchModel getModel() {
    return this.model;
  }

  /**
   * @param value Value to set for property 'advancedSearchOn'.
   */
  public void setAdvancedSearchOn(final boolean value) {
    stAdvancedSearch.setActive(value);
  }

  /**
   * @return Value for property 'advancedSearchOn'.
   */
  public boolean isAdvancedSearchOn() {
    return stAdvancedSearch.isActive();
  }

  /**
   * @return Value for property 'upperField'.
   */
  public JComponent getUpperField() {
    return upperField;
  }

  /**
   * @return Value for property 'lowerField'.
   */
  public JComponent getLowerField() {
    return lowerField;
  }

  /**
   * Binds ModelEvents to relevant GUI actions
   */
  protected void bindEvents() {
    stAdvancedSearch.evtStateChanged.addListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        initializePanel();
        if (toggleSearchAdvanced != null)
          toggleSearchAdvanced.requestFocusInWindow();
        else
          upperField.requestFocusInWindow();
      }
    });
    model.evtSearchTypeChanged.addListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        stTwoSearchFields.setActive(model.getSearchType() == SearchType.INSIDE
                || model.getSearchType() == SearchType.OUTSIDE);
      }
    });
    stTwoSearchFields.evtStateChanged.addListener(new ActionListener() {
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
    final ItemComboBoxModel ret = new ItemComboBoxModel();
    for (int i = 0; i < searchTypes.length; i++)
      if (searchTypeAllowed(searchTypes[i]))
        ret.addElement(new ItemComboBoxModel.IconItem(searchTypes[i],Images.loadImage(searchTypeImageNames[i])));

    return ret;
  }

  protected abstract boolean searchTypeAllowed(final SearchType searchType);

  protected abstract JComponent getInputField(final boolean isUpperBound);

  protected abstract boolean isLowerFieldRequired(final Type type);

  private JComboBox initSearchTypeComboBox() {
    final JComboBox ret = new SteppedComboBox(initSearchTypeModel());
    ControlProvider.bindItemSelector(ret, model, "searchType", SearchType.class,
            model.evtSearchTypeChanged, null);

    return ret;
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
      fieldBase.add(upperField);
      fieldBase.add(lowerField);
      basePanel.add(fieldBase, BorderLayout.CENTER);
    }
    else
      basePanel.add(upperField, BorderLayout.CENTER);

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
      fieldBase.add(lowerField);
      fieldBase.add(upperField);
      inputPanel.add(fieldBase, BorderLayout.CENTER);
    }
    else
      inputPanel.add(upperField, BorderLayout.CENTER);

    final JPanel controlPanel = new JPanel(new BorderLayout(1,1));
    controlPanel.add(searchTypeCombo, BorderLayout.CENTER);
    if (includeToggleSearchEnabledBtn)
      controlPanel.add(toggleSearchEnabled, BorderLayout.EAST);
    if (includeToggleSearchAdvancedBtn)
      controlPanel.add(toggleSearchAdvanced, BorderLayout.WEST);

    add(controlPanel);
    add(inputPanel);

    setPreferredSize(new Dimension(getPreferredSize().width, UiUtil.getPreferredTextFieldHeight()*2));

    revalidate();
  }
}
