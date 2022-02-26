package is.codion.swing.common.ui.combobox;

import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JScrollBar;
import javax.swing.SwingConstants;
import javax.swing.plaf.basic.BasicComboPopup;
import javax.swing.plaf.basic.ComboPopup;
import javax.swing.plaf.metal.MetalComboBoxUI;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.util.Objects;

/**
 * A JComboBox which automatically sets the popup width according to the largest value in the combo box.
 * Slightly modified, automatic popup size according to getDisplaySize().
 * @param <T> the type of values contained in this combo box
 * @author Nobuo Tamemasa
 */
public class SteppedComboBox<T> extends JComboBox<T> {

  private int popupWidth = 0;

  /**
   * Instantiates a new SteppedComboBox.
   * @param comboBoxModel the combo box model
   */
  public SteppedComboBox(ComboBoxModel<T> comboBoxModel) {
    super(Objects.requireNonNull(comboBoxModel, "comboBoxModel"));
    setUI(new SteppedComboBoxUI());
  }

  /**
   * @param width the width of the popup
   */
  public final void setPopupWidth(int width) {
    popupWidth = width;
  }

  /**
   * @return the popup size
   */
  public final Dimension getPopupSize() {
    Dimension displaySize = ((SteppedComboBoxUI) getUI()).getDisplaySize();
    Dimension size = getSize();

    return new Dimension(Math.max(size.width, popupWidth <= 0 ? displaySize.width : popupWidth), size.height);
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

    private SteppedComboBoxPopup(JComboBox comboBox) {
      super(comboBox);
      getAccessibleContext().setAccessibleParent(comboBox);
    }

    @Override
    public void setVisible(boolean visible) {
      if (visible) {
        Dimension popupSize = ((SteppedComboBox<?>) comboBox).getPopupSize();
        popupSize.setSize(popupSize.width, getPopupHeightForRowCount(comboBox.getMaximumRowCount()));
        Rectangle popupBounds = computePopupBounds(0, comboBox.getBounds().height,
                popupSize.width + new JScrollBar(SwingConstants.VERTICAL).getWidth(), popupSize.height);
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
  }
}