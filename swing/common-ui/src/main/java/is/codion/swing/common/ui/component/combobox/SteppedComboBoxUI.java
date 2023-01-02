/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.combobox;

import javax.swing.JComboBox;
import javax.swing.plaf.basic.BasicComboPopup;
import javax.swing.plaf.basic.ComboPopup;
import javax.swing.plaf.metal.MetalComboBoxUI;
import java.awt.Dimension;
import java.awt.Rectangle;

import static java.util.Objects.requireNonNull;

/**
 * A JComboBox UI which automatically sets the popup width according to the largest value in the combo box.
 * Slightly modified, automatic popup size according to getDisplaySize().
 * @author Nobuo Tamemasa (originally)
 */
final class SteppedComboBoxUI extends MetalComboBoxUI {

  private int popupWidth = 0;

  SteppedComboBoxUI(JComboBox<?> comboBox, int popupWidth) {
    requireNonNull(comboBox).setUI(this);
    this.popupWidth = popupWidth;
  }

  @Override
  protected ComboPopup createPopup() {
    return new SteppedComboBoxPopup(comboBox, this);
  }

  private static final class SteppedComboBoxPopup extends BasicComboPopup {

    private final SteppedComboBoxUI comboBoxUI;

    private SteppedComboBoxPopup(JComboBox comboBox, SteppedComboBoxUI comboBoxUI) {
      super(comboBox);
      this.comboBoxUI = comboBoxUI;
      getAccessibleContext().setAccessibleParent(comboBox);
    }

    @Override
    public void setVisible(boolean visible) {
      if (visible) {
        Dimension popupSize = popupSize(comboBox);
        popupSize.setSize(popupSize.width, getPopupHeightForRowCount(comboBox.getMaximumRowCount()));
        Rectangle popupBounds = computePopupBounds(0, comboBox.getBounds().height, popupSize.width, popupSize.height);
        scroller.setMaximumSize(popupBounds.getSize());
        scroller.setPreferredSize(popupBounds.getSize());
        scroller.setMinimumSize(popupBounds.getSize());
        getList().invalidate();
        int selectedIndex = comboBox.getSelectedIndex();
        if (selectedIndex == -1) {
          getList().clearSelection();
        }
        else {
          getList().setSelectedIndex(selectedIndex);
        }
        getList().ensureIndexIsVisible(getList().getSelectedIndex());
        setLightWeightPopupEnabled(comboBox.isLightWeightPopupEnabled());
      }

      super.setVisible(visible);
    }

    private Dimension popupSize(JComboBox<?> comboBox) {
      Dimension displaySize = comboBoxUI.getDisplaySize();
      Dimension size = comboBox.getSize();

      return new Dimension(Math.max(size.width, comboBoxUI.popupWidth <= 0 ? displaySize.width :
              comboBoxUI.popupWidth), size.height);
    }
  }
}
