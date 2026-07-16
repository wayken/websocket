package cloud.apposs.websocket.buildin;

import cloud.apposs.logger.Logger;
import cloud.apposs.react.React;
import cloud.apposs.rest.FileStream;
import cloud.apposs.rest.annotation.Request;
import cloud.apposs.util.CachedFileStream;
import cloud.apposs.util.CharsetUtil;
import cloud.apposs.util.MediaType;
import cloud.apposs.websocket.WSHttpRequest;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URLDecoder;

public class ResourceAction {
    public String[] getStaticPath() {
        return new String[] {
                "classpath:/static/",
                "classpath:/public/",
                "classpath:/dist/",
        };
    }

    public String getDefaultPage() {
        return "/index.html";
    }

    public boolean isReturnDefaultPage() {
        return true;
    }

    @Request.Read("/**")
    public React<FileStream> handleResource(WSHttpRequest request) {
        return React.emitter(() -> {
            String path = request.getPath();
            if (path == null) {
                throw new IllegalArgumentException(
                        "Required request path for url '" + request.getUri() + "' is not set");
            }
            if (path.indexOf('%') >= 0) {
                path = URLDecoder.decode(path, CharsetUtil.UTF_8.name());
            }
            path = normalizeRequestPath(path);

            String defaultPage = normalizeRequestPath(getDefaultPage());
            if ("/".equals(path)) {
                path = defaultPage;
            }

            FileStream fileStream = handleMatchedFileLoad(path);
            if (fileStream != null) {
                return fileStream;
            }
            if (isReturnDefaultPage() && !defaultPage.equals(path)) {
                fileStream = handleMatchedFileLoad(defaultPage);
                if (fileStream != null) {
                    return fileStream;
                }
            }
            throw new FileNotFoundException("Resource file not found for path '" + path + "'");
        });
    }

    protected FileStream handleMatchedFileLoad(String path) throws Exception {
        MediaType mediaType = resolveMediaType(path);
        String relativePath = path.substring(1);
        for (String staticPath : getStaticPath()) {
            if (staticPath == null) {
                continue;
            }
            if (staticPath.startsWith("classpath:")) {
                String basePath = trimSlashes(staticPath.substring("classpath:".length()));
                String resourcePath = basePath.length() == 0 ? relativePath : basePath + "/" + relativePath;
                InputStream resource = getClass().getClassLoader().getResourceAsStream(resourcePath);
                if (Logger.isTraceEnabled()) {
                    Logger.trace("Try to load resource file for path '%s' of resource %s", resourcePath, resource);
                }
                if (resource != null) {
                    try {
                        return FileStream.create(mediaType, CachedFileStream.wrap(resource));
                    } finally {
                        resource.close();
                    }
                }
            } else {
                File baseDirectory = new File(staticPath).getCanonicalFile();
                File file = new File(baseDirectory, relativePath).getCanonicalFile();
                if (!isChild(baseDirectory, file)) {
                    throw new IllegalArgumentException("Resource path escapes static directory: '" + path + "'");
                }
                if (Logger.isTraceEnabled()) {
                    Logger.trace("Try to load disk file for path '%s' of file %s", path, file);
                }
                if (file.isFile()) {
                    return FileStream.create(mediaType, file);
                }
            }
        }
        return null;
    }

    private MediaType resolveMediaType(String path) {
        int slash = path.lastIndexOf('/');
        int dot = path.lastIndexOf('.');
        String extension = dot > slash && dot + 1 < path.length() ? path.substring(dot + 1) : null;
        MediaType mediaType = MediaType.getMediaTypeByFileExtension(extension);
        return mediaType != null ? mediaType : MediaType.APPLICATION_OCTET_STREAM;
    }

    private String normalizeRequestPath(String path) {
        if (path == null || path.length() == 0) {
            return "/";
        }
        String normalized = path.replace('\\', '/');
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        String[] segments = normalized.split("/");
        StringBuilder result = new StringBuilder();
        for (String segment : segments) {
            if (segment.length() == 0 || ".".equals(segment)) {
                continue;
            }
            if ("..".equals(segment)) {
                throw new IllegalArgumentException("Invalid resource path '" + path + "'");
            }
            result.append('/').append(segment);
        }
        return result.length() == 0 ? "/" : result.toString();
    }

    private String trimSlashes(String path) {
        int start = 0;
        int end = path.length();
        while (start < end && path.charAt(start) == '/') {
            start++;
        }
        while (end > start && path.charAt(end - 1) == '/') {
            end--;
        }
        return path.substring(start, end);
    }

    private boolean isChild(File directory, File file) {
        String directoryPath = directory.getPath();
        String filePath = file.getPath();
        return filePath.equals(directoryPath) || filePath.startsWith(directoryPath + File.separator);
    }
}
