/*
 * Copyright (c) 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

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
  private final List<DefaultTabBuilder> tabBuilders = new ArrayList<>();
  private final List<ChangeListener> changeListeners = new ArrayList<>();

  @Override
  public TabbedPaneBuilder tabPlacement(int tabPlacement) {
    this.tabPlacement = tabPlacement;
    return this;
  }

  @Override
  public TabBuilder tab(JComponent component) {
    return new DefaultTabBuilder(this, null, component);
  }

  @Override
  public TabBuilder tab(String title, JComponent component) {
    return new DefaultTabBuilder(this, requireNonNull(title), component);
  }

  @Override
  public TabbedPaneBuilder changeListener(ChangeListener changeListener) {
    changeListeners.add(requireNonNull(changeListener));
    return this;
  }

  @Override
  protected JTabbedPane createComponent() {
    JTabbedPane tabbedPane = new JTabbedPane(tabPlacement);
    tabBuilders.forEach(tabBuilder -> {
      int index = tabbedPane.getTabCount();
      tabbedPane.addTab(tabBuilder.title, tabBuilder.icon, tabBuilder.component, tabBuilder.toolTipText);
      if (tabBuilder.mnemonic != 0) {
        tabbedPane.setMnemonicAt(index, tabBuilder.mnemonic);
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
    public TabbedPaneBuilder add() {
      tabbedPaneBuilder.tabBuilders.add(this);

      return tabbedPaneBuilder;
    }
  }
}
