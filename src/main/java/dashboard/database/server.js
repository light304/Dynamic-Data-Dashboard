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
// ALERTS + THRESHOLDS SETUP
// FR5 - Persistent alert detection with adjustable thresholds
// ============================================================

db.exec(`
  CREATE TABLE IF NOT EXISTS alert_thresholds (
      threshold_key TEXT PRIMARY KEY,
      display_name TEXT NOT NULL,
      threshold_value REAL NOT NULL,
      unit TEXT NOT NULL
  );

  CREATE TABLE IF NOT EXISTS alerts (
      alert_id INTEGER PRIMARY KEY AUTOINCREMENT,
      alert_type TEXT NOT NULL,
      entity_key TEXT NOT NULL,
      title TEXT NOT NULL,
      message TEXT NOT NULL,
      current_value REAL,
      threshold_value REAL,
      severity TEXT NOT NULL DEFAULT 'WARNING',
      status TEXT NOT NULL DEFAULT 'ACTIVE',
      created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
      resolved_at TEXT
  );
`);


// ------------------------------------------------------------
// Default thresholds
// INSERT OR IGNORE means saved user values are NOT overwritten
// when the backend restarts.
// ------------------------------------------------------------

const insertDefaultThreshold = db.prepare(`
  INSERT OR IGNORE INTO alert_thresholds
  (
      threshold_key,
      display_name,
      threshold_value,
      unit
  )
  VALUES (?, ?, ?, ?)
`);

insertDefaultThreshold.run(
  'LOW_STOCK_PERCENT',
  'Low Stock',
  10,
  '% of average sales'
);

insertDefaultThreshold.run(
  'REVENUE_DROP_PERCENT',
  'Revenue Drop',
  10,
  '%'
);

insertDefaultThreshold.run(
  'PROFIT_DROP_PERCENT',
  'Profit Drop',
  10,
  '%'
);

console.log('Alert tables and default thresholds ready.');

// CSV parsing
const { parse } = require('csv-parse/sync');

// Maps each expected filename to its table, columns, and validation rules.
// `columns` maps CSV header -> database column (the two date renames live here).
const DATASETS = {

  products: {
    table: 'products',
    pk: 'product_id',
    columns: {
      product_id: 'product_id',
      category: 'category',
      price: 'price',
      cost: 'cost'
    },
    numeric: ['product_id', 'price', 'cost'],
    required: ['product_id'],
    dates: [],
    parents: []
  },

  customers: {
    table: 'customers',
    pk: 'customer_id',
    columns: {
      customer_id: 'customer_id',
      age: 'age',
      gender: 'gender',
      country: 'country',
      signup_date: 'signup_date'
    },
    numeric: ['customer_id', 'age'],
    required: ['customer_id'],
    dates: ['signup_date'],
    parents: []
  },

  marketing: {
    table: 'marketing',
    pk: 'campaign_id',
    columns: {
      campaign_id: 'campaign_id',
      channel: 'channel',
      cost: 'cost',
      conversions: 'conversions',
      date: 'campaign_date'
    },
    numeric: ['campaign_id', 'cost', 'conversions'],
    required: ['campaign_id'],
    dates: ['date'],
    parents: []
  },

  inventory: {
    table: 'inventory',
    pk: 'inventory_id',
    columns: {
      inventory_id: 'inventory_id',
      product_id: 'product_id',
      stock_level: 'stock_level',
      warehouse: 'warehouse',
      date: 'snapshot_date'
    },
    numeric: ['inventory_id', 'product_id', 'stock_level'],
    required: ['inventory_id', 'product_id'],
    dates: ['date'],
    parents: [
      {
        column: 'product_id',
        table: 'products',
        key: 'product_id'
      }
    ]
  },

  sales: {
    table: 'sales',
    pk: 'order_id',
    columns: {
      order_id: 'order_id',
      customer_id: 'customer_id',
      product_id: 'product_id',
      quantity: 'quantity',
      order_date: 'order_date',
      region: 'region',
      price: 'price',
      revenue: 'revenue'
    },
    numeric: [
      'order_id',
      'customer_id',
      'product_id',
      'quantity',
      'price',
      'revenue'
    ],
    required: [
      'order_id',
      'customer_id',
      'product_id'
    ],
    dates: ['order_date'],
    parents: [
      {
        column: 'customer_id',
        table: 'customers',
        key: 'customer_id'
      },
      {
        column: 'product_id',
        table: 'products',
        key: 'product_id'
      }
    ]
  }
};


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
            ? null
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
        // MARKETING COST
        // -----------------------------

        const marketingCostRow =
          db.prepare(`
            SELECT COALESCE(SUM(cost), 0) AS cost
            FROM marketing
            WHERE campaign_date BETWEEN ? AND ?
          `).get(
            range.start,
            range.end
          );

        const isWholeBusiness =
          region === 'All Regions';

        const netProfit =
          isWholeBusiness
            ? profitRow.profit
              - marketingCostRow.cost
            : profitRow.profit;


        // -----------------------------
        // INVENTORY TURNOVER
        // -----------------------------

        const cogsParams = [
          range.start,
          range.end
        ];

        const cogsRegion = '';

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
                SUM(
                  avg_stock
                  * cost
                ),
                0
              ) AS value

            FROM (

              SELECT
                i.product_id,

                AVG(
                  i.stock_level
                ) AS avg_stock,

                p.cost

              FROM inventory i

              JOIN products p
                ON p.product_id
                = i.product_id

              WHERE
                i.snapshot_date
                BETWEEN ? AND ?

              GROUP BY
                i.product_id,
                p.cost
            )
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
        // -----------------------------

        const existingBase =
          db.prepare(`
            SELECT
              COUNT(*) AS value

            FROM customers

            WHERE
              signup_date < ?
          `)
            .get(
              range.start
            )
            .value;


        const retainedParams = [
          range.start,
          range.start,
          range.end
        ];

        const retainedRegion = '';

        const retained =
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
              c.signup_date < ?

              AND s.order_date
              BETWEEN ? AND ?

              ${retainedRegion}
          `)
            .get(
              ...retainedParams
            )
            .value;


        const retention =
          existingBase === 0
            ? 0
            : retained
              * 100.0
              / existingBase;


        // -----------------------------
        // COST PER CONVERSION
        // -----------------------------

        const conversions =
          db.prepare(`
            SELECT
              COALESCE(
                SUM(conversions),
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


        const costPerConversion =
          conversions === 0
            ? 0
            : marketingCostRow.cost
              / conversions;


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
              growth === null
                ? null
                : Number(
                    growth.toFixed(
                      2
                    )
                  ),

            profit:
              Number(
                netProfit.toFixed(
                  2
                )
              ),

            profit_includes_marketing:
              isWholeBusiness,

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

            inventory_turnover_region_ignored:
              !isWholeBusiness,

            customer_retention_pct:
              Number(
                retention.toFixed(
                  2
                )
              ),

            cost_per_conversion:
              Number(
                costPerConversion.toFixed(
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

        // No region filter for COGS because inventory has no region column,
        // so turnover is always whole-business
        let regionSql = '';

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

                month,

                SUM(
                  avg_stock
                  * cost
                ) AS inv_value

              FROM (

                SELECT

                  strftime(
                    '%Y-%m',
                    i.snapshot_date
                  ) AS month,

                  i.product_id,

                  AVG(
                    i.stock_level
                  ) AS avg_stock,

                  p.cost

                FROM inventory i

                JOIN products p
                  ON p.product_id
                  = i.product_id

                WHERE
                  i.snapshot_date
                  BETWEEN ? AND ?

                GROUP BY
                  month,
                  i.product_id,
                  p.cost
              )

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


// CROSS KPI:
// Stock cover in weeks
// inventory + sales + products
//
// How long current stock would last at the current sales rate.
// Uses average stock across the period, so this is a typical
// holding position rather than a live one.

app.get(
  '/api/inventory/stock-cover-weeks',
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

        const weeks =
          Math.max(
            1,
            (
              new Date(range.end)
              - new Date(range.start)
            )
            / 604800000
          );

        const params = [
          range.start,
          range.end,
          range.start,
          range.end
        ];

        // Inventory has no region column, so stock cover is whole-business only
        let regionSql = '';

        params.push(
          weeks
        );

        const rows =
          db.prepare(`
            WITH stock AS (

              SELECT

                category,

                SUM(
                  avg_stock
                ) AS avg_stock

              FROM (

                SELECT

                  p.category,

                  i.product_id,

                  AVG(
                    i.stock_level
                  ) AS avg_stock

                FROM inventory i

                JOIN products p
                  ON p.product_id
                  = i.product_id

                WHERE
                  i.snapshot_date
                  BETWEEN ? AND ?

                GROUP BY
                  p.category,
                  i.product_id
              )

              GROUP BY category
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
            )

            SELECT

              stock.category
                AS label,

              ROUND(
                stock.avg_stock
                /
                NULLIF(
                  sold.sold / ?,
                  0
                ),
                1
              ) AS value

            FROM stock

            JOIN sold
              USING(category)

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
// Whole-business Cost per Conversion over time

app.get(
  '/api/marketing/cost-per-conversion-trend',
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
                campaign_date
              ) AS label,

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
// SALES DRILL-DOWN
// ============================================================
//
// Opens the detailed sales records behind a chart.
//
// Supports:
// - month
// - week
// - category
// - region
//
// Example:
// /api/drilldown/sales?month=2023-01&week=Week%202
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

        const week =
          String(
            req.query.week || ''
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


        // MONTH

        if (month) {

          where.push(
            "strftime('%Y-%m', s.order_date) = ?"
          );

          params.push(
            month
          );
        }


        // WEEK
        //
        // Week 1 = days 1-7
        // Week 2 = days 8-14
        // Week 3 = days 15-21
        // Week 4 = days 22-28
        // Week 5 = days 29-31

        if (week) {

          let startDay = null;
          let endDay = null;

          switch (week) {

            case 'Week 1':
              startDay = 1;
              endDay = 7;
              break;

            case 'Week 2':
              startDay = 8;
              endDay = 14;
              break;

            case 'Week 3':
              startDay = 15;
              endDay = 21;
              break;

            case 'Week 4':
              startDay = 22;
              endDay = 28;
              break;

            case 'Week 5':
              startDay = 29;
              endDay = 31;
              break;
          }


          if (
            startDay !== null
            && endDay !== null
          ) {

            where.push(`
              CAST(
                strftime(
                  '%d',
                  s.order_date
                )
                AS INTEGER
              )
              BETWEEN ? AND ?
            `);

            params.push(
              startDay,
              endDay
            );
          }
        }


        // CATEGORY

        if (category) {

          where.push(
            'p.category = ?'
          );

          params.push(
            category
          );
        }


        // REGION

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


        // QUERY

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


        // Keep the original response structure because
        // AnalyticsApi.drilldownSales() expects columns
        // at the top level and rows inside "data".

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

          data: rows
        });
      }
    )
);
// ============================================================
// INVENTORY DRILL-DOWN
// ============================================================

app.get(
  '/api/drilldown/inventory',
  (req, res) =>
    safeRoute(
      res,
      () => {

        const warehouse =
          String(
            req.query.warehouse || ''
          ).trim();

        const range =
          dashboardRange(req);

        const where = [
          'date(i.snapshot_date) BETWEEN date(?) AND date(?)'
        ];

        const params = [
          range.start,
          range.end
        ];

        if (warehouse) {

          where.push(
            'i.warehouse = ?'
          );

          params.push(
            warehouse
          );
        }

        const rows =
          db.prepare(`
            SELECT
              i.inventory_id,
              i.product_id,
              p.category,
              i.stock_level,
              i.warehouse,
              i.snapshot_date

            FROM inventory i

            LEFT JOIN products p
              ON p.product_id = i.product_id

            WHERE
              ${where.join(' AND ')}

            ORDER BY
              date(i.snapshot_date) DESC,
              i.stock_level ASC

            LIMIT 1000
          `).all(...params);

        res.json({
          success: true,

          columns: [
            'inventory_id',
            'product_id',
            'category',
            'stock_level',
            'warehouse',
            'snapshot_date'
          ],

          data: rows
        });
      }
    )
);



// ============================================================
// ALERT DETECTION
// ============================================================

function getThreshold(key) {

  const row = db.prepare(`
      SELECT threshold_value
      FROM alert_thresholds
      WHERE threshold_key = ?
  `).get(key);

  return row
      ? Number(row.threshold_value)
      : 0;
}


function createOrUpdateAlert({
  type,
  entityKey,
  title,
  message,
  currentValue,
  thresholdValue,
  severity = 'WARNING'
}) {

  const existing = db.prepare(`
      SELECT alert_id
      FROM alerts
      WHERE alert_type = ?
        AND entity_key = ?
        AND status = 'ACTIVE'
      LIMIT 1
  `).get(
      type,
      entityKey
  );


  if (existing) {

      db.prepare(`
          UPDATE alerts
          SET
              title = ?,
              message = ?,
              current_value = ?,
              threshold_value = ?,
              severity = ?
          WHERE alert_id = ?
      `).run(
          title,
          message,
          currentValue,
          thresholdValue,
          severity,
          existing.alert_id
      );

  } else {

      db.prepare(`
          INSERT INTO alerts
          (
              alert_type,
              entity_key,
              title,
              message,
              current_value,
              threshold_value,
              severity,
              status
          )
          VALUES (?, ?, ?, ?, ?, ?, ?, 'ACTIVE')
      `).run(
          type,
          entityKey,
          title,
          message,
          currentValue,
          thresholdValue,
          severity
      );
  }
}


function resolveMissingAlerts(
  type,
  activeEntityKeys
) {

  const existing =
      db.prepare(`
          SELECT
              alert_id,
              entity_key
          FROM alerts
          WHERE alert_type = ?
            AND status = 'ACTIVE'
      `).all(type);


  const activeSet =
      new Set(
          activeEntityKeys.map(String)
      );


  const resolve =
      db.prepare(`
          UPDATE alerts
          SET
              status = 'RESOLVED',
              resolved_at = CURRENT_TIMESTAMP
          WHERE alert_id = ?
      `);


  for (const alert of existing) {

      if (
          !activeSet.has(
              String(alert.entity_key)
          )
      ) {

          resolve.run(
              alert.alert_id
          );
      }
  }
}


function detectLowStockAlerts() {

  const thresholdPercent =
      getThreshold(
          'LOW_STOCK_PERCENT'
      );


  /*
   * Average quantity sold per product is used as the
   * sales baseline.
   *
   * An alert is generated when the latest stock level
   * is below the configured percentage of that baseline.
   */

  const rows =
      db.prepare(`
          WITH sales_average AS (

              SELECT
                  product_id,
                  AVG(quantity) AS average_sales

              FROM sales

              GROUP BY product_id
          ),

          latest_inventory_date AS (

              SELECT
                  product_id,
                  MAX(snapshot_date) AS latest_date

              FROM inventory

              GROUP BY product_id
          )

          SELECT
              i.product_id,
              p.category,
              i.warehouse,
              i.stock_level,
              COALESCE(sa.average_sales, 0) AS average_sales

          FROM inventory i

          JOIN latest_inventory_date latest
              ON latest.product_id = i.product_id
             AND latest.latest_date = i.snapshot_date

          JOIN products p
              ON p.product_id = i.product_id

          LEFT JOIN sales_average sa
              ON sa.product_id = i.product_id

          ORDER BY i.stock_level ASC
      `).all();


  const activeKeys = [];


  for (const row of rows) {

      if (row.average_sales <= 0) {
          continue;
      }


      const triggerLevel =
          row.average_sales
          * (
              thresholdPercent / 100.0
          );


      if (
          Number(row.stock_level)
          < triggerLevel
      ) {

          const entityKey =
              `${row.product_id}:${row.warehouse}`;

          activeKeys.push(
              entityKey
          );


          createOrUpdateAlert({

              type:
                  'LOW_STOCK',

              entityKey,

              title:
                  `Low Stock - Product ${row.product_id}`,

              message:
                  `${row.category} in ${row.warehouse} has `
                  + `${row.stock_level} units remaining.`,

              currentValue:
                  Number(row.stock_level),

              thresholdValue:
                  Number(
                      triggerLevel.toFixed(2)
                  ),

              severity:
                  'WARNING'
          });
      }
  }


  resolveMissingAlerts(
      'LOW_STOCK',
      activeKeys
  );
}

function detectPerformanceAlerts() {

  const revenueThreshold =
      getThreshold(
          'REVENUE_DROP_PERCENT'
      );

  const profitThreshold =
      getThreshold(
          'PROFIT_DROP_PERCENT'
      );


  /*
   * Find the latest two months available in sales.
   */

  const months =
      db.prepare(`
          SELECT DISTINCT
              strftime(
                  '%Y-%m',
                  order_date
              ) AS month

          FROM sales

          WHERE order_date IS NOT NULL

          ORDER BY month DESC

          LIMIT 2
      `).all();


  if (months.length < 2) {

      resolveMissingAlerts(
          'REVENUE_DROP',
          []
      );

      resolveMissingAlerts(
          'PROFIT_DROP',
          []
      );

      return;
  }


  const currentMonth =
      months[0].month;

  const previousMonth =
      months[1].month;


  const performance =
      db.prepare(`
          SELECT

              strftime(
                  '%Y-%m',
                  s.order_date
              ) AS month,

              COALESCE(
                  SUM(s.revenue),
                  0
              ) AS revenue,

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
              ) AS profit

          FROM sales s

          JOIN products p
              ON p.product_id
              = s.product_id

          WHERE
              strftime(
                  '%Y-%m',
                  s.order_date
              ) IN (?, ?)

          GROUP BY month
      `).all(
          currentMonth,
          previousMonth
      );


  const current =
      performance.find(
          row =>
              row.month === currentMonth
      );

  const previous =
      performance.find(
          row =>
              row.month === previousMonth
      );


  if (!current || !previous) {
      return;
  }


  // ========================================================
  // REVENUE DROP
  // ========================================================

  const revenueActive = [];


  if (
      Number(previous.revenue) > 0
  ) {

      const revenueDrop =
          (
              (
                  Number(previous.revenue)
                  -
                  Number(current.revenue)
              )
              /
              Number(previous.revenue)
          )
          * 100;


      if (
          revenueDrop >= revenueThreshold
      ) {

          revenueActive.push(
              'BUSINESS'
          );


          createOrUpdateAlert({

              type:
                  'REVENUE_DROP',

              entityKey:
                  'BUSINESS',

              title:
                  'Revenue Drop',

              message:
                  `Revenue decreased by `
                  + `${revenueDrop.toFixed(2)}% `
                  + `from ${previousMonth} `
                  + `to ${currentMonth}.`,

              currentValue:
                  Number(
                      revenueDrop.toFixed(2)
                  ),

              thresholdValue:
                  revenueThreshold,

              severity:
                  'CRITICAL'
          });
      }
  }


  resolveMissingAlerts(
      'REVENUE_DROP',
      revenueActive
  );


  // ========================================================
  // PROFIT DROP
  // ========================================================

  const profitActive = [];


  if (
      Number(previous.profit) > 0
  ) {

      const profitDrop =
          (
              (
                  Number(previous.profit)
                  -
                  Number(current.profit)
              )
              /
              Number(previous.profit)
          )
          * 100;


      if (
          profitDrop >= profitThreshold
      ) {

          profitActive.push(
              'BUSINESS'
          );


          createOrUpdateAlert({

              type:
                  'PROFIT_DROP',

              entityKey:
                  'BUSINESS',

              title:
                  'Profit Drop',

              message:
                  `Profit decreased by `
                  + `${profitDrop.toFixed(2)}% `
                  + `from ${previousMonth} `
                  + `to ${currentMonth}.`,

              currentValue:
                  Number(
                      profitDrop.toFixed(2)
                  ),

              thresholdValue:
                  profitThreshold,

              severity:
                  'CRITICAL'
          });
      }
  }


  resolveMissingAlerts(
      'PROFIT_DROP',
      profitActive
  );
}


function runAlertDetection() {

  const transaction =
      db.transaction(() => {

          detectLowStockAlerts();

          detectPerformanceAlerts();
      });


  transaction();
}

// ============================================================
// ALERT API
// ============================================================


// ------------------------------------------------------------
// Get saved thresholds
// ------------------------------------------------------------

app.get(
  '/api/alerts/thresholds',
  (req, res) =>
      safeRoute(
          res,
          () => {

              const rows =
                  db.prepare(`
                      SELECT
                          threshold_key,
                          display_name,
                          threshold_value,
                          unit

                      FROM alert_thresholds

                      ORDER BY display_name
                  `).all();


              res.json({
                  success: true,
                  data: rows
              });
          }
      )
);


// ------------------------------------------------------------
// Update threshold
// ------------------------------------------------------------

app.post(
  '/api/alerts/thresholds',
  (req, res) =>
      safeRoute(
          res,
          () => {

              const {
                  threshold_key,
                  threshold_value
              } = req.body;


              const allowed = new Set([
                  'LOW_STOCK_PERCENT',
                  'REVENUE_DROP_PERCENT',
                  'PROFIT_DROP_PERCENT'
              ]);


              if (
                  !allowed.has(
                      threshold_key
                  )
              ) {

                  return res.status(400).json({
                      success: false,
                      error:
                          'Unknown alert threshold.'
                  });
              }


              const value =
                  Number(
                      threshold_value
                  );


              if (
                  !Number.isFinite(value)
                  || value < 0
                  || value > 100
              ) {

                  return res.status(400).json({
                      success: false,
                      error:
                          'Threshold must be between 0 and 100.'
                  });
              }


              db.prepare(`
                  UPDATE alert_thresholds

                  SET threshold_value = ?

                  WHERE threshold_key = ?
              `).run(
                  value,
                  threshold_key
              );


              // Immediately re-check alerts using
              // the new user-defined threshold.
              runAlertDetection();


              res.json({
                  success: true
              });
          }
      )
);


// ------------------------------------------------------------
// Run detection + return active alerts
// ------------------------------------------------------------

app.get(
  '/api/alerts/active',
  (req, res) =>
      safeRoute(
          res,
          () => {

              runAlertDetection();


              const rows =
                  db.prepare(`
                      SELECT
                          alert_id,
                          alert_type,
                          entity_key,
                          title,
                          message,
                          current_value,
                          threshold_value,
                          severity,
                          status,
                          created_at

                      FROM alerts

                      WHERE status = 'ACTIVE'

                      ORDER BY
                          CASE severity
                              WHEN 'CRITICAL' THEN 1
                              WHEN 'WARNING' THEN 2
                              ELSE 3
                          END,
                          created_at DESC
                  `).all();


              res.json({
                  success: true,
                  data: rows
              });
          }
      )
);


// ------------------------------------------------------------
// Manually run alert detection
// ------------------------------------------------------------

app.post(
  '/api/alerts/detect',
  (req, res) =>
      safeRoute(
          res,
          () => {

              runAlertDetection();


              const count =
                  db.prepare(`
                      SELECT COUNT(*) AS count
                      FROM alerts
                      WHERE status = 'ACTIVE'
                  `).get().count;


              res.json({
                  success: true,
                  active_alerts: count
              });
          }
      )
);
// ============================================================
// MARKETING DRILL-DOWN
// ============================================================

app.get(
  '/api/drilldown/marketing',
  (req, res) =>
    safeRoute(
      res,
      () => {

        const channel =
          String(
            req.query.channel || ''
          ).trim();

        const range =
          dashboardRange(req);

        const where = [
          'date(m.campaign_date) BETWEEN date(?) AND date(?)'
        ];

        const params = [
          range.start,
          range.end
        ];

        if (channel) {

          where.push(
            'm.channel = ?'
          );

          params.push(
            channel
          );
        }

        const rows =
          db.prepare(`
            SELECT
              m.campaign_id,
              m.channel,
              m.cost,
              m.conversions,
              m.campaign_date

            FROM marketing m

            WHERE
              ${where.join(' AND ')}

            ORDER BY
              date(m.campaign_date) DESC,
              m.cost DESC

            LIMIT 1000
          `).all(...params);

        res.json({
          success: true,

          columns: [
            'campaign_id',
            'channel',
            'cost',
            'conversions',
            'campaign_date'
          ],

          data: rows
        });
      }
    )
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
// CSV UPLOAD
// ============================================================

function uploadError(res, problem, fix) {

  return res.status(400).json({
    success: false,
    error: problem + (fix ? '\n\n' + fix : '')
  });
}


app.post(
  '/api/upload',
  (req, res) =>
    safeRoute(
      res,
      () => {

        const filePath =
          String(
            req.body.path || ''
          ).trim();


        if (
          !filePath
          || !fs.existsSync(
            filePath
          )
        ) {

          return uploadError(
            res,
            'File not found: ' + filePath
          );
        }


        // Dataset identified by filename

        const fileName =
          path.basename(
            filePath
          ).toLowerCase();

        const key =
          Object.keys(
            DATASETS
          )
            .find(
              k =>
                fileName
                === k + '.csv'
            );


        if (!key) {

          return uploadError(
            res,

            '"' + fileName
            + '" is not a recognised data file.\n',

            'Ensure it fits the type (and naming) of one of: '
            + Object.keys(
                DATASETS
              )
                .map(
                  k =>
                    k + '.csv'
                )
                .join(', ')
          );
        }


        const spec =
          DATASETS[key];


        // Parse

        let rows;

        try {

          rows =
            parse(
              fs.readFileSync(
                filePath,
                'utf8'
              ),
              {
                columns: true,
                skip_empty_lines: true,
                trim: true,
                bom: true
              }
            );

        } catch (err) {

          return uploadError(
            res,

            'Could not read '
            + path.basename(
                filePath
              )
            + ': '
            + err.message,

            '\nEvery row must have the same number of commas as the header row. '
            + '\nIf a value contains a comma, wrap it in double quotes, like "Home, Garden"'
          );
        }


        if (
          rows.length === 0
        ) {

          return uploadError(
            res,
            'File is empty (contains no data rows).'
          );
        }


        // Header check

        const expected =
          Object.keys(
            spec.columns
          );

        const actual =
          Object.keys(
            rows[0]
          );

        const missing =
          expected.filter(
            c =>
              !actual.includes(
                c
              )
          );


        if (
          missing.length > 0
        ) {

          return uploadError(
            res,

            'Missing column(s): '
            + missing.join(
                ', '
              ),

            '\nExpected header: '
            + expected.join(
                ', '
              )
          );
        }


        // Statement for upserting into this table

        const dbCols =
          expected.map(
            c =>
              spec.columns[c]
          );

        const updates =
          dbCols
            .filter(
              c =>
                c !== spec.pk
            )
            .map(
              c =>
                `"${c}" = excluded."${c}"`
            )
            .join(', ');


        const insert =
          db.prepare(`
            INSERT INTO "${spec.table}"
            (
              ${dbCols
                .map(
                  c =>
                    `"${c}"`
                )
                .join(', ')
              }
            )

            VALUES
            (
              ${dbCols
                .map(
                  () => '?'
                )
                .join(', ')
              }
            )

            ON CONFLICT("${spec.pk}")
            DO UPDATE SET
              ${updates}
          `);


        // Parent lookups for foreign key checks

        const parentChecks =
          spec.parents.map(
            p => ({

              column:
                p.column,

              table:
                p.table,

              stmt:
                db.prepare(
                  `SELECT 1 AS ok
                   FROM "${p.table}"
                   WHERE "${p.key}" = ?`
                )
            })
          );


        const rejected = [];
        let inserted = 0;


        const loadAll =
          db.transaction(
            () => {

              rows.forEach(
                (row, index) => {

                  // +1 for header
                  // +1 for 1-based line numbers

                  const lineNumber =
                    index + 2;


                  // Empty row

                  if (
                    expected.every(
                      c =>
                        String(
                          row[c] ?? ''
                        ).trim() === ''
                    )
                  ) {

                    rejected.push({
                      line: lineNumber,
                      reason: 'Row is empty'
                    });

                    return;
                  }


                  // Required fields

                  const blank =
                    spec.required
                      .find(
                        c =>
                          String(
                            row[c] ?? ''
                          ).trim() === ''
                      );


                  if (blank) {

                    rejected.push({
                      line: lineNumber,
                      reason:
                        'Missing required field: '
                        + blank
                    });

                    return;
                  }


                  // Numeric fields must parse

                  const values = [];
                  let badNumber = null;


                  for (
                    const csvCol
                    of expected
                  ) {

                    const raw =
                      row[csvCol];


                    if (
                      spec.numeric.includes(
                        csvCol
                      )
                    ) {

                      const n =
                        Number(
                          raw
                        );


                      if (
                        raw === ''
                        || raw === null
                        || Number.isNaN(
                          n
                        )
                      ) {

                        badNumber =
                          csvCol;

                        break;
                      }


                      values.push(
                        n
                      );

                    } else {

                      values.push(
                        raw === ''
                          ? null
                          : raw
                      );
                    }
                  }


                  if (badNumber) {

                    rejected.push({
                      line: lineNumber,
                      reason:
                        'Not a number: '
                        + badNumber
                    });

                    return;
                  }


                  // Date fields must look like YYYY-MM-DD

                  const badDate =
                    (
                      spec.dates || []
                    )
                      .find(
                        c =>
                          !/^\d{4}-\d{2}-\d{2}$/
                            .test(
                              String(
                                row[c] ?? ''
                              ).trim()
                            )
                      );


                  if (badDate) {

                    rejected.push({
                      line: lineNumber,

                      reason:
                        'Date must be YYYY-MM-DD: '
                        + badDate
                    });

                    return;
                  }


                  // Foreign keys must exist

                  let missingParent =
                    null;


                  for (
                    const check
                    of parentChecks
                  ) {

                    const value =
                      Number(
                        row[
                          check.column
                        ]
                      );


                    if (
                      !check.stmt.get(
                        value
                      )
                    ) {

                      missingParent =
                        `${check.column} ${value} not found in ${check.table}`;

                      break;
                    }
                  }


                  if (
                    missingParent
                  ) {

                    rejected.push({
                      line: lineNumber,
                      reason: missingParent
                    });

                    return;
                  }


                  insert.run(
                    ...values
                  );

                  inserted++;
                }
              );
            }
          );


        loadAll();


        if (
          rejected.length > 0
        ) {

          console.log(
            'Rejected rows in '
            + fileName
            + ':'
          );

          rejected
            .slice(
              0,
              20
            )
            .forEach(
              r =>
                console.log(
                  '  line '
                  + r.line
                  + ': '
                  + r.reason
                )
            );
        }


        res.json({

          success: true,

          dataset:
            key,

          table:
            spec.table,

          total_rows:
            rows.length,

          loaded:
            inserted,

          rejected:
            rejected.length,

          // Cap detail so a badly broken
          // file doesn't return 50,000 messages

          rejected_detail:
            rejected.slice(
              0,
              20
            )
        });
      }
    )
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