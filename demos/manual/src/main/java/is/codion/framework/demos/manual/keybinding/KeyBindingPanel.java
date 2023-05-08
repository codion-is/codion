/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.manual.keybinding;

import is.codion.framework.demos.manual.keybinding.KeyBindingTableModel.KeyBinding;
import is.codion.swing.common.ui.Windows;
import is.codion.swing.common.ui.component.table.FilteredTable;
import is.codion.swing.common.ui.laf.LookAndFeelComboBox;

import com.formdev.flatlaf.intellijthemes.FlatAllIJThemes;

import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.util.Arrays;

import static is.codion.swing.common.ui.component.Components.*;
import static is.codion.swing.common.ui.component.text.TextComponents.preferredTextFieldHeight;
import static is.codion.swing.common.ui.laf.LookAndFeelComboBox.lookAndFeelComboBox;
import static is.codion.swing.common.ui.laf.LookAndFeelProvider.addLookAndFeelProvider;
import static is.codion.swing.common.ui.laf.LookAndFeelProvider.lookAndFeelProvider;
import static is.codion.swing.common.ui.layout.Layouts.borderLayout;
import static is.codion.swing.common.ui.layout.Layouts.flexibleGridLayout;
import static javax.swing.BorderFactory.createEmptyBorder;

/**
 * A utility for displaying component action/input maps for installed look and feels.<br>
 * Based on <a href="https://tips4java.wordpress.com/2008/10/10/key-bindings/">KeyBindings.java by Rob Comick</a>
 * @author Rob Camick
 * @author bjorndarri
 */
public final class KeyBindingPanel extends JPanel {

  private final LookAndFeelComboBox lookAndFeelComboBox = lookAndFeelComboBox(true);
  private final KeyBindingTableModel tableModel = new KeyBindingTableModel(lookAndFeelComboBox.getModel());
  private final FilteredTable<KeyBindingTableModel, KeyBinding, Integer> table = FilteredTable.builder(tableModel).build();
  private final JComboBox<String> componentComboBox = comboBox(tableModel.componentComboBoxModel())
          .preferredHeight(preferredTextFieldHeight())
          .preferredWidth(200)
          .build();

  public KeyBindingPanel() {
    super(borderLayout());
    setBorder(createEmptyBorder(10, 10, 10, 10));
    add(panel(flexibleGridLayout(1, 4))
            .add(label("Look & Feel")
                    .horizontalAlignment(SwingConstants.RIGHT)
                    .preferredWidth(100)
                    .build())
            .add(lookAndFeelComboBox)
            .add(label("Component")
                    .horizontalAlignment(SwingConstants.RIGHT)
                    .preferredWidth(100)
                    .build())
            .add(componentComboBox)
            .build(), BorderLayout.NORTH);
    add(new JScrollPane(table), BorderLayout.CENTER);
    add(table.searchField(), BorderLayout.SOUTH);
  }

  public static void main(String[] args) {
    System.setProperty("sun.awt.disablegrab", "true");
    Arrays.stream(FlatAllIJThemes.INFOS).forEach(themeInfo ->
            addLookAndFeelProvider(lookAndFeelProvider(themeInfo.getClassName())));
    SwingUtilities.invokeLater(() -> Windows.frame(new KeyBindingPanel())
            .title("Key Bindings")
            .defaultCloseOperation(JFrame.EXIT_ON_CLOSE)
            .centerFrame(true)
            .show());
  }
}
