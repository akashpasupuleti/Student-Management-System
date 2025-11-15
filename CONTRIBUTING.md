
# 📚 Student Management & Result Processing System

A complete Spring Boot application that allows **teachers** to upload Excel files with student marks and **students** to view their performance dashboards.  
This project is **open-source**, and we are actively welcoming **frontend/UI contributors**.

---

# 📌 Table of Contents

1. Overview  
2. Features  
3. Tech Stack  
4. Project Architecture  
5. How the System Works  
6. Installation (Backend – Optional)  
7. Frontend Contribution Guide (Recommended)  
8. Git Contribution Workflow  
9. Good First Issues  
10. License  

---

# 1️⃣ Overview

This project automates the student result-processing workflow:

- Teachers upload Excel sheets.
- System extracts marks using Apache POI.
- Grades & SGPA are calculated automatically.
- Students log in and view dashboards.
- Admin/Teacher can view analytics & performance.

The backend is fully functional, but the **frontend is intentionally simple** to allow room for UI/UX contributors.

---

# 2️⃣ Features

### 👨‍🏫 Teacher Functions
- Upload Excel sheets containing:
  - HTNO  
  - Subject codes  
  - Marks  
  - Credits  
  - Grades  
- Automatic parsing and validation  
- Saves all records into MySQL  
- View complete student profiles  
- View all exam results

### 👩‍🎓 Student Functions
- Secure login  
- View subject-wise marks & grades  
- SGPA calculator  
- View top achievers  
- Clean dashboard layout  

### 🔐 Security
- Login system with Spring Security  
- Optional email alerts (SMTP configurable)  
- Input validation & error handling  

---

# 3️⃣ Tech Stack

### **Backend**
- Java 17  
- Spring Boot  
- Spring MVC  
- Spring Security  
- Spring Data JPA (Hibernate)  
- MySQL  
- Apache POI (Excel parsing)

### **Frontend**
- HTML  
- CSS  
- JavaScript  
- Thymeleaf  

---

# 4️⃣ Project Architecture

```

src/
└── main/
├── java/com/app/...          # Controllers, Services, Repositories
├── resources/
│     ├── templates/          # HTML (Frontend)
│     ├── static/css/         # CSS
│     ├── static/js/          # JavaScript
│     ├── static/images/      # Images
│     └── application.properties
└── test/

```

---

# 5️⃣ How the Excel Processing Works

1. Teacher uploads an `.xlsx` file.  
2. Apache POI reads each row and validates fields.  
3. System maps:  
   - Student → Subjects → Marks → Credits → Grade  
4. Data is inserted into MySQL.  
5. SGPA is automatically computed.  
6. Student can view results instantly.

---

# 6️⃣ Backend Installation (Optional — Only for full stack contributors)

If you are only contributing UI/Frontend, **skip to section 7**.

### **Step 1: Install Requirements**
- Java 17+
- Maven
- MySQL Server

### **Step 2: Configure the database**

Create a database named:

```

test1

```

Update credentials inside:

```

src/main/resources/application.properties

```

Example:

```

spring.datasource.url=jdbc:mysql://localhost:3306/test1
spring.datasource.username=YOUR_USERNAME
spring.datasource.password=YOUR_PASSWORD

````

### **Step 3: Run the backend**

```bash
mvn spring-boot:run
````

App runs at:
👉 [http://localhost:8081/](http://localhost:8081/)

---

# 7️⃣ Frontend Contribution Guide (Recommended)

You **do NOT** need the backend running** if you are only improving the UI.**

---

## ⚠️ Important Note for Frontend Contributors

Many HTML pages currently contain:

* Inline CSS
* Inline JavaScript
* Repeated styling
* Unstructured template layout

This is intentional — contributors are encouraged to help **clean and reorganize the frontend**.

You can improve the structure by:

* Moving inline CSS →
  `src/main/resources/static/css/`
* Moving inline JS →
  `src/main/resources/static/js/`
* Optimizing repetitive HTML
* Making pages responsive
* Rebuilding dashboard UI

This project is a great opportunity to learn **proper frontend architecture**.

---

### 📁 Frontend Files are Here:

```
src/main/resources/templates/       # HTML files
src/main/resources/static/css/      # CSS files
src/main/resources/static/js/       # JS files
src/main/resources/static/images/   # Images
```

### Suggested Frontend Enhancements

✔ Improve login/signup pages
✔ Enhance student & teacher dashboards
✔ Apply Tailwind / Bootstrap
✔ Add charts (Chart.js)
✔ Make UI responsive
✔ Remove inline CSS/JS

---

# 8️⃣ Git Contribution Workflow (Step-by-Step)

### ✔ Step 1: Fork the repo

Click **Fork** on GitHub.

### ✔ Step 2: Clone your fork

```bash
git clone https://github.com/<your-username>/Student-Management-System.git
cd Student-Management-System
```

### ✔ Step 3: Create your feature branch

```bash
git checkout -b feature-ui-improvements
```

### ✔ Step 4: Make changes

Work inside:

```
templates/
static/css/
static/js/
```

### ✔ Step 5: Stage and commit

```bash
git add .
git commit -m "Improved student signup UI and moved inline CSS to external file"
```

(If you improved teacher signup, change the message accordingly.)

Example:

```bash
git commit -m "Refactored teacher signup UI and cleaned inline JS"
```

### ✔ Step 6: Push your branch

```bash
git push origin feature-ui-improvements
```

### ✔ Step 7: Create a Pull Request

* Go to your GitHub fork
* Click **Compare & Pull Request**
* Submit 🚀

---

# 9️⃣ Good First Issues (Beginner-Friendly)

* Convert inline CSS → external files
* Convert inline JS → external files
* Add TailwindCSS or Bootstrap
* Improve layout of dashboard pages
* Implement Chart.js visualizations
* Add transitions & animations
* Improve error pages

---

# 🔟 License

This project is licensed under the GNU GPL v3 License (NO commercial use allowed).
See the LICENSE file for details.
--- 

