package cloud.apposs.websocket.banner;

import java.io.PrintStream;

/**
 * 在线BANNER生成：http://patorjk.com/software/taag
 */
public interface Banner {
    void printBanner(PrintStream printStream);

    enum Mode {
        // 关闭BANNER输出
        OFF,
        // BANNER终端System.out输出
        CONSOLE,
        // BANNER日志输出
        LOGGER
    }
}
