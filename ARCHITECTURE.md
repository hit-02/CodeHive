# CodeHive Architecture Documentation

## Table of Contents
1. [Overview](#overview)
2. [High-Concurrency Design](#high-concurrency-design)
3. [Docker Sandbox Implementation](#docker-sandbox-implementation)
4. [Data Synchronization Pipeline](#data-synchronization-pipeline)
5. [Multi-LLM Routing System](#multi-llm-routing-system)
6. [Caching Strategies](#caching-strategies)
7. [Real-Time Leaderboard Implementation](#real-time-leaderboard-implementation)
8. [System Architecture Diagram](#system-architecture-diagram)
9. [Deployment Considerations](#deployment-considerations)

---

## Overview

CodeHive is a high-performance, distributed system designed to facilitate collaborative code execution, AI-powered code analysis, and competitive programming. The architecture emphasizes scalability, reliability, and low-latency operations through a carefully orchestrated stack of microservices and supporting infrastructure.

### Key Design Principles
- **Horizontal Scalability**: Stateless services that scale independently
- **Resilience**: Graceful degradation and circuit breaker patterns
- **Performance**: Multi-layer caching and asynchronous processing
- **Security**: Isolated execution environments and comprehensive access controls
- **Observability**: Structured logging and distributed tracing

---

## High-Concurrency Design

### 1. Request Handling Architecture

#### Connection Pooling
```
┌─────────────────────────────────────────────┐
│         Load Balancer (nginx/HAProxy)       │
├─────────────────────────────────────────────┤
│  Connection Pool (1000+ concurrent)         │
├─────────────────────────────────────────────┤
│  Worker Threads (Event-driven, async I/O)   │
└─────────────────────────────────────────────┘
```

**Implementation Details:**
- **Thread Pool Configuration**: Core pool size of 100-200 threads, max pool size of 1000+ depending on system resources
- **Queue Management**: Unbounded queues for non-critical operations, bounded queues for critical paths with rejection policies
- **Connection Reuse**: HTTP Keep-Alive enabled, connection pooling for database connections (min: 20, max: 100 per database)

#### Async/Non-blocking I/O
- **Framework**: Node.js/Go-based services using event-driven architecture or Python's asyncio
- **Database Drivers**: Async-compatible drivers (e.g., asyncpg for PostgreSQL, motor for MongoDB)
- **HTTP Clients**: Non-blocking HTTP clients with configurable timeouts and retries
- **Message Queues**: Async message consumption with acknowledgment strategies

### 2. Rate Limiting & Load Shedding

**Token Bucket Algorithm**
```
User Rate Limit: 1000 requests/minute
- Tokens: 1000
- Refill Rate: ~16.67 tokens/second
- Token Cost per Request: Varies by endpoint
  * Read operations: 1 token
  * Compute operations: 5-10 tokens
  * Admin operations: 50+ tokens
```

**Endpoint-Specific Limits:**
| Endpoint | Limit | Window |
|----------|-------|--------|
| Code Execution | 100/min | Per user |
| API Requests | 1000/min | Per user |
| Leaderboard Queries | 500/min | Per user |
| File Operations | 200/min | Per user |

**Overload Protection:**
- Circuit breaker pattern for external service calls
- Graceful degradation: cache hits return stale data when backends are overloaded
- Request queuing with priority levels (critical > normal > background)

### 3. Database Connection Management

**Connection Pool Configuration:**
```yaml
Database: PostgreSQL
Connections:
  Min Pool Size: 20
  Max Pool Size: 100
  Idle Timeout: 900 seconds
  Max Lifetime: 1800 seconds
  Validation Query: "SELECT 1"
  
Database: Redis
Connections:
  Min Pool Size: 10
  Max Pool Size: 50
  Idle Timeout: 300 seconds
```

**Query Optimization:**
- Connection pooling with statement caching
- Query timeouts (5s for interactive, 30s for batch operations)
- Index strategies for hot data paths
- Read replicas for reporting and analytics

### 4. Concurrency Metrics & Monitoring

**Key Metrics:**
```
- Active Connections: Current concurrent connections
- Request Latency: p50, p95, p99 latencies
- Queue Depth: Number of pending requests
- Thread Pool Utilization: % of threads in use
- Database Connection Utilization: % of connections in use
- Error Rate: Failed requests per second
- Throughput: Requests per second
```

**Alerting Thresholds:**
- Active connections > 80% capacity → scale out
- p99 latency > 5 seconds → investigate bottlenecks
- Queue depth > 1000 → trigger backpressure
- Error rate > 0.1% → page on-call engineer

---

## Docker Sandbox Implementation

### 1. Sandbox Architecture

**Isolated Execution Environment**
```
┌─────────────────────────────────────────────┐
│         Code Execution Request              │
├─────────────────────────────────────────────┤
│    Sandboxing Service (Resource Manager)    │
├─────────────────────────────────────────────┤
│  ┌─────────────────────────────────────┐   │
│  │    Docker Container (Isolate)       │   │
│  │  ├─ Memory: 256MB - 512MB           │   │
│  │  ├─ CPU: 1 core (limited)           │   │
│  │  ├─ Disk: 1GB ephemeral             │   │
│  │  ├─ Network: Disabled (except I/O)  │   │
│  │  └─ User: Non-root user             │   │
│  └─────────────────────────────────────┘   │
└─────────────────────────────────────────────┘
```

### 2. Container Images & Runtimes

**Supported Languages:**
```dockerfile
# Python Runtime
FROM python:3.11-slim
RUN pip install --no-cache-dir numpy pandas requests
RUN useradd -m codehive && chown -R codehive:codehive /app

# JavaScript/Node.js Runtime
FROM node:20-alpine
RUN npm install -g pm2
RUN adduser -D -h /home/codehive codehive

# Java Runtime
FROM eclipse-temurin:21-jdk-alpine
RUN adduser -D -h /home/codehive codehive

# Go Runtime
FROM golang:1.21-alpine
RUN adduser -D -h /home/codehive codehive
```

**Image Configuration:**
- Minimal base images (alpine/slim variants)
- Pre-installed common libraries
- Non-root user execution (UID 1000+)
- Read-only root filesystem where possible
- Distroless images for production (reduces attack surface)

### 3. Resource Limits & Constraints

**Per-Container Limits:**
```yaml
Memory:
  Default: 256MB
  Max: 512MB
  Swap: Disabled

CPU:
  Default: 1 CPU core
  CFS Quota: 100,000 microseconds per 100ms period
  Shares: 1024 (relative priority)

Disk:
  Ephemeral Storage: 1GB
  Temporary Directory: /tmp (200MB)
  Timeout Cleanup: 5 minutes after completion

Network:
  Outbound: Allowed to whitelisted services only
  Inbound: Disabled except for stdin/stdout
  Rate Limit: 10Mbps per container
```

**Resource Enforcement:**
```python
import docker
import resource

client = docker.from_env()

container = client.containers.run(
    image="codehive-python:latest",
    mem_limit="256m",
    memswap_limit="256m",
    cpu_period=100000,
    cpu_quota=100000,
    cpu_shares=1024,
    stdin_open=True,
    stdout=True,
    stderr=True,
    timeout=30,  # 30-second execution timeout
    security_opt=["no-new-privileges"],
    cap_drop=["ALL"],
    cap_add=["NET_BIND_SERVICE"],
    tmpfs={"/tmp": "size=200m,noexec,nodev,nosuid"}
)
```

### 4. Security Measures

**Isolation Layers:**
1. **Kernel Level**: cgroups for resource limits, namespaces for process isolation
2. **Container Level**: seccomp profiles to restrict syscalls, AppArmor/SELinux policies
3. **Application Level**: Sandboxed interpreters, jailed execution environments

**Seccomp Profile:**
```json
{
  "defaultAction": "SCMP_ACT_ERRNO",
  "defaultErrnoRet": 1,
  "archMap": [
    {
      "architecture": "SCMP_ARCH_X86_64",
      "subArchitectures": ["SCMP_ARCH_X86", "SCMP_ARCH_X32"]
    }
  ],
  "syscalls": [
    {
      "names": ["read", "write", "open", "close", "stat"],
      "action": "SCMP_ACT_ALLOW"
    },
    {
      "names": ["socket", "connect"],
      "action": "SCMP_ACT_ERRNO",
      "errnoRet": 13
    }
  ]
}
```

**Networking Security:**
- Containers run in isolated networks
- Egress filtering: Only allow connections to approved service registries
- Ingress blocking: Disable inbound network connections by default
- DNS: Custom DNS resolver with query logging

### 5. Container Lifecycle Management

**Creation → Execution → Cleanup Pipeline:**
```
1. Pre-execution (100ms)
   - Allocate unique container ID
   - Pull/verify image signature
   - Prepare mount points
   - Initialize resource accounting

2. Execution (0-30s)
   - Start container with enforced limits
   - Stream stdin/stdout/stderr
   - Monitor resource usage
   - Handle interrupts gracefully

3. Post-execution (5s)
   - Capture exit code and metrics
   - Collect container logs
   - Unmount filesystems
   - Clean up temporary storage
   - Update execution records
```

**Cleanup Policy:**
```python
CONTAINER_CLEANUP = {
    "max_age_seconds": 300,  # 5 minutes
    "max_failed_containers": 100,
    "cleanup_interval": 60,  # seconds
    "preserve_logs": True,
    "log_retention_days": 30
}
```

### 6. Performance Optimization

**Container Pooling:**
- Pre-warm containers to reduce startup latency
- Reuse container instances for sequential executions by same user
- Implement container state checkpointing for rapid resets

**Image Caching:**
- Multi-layer image caching with content-addressable storage
- Layer deduplication across multiple image variants
- Distributed cache for image pulls (Docker registry mirror)

---

## Data Synchronization Pipeline

### 1. Event-Driven Architecture

**Event Flow**
```
┌──────────────────┐
│   Event Source   │ (User action, Code execution, etc.)
└────────┬─────────┘
         │
         ▼
┌──────────────────────────────────────┐
│   Event Broker (Kafka/RabbitMQ)      │
├──────────────────────────────────────┤
│  Topics:                             │
│  - code-execution-events             │
│  - leaderboard-updates               │
│  - user-activity-events              │
│  - data-sync-events                  │
└────────┬──────────────────────┬──────┘
         │                      │
    ┌────▼──────┐          ┌────▼──────┐
    │ Consumer 1 │          │ Consumer 2 │
    │ (Database) │          │ (Cache)    │
    └───────────┘          └───────────┘
```

**Event Types:**
```typescript
interface ExecutionEvent {
  event_id: string;
  timestamp: ISO8601;
  user_id: string;
  execution_id: string;
  status: "started" | "completed" | "failed";
  duration_ms: number;
  language: string;
  metadata: Record<string, any>;
}

interface LeaderboardUpdateEvent {
  event_id: string;
  timestamp: ISO8601;
  user_id: string;
  score_delta: number;
  challenge_id: string;
  change_type: "new_submission" | "score_update" | "challenge_solve";
}

interface DataSyncEvent {
  event_id: string;
  timestamp: ISO8601;
  entity_type: string;
  entity_id: string;
  operation: "create" | "update" | "delete";
  data: Record<string, any>;
  version: number;
}
```

### 2. Sync Pipeline Stages

**Stage 1: Capture**
- Transactional outbox pattern for primary database
- CDC (Change Data Capture) using database triggers or logs
- Event timestamp with microsecond precision

**Stage 2: Processing**
```yaml
Processing:
  Deduplication:
    Strategy: Event ID + idempotency key
    Window: 24 hours
    Backend: Redis Bloom Filter
  
  Ordering:
    Guarantee: Causal consistency per user
    Implementation: Partition by user_id + timestamp ordering
  
  Transformation:
    Enrichment: Add computed fields (derived data)
    Normalization: Standard field names and formats
```

**Stage 3: Distribution**
- Fan-out to multiple consumers based on event type
- Priority queue for critical updates (leaderboard > cache > archival)
- Circuit breaker for slow consumers to prevent backpressure

**Stage 4: Persistence**
```sql
-- Sync log table
CREATE TABLE sync_events (
  event_id UUID PRIMARY KEY,
  entity_type VARCHAR(50),
  entity_id UUID,
  operation VARCHAR(20),
  data JSONB,
  version INT,
  created_at TIMESTAMP WITH TIME ZONE,
  synced_to_cache TIMESTAMP,
  synced_to_replicas TIMESTAMP,
  status VARCHAR(20),
  UNIQUE(entity_type, entity_id, version)
);

CREATE INDEX idx_sync_events_created ON sync_events(created_at);
CREATE INDEX idx_sync_events_status ON sync_events(status) WHERE status != 'completed';
```

### 3. Consistency Models

**Strong Consistency for Critical Data:**
```
User makes submission → 
Write to Primary DB → 
Wait for replication → 
Return success

Latency: ~100-200ms
Used for: Score updates, leaderboard ranks
```

**Eventual Consistency for Analytics:**
```
Event captured → 
Propagated to replicas → 
Available within seconds

Latency: 1-5 seconds
Used for: Analytics, reporting, cache updates
```

**Implementation:**
```python
class DataSyncManager:
    async def sync_with_consistency(self, entity, consistency_level="eventual"):
        if consistency_level == "strong":
            # Synchronous replication
            await self.write_primary(entity)
            await self.replicate_to_secondary(entity)
            return {"status": "success", "latency_ms": 150}
        
        elif consistency_level == "eventual":
            # Async replication
            await self.write_primary(entity)
            asyncio.create_task(self.queue_for_replication(entity))
            return {"status": "success", "latency_ms": 50}
```

### 4. Conflict Resolution

**Last-Write-Wins (LWW):**
```python
def resolve_conflict_lww(local_event, remote_event):
    """Resolve conflicts by timestamp"""
    if local_event["timestamp"] > remote_event["timestamp"]:
        return local_event
    return remote_event
```

**Vector Clocks for Causal Consistency:**
```python
class VectorClock:
    def __init__(self, clock_dict=None):
        self.clock = clock_dict or {}
    
    def increment(self, node_id):
        self.clock[node_id] = self.clock.get(node_id, 0) + 1
    
    def compare(self, other):
        """Returns: less_than, equal, greater_than, or concurrent"""
        pass
```

### 5. Monitoring & Alerting

**Sync Pipeline Metrics:**
```
- Event throughput: events/second
- End-to-end latency: p50, p95, p99
- Consumer lag: Events waiting to be processed
- Replication lag: Time for secondary synchronization
- Dead letter queue size: Failed events
- Conflict rate: Percentage of conflicting updates
```

---

## Multi-LLM Routing System

### 1. Architecture Overview

**Request Flow**
```
┌─────────────────────────────────┐
│    Code Analysis Request        │
└────────┬────────────────────────┘
         │
         ▼
┌─────────────────────────────────┐
│   LLM Router (Intelligent)      │
├─────────────────────────────────┤
│ • Request Analysis              │
│ • Model Selection               │
│ • Load Balancing                │
│ • Cost Optimization             │
└────────┬─────────┬──────────┬───┘
         │         │          │
    ┌────▼─┐  ┌────▼─┐  ┌────▼─┐
    │GPT-4 │  │Claude│  │Llama │
    │(Fast)│  │(Accu)│  │(Cost)│
    └──────┘  └──────┘  └──────┘
```

### 2. Model Selection Algorithm

**Scoring Function:**
```python
class LLMRouter:
    def select_model(self, request: AnalysisRequest) -> str:
        """
        Multi-objective optimization for model selection
        Factors: accuracy, cost, latency, queue depth
        """
        scores = {}
        
        for model in self.available_models:
            accuracy_score = self.get_model_accuracy(model, request.type)
            cost_score = self.get_model_cost(model)
            latency_score = self.estimate_latency(model)
            availability = self.check_availability(model)
            queue_depth = self.get_queue_depth(model)
            
            # Weighted scoring
            composite_score = (
                0.35 * accuracy_score +      # Accuracy is critical
                0.25 * (1 / (1 + cost_score)) +  # Cost inverse
                0.20 * (1 / (1 + latency_score)) +  # Lower latency better
                0.15 * availability +         # Availability bonus
                -0.05 * (queue_depth / MAX_QUEUE)   # Queue depth penalty
            )
            
            if composite_score > 0:
                scores[model] = composite_score
        
        return max(scores, key=scores.get)
```

**Model Characteristics:**
```yaml
GPT-4:
  Accuracy: 0.95
  Cost: $0.03 per 1K tokens
  Latency: 2-5 seconds
  Specializations: [code-review, architecture, complex-logic]
  Rate Limit: 3500 RPM

Claude 3 Opus:
  Accuracy: 0.93
  Cost: $0.015 per 1K tokens
  Latency: 1-3 seconds
  Specializations: [documentation, explanation, refactoring]
  Rate Limit: 5000 RPM

Llama 2 (Self-hosted):
  Accuracy: 0.80
  Cost: $0.001 per 1K tokens (amortized)
  Latency: 0.5-2 seconds
  Specializations: [simple-analysis, formatting]
  Rate Limit: Unlimited (self-hosted)
```

### 3. Load Balancing Strategy

**Request Distribution:**
```python
class LoadBalancer:
    def distribute_request(self, request):
        # Priority 1: Use local models (Llama) if suitable
        if self.can_handle_with_llama(request):
            return self.llama_queue.put(request)
        
        # Priority 2: Balance API calls based on cost/latency
        if request.priority == "high" or request.budget == "unlimited":
            return self.gpt4_queue.put(request)
        
        # Priority 3: Use cost-effective options
        if request.cost_sensitive:
            return self.claude_queue.put(request)
        
        # Default: Round-robin with weighted distribution
        return self.round_robin_with_weights(request)
    
    def round_robin_with_weights(self, request):
        weights = {
            "llama": 50,    # 50% - cost-effective
            "claude": 30,   # 30% - balanced
            "gpt4": 20      # 20% - premium
        }
        
        model = random.choices(
            list(weights.keys()),
            weights=list(weights.values())
        )[0]
        
        return self.route_to_model(model, request)
```

### 4. Fallback & Retry Logic

**Retry Strategy:**
```python
class FailoverHandler:
    async def execute_with_fallback(self, request, primary_model):
        fallback_models = self.get_fallback_order(primary_model)
        
        for attempt, model in enumerate([primary_model] + fallback_models):
            try:
                result = await self.call_model(model, request)
                
                # Log successful execution
                self.metrics.record_success(model, attempt)
                return result
                
            except TimeoutError:
                if attempt < len(fallback_models):
                    self.logger.warning(f"{model} timeout, trying {fallback_models[attempt]}")
                    continue
                raise
            
            except RateLimitError:
                # Switch to different model if rate-limited
                self.logger.info(f"{model} rate limited, routing to alternative")
                await asyncio.sleep(2 ** attempt)  # Exponential backoff
                continue
        
        raise Exception("All models failed")
    
    def get_fallback_order(self, primary_model):
        """Priority order for fallbacks"""
        fallback_chains = {
            "gpt4": ["claude", "llama"],
            "claude": ["gpt4", "llama"],
            "llama": ["claude", "gpt4"]
        }
        return fallback_chains.get(primary_model, [])
```

### 5. Cost Optimization

**Cost Tracking & Control:**
```python
class CostManager:
    def __init__(self, monthly_budget=10000):
        self.monthly_budget = monthly_budget
        self.spent = 0
        self.requests_by_model = defaultdict(int)
    
    async def charge_and_validate(self, model, tokens):
        cost = self.calculate_cost(model, tokens)
        
        if self.spent + cost > self.monthly_budget * 0.8:
            # Trigger warning at 80% budget
            self.logger.warning(f"Approaching budget limit: {self.spent}/{self.monthly_budget}")
            
            # Prefer cheaper models
            self.router.increase_weight("llama", 100)
            self.router.decrease_weight("gpt4", 50)
        
        if self.spent + cost > self.monthly_budget:
            raise BudgetExceededError("Monthly API budget exhausted")
        
        self.spent += cost
        self.requests_by_model[model] += 1
    
    def calculate_cost(self, model, tokens):
        rates = {
            "gpt4": 0.00003,  # $0.03 per 1K tokens
            "claude": 0.0000125,
            "llama": 0.000001
        }
        return tokens * rates.get(model, 0)
```

### 6. Monitoring & Analytics

**Key Metrics:**
```
- Model selection distribution: % requests per model
- Average response quality by model: Accuracy, relevance
- Cost per request: Aggregated and by model
- Queue depths: Requests waiting per model
- Error rates: Failures and timeouts per model
- Latency distribution: p50, p95, p99 per model
- Model availability: Uptime % for each service
```

---

## Caching Strategies

### 1. Multi-Layer Cache Architecture

**Cache Hierarchy**
```
┌────────────────────────────────────────┐
│     Level 1: In-Memory (L1)            │
│  (Process Memory, <1ms latency)        │
├────────────────────────────────────────┤
│     Level 2: Redis (L2)                │
│  (Distributed, 1-10ms latency)         │
├────────────────────────────────────────┤
│     Level 3: CDN Cache (L3)            │
│  (Edge, 10-100ms latency)              │
├────────────────────────────────────────┤
│     Level 4: Primary Database (DB)     │
│  (Source of truth, 100-1000ms)         │
└────────────────────────────────────────┘
```

### 2. Cache Types & Eviction Policies

**In-Memory Cache (L1):**
```python
from functools import lru_cache
from cachetools import TTLCache, LRUCache

class L1Cache:
    # User-specific cache with TTL
    user_profile_cache = TTLCache(
        maxsize=10000,
        ttl=300  # 5 minutes
    )
    
    # Frequently accessed data with LRU
    leaderboard_cache = LRUCache(
        maxsize=1000
    )
    
    # Hot code samples cache
    @lru_cache(maxsize=500)
    def get_code_sample(self, sample_id):
        return self.db.fetch_sample(sample_id)
```

**Distributed Cache (L2 - Redis):**
```yaml
Redis Cache Strategy:
  Structure:
    - String: Simple key-value (user profiles, configs)
    - Hash: Complex objects (execution results, metadata)
    - Sorted Set: Leaderboard rankings, time-series data
    - List: Queues, recent activity
    - Bloom Filter: Deduplication, membership testing
  
  Eviction Policies:
    - noeviction: Return errors when memory full (critical data)
    - allkeys-lru: Evict least-used keys (user sessions)
    - volatile-lru: Evict least-used keys with TTL set (cache layer)
    - volatile-ttl: Evict keys with shortest TTL remaining

  Memory Limits:
    Total Redis Memory: 32GB
    Leaderboard Data: 8GB
    Cache Data: 16GB
    Session Data: 4GB
    Ephemeral Data: 4GB
```

**Cache Key Design:**
```python
class CacheKeyGenerator:
    @staticmethod
    def user_profile_key(user_id: str) -> str:
        return f"user:profile:{user_id}"
    
    @staticmethod
    def execution_result_key(execution_id: str) -> str:
        return f"execution:result:{execution_id}"
    
    @staticmethod
    def leaderboard_key(challenge_id: str, limit: int = 100) -> str:
        return f"leaderboard:{challenge_id}:top{limit}"
    
    @staticmethod
    def code_analysis_key(code_hash: str, model: str) -> str:
        return f"analysis:{model}:{code_hash}"
```

### 3. Cache Invalidation Strategies

**Event-Based Invalidation:**
```python
class CacheInvalidationManager:
    async def invalidate_on_event(self, event: DataSyncEvent):
        """Invalidate caches based on data changes"""
        
        if event.entity_type == "user_profile":
            await self.invalidate_pattern(f"user:profile:{event.entity_id}")
        
        elif event.entity_type == "execution":
            await self.invalidate_pattern(f"execution:result:{event.entity_id}")
        
        elif event.entity_type == "leaderboard_entry":
            # Invalidate leaderboard caches for affected challenges
            affected_challenge = event.data["challenge_id"]
            await self.invalidate_pattern(f"leaderboard:{affected_challenge}:*")
        
        # Cascade invalidations
        if event.entity_type == "challenge":
            await self.invalidate_pattern(f"leaderboard:*")
            await self.invalidate_pattern(f"cache:code_samples:{event.entity_id}:*")
```

**TTL-Based Invalidation:**
```yaml
Cache TTLs:
  User Profile: 5 minutes
  User Preferences: 30 minutes
  Leaderboard Rankings: 1 minute
  Execution Results: 1 hour
  Code Samples: 1 day
  Challenge Metadata: 6 hours
  Computed Analytics: 5 minutes
```

**Pattern-Based Invalidation:**
```python
async def cache_update_pattern(self, pattern: str, data: dict):
    """Update cache using patterns"""
    
    # Find all matching keys
    keys = await self.redis.keys(pattern)
    
    if len(keys) > 1000:
        # Use SCAN for large sets
        cursor = 0
        while cursor is not None:
            cursor, keys = await self.redis.scan(
                cursor, 
                match=pattern, 
                count=100
            )
            await self.redis.delete(*keys)
    else:
        await self.redis.delete(*keys)
```

### 4. Cache Warming & Preloading

**Predictive Cache Warming:**
```python
class CacheWarmer:
    async def warm_cache(self):
        """Preload hot data into cache"""
        
        # Warm leaderboards
        challenges = await self.db.get_active_challenges()
        for challenge in challenges:
            leaderboard = await self.db.get_leaderboard(challenge.id, limit=100)
            await self.redis.set(
                f"leaderboard:{challenge.id}:top100",
                json.dumps(leaderboard),
                ex=60  # 1 minute TTL
            )
        
        # Warm user profiles
        active_users = await self.db.get_active_users(last_activity=3600)
        for user in active_users:
            profile = await self.db.get_user_profile(user.id)
            await self.redis.set(
                f"user:profile:{user.id}",
                json.dumps(profile),
                ex=300  # 5 minutes TTL
            )
    
    async def schedule_periodic_warming(self):
        """Warm cache every 5 minutes"""
        while True:
            try:
                await self.warm_cache()
            except Exception as e:
                self.logger.error(f"Cache warming failed: {e}")
            
            await asyncio.sleep(300)  # 5 minutes
```

### 5. Cache Performance Monitoring

**Metrics Tracking:**
```python
class CacheMetrics:
    def __init__(self):
        self.hits = 0
        self.misses = 0
        self.evictions = 0
    
    @property
    def hit_rate(self) -> float:
        total = self.hits + self.misses
        return self.hits / total if total > 0 else 0
    
    def record_hit(self, cache_level: str):
        self.hits += 1
        self.metrics.gauge(f"cache.{cache_level}.hit_rate", self.hit_rate)
    
    def record_miss(self, cache_level: str):
        self.misses += 1
        self.metrics.gauge(f"cache.{cache_level}.hit_rate", self.hit_rate)
```

**Target Hit Rates:**
- L1 (In-Memory): > 80%
- L2 (Redis): > 70%
- L3 (CDN): > 60%
- Overall: > 85%

---

## Real-Time Leaderboard Implementation

### 1. Leaderboard Data Model

**Core Schema:**
```sql
CREATE TABLE leaderboard_entries (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    challenge_id UUID NOT NULL,
    score INT NOT NULL,
    rank INT,
    submission_count INT DEFAULT 0,
    last_submission_at TIMESTAMP WITH TIME ZONE,
    best_solution_time INT,  -- milliseconds
    created_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE,
    UNIQUE(user_id, challenge_id),
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (challenge_id) REFERENCES challenges(id)
);

CREATE INDEX idx_leaderboard_rank ON leaderboard_entries(challenge_id, rank);
CREATE INDEX idx_leaderboard_user ON leaderboard_entries(user_id, challenge_id);
CREATE INDEX idx_leaderboard_updated ON leaderboard_entries(updated_at DESC);
```

**Ranking Calculation:**
```python
class LeaderboardRanker:
    def calculate_score(self, entry):
        """
        Composite scoring: accuracy + speed + attempt efficiency
        """
        base_score = entry["correctness_score"] * 100
        
        # Bonus for solve speed
        speed_bonus = max(0, 60 - entry["solve_time_minutes"]) * 10
        
        # Penalty for attempts
        attempt_penalty = max(0, entry["submission_count"] - 1) * 5
        
        final_score = base_score + speed_bonus - attempt_penalty
        return max(0, final_score)
    
    async def calculate_rank(self, challenge_id, user_id):
        """Calculate percentile rank"""
        higher_scores = await self.db.query(
            """
            SELECT COUNT(*) FROM leaderboard_entries
            WHERE challenge_id = %s AND score > (
                SELECT score FROM leaderboard_entries
                WHERE challenge_id = %s AND user_id = %s
            )
            """,
            (challenge_id, challenge_id, user_id)
        )
        total = await self.db.query(
            "SELECT COUNT(*) FROM leaderboard_entries WHERE challenge_id = %s",
            (challenge_id,)
        )
        return higher_scores[0][0] + 1, total[0][0]
```

### 2. Real-Time Update Architecture

**WebSocket Connection Flow:**
```
┌─────────────────────────────────────┐
│  Client (Browser)                   │
├─────────────────────────────────────┤
│  WebSocket Connection (persistent)  │
└────────────┬────────────────────────┘
             │
             │ Subscribe to leaderboard updates
             ▼
┌─────────────────────────────────────┐
│  WebSocket Gateway (Load Balanced)  │
├─────────────────────────────────────┤
│  Connection Pool: 100K+ concurrent  │
└────────────┬────────────────────────┘
             │
             │ Router/Demultiplexer
             ▼
┌────────────────────────────────────────┐
│  Message Broker (Redis Pub/Sub)        │
├────────────────────────────────────────┤
│  Channels:                             │
│  - leaderboard:challenge:{id}          │
│  - leaderboard:user:{id}               │
│  - leaderboard:global                  │
└────────────────────────────────────────┘
```

**WebSocket Implementation:**
```python
from fastapi import WebSocket, WebSocketDisconnect
import asyncio
import json

class LeaderboardWebSocketManager:
    def __init__(self):
        self.active_connections: dict[str, list[WebSocket]] = {}
        self.redis = None
    
    async def connect(self, websocket: WebSocket, challenge_id: str):
        await websocket.accept()
        
        if challenge_id not in self.active_connections:
            self.active_connections[challenge_id] = []
        
        self.active_connections[challenge_id].append(websocket)
        
        # Subscribe to Redis channel
        await self.subscribe_to_updates(challenge_id)
    
    async def disconnect(self, websocket: WebSocket, challenge_id: str):
        self.active_connections[challenge_id].remove(websocket)
        
        if not self.active_connections[challenge_id]:
            del self.active_connections[challenge_id]
    
    async def broadcast_update(self, challenge_id: str, update: dict):
        """Send update to all connected clients"""
        if challenge_id in self.active_connections:
            disconnected = []
            
            for websocket in self.active_connections[challenge_id]:
                try:
                    await websocket.send_json({
                        "type": "leaderboard_update",
                        "timestamp": datetime.utcnow().isoformat(),
                        "data": update
                    })
                except Exception as e:
                    self.logger.error(f"Failed to send update: {e}")
                    disconnected.append(websocket)
            
            # Clean up disconnected clients
            for ws in disconnected:
                await self.disconnect(ws, challenge_id)
    
    async def subscribe_to_updates(self, challenge_id: str):
        """Listen to Redis pub/sub for updates"""
        pubsub = self.redis.pubsub()
        await pubsub.subscribe(f"leaderboard:challenge:{challenge_id}")
        
        async for message in pubsub.listen():
            if message["type"] == "message":
                update = json.loads(message["data"])
                await self.broadcast_update(challenge_id, update)
```

### 3. Update Batching & Rate Limiting

**Batched Updates:**
```python
class LeaderboardUpdateBatcher:
    def __init__(self, batch_size=100, batch_timeout_ms=500):
        self.batch_size = batch_size
        self.batch_timeout = batch_timeout_ms / 1000
        self.batches = {}
        self.batch_timers = {}
    
    async def queue_update(self, challenge_id: str, entry: dict):
        """Queue an update and batch with others"""
        
        if challenge_id not in self.batches:
            self.batches[challenge_id] = []
            self.schedule_batch_flush(challenge_id)
        
        self.batches[challenge_id].append(entry)
        
        # Flush if batch size reached
        if len(self.batches[challenge_id]) >= self.batch_size:
            await self.flush_batch(challenge_id)
    
    async def flush_batch(self, challenge_id: str):
        """Send batched updates to clients"""
        if challenge_id not in self.batches:
            return
        
        updates = self.batches[challenge_id]
        self.batches[challenge_id] = []
        
        # Cancel pending timer
        if challenge_id in self.batch_timers:
            self.batch_timers[challenge_id].cancel()
        
        # Consolidate updates
        consolidated = self.consolidate_updates(updates)
        
        # Broadcast
        await self.broadcast_to_clients(challenge_id, consolidated)
    
    def schedule_batch_flush(self, challenge_id: str):
        """Schedule automatic flush after timeout"""
        async def auto_flush():
            await asyncio.sleep(self.batch_timeout)
            await self.flush_batch(challenge_id)
        
        self.batch_timers[challenge_id] = asyncio.create_task(auto_flush())
```

**Rate Limiting Updates:**
```python
class UpdateRateLimiter:
    def __init__(self, updates_per_second=10):
        self.rate = updates_per_second
        self.tokens = updates_per_second
        self.last_refill = time.time()
    
    def should_send_update(self, challenge_id) -> bool:
        """Token bucket algorithm for rate limiting"""
        now = time.time()
        elapsed = now - self.last_refill
        
        # Refill tokens
        self.tokens = min(
            self.rate,
            self.tokens + elapsed * self.rate
        )
        self.last_refill = now
        
        if self.tokens >= 1:
            self.tokens -= 1
            return True
        
        return False
```

### 4. Incremental & Delta Updates

**Delta Calculation:**
```python
class LeaderboardDeltaCalculator:
    async def calculate_delta(
        self,
        challenge_id: str,
        previous_state: List[dict],
        new_state: List[dict]
    ) -> dict:
        """Calculate minimal changes between states"""
        
        prev_dict = {e["user_id"]: e for e in previous_state}
        new_dict = {e["user_id"]: e for e in new_state}
        
        delta = {
            "updated": [],
            "added": [],
            "removed": []
        }
        
        # Find updated and added entries
        for user_id, new_entry in new_dict.items():
            prev_entry = prev_dict.get(user_id)
            
            if not prev_entry:
                delta["added"].append(new_entry)
            elif prev_entry != new_entry:
                # Only include changed fields
                changes = {}
                for key in new_entry:
                    if new_entry[key] != prev_entry[key]:
                        changes[key] = {
                            "old": prev_entry[key],
                            "new": new_entry[key]
                        }
                
                delta["updated"].append({
                    "user_id": user_id,
                    "changes": changes
                })
        
        # Find removed entries
        for user_id in prev_dict:
            if user_id not in new_dict:
                delta["removed"].append(user_id)
        
        return delta
```

### 5. Consistency & Synchronization

**Eventual Consistency with Sync Mechanism:**
```python
class LeaderboardSynchronizer:
    async def sync_leaderboard(self, challenge_id: str):
        """Periodic full sync to ensure consistency"""
        
        # Get canonical state from database
        db_leaderboard = await self.db.get_leaderboard(challenge_id)
        
        # Get current cached state
        cache_leaderboard = await self.cache.get(
            f"leaderboard:{challenge_id}:top100"
        )
        
        if cache_leaderboard != db_leaderboard:
            # Recalculate all ranks
            ranked_entries = self.calculate_ranks(db_leaderboard)
            
            # Update cache
            await self.cache.set(
                f"leaderboard:{challenge_id}:top100",
                ranked_entries,
                ex=60
            )
            
            # Notify all connected clients
            await self.broadcast_sync_update(challenge_id, ranked_entries)
            
            self.logger.warning(
                f"Leaderboard {challenge_id} out of sync, resynced"
            )
    
    async def schedule_periodic_sync(self):
        """Sync every 5 minutes"""
        while True:
            challenges = await self.db.get_all_challenges()
            for challenge in challenges:
                await self.sync_leaderboard(challenge.id)
            
            await asyncio.sleep(300)  # 5 minutes
```

### 6. Performance Optimization

**Redis Sorted Set Usage:**
```python
class RedisLeaderboard:
    async def add_entry(self, challenge_id: str, user_id: str, score: int):
        """Add/update leaderboard entry in Redis"""
        
        key = f"leaderboard:{challenge_id}"
        
        # Use sorted set with score as member value
        await self.redis.zadd(
            key,
            {user_id: score},
            xx=False  # Create if not exists
        )
        
        # Set expiration (6 hours)
        await self.redis.expire(key, 21600)
    
    async def get_top_n(self, challenge_id: str, n: int = 100):
        """Get top N entries efficiently"""
        
        key = f"leaderboard:{challenge_id}"
        
        # ZREVRANGE returns in descending order
        top_users = await self.redis.zrevrange(
            key,
            0,
            n - 1,
            withscores=True
        )
        
        return [
            {"user_id": user_id, "score": score}
            for user_id, score in top_users
        ]
    
    async def get_rank(self, challenge_id: str, user_id: str) -> int:
        """Get user rank in O(log N)"""
        
        key = f"leaderboard:{challenge_id}"
        rank = await self.redis.zrevrank(key, user_id)
        
        return rank + 1 if rank is not None else None
```

---

## System Architecture Diagram

```
┌──────────────────────────────────────────────────────────────────────┐
│                         Client Layer                                  │
├──────────────────────────────────────────────────────────────────────┤
│  Web UI (React)  │  Mobile App  │  IDE Extensions  │  REST Clients   │
└──────────────────────────────────────────────────────────────────────┘
                               │
                               ▼
┌──────────────────────────────────────────────────────────────────────┐
│                       API Gateway Layer                               │
├──────────────────────────────────────────────────────────────────────┤
│  Load Balancer (nginx) → Rate Limiting → Auth → Request Routing     │
└──────────────────────────────────────────────────────────────────────┘
                               │
                ┌──────────────┼──────────────┐
                │              │              │
                ▼              ▼              ▼
        ┌──────────────┐ ┌──────────────┐ ┌──────────────┐
        │   API Srv    │ │   WebSocket  │ │   Execution  │
        │   (REST)     │ │   Gateway    │ │   Service    │
        └──────────────┘ └──────────────┘ └──────────────┘
                │              │                   │
                └──────────────┼───────────────────┘
                               │
                ┌──────────────┴──────────────┐
                │                             │
                ▼                             ▼
        ┌─────────────────┐          ┌──────────────────┐
        │  Cache Layer    │          │  Sandbox Layer   │
        ├─────────────────┤          ├──────────────────┤
        │ L1: In-Memory   │          │  Docker (100+)   │
        │ L2: Redis (32GB)│          │  Container Mgmt  │
        │ L3: CDN         │          └──────────────────┘
        └─────────────────┘
                │
                ▼
        ┌─────────────────────────┐
        │   Event Broker          │
        │  (Kafka / RabbitMQ)     │
        └─────────────────────────┘
         │        │        │         │
         ▼        ▼        ▼         ▼
     Database  Analytics  Cache   Notifications
     Replicas  Pipeline   Warmer  Service
         │        │        │         │
         └────────┼────────┼─────────┘
                  │
                  ▼
        ┌──────────────────────┐
        │   Data Layer         │
        ├──────────────────────┤
        │ PostgreSQL (Primary) │
        │ PostgreSQL (Replicas)│
        │ MongoDB (Documents)  │
        └──────────────────────┘
                  │
                  ▼
        ┌──────────────────────┐
        │  External Services   │
        ├──────────────────────┤
        │ • OpenAI (GPT-4)     │
        │ • Anthropic (Claude) │
        │ • Self-hosted LLM    │
        │ • GitHub API         │
        └──────────────────────┘
```

---

## Deployment Considerations

### 1. Scaling Strategy

**Horizontal Scaling:**
- **Stateless Services**: API servers, execution workers, cache services scale independently
- **Load Distribution**: Round-robin with sticky sessions for WebSocket connections
- **Auto-scaling**: Based on CPU (60-80%), memory (70-85%), and queue depth metrics

**Vertical Scaling Limits:**
- Docker host: 64GB RAM, 32 CPU cores
- Each service instance: 2GB RAM minimum

### 2. Disaster Recovery

**Backup Strategy:**
- Database backups: Hourly snapshots, 30-day retention
- Point-in-time recovery: Available for 7 days
- Execution logs: Archived to S3 after 30 days

**Failover:**
- RTO (Recovery Time Objective): 5 minutes
- RPO (Recovery Point Objective): 1 minute
- Automated database failover with Read replicas
- Cache reconstruction on startup

### 3. Security Considerations

- All communication encrypted with TLS 1.3
- Database encryption at rest (AES-256)
- Container image scanning for vulnerabilities
- Network isolation with VPC and security groups
- Regular penetration testing and security audits

### 4. Monitoring & Observability

**Metrics Collection:**
```
- Prometheus: Metrics scraping every 15 seconds
- ELK Stack: Centralized logging
- Jaeger: Distributed tracing
- Custom dashboards in Grafana
```

**Key SLOs:**
- API Availability: 99.9% uptime
- Leaderboard Update Latency: p99 < 2 seconds
- Code Execution: p99 < 30 seconds
- Cache Hit Rate: > 85%

---

## References & Further Reading

- [Docker Security Best Practices](https://docs.docker.com/engine/security/)
- [Kafka Event Streaming](https://kafka.apache.org/intro)
- [Redis Caching Strategies](https://redis.io/)
- [WebSocket Real-Time Architecture](https://developer.mozilla.org/en-US/docs/Web/API/WebSocket)
- [LLM API Integration Patterns](https://openai.com/documentation)

---

**Document Version**: 1.0  
**Last Updated**: 2026-01-05  
**Author**: CodeHive Architecture Team
