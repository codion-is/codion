/*
 * Copyright (c) 2010 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.tools.ui.randomizer;

import is.codion.common.event.Event;
import is.codion.common.event.EventDataListener;
import is.codion.common.value.AbstractValue;
import is.codion.swing.common.tools.randomizer.ItemRandomizer;
import is.codion.swing.common.ui.component.Components;
import is.codion.swing.common.ui.layout.Layouts;

import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * A default UI for the ItemRandomizer class.
 * @param <T> the type of items being randomized
 */
public final class ItemRandomizerPanel<T> extends JPanel {

  private static final int SPINNER_COLUMNS = 3;

  private final ItemRandomizer<T> model;
  private final JPanel configPanel = new JPanel(Layouts.gridLayout(0, 1));
  private final JList<ItemRandomizer.RandomItem<T>> itemList = new JList<>(new DefaultListModel<>());
  private final Event<List<ItemRandomizer.RandomItem<T>>> selectedItemChangedEvent = Event.event();

  /**
   * Instantiates a new RandomItemPanel.
   * @param itemRandomizer the ItemRandomizer to base this panel on
   * @throws NullPointerException in case itemRandomizer is null
   */
  public ItemRandomizerPanel(ItemRandomizer<T> itemRandomizer) {
    this.model = requireNonNull(itemRandomizer, "itemRandomizer");
    initializeUI();
  }

  /**
   * @return the randomizer this panel is based on
   */
  public ItemRandomizer<T> model() {
    return model;
  }

  /**
   * @param listener a listener notified each time the selected item changes
   */
  public void addSelectedItemListener(EventDataListener<List<ItemRandomizer.RandomItem<T>>> listener) {
    selectedItemChangedEvent.addDataListener(listener);
  }

  /**
   * @param listener the listener to remove
   */
  public void removeSelectedItemListener(EventDataListener<List<ItemRandomizer.RandomItem<T>>> listener) {
    selectedItemChangedEvent.removeDataListener(listener);
  }

  /**
   * @return the currently selected item
   */
  public List<ItemRandomizer.RandomItem<T>> selectedItems() {
    return itemList.getSelectedValuesList();
  }

  /**
   * Initializes the UI
   */
  private void initializeUI() {
    List<ItemRandomizer.RandomItem<T>> items = new ArrayList<>(model.items());
    items.sort(Comparator.comparing(item -> item.item().toString()));
    items.forEach(((DefaultListModel<ItemRandomizer.RandomItem<T>>) itemList.getModel())::addElement);
    itemList.addListSelectionListener(e -> selectedItemChangedEvent.onEvent(itemList.getSelectedValuesList()));
    addSelectedItemListener(selectedItems -> {
      configPanel.removeAll();
      for (ItemRandomizer.RandomItem<T> item : selectedItems) {
        configPanel.add(createWeightPanel(item));
      }
      revalidate();
    });
    setLayout(Layouts.borderLayout());
    add(new JScrollPane(itemList), BorderLayout.CENTER);
    add(configPanel, BorderLayout.SOUTH);
  }

  /**
   * Returns a JPanel with controls configuring the weight of the given item
   * @param item the item for which to create a configuration panel
   * @return a control panel for the item weight
   */
  private JPanel createWeightPanel(ItemRandomizer.RandomItem<T> item) {
    JPanel panel = new JPanel(Layouts.borderLayout());
    panel.add(new JLabel(item.item().toString()), BorderLayout.NORTH);
    panel.add(Components.checkBox(new EnabledModelValue(item.item()))
            .caption("Enabled")
            .build(), BorderLayout.WEST);
    panel.add(Components.label("Weight")
            .horizontalAlignment(SwingConstants.RIGHT)
            .build(), BorderLayout.CENTER);
    panel.add(Components.integerSpinner(new WeightModelValue(item.item()))
            .minimum(0)
            .columns(SPINNER_COLUMNS)
            .toolTipText(item.item().toString())
            .build(), BorderLayout.EAST);

    return panel;
  }

  private final class EnabledModelValue extends AbstractValue<Boolean> {

    private final T item;

    private EnabledModelValue(T item) {
      super(false);
      this.item = item;
    }

    @Override
    public Boolean get() {
      return model.isItemEnabled(item);
    }

    @Override
    protected void setValue(Boolean value) {
      model.setItemEnabled(item, value);
    }
  }

  private final class WeightModelValue extends AbstractValue<Integer> {

    private final T item;

    private WeightModelValue(T item) {
      super(0);
      this.item = item;
    }

    @Override
    public Integer get() {
      return model.weight(item);
    }

    @Override
    protected void setValue(Integer value) {
      model.setWeight(item, value);
    }
  }
}