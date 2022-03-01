/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.common.event.EventDataListener;
import is.codion.common.model.combobox.FilteredComboBoxModel;
import is.codion.common.value.Value;
import is.codion.swing.common.ui.TransferFocusOnEnter;
import is.codion.swing.common.ui.combobox.ComboBoxMouseWheelListener;
import is.codion.swing.common.ui.combobox.Completion;
import is.codion.swing.common.ui.laf.LookAndFeelProvider;

import javax.swing.ComboBoxEditor;
import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.ListCellRenderer;
import javax.swing.plaf.basic.BasicComboPopup;
import javax.swing.plaf.basic.ComboPopup;
import javax.swing.plaf.metal.MetalComboBoxUI;
import javax.swing.text.JTextComponent;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Rectangle;

import static is.codion.swing.common.ui.textfield.TextComponents.getPreferredTextFieldHeight;
import static java.util.Objects.requireNonNull;

public class DefaultComboBoxBuilder<T, C extends JComboBox<T>, B extends ComboBoxBuilder<T, C, B>> extends AbstractComponentBuilder<T, C, B>
        implements ComboBoxBuilder<T, C, B> {

  protected final ComboBoxModel<T> comboBoxModel;

  private boolean editable = false;
  private Completion.Mode completionMode = Completion.COMBO_BOX_COMPLETION_MODE.get();
  private ListCellRenderer<T> renderer;
  private ComboBoxEditor editor;
  private boolean mouseWheelScrolling = true;
  private boolean mouseWheelScrollingWithWrapAround = false;
  private int maximumRowCount = -1;
  private boolean moveCaretOnSelection = true;
  private int popupWidth = 0;

  protected DefaultComboBoxBuilder(ComboBoxModel<T> comboBoxModel, Value<T> linkedValue) {
    super(linkedValue);
    this.comboBoxModel = requireNonNull(comboBoxModel);
    preferredHeight(getPreferredTextFieldHeight());
  }

  @Override
  public final B editable(boolean editable) {
    this.editable = editable;
    return (B) this;
  }

  @Override
  public final B completionMode(Completion.Mode completionMode) {
    this.completionMode = requireNonNull(completionMode);
    return (B) this;
  }

  @Override
  public final B renderer(ListCellRenderer<T> renderer) {
    this.renderer = requireNonNull(renderer);
    return (B) this;
  }

  @Override
  public final B editor(ComboBoxEditor editor) {
    this.editor = requireNonNull(editor);
    return (B) this;
  }

  @Override
  public final B mouseWheelScrolling(boolean mouseWheelScrolling) {
    this.mouseWheelScrolling = mouseWheelScrolling;
    if (mouseWheelScrolling) {
      this.mouseWheelScrollingWithWrapAround = false;
    }
    return (B) this;
  }

  @Override
  public final B mouseWheelScrollingWithWrapAround(boolean mouseWheelScrollingWithWrapAround) {
    this.mouseWheelScrollingWithWrapAround = mouseWheelScrollingWithWrapAround;
    if (mouseWheelScrollingWithWrapAround) {
      this.mouseWheelScrolling = false;
    }
    return (B) this;
  }

  @Override
  public final B maximumRowCount(int maximumRowCount) {
    this.maximumRowCount = maximumRowCount;
    return (B) this;
  }

  @Override
  public final B moveCaretOnSelection(boolean moveCaretOnSelection) {
    this.moveCaretOnSelection = moveCaretOnSelection;
    return (B) this;
  }

  @Override
  public final B popupWidth(int popupWidth) {
    this.popupWidth = popupWidth;
    return (B) this;
  }

  @Override
  protected final C buildComponent() {
    C comboBox = createComboBox();
    if (renderer != null) {
      comboBox.setRenderer(renderer);
    }
    if (editor != null) {
      comboBox.setEditor(editor);
    }
    if (editable) {
      comboBox.setEditable(true);
    }
    if (!editable && editor == null) {
      Completion.enable(comboBox, completionMode);
    }
    if (mouseWheelScrolling) {
      comboBox.addMouseWheelListener(ComboBoxMouseWheelListener.create(comboBoxModel));
    }
    if (mouseWheelScrollingWithWrapAround) {
      comboBox.addMouseWheelListener(ComboBoxMouseWheelListener.createWithWrapAround(comboBoxModel));
    }
    if (maximumRowCount >= 0) {
      comboBox.setMaximumRowCount(maximumRowCount);
    }
    if (comboBoxModel instanceof FilteredComboBoxModel && comboBox.isEditable() && moveCaretOnSelection) {
      ((FilteredComboBoxModel<T>) comboBoxModel).addSelectionListener(new MoveCaretListener<>(comboBox));
    }
    if (LookAndFeelProvider.isSystemLookAndFeelEnabled()) {
      new SteppedComboBoxUI(comboBox, popupWidth);
    }

    return comboBox;
  }

  @Override
  protected final ComponentValue<T, C> buildComponentValue(C component) {
    return ComponentValues.comboBox(component);
  }

  @Override
  protected final void setTransferFocusOnEnter(C component) {
    TransferFocusOnEnter.enable(component);
    TransferFocusOnEnter.enable((JComponent) component.getEditor().getEditorComponent());
  }

  @Override
  protected final void setInitialValue(C component, T initialValue) {
    component.setSelectedItem(initialValue);
  }

  protected C createComboBox() {
    return (C) new JComboBox<T>(comboBoxModel) {
      /**
       * Overridden as a workaround for editable combo boxes as initial focus components on
       * detail panels stealing the focus from the parent panel on initialization
       */
      @Override
      public void requestFocus() {
        if (isEditable()) {
          getEditor().getEditorComponent().requestFocus();
        }
        else {
          super.requestFocus();
        }
      }
    };
  }

  private static final class MoveCaretListener<T> implements EventDataListener<T> {

    private final JComboBox<?> comboBox;

    private MoveCaretListener(JComboBox<T> comboBox) {
      this.comboBox = comboBox;
    }

    @Override
    public void onEvent(Object selectedItem) {
      Component editorComponent = comboBox.getEditor().getEditorComponent();
      if (selectedItem != null && editorComponent instanceof JTextComponent) {
        ((JTextComponent) editorComponent).setCaretPosition(0);
      }
    }
  }

  /**
   * A JComboBox UI which automatically sets the popup width according to the largest value in the combo box.
   * Slightly modified, automatic popup size according to getDisplaySize().
   * @author Nobuo Tamemasa (originally)
   */
  static final class SteppedComboBoxUI extends MetalComboBoxUI {

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
          Dimension popupSize = getPopupSize(comboBox);
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

      private Dimension getPopupSize(JComboBox<?> comboBox) {
        Dimension displaySize = comboBoxUI.getDisplaySize();
        Dimension size = comboBox.getSize();

        return new Dimension(Math.max(size.width, comboBoxUI.popupWidth <= 0 ? displaySize.width : comboBoxUI.popupWidth), size.height);
      }
    }
  }
}
