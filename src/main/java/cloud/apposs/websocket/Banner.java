package cloud.apposs.websocket;

import java.io.PrintStream;

public class Banner {
    private static final String[] BANNER = {
            "                __                    __        __ ",
            " _      _____  / /_  _________  _____/ /_____  / /_",
            "| | /| / / _ \\/ __ \\/ ___/ __ \\/ ___/ //_/ _ \\/ __/",
            "| |/ |/ /  __/ /_/ (__  ) /_/ / /__/ ,< /  __/ /_  ",
            "|__/|__/\\___/_.___/____/\\____/\\___/_/|_|\\___/\\__/  "
    };
    private static final String CLOUDX_BOOT = " :: CloudX WebSocket :: ";
    private static final int STRAP_LINE_SIZE = 38;

    public void printBanner(PrintStream printStream) {
        for (String line : BANNER) {
            printStream.println(line);
        }
        StringBuilder padding = new StringBuilder();
        while (padding.length() < STRAP_LINE_SIZE - (WebSocketConstants.VERSION.length() + CLOUDX_BOOT.length())) {
            padding.append(" ");
        }
        printStream.println(CLOUDX_BOOT + padding.toString() + WebSocketConstants.VERSION);
        printStream.println();
        printStream.flush();
    }
}
