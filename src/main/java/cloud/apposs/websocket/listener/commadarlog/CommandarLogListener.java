package cloud.apposs.websocket.listener.commadarlog;

import cloud.apposs.ioc.annotation.Component;
import cloud.apposs.logger.Logger;
import cloud.apposs.websocket.WSConfig;
import cloud.apposs.websocket.WSSession;
import cloud.apposs.websocket.WebSocketConstants;
import cloud.apposs.websocket.commandar.Commandar;
import cloud.apposs.websocket.listener.CommandarListener;
import cloud.apposs.websocket.listener.commadarlog.variable.CommandarVariableParser;

import java.util.List;

/**
 * 请求日志监听输出，支持自定义HTTP请求日志格式输出，格式详见：{@link CommandarVariableParser}
 */
@Component
public class CommandarLogListener implements CommandarListener {
    private boolean loggable = false;

    // 日志解析器
    private CommandarVariableParser parser;

    @Override
    public void initialize(WSConfig configuration) {
        this.loggable = configuration.isCommandLogEnable();
        this.parser = new CommandarVariableParser(configuration.getCommandLogFormat());
    }

    @Override
    public void commandarStart(Commandar commandar, WSSession session, List<Object> argument) {
        if (!loggable) return;
        session.setAttribute(WebSocketConstants.COMMAND_ATTRIBUTE_START_TIME, System.currentTimeMillis());
    }

    @Override
    public void commandarCompletion(Commandar commandar, WSSession session, Throwable cause) {
        if (!loggable) return;
        if (cause != null) {
            Logger.error(cause, parser.parse(commandar, session, cause));
        } else {
            Logger.info(parser.parse(commandar, session, cause));
        }
    }
}
