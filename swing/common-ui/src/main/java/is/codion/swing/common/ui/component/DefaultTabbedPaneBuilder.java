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
  public TabbedPaneBuilder tabPlacement(final int tabPlacement) {
    this.tabPlacement = tabPlacement;
    return this;
  }

  @Override
  public TabbedPaneBuilder tab(final String title, final JComponent component) {
    tabs.put(requireNonNull(title), requireNonNull(component));
    return this;
  }

  @Override
  public TabbedPaneBuilder mnemonicAt(final int index, final int mnemonic) {
    mnemonicAt.put(index, mnemonic);
    return this;
  }

  @Override
  public TabbedPaneBuilder changeListener(final ChangeListener changeListener) {
    changeListeners.add(requireNonNull(changeListener));
    return this;
  }

  @Override
  protected JTabbedPane buildComponent() {
    final JTabbedPane tabbedPane = new JTabbedPane(tabPlacement);
    tabs.forEach(tabbedPane::addTab);
    mnemonicAt.forEach(tabbedPane::setMnemonicAt);
    changeListeners.forEach(tabbedPane::addChangeListener);

    return tabbedPane;
  }

  @Override
  protected ComponentValue<Void, JTabbedPane> buildComponentValue(final JTabbedPane component) {
    throw new UnsupportedOperationException("A ComponentValue can not be based on a JTabbedPane");
  }

  @Override
  protected void setInitialValue(final JTabbedPane component, final Void initialValue) {}
}
