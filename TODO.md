1. Move tests to docker-compose based databases.
2. Upgrade test dependencies.
3. Get rid of JMS. We don't need it in Wise.
4. Find a better way to check that connections closing properly, maybe on db level once move to docker compose.