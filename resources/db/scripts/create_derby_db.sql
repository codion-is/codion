RUN './resources/demos/empdept/scripts/ddl.sql';
RUN './resources/demos/empdept/scripts/dml.sql';
RUN './resources/demos/petstore/scripts/ddl.sql';
RUN './resources/demos/petstore/scripts/dml.sql';
CALL SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY('derby.user.scott', 'tiger');
CALL SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY('derby.connection.requireAuthentication', 'true');
EXIT;