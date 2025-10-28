package cloud.apposs.websocket;

import cloud.apposs.cache.CacheConfig;
import cloud.apposs.cache.CacheConfig.JvmConfig;
import cloud.apposs.cache.CacheConfig.RedisConfig;
import cloud.apposs.cachex.CacheXConfig;
import cloud.apposs.configure.Value;
import cloud.apposs.logger.Appender;
import cloud.apposs.logger.Logger;
import cloud.apposs.websocket.protocol.JsonSupport;

import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

public class WSConfig {
    public static final String IO_MODE_NETTY = "netty";

    /**
     * 扫描基础包，必须配置，框架会自动扫描ServerEndpoint注解类
     */
    protected String basePackage;

    /**
     * 底层网格模型，默认是采用NETTY
     */
    protected String ioMode = IO_MODE_NETTY;

    /**
     * 绑定服务器地址
     */
    private String host = "0.0.0.0";
    /**
     * 绑定服务器端口
     */
    private int port = -1;
    /**
     * 绑定的主机列表
     */
    private InetSocketAddress bindSocketAddress;

    private int backlog = 1024;

    private boolean reuseAddress = true;

    /**
     * 开启此参数，那么客户端在每次发送数据时，无论数据包的大小都会将这些数据发送出 去
     * 参考：
     * http://blog.csdn.net/huang_xw/article/details/7340241
     * http://www.open-open.com/lib/view/open1412994697952.html
     */
    private boolean tcpNoDelay = true;

    /**
     * 多少个EventLoop轮询器，主要用于处理各自网络读写数据，
     * 当服务性能不足可提高此配置提升对网络IO的并发处理，但注意EventLoop业务层必须要做到异步，不能有同步阻塞请求
     */
    private int numOfGroup = Runtime.getRuntime().availableProcessors() + 1;

    /**
     * 是否开启HTTP压缩
     */
    private boolean httpCompression = true;

    /**
     * 是否开启线程池
     */
    private boolean executorOn = false;

    /**
     * 工作线程池数量
     */
    private int workerCount = Runtime.getRuntime().availableProcessors() << 1;

    /**
     * 是否输出系统信息
     */
    protected boolean showSysInfo = true;

    /**
     * 自定义JSON解析器
     */
    private JsonSupport jsonSupport;

    /**
     * 第一次建立TCP连接时数据传输之间超时时间，
     * 避免'silent channel'静默攻击导致的'Too many open files'问题
     */
    private int firstDataTimeout = 5000;

    /**
     * 是否将WS请求的HEADER KEY自动转换成小写，
     * 在查询header数据时直接用小写获取，无需遍历，便于提升性能，
     * 不过转换为小写业务传递的再获取的时候可能会踩坑，视业务特点而定
     */
    private boolean lowerHeaderKey = false;

    private boolean websocketCompression = true;

    /**
     * Maximum http content length limit
     */
    private int maxHttpContentLength = 2 * 1024 * 1024;

    /**
     * Maximum websocket frame content length limit
     */
    private int maxFramePayloadLength = 2 * 1024 * 1024;

    /**
     * 服务是否为只读
     */
    private boolean readonly;

    /**
     * 是否为调式模式
     */
    private boolean debug;

    /**
     * 服务编码
     */
    private String charset = "utf-8";

    /**
     * 发送数据缓存默认分配内存大小
     */
    private int bufferSize = 2 * 1024;
    /**
     * 是否直接使用堆内存
     */
    private boolean bufferDirect = false;

    /**
     * 是否保持服务器端长连接，不检查网络超时
     */
    private boolean keepAlive = false;

    /**
     * SSL证书相关配置
     */
    private String sslProtocol = "TLSv1";

    private boolean needClientAuth = false;

    private String keyStoreFormat = "JKS";
    private InputStream keyStore;
    private String keyStorePassword;

    private String trustStoreFormat = "JKS";
    private InputStream trustStore;
    private String trustStorePassword;

    /**
     * 数据源相关配置，支持多数据源配置，适用场景：
     * 1、固定数据存储用mysql源存储，文本存储用es源存储，便于文档可通过ES快速检索
     * 2、主从数据库读写分离，写用主库，读用从库，减少数据库压力，提升读性能
     */
    protected Map<String, ResourceConfig> resources;

    /**
     * 自定义业务握手传输数据
     */
    private final Map<String, String> handshakeParameter = new HashMap<String, String>(1);

    /**
     * 日志输出终端
     */
    private String logAppender = Appender.CONSOLE;

    /**
     * 日志输出级别，
     * FATAL（致命）、
     * ERROR（错误）、
     * WARN（警告）、
     * INFO（信息）、
     * DEBUG（调试）、
     * OFF（关闭），
     * 默认为INFO
     */
    private String logLevel = "INFO";

    /**
     * 日志的存储路径
     */
    private String logPath = "log";

    /**
     * 日志输出模板
     */
    private String logFormat = Logger.DEFAULT_LOG_FORMAT;

    /**
     * 业务自定义配置
     */
    protected Object options;

    public String getBasePackage() {
        return basePackage;
    }

    public void setBasePackage(String basePackage) {
        this.basePackage = basePackage;
    }

    public String getIoMode() {
        return ioMode;
    }

    public void setIoMode(String ioMode) {
        this.ioMode = ioMode;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public InetSocketAddress getBindSocketAddress() {
        return bindSocketAddress;
    }

    public void setBindSocketAddress(InetSocketAddress bindSocketAddress) {
        this.bindSocketAddress = bindSocketAddress;
    }

    public int getBacklog() {
        return backlog;
    }

    public void setBacklog(int backlog) {
        this.backlog = backlog;
    }

    public boolean isReuseAddress() {
        return reuseAddress;
    }

    public void setReuseAddress(boolean reuseAddress) {
        this.reuseAddress = reuseAddress;
    }

    public boolean isTcpNoDelay() {
        return tcpNoDelay;
    }

    public void setTcpNoDelay(boolean tcpNoDelay) {
        this.tcpNoDelay = tcpNoDelay;
    }

    public int getNumOfGroup() {
        return numOfGroup;
    }

    public void setNumOfGroup(int numOfGroup) {
        this.numOfGroup = numOfGroup;
    }

    public boolean isHttpCompression() {
        return httpCompression;
    }

    public void setHttpCompression(boolean httpCompression) {
        this.httpCompression = httpCompression;
    }

    public boolean isExecutorOn() {
        return executorOn;
    }

    public void setExecutorOn(boolean executorOn) {
        this.executorOn = executorOn;
    }

    public int getWorkerCount() {
        return workerCount;
    }

    public void setWorkerCount(int workerCount) {
        this.workerCount = workerCount;
    }

    public boolean isShowSysInfo() {
        return showSysInfo;
    }

    public void setShowSysInfo(boolean showSysInfo) {
        this.showSysInfo = showSysInfo;
    }

    public JsonSupport getJsonSupport() {
        return jsonSupport;
    }

    public void setJsonSupport(JsonSupport jsonSupport) {
        this.jsonSupport = jsonSupport;
    }

    public int getFirstDataTimeout() {
        return firstDataTimeout;
    }

    public void setFirstDataTimeout(int firstDataTimeout) {
        this.firstDataTimeout = firstDataTimeout;
    }

    public boolean isLowerHeaderKey() {
        return lowerHeaderKey;
    }

    public void setLowerHeaderKey(boolean lowerHeaderKey) {
        this.lowerHeaderKey = lowerHeaderKey;
    }

    public boolean isWebsocketCompression() {
        return websocketCompression;
    }

    public void setWebsocketCompression(boolean websocketCompression) {
        this.websocketCompression = websocketCompression;
    }

    public int getMaxHttpContentLength() {
        return maxHttpContentLength;
    }

    public void setMaxHttpContentLength(int maxHttpContentLength) {
        this.maxHttpContentLength = maxHttpContentLength;
    }

    public int getMaxFramePayloadLength() {
        return maxFramePayloadLength;
    }

    public void setMaxFramePayloadLength(int maxFramePayloadLength) {
        this.maxFramePayloadLength = maxFramePayloadLength;
    }

    public boolean isReadonly() {
        return readonly;
    }

    public void setReadonly(boolean readonly) {
        this.readonly = readonly;
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public String getCharset() {
        return charset;
    }

    public void setCharset(String charset) {
        this.charset = charset;
    }

    public int getBufferSize() {
        return bufferSize;
    }

    public void setBufferSize(int bufferSize) {
        this.bufferSize = bufferSize;
    }

    public boolean isBufferDirect() {
        return bufferDirect;
    }

    public void setBufferDirect(boolean bufferDirect) {
        this.bufferDirect = bufferDirect;
    }

    public boolean isKeepAlive() {
        return keepAlive;
    }

    public void setKeepAlive(boolean keepAlive) {
        this.keepAlive = keepAlive;
    }

    public String getSslProtocol() {
        return sslProtocol;
    }

    public void setSslProtocol(String sslProtocol) {
        this.sslProtocol = sslProtocol;
    }

    public boolean isNeedClientAuth() {
        return needClientAuth;
    }

    public void setNeedClientAuth(boolean needClientAuth) {
        this.needClientAuth = needClientAuth;
    }

    public String getKeyStoreFormat() {
        return keyStoreFormat;
    }

    public void setKeyStoreFormat(String keyStoreFormat) {
        this.keyStoreFormat = keyStoreFormat;
    }

    public InputStream getKeyStore() {
        return keyStore;
    }

    public void setKeyStore(InputStream keyStore) {
        this.keyStore = keyStore;
    }

    public String getKeyStorePassword() {
        return keyStorePassword;
    }

    public void setKeyStorePassword(String keyStorePassword) {
        this.keyStorePassword = keyStorePassword;
    }

    public String getTrustStoreFormat() {
        return trustStoreFormat;
    }

    public void setTrustStoreFormat(String trustStoreFormat) {
        this.trustStoreFormat = trustStoreFormat;
    }

    public InputStream getTrustStore() {
        return trustStore;
    }

    public void setTrustStore(InputStream trustStore) {
        this.trustStore = trustStore;
    }

    public String getTrustStorePassword() {
        return trustStorePassword;
    }

    public void setTrustStorePassword(String trustStorePassword) {
        this.trustStorePassword = trustStorePassword;
    }

    public Map<String, String> getHandshakeParameter() {
        return handshakeParameter;
    }

    public String getLogAppender() {
        return logAppender;
    }

    public void setLogAppender(String logAppender) {
        this.logAppender = logAppender;
    }

    public String getLogLevel() {
        return logLevel;
    }

    public void setLogLevel(String logLevel) {
        this.logLevel = logLevel;
    }

    public String getLogPath() {
        return logPath;
    }

    public void setLogPath(String logPath) {
        this.logPath = logPath;
    }

    public String getLogFormat() {
        return logFormat;
    }

    public void setLogFormat(String logFormat) {
        this.logFormat = logFormat;
    }

    public Object getOptions() {
        return options;
    }

    public void setOptions(Object options) {
        this.options = options;
    }

    /**
     * 默认返回配置的第一个数据源配置
     */
    public ResourceConfig getResourceConfig() {
        // 没有配置数据源直接返回空
        if (resources == null) {
            return null;
        }

        for (String resouce : resources.keySet()) {
            return resources.get(resouce);
        }
        return null;
    }

    /**
     * 获取指定Key的数据源配置
     */
    public ResourceConfig getResourceConfig(String resource) {
        return resources.get(resource);
    }

    public Map<String, ResourceConfig> getResources() {
        return resources;
    }

    public void setResources(Map<String, ResourceConfig> resources) {
        this.resources = resources;
    }

    public CacheXConfig getCacheXConfig() {
        return getCacheXConfig(null);
    }

    /**
     * 获取数据源框架配置
     *
     * @param resource 指定的数据源类型，为空则返回第一个数据源配置
     */
    public CacheXConfig getCacheXConfig(String resource) {
        ResourceConfig resourceConfig = null;
        if (resource == null) {
            // 如果不指定数据源则返回第一个数据源配置
            resourceConfig = getResourceConfig();
        } else {
            resourceConfig = getResourceConfig(resource);
        }

        CacheXConfig cacheXConfig = new CacheXConfig();
        if (resourceConfig != null) {
            CacheConfig cacheConfig = new CacheConfig();
            cacheConfig.setType(resourceConfig.getCache());
            CacheXConfig.DbConfig dbConfig = cacheXConfig.getDbConfig();
            DbPoolConfig dbPoolConfig = resourceConfig.getDbPoolConfig();
            if (dbPoolConfig != null) {
                dbConfig.setDialect(resourceConfig.getDialect());
                dbConfig.setJdbcUrl(dbPoolConfig.getJdbcUrl());
                dbConfig.setUsername(dbPoolConfig.getUsername());
                dbConfig.setPassword(dbPoolConfig.getPassword());
            }
            cacheConfig.setJvmConfig(resourceConfig.getJvmConfig());
            cacheConfig.setRedisConfig(resourceConfig.getRedisConfig());
            cacheXConfig.setCacheConfig(cacheConfig);
        }
        return cacheXConfig;
    }


    @Value("cachex")
    public static class ResourceConfig {
        /**
         * 数据库方言，默认为MYSQL
         */
        private String dialect;

        /**
         * 缓存类型，默认为JVM内存
         */
        private String cache;

        /**
         * 数据源相关配置
         */
        protected DbPoolConfig dbPoolConfig;

        /**
         * JVM缓存相关配置
         */
        @Value("jvm")
        private JvmConfig jvmConfig = new JvmConfig();

        /**
         * Redis缓存相关配置
         */
        @Value("redis")
        private RedisConfig redisConfig = new RedisConfig();

        public String getDialect() {
            return dialect;
        }

        public void setDialect(String dialect) {
            this.dialect = dialect;
        }

        public String getCache() {
            return cache;
        }

        public void setCache(String cache) {
            this.cache = cache;
        }

        public DbPoolConfig getDbPoolConfig() {
            return dbPoolConfig;
        }

        public void setDbPoolConfig(DbPoolConfig dbPoolConfig) {
            this.dbPoolConfig = dbPoolConfig;
        }

        public JvmConfig getJvmConfig() {
            return jvmConfig;
        }

        public void setJvmConfig(JvmConfig jvmConfig) {
            this.jvmConfig = jvmConfig;
        }

        public RedisConfig getRedisConfig() {
            return redisConfig;
        }

        public void setRedisConfig(RedisConfig redisConfig) {
            this.redisConfig = redisConfig;
        }
    }

    @Value("dbpool")
    public static class DbPoolConfig {
        /**
         * 数据库URL连接
         */
        private String jdbcUrl;

        /**
         * 数据库用户名
         */
        private String username;

        /**
         * 数据库密码
         */
        private String password;

        /**
         * 连接池最小Connection连接数
         */
        private int minConnections = 12;

        /**
         * 连接池最大Connection连接数，包括空闲和忙碌的Connection连接数
         */
        private int maxConnections = Byte.MAX_VALUE;

        public String getJdbcUrl() {
            return jdbcUrl;
        }

        public void setJdbcUrl(String jdbcUrl) {
            this.jdbcUrl = jdbcUrl;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public int getMinConnections() {
            return minConnections;
        }

        public void setMinConnections(int minConnections) {
            this.minConnections = minConnections;
        }

        public int getMaxConnections() {
            return maxConnections;
        }

        public void setMaxConnections(int maxConnections) {
            this.maxConnections = maxConnections;
        }
    }
}
