# 实时通知系统技术设计文档

> 适用项目：前后端分离 Spring Boot 3 + Vue 3  
> 通知推送方式：SSE（Server-Sent Events）  
> 异步解耦：RabbitMQ  
> 持久化：MySQL + Redis

---

## 一、整体架构

```
客户端(Vue3)
   │
   ├─ 提交审核 ──────────────────────────────────► ReviewController
   │                                                      │
   │                                               ReviewService
   │                                                      │
   │                                         ┌────────────┤
   │                                         ▼            ▼
   │                                      写 MySQL   发 RabbitMQ
   │                                                      │
   │                                               NotificationConsumer
   │                                                      │
   │                                         ┌────────────┼────────────┐
   │                                         ▼            ▼            ▼
   │                                      写 MySQL   Redis +1    SseEmitterManager
   │                                                                    │
   └─ SSE 长连接 ◄──────────────────────────────────────────────────────┘
        │
   通知铃铛 +1 / 弹出通知卡片
```

**多实例部署补充：**  
若部署多个后端实例，SSE 连接分散在各节点。Consumer 消费 MQ 后，需通过 **Redis Pub/Sub** 广播给所有节点，各节点判断本地是否持有目标用户连接再推送。

---

## 二、消息类型定义

### 2.1 通知类型枚举

```java
public enum NotificationType {

    // ─── 审核相关 ───────────────────────────────────────────
    REVIEW_PASSED("审核通过"),
    REVIEW_REJECTED("审核拒绝"),
    REVIEW_RECALLED("审核撤回"),

    // ─── 系统消息 ───────────────────────────────────────────
    SYSTEM_ANNOUNCEMENT("系统公告"),
    SYSTEM_MAINTENANCE("系统维护通知"),

    // ─── 业务操作 ───────────────────────────────────────────
    TASK_ASSIGNED("任务分配"),
    TASK_COMPLETED("任务完成"),
    COMMENT_RECEIVED("收到评论"),

    // ─── 告警通知 ───────────────────────────────────────────
    ALERT_WARNING("预警通知"),
    ALERT_CRITICAL("严重告警");

    private final String desc;

    NotificationType(String desc) { this.desc = desc; }
    public String getDesc() { return desc; }
}
```

### 2.2 RabbitMQ Exchange / Queue 规划

```
Exchange: notification.topic  (topic 类型)

routing key 规则：notify.{type}

Queue 列表：
  notify.review.queue     → binding: notify.REVIEW_*
  notify.system.queue     → binding: notify.SYSTEM_*
  notify.business.queue   → binding: notify.TASK_* / notify.COMMENT_*
  notify.alert.queue      → binding: notify.ALERT_*

死信 Exchange: notification.dlx
死信 Queue:    notification.dead.queue
```

### 2.3 MQ 消息体

```java
@Data
@Builder
public class NotificationMessage implements Serializable {

    /** 目标用户 ID */
    private Long userId;

    /** 通知类型，对应 NotificationType 枚举 */
    private String type;

    /** 通知标题 */
    private String title;

    /** 通知正文 */
    private String content;

    /** 前端跳转路径，如 /review/detail/123 */
    private String linkUrl;

    /** 业务数据 ID（可选），用于前端精准跳转 */
    private Long bizId;

    /** 消息发送时间戳 */
    private Long timestamp;
}
```

---

## 三、数据库设计

### 3.1 notification 表

```sql
CREATE TABLE notification
(
    id         BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '通知 ID',
    user_id    BIGINT       NOT NULL COMMENT '接收用户 ID',
    type       VARCHAR(32)  NOT NULL COMMENT '通知类型，对应 NotificationType',
    title      VARCHAR(100) NOT NULL COMMENT '通知标题',
    content    VARCHAR(500) COMMENT '通知内容',
    link_url   VARCHAR(255) COMMENT '跳转链接',
    biz_id     BIGINT COMMENT '关联业务 ID',
    is_read    TINYINT      NOT NULL DEFAULT 0 COMMENT '是否已读：0未读 1已读',
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    read_at    DATETIME COMMENT '已读时间',
    INDEX idx_user_unread (user_id, is_read),
    INDEX idx_user_created (user_id, created_at)
) COMMENT '通知记录表';
```

### 3.2 Redis Key 规范

| Key 格式                     | 类型   | 说明                       | TTL    |
|------------------------------|--------|----------------------------|--------|
| `notify:unread:{userId}`     | String | 用户未读通知总数           | 永久   |
| `notify:sse:nodes:{userId}`  | Set    | 多实例下用户连接所在节点 IP | 1 小时 |

---

## 四、后端实现

### 4.1 RabbitMQ 配置

```java
@Configuration
public class NotificationMqConfig {

    public static final String EXCHANGE = "notification.topic";
    public static final String DLX      = "notification.dlx";

    // ── Exchanges ──────────────────────────────────────────────────────────
    @Bean
    public TopicExchange notificationExchange() {
        return ExchangeBuilder.topicExchange(EXCHANGE).durable(true).build();
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return ExchangeBuilder.directExchange(DLX).durable(true).build();
    }

    // ── Queues ────────────────────────────────────────────────────────────
    private Queue buildQueue(String name) {
        return QueueBuilder.durable(name)
                .withArgument("x-dead-letter-exchange", DLX)
                .withArgument("x-dead-letter-routing-key", "dead")
                .withArgument("x-message-ttl", 60000)    // 消息 60s 未消费进死信
                .build();
    }

    @Bean public Queue reviewQueue()   { return buildQueue("notify.review.queue");   }
    @Bean public Queue systemQueue()   { return buildQueue("notify.system.queue");   }
    @Bean public Queue businessQueue() { return buildQueue("notify.business.queue"); }
    @Bean public Queue alertQueue()    { return buildQueue("notify.alert.queue");    }

    @Bean public Queue deadLetterQueue() {
        return QueueBuilder.durable("notification.dead.queue").build();
    }

    // ── Bindings ──────────────────────────────────────────────────────────
    @Bean public Binding reviewBinding()   { return BindingBuilder.bind(reviewQueue())  .to(notificationExchange()).with("notify.REVIEW_*"); }
    @Bean public Binding systemBinding()   { return BindingBuilder.bind(systemQueue())  .to(notificationExchange()).with("notify.SYSTEM_*"); }
    @Bean public Binding businessBinding() { return BindingBuilder.bind(businessQueue()).to(notificationExchange()).with("notify.TASK_*");   }
    @Bean public Binding commentBinding()  { return BindingBuilder.bind(businessQueue()).to(notificationExchange()).with("notify.COMMENT_*");}
    @Bean public Binding alertBinding()    { return BindingBuilder.bind(alertQueue())   .to(notificationExchange()).with("notify.ALERT_*");  }
    @Bean public Binding deadBinding()     { return BindingBuilder.bind(deadLetterQueue()).to(deadLetterExchange()).with("dead"); }
}
```

### 4.2 SSE 连接管理器

```java
@Component
@Slf4j
public class SseEmitterManager {

    /** key: userId，支持同一用户多标签页 */
    private final Map<Long, CopyOnWriteArrayList<SseEmitter>> registry = new ConcurrentHashMap<>();

    /**
     * 用户建立 SSE 连接（前端调用 /api/sse/connect 时触发）
     */
    public SseEmitter connect(Long userId) {
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L); // 30 分钟超时

        registry.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        Runnable remove = () -> removeEmitter(userId, emitter);
        emitter.onCompletion(remove);
        emitter.onTimeout(remove);
        emitter.onError(e -> remove.run());

        log.info("[SSE] 用户 {} 建立连接，当前连接数: {}", userId,
                registry.getOrDefault(userId, new CopyOnWriteArrayList<>()).size());
        return emitter;
    }

    /**
     * 向指定用户推送通知（所有标签页均会收到）
     */
    public void push(Long userId, NotificationVO vo) {
        List<SseEmitter> emitters = registry.get(userId);
        if (emitters == null || emitters.isEmpty()) {
            log.debug("[SSE] 用户 {} 无在线连接，跳过推送", userId);
            return;
        }

        String payload = JSON.toJSONString(vo);
        emitters.removeIf(emitter -> {
            try {
                emitter.send(SseEmitter.event()
                        .id(String.valueOf(vo.getId()))
                        .name("notification")           // 前端监听 addEventListener('notification', ...)
                        .data(payload));
                return false;
            } catch (Exception e) {
                log.warn("[SSE] 推送失败，移除失效连接: userId={}", userId);
                return true;
            }
        });
    }

    private void removeEmitter(Long userId, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> list = registry.get(userId);
        if (list != null) {
            list.remove(emitter);
            if (list.isEmpty()) registry.remove(userId);
        }
    }
}
```

### 4.3 SSE Controller

```java
@RestController
@RequestMapping("/api/sse")
public class SseController {

    @Autowired private SseEmitterManager sseEmitterManager;

    /**
     * 前端在页面初始化时调用，建立长连接
     * GET /api/sse/connect
     */
    @GetMapping(value = "/connect", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter connect(@RequestParam Long userId) {
        return sseEmitterManager.connect(userId);
    }
}
```

### 4.4 通知服务

```java
@Service
@Slf4j
public class NotificationService {

    @Autowired private NotificationMapper     notificationMapper;
    @Autowired private StringRedisTemplate    redisTemplate;
    @Autowired private SseEmitterManager      sseEmitterManager;
    @Autowired private RabbitTemplate         rabbitTemplate;

    /**
     * 发布通知消息到 MQ（由业务服务调用，如 ReviewService.approve()）
     */
    public void publish(NotificationMessage msg) {
        msg.setTimestamp(System.currentTimeMillis());
        String routingKey = "notify." + msg.getType();
        rabbitTemplate.convertAndSend(NotificationMqConfig.EXCHANGE, routingKey, msg);
        log.info("[Notification] 已发布消息: type={}, userId={}", msg.getType(), msg.getUserId());
    }

    /**
     * 消费 MQ 消息后的核心处理：持久化 + Redis + SSE 推送
     * 由 NotificationConsumer 调用
     */
    public void process(NotificationMessage msg) {
        // 1. 持久化到 MySQL
        Notification entity = toEntity(msg);
        notificationMapper.insert(entity);

        // 2. Redis 未读数 +1
        String redisKey = "notify:unread:" + msg.getUserId();
        redisTemplate.opsForValue().increment(redisKey);

        // 3. 组装 VO，推送 SSE
        NotificationVO vo = toVO(entity);
        sseEmitterManager.push(msg.getUserId(), vo);
    }

    /** 标记已读（前端点击通知时调用） */
    @Transactional
    public void markRead(Long notificationId, Long userId) {
        notificationMapper.markRead(notificationId, userId);
        // 未读数 -1，最低归 0
        String key = "notify:unread:" + userId;
        Long current = Long.parseLong(Optional.ofNullable(redisTemplate.opsForValue().get(key)).orElse("0"));
        if (current > 0) {
            redisTemplate.opsForValue().decrement(key);
        }
    }

    /** 全部已读 */
    @Transactional
    public void markAllRead(Long userId) {
        notificationMapper.markAllRead(userId);
        redisTemplate.delete("notify:unread:" + userId);
    }

    /** 获取未读数（从 Redis，没有则查库并回填） */
    public long getUnreadCount(Long userId) {
        String key = "notify:unread:" + userId;
        String val = redisTemplate.opsForValue().get(key);
        if (val != null) return Long.parseLong(val);
        long count = notificationMapper.countUnread(userId);
        redisTemplate.opsForValue().set(key, String.valueOf(count));
        return count;
    }

    private Notification toEntity(NotificationMessage msg) {
        Notification n = new Notification();
        n.setUserId(msg.getUserId());
        n.setType(msg.getType());
        n.setTitle(msg.getTitle());
        n.setContent(msg.getContent());
        n.setLinkUrl(msg.getLinkUrl());
        n.setBizId(msg.getBizId());
        n.setIsRead(0);
        return n;
    }

    private NotificationVO toVO(Notification n) {
        NotificationVO vo = new NotificationVO();
        BeanUtils.copyProperties(n, vo);
        return vo;
    }
}
```

### 4.5 MQ 消费者（分类型路由）

```java
@Component
@Slf4j
public class NotificationConsumer {

    @Autowired private NotificationService notificationService;

    // 审核类通知
    @RabbitListener(queues = "notify.review.queue")
    public void onReview(NotificationMessage msg, Channel channel,
                         @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {
        handle(msg, channel, tag, "REVIEW");
    }

    // 系统类通知
    @RabbitListener(queues = "notify.system.queue")
    public void onSystem(NotificationMessage msg, Channel channel,
                         @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {
        handle(msg, channel, tag, "SYSTEM");
    }

    // 业务类通知（任务、评论）
    @RabbitListener(queues = "notify.business.queue")
    public void onBusiness(NotificationMessage msg, Channel channel,
                           @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {
        handle(msg, channel, tag, "BUSINESS");
    }

    // 告警类通知
    @RabbitListener(queues = "notify.alert.queue")
    public void onAlert(NotificationMessage msg, Channel channel,
                        @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {
        handle(msg, channel, tag, "ALERT");
    }

    // 死信队列：记录日志或人工干预
    @RabbitListener(queues = "notification.dead.queue")
    public void onDeadLetter(Message raw) {
        log.error("[Notification-DLQ] 死信消息: {}", new String(raw.getBody()));
        // TODO: 告警钉钉 / 写失败记录表
    }

    private void handle(NotificationMessage msg, Channel channel, long tag, String category) throws IOException {
        try {
            log.info("[Notification-{}] 处理消息: type={}, userId={}", category, msg.getType(), msg.getUserId());
            notificationService.process(msg);
            channel.basicAck(tag, false);
        } catch (Exception e) {
            log.error("[Notification-{}] 处理失败，拒绝消息（进死信）: {}", category, e.getMessage(), e);
            channel.basicNack(tag, false, false); // 不重新入队，直接进死信
        }
    }
}
```

### 4.6 业务服务调用示例（以审核为例）

```java
@Service
public class ReviewService {

    @Autowired private ReviewMapper          reviewMapper;
    @Autowired private NotificationService   notificationService;

    @Transactional
    public void approve(Long reviewId, Long operatorId) {
        ReviewRecord review = reviewMapper.selectById(reviewId);
        review.setStatus(ReviewStatus.PASSED);
        review.setOperatorId(operatorId);
        reviewMapper.updateById(review);

        // 发布通知到 MQ
        notificationService.publish(NotificationMessage.builder()
                .userId(review.getSubmitterId())
                .type(NotificationType.REVIEW_PASSED.name())
                .title("审核通过")
                .content("您提交的「" + review.getTitle() + "」已审核通过")
                .linkUrl("/review/detail/" + reviewId)
                .bizId(reviewId)
                .build());
    }

    @Transactional
    public void reject(Long reviewId, Long operatorId, String reason) {
        ReviewRecord review = reviewMapper.selectById(reviewId);
        review.setStatus(ReviewStatus.REJECTED);
        review.setRejectReason(reason);
        reviewMapper.updateById(review);

        notificationService.publish(NotificationMessage.builder()
                .userId(review.getSubmitterId())
                .type(NotificationType.REVIEW_REJECTED.name())
                .title("审核未通过")
                .content("「" + review.getTitle() + "」未通过，原因：" + reason)
                .linkUrl("/review/detail/" + reviewId)
                .bizId(reviewId)
                .build());
    }
}
```

---

## 五、多实例部署：Redis Pub/Sub 广播

> 单机部署可跳过此节。

```java
// ── 发布端（NotificationService.process() 末尾追加）─────────────────────
String channel = "sse:push:" + msg.getUserId();
redisTemplate.convertAndSend(channel, JSON.toJSONString(toVO(entity)));

// ── 订阅端（每个实例各自监听）──────────────────────────────────────────
@Component
public class SsePushSubscriber implements MessageListener {

    @Autowired private SseEmitterManager sseEmitterManager;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String body    = new String(message.getBody());
        String channel = new String(message.getChannel());  // sse:push:{userId}
        Long userId    = Long.parseLong(channel.split(":")[2]);

        NotificationVO vo = JSON.parseObject(body, NotificationVO.class);
        // 只有本节点持有该用户连接时才推送
        sseEmitterManager.push(userId, vo);
    }
}

@Configuration
public class RedisSubscribeConfig {

    @Bean
    public RedisMessageListenerContainer listenerContainer(
            RedisConnectionFactory factory, SsePushSubscriber subscriber) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(factory);
        // 订阅所有用户的 SSE 推送频道
        container.addMessageListener(subscriber, new PatternTopic("sse:push:*"));
        return container;
    }
}
```

---

## 六、前端实现（Vue 3）

### 6.1 通知 Composable

```javascript
// composables/useNotification.js
import { ref, onUnmounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { markReadApi, getNotificationsApi, getUnreadCountApi } from '@/api/notification'

export function useNotification() {
    const unreadCount   = ref(0)
    const notifications = ref([])
    const visible       = ref(false)   // 通知下拉框是否展示
    let eventSource     = null

    // 建立 SSE 长连接
    const connect = () => {
        const userId = useUserStore().userId
        eventSource  = new EventSource(`/api/sse/connect?userId=${userId}`)

        eventSource.addEventListener('notification', (e) => {
            const item = JSON.parse(e.data)
            notifications.value.unshift(item)
            unreadCount.value++
            // 可在此触发桌面通知 Notification API
        })

        eventSource.onerror = () => {
            eventSource.close()
            setTimeout(connect, 3000) // 断线重连
        }
    }

    // 拉取通知列表
    const fetchList = async () => {
        const { data } = await getNotificationsApi({ page: 1, pageSize: 20 })
        notifications.value = data.records
    }

    // 拉取未读数（页面初始化）
    const fetchUnread = async () => {
        const { data } = await getUnreadCountApi()
        unreadCount.value = data
    }

    // 点击通知：标记已读 + 路由跳转
    const handleClick = async (item) => {
        if (!item.isRead) {
            await markReadApi(item.id)
            item.isRead = true
            unreadCount.value = Math.max(0, unreadCount.value - 1)
        }
        if (item.linkUrl) {
            window.location.href = item.linkUrl
        }
    }

    onUnmounted(() => eventSource?.close())

    return { unreadCount, notifications, visible, connect, fetchList, fetchUnread, handleClick }
}
```

### 6.2 铃铛组件（NavBell.vue）

```vue
<template>
  <div class="nav-bell" @click="toggle">
    <el-badge :value="unreadCount || ''" :hidden="unreadCount === 0">
      <el-icon size="20"><Bell /></el-icon>
    </el-badge>

    <!-- 通知下拉列表 -->
    <transition name="fade">
      <div v-if="visible" class="notification-dropdown" @click.stop>
        <div class="dropdown-header">
          <span>通知</span>
          <el-button text size="small" @click="markAll">全部已读</el-button>
        </div>

        <el-empty v-if="!notifications.length" description="暂无通知" :image-size="60" />

        <div v-for="item in notifications" :key="item.id"
             class="notify-item" :class="{ unread: !item.isRead }"
             @click="handleClick(item)">
          <el-tag :type="tagType(item.type)" size="small" class="type-tag">
            {{ typeLabel(item.type) }}
          </el-tag>
          <div class="notify-title">{{ item.title }}</div>
          <div class="notify-content">{{ item.content }}</div>
          <div class="notify-time">{{ formatTime(item.createdAt) }}</div>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { useNotification } from '@/composables/useNotification'
import { markAllReadApi } from '@/api/notification'

const {
  unreadCount, notifications, visible,
  connect, fetchList, fetchUnread, handleClick
} = useNotification()

const toggle = async () => {
  visible.value = !visible.value
  if (visible.value) await fetchList()
}

const markAll = async () => {
  await markAllReadApi()
  notifications.value.forEach(n => (n.isRead = true))
  unreadCount.value = 0
}

// 通知类型 → el-tag 颜色
const tagType = (type) => {
  if (type?.startsWith('REVIEW')) return 'primary'
  if (type?.startsWith('ALERT'))  return 'danger'
  if (type?.startsWith('SYSTEM')) return 'warning'
  return 'info'
}

// 通知类型 → 中文标签
const typeLabel = (type) => {
  const map = {
    REVIEW_PASSED:    '审核通过',
    REVIEW_REJECTED:  '审核拒绝',
    SYSTEM_ANNOUNCEMENT: '系统公告',
    TASK_ASSIGNED:    '任务分配',
    ALERT_WARNING:    '预警',
    ALERT_CRITICAL:   '严重告警',
  }
  return map[type] ?? type
}

const formatTime = (t) => t ? new Date(t).toLocaleString('zh-CN') : ''

onMounted(() => {
  connect()
  fetchUnread()
})
</script>
```

---

## 七、接口汇总

| 方法   | 路径                            | 说明               |
|--------|---------------------------------|--------------------|
| GET    | `/api/sse/connect?userId={id}`  | 建立 SSE 长连接     |
| GET    | `/api/notification/list`        | 分页获取通知列表    |
| GET    | `/api/notification/unread/count`| 获取未读数          |
| PUT    | `/api/notification/{id}/read`   | 单条标记已读        |
| PUT    | `/api/notification/read/all`    | 全部标记已读        |

---

## 八、application.yml 关键配置

```yaml
spring:
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest
    listener:
      simple:
        acknowledge-mode: manual        # 手动 ACK，配合 basicAck/basicNack
        prefetch: 10                    # 每次最多预取 10 条
        retry:
          enabled: false                # 不走 Spring 重试，失败直接进死信

  # Nginx 代理 SSE 时需关闭缓冲
  # proxy_buffering off;
  # proxy_cache off;
  # proxy_read_timeout 3600s;

  data:
    redis:
      host: localhost
      port: 6379
```

---

## 九、注意事项

1. **Nginx 代理 SSE**：必须在 location 块增加 `proxy_buffering off` 和 `proxy_read_timeout 3600s`，否则消息会被缓冲延迟。

2. **前端断线重连**：`EventSource` 浏览器原生支持自动重连，但重连间隔默认 3s，建议在 `onerror` 中手动控制退避重连（3s → 10s → 30s）。

3. **Token 鉴权**：SSE 是 GET 请求，无法自定义请求头，建议将 JWT 作为 query 参数传递（注意 HTTPS 保护），或使用 Cookie 方案。

4. **消息幂等**：Consumer 处理前建议用 `notification` 表的 `(user_id, biz_id, type)` 联合唯一索引做幂等校验，防止 MQ 重投导致重复通知。

5. **死信监控**：`notification.dead.queue` 建议接入钉钉/企微告警，出现死信说明业务处理异常，需人工介入。
