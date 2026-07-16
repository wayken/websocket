package cloud.apposs.websocket.resolver.view;

import cloud.apposs.ioc.annotation.Component;
import cloud.apposs.rest.FileStream;
import cloud.apposs.rest.annotation.Order;
import cloud.apposs.rest.view.AbstractViewResolver;
import cloud.apposs.util.CachedFileStream;
import cloud.apposs.websocket.WSHttpRequest;
import cloud.apposs.websocket.WSHttpResponse;

import java.util.Map;

@Component
@Order(-100)
public class FileViewResolver extends AbstractViewResolver<WSHttpRequest, WSHttpResponse> {
    @Override
    public boolean supports(WSHttpRequest request, WSHttpResponse response, Object result) {
        return result instanceof FileStream;
    }

    @Override
    public void render(WSHttpRequest request, WSHttpResponse response, Object result, boolean flush) throws Exception {
        FileStream fileStream = (FileStream) result;
        for (Map.Entry<String, String> header : fileStream.getHeaders().entrySet()) {
            response.putHeader(header.getKey(), header.getValue());
        }
        response.setContentType(fileStream.getMediaType().value());

        if (fileStream.isMediaMode()) {
            response.write(fileStream.getMediaFile(), flush);
            return;
        }

        CachedFileStream downloadFile = fileStream.getDownloadFile();
        if (downloadFile.isInMemory()) {
            response.write(downloadFile.getRawData(), flush);
        } else {
            response.write(downloadFile.getRawFile(), flush);
        }
    }
}
