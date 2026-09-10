-- =====================================================================
-- RETAIL DASHBOARD  --  SQLite SCHEMA 

PRAGMA foreign_keys = ON;

-- ---------- SECTION 1 : BASE TABLES (loaded from the 5 CSVs) ----------

CREATE TABLE products (
    product_id   INTEGER PRIMARY KEY,
    category     TEXT    NOT NULL,
    price        REAL    NOT NULL,
    cost         REAL    NOT NULL
);

CREATE TABLE customers (
    customer_id  INTEGER PRIMARY KEY,
    age          INTEGER,
    gender       TEXT,
    country      TEXT,
    signup_date  TEXT      -- ISO date string YYYY-MM-DD
);

CREATE TABLE marketing (
    campaign_id   INTEGER PRIMARY KEY,
    channel       TEXT NOT NULL,
    cost          REAL NOT NULL,
    conversions   INTEGER NOT NULL,
    campaign_date TEXT NOT NULL
);

CREATE TABLE inventory (
    inventory_id  INTEGER PRIMARY KEY,
    product_id    INTEGER NOT NULL,
    stock_level   INTEGER NOT NULL,
    warehouse     TEXT NOT NULL,
    snapshot_date TEXT NOT NULL,
    FOREIGN KEY (product_id) REFERENCES products(product_id)
);

CREATE TABLE sales (
    order_id    INTEGER PRIMARY KEY,
    customer_id INTEGER NOT NULL,
    product_id  INTEGER NOT NULL,
    quantity    INTEGER NOT NULL,
    order_date  TEXT NOT NULL,
    region      TEXT,
    price       REAL NOT NULL,
    revenue     REAL NOT NULL,
    FOREIGN KEY (customer_id) REFERENCES customers(customer_id),
    FOREIGN KEY (product_id)  REFERENCES products(product_id)
);

-- Indexes for dashboard performance
CREATE INDEX ix_sales_date    ON sales(order_date);
CREATE INDEX ix_sales_cust    ON sales(customer_id);
CREATE INDEX ix_sales_prod    ON sales(product_id);
CREATE INDEX ix_mkt_date      ON marketing(campaign_date);
CREATE INDEX ix_inv_prod_date ON inventory(product_id, snapshot_date);


-- ---------- SECTION 2 : SANITY CHECK (run after importing the 5 CSVs) ----------

SELECT 'products'  AS t, COUNT(*) AS n FROM products  UNION ALL
SELECT 'customers',      COUNT(*)      FROM customers UNION ALL
SELECT 'marketing',      COUNT(*)      FROM marketing UNION ALL
SELECT 'inventory',      COUNT(*)      FROM inventory UNION ALL
SELECT 'sales',          COUNT(*)      FROM sales;


-- ---------- SECTION 3 : KPI VIEWS (live, on-demand versions of each KPI) ----------

-- 4.1 Total Revenue & Revenue Growth Rate (Financial)
CREATE VIEW v_kpi_revenue AS
WITH yearly AS (
    SELECT CAST(strftime('%Y', order_date) AS INTEGER) AS yr,
           SUM(revenue) AS total_revenue
    FROM sales
    GROUP BY yr
)
SELECT yr,
       total_revenue,
       ROUND(
         (total_revenue - LAG(total_revenue) OVER (ORDER BY yr)) * 100.0
         / NULLIF(LAG(total_revenue) OVER (ORDER BY yr), 0), 2
       ) AS revenue_growth_pct
FROM yearly;

-- 4.2 Profit & Profit Margin (Financial)
CREATE VIEW v_kpi_profit AS
WITH yr_rev AS (
    SELECT CAST(strftime('%Y', order_date) AS INTEGER) AS yr, SUM(revenue) AS rev
    FROM sales GROUP BY yr
),
yr_cogs AS (
    SELECT CAST(strftime('%Y', s.order_date) AS INTEGER) AS yr,
           SUM(p.cost * s.quantity) AS cogs
    FROM sales s JOIN products p ON p.product_id = s.product_id
    GROUP BY yr
),
yr_mkt AS (
    SELECT CAST(strftime('%Y', campaign_date) AS INTEGER) AS yr, SUM(cost) AS mkt_cost
    FROM marketing GROUP BY yr
)
SELECT r.yr,
       r.rev - c.cogs - COALESCE(m.mkt_cost, 0)                        AS profit,
       ROUND((r.rev - c.cogs) * 100.0 / NULLIF(r.rev, 0), 2)           AS profit_margin_pct
FROM yr_rev r
JOIN yr_cogs c ON c.yr = r.yr
LEFT JOIN yr_mkt m ON m.yr = r.yr;

-- 4.3 Inventory Turnover 
CREATE VIEW v_kpi_inventory_turnover AS
WITH cogs AS (
    SELECT CAST(strftime('%Y', s.order_date) AS INTEGER) AS yr,
           SUM(p.cost * s.quantity) AS cogs
    FROM sales s JOIN products p ON p.product_id = s.product_id
    GROUP BY yr
),
avg_inv AS (
    SELECT CAST(strftime('%Y', i.snapshot_date) AS INTEGER) AS yr,
           AVG(i.stock_level * p.cost) AS avg_inv_value
    FROM inventory i JOIN products p ON p.product_id = i.product_id
    GROUP BY yr
)
SELECT c.yr,
       c.cogs,
       a.avg_inv_value,
       ROUND(c.cogs / NULLIF(a.avg_inv_value, 0), 2) AS inventory_turnover
FROM cogs c JOIN avg_inv a ON a.yr = c.yr;

-- 4.4 Customer Retention Rate (Customer Experience)
CREATE VIEW v_kpi_retention AS
WITH cust_year AS (
    SELECT customer_id, CAST(strftime('%Y', order_date) AS INTEGER) AS yr
    FROM sales GROUP BY customer_id, yr
)
SELECT cy.yr,
       (SELECT COUNT(DISTINCT customer_id) FROM cust_year p2 WHERE p2.yr = cy.yr - 1) AS starting_customers,
       COUNT(DISTINCT CASE WHEN EXISTS (
             SELECT 1 FROM cust_year p WHERE p.customer_id = cy.customer_id AND p.yr = cy.yr - 1
           ) THEN cy.customer_id END) AS repeat_customers,
       ROUND(
         COUNT(DISTINCT CASE WHEN EXISTS (
               SELECT 1 FROM cust_year p WHERE p.customer_id = cy.customer_id AND p.yr = cy.yr - 1
             ) THEN cy.customer_id END) * 100.0
         / NULLIF((SELECT COUNT(DISTINCT customer_id) FROM cust_year p2 WHERE p2.yr = cy.yr - 1), 0), 2
       ) AS retention_rate_pct
FROM cust_year cy
GROUP BY cy.yr;

-- 4.5 Marketing spend / conversions (Marketing)
CREATE VIEW v_kpi_marketing AS
SELECT CAST(strftime('%Y', campaign_date) AS INTEGER) AS yr,
       channel,
       SUM(cost)                                    AS total_cost,
       SUM(conversions)                             AS total_conversions,
       ROUND(SUM(cost) / NULLIF(SUM(conversions), 0), 2) AS cost_per_conversion
FROM marketing
GROUP BY yr, channel;

-- 4.6 Low-stock alert 
CREATE VIEW v_low_stock_alert AS
SELECT i.product_id, p.category, i.warehouse, i.stock_level
FROM inventory i
JOIN products p ON p.product_id = i.product_id
WHERE i.snapshot_date = (SELECT MAX(snapshot_date) FROM inventory)
  AND i.stock_level < 50;
