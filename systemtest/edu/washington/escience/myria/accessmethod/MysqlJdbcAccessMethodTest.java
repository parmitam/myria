/**
 * 
 */
package edu.washington.escience.myria.accessmethod;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.google.common.collect.ImmutableList;

import edu.washington.escience.myria.DbException;
import edu.washington.escience.myria.Schema;
import edu.washington.escience.myria.TupleBatch;
import edu.washington.escience.myria.Type;
import edu.washington.escience.myria.operator.DbQueryScan;

/**
 * @author dhalperi
 * 
 */
public class MysqlJdbcAccessMethodTest {

  @Test
  public void testNumberResultsAndMultipleBatches() throws DbException, InterruptedException {
    /* Connection information */
    final String host = "54.213.118.143";
    final int port = 3306;
    final String user = "myria";
    final String password = "nays26[shark";
    final String dbms = "mysql";
    final String databaseName = "myria_test";
    final String jdbcDriverName = "com.mysql.jdbc.Driver";
    final int expectedNumResults = 250; /* Hardcoded in setup_testtablebig.sql */
    final JdbcInfo jdbcInfo = JdbcInfo.of(jdbcDriverName, dbms, host, port, databaseName, user, password);
    /* Query information */
    final String query = "select * from testtablebig";
    final ImmutableList<Type> types = ImmutableList.of(Type.INT_TYPE);
    final ImmutableList<String> columnNames = ImmutableList.of("value");
    final Schema schema = new Schema(types, columnNames);

    /* Build up the DbQueryScan parameters and open the scan */
    final DbQueryScan scan = new DbQueryScan(jdbcInfo, query, schema);
    scan.open(null);

    /* Count up the results and assert they match expectations */
    int count = 0;
    TupleBatch tb = null;
    while (!scan.eos()) {
      tb = scan.nextReady();
      if (tb != null) {
        count += tb.numTuples();
      }
    }
    assertEquals(expectedNumResults, count);

    /* Cleanup */
    scan.close();
  }

}
