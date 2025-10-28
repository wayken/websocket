package cloud.apposs.websocket;

import cloud.apposs.configure.ConfigurationFactory;
import cloud.apposs.configure.ConfigurationParser;
import cloud.apposs.util.GetOpt;
import cloud.apposs.util.ReflectUtil;
import cloud.apposs.util.ResourceUtil;
import cloud.apposs.util.StrUtil;
import cloud.apposs.websocket.netty.NettyApplicationContext;

import java.io.InputStream;

/**
 * WebSocket服务启动程序
 */
public class WebSocketApplication {
    private final Class<?> primarySource;

    public WebSocketApplication(Class<?> primarySource) {
        this.primarySource = primarySource;
    }

    /**
     * 启动WebSocket服务
     */
    public static ApplicationContext run(Class<?> primarySource, String... args) throws Exception {
        return run(primarySource, generateConfiguration(primarySource, args), args);
    }

    public static ApplicationContext run(Class<?> primarySource, Object options, String... args) throws Exception {
        WSConfig config = new WSConfig();
        config.setOptions(options);
        return run(primarySource, WebSocketApplication.generateConfiguration(primarySource, config, args), args);
    }

    public static ApplicationContext run(Class<?> primarySource, WSConfig config, String... args) throws Exception {
        return new WebSocketApplication(primarySource).run(config, args);
    }

    public ApplicationContext run(WSConfig config, String... args) throws Exception {
        return new NettyApplicationContext(config).run(primarySource, args);
    }

    public static WSConfig generateConfiguration(Class<?> primarySource, String... args) throws Exception {
        return generateConfiguration(primarySource, WebSocketConstants.DEFAULT_HOST, WebSocketConstants.DEFAULT_PORT, args);
    }

    public static WSConfig generateConfiguration(Class<?> primarySource, int bindPort, String... args) throws Exception {
        return generateConfiguration(primarySource, WebSocketConstants.DEFAULT_HOST, bindPort, args);
    }

    public static WSConfig generateConfiguration(Class<?> primarySource, WSConfig config , String... args) throws Exception {
        return generateConfiguration(primarySource, config, WebSocketConstants.DEFAULT_HOST, WebSocketConstants.DEFAULT_PORT, args);
    }

    public static WSConfig generateConfiguration(Class<?> primarySource,
                                                     String bindHost, int bindPort, String... args) throws Exception {
        return generateConfiguration(primarySource, new WSConfig(), bindHost, bindPort, args);
    }

    public static WSConfig generateConfiguration(Class<?> primarySource, WSConfig config,
                                                     String bindHost, int bindPort, String... args) throws Exception {
        String configFile = WebSocketConstants.DEFAULT_CONFIG_FILE;
        // 判断是否从命令行中传递配置文件路径
        GetOpt option = new GetOpt(args);
        if (option.containsKey("c")) {
            configFile = option.get("c");
        }
        // 加载配置文件配置
        InputStream filestream = ResourceUtil.getResource(configFile, primarySource);
        ConfigurationParser cp = ConfigurationFactory.getConfigurationParser(ConfigurationFactory.XML);
        cp.parse(config, filestream);

        if (config.getPort() == -1) {
            config.setHost(bindHost);
            config.setPort(bindPort);
        }
        if (StrUtil.isEmpty(config.getBasePackage())) {
            String basePackage = ReflectUtil.getPackage(primarySource);
            config.setBasePackage(basePackage);
        }

        return config;
    }

    /**
     * 关闭HTTP服务
     */
    public static void shutdown(ApplicationContext context) {
        if (context != null) {
            context.shutdown();
        }
    }
}
