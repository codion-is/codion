create schema scott;

CREATE TABLE scott.dept (
  deptno INT NOT NULL,
  dname VARCHAR(14) NOT NULL,
  loc VARCHAR(13),
  constraint dept_pk primary key (deptno),
  constraint dept_uk unique (dname)
);

CREATE TABLE scott.emp (
  empno INT NOT NULL,
  ename VARCHAR(10) NOT NULL,
  job VARCHAR(9),
  mgr INT,
  hiredate DATE,
  hiretime TIMESTAMP,
  sal DECIMAL(7, 2) NOT NULL,
  comm DECIMAL(7, 2) DEFAULT 200,
  deptno INT NOT NULL,
  data blob,
  constraint emp_pk primary key (empno),
  constraint emp_dept_fk foreign key (deptno) references scott.dept(deptno),
  constraint emp_mgr_fk foreign key (mgr) references scott.emp(empno)
);

INSERT INTO scott.dept(deptno, dname, loc)
VALUES (10, 'ACCOUNTING', 'NEW YORK'),
  (20, 'RESEARCH', 'DALLAS'),
  (30, 'SALES', 'CHICAGO'),
  (40, 'OPERATIONS', 'BOSTON');

INSERT INTO scott.emp(empno, ename, job, mgr, hiredate, sal, comm, deptno)
VALUES (8, 'KING', 'PRESIDENT', NULL, '1981-11-17', 5000, NULL, 10),
  (3, 'JONES', 'MANAGER', 8, '1981-04-02', 2975, NULL, 20),
  (5, 'BLAKE', 'MANAGER', 3, '1981-05-01', 2850, NULL, 10),
  (1, 'ALLEN', 'SALESMAN', 5, '1981-02-20', 1600, 300, 30),
  (2, 'WARD', 'SALESMAN', 8, '1981-02-22', 1250, 500, 30),
  (4, 'MARTIN', 'SALESMAN', 3, '1981-09-28', 1250, 1400, 30),
  (6, 'CLARK', 'MANAGER', 8, '1981-06-09', 2450, 1500, 10),
  (7, 'SCOTT', 'ANALYST', 3, '1987-04-19', 3000, 1500, 20),
  (9, 'TURNER', 'SALESMAN', 5, '1981-09-08', 1500, 0, 30),
  (10, 'ADAMS', 'CLERK', 3, '1988-05-15', 1100, NULL, 20),
  (11, 'JAMES', 'CLERK', 5, '1996-10-03', 950, 1500, 10),
  (12, 'FORD', 'CLERK', 3, '1988-12-12', 3200, 1200, 20),
  (13, 'MILLER', 'CLERK', 6, '1983-01-23', 1300, 1200, 10),
  (14, 'SCOTT', 'CLERK', 3, '1989-12-12', 1600, 1200, 20),
  (15, 'BAKER', 'CLERK', 5, '2007-01-01', 1000, NULL, 10),
  (16, 'TREVOR', 'CLERK', 5, '2007-01-01', 1000, NULL, 10);

commit;

create user scott password 'tiger';
alter user scott admin true;