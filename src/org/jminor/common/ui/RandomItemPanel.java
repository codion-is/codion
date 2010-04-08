/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui;

import org.jminor.common.model.RandomItemModel;
import org.jminor.common.ui.control.AbstractPropertyLink;
import org.jminor.common.ui.control.Control;
import org.jminor.common.ui.control.LinkType;
import org.jminor.common.ui.textfield.IntField;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;

/**
 * A default UI for the RandomItemModel.
 */
public class RandomItemPanel extends JPanel {

  private final RandomItemModel model;

  /**
   * Instantiates a new RandomItemPanel.
   * @param model the RandomItemModel to base this panel on
   */
  public RandomItemPanel(final RandomItemModel model) {
    if (model == null)
      throw new IllegalArgumentException("Model can not be null");

    this.model = model;
    initializeUI();
  }

  /**
   * Initializes the UI
   */
  protected void initializeUI() {
    final int count = model.getItemCount();
    setLayout(new GridLayout(count, 1, 0, 0));
    for (final RandomItemModel.RandomItem item : model.getItems())
      add(initializeWeightPanel(item));
  }

  /**
   * Returns a JPanel with controls configuring the weight of the given item
   * @param item the item for which to create a configuration panel
   * @return a conrol panel for the item weight
   */
  protected JPanel initializeWeightPanel(final RandomItemModel.RandomItem item) {
    final JPanel panel = new JPanel(new BorderLayout(0, 0));

    panel.add(initializeDisplayField(item), BorderLayout.CENTER);
    panel.add(initializeWeightControlPanel(item), BorderLayout.EAST);
    panel.setBorder(BorderFactory.createTitledBorder(item.getItem().toString()));

    return panel;
  }

  /**
   * Returns a JTextField bound to the weight of the given item
   * @param item the item
   * @return a JTextField displaying the weight value of the given item
   */
  protected JTextField initializeDisplayField(final RandomItemModel.RandomItem item) {
    final IntField txtValue = new IntField();
    txtValue.setToolTipText(item.getItem().toString());
    txtValue.setPreferredSize(UiUtil.getPreferredTextFieldSize());
    txtValue.setHorizontalAlignment(JTextField.CENTER);
    txtValue.setEditable(false);
    new AbstractPropertyLink(model, model.eventWeightsChanged(), LinkType.READ_ONLY) {
      @Override
      public Object getModelPropertyValue() {
        return model.getWeight(item.getItem());
      }
      @Override
      protected Object getUIPropertyValue() {
        return txtValue.getInt();
      }
      @Override
      public void setModelPropertyValue(final Object value) {}
      @Override
      protected void setUIPropertyValue(final Object propertyValue) {
        txtValue.setInt((Integer) propertyValue);
      }
    }.updateUI();

    return txtValue;
  }

  /**
   * Returns a JPanel containing controls for adjusting the weight of the given item
   * @param item the item
   * @return a control panel for the item weight
   */
  protected JPanel initializeWeightControlPanel(final RandomItemModel.RandomItem item) {
    final JPanel btnPanel = new JPanel(new GridLayout(1, 2, 0, 0));
    btnPanel.add(initializeWeightButton(item, false));
    btnPanel.add(initializeWeightButton(item, true));

    return btnPanel;
  }

  /**
   * Returns a JButton for either incrementing or decrementing the weight of the given item.
   * @param item the item
   * @param increment if true then a 'increment' button is returned otherwise a 'decrement' button
   * @return a button for adjusting the item weight
   */
  protected JButton initializeWeightButton(final RandomItemModel.RandomItem item, final boolean increment) {
    final JButton btn = new JButton(new Control(increment ? "+" : "-") {
      @Override
      public void actionPerformed(final ActionEvent e) {
        if (increment)
          model.increment(item.getItem());
        else
          model.decrement(item.getItem());
      }
    });
    btn.setPreferredSize(UiUtil.DIMENSION_TEXT_FIELD_SQUARE);
    btn.setMargin(new Insets(0, 0, 0, 0));

    return btn;
  }
}