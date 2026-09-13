const express = require('express');
const Database = require('better-sqlite3');
const path = require('path');
const fs = require('fs');

const app = express();
app.use(express.json());

// Database
const dbPath = path.join(
  __dirname,
  '../../../../../retail_database.db'
);

const db = new Database(dbPath);


// ============================================================
// SCHEMA INTROSPECTION
// ============================================================

app.get('/api/schema/introspect', (req, res) => {

  try {

    const tableRows = db.prepare(
      "SELECT name FROM sqlite_master WHERE type = 'table' AND name NOT LIKE 'sqlite_%'"
    ).all();

    const tables = {};

    for (const { name: tableName } of tableRows) {

      const columnInfo =
        db.prepare(
          `PRAGMA table_info(${tableName})`
        ).all();

      const foreignKeys =
        db.prepare(
          `PRAGMA foreign_key_list(${tableName})`
        ).all();

      const columns =
        columnInfo.map(col => {

          let sample = null;

          try {

            const row =
              db.prepare(`
                SELECT "${col.name}" AS v
                FROM "${tableName}"
                WHERE "${col.name}" IS NOT NULL
                LIMIT 1
              `).get();

            sample =
              row
                ? row.v
                : null;

          } catch (err) {

            sample = null;
          }

          return {
            name: col.name,
            sqlType: col.type,
            nullable: col.notnull === 0,
            primaryKey: col.pk > 0,
            sample
          };
        });

      tables[tableName] = {

        columns,

        foreignKeys:
          foreignKeys.map(fk => ({
            column: fk.from,
            refTable: fk.table,
            refColumn: fk.to
          }))
      };
    }

    res.json({
      success: true,
      tables
    });

  } catch (err) {

    res.status(500).json({
      success: false,
      error: err.message
    });
  }
});


// ============================================================
// GENERAL QUERY HELPERS
// ============================================================

function tableExists(table) {

  return !!db.prepare(
    "SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = ?"
  ).get(table);
}


function columnNames(table) {

  return db.prepare(
    `PRAGMA table_info("${table}")`
  )
    .all()
    .map(c => c.name);
}


// ============================================================
// SAME-TABLE COMPARISON
// ============================================================

app.get('/api/query/compare', (req, res) => {

  try {

    const {
      table,
      measureColumn,
      groupColumn,
      aggFn
    } = req.query;

    const ALLOWED_AGG = {
      SUM: 'SUM',
      AVG: 'AVG',
      COUNT: 'COUNT',
      MIN: 'MIN',
      MAX: 'MAX'
    };

    const fn =
      ALLOWED_AGG[
        String(
          aggFn || 'SUM'
        ).toUpperCase()
      ];

    if (!fn) {

      return res.status(400).json({
        success: false,
        error: `Unsupported aggFn: ${aggFn}`
      });
    }

    if (!tableExists(table)) {

      return res.status(400).json({
        success: false,
        error: `Unknown table: ${table}`
      });
    }

    const cols =
      columnNames(table);

    if (!cols.includes(measureColumn)) {

      return res.status(400).json({
        success: false,
        error: `Unknown column: ${measureColumn}`
      });
    }

    if (!cols.includes(groupColumn)) {

      return res.status(400).json({
        success: false,
        error: `Unknown column: ${groupColumn}`
      });
    }

    const query = `
      SELECT
        "${groupColumn}" AS label,
        ${fn}("${measureColumn}") AS value
      FROM "${table}"
      GROUP BY "${groupColumn}"
      ORDER BY "${groupColumn}" ASC
    `;

    const rows =
      db.prepare(query).all();

    res.json({
      success: true,
      data: rows
    });

  } catch (err) {

    res.status(500).json({
      success: false,
      error: err.message
    });
  }
});


// ============================================================
// DIRECT TWO-TABLE JOIN COMPARISON
// ============================================================

app.get('/api/query/compare-join', (req, res) => {

  try {

    const {
      tableA,
      measureColumn,
      joinColumnA,
      tableB,
      groupColumn,
      joinColumnB,
      aggFn
    } = req.query;

    const ALLOWED_AGG = {
      SUM: 'SUM',
      AVG: 'AVG',
      COUNT: 'COUNT',
      MIN: 'MIN',
      MAX: 'MAX'
    };

    const fn =
      ALLOWED_AGG[
        String(
          aggFn || 'SUM'
        ).toUpperCase()
      ];

    if (!fn) {

      return res.status(400).json({
        success: false,
        error: `Unsupported aggFn: ${aggFn}`
      });
    }

    for (const table of [tableA, tableB]) {

      if (!tableExists(table)) {

        return res.status(400).json({
          success: false,
          error: `Unknown table: ${table}`
        });
      }
    }

    const colsA =
      columnNames(tableA);

    const colsB =
      columnNames(tableB);

    const checks = [
      [colsA, measureColumn],
      [colsA, joinColumnA],
      [colsB, groupColumn],
      [colsB, joinColumnB]
    ];

    for (const [cols, name] of checks) {

      if (!cols.includes(name)) {

        return res.status(400).json({
          success: false,
          error: `Unknown column: ${name}`
        });
      }
    }

    const query = `
      SELECT
        B."${groupColumn}" AS label,
        ${fn}(A."${measureColumn}") AS value
      FROM "${tableA}" A
      JOIN "${tableB}" B
        ON A."${joinColumnA}" = B."${joinColumnB}"
      GROUP BY B."${groupColumn}"
      ORDER BY B."${groupColumn}" ASC
    `;

    const rows =
      db.prepare(query).all();

    res.json({
      success: true,
      data: rows
    });

  } catch (err) {

    res.status(500).json({
      success: false,
      error: err.message
    });
  }
});


// ============================================================
// KPI SNAPSHOT
// ============================================================

app.get('/api/kpis/summary', (req, res) => {

  try {

    const {
      year_from,
      year_to
    } = req.query;

    let query =
      'SELECT * FROM kpi_snapshot';

    const conditions = [];
    const params = [];

    if (year_from) {

      conditions.push(
        'kpi_year >= ?'
      );

      params.push(
        year_from
      );
    }

    if (year_to) {

      conditions.push(
        'kpi_year <= ?'
      );

      params.push(
        year_to
      );
    }

    if (conditions.length > 0) {

      query +=
        ' WHERE '
        + conditions.join(' AND ');
    }

    query +=
      ' ORDER BY kpi_year ASC';

    const rows =
      db.prepare(query).all(
        ...params
      );

    res.json({
      success: true,
      data: rows
    });

  } catch (err) {

    res.status(500).json({
      success: false,
      error: err.message
    });
  }
});


// ============================================================
// DASHBOARD ANALYTICS HELPERS
// ============================================================

const MONTHS = {

  January: 1,
  February: 2,
  March: 3,
  April: 4,
  May: 5,
  June: 6,
  July: 7,
  August: 8,
  September: 9,
  October: 10,
  November: 11,
  December: 12
};


function pad2(number) {

  return String(
    number
  ).padStart(
    2,
    '0'
  );
}


function isoDate(
  year,
  month,
  day
) {

  return (
    `${year}-${pad2(month)}-${pad2(day)}`
  );
}


function daysInMonth(
  year,
  month
) {

  return new Date(
    year,
    month,
    0
  ).getDate();
}


/*
 * Converts the dashboard filters into
 * a real date range.
 *
 * Supports:
 * Yearly
 * Quarterly
 * Monthly
 * Weekly
 */
function dashboardRange(req) {

  const year =
    Number(
      req.query.year || 2023
    );

  const scope =
    String(
      req.query.scope || 'Yearly'
    ).toLowerCase();

  const period =
    String(
      req.query.period || 'Full Year'
    );


  // YEARLY

  if (
    scope === 'yearly'
  ) {

    return {
      start:
        `${year}-01-01`,

      end:
        `${year}-12-31`
    };
  }


  // QUARTERLY

  if (
    scope === 'quarterly'
  ) {

    const quarter =
      Number(
        period.replace(
          /[^1-4]/g,
          ''
        )
      ) || 1;

    const startMonth =
      (
        quarter - 1
      )
      * 3
      + 1;

    const endMonth =
      startMonth + 2;

    return {

      start:
        isoDate(
          year,
          startMonth,
          1
        ),

      end:
        isoDate(
          year,
          endMonth,
          daysInMonth(
            year,
            endMonth
          )
        )
    };
  }


  // MONTHLY

  if (
    scope === 'monthly'
  ) {

    const month =
      MONTHS[period] || 1;

    return {

      start:
        isoDate(
          year,
          month,
          1
        ),

      end:
        isoDate(
          year,
          month,
          daysInMonth(
            year,
            month
          )
        )
    };
  }


  // WEEKLY

  if (
    scope === 'weekly'
  ) {

    const selectedMonthName =
      String(
        req.query.month || 'January'
      );

    const month =
      MONTHS[
        selectedMonthName
      ] || 1;

    const week =
      Number(
        period.replace(
          /[^1-5]/g,
          ''
        )
      ) || 1;

    const startDay =
      (
        week - 1
      )
      * 7
      + 1;

    const lastDay =
      daysInMonth(
        year,
        month
      );

    const safeStart =
      Math.min(
        startDay,
        lastDay
      );

    const endDay =
      Math.min(
        startDay + 6,
        lastDay
      );

    return {

      start:
        isoDate(
          year,
          month,
          safeStart
        ),

      end:
        isoDate(
          year,
          month,
          endDay
        )
    };
  }


  return {

    start:
      `${year}-01-01`,

    end:
      `${year}-12-31`
  };
}


function previousRange(range) {

  const start =
    new Date(
      range.start
      + 'T00:00:00'
    );

  const end =
    new Date(
      range.end
      + 'T00:00:00'
    );

  const duration =
    Math.round(
      (
        end - start
      )
      / 86400000
    )
    + 1;

  const previousEnd =
    new Date(
      start
    );

  previousEnd.setDate(
    previousEnd.getDate()
    - 1
  );

  const previousStart =
    new Date(
      previousEnd
    );

  previousStart.setDate(
    previousStart.getDate()
    - duration
    + 1
  );

  const format =
    date =>
      `${date.getFullYear()}-${pad2(
        date.getMonth() + 1
      )}-${pad2(
        date.getDate()
      )}`;

  return {

    start:
      format(
        previousStart
      ),

    end:
      format(
        previousEnd
      )
  };
}


function regionClause(
  alias,
  region,
  params
) {

  if (
    region
    && region !== 'All Regions'
  ) {

    params.push(
      region
    );

    return (
      ` AND ${alias}.region = ? `
    );
  }

  return '';
}


function sendRows(
  res,
  rows
) {

  res.json({
    success: true,
    data: rows
  });
}


function safeRoute(
  res,
  fn
) {

  try {

    fn();

  } catch (err) {

    console.error(
      err
    );

    res.status(500).json({
      success: false,
      error: err.message
    });
  }
}


// ============================================================
// OVERVIEW
// ============================================================

app.get(
  '/api/dashboard/overview',
  (req, res) =>
    safeRoute(
      res,
      () => {

        const range =
          dashboardRange(
            req
          );

        const previous =
          previousRange(
            range
          );

        const region =
          String(
            req.query.region
            || 'All Regions'
          );


        // -----------------------------
        // REVENUE
        // -----------------------------

        const salesParams = [
          range.start,
          range.end
        ];

        const salesRegion =
          regionClause(
            's',
            region,
            salesParams
          );

        const currentRevenue =
          db.prepare(`
            SELECT
              COALESCE(
                SUM(s.revenue),
                0
              ) AS value

            FROM sales s

            WHERE
              s.order_date
              BETWEEN ? AND ?

              ${salesRegion}
          `)
            .get(
              ...salesParams
            )
            .value;


        // -----------------------------
        // PREVIOUS REVENUE
        // -----------------------------

        const previousParams = [
          previous.start,
          previous.end
        ];

        const previousRegion =
          regionClause(
            's',
            region,
            previousParams
          );

        const previousRevenue =
          db.prepare(`
            SELECT
              COALESCE(
                SUM(s.revenue),
                0
              ) AS value

            FROM sales s

            WHERE
              s.order_date
              BETWEEN ? AND ?

              ${previousRegion}
          `)
            .get(
              ...previousParams
            )
            .value;


        const growth =
          previousRevenue === 0
            ? 0
            : (
                (
                  currentRevenue
                  - previousRevenue
                )
                / previousRevenue
              )
              * 100;


        // -----------------------------
        // PROFIT + PROFIT MARGIN
        // sales + products
        // -----------------------------

        const profitParams = [
          range.start,
          range.end
        ];

        const profitRegion =
          regionClause(
            's',
            region,
            profitParams
          );

        const profitRow =
          db.prepare(`
            SELECT

              COALESCE(
                SUM(
                  s.revenue
                  -
                  (
                    s.quantity
                    * p.cost
                  )
                ),
                0
              ) AS profit,

              CASE

                WHEN
                  SUM(
                    s.revenue
                  ) = 0

                THEN 0

                ELSE

                  SUM(
                    s.revenue
                    -
                    (
                      s.quantity
                      * p.cost
                    )
                  )
                  * 100.0
                  /
                  SUM(
                    s.revenue
                  )

              END AS margin

            FROM sales s

            JOIN products p
              ON p.product_id
              = s.product_id

            WHERE
              s.order_date
              BETWEEN ? AND ?

              ${profitRegion}
          `)
            .get(
              ...profitParams
            );


        // -----------------------------
        // INVENTORY TURNOVER
        // sales + products + inventory
        // -----------------------------

        const cogsParams = [
          range.start,
          range.end
        ];

        const cogsRegion =
          regionClause(
            's',
            region,
            cogsParams
          );

        const cogs =
          db.prepare(`
            SELECT
              COALESCE(
                SUM(
                  s.quantity
                  * p.cost
                ),
                0
              ) AS value

            FROM sales s

            JOIN products p
              ON p.product_id
              = s.product_id

            WHERE
              s.order_date
              BETWEEN ? AND ?

              ${cogsRegion}
          `)
            .get(
              ...cogsParams
            )
            .value;


        const inventoryValue =
          db.prepare(`
            SELECT
              COALESCE(
                AVG(
                  i.stock_level
                  * p.cost
                ),
                0
              ) AS value

            FROM inventory i

            JOIN products p
              ON p.product_id
              = i.product_id

            WHERE
              i.snapshot_date
              BETWEEN ? AND ?
          `)
            .get(
              range.start,
              range.end
            )
            .value;


        const turnover =
          inventoryValue === 0
            ? 0
            : cogs
              / inventoryValue;


        // -----------------------------
        // CUSTOMER RETENTION
        // sales + customers
        // -----------------------------

        const priorParams = [
          previous.start,
          previous.end
        ];

        const priorRegion =
          regionClause(
            's',
            region,
            priorParams
          );

        const priorCount =
          db.prepare(`
            SELECT
              COUNT(
                DISTINCT s.customer_id
              ) AS value

            FROM sales s

            JOIN customers c
              ON c.customer_id
              = s.customer_id

            WHERE
              s.order_date
              BETWEEN ? AND ?

              ${priorRegion}
          `)
            .get(
              ...priorParams
            )
            .value;


        const returnedParams = [
          range.start,
          range.end
        ];

        let currentRegionSql = '';
        let previousRegionSql = '';

        if (
          region
          && region !== 'All Regions'
        ) {

          currentRegionSql =
            'AND current.region = ?';

          returnedParams.push(
            region
          );
        }

        returnedParams.push(
          previous.start,
          previous.end
        );

        if (
          region
          && region !== 'All Regions'
        ) {

          previousRegionSql =
            'AND previousSales.region = ?';

          returnedParams.push(
            region
          );
        }


        const returned =
          db.prepare(`
            SELECT
              COUNT(
                DISTINCT current.customer_id
              ) AS value

            FROM sales current

            JOIN customers c
              ON c.customer_id
              = current.customer_id

            WHERE
              current.order_date
              BETWEEN ? AND ?

              ${currentRegionSql}

              AND EXISTS (

                SELECT 1

                FROM sales previousSales

                WHERE
                  previousSales.customer_id
                  = current.customer_id

                  AND
                  previousSales.order_date
                  BETWEEN ? AND ?

                  ${previousRegionSql}
              )
          `)
            .get(
              ...returnedParams
            )
            .value;


        const retention =
          priorCount === 0
            ? 0
            : returned
              * 100.0
              / priorCount;


        // -----------------------------
        // MARKETING ROI
        //
        // Whole-business only because
        // marketing has no region FK.
        // -----------------------------

        const wholeRevenue =
          db.prepare(`
            SELECT
              COALESCE(
                SUM(revenue),
                0
              ) AS value

            FROM sales

            WHERE
              order_date
              BETWEEN ? AND ?
          `)
            .get(
              range.start,
              range.end
            )
            .value;


        const marketingCost =
          db.prepare(`
            SELECT
              COALESCE(
                SUM(cost),
                0
              ) AS value

            FROM marketing

            WHERE
              campaign_date
              BETWEEN ? AND ?
          `)
            .get(
              range.start,
              range.end
            )
            .value;


        const marketingRoi =
          marketingCost === 0
            ? 0
            : (
                (
                  wholeRevenue
                  - marketingCost
                )
                / marketingCost
              )
              * 100;


        res.json({

          success: true,

          data: {

            total_revenue:
              Number(
                currentRevenue.toFixed(
                  2
                )
              ),

            revenue_growth_pct:
              Number(
                growth.toFixed(
                  2
                )
              ),

            profit:
              Number(
                profitRow.profit.toFixed(
                  2
                )
              ),

            profit_margin_pct:
              Number(
                profitRow.margin.toFixed(
                  2
                )
              ),

            inventory_turnover:
              Number(
                turnover.toFixed(
                  4
                )
              ),

            customer_retention_pct:
              Number(
                retention.toFixed(
                  2
                )
              ),

            marketing_roi_pct:
              Number(
                marketingRoi.toFixed(
                  2
                )
              )
          }
        });
      }
    )
);


// ============================================================
// SALES
// ============================================================


// Revenue over time

app.get(
  '/api/sales/revenue-trend',
  (req, res) =>
    safeRoute(
      res,
      () => {

        const range =
          dashboardRange(
            req
          );

        const region =
          String(
            req.query.region
            || 'All Regions'
          );

        const params = [
          range.start,
          range.end
        ];

        const regionSql =
          regionClause(
            's',
            region,
            params
          );

        const rows =
          db.prepare(`
            SELECT

              strftime(
                '%Y-%m',
                s.order_date
              ) AS label,

              ROUND(
                SUM(
                  s.revenue
                ),
                2
              ) AS value

            FROM sales s

            WHERE
              s.order_date
              BETWEEN ? AND ?

              ${regionSql}

            GROUP BY label

            ORDER BY label
          `)
            .all(
              ...params
            );

        sendRows(
          res,
          rows
        );
      }
    )
);


// Revenue by region

app.get(
  '/api/sales/revenue-region',
  (req, res) =>
    safeRoute(
      res,
      () => {

        const range =
          dashboardRange(
            req
          );

        const rows =
          db.prepare(`
            SELECT

              COALESCE(
                region,
                'Unknown'
              ) AS label,

              ROUND(
                SUM(
                  revenue
                ),
                2
              ) AS value

            FROM sales

            WHERE
              order_date
              BETWEEN ? AND ?

            GROUP BY region

            ORDER BY value DESC
          `)
            .all(
              range.start,
              range.end
            );

        sendRows(
          res,
          rows
        );
      }
    )
);


// CROSS KPI:
// Profit Margin % over time
// sales + products

app.get(
  '/api/sales/profit-margin',
  (req, res) =>
    safeRoute(
      res,
      () => {

        const range =
          dashboardRange(
            req
          );

        const region =
          String(
            req.query.region
            || 'All Regions'
          );

        const params = [
          range.start,
          range.end
        ];

        const regionSql =
          regionClause(
            's',
            region,
            params
          );

        const rows =
          db.prepare(`
            SELECT

              strftime(
                '%Y-%m',
                s.order_date
              ) AS label,

              ROUND(

                CASE

                  WHEN
                    SUM(
                      s.revenue
                    ) = 0

                  THEN 0

                  ELSE

                    SUM(
                      s.revenue
                      -
                      (
                        s.quantity
                        * p.cost
                      )
                    )
                    * 100.0
                    /
                    SUM(
                      s.revenue
                    )

                END,

                2

              ) AS value

            FROM sales s

            JOIN products p
              ON p.product_id
              = s.product_id

            WHERE
              s.order_date
              BETWEEN ? AND ?

              ${regionSql}

            GROUP BY label

            ORDER BY label
          `)
            .all(
              ...params
            );

        sendRows(
          res,
          rows
        );
      }
    )
);


// CROSS:
// Quantity vs Revenue by Product
// category included for scatter colouring

app.get(
  '/api/sales/quantity-revenue',
  (req, res) =>
    safeRoute(
      res,
      () => {

        const range =
          dashboardRange(
            req
          );

        const region =
          String(
            req.query.region
            || 'All Regions'
          );

        const params = [
          range.start,
          range.end
        ];

        const regionSql =
          regionClause(
            's',
            region,
            params
          );

        const rows =
          db.prepare(`
            SELECT

              CAST(
                s.product_id
                AS TEXT
              ) AS label,

              p.category
                AS category,

              SUM(
                s.quantity
              ) AS x,

              ROUND(
                SUM(
                  s.revenue
                ),
                2
              ) AS y

            FROM sales s

            JOIN products p
              ON p.product_id
              = s.product_id

            WHERE
              s.order_date
              BETWEEN ? AND ?

              ${regionSql}

            GROUP BY
              s.product_id,
              p.category

            ORDER BY
              p.category,
              s.product_id
          `)
            .all(
              ...params
            );

        sendRows(
          res,
          rows
        );
      }
    )
);


// ============================================================
// INVENTORY
// ============================================================


// SOLO:
// Stock levels by warehouse

app.get(
  '/api/inventory/stock-warehouse',
  (req, res) =>
    safeRoute(
      res,
      () => {

        const range =
          dashboardRange(
            req
          );

        const rows =
          db.prepare(`
            SELECT

              warehouse
                AS label,

              SUM(
                stock_level
              ) AS value

            FROM inventory

            WHERE
              snapshot_date
              BETWEEN ? AND ?

            GROUP BY warehouse

            ORDER BY warehouse
          `)
            .all(
              range.start,
              range.end
            );

        sendRows(
          res,
          rows
        );
      }
    )
);


// SOLO:
// Average stock level over time

app.get(
  '/api/inventory/stock-trend',
  (req, res) =>
    safeRoute(
      res,
      () => {

        const range =
          dashboardRange(
            req
          );

        const rows =
          db.prepare(`
            SELECT

              strftime(
                '%Y-%m',
                snapshot_date
              ) AS label,

              ROUND(
                AVG(
                  stock_level
                ),
                2
              ) AS value

            FROM inventory

            WHERE
              snapshot_date
              BETWEEN ? AND ?

            GROUP BY label

            ORDER BY label
          `)
            .all(
              range.start,
              range.end
            );

        sendRows(
          res,
          rows
        );
      }
    )
);


// CROSS KPI:
// Inventory turnover
// sales + products + inventory

app.get(
  '/api/inventory/turnover',
  (req, res) =>
    safeRoute(
      res,
      () => {

        const range =
          dashboardRange(
            req
          );

        const region =
          String(
            req.query.region
            || 'All Regions'
          );

        const params = [
          range.start,
          range.end
        ];

        let regionSql = '';

        if (
          region
          && region !== 'All Regions'
        ) {

          regionSql =
            'AND s.region = ?';

          params.push(
            region
          );
        }

        params.push(
          range.start,
          range.end
        );


        const rows =
          db.prepare(`
            WITH cogs AS (

              SELECT

                strftime(
                  '%Y-%m',
                  s.order_date
                ) AS month,

                SUM(
                  s.quantity
                  * p.cost
                ) AS cogs

              FROM sales s

              JOIN products p
                ON p.product_id
                = s.product_id

              WHERE
                s.order_date
                BETWEEN ? AND ?

                ${regionSql}

              GROUP BY month
            ),

            inventory_value AS (

              SELECT

                strftime(
                  '%Y-%m',
                  i.snapshot_date
                ) AS month,

                AVG(
                  i.stock_level
                  * p.cost
                ) AS inv_value

              FROM inventory i

              JOIN products p
                ON p.product_id
                = i.product_id

              WHERE
                i.snapshot_date
                BETWEEN ? AND ?

              GROUP BY month
            )

            SELECT

              cogs.month
                AS label,

              ROUND(

                CASE

                  WHEN
                    inventory_value.inv_value
                    IS NULL

                    OR
                    inventory_value.inv_value
                    = 0

                  THEN 0

                  ELSE
                    cogs.cogs
                    /
                    inventory_value.inv_value

                END,

                4

              ) AS value

            FROM cogs

            JOIN inventory_value
              ON inventory_value.month
              = cogs.month

            ORDER BY cogs.month
          `)
            .all(
              ...params
            );

        sendRows(
          res,
          rows
        );
      }
    )
);


// CROSS:
// Stock cover by category

app.get(
  '/api/inventory/stock-cover',
  (req, res) =>
    safeRoute(
      res,
      () => {

        const range =
          dashboardRange(
            req
          );

        const region =
          String(
            req.query.region
            || 'All Regions'
          );

        const params = [
          range.start,
          range.end,
          range.start,
          range.end
        ];

        let regionSql = '';

        if (
          region
          && region !== 'All Regions'
        ) {

          regionSql =
            'AND s.region = ?';

          params.push(
            region
          );
        }


        const rows =
          db.prepare(`
            WITH stock AS (

              SELECT

                p.category,

                SUM(
                  i.stock_level
                ) AS stock

              FROM inventory i

              JOIN products p
                ON p.product_id
                = i.product_id

              WHERE
                i.snapshot_date
                BETWEEN ? AND ?

              GROUP BY p.category
            ),

            sold AS (

              SELECT

                p.category,

                SUM(
                  s.quantity
                ) AS sold

              FROM sales s

              JOIN products p
                ON p.product_id
                = s.product_id

              WHERE
                s.order_date
                BETWEEN ? AND ?

                ${regionSql}

              GROUP BY p.category
            ),

            categories AS (

              SELECT category
              FROM stock

              UNION

              SELECT category
              FROM sold
            )

            SELECT

              categories.category,

              COALESCE(
                stock.stock,
                0
              ) AS stock,

              COALESCE(
                sold.sold,
                0
              ) AS sold

            FROM categories

            LEFT JOIN stock
              USING(category)

            LEFT JOIN sold
              USING(category)

            ORDER BY
              categories.category
          `)
            .all(
              ...params
            );


        const data = [];


        for (const row of rows) {

          let status;

          if (
            row.sold === 0
          ) {

            status =
              'OVER';

          } else if (
            row.stock
            > row.sold * 1.2
          ) {

            status =
              'OVER';

          } else if (
            row.stock
            < row.sold * 0.8
          ) {

            status =
              'UNDER';

          } else {

            status =
              'BALANCED';
          }


          const label =
            `${row.category} [${status}]`;


          data.push({

            label,

            series:
              'Units in stock',

            value:
              row.stock
          });


          data.push({

            label,

            series:
              'Units sold',

            value:
              row.sold
          });
        }


        sendRows(
          res,
          data
        );
      }
    )
);


// ============================================================
// PRODUCTS
// ============================================================


// SOLO:
// Price vs Cost

app.get(
  '/api/products/price-cost',
  (req, res) =>
    safeRoute(
      res,
      () => {

        const rows =
          db.prepare(`
            SELECT

              CAST(
                product_id
                AS TEXT
              ) AS label,

              category,

              price AS x,

              cost AS y

            FROM products

            ORDER BY product_id
          `)
            .all();

        sendRows(
          res,
          rows
        );
      }
    )
);


// SOLO:
// Catalogue margin by category

app.get(
  '/api/products/catalogue-margin',
  (req, res) =>
    safeRoute(
      res,
      () => {

        const rows =
          db.prepare(`
            SELECT

              category
                AS label,

              ROUND(

                AVG(

                  CASE

                    WHEN price = 0

                    THEN 0

                    ELSE

                      (
                        price
                        - cost
                      )
                      * 100.0
                      /
                      price

                  END
                ),

                2

              ) AS value

            FROM products

            GROUP BY category

            ORDER BY category
          `)
            .all();

        sendRows(
          res,
          rows
        );
      }
    )
);


// CROSS KPI:
// Realised Profit Margin by category

app.get(
  '/api/products/realised-margin',
  (req, res) =>
    safeRoute(
      res,
      () => {

        const range =
          dashboardRange(
            req
          );

        const region =
          String(
            req.query.region
            || 'All Regions'
          );

        const params = [
          range.start,
          range.end
        ];

        const regionSql =
          regionClause(
            's',
            region,
            params
          );


        const rows =
          db.prepare(`
            SELECT

              p.category
                AS label,

              ROUND(

                CASE

                  WHEN
                    SUM(
                      s.revenue
                    ) = 0

                  THEN 0

                  ELSE

                    SUM(
                      s.revenue
                      -
                      (
                        s.quantity
                        * p.cost
                      )
                    )
                    * 100.0
                    /
                    SUM(
                      s.revenue
                    )

                END,

                2

              ) AS value

            FROM products p

            JOIN sales s
              ON s.product_id
              = p.product_id

            WHERE
              s.order_date
              BETWEEN ? AND ?

              ${regionSql}

            GROUP BY p.category

            ORDER BY p.category
          `)
            .all(
              ...params
            );

        sendRows(
          res,
          rows
        );
      }
    )
);


// CROSS:
// Revenue by category

app.get(
  '/api/products/revenue-category',
  (req, res) =>
    safeRoute(
      res,
      () => {

        const range =
          dashboardRange(
            req
          );

        const region =
          String(
            req.query.region
            || 'All Regions'
          );

        const params = [
          range.start,
          range.end
        ];

        const regionSql =
          regionClause(
            's',
            region,
            params
          );


        const rows =
          db.prepare(`
            SELECT

              p.category
                AS label,

              ROUND(
                SUM(
                  s.revenue
                ),
                2
              ) AS value

            FROM products p

            JOIN sales s
              ON s.product_id
              = p.product_id

            WHERE
              s.order_date
              BETWEEN ? AND ?

              ${regionSql}

            GROUP BY p.category

            ORDER BY value DESC
          `)
            .all(
              ...params
            );

        sendRows(
          res,
          rows
        );
      }
    )
);


// ============================================================
// MARKETING
// ============================================================


// SOLO:
// Spend by channel

app.get(
  '/api/marketing/spend-channel',
  (req, res) =>
    safeRoute(
      res,
      () => {

        const range =
          dashboardRange(
            req
          );

        const rows =
          db.prepare(`
            SELECT

              channel
                AS label,

              ROUND(
                SUM(
                  cost
                ),
                2
              ) AS value

            FROM marketing

            WHERE
              campaign_date
              BETWEEN ? AND ?

            GROUP BY channel

            ORDER BY value DESC
          `)
            .all(
              range.start,
              range.end
            );

        sendRows(
          res,
          rows
        );
      }
    )
);


// SOLO:
// Cost per conversion by channel

app.get(
  '/api/marketing/cost-conversion',
  (req, res) =>
    safeRoute(
      res,
      () => {

        const range =
          dashboardRange(
            req
          );

        const rows =
          db.prepare(`
            SELECT

              channel
                AS label,

              ROUND(

                CASE

                  WHEN
                    SUM(
                      conversions
                    ) = 0

                  THEN 0

                  ELSE

                    SUM(
                      cost
                    )
                    * 1.0
                    /
                    SUM(
                      conversions
                    )

                END,

                2

              ) AS value

            FROM marketing

            WHERE
              campaign_date
              BETWEEN ? AND ?

            GROUP BY channel

            ORDER BY channel
          `)
            .all(
              range.start,
              range.end
            );

        sendRows(
          res,
          rows
        );
      }
    )
);


// CROSS KPI:
// Whole-business Marketing ROI over time

app.get(
  '/api/marketing/roi-trend',
  (req, res) =>
    safeRoute(
      res,
      () => {

        const range =
          dashboardRange(
            req
          );


        const rows =
          db.prepare(`
            WITH revenue AS (

              SELECT

                strftime(
                  '%Y-%m',
                  order_date
                ) AS month,

                SUM(
                  revenue
                ) AS revenue

              FROM sales

              WHERE
                order_date
                BETWEEN ? AND ?

              GROUP BY month
            ),

            spend AS (

              SELECT

                strftime(
                  '%Y-%m',
                  campaign_date
                ) AS month,

                SUM(
                  cost
                ) AS cost

              FROM marketing

              WHERE
                campaign_date
                BETWEEN ? AND ?

              GROUP BY month
            )

            SELECT

              revenue.month
                AS label,

              ROUND(

                CASE

                  WHEN
                    spend.cost
                    IS NULL

                    OR
                    spend.cost
                    = 0

                  THEN 0

                  ELSE

                    (
                      revenue.revenue
                      - spend.cost
                    )
                    * 100.0
                    /
                    spend.cost

                END,

                2

              ) AS value

            FROM revenue

            JOIN spend
              ON spend.month
              = revenue.month

            ORDER BY
              revenue.month
          `)
            .all(
              range.start,
              range.end,
              range.start,
              range.end
            );


        sendRows(
          res,
          rows
        );
      }
    )
);


// CROSS:
// Marketing Spend vs Revenue

app.get(
  '/api/marketing/spend-revenue',
  (req, res) =>
    safeRoute(
      res,
      () => {

        const range =
          dashboardRange(
            req
          );


        const rows =
          db.prepare(`
            WITH months AS (

              SELECT
                strftime(
                  '%Y-%m',
                  order_date
                ) AS month

              FROM sales

              WHERE
                order_date
                BETWEEN ? AND ?

              UNION

              SELECT
                strftime(
                  '%Y-%m',
                  campaign_date
                ) AS month

              FROM marketing

              WHERE
                campaign_date
                BETWEEN ? AND ?
            ),

            revenue_data AS (

              SELECT

                strftime(
                  '%Y-%m',
                  order_date
                ) AS month,

                SUM(
                  revenue
                ) AS value

              FROM sales

              WHERE
                order_date
                BETWEEN ? AND ?

              GROUP BY month
            ),

            spend_data AS (

              SELECT

                strftime(
                  '%Y-%m',
                  campaign_date
                ) AS month,

                SUM(
                  cost
                ) AS value

              FROM marketing

              WHERE
                campaign_date
                BETWEEN ? AND ?

              GROUP BY month
            )

            SELECT

              months.month,

              COALESCE(
                revenue_data.value,
                0
              ) AS revenue,

              COALESCE(
                spend_data.value,
                0
              ) AS spend

            FROM months

            LEFT JOIN revenue_data
              USING(month)

            LEFT JOIN spend_data
              USING(month)

            ORDER BY
              months.month
          `)
            .all(
              range.start,
              range.end,

              range.start,
              range.end,

              range.start,
              range.end,

              range.start,
              range.end
            );


        const data = [];


        rows.forEach(
          row => {

            data.push({

              label:
                row.month,

              series:
                'Revenue',

              value:
                Number(
                  row.revenue.toFixed(
                    2
                  )
                )
            });


            data.push({

              label:
                row.month,

              series:
                'Marketing Spend',

              value:
                Number(
                  row.spend.toFixed(
                    2
                  )
                )
            });
          }
        );


        sendRows(
          res,
          data
        );
      }
    )
);


// ============================================================
// CUSTOMERS
// ============================================================


// SOLO:
// New customers by signup month

app.get(
  '/api/customers/new-signups',
  (req, res) =>
    safeRoute(
      res,
      () => {

        const range =
          dashboardRange(
            req
          );


        const rows =
          db.prepare(`
            SELECT

              strftime(
                '%Y-%m',
                signup_date
              ) AS label,

              COUNT(*)
                AS value

            FROM customers

            WHERE
              signup_date
              BETWEEN ? AND ?

            GROUP BY label

            ORDER BY label
          `)
            .all(
              range.start,
              range.end
            );


        sendRows(
          res,
          rows
        );
      }
    )
);


// SOLO:
// Customer breakdown by country

app.get(
  '/api/customers/country-breakdown',
  (req, res) =>
    safeRoute(
      res,
      () => {

        const range =
          dashboardRange(
            req
          );


        const rows =
          db.prepare(`
            SELECT

              COALESCE(
                country,
                'Unknown'
              ) AS label,

              COUNT(*)
                AS value

            FROM customers

            WHERE
              signup_date
              BETWEEN ? AND ?

            GROUP BY country

            ORDER BY value DESC
          `)
            .all(
              range.start,
              range.end
            );


        sendRows(
          res,
          rows
        );
      }
    )
);


// CROSS KPI:
// Customer Retention

app.get(
  '/api/customers/retention',
  (req, res) =>
    safeRoute(
      res,
      () => {

        const range =
          dashboardRange(
            req
          );

        const region =
          String(
            req.query.region
            || 'All Regions'
          );

        const params = [
          range.start,
          range.end
        ];

        let regionSql = '';

        if (
          region
          && region !== 'All Regions'
        ) {

          regionSql =
            'AND s.region = ?';

          params.push(
            region
          );
        }


        const rows =
          db.prepare(`
            WITH active AS (

              SELECT DISTINCT

                strftime(
                  '%Y-%m',
                  s.order_date
                ) AS month,

                s.customer_id

              FROM sales s

              JOIN customers c
                ON c.customer_id
                = s.customer_id

              WHERE
                s.order_date
                BETWEEN ? AND ?

                ${regionSql}
            ),

            months AS (

              SELECT DISTINCT
                month

              FROM active
            )

            SELECT

              m.month,

              (
                SELECT
                  COUNT(*)

                FROM active previous

                WHERE
                  previous.month
                  =
                  strftime(
                    '%Y-%m',
                    date(
                      m.month
                      || '-01',
                      '-1 month'
                    )
                  )
              ) AS prior_count,

              (
                SELECT
                  COUNT(
                    DISTINCT current.customer_id
                  )

                FROM active current

                JOIN active previous
                  ON previous.customer_id
                  = current.customer_id

                WHERE
                  current.month
                  = m.month

                  AND
                  previous.month
                  =
                  strftime(
                    '%Y-%m',
                    date(
                      m.month
                      || '-01',
                      '-1 month'
                    )
                  )
              ) AS returned_count

            FROM months m

            ORDER BY m.month
          `)
            .all(
              ...params
            );


        const data =
          rows.map(
            row => ({

              label:
                row.month,

              value:
                row.prior_count === 0
                  ? 0
                  : Number(
                      (
                        row.returned_count
                        * 100.0
                        / row.prior_count
                      ).toFixed(
                        2
                      )
                    )
            })
          );


        sendRows(
          res,
          data
        );
      }
    )
);


// CROSS:
// Revenue by customer country

app.get(
  '/api/customers/revenue-segment',
  (req, res) =>
    safeRoute(
      res,
      () => {

        const range =
          dashboardRange(
            req
          );

        const region =
          String(
            req.query.region
            || 'All Regions'
          );

        const params = [
          range.start,
          range.end
        ];

        const regionSql =
          regionClause(
            's',
            region,
            params
          );


        const rows =
          db.prepare(`
            SELECT

              COALESCE(
                c.country,
                'Unknown'
              ) AS label,

              ROUND(
                SUM(
                  s.revenue
                ),
                2
              ) AS value

            FROM sales s

            JOIN customers c
              ON c.customer_id
              = s.customer_id

            WHERE
              s.order_date
              BETWEEN ? AND ?

              ${regionSql}

            GROUP BY c.country

            ORDER BY value DESC
          `)
            .all(
              ...params
            );


        sendRows(
          res,
          rows
        );
      }
    )
);


// ============================================================
// EXPERIMENTAL SALES DRILL-DOWN
// ============================================================

app.get(
  '/api/drilldown/sales',
  (req, res) =>
    safeRoute(
      res,
      () => {

        const month =
          String(
            req.query.month || ''
          ).trim();

        const category =
          String(
            req.query.category || ''
          )
            .replace(
              /\s*\[(OVER|UNDER|BALANCED)\]\s*$/,
              ''
            )
            .trim();

        const region =
          String(
            req.query.region || ''
          ).trim();


        const where = [];
        const params = [];


        if (month) {

          where.push(
            "strftime('%Y-%m', s.order_date) = ?"
          );

          params.push(
            month
          );
        }


        if (category) {

          where.push(
            'p.category = ?'
          );

          params.push(
            category
          );
        }


        if (
          region
          && region !== 'All Regions'
        ) {

          where.push(
            's.region = ?'
          );

          params.push(
            region
          );
        }


        const sql = `
          SELECT

            s.order_id,
            s.order_date,
            s.customer_id,
            s.product_id,
            p.category,
            s.quantity,
            s.region,
            s.revenue

          FROM sales s

          JOIN products p
            ON p.product_id
            = s.product_id

          ${
            where.length
              ? 'WHERE '
                + where.join(
                    ' AND '
                  )
              : ''
          }

          ORDER BY
            s.order_date,
            s.order_id

          LIMIT 1000
        `;


        const rows =
          db.prepare(
            sql
          ).all(
            ...params
          );


        res.json({

          success: true,

          columns: [
            'order_id',
            'order_date',
            'customer_id',
            'product_id',
            'category',
            'quantity',
            'region',
            'revenue'
          ],

          data:
            rows
        });
      }
    )
);


// ============================================================
// EXISTING MARKETING VIEW
// ============================================================

app.get(
  '/api/kpis/marketing',
  (req, res) => {

    try {

      const rows =
        db.prepare(
          'SELECT * FROM v_kpi_marketing ORDER BY yr, channel'
        ).all();

      res.json({
        success: true,
        data: rows
      });

    } catch (err) {

      res.status(500).json({
        success: false,
        error: err.message
      });
    }
  }
);


// ============================================================
// EXISTING LOW STOCK ALERT
// ============================================================

app.get(
  '/api/alerts/low-stock',
  (req, res) => {

    try {

      const rows =
        db.prepare(
          'SELECT * FROM v_low_stock_alert ORDER BY stock_level ASC'
        ).all();

      res.json({
        success: true,
        data: rows
      });

    } catch (err) {

      res.status(500).json({
        success: false,
        error: err.message
      });
    }
  }
);


// ============================================================
// KPI REFRESH
// ============================================================

app.post(
  '/api/kpis/refresh',
  (req, res) => {

    try {

      const refreshScript =
        fs.readFileSync(

          path.join(
            __dirname,
            'refresh_kpi_snapshot_sqlite.sql'
          ),

          'utf8'
        );


      db.exec(
        refreshScript
      );


      res.json({

        success: true,

        message:
          'KPI cache successfully updated.'
      });


    } catch (err) {

      res.status(500).json({

        success: false,

        error:
          err.message
      });
    }
  }
);


// ============================================================
// START SERVER
// ============================================================

app.listen(
  3000,
  () => {

    console.log(
      'Backend listening on http://localhost:3000'
    );
  }
);

// For test purposes only - is safe to be deleted or commented out.
// Nothing above this line was changed - this block only reaches into the existing module scope to expose the pure 
// helper functions so an external test file can call the real functions directly, instead of re-implementing/copying 
// their logic into the test.

module.exports.testHelpers = {
  pad2,
  isoDate,
  daysInMonth,
  dashboardRange,
  previousRange,
  regionClause
};