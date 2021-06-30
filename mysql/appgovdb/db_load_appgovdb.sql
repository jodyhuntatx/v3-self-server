USE appgovdb;

INSERT IGNORE INTO projects
(id, name, admin)
VALUES
(1, "project1","jhunt"),
(2, "project2", "bob"),
(3, "project3", "doug")
;

INSERT IGNORE INTO accessrequests 
(id, project_id, datetime, approved, provisioned, vault_name, safe_name, requestor, cpm_name, lob_name)
VALUES
(1, 1, NOW(), 1, 1, "DemoVault", "project1", "bill", "PasswordManager", "CICD"),
(2, 2, NOW(), 1, 1, "DemoVault", "project2", "dave", "PasswordManager", "CICD")
;

INSERT INTO appidentities
(id, project_id, accreq_id, name, auth_method)
VALUES
(1, 1, 1, "app-example-sidecar", "k8s"),
(2, 2, 2, "app-example-provider", "k8s"),
(3, 1, 1, "app-example-secretless", "k8s")
;

INSERT IGNORE INTO cybraccounts
(id, project_id, name, username, db_name)
VALUES
(1, 1, 'PetClinicDev', 'javauser1', 'petclinic'),
(2, 1, 'DocInaBoxTest', 'javauser1', 'docinabox'),
(3, 1, 'DocInaBoxTest', 'javauser2', 'docinabox');
