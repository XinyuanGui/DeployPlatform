DROP DATABASE IF EXISTS deploy_system2_db;
CREATE DATABASE IF NOT EXISTS deploy_system2_db;
USE deploy_system2_db;

DROP TABLE IF EXISTS single_file_code;
CREATE TABLE IF NOT EXISTS single_file_code (
  id INT AUTO_INCREMENT,
  TYPE VARCHAR (10),
  NAME VARCHAR (100),
  DESCRIPTION VARCHAR (200),
  CODE TEXT,
  PRIMARY KEY (id)
);