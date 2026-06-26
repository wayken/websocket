package cloud.apposs.websocket.listener.httplog.variable;

import cloud.apposs.rest.listener.httplog.variable.AbstractVariableParser;
import cloud.apposs.rest.listener.httplog.variable.LiteralVariable;
import cloud.apposs.websocket.WSHttpRequest;
import cloud.apposs.websocket.WSHttpResponse;

public class VariableParser extends AbstractVariableParser<WSHttpRequest, WSHttpResponse> {
    public VariableParser(String format) {
        super(format);
    }

    @Override
    protected void finalizeFormatter(String option) {
        if (option.startsWith("http_")) {
            String header = option.substring(5).replaceAll("_", "-");
            variableList.add(new HttpHeaderVariable(header));
        } else if (option.startsWith("attr_")) {
            String attribute = option.substring(5).replaceAll("_", "-");
            variableList.add(new HttpAttributeVariable(attribute));
        } else if (option.equals("remote_addr")) {
            variableList.add(new RemoteAddressVariable());
        }  else if (option.equals("remote_port")) {
            variableList.add(new RemotePortVariable());
        } else if (option.equals("host")) {
            variableList.add(new HostVariable());
        } else if (option.equals("request")) {
            variableList.add(new RequestVariable());
        } else if (option.equals("method")) {
            variableList.add(new RequestMethodVariable());
        } else if (option.equals("uri")) {
            variableList.add(new RequestUriVariable());
        } else if (option.equals("status")) {
            variableList.add(new HttpStatusVariable());
        } else if (option.equals("action")) {
            variableList.add(new ActionVariable());
        } else if (option.equals("handler")) {
            variableList.add(new HandlerVariable());
        } else if (option.equals("exp")) {
            variableList.add(new ExceptionVariable());
        } else if (option.equals("expm")) {
            variableList.add(new ExceptionMessageVariable());
        } else if (option.equals("time")) {
            variableList.add(new TimeVariable());
        } else {
            // 所有日志项都没匹配到，那就直接文本输出
            variableList.add(new LiteralVariable<WSHttpRequest, WSHttpResponse>("$" + option));
        }
    }
}
