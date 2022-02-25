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
import is.codion.swing.common.ui.combobox.SteppedComboBox;

import javax.swing.ComboBoxEditor;
import javax.swing.ComboBoxModel;
import javax.swing.JComponent;
import javax.swing.ListCellRenderer;
import javax.swing.text.JTextComponent;
import java.awt.Component;

import static is.codion.swing.common.ui.textfield.TextComponents.getPreferredTextFieldHeight;
import static java.util.Objects.requireNonNull;

public class DefaultComboBoxBuilder<T, C extends SteppedComboBox<T>, B extends ComboBoxBuilder<T, C, B>> extends AbstractComponentBuilder<T, C, B>
        implements ComboBoxBuilder<T, C, B> {

  protected final ComboBoxModel<T> comboBoxModel;

  private boolean editable = false;
  private Completion.Mode completionMode = Completion.COMBO_BOX_COMPLETION_MODE.get();
  private ListCellRenderer<T> renderer;
  private ComboBoxEditor editor;
  private int popupWidth;
  private boolean mouseWheelScrolling = true;
  private boolean mouseWheelScrollingWithWrapAround = false;
  private int maximumRowCount = -1;
  private boolean moveCaretOnSelection = true;

  protected DefaultComboBoxBuilder(ComboBoxModel<T> comboBoxModel, Value<T> linkedValue) {
    super(linkedValue);
    this.comboBoxModel = requireNonNull(comboBoxModel);
    preferredHeight(getPreferredTextFieldHeight());
  }

  @Override
  public final B popupWidth(int popupWidth) {
    this.popupWidth = popupWidth;
    return (B) this;
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
    if (popupWidth > 0) {
      comboBox.setPopupWidth(popupWidth);
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

    return comboBox;
  }

  @Override
  protected final ComponentValue<T, C> buildComponentValue(C component) {
    return ComponentValues.comboBox(component);
  }

  @Override
  protected final void setTransferFocusOnEnter(C component) {
    component.setTransferFocusOnEnter(true);
    TransferFocusOnEnter.enable((JComponent) component.getEditor().getEditorComponent());
  }

  @Override
  protected final void setInitialValue(C component, T initialValue) {
    component.setSelectedItem(initialValue);
  }

  protected C createComboBox() {
    return (C) new SteppedComboBox<>(comboBoxModel);
  }

  private static final class MoveCaretListener<T> implements EventDataListener<T> {

    private final SteppedComboBox<?> comboBox;

    private MoveCaretListener(SteppedComboBox<T> comboBox) {
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
}
