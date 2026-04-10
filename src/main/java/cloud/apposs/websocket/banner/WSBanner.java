package cloud.apposs.websocket.banner;

import cloud.apposs.websocket.WebSocketConstants;

import java.io.PrintStream;

public class WSBanner implements Banner {
    private static final String[] BANNER = {
            "                __                    __        __ ",
            " _      _____  / /_  _________  _____/ /_____  / /_",
            "| | /| / / _ \\/ __ \\/ ___/ __ \\/ ___/ //_/ _ \\/ __/",
            "| |/ |/ /  __/ /_/ (__  ) /_/ / /__/ ,< /  __/ /_  ",
            "|__/|__/\\___/_.___/____/\\____/\\___/_/|_|\\___/\\__/  "
    };
    private static final String WS_BOOT = " :: CloudX WebSocket :: ";
    private static final int STRAP_LINE_SIZE = 38;

    @Override
    public void printBanner(PrintStream printStream) {
        for (String line : BANNER) {
            printStream.println(line);
        }
        StringBuilder padding = new StringBuilder();
        while (padding.length() < STRAP_LINE_SIZE - (WebSocketConstants.VERSION.length() + WS_BOOT.length())) {
            padding.append(" ");
        }
        printStream.println(WS_BOOT + padding + WebSocketConstants.VERSION);
        printStream.println();
        printStream.flush();
    }
}
