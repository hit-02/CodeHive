# CodeHive Forum

A modern, feature-rich community forum platform built with Spring Boot, designed to foster collaboration and knowledge sharing among developers and tech enthusiasts.

## 📋 Table of Contents

- [Project Description](#project-description)
- [Key Features](#key-features)
- [Tech Stack](#tech-stack)
- [Architecture](#architecture)
- [Installation](#installation)
- [Usage](#usage)
- [Contribution Guidelines](#contribution-guidelines)
- [License](#license)

## 🎯 Project Description

CodeHive is a comprehensive community forum application that provides a robust platform for users to publish articles, engage in discussions, and build knowledge collaboratively. The platform is built on modern technologies and designed with scalability, performance, and user experience in mind.

The project combines backend services powered by Spring Boot with advanced features including real-time communication, intelligent search capabilities, and AI-powered assistance. Whether you're looking to share technical insights or seek solutions from the community, CodeHive provides an intuitive and efficient platform.

## ✨ Key Features

### Article Publishing
- **Create and Publish Articles**: Users can easily write and publish technical articles with rich text formatting
- **Article Management**: Edit, delete, and organize published content
- **Version History**: Track changes and revisions to articles over time
- **Metadata Support**: Add tags, categories, and descriptions for better content organization

### Search and Discovery
- **Full-Text Search**: Powerful search functionality to find articles and discussions
- **Advanced Filtering**: Filter content by category, tags, author, and date
- **Search Analytics**: Track trending topics and popular searches
- **Autocomplete Suggestions**: Intelligent suggestions while typing search queries

### Commenting and Discussions
- **Nested Comments**: Support for threaded discussions with reply functionality
- **Rich Comment Formatting**: Format comments with code snippets, links, and text styling
- **Comment Moderation**: Tools for managing and moderating community discussions
- **Notification System**: Real-time notifications for replies and mentions

### Real-Time Features
- **WebSocket Integration**: Bi-directional real-time communication
- **Live Updates**: Instant updates for new articles, comments, and user activity
- **Presence Indicators**: See who is currently online and active
- **Real-Time Notifications**: Immediate alerts for user interactions

### AI-Powered Assistant
- **Intelligent Recommendations**: AI suggestions for relevant articles and discussions
- **Smart Search Enhancement**: AI-powered search result ranking and relevance
- **Automated Content Tagging**: Automatic tag suggestions for articles
- **Chatbot Support**: AI assistant for answering frequently asked questions

### User Management
- **User Registration and Authentication**: Secure user account creation and login
- **User Profiles**: Customizable user profiles with contributions tracking
- **Reputation System**: Earn badges and reputation points through community contributions
- **Role-Based Access Control**: Different permission levels for users, moderators, and administrators

## 🔧 Tech Stack

### Backend
- **Framework**: Spring Boot 2.x/3.x
- **Language**: Java 11+
- **Build Tool**: Maven/Gradle

### Databases
- **Primary Database**: MySQL/PostgreSQL for relational data
- **Document Database**: MongoDB for flexible document storage
- **Time-Series Data**: InfluxDB for analytics and metrics

### Caching and Performance
- **Cache Layer**: Redis for session management and data caching
- **Message Queue**: RabbitMQ/Kafka for asynchronous processing
- **Search Engine**: Elasticsearch for full-text search capabilities

### Real-Time Communication
- **WebSocket**: Spring WebSocket for real-time bidirectional communication
- **Protocol**: STOMP (Simple Text Oriented Messaging Protocol)

### Integration and APIs
- **REST APIs**: RESTful web services for client-server communication
- **OpenAPI/Swagger**: API documentation and interactive testing

### Containerization and Deployment
- **Container Platform**: Docker for application containerization
- **Orchestration**: Docker Compose for multi-container orchestration
- **Cloud Ready**: Support for Kubernetes deployment

### Development Tools
- **Testing**: JUnit 5, Mockito for unit and integration testing
- **Code Quality**: SonarQube for code analysis
- **Version Control**: Git for source code management
- **CI/CD**: GitHub Actions/Jenkins for continuous integration

## 🏗️ Architecture

### System Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                     Client Layer                             │
│  (Web Browser, Mobile App, Desktop Client)                  │
└────────────────────┬────────────────────────────────────────┘
                     │
         ┌───────────┼───────────┐
         │           │           │
    ┌────▼───┐  ┌───▼────┐  ┌──▼──────┐
    │  REST  │  │WebSocket│  │GraphQL  │
    │  API   │  │  API    │  │  API    │
    └────┬───┘  └───┬────┘  └──┬──────┘
         └───────────┼───────────┘
                     │
    ┌────────────────▼──────────────────┐
    │   Spring Boot Application Layer   │
    │ ┌──────────────────────────────┐ │
    │ │  Controller Layer            │ │
    │ │  (Request Handling)          │ │
    │ └──────────────────────────────┘ │
    │ ┌──────────────────────────────┐ │
    │ │  Service Layer               │ │
    │ │  (Business Logic)            │ │
    │ └──────────────────────────────┘ │
    │ ┌──────────────────────────────┐ │
    │ │  Repository Layer            │ │
    │ │  (Data Access)               │ │
    │ └──────────────────────────────┘ │
    └────────────────┬─────────────────┘
         ┌───────────┼───────────┐
         │           │           │
    ┌────▼───┐  ┌───▼────┐  ┌──▼───────┐
    │ Redis  │  │Message  │  │Elastic   │
    │ Cache  │  │ Queue   │  │Search    │
    └────────┘  └────────┘  └──────────┘
         │           │           │
    ┌────▼───────────▼───────────▼─────┐
    │      Database Layer               │
    │ ┌─────────────────────────────┐  │
    │ │  MySQL/PostgreSQL           │  │
    │ │  (Relational Data)          │  │
    │ └─────────────────────────────┘  │
    │ ┌─────────────────────────────┐  │
    │ │  MongoDB                    │  │
    │ │  (Document Data)            │  │
    │ └─────────────────────────────┘  │
    │ ┌─────────────────────────────┐  │
    │ │  InfluxDB                   │  │
    │ │  (Time-Series Data)         │  │
    │ └─────────────────────────────┘  │
    └─────────────────────────────────┘
```

### Core Modules

1. **Article Module**: Handles article creation, publishing, and management
2. **Comment Module**: Manages comments, replies, and discussions
3. **User Module**: Handles user registration, authentication, and profiles
4. **Search Module**: Provides search and filtering capabilities
5. **Notification Module**: Manages real-time notifications and alerts
6. **AI Assistant Module**: Integrates AI-powered features and recommendations
7. **Admin Module**: Provides administration and moderation tools

## 📦 Installation

### Prerequisites

- **Java Development Kit (JDK)**: Version 11 or higher
- **Maven**: Version 3.6+ or Gradle 7.0+
- **Docker**: Version 20.10+ (optional, for containerization)
- **Git**: For version control

### Step 1: Clone the Repository

```bash
git clone https://github.com/hit-02/CodeHive.git
cd CodeHive
```

### Step 2: Configure Application Properties

Create a `application.properties` or `application.yml` file in the `src/main/resources` directory:

```properties
# Server Configuration
server.port=8080
server.servlet.context-path=/api

# Database Configuration
spring.datasource.url=jdbc:mysql://localhost:3306/codehive
spring.datasource.username=root
spring.datasource.password=your_password
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# JPA Configuration
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQL8Dialect

# Redis Configuration
spring.redis.host=localhost
spring.redis.port=6379
spring.redis.password=

# Elasticsearch Configuration
spring.elasticsearch.rest.uris=http://localhost:9200

# MongoDB Configuration
spring.data.mongodb.uri=mongodb://localhost:27017/codehive

# File Upload Configuration
file.upload.path=/data/uploads
file.max-size=50MB

# JWT Configuration
jwt.secret=your_jwt_secret_key
jwt.expiration=86400000
```

### Step 3: Build the Project

Using Maven:
```bash
mvn clean install
```

Using Gradle:
```bash
gradle clean build
```

### Step 4: Run the Application

Using Maven:
```bash
mvn spring-boot:run
```

Using Gradle:
```bash
gradle bootRun
```

The application will start on `http://localhost:8080/api`

### Step 5: Docker Deployment (Optional)

#### Building Docker Image

```bash
docker build -t codehive:latest .
```

#### Running with Docker Compose

Create a `docker-compose.yml` file in the project root:

```yaml
version: '3.8'

services:
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: root_password
      MYSQL_DATABASE: codehive
    ports:
      - "3306:3306"
    volumes:
      - mysql_data:/var/lib/mysql

  mongodb:
    image: mongo:5.0
    ports:
      - "27017:27017"
    volumes:
      - mongo_data:/data/db

  redis:
    image: redis:7.0
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data

  elasticsearch:
    image: docker.elastic.co/elasticsearch/elasticsearch:8.0.0
    environment:
      - discovery.type=single-node
      - xpack.security.enabled=false
    ports:
      - "9200:9200"
    volumes:
      - es_data:/usr/share/elasticsearch/data

  codehive:
    build: .
    ports:
      - "8080:8080"
    environment:
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/codehive
      SPRING_REDIS_HOST: redis
      SPRING_ELASTICSEARCH_URIS: http://elasticsearch:9200
      SPRING_DATA_MONGODB_URI: mongodb://mongodb:27017/codehive
    depends_on:
      - mysql
      - mongodb
      - redis
      - elasticsearch

volumes:
  mysql_data:
  mongo_data:
  redis_data:
  es_data:
```

Run all services:
```bash
docker-compose up -d
```

## 🚀 Usage

### API Endpoints

#### Authentication
```
POST   /api/auth/register          - Register a new user
POST   /api/auth/login             - Login user
POST   /api/auth/logout            - Logout user
POST   /api/auth/refresh-token     - Refresh JWT token
```

#### Articles
```
GET    /api/articles               - Get all articles with pagination
GET    /api/articles/{id}          - Get article by ID
POST   /api/articles               - Create a new article
PUT    /api/articles/{id}          - Update article
DELETE /api/articles/{id}          - Delete article
GET    /api/articles/search        - Search articles (full-text search)
GET    /api/articles/{id}/comments - Get comments for article
```

#### Comments
```
POST   /api/comments               - Create a new comment
PUT    /api/comments/{id}          - Update comment
DELETE /api/comments/{id}          - Delete comment
GET    /api/comments/{id}/replies  - Get replies to a comment
```

#### Users
```
GET    /api/users/{id}             - Get user profile
PUT    /api/users/{id}             - Update user profile
GET    /api/users/{id}/articles    - Get user's articles
GET    /api/users/{id}/comments    - Get user's comments
POST   /api/users/{id}/follow      - Follow a user
POST   /api/users/{id}/unfollow    - Unfollow a user
```

#### Search
```
GET    /api/search                 - Search articles and comments
GET    /api/search/trending        - Get trending topics
GET    /api/search/suggestions     - Get search suggestions
```

#### Notifications
```
GET    /api/notifications          - Get user notifications
PUT    /api/notifications/{id}     - Mark notification as read
DELETE /api/notifications/{id}     - Delete notification
```

### WebSocket Events

Real-time communication is available via WebSocket at `/ws`:

#### Client to Server Events
```
SUBSCRIBE /user/queue/notifications     - Subscribe to user notifications
SEND /app/article/publish               - Broadcast new article
SEND /app/comment/create                - Broadcast new comment
SEND /app/user/online                   - Update user presence
```

#### Server to Client Events
```
/user/queue/notifications               - Receive notifications
/topic/articles                         - Receive article updates
/topic/comments                         - Receive comment updates
/topic/users                            - Receive user activity updates
```

### Example API Usage

#### Create an Article
```bash
curl -X POST http://localhost:8080/api/articles \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Getting Started with Spring Boot",
    "content": "Spring Boot is a powerful framework...",
    "tags": ["spring-boot", "java", "backend"],
    "category": "Tutorial"
  }'
```

#### Search Articles
```bash
curl "http://localhost:8080/api/search?q=spring+boot&category=tutorial&limit=10"
```

#### Get Notifications
```bash
curl http://localhost:8080/api/notifications \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

## 👥 Contribution Guidelines

We welcome contributions from the community! Please follow these guidelines to contribute to CodeHive:

### Getting Started

1. **Fork the Repository**: Click the "Fork" button on GitHub to create a personal copy
2. **Clone Your Fork**: 
   ```bash
   git clone https://github.com/YOUR_USERNAME/CodeHive.git
   cd CodeHive
   ```
3. **Create a Feature Branch**: 
   ```bash
   git checkout -b feature/your-feature-name
   ```

### Development Workflow

1. **Make Changes**: Implement your feature or fix
2. **Write Tests**: Add unit and integration tests for your changes
3. **Follow Code Style**: Adhere to the project's coding standards
4. **Commit with Clear Messages**: 
   ```bash
   git commit -m "feat: add new feature description"
   ```
5. **Push to Your Fork**: 
   ```bash
   git push origin feature/your-feature-name
   ```

### Submitting a Pull Request

1. **Create a Pull Request**: Go to GitHub and create a PR from your feature branch
2. **Describe Your Changes**: Provide a clear description of what you've changed and why
3. **Reference Issues**: Link related issues using `#issue-number`
4. **Wait for Review**: Maintainers will review your changes
5. **Address Feedback**: Make requested changes and update your PR
6. **Merge**: Once approved, your PR will be merged

### Code Style Guidelines

- **Java Code**: Follow Google Java Style Guide
- **Variable Naming**: Use camelCase for variables and methods
- **Constants**: Use UPPER_CASE for constants
- **Documentation**: Add JavaDoc comments for public classes and methods
- **Line Length**: Keep lines under 120 characters

### Commit Message Format

Use the following format for commit messages:

```
type(scope): subject

body (optional)

footer (optional)
```

Types:
- `feat`: A new feature
- `fix`: A bug fix
- `docs`: Documentation changes
- `style`: Code style changes (formatting, missing semicolons, etc.)
- `refactor`: Code refactoring without feature changes
- `perf`: Performance improvements
- `test`: Adding or updating tests
- `chore`: Build process, dependencies, etc.

Example:
```
feat(articles): add markdown support for article content

- Implement markdown parser integration
- Update article model to support raw markdown
- Add markdown preview endpoint

Closes #42
```

### Testing Requirements

- Write unit tests for new features
- Maintain test coverage above 80%
- Run existing tests before submitting PR:
  ```bash
  mvn test
  ```

### Code Review Process

1. At least one maintainer must approve your PR
2. All CI/CD checks must pass
3. Code quality gates must be met
4. No conflicts with the base branch

### Reporting Issues

If you find a bug or have a feature request:

1. **Check Existing Issues**: Search for similar issues first
2. **Create a New Issue**: Use clear, descriptive titles
3. **Include Details**: Provide reproduction steps and expected behavior
4. **Attach Logs**: Include error messages and stack traces

### Communication

- **Discussions**: Use GitHub Discussions for general questions
- **Issues**: Use GitHub Issues for bugs and feature requests
- **Email**: Contact maintainers directly for sensitive issues

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

---

## 🤝 Support and Community

- **GitHub Issues**: Report bugs and request features
- **Discussions**: Join community discussions
- **Contributing**: See CONTRIBUTING.md for detailed guidelines
- **Code of Conduct**: Please adhere to our Code of Conduct

## 📚 Additional Resources

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Elasticsearch Guide](https://www.elastic.co/guide/en/elasticsearch/reference/current/index.html)
- [Redis Documentation](https://redis.io/documentation)
- [Docker Documentation](https://docs.docker.com/)

---

**Last Updated**: January 5, 2026

For more information, issues, or suggestions, please visit our [GitHub Repository](https://github.com/hit-02/CodeHive) or contact the maintainers.

Happy coding! 🚀
