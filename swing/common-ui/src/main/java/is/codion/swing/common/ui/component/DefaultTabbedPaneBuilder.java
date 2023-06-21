/*
 * Copyright (c) 2022 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.swing.common.ui.component.builder.AbstractComponentBuilder;
import is.codion.swing.common.ui.component.value.ComponentValue;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeListener;
import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.requireNonNull;

final class DefaultTabbedPaneBuilder extends AbstractComponentBuilder<Void, JTabbedPane, TabbedPaneBuilder> implements TabbedPaneBuilder {

  private int tabPlacement = SwingConstants.TOP;
  private int tabLayoutPolicy = JTabbedPane.WRAP_TAB_LAYOUT;
  private final List<DefaultTabBuilder> tabBuilders = new ArrayList<>();
  private final List<ChangeListener> changeListeners = new ArrayList<>();

  @Override
  public TabbedPaneBuilder tabPlacement(int tabPlacement) {
    this.tabPlacement = tabPlacement;
    return this;
  }

  @Override
  public TabbedPaneBuilder tabLayoutPolicy(int tabLayoutPolicy) {
    this.tabLayoutPolicy = tabLayoutPolicy;
    return this;
  }

  @Override
  public TabbedPaneBuilder changeListener(ChangeListener changeListener) {
    changeListeners.add(requireNonNull(changeListener));
    return this;
  }

  @Override
  public TabbedPaneBuilder tab(String title, JComponent component) {
    new DefaultTabBuilder(this, requireNonNull(title), component).add();
    return this;
  }

  @Override
  public TabBuilder tabBuilder(JComponent component) {
    return new DefaultTabBuilder(this, null, component);
  }

  @Override
  public TabBuilder tabBuilder(String title, JComponent component) {
    return new DefaultTabBuilder(this, requireNonNull(title), component);
  }

  @Override
  protected JTabbedPane createComponent() {
    JTabbedPane tabbedPane = new JTabbedPane(tabPlacement, tabLayoutPolicy);
    tabBuilders.forEach(tabBuilder -> {
      int tabIndex = tabbedPane.getTabCount();
      tabbedPane.addTab(tabBuilder.title, tabBuilder.icon, tabBuilder.component, tabBuilder.toolTipText);
      if (tabBuilder.mnemonic != 0) {
        tabbedPane.setMnemonicAt(tabIndex, tabBuilder.mnemonic);
      }
      if (tabBuilder.tabComponent != null) {
        tabbedPane.setTabComponentAt(tabIndex, tabBuilder.tabComponent);
      }
    });
    changeListeners.forEach(tabbedPane::addChangeListener);

    return tabbedPane;
  }

  @Override
  protected ComponentValue<Void, JTabbedPane> createComponentValue(JTabbedPane component) {
    throw new UnsupportedOperationException("A ComponentValue can not be based on a JTabbedPane");
  }

  @Override
  protected void setInitialValue(JTabbedPane component, Void initialValue) {}

  private static final class DefaultTabBuilder implements TabBuilder {

    private final DefaultTabbedPaneBuilder tabbedPaneBuilder;
    private final JComponent component;
    private final String title;

    private int mnemonic;
    private String toolTipText;
    private Icon icon;
    private JComponent tabComponent;

    private DefaultTabBuilder(DefaultTabbedPaneBuilder tabbedPaneBuilder, String title, JComponent component) {
      this.tabbedPaneBuilder = tabbedPaneBuilder;
      this.title = title;
      this.component = requireNonNull(component);
    }

    @Override
    public TabBuilder mnemonic(int mnemonic) {
      this.mnemonic = mnemonic;
      return this;
    }

    @Override
    public TabBuilder toolTipText(String toolTipText) {
      this.toolTipText = toolTipText;
      return this;
    }

    @Override
    public TabBuilder icon(Icon icon) {
      this.icon = icon;
      return this;
    }

    @Override
    public TabBuilder tabComponent(JComponent tabComponent) {
      this.tabComponent = tabComponent;
      return this;
    }

    @Override
    public TabbedPaneBuilder add() {
      tabbedPaneBuilder.tabBuilders.add(this);

      return tabbedPaneBuilder;
    }
  }
}
