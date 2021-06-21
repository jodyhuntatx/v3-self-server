cat base.yml | docker exec -i conjur-cli conjur policy load root -
cat group.yml | docker exec -i conjur-cli conjur policy load foo -
cat safe.yml | docker exec -i conjur-cli conjur policy load root -
cat id.yml | docker exec -i conjur-cli conjur policy load foo -
cat grant.yml | docker exec -i conjur-cli conjur policy load foo -
