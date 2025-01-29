# IEA Task 18 openEMS development environment

This docker compose setup provides:

 * [openems-ui](http://openems-ui.localhost/)
 * [InfluxDB](http://influxdb.localhost/)
 * [Grafana](http://grafana.localhost/)

To start, run ```docker compose up -d```. To stop it, run ```docker compose down```. To also remove all volumes and thus all data in influxdb, run ```docker compose down -v```.

With openEMS running, use [Apache Felix](http://localhost:8080/system/console/configMgr) to configure the "Timedata InfluxDB" plugin:

 * URL: http://localhost:8086
 * Org: IEA_T18
 * ApiKey: DOCKER_INFLUXDB_INIT_ADMIN_TOKEN value from .env file
 * Bucket: openems

Once openEMS is writing data to InfluxDB, it can be seen via the [Grafana sample dashboard](http://grafana.localhost/d/deav5oih4ej28f/openems?orgId=1&from=now-1h&to=now&timezone=browser) (login: admin, password: openems).
