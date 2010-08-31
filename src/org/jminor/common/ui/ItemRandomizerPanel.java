/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui;

import org.jminor.common.model.Event;
import org.jminor.common.model.Events;
import org.jminor.common.model.ItemRandomizer;
import org.jminor.common.model.ItemRandomizerModel;
import org.jminor.common.model.Util;
import org.jminor.common.ui.control.AbstractValueLink;
import org.jminor.common.ui.control.LinkType;

import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * A default UI for the ItemRandomizer class.
 */
public final class ItemRandomizerPanel<T> extends JPanel {

  private final ItemRandomizer<T> model;
  private final JPanel configPanel = new JPanel(new GridLayout(0, 1, 5, 5));
  private final JList itemList = new JList(new DefaultListModel());
  private final Event evtSelectedItemChanged = Events.event();

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
   * @param listener a listener notified each time the selected item changes
   */
  public void addSelectedItemListener(final ActionListener listener) {
    evtSelectedItemChanged.addListener(listener);
  }

  /**
   * @param listener the listener to remove
   */
  public void removeSelectedItemListener(final ActionListener listener) {
    evtSelectedItemChanged.removeListener(listener);
  }

  /**
   * @return the currently selected item
   */
  @SuppressWarnings({"unchecked"})
  public List<ItemRandomizer.RandomItem<T>> getSelectedItems() {
    final List<ItemRandomizer.RandomItem<T>> items = new ArrayList<ItemRandomizer.RandomItem<T>>();
    for (final Object object : itemList.getSelectedValues()) {
      items.add((ItemRandomizer.RandomItem<T>) object);
    }

    return items;
  }

  /**
   * Initializes the UI
   */
  private void initializeUI() {
    final List<ItemRandomizer.RandomItem<T>> items = new ArrayList<ItemRandomizer.RandomItem<T>>(model.getItems());
    Collections.sort(items, new Comparator<ItemRandomizer.RandomItem<T>>() {
      public int compare(final ItemRandomizer.RandomItem<T> o1, final ItemRandomizer.RandomItem<T> o2) {
        return o1.getItem().toString().compareTo(o2.getItem().toString());
      }
    });
    for (final ItemRandomizer.RandomItem<T> item : items) {
      ((DefaultListModel) itemList.getModel()).addElement(item);
    }
    itemList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    itemList.addListSelectionListener(new ListSelectionListener() {
      @SuppressWarnings({"unchecked"})
      public void valueChanged(final ListSelectionEvent e) {
        handleSelectionChanged();
        evtSelectedItemChanged.fire();
      }
    });
    setLayout(new BorderLayout(5, 5));
    add(new JScrollPane(itemList), BorderLayout.CENTER);
    add(configPanel, BorderLayout.SOUTH);
  }

  private void handleSelectionChanged() {
    configPanel.removeAll();
    for (final ItemRandomizer.RandomItem<T> item : getSelectedItems()) {
      configPanel.add(initializeWeightPanel(item));
    }
    revalidate();
  }

  /**
   * Returns a JPanel with controls configuring the weight of the given item
   * @param item the item for which to create a configuration panel
   * @return a conrol panel for the item weight
   */
  private JPanel initializeWeightPanel(final ItemRandomizerModel.RandomItem<T> item) {
    final JPanel panel = new JPanel(new GridLayout(1, 2, 5, 5));
    final JSpinner spinner = new JSpinner(createWeightSpinnerModel(item.getItem()));
    ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField().setColumns(3);
    spinner.setToolTipText(item.getItem().toString());
    
    panel.add(new JLabel(item.getItem().toString()));
    panel.add(spinner);

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