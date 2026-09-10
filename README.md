# Dynamic Data Dashboard
 
An offline desktop dashboard for retail KPI monitoring.
It runs entirely locally, offline and on one machine using Java Swing client, local Node.js API service, and SQLite database(s). 
 
---
 
## Requirements
 
- Java 21+
- Node.js 18+
- Maven

---

## Setup

1. Install backend dependencies (once the repository is cloned):

       cd src/main/java/dashboard/database
       npm install

2. Start the backend and leave it running:

       npm start

   Listens on http://localhost:3000

3. In a second terminal, run the client:

       mvn clean compile exec:java -Dexec.mainClass="dashboard.Main"

The backend must be running before the client starts - the client reads the database schema from it on launch.

## KPIs

Total Revenue, Revenue Growth Rate, Profit, Profit Margin,
Inventory Turnover, Customer Retention, Cost per Conversion.

Definitions and formulas: see KPI Calculation Documentation.