DROP DATABASE IF EXISTS appgovdb;
CREATE DATABASE appgovdb;

USE appgovdb;

# Projects table
CREATE TABLE projects (
  id INT(4) UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(30) NOT NULL,
  admin_user VARCHAR(30),
  billing_code VARCHAR(30),
  UNIQUE INDEX(name,admin_user)
) engine=InnoDB;

# Application Identity table
CREATE TABLE appidentities (
  id INT(4) UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  project_id INT(4) UNSIGNED NOT NULL,
  FOREIGN KEY (project_id) REFERENCES projects(id),
  name VARCHAR(30) NOT NULL,
  authn_method VARCHAR(30),
  authn_attribute1_key VARCHAR(50),
  authn_attribute1_value VARCHAR(50),
  authn_attribute2_key VARCHAR(50),
  authn_attribute2_value VARCHAR(50),
  authn_attribute3_key VARCHAR(50),
  authn_attribute3_value VARCHAR(50),
  UNIQUE INDEX(project_id, name)
) engine=InnoDB;

# Safes table
CREATE TABLE safes (
  id INT(4) UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(30) NOT NULL,
  vault_name VARCHAR(30) NOT NULL,
  cpm_name VARCHAR(30),
  UNIQUE INDEX(name,vault_name)
) engine=InnoDB;

# CyberArk Accounts table
CREATE TABLE cybraccounts (
  id INT(4) UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  safe_id INT(4) UNSIGNED NOT NULL,
  FOREIGN KEY (safe_id) REFERENCES safes(id),
  name VARCHAR(30) NOT NULL,
  platform_id VARCHAR(30),
  secret_type VARCHAR(30),
  username VARCHAR(30),
  address VARCHAR(80),
  resource_type VARCHAR(30),
  resource_name VARCHAR(30),
  UNIQUE INDEX(safe_id, name)
) engine=InnoDB;

# Access Requests == "request read-acces to safe for project/app_id"
CREATE TABLE accessrequests (
  id INT(4) UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  project_id INT(4) UNSIGNED NOT NULL,
  FOREIGN KEY (project_id) REFERENCES projects(id),
  app_id INT(4) UNSIGNED NOT NULL,
  FOREIGN KEY (app_id) REFERENCES appidentities(id),
  safe_id INT(4) UNSIGNED NOT NULL,
  FOREIGN KEY (safe_id) REFERENCES safes(id),
  datetime DATETIME NOT NULL,
  approved BOOLEAN DEFAULT FALSE,
  rejected BOOLEAN DEFAULT FALSE,
  provisioned BOOLEAN DEFAULT FALSE,
  revoked BOOLEAN DEFAULT FALSE,
  environment VARCHAR(30) NOT NULL,
  lob_name VARCHAR(30) NOT NULL,
  requestor VARCHAR(30) NOT NULL
) engine=InnoDB;

