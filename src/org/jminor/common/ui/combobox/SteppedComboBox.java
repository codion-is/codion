package org.jminor.common.ui.combobox;

import org.jminor.common.ui.UiUtil;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.plaf.basic.BasicComboPopup;
import javax.swing.plaf.basic.ComboPopup;
import javax.swing.plaf.metal.MetalComboBoxUI;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.Collection;

/**
 * A JComboBox which automatically sets the popup width according to the largest value in the combo box.
 * Slightly modified, automatic popup size according to getDisplaySize().
 * @author Nobuo Tamemasa
 */
public class SteppedComboBox extends JComboBox {

  private final boolean hidePopupOnFocusLoss;
  private int popupWidth = 0;

  public SteppedComboBox(final Collection items) {
    this(items.toArray());
  }

  public SteppedComboBox(final Object[] items) {
    this(new DefaultComboBoxModel(items));
  }

  public SteppedComboBox(final ComboBoxModel boxModel) {
    super(boxModel);
    initUI();
    // Bug 5100422 on Java 1.5: Editable JComboBox won't hide popup when tabbing out
    hidePopupOnFocusLoss = System.getProperty("java.version").startsWith("1.5");
    bindEvents();
  }

  /**
   * @param width the width of the popup
   */
  public void setPopupWidth(int width) {
    popupWidth = width;
  }

  /**
   * @param displaySize the display size provided by the UI
   * @return Value for property 'popupSize'.
   */
  public Dimension getPopupSize(final Dimension displaySize) {
    final Dimension size = getSize();

    return new Dimension(Math.max(size.width, popupWidth <= 0 ? displaySize.width : popupWidth), size.height);
  }

  private void bindEvents() {
    getEditor().getEditorComponent().addFocusListener(new FocusAdapter() {
      @Override
      public void focusLost(FocusEvent e) {
        // Workaround for Bug 5100422 - Hide Popup on focus loss
        if (hidePopupOnFocusLoss) {
          setPopupVisible(false);
        }
      }
    });
  }

  private void initUI() {
    setUI(new MetalComboBoxUI() {
      @Override
      protected ComboPopup createPopup() {
        final BasicComboPopup basicPopup = new BasicComboPopup(comboBox) {
          @Override
          public void show() {
            final Dimension popupSize = ((SteppedComboBox)comboBox).getPopupSize(getDisplaySize());
            popupSize.setSize(popupSize.width, getPopupHeightForRowCount(comboBox.getMaximumRowCount()));
            final Rectangle popupBounds = computePopupBounds(0, comboBox.getBounds().height,
                    popupSize.width + UiUtil.getPreferredScrollBarWidth(), popupSize.height);
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

            show(comboBox, popupBounds.x, popupBounds.y);
          }
        };
        basicPopup.getAccessibleContext().setAccessibleParent(comboBox);

        return basicPopup;
      }
    });
  }
}