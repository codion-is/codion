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

  public RandomItemPanel(final RandomItemModel model) {
    if (model == null)
      throw new IllegalArgumentException("Model can not be null");

    this.model = model;
    initUI();
  }

  private void initUI() {
    final int count = model.getItemCount();
    setLayout(new GridLayout(count, 1, 5, 5));
    for (final RandomItemModel.RandomItem item : model.getItems())
      add(createWeightPanel(item));
  }

  protected JPanel createWeightPanel(final RandomItemModel.RandomItem item) {
    final JPanel panel = new JPanel(new BorderLayout(5, 5));
    final Control incrementControl = new Control("+") {
      @Override
      public void actionPerformed(final ActionEvent e) {
        model.increment(item.getItem());
      }
    };
    final Control decrementControl = new Control("-") {
      @Override
      public void actionPerformed(final ActionEvent e) {
        model.decrement(item.getItem());
      }
    };
    final IntField txtValue = new IntField();
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

    JButton btn = new JButton(decrementControl);
    btn.setPreferredSize(UiUtil.DIMENSION_TEXT_FIELD_SQUARE);
    btn.setMargin(new Insets(0,0,0,0));
    panel.add(btn, BorderLayout.WEST);
    panel.add(txtValue, BorderLayout.CENTER);
    btn = new JButton(incrementControl);
    btn.setPreferredSize(UiUtil.DIMENSION_TEXT_FIELD_SQUARE);
    btn.setMargin(new Insets(0,0,0,0));
    panel.add(btn, BorderLayout.EAST);

    panel.setBorder(BorderFactory.createTitledBorder(item.getItem().toString()));

    return panel;
  }
}