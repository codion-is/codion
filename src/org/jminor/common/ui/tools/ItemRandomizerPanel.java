/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.tools;

import org.jminor.common.model.Event;
import org.jminor.common.model.EventListener;
import org.jminor.common.model.EventObserver;
import org.jminor.common.model.Events;
import org.jminor.common.model.Util;
import org.jminor.common.model.Value;
import org.jminor.common.model.tools.ItemRandomizer;
import org.jminor.common.model.tools.ItemRandomizerModel;
import org.jminor.common.ui.UiUtil;
import org.jminor.common.ui.control.LinkType;
import org.jminor.common.ui.control.ValueLink;

import javax.swing.ButtonModel;
import javax.swing.DefaultListModel;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.BorderLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * A default UI for the ItemRandomizer class.
 */
public final class ItemRandomizerPanel<T> extends JPanel {

  private final ItemRandomizer<T> model;
  private final JPanel configPanel = new JPanel(UiUtil.createGridLayout(0, 1));
  private final JList itemList = new JList(new DefaultListModel());
  private final Event evtSelectedItemChanged = Events.event();

  /**
   * Instantiates a new RandomItemPanel.
   * @param itemRandomizer the ItemRandomizer to base this panel on
   * @throws IllegalArgumentException in case itemRandomizer is null
   */
  public ItemRandomizerPanel(final ItemRandomizer<T> itemRandomizer) {
    Util.rejectNullValue(itemRandomizer, "itemRandomizer");
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
  public void addSelectedItemListener(final EventListener listener) {
    evtSelectedItemChanged.addListener(listener);
  }

  /**
   * @param listener the listener to remove
   */
  public void removeSelectedItemListener(final EventListener listener) {
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
      @Override
      public int compare(final ItemRandomizer.RandomItem<T> o1, final ItemRandomizer.RandomItem<T> o2) {
        return o1.getItem().toString().compareTo(o2.getItem().toString());
      }
    });
    for (final ItemRandomizer.RandomItem<T> item : items) {
      ((DefaultListModel) itemList.getModel()).addElement(item);
    }
    itemList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    itemList.addListSelectionListener(new ListSelectionListener() {
      @Override
      @SuppressWarnings({"unchecked"})
      public void valueChanged(final ListSelectionEvent e) {
        handleSelectionChanged();
        evtSelectedItemChanged.fire();
      }
    });
    setLayout(UiUtil.createBorderLayout());
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
   * @return a control panel for the item weight
   */
  private JPanel initializeWeightPanel(final ItemRandomizerModel.RandomItem<T> item) {
    final JPanel panel = new JPanel(UiUtil.createBorderLayout());
    final JSpinner spinner = new JSpinner(createWeightSpinnerModel(item.getItem()));
    ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField().setColumns(3);
    spinner.setToolTipText(item.getItem().toString());
    final JCheckBox chkEnabled = createEnabledCheckBox(item.getItem());
    final JLabel lblWeight = new JLabel("Weight");
    lblWeight.setHorizontalAlignment(SwingConstants.RIGHT);

    panel.add(new JLabel(item.getItem().toString()), BorderLayout.NORTH);
    panel.add(chkEnabled, BorderLayout.WEST);
    panel.add(lblWeight, BorderLayout.CENTER);
    panel.add(spinner, BorderLayout.EAST);

    return panel;
  }

  /**
   * Returns a JCheckBox for controlling the enabled state of the given item.
   * @param item the item
   * @return an enabling JCheckBox
   */
  private JCheckBox createEnabledCheckBox(final T item) {
    final JCheckBox enabledBox = new JCheckBox("Enabled");
    new EnabledValueLink(enabledBox.getModel(), item);

    return enabledBox;
  }

  /**
   * Returns a SpinnerModel for controlling the weight of the given item.
   * @param item the item
   * @return a weight controlling SpinnerModel
   */
  private SpinnerModel createWeightSpinnerModel(final T item) {
    final SpinnerNumberModel spinnerModel = new SpinnerNumberModel(model.getWeight(item), 0, Integer.MAX_VALUE, 1);
    new WeightValueLink(spinnerModel, item);

    return spinnerModel;
  }

  private final class WeightValueLink extends ValueLink<Integer> {

    private WeightValueLink(final SpinnerNumberModel spinnerModel, final T item) {
      super(new Value<Integer>() {
              /** {@inheritDoc} */
              @Override
              public void set(final Integer value) {
                getModel().setWeight(item, value);
              }

              /** {@inheritDoc} */
              @Override
              public Integer get() {
                return getModel().getWeight(item);
              }

              /** {@inheritDoc} */
              @Override
              public EventObserver getChangeEvent() {
                return getModel().getWeightsObserver();
              }
            }, new Value<Integer>() {
              /** {@inheritDoc} */
              @Override
              public void set(final Integer value) {
                spinnerModel.setValue(value);
              }

              /** {@inheritDoc} */
              @Override
              public Integer get() {
                return (Integer) spinnerModel.getValue();
              }

              /** {@inheritDoc} */
              @Override
              public EventObserver getChangeEvent() {
                final Event changeEvent = Events.event();
                spinnerModel.addChangeListener(new ChangeListener() {
                  /** {@inheritDoc} */
                  @Override
                  public void stateChanged(final ChangeEvent e) {
                    changeEvent.fire();
                  }
                });
                return changeEvent.getObserver();
              }
            }, LinkType.READ_WRITE
      );
    }
  }

  private final class EnabledValueLink extends ValueLink<Boolean> {

    private EnabledValueLink(final ButtonModel buttonModel, final T item) {
      super(new Value<Boolean>() {
              /** {@inheritDoc} */
              @Override
              public void set(final Boolean value) {
                getModel().setItemEnabled(item, value);
              }

              /** {@inheritDoc} */
              @Override
              public Boolean get() {
                return getModel().isItemEnabled(item);
              }

              /** {@inheritDoc} */
              @Override
              public EventObserver getChangeEvent() {
                return getModel().getEnabledObserver();
              }
            }, new Value<Boolean>() {
              /** {@inheritDoc} */
              @Override
              public void set(final Boolean value) {
                buttonModel.setSelected(value);
              }

              /** {@inheritDoc} */
              @Override
              public Boolean get() {
                return buttonModel.isSelected();
              }

              /** {@inheritDoc} */
              @Override
              public EventObserver getChangeEvent() {
                final Event changeEvent = Events.event();
                buttonModel.addItemListener(new ItemListener() {
                  /** {@inheritDoc} */
                  @Override
                  public void itemStateChanged(final ItemEvent e) {
                    changeEvent.fire();
                  }
                });

                return changeEvent.getObserver();
              }
            }, LinkType.READ_WRITE
      );
    }
  }
}