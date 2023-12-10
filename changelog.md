Codion Change Log
==================

## 0.17.20-SNAPSHOT
### is.codion.common.model
- ColumnConditionModel.accept() no longer returns true in case the condition model is disabled.
- ColumnConditionModel.accept() bug fixed, case insensitivity only worked if a wildcard was present, related refactoring.
### is.codion.swing.framework.ui
- EntityTablePanel, table status message now indicates whether the result is limited.

## 0.17.19
### is.codion.common.db
- AbstractConnectionPoolWrapper, prevent destroyed connection counter being called twice for each closed connection, see https://github.com/brettwooldridge/HikariCP/issues/2152.
### is.codion.framework.domain
- Condition.customCondition() renamed custom().
- BlobColumnDefinition removed, ColumnDefinition.lazy() added. Default behaviour for byte array columns being lazy and hidden removed. Related changes.
- DefaultEntityBuilder, defensive copying of value sets added.
- Entity.loaded(ForeignKey) removed, only used in tests.
### is.codion.framework.db
- EntityConnection.readBlob() and writeBlob() removed.
### is.codion.framework.model
- EntityTableModel.attributes() added, specifying the attributes to include when selecting. Implemented in SwingEntityTableModel, replacing overridable attributes() method. Related refactoring.
### is.codion.swing.framework.model
- SwingEntityTableModel.getColor() renamed toColor().
### is.codion.swing.framework.ui
- EntityPanel.createEditControlPanel() renamed createEditControls().

## 0.17.18
- Text.padString() deprecated for removal along with Text.ALIGNMENT, Text.leftPad() and rightPad() added.
- EntityTable.layoutPanel(), tablePanel parameter replaced with tableComponent, no longer restricted to JPanel.
- Jasper Reports and Apache Commons Logging upgraded.
- EntityServerConfiguration, connectionPoolProvider renamed connectionPoolFactory
- ConnectionPoolFactory.createConnectionPoolWrapper() renamed createConnectionPool()
- WaitCursor replaced with Cursors utility class, wait cursor usage reduced and simplified throughout.
- EntityEditPanel.beforeInsert() and beforeUpdate() no longer throw ValidationException, now called outside of try/catch block along with beforeDelete(). EntityTablePanel.beforeDelete() moved outside of try/catch block, delete() added for deleting without confirmation.
- Text.padString() removed along with Text.ALIGNMENT.
- EntityEditPanel.controls() added, public, can be used instead of the protected createControls().