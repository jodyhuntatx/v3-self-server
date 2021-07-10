SELECT appid.name, ca.resource_type, ca.resource_name, ca.username, sf.name
FROM appidentities appid, accessrequests ar, safes sf, cybraccounts ca
WHERE ar.provisioned AND NOT ar.revoked
AND ar.project_id = 1
AND appid.project_id = 1
AND appid.id = 2
AND ar.app_id = 2
AND ar.safe_id = ca.safe_id
AND ca.safe_id = sf.id;
