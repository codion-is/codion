/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui;

import org.jminor.common.model.ItemRandomizer;
import org.jminor.common.model.ItemRandomizerModel;
import org.jminor.common.model.Util;
import org.jminor.common.ui.control.AbstractValueLink;
import org.jminor.common.ui.control.LinkType;
import org.jminor.common.ui.layout.FlexibleGridLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.BorderLayout;

/**
 * A default UI for the ItemRandomizer class.
 */
public final class ItemRandomizerPanel<T> extends JPanel {

  private final ItemRandomizer<T> model;

  /**
   * Instantiates a new RandomItemPanel.
   * @param itemRandomizer the ItemRandomizer to base this panel on
   */
  public ItemRandomizerPanel(final ItemRandomizer<T> itemRandomizer) {
    Util.rejectNullValue(itemRandomizer, "model");
    this.model = itemRandomizer;
    initializeUI();
  }

  /**
   * @return the randomizer this panel is based on
   */
  public ItemRandomizer<T> getModel() {
    return model;
  }

  /**
   * Initializes the UI
   */
  private void initializeUI() {
    final int count = model.getItemCount();
    setLayout(new FlexibleGridLayout(count * 2, 1, 5, 5, true, false));
    for (final ItemRandomizer.RandomItem<T> item : model.getItems()) {
      add(new JLabel(item.getItem().toString()));
      add(initializeWeightPanel(item));
    }
  }

  /**
   * Returns a JPanel with controls configuring the weight of the given item
   * @param item the item for which to create a configuration panel
   * @return a conrol panel for the item weight
   */
  private JPanel initializeWeightPanel(final ItemRandomizerModel.RandomItem<T> item) {
    final JPanel panel = new JPanel(new BorderLayout(0, 0));
    final JSpinner spinner = new JSpinner(createWeightSpinnerModel(item.getItem()));
    spinner.setToolTipText(item.getItem().toString());
    panel.add(spinner, BorderLayout.CENTER);

    return panel;
  }

  /**
   * Returns a SpinnerModel for controlling the weight of the given item.
   * @param item the item
   * @return a weight controlling SpinnerModel
   */
  private SpinnerModel createWeightSpinnerModel(final T item) {
    final SpinnerNumberModel spinnerModel = new SpinnerNumberModel(model.getWeight(item), 0, Integer.MAX_VALUE, 1);
    final AbstractValueLink<ItemRandomizerPanel, Integer> valueLink = new WeightValueLink(spinnerModel, item);
    spinnerModel.addChangeListener(new ChangeListener() {
      /** {@inheritDoc} */
      public void stateChanged(final ChangeEvent e) {
        valueLink.updateModel();
      }
    });

    return spinnerModel;
  }

  private final class WeightValueLink extends AbstractValueLink<ItemRandomizerPanel, Integer> {

    private final SpinnerNumberModel spinnerModel;
    private final T item;

    private WeightValueLink(final SpinnerNumberModel spinnerModel, final T item) {
      super(ItemRandomizerPanel.this, model.getWeightsObserver(), LinkType.READ_WRITE);
      this.spinnerModel = spinnerModel;
      this.item = item;
      updateUI();
    }

    /** {@inheritDoc} */
    @Override
    public Integer getModelValue() {
      return getModel().getWeight(item);
    }

    /** {@inheritDoc} */
    @Override
    public void setModelValue(final Integer value) {
      getModel().setWeight(item, value);
    }

    /** {@inheritDoc} */
    @Override
    protected Integer getUIValue() {
      return (Integer) spinnerModel.getValue();
    }

    /** {@inheritDoc} */
    @Override
    protected void setUIValue(final Integer value) {
      spinnerModel.setValue(value);
    }
  }
}