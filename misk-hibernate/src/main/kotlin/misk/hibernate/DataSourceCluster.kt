package misk.hibernate

import javax.sql.DataSource

class DataSourceCluster(
  override val writer: DataSource,
  override val reader: DataSource
) : Cluster<DataSource>
