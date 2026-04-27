# 🚀 Employee REST API with Kafka Integration

**A Spring Boot REST service that manages employees in MySQL and publishes events to Kafka whenever someone fetches a record by ID. Clean layered architecture — Controller → Service → Repository — with custom Spring Data finder methods.**

This isn't a tutorial CRUD. This is a CRUD that knows when someone looks at employee data and tells Kafka about it.

---

## 🎯 What This API Does

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/employees` | GET | Returns all employees |
| `/api/employees/{id}` | GET | Returns one employee by ID |
| `/api/employees/name/{name}` | GET | Custom finder — all employees by name |
| `/api/employees` | POST | Creates a new employee |
| `/api/employees` | PUT | Updates an existing employee |
| `/api/employees/{id}` | DELETE | Deletes employee by ID |
| `/kafka/send?id={id}` | POST | Fetches employee by ID and sends to Kafka topic |

---

## 🏗️ Layered Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    HTTP Requests                         │
└──────────────────────┬──────────────────────────────────┘
                       │
┌──────────────────────▼───────────────────────────────────┐
│                REST Controllers                           │
│  MyRESTController — CRUD endpoints                       │
│  Controller — Kafka trigger endpoint                     │
└──────────────────────┬──────────────────────────────────┘
                       │
┌──────────────────────▼───────────────────────────────────┐
│              EmployeeService (interface)                  │
│              EmployeeServiceImpl                          │
│  - Business logic                                        │
│  - Transactional boundaries                              │
└──────────────────────┬──────────────────────────────────┘
                       │
┌──────────────────────▼───────────────────────────────────┐
│           EmployeeRepository (Spring Data JPA)            │
│  - findAll()                                             │
│  - findById(id)                                          │
│  - findAllByName(name) ← Custom query method             │
│  - save(entity)                                          │
│  - deleteById(id)                                        │
└──────────────────────┬──────────────────────────────────┘
                       │               │
              ┌────────▼────┐   ┌──────▼──────────┐
              │    MySQL    │   │  Kafka Producer  │
              │  employees  │   │  topic: employee │
              └─────────────┘   └─────────────────┘
```

---

## 🧠 Design Decisions Worth Noticing

| Decision | Why It's Not Obvious |
|----------|----------------------|
| **Service layer via interface** | `EmployeeService` interface + `EmployeeServiceImpl` — allows future mocking in tests, follows dependency inversion |
| **Custom finder by name** | `findAllByName(String name)` — Spring Data derives the query from method name. No SQL written |
| **Kafka on read, not write** | The `/kafka/send` endpoint fetches existing data and publishes to Kafka. Audit trail for data access, not just data changes |
| **Constructor injection** | Both controllers use constructor injection — no `@Autowired` on fields. Testable, immutable |
| **Topic created via @Bean** | `NewTopic` bean in `KafkaConfiguration` — infrastructure-as-code, topic auto-created on startup |
| **GenerationType.IDENTITY** | Primary key strategy — MySQL auto-increment, no sequence table, no hibernate_sequence |

---

## 🗄️ Database Schema

```sql
CREATE TABLE employees (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255),
    surname VARCHAR(255),
    department VARCHAR(255),
    salary INT
);
```

Managed by Hibernate `ddl-auto` — but explicit schema control via `@Table(name = "employees")` and `@Column(name = "...")` annotations.

---

## 📦 Kafka Integration

```java
// Topic auto-created on startup
@Bean
public NewTopic newTopic() {
    return new NewTopic("employee", 1, (short) 1);
}

// Producer sends to "employee" topic
public void sendMessage(String message) {
    kafkaTemplate.send("employee", message);
}
```

- **Topic name:** `employee`
- **Partitions:** 1
- **Replication factor:** 1
- **Serialization:** `KafkaTemplate<String, String>` — both key and value are Strings

---

## 🛠️ Tech Stack

![Java](https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)
![Spring Data JPA](https://img.shields.io/badge/Spring_Data_JPA-6DB33F?style=for-the-badge&logo=spring&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-4479A1?style=for-the-badge&logo=mysql&logoColor=white)
![Apache Kafka](https://img.shields.io/badge/Apache_Kafka-231F20?style=for-the-badge&logo=apache-kafka&logoColor=white)
![Maven](https://img.shields.io/badge/Maven-C71A36?style=for-the-badge&logo=apache-maven&logoColor=white)

---

## 🚀 Quick Run

```bash
# 1. Start MySQL & Kafka
# 2. Create database
mysql -u root -p -e "CREATE DATABASE spring_data_jpa;"

# 3. Clone & build
git clone https://github.com/NeironEssensive/spring-boot-data-jpa.git
cd spring-boot-data-jpa
mvn clean package

# 4. Run
java -jar target/spring-boot-data-jpa.jar

# 5. Test endpoints
curl http://localhost:8080/api/employees
curl -X POST http://localhost:8080/api/employees \
  -H "Content-Type: application/json" \
  -d '{"name":"Alex","surname":"Smith","department":"IT","salary":5000}'
curl http://localhost:8080/api/employees/name/Alex
curl -X POST http://localhost:8080/kafka/send?id=1
```

---

## 🧪 What I'd Improve Next

- [ ] Global exception handler (`@ControllerAdvice`) — return proper 404 when employee not found
- [ ] DTO layer — separate API contracts from entities (`EmployeeDto` vs `Employee`)
- [ ] Pagination — `GET /api/employees?page=0&size=20`
- [ ] Kafka consumer — consume from `employee` topic and log/store audit events
- [ ] Integration tests with Testcontainers (MySQL + Kafka)
- [ ] Swagger/OpenAPI documentation
- [ ] Bean Validation (`@NotBlank`, `@Min`) on request body
