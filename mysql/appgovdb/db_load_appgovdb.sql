USE appgovdb;

INSERT IGNORE INTO projects
(id, name, admin)
VALUES
(1, "project1","jhunt"),
(2, "project2", "bob"),
(3, "project3", "doug")
;

INSERT IGNORE INTO accessrequests 
(id, project_id, datetime, vault_name, safe_name, requestor, cpm_name, lob_name)
VALUES
(1, 1, NOW(), "DemoVault", "project1", "bill", "PasswordManager", "CICD"),
(2, 2, NOW(), "DemoVault", "project2", "dave", "PasswordManager", "CICD")
;

INSERT IGNORE INTO appidentities
(id, accreq_id, name, auth_method)
VALUES
(1, 1, "app-example-sidecar-user1", "k8s"),
(2, 2, "app-example-provider-user1", "k8s"),
(3, 1, "app-example-secretless-user1", "k8s")
;

INSERT IGNORE INTO cybraccounts
(id, accreq_id, name, platform_id, system_type, secret_type, username, address, port, db_name)
VALUES
(1, 1, "MySQL-Dev1", "MySQL", "Database", "password", "devapp1", "conjur-master-mac", "3306", "petclinic"),
(2, 1, "MySQL-Dev2", "MySQL", "Database", "password", "devapp2", "conjur-master-mac", "3306", "petclinic2"),
(3, 2, "MySQL-Dev3", "MySQL", "Database", "password", "devapp3", "conjur-master-mac", "3306", "petclinic3")
;
