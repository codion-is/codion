Codion Change Log
==================

## 0.17.18-SNAPSHOT
- Text.padString() deprecated for removal along with Text.ALIGNMENT, Text.leftPad() and rightPad() added.
- EntityTable.layoutPanel(), tablePanel parameter replaced with tableComponent, no longer restricted to JPanel.
- Jasper Reports and Apache Commons Logging upgraded.
- EntityServerConfiguration, connectionPoolProvider renamed connectionPoolFactory
- ConnectionPoolFactory.createConnectionPoolWrapper() renamed createConnectionPool()
- WaitCursor replaced with Cursors utility class, wait cursor usage reduced and simplified throughout.
- EntityEditPanel.beforeInsert() and beforeUpdate() no longer throw ValidationException, now called outside of try/catch block along with beforeDelete(). EntityTablePanel.beforeDelete() moved outside of try/catch block, delete() added for deleting without confirmation.
- Text.padString() removed along with Text.ALIGNMENT.
- EntityEditPanel.controls() added, public, can be used instead of the protected createControls().