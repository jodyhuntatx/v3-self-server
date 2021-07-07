SELECT pr.name, appid.name, ca.name, ca.username
FROM projects pr, appidentities appid, accessrequests ar, safes sf, cybraccounts ca
WHERE
ar.app_id = appid.id
AND ar.project_id = pr.id 
AND ar.safe_id = sf.id
AND ca.safe_id = sf.id;
