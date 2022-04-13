/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import javax.swing.JComponent;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.util.Objects.requireNonNull;

final class DefaultTabbedPaneBuilder extends AbstractComponentBuilder<Void, JTabbedPane, TabbedPaneBuilder> implements TabbedPaneBuilder {

  private int tabPlacement = SwingConstants.TOP;
  private final Map<String, JComponent> tabs = new LinkedHashMap<>();
  private final Map<Integer, Integer> mnemonicAt = new HashMap<>();
  private final List<ChangeListener> changeListeners = new ArrayList<>();

  @Override
  public TabbedPaneBuilder tabPlacement(int tabPlacement) {
    this.tabPlacement = tabPlacement;
    return this;
  }

  @Override
  public TabbedPaneBuilder tab(String title, JComponent component) {
    tabs.put(requireNonNull(title), requireNonNull(component));
    return this;
  }

  @Override
  public TabbedPaneBuilder tab(String title, int mnemonic, JComponent component) {
    mnemonicAt.put(tabs.size(), mnemonic);
    return tab(title, component);
  }

  @Override
  public TabbedPaneBuilder changeListener(ChangeListener changeListener) {
    changeListeners.add(requireNonNull(changeListener));
    return this;
  }

  @Override
  protected JTabbedPane createComponent() {
    JTabbedPane tabbedPane = new JTabbedPane(tabPlacement);
    tabs.forEach(tabbedPane::addTab);
    mnemonicAt.forEach(tabbedPane::setMnemonicAt);
    changeListeners.forEach(tabbedPane::addChangeListener);

    return tabbedPane;
  }

  @Override
  protected ComponentValue<Void, JTabbedPane> createComponentValue(JTabbedPane component) {
    throw new UnsupportedOperationException("A ComponentValue can not be based on a JTabbedPane");
  }

  @Override
  protected void setInitialValue(JTabbedPane component, Void initialValue) {}
}
