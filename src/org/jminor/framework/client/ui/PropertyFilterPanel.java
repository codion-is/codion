/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui;

import org.jminor.common.model.DateUtil;
import org.jminor.common.model.SearchType;
import org.jminor.common.model.State;
import org.jminor.common.ui.AbstractSearchPanel;
import org.jminor.common.ui.UiUtil;
import org.jminor.common.ui.control.DoubleBeanValueLink;
import org.jminor.common.ui.control.FormattedTextBeanValueLink;
import org.jminor.common.ui.control.IntBeanValueLink;
import org.jminor.common.ui.control.LinkType;
import org.jminor.common.ui.control.TextBeanValueLink;
import org.jminor.common.ui.control.ToggleBeanValueLink;
import org.jminor.common.ui.textfield.DoubleField;
import org.jminor.common.ui.textfield.IntField;
import org.jminor.framework.Configuration;
import org.jminor.framework.client.model.PropertyFilterModel;
import org.jminor.framework.domain.Property;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.SimpleDateFormat;
import java.util.Date;

public class PropertyFilterPanel extends AbstractSearchPanel<Property> {

  private final State stIsDialogActive = new State();
  private final State stIsDialogShowing = new State();

  private JDialog dialog;
  private Point lastPosition;

  public PropertyFilterPanel(final PropertyFilterModel model) {
    this(model, false, false);
  }

  public PropertyFilterPanel(final PropertyFilterModel model, final boolean includeActivateBtn,
                             final boolean includeToggleAdvBtn) {
    super(model, includeActivateBtn, includeToggleAdvBtn);
  }

  /**
   * @return the last screen position
   */
  public Point getLastPosition() {
    return lastPosition;
  }

  /**
   * @return true if the dialog is active
   */
  public boolean isDialogActive() {
    return stIsDialogActive.isActive();
  }

  /**
   * @return true if the dialog is being shown
   */
  public boolean isDialogShowing() {
    return stIsDialogShowing.isActive();
  }

  public void activateDialog(final Container dialogParent, final Point position) {
    if (!isDialogActive()) {
      initSearchDlg(dialogParent);
      Point actualPosition = null;
      if (position == null) {
        actualPosition = lastPosition;
      }
      if (actualPosition == null) {
        actualPosition = new Point(0, 0);
      }

      actualPosition.y = actualPosition.y - dialog.getHeight();
      dialog.setLocation(actualPosition);
      stIsDialogActive.setActive(true);
    }

    showDialog();
  }

  public void inactivateDialog() {
    if (isDialogActive()) {
      if (isDialogShowing()) {
        hideDialog();
      }
      lastPosition = dialog.getLocation();
      lastPosition.y = lastPosition.y + dialog.getHeight();
      dialog.dispose();
      dialog = null;

      stIsDialogActive.setActive(false);
    }
  }

  public void showDialog() {
    if (isDialogActive() && !isDialogShowing()) {
      dialog.setVisible(true);
      getUpperBoundField().requestFocusInWindow();
      stIsDialogShowing.setActive(true);
    }
  }

  public void hideDialog() {
    if (isDialogShowing()) {
      dialog.setVisible(false);
      stIsDialogShowing.setActive(false);
    }
  }

  /**
   * @return the dialog used to show this filter panel
   */
  public JDialog getDialog() {
    return dialog;
  }

  public State stateIsDialogActive() {
    return stIsDialogActive.getLinkedState();
  }

  public State stateIsDialogShowing() {
    return stIsDialogShowing.getLinkedState();
  }

  /** {@inheritDoc} */
  @Override
  protected boolean isLowerBoundFieldRequired(final Property property) {
    return property.isBoolean();
  }

  /** {@inheritDoc} */
  @Override
  protected boolean searchTypeAllowed(final SearchType searchType) {
    return true;
  }

  /** {@inheritDoc} */
  @Override
  protected SimpleDateFormat getInputFormat() {
    if (getModel().getType() == Types.TIMESTAMP) {
      return Configuration.getDefaultTimestampFormat();
    }
    if (getModel().getType() == Types.DATE) {
      return Configuration.getDefaultDateFormat();
    }

    return null;
  }

  /** {@inheritDoc} */
  @Override
  protected JComponent getInputField(final boolean isUpperBound) {
    final SimpleDateFormat format = getInputFormat();
    final JComponent field = initField(format);
    if (getModel().getType() == Types.BOOLEAN) {
      createToggleProperty((JCheckBox) field, isUpperBound);
    }
    else {
      createTextProperty(field, isUpperBound, format);
    }

    if (field instanceof JTextField) {//enter button toggles the filter on/off
      ((JTextField) field).addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          getModel().setSearchEnabled(!getModel().isSearchEnabled());
        }
      });
    }
    field.setToolTipText(isUpperBound ? "a" : "b");

    return field;
  }

  private JComponent initField(final SimpleDateFormat format) {
    final Property property = getModel().getSearchProperty();
    if (property.isTime()) {
      return UiUtil.createFormattedField(DateUtil.getDateMask(format));
    }
    else if (property.isDouble()) {
      return new DoubleField(4);
    }
    else if (property.isInteger()) {
      return new IntField(4);
    }
    else if (property.isBoolean()) {
      return new JCheckBox();
    }
    else {
      return new JTextField(4);
    }
  }

  private void initSearchDlg(Container parent) {
    if (dialog != null) {
      return;
    }

    final JDialog dlgParent = UiUtil.getParentDialog(parent);
    if (dlgParent != null) {
      dialog = new JDialog(dlgParent, getModel().getSearchProperty().getCaption(), false);
    }
    else {
      dialog = new JDialog(UiUtil.getParentFrame(parent), getModel().getSearchProperty().getCaption(), false);
    }

    final JPanel searchPanel = new JPanel(new BorderLayout());
    searchPanel.add(this, BorderLayout.NORTH);
    dialog.getContentPane().add(searchPanel);
    dialog.pack();

    stateAdvancedSearch().eventStateChanged().addListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        dialog.pack();
      }
    });

    dialog.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent e) {
        inactivateDialog();
      }
    });
  }

  private void createToggleProperty(final JCheckBox checkBox, final boolean isUpperBound) {
    new ToggleBeanValueLink(checkBox.getModel(), getModel(),
            isUpperBound ? PropertyFilterModel.UPPER_BOUND_PROPERTY : PropertyFilterModel.LOWER_BOUND_PROPERTY,
            isUpperBound ? getModel().eventUpperBoundChanged() : getModel().eventLowerBoundChanged());
  }

  private TextBeanValueLink createTextProperty(final JComponent component, boolean isUpper, final SimpleDateFormat format) {
    final Property property = getModel().getSearchProperty();
    if (property.isInteger()) {
      return new IntBeanValueLink((IntField) component, getModel(),
              isUpper ? PropertyFilterModel.UPPER_BOUND_PROPERTY : PropertyFilterModel.LOWER_BOUND_PROPERTY,
              isUpper ? getModel().eventUpperBoundChanged() : getModel().eventLowerBoundChanged());
    }
    if (property.isDouble()) {
      return new DoubleBeanValueLink((DoubleField) component, getModel(),
              isUpper ? PropertyFilterModel.UPPER_BOUND_PROPERTY : PropertyFilterModel.LOWER_BOUND_PROPERTY,
              isUpper ? getModel().eventUpperBoundChanged() : getModel().eventLowerBoundChanged());
    }
    if (property.isTime()) {
      return new FormattedTextBeanValueLink((JFormattedTextField) component, getModel(),
              isUpper ? PropertyFilterModel.UPPER_BOUND_PROPERTY : PropertyFilterModel.LOWER_BOUND_PROPERTY,
              property.isTimestamp() ? Timestamp.class : Date.class,
              isUpper ? getModel().eventUpperBoundChanged() : getModel().eventLowerBoundChanged(), LinkType.READ_WRITE, format) {
        @Override
        protected Object getUIValue() {
          final Date date = (Date) super.getUIValue();
          return date == null ? null : property.isTimestamp() ? new Timestamp(date.getTime()) : date;
        }
      };
    }

    return new TextBeanValueLink((JTextField) component, getModel(),
            isUpper ? PropertyFilterModel.UPPER_BOUND_PROPERTY : PropertyFilterModel.LOWER_BOUND_PROPERTY,
            String.class, isUpper ? getModel().eventUpperBoundChanged() : getModel().eventLowerBoundChanged());
  }
}