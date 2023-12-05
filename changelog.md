Codion Change Log
==================

## 0.17.19-SNAPSHOT
### is.codion.common.db
- AbstractConnectionPoolWrapper, prevent destroyed connection counter being called twice for each closed connection, see https://github.com/brettwooldridge/HikariCP/issues/2152.
### is.codion.framework.domain
- Condition.customCondition() renamed custom().
- BlobColumnDefinition removed, ColumnDefinition.lazy() added. Default behaviour for byte array columns being lazy and hidden removed. Related changes.
### is.codion.framework.db
- EntityConnection.readBlob() and writeBlob() removed.

## 0.17.18
- Text.padString() deprecated for removal along with Text.ALIGNMENT, Text.leftPad() and rightPad() added.
- EntityTable.layoutPanel(), tablePanel parameter replaced with tableComponent, no longer restricted to JPanel.
- Jasper Reports and Apache Commons Logging upgraded.
- Logback and Spotless upgraded.
- EntityServerConfiguration, connectionPoolProvider renamed connectionPoolFactory
- ConnectionPoolFactory.createConnectionPoolWrapper() renamed createConnectionPool()
- WaitCursor replaced with Cursors utility class, wait cursor usage reduced and simplified throughout.
- EntityEditPanel.beforeInsert() and beforeUpdate() no longer throw ValidationException, now called outside of try/catch block along with beforeDelete(). EntityTablePanel.beforeDelete() moved outside of try/catch block, delete() added for deleting without confirmation.
- Commons Logging upgrade to 1.3.0 caused NoClassDefFoundError: org/apache/commons/logging/LogFactory in modular demo applications, reverted back to 1.2.
- Text.padString() removed along with Text.ALIGNMENT.
- EntityEditPanel.controls() added, public, can be used instead of the protected createControls().