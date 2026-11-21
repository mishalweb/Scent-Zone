# SCENT ZONE

**Luxury Perfume Store Management System** — Java 21 • JavaFX 21 • MySQL • JDBC • MVC

A black-and-gold desktop perfume store for a university OOP/database project. Includes **Admin** and **Customer** dashboards, sample fragrance catalog, login/registration, inventory control, shopping cart, transactional checkout, and order history.

## What is implemented?

| Role | Features |
| --- | --- |
| Customer | Register, log in, search/filter products, choose quantity, add/remove/clear cart, place an order, view own orders and order items |
| Admin | Log in, overview cards, browse products, create/edit/hide/unhide perfumes, add brands, view all orders, change status, view order items |
| Shared | Passwords hashed with PBKDF2-HMAC-SHA256 (per-user salts), prepared SQL statements, MySQL JDBC, FXML + CSS, database transactions for checkout |

**Important scope:** This is a local educational desktop project, not a production e-commerce system. It **does not charge real money**, send shipping requests, upload actual product images, or provide a web storefront. The application connects directly to a local MySQL database; production deployments should use a protected backend API instead of distributing database credentials with desktop clients.

## 1. Requirements (Windows + VS Code)

- **JDK 21** (you can keep other JDKs installed). Confirm `java -version` and `javac -version` show 21.
- **Apache Maven** (3.9+ is a good choice). Confirm `mvn -version` shows Maven and Java 21.
- **VS Code** with **Extension Pack for Java** and **Maven for Java**.
- **MySQL Server 8.0+** and optionally **MySQL Workbench**.
- Internet connection **for the first Maven run** to download JavaFX and MySQL Connector/J libraries.

JavaFX is supplied by Maven. **Do not manually install the JavaFX SDK** for this project.

### Your Java 21 path (example from Windows)

```powershell
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-21.0.13.10002-hotspot'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
java -version
javac -version
mvn -version
```

The `$env:` changes above affect only that terminal session. If needed, permanently set `JAVA_HOME` and move `%JAVA_HOME%\bin` above Oracle Java entries in Windows Environment Variables, then restart VS Code.

**If `mvn` is not recognized:** install Apache Maven from https://maven.apache.org/download.cgi , unzip it, and add its `bin` directory to Windows `Path`. Restart VS Code and try `mvn -version`.

## 2. Open the project in VS Code

1. Extract `Scent-Zone.zip` (keep the folder structure).
2. In VS Code click **File → Open Folder** and choose **Scent-Zone** (the folder containing `pom.xml`).
3. Install recommended Java extensions if prompted.
4. Open **Terminal → New Terminal** from that folder.

## 3. Create the MySQL database

1. Start the **MySQL Server** Windows service (typically `MySQL80`).
2. Open **MySQL Workbench** and connect using your MySQL admin credentials.
3. Open `database/scent_zone.sql` and execute the **entire script** (lightning-bolt icon).
4. Verify with:

```sql
USE scent_zone_db;
SHOW TABLES;
SELECT perfume_name, price, stock_quantity FROM perfumes;
```

The schema creates five tables: `users`, `brands`, `perfumes`, `orders`, and `order_items`. It inserts six sample perfumes with **illustrative BDT prices**. Re-running the script is safe for the sample rows because they use `INSERT IGNORE` and fixed IDs.

### Optional, recommended: a dedicated MySQL user

Run this in MySQL Workbench while connected as an account with permission to create users:

```sql
CREATE USER IF NOT EXISTS 'scent_user'@'localhost' IDENTIFIED BY 'REPLACE_WITH_A_STRONG_PASSWORD';
GRANT SELECT, INSERT, UPDATE, DELETE ON scent_zone_db.* TO 'scent_user'@'localhost';
```

Change the example password! Alternatively use your existing local MySQL username/password.

## 4. Configure database credentials (PowerShell)

Open a **new PowerShell terminal in VS Code** and run:

```powershell
$env:SCENT_DB_USER = 'scent_user'  # or your MySQL username, e.g. root
$env:SCENT_DB_PASSWORD = 'YOUR_MYSQL_PASSWORD'
```

You may optionally set a different database connection URL:

```powershell
$env:SCENT_DB_URL = 'jdbc:mysql://localhost:3306/scent_zone_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC'
```

These environment variables are **only stored in the current terminal process**. Run both Maven commands below **from this same terminal**. Avoid saving passwords into Git, Java files, or `.vscode/settings.json`.

The default JDBC URL disables TLS and enables public key retrieval for **local-only development**. Do not use that configuration for a remote or production database.

## 5. Create your first admin (one time)

Public registration creates **CUSTOMER** accounts only. No known/default admin password is embedded in the project.

From the same PowerShell terminal (where the environment variables are set):

```powershell
mvn clean compile
mvn -q exec:java -Dexec.mainClass=com.scentzone.tools.AdminSetup
```

Enter an admin full name, email, and a password of at least 8 characters. Depending on the terminal, the password input may be visible (the tool will warn you); avoid doing this on a shared screen. Use a unique email address. You can use the admin to log in immediately afterward.

If the Maven terminal cannot accept interactive input, open `src/main/java/com/scentzone/tools/AdminSetup.java` in VS Code and click **Run Java** above `main`; make sure VS Code's launched Java process receives the `SCENT_DB_USER` and `SCENT_DB_PASSWORD` variables from its environment.

## 6. Run the JavaFX application

```powershell
mvn clean javafx:run
```

A black-and-gold **SCENT ZONE** login window should appear.

- **Customer:** choose **Create an account**, register, log in, search perfumes, add to cart and place an order.
- **Admin:** log in with the account you created using `AdminSetup`.
- Open MySQL Workbench and run `SELECT * FROM orders;` or `SELECT * FROM order_items;` to inspect purchases.

## 7. Order processing rules

- New orders start as `PENDING`.
- Admin can advance `PENDING → CONFIRMED → SHIPPED → DELIVERED`.
- Admin can cancel `PENDING` or `CONFIRMED` orders. Cancelling restocks their items **once**, in the same transaction.
- A `SHIPPED`, `DELIVERED`, or `CANCELLED` order cannot be cancelled from this GUI.
- Checkout rechecks stock and prices in the database, locks the necessary stock rows, creates the order/items and adjusts inventory in **one transaction**.
- Hide/unhide products instead of deleting records referenced by old orders.

## 8. Files and architecture

```text
Scent-Zone/
├── pom.xml
├── .vscode/
│   ├── extensions.json
│   └── settings.json
├── database/
│   └── scent_zone.sql
├── docs/
│   └── ARCHITECTURE.md
├── src/main/java/com/scentzone/
│   ├── MainApp.java                  JavaFX entrypoint
│   ├── config/Database.java          Environment-based JDBC connection
│   ├── controller/                   Login, register, admin, customer screens
│   ├── dao/                          Prepared SQL queries + transactions
│   ├── model/                        User, Brand, Perfume, CartLine, Order
│   ├── security/PasswordUtil.java    Salted PBKDF2 password storage
│   ├── service/                      Auth and checkout services
│   ├── tools/AdminSetup.java         One-time admin creation
│   └── util/                         Navigation, dialogs, formatting
└── src/main/resources/
    ├── fxml/                         Four GUI layouts
    └── css/style.css                 Black/gold theme
```

## 9. Quick troubleshooting

| Symptom | What to do |
| --- | --- |
| `java -version` is 22 | Activate JDK 21; verify `mvn -version` also uses Java 21 |
| `mvn` not recognized | Install Maven, add Maven's `bin` directory to Path, restart VS Code |
| Dependencies cannot download | Check internet / Maven proxy / firewall; first run fetches libraries |
| `Access denied for user` | Check `SCENT_DB_USER`, `SCENT_DB_PASSWORD`, MySQL service and grants |
| `Unknown database 'scent_zone_db'` | Run `database/scent_zone.sql` first |
| `Communications link failure` | Start MySQL server, verify port 3306, inspect `SCENT_DB_URL` |
| `Incorrect email or password` | Create a customer through Register or an admin with AdminSetup |
| Blank products / no brands | Run the sample seed script or add products from Admin |
| GUI doesn't start | Run `mvn clean javafx:run` **inside** folder containing `pom.xml`; inspect terminal errors |

## Validation notes

The Java backend was compiled with Java 21 and FXML files were XML-validated during package creation. A complete Maven/JavaFX launch and MySQL integration test require Maven dependencies and a local MySQL server on your computer. See `docs/ARCHITECTURE.md` for data flow and additional testing ideas.

**Built for study and local demonstration.**
