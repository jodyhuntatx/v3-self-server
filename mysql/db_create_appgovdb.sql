DROP DATABASE appgovdb;
CREATE DATABASE appgovdb;

USE appgovdb;

# Project table just has project name & admin name
CREATE TABLE projects (
  id INT(4) UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(30) NOT NULL,
  UNIQUE INDEX(name),
  admin VARCHAR(30)
) engine=InnoDB;

# Access Requests are per Project/Vault/Safe and unique
CREATE TABLE accessrequests (
  id INT(4) UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  project_id INT(4) UNSIGNED NOT NULL,
  FOREIGN KEY (project_id) REFERENCES projects(id),
  datetime DATETIME NOT NULL,
  approved BOOLEAN DEFAULT FALSE,
  provisioned BOOLEAN DEFAULT FALSE,
  vault_name VARCHAR(30) NOT NULL,
  safe_name VARCHAR(30) NOT NULL,
  lob_name VARCHAR(30),
  requestor VARCHAR(30),
  cpm_name VARCHAR(30),
  INDEX(safe_name, lob_name)
) engine=InnoDB;

# Application Identity table
CREATE TABLE appidentities (
  id INT(4) UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  project_id INT(4) UNSIGNED NOT NULL,
  FOREIGN KEY (project_id) REFERENCES projects(id),
  accreq_id INT(4) UNSIGNED NOT NULL,
  FOREIGN KEY (accreq_id) REFERENCES accessrequests(id),
  name VARCHAR(30) NOT NULL,
  auth_method VARCHAR(30),
  UNIQUE INDEX(project_id, name)
) engine=InnoDB;

# CyberArk Account table
CREATE TABLE cybraccounts (
  id INT(4) UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  project_id INT(4) UNSIGNED NOT NULL,
  FOREIGN KEY (project_id) REFERENCES projects(id),
  name VARCHAR(30) NOT NULL,
  platform_id VARCHAR(30),
  system_type VARCHAR(30),
  secret_type VARCHAR(30),
  username VARCHAR(30),
  address VARCHAR(30),
  port VARCHAR(30),
  db_name VARCHAR(30),
  UNIQUE INDEX(project_id, name)
) engine=InnoDB;
