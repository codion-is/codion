-- ----------------------------------------------------------------------
-- MySQL Migration Toolkit
-- SQL Create Script
-- ----------------------------------------------------------------------

SET FOREIGN_KEY_CHECKS = 0;

CREATE DATABASE IF NOT EXISTS `scott`
  CHARACTER SET latin1 COLLATE latin1_swedish_ci;
USE `scott`;
-- -------------------------------------
-- Tables

DROP TABLE IF EXISTS `scott`.`dept`;
CREATE TABLE `scott`.`dept` (
  `deptno` INT(2) NOT NULL,
  `dname` VARCHAR(14) BINARY NULL,
  `loc` VARCHAR(13) BINARY NULL,
  PRIMARY KEY (`deptno`)
)
ENGINE = INNODB;

DROP TABLE IF EXISTS `scott`.`emp`;
CREATE TABLE `scott`.`emp` (
  `empno` INT(4) NOT NULL,
  `ename` VARCHAR(10) BINARY NULL,
  `job` VARCHAR(9) BINARY NULL,
  `mgr` INT(4) NULL,
  `hiredate` DATETIME NULL,
  `sal` DECIMAL(7, 2) NULL,
  `comm` DECIMAL(7, 2) NULL,
  `deptno` INT(2) NULL,
  PRIMARY KEY (`empno`),
  CONSTRAINT `fk_deptno` FOREIGN KEY `fk_deptno` (`deptno`)
    REFERENCES `scott`.`dept` (`deptno`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION
)
ENGINE = INNODB;



SET FOREIGN_KEY_CHECKS = 1;

-- ----------------------------------------------------------------------
-- EOF

