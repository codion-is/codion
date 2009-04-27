RUN './resources/demos/empdept/scripts/ddl_derby.sql';
RUN './resources/demos/empdept/scripts/dml_derby.sql';
RUN './resources/demos/petstore/scripts/ddl_derby.sql';
RUN './resources/demos/petstore/scripts/dml_derby.sql';
CALL SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY('derby.user.scott', 'tiger');
CALL SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY('derby.connection.requireAuthentication', 'true');
EXIT;