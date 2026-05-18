package cloud.apposs.websocket.listener.commadarlog.variable;

import cloud.apposs.websocket.WSSession;
import cloud.apposs.websocket.commandar.Commandar;

import java.util.LinkedList;
import java.util.List;

public class CommandarVariableParser {
    private static final char ESCAPE_CHAR = '$';
    private static final int BUFFER_SIZE = 256;

    private final String format;

    // 日志选项解析器
    protected final List<IVariable> variableList = new LinkedList<IVariable>();

    public CommandarVariableParser(String format) {
        this.format = format;
        if (format != null) {
            this.handleFormatParse();
        }
    }

    public String parse(Commandar commandar, WSSession session, Throwable cause) {
        StringBuilder output = new StringBuilder(BUFFER_SIZE);
        for (IVariable format : variableList) {
            String message = format.parse(commandar, session, cause);
            if (message != null) {
                output.append(message);
            }
        }
        return output.toString();
    }

    private void handleFormatParse() {
        StringBuilder currentLiteral = new StringBuilder(32);
        int state = State.LITERAL_STATE;
        for (int i = 0; i < format.length(); i++) {
            char letter = format.charAt(i);
            switch (state) {
                case State.LITERAL_STATE:
                    if (letter != ESCAPE_CHAR) { // 只是普通字符而已
                        currentLiteral.append(letter);
                        break;
                    }
                    // 匹配关键字$
                    if (currentLiteral.length() != 0) {
                        variableList.add(new LiteralVariable(currentLiteral.toString()));
                    }
                    currentLiteral.setLength(0);
                    state = State.FORMAT_STATE;
                    break;
                case State.FORMAT_STATE:
                    if (letter != '_' && !Character.isLetter(letter)) {
                        // 对应一项日志项已经解析结束，判断是否存在对应IFormatter对象映射
                        finalizeFormatter(currentLiteral.toString());
                        currentLiteral.setLength(0);
                        state = State.LITERAL_STATE;
                    }
                    currentLiteral.append(letter);
                    break;
            }
        }
        // 最后结束项的解析
        if (state == State.LITERAL_STATE) {
            if (currentLiteral.length() != 0) {
                variableList.add(new LiteralVariable(currentLiteral.toString()));
            }
        } else if (state == State.FORMAT_STATE) {
            if (currentLiteral.length() != 0) {
                finalizeFormatter(currentLiteral.toString());
            }
        }
    }

    private void finalizeFormatter(String option) {
        if (option.startsWith("http_")) {
            String header = option.substring(5).replaceAll("_", "-");
            variableList.add(new HttpHeaderVariable(header));
        } else if (option.startsWith("attr_")) {
            String attribute = option.substring(5).replaceAll("_", "-");
            variableList.add(new SessionAttributeVariable(attribute));
        } else if (option.equals("remote_addr")) {
            variableList.add(new RemoteAddressVariable());
        }  else if (option.equals("remote_port")) {
            variableList.add(new RemotePortVariable());
        } else if (option.equals("host")) {
            variableList.add(new HostVariable());
        } else if (option.equals("command_name")) {
            variableList.add(new CommandNameVariable());
        } else if (option.equals("command_path")) {
            variableList.add(new CommandPathVariable());
        } else if (option.equals("command_method")) {
            variableList.add(new CommandMethodVariable());
        } else if (option.equals("session_id")) {
            variableList.add(new SessionIdVariable());
        } else if (option.equals("url")) {
            variableList.add(new RequestUrlVariable());
        } else if (option.equals("commandar")) {
            variableList.add(new CommandarVariable());
        } else if (option.equals("exp")) {
            variableList.add(new ExceptionVariable());
        } else if (option.equals("expm")) {
            variableList.add(new ExceptionMessageVariable());
        } else if (option.equals("time")) {
            variableList.add(new TimeVariable());
        } else {
            // 所有日志项都没匹配到，那就直接文本输出
            variableList.add(new LiteralVariable("$" + option));
        }
    }

    public class State {
        private static final int LITERAL_STATE = 0;
        private static final int FORMAT_STATE = 1;
    }
}
