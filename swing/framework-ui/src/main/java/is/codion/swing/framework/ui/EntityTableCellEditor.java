/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui;

import is.codion.common.value.Value;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.swing.common.ui.value.ComponentValue;
import is.codion.swing.framework.ui.component.EntityInputComponents;

import javax.swing.AbstractCellEditor;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.TableCellEditor;
import java.awt.Component;
import java.awt.event.MouseEvent;
import java.util.EventObject;

import static java.util.Objects.requireNonNull;

/**
 * A {@link TableCellEditor} implementation for {@link EntityTablePanel}.
 */
class EntityTableCellEditor<T> extends AbstractCellEditor implements TableCellEditor {

  private final EntityInputComponents inputComponents;
  private final Attribute<T> attribute;
  private final Value<T> cellValue = Value.value();

  private JComponent component;

  EntityTableCellEditor(final EntityDefinition entityDefinition, final Attribute<T> attribute) {
    this.inputComponents = new EntityInputComponents(entityDefinition);
    this.attribute = requireNonNull(attribute, "attribute");
  }

  @Override
  public final Component getTableCellEditorComponent(final JTable table, final Object value, final boolean isSelected,
                                                     final int row, final int column) {
    if (component == null) {
      component = initializeEditorComponent();
    }
    cellValue.set((T) value);

    return component;
  }

  @Override
  public final Object getCellEditorValue() {
    return cellValue.get();
  }

  @Override
  public final boolean isCellEditable(final EventObject event) {
    if (event instanceof MouseEvent) {
      return ((MouseEvent) event).getClickCount() >= 2;
    }

    return false;
  }

  protected final EntityInputComponents getInputComponents() {
    return inputComponents;
  }

  protected final Attribute<T> getAttribute() {
    return attribute;
  }

  protected final Value<T> getCellValue() {
    return cellValue;
  }

  protected JComponent initializeEditorComponent() {
    final ComponentValue<T, JComponent> componentValue = inputComponents.createInputComponent(attribute);
    cellValue.link(componentValue);
    final JComponent editorComponent = componentValue.getComponent();
    if (editorComponent instanceof JCheckBox) {
      ((JCheckBox) editorComponent).setHorizontalAlignment(SwingConstants.CENTER);
    }

    return editorComponent;
  }
}
