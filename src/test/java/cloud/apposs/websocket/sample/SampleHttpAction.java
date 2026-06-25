package cloud.apposs.websocket.sample;

import cloud.apposs.react.React;
import cloud.apposs.rest.annotation.Action;
import cloud.apposs.rest.annotation.Request;
import cloud.apposs.rest.annotation.Variable;
import cloud.apposs.util.StandardResult;
import cloud.apposs.websocket.WSHttpRequest;

/**
 * 测试用HTTP Action，与WebSocket共用同一端口
 */
@Action
public class SampleHttpAction {
    @Request.Get("/api/hello")
    public React<StandardResult> hello() {
        return React.just(StandardResult.success("hello"));
    }

    @Request.Get("/api/user/{id}")
    public React<StandardResult> getUser(@Variable("id") String id) {
        return React.just(StandardResult.success("user-" + id));
    }

    @Request.Post("/api/echo")
    public React<StandardResult> echo(WSHttpRequest request) {
        return React.just(StandardResult.success("echo"));
    }
}
