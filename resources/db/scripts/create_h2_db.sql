RUNSCRIPT FROM 'demos/src/main/sql/empdept/ddl.sql';
RUNSCRIPT FROM 'demos/src/main/sql/empdept/dml.sql';
RUNSCRIPT FROM 'demos/src/main/sql/petstore/ddl.sql';
RUNSCRIPT FROM 'demos/src/main/sql/petstore/dml.sql';
RUNSCRIPT FROM 'demos/src/main/sql/chinook/ddl.sql';
RUNSCRIPT FROM 'demos/src/main/sql/chinook/dml.sql';
RUNSCRIPT FROM 'demos/src/main/sql/world/ddl.sql';
RUNSCRIPT FROM 'demos/src/main/sql/world/dml.sql';
RUNSCRIPT FROM 'demos/src/main/sql/world/ddl_fk.sql';
create user scott password 'tiger';
alter user scott admin true;