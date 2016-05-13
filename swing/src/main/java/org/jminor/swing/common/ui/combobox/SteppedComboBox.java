package org.jminor.swing.common.ui.combobox;

import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JScrollBar;
import javax.swing.plaf.basic.BasicComboPopup;
import javax.swing.plaf.basic.ComboPopup;
import javax.swing.plaf.metal.MetalComboBoxUI;
import java.awt.Dimension;
import java.awt.Rectangle;

/**
 * A JComboBox which automatically sets the popup width according to the largest value in the combo box.
 * Slightly modified, automatic popup size according to getDisplaySize().
 * @param V the type of values contained in this combo box
 * @author Nobuo Tamemasa
 */
public class SteppedComboBox<V> extends JComboBox<V> {

  private int popupWidth = 0;

  /**
   * Instantiates a new SteppedComboBox.
   * @param boxModel the combo box model
   */
  public SteppedComboBox(final ComboBoxModel<V> boxModel) {
    super(boxModel);
    initUI();
  }

  /**
   * @param width the width of the popup
   */
  public final void setPopupWidth(final int width) {
    popupWidth = width;
  }

  /**
   * @return Value for property 'popupSize'.
   */
  public final Dimension getPopupSize() {
    final Dimension displaySize = ((SteppedComboBoxUI) getUI()).getDisplaySize();
    final Dimension size = getSize();

    return new Dimension(Math.max(size.width, popupWidth <= 0 ? displaySize.width : popupWidth), size.height);
  }

  private void initUI() {
    setUI(new SteppedComboBoxUI());
  }

  private static final class SteppedComboBoxUI extends MetalComboBoxUI {
    @Override
    protected ComboPopup createPopup() {
      return new SteppedComboBoxPopup(comboBox);
    }

    @Override
    public Dimension getDisplaySize() {
      return super.getDisplaySize();
    }
  }

  private static final class SteppedComboBoxPopup extends BasicComboPopup {

    private SteppedComboBoxPopup(final JComboBox comboBox) {
      super(comboBox);
      getAccessibleContext().setAccessibleParent(comboBox);
    }

    @Override
    public void setVisible(final boolean visible) {
      if (visible) {
        final Dimension popupSize = ((SteppedComboBox) comboBox).getPopupSize();
        popupSize.setSize(popupSize.width, getPopupHeightForRowCount(comboBox.getMaximumRowCount()));
        final Rectangle popupBounds = computePopupBounds(0, comboBox.getBounds().height,
                popupSize.width + new JScrollBar(JScrollBar.VERTICAL).getWidth(), popupSize.height);
        scroller.setMaximumSize(popupBounds.getSize());
        scroller.setPreferredSize(popupBounds.getSize());
        scroller.setMinimumSize(popupBounds.getSize());
        getList().invalidate();
        final int selectedIndex = comboBox.getSelectedIndex();
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
  }
}