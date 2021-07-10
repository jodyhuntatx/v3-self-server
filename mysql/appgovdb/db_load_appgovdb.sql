USE appgovdb;

INSERT INTO projects
(id,name,admin_user,billing_code)
VALUES
(1,'project1','jhunt','1024'),
(2,'project2','bob','2048'),
(3,'project3','doug','4096')
;

INSERT INTO appidentities
(id,project_id,name,authn_method,
authn_attribute1_key,authn_attribute1_value,
authn_attribute2_key,authn_attribute2_value,
authn_attribute3_key,authn_attribute3_value) 
VALUES
(1,1,'app-dev1','authn-k8s',
'authn-k8s/namespace','k8s-dev',
'auth-k8s/service-account','app-dev',
'authn-k8s/authentication-container-name','authenticator'),
(2,1,'app-dev2','authn-k8s',
'authn-k8s/namespace','k8s-dev',
'auth-k8s/service-account','app-dev',
'authn-k8s/authentication-container-name','authenticator'),
(3,2,'app-test','authn-k8s',
'authn-k8s/namespace','k8s-test',
'auth-k8s/service-account','app-test',
'authn-k8s/authentication-container-name','authenticator'),
(4,3,'app-prod','authn-k8s',
'authn-k8s/namespace','k8s-prod',
'auth-k8s/service-account','app-prod',
'authn-k8s/authentication-container-name','authenticator')
;

INSERT INTO safes
(id,name,vault_name,cpm_name)
VALUES
(1,'PetClinicDev','DemoVault','PasswordManager'),
(2,'PetClinicTest','DemoVault','PasswordManager')
;

INSERT INTO accessrequests 
(id,project_id,app_id,safe_id,datetime,approved,rejected,provisioned,revoked,environment,lob_name,requestor)
VALUES
(1,1,1,1,NOW(),0,0,0,0,'dev','CICD','jhunt'),
(2,1,2,2,NOW(),0,0,0,0,'test','CICD','jhunt'),
(3,2,3,2,NOW(),0,0,0,0,'test','CICD','jhunt')
;

INSERT INTO cybraccounts
(id,safe_id,name,platform_id,secret_type,username,address,resource_type,resource_name)
VALUES
(1,1,'MySQL','MySQL','Password','javauser1','conjurmaster2.northcentralus.cloudapp.azure.com','database','petclinic'),
(2,2,'MySQL','MySQL','Password','test_user1','mysql-db.cyberark.svc.cluster.local','database','petclinic')
;
