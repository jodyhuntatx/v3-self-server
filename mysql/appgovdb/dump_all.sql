SELECT pr.name, appid.name, ca.resource_type, ca.resource_name, ca.username, sf.name
FROM projects pr, appidentities appid, accessrequests ar, safes sf, cybraccounts ca
WHERE ar.provisioned AND NOT ar.revoked
AND ar.app_id = appid.id
AND ar.project_id = pr.id 
AND ar.project_id = pr.id
AND ar.safe_id = sf.id
AND ca.safe_id = sf.id;
