package cloud.apposs.websocket;

import cloud.apposs.logger.Configuration;
import cloud.apposs.logger.Logger;
import cloud.apposs.util.ReflectUtil;
import cloud.apposs.util.StrUtil;
import cloud.apposs.util.SystemInfo;
import cloud.apposs.websocket.banner.Banner;
import cloud.apposs.websocket.banner.WSBanner;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public abstract class ApplicationContext {
    // 全局配置
    private WSConfig config;

    protected Banner.Mode bannerMode = Banner.Mode.CONSOLE;
    protected static final Banner DEFAULT_BANNER = new WSBanner();
    protected Banner banner = DEFAULT_BANNER;

    // 服务启动开始时间
    protected long appStartTime;

    public ApplicationContext() {
        this(new WSConfig());
    }

    public ApplicationContext(WSConfig config) {
        this.config = config;
    }

    /**
     * 启动WebSocket服务
     */
    public ApplicationContext run(Class<?> primarySource, String... args) throws Exception {
        appStartTime = System.currentTimeMillis();
        try {
            handleRunApplication();
            Logger.info("%s WebSocket Server %s:%s Startup In %d MilliSeconds", primarySource.getSimpleName(),
                    config.getHost(), config.getPort(), (System.currentTimeMillis() - appStartTime));
        } catch (Exception cause) {
            Logger.error(cause, "%s WebSocket Server Startup Fail @%s:%s", primarySource.getSimpleName(),
                    config.getHost(), config.getPort());
            shutdown();
        }
        return this;
    }

    /**
     * 设置Banner终端显示
     */
    public ApplicationContext setBanner(Banner banner) {
        this.banner = banner;
        return this;
    }

    private void handleRunApplication() throws Exception {
        // 初始化配置
        handleInitConfig(config);
        // 初始化日志
        handleInitLogger(config);
        // 输出BANNER信息
        handleInitBanner(bannerMode, banner, config.getCharset());
        handlePrintSysInfomation();
        // 开始启动WebSocket服务
        handleStartWebSocketServer(config);
        // 注册服务被kill时的回调
        handleShutdownHookRegister();
    }

    private void handleInitConfig(WSConfig config) {
        if (StrUtil.isEmpty(config.getHost())) {
            config.setHost(WebSocketConstants.DEFAULT_HOST);
        }
        if (config.getPort() <= 0) {
            config.setPort(WebSocketConstants.DEFAULT_PORT);
        }
        String basePackage = config.getBasePackage();
        // 是否配置中不存在框架中的包，则需要配置进去，方便扫描框架中的各种组件包
        if (!StrUtil.isEmpty(basePackage)) {
            String bootorPackage = ReflectUtil.getPackage(ApplicationContext.class);
            String[] basePackageSplit = basePackage.split(",");
            List<String> basePackageList = new ArrayList<String>(basePackageSplit.length);
            for (int i = 0; i < basePackageSplit.length; i++) {
                basePackageList.add(basePackageSplit[i].trim());
            }
            if (!basePackageList.contains(bootorPackage + ".interceptor")) {
                basePackageList.add(bootorPackage + ".interceptor");
            }
            if (!basePackageList.contains(bootorPackage + ".listener")) {
                basePackageList.add(bootorPackage + ".listener");
            }
            if (!basePackageList.contains(bootorPackage + ".resolver")) {
                basePackageList.add(bootorPackage + ".resolver");
            }
            basePackage = StrUtil.joinArrayString(basePackageList, ",");
        }
        config.setBasePackage(basePackage);
    }

    private void handleInitLogger(WSConfig config) {
        Properties properties = new Properties();
        properties.put(Configuration.Prefix.APPENDER, config.getLogAppender());
        properties.put(Configuration.Prefix.LEVEL, config.getLogLevel());
        properties.put(Configuration.Prefix.FILE, config.getLogPath());
        properties.put(Configuration.Prefix.FORMAT, config.getLogFormat());
        Logger.config(properties);
    }

    private void handleInitBanner(Banner.Mode bannerMode, Banner banner, String charset) throws Exception {
        if (bannerMode != Banner.Mode.OFF) {
            if (bannerMode == Banner.Mode.CONSOLE) {
                banner.printBanner(System.out);
            } else {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                banner.printBanner(new PrintStream(baos));
                Logger.info(baos.toString(charset));
            }
        }
    }

    private void handlePrintSysInfomation() {
        if (config.isShowSysInfo()) {
            SystemInfo OS = SystemInfo.getInstance();
            Logger.info("OS Name: %s", OS.getOsName());
            Logger.info("OS Arch: %s", OS.getOsArch());
            Logger.info("IO Mode: %s", config.getIoMode());
            Logger.info("Java Home: %s", OS.getJavaHome());
            Logger.info("Java Version: %s", OS.getJavaVersion());
            Logger.info("Java Vendor: %s", OS.getJavaVendor());
            List<String> jvmArguments = OS.getJvmArguments();
            for (String argument : jvmArguments) {
                Logger.info("Jvm Argument: [%s]", argument);
            }
        }
    }

    /**
     * 注册服务被kill时的回调，只能捕获kill -15的信号量 kill -9 没办法
     */
    private void handleShutdownHookRegister() {
        Runtime.getRuntime().addShutdownHook(new Thread(){
            @Override
            public void run() {
                shutdown();
            }
        });
    }

    public void shutdown() {
        this.handleCloseWebSocketServer();
        Logger.info("WebSocket Server Has Been Shutdown. Running %s", StrUtil.formatTimeOutput(System.currentTimeMillis() - appStartTime));
        Logger.close(true);
    }

    /**
     * 启动服务，由网络内核服务（如Netty/Undertow）根据自身服务特点启动
     */
    protected abstract void handleStartWebSocketServer(WSConfig config) throws Exception;

    /**
     * 关闭服务，释放资源
     */
    protected abstract void handleCloseWebSocketServer();
}
