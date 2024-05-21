package com.miniSpring.io;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * ClassName: io.ResourceResolver
 * Description:
 *
 * @Author Jeffer Chen
 * @Create 2024/4/15 21:08
 * @Version 1.0
 */
public class ResourceResolver {
    //记录日志
    Logger logger = LoggerFactory.getLogger(getClass());
    //需要扫描的包名
    String basePackage;

    //构造器传入需要扫描的包名
    public ResourceResolver(String basePackage) {
        this.basePackage = basePackage;
    }

    /**
     * 获取扫描到的Resource
     * @param mapper 映射函数，传入Resource到指定类型（R，在调用时自定义需求）的映射
     * @return
     * @param <R>
     */
    public <R> List<R> scan(Function<Resource, R> mapper) {
        //将包名中的 . 替换成 /
        String basePackagePath = this.basePackage.replace(".", "/");
        String path = basePackagePath;
        try {
            List<R> collector = new ArrayList<>();
            scan0(basePackagePath, path, collector, mapper);
            return collector;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    <R> void scan0(String basePackagePath, String path, List<R> collector, Function<Resource,R> mapper)
            throws IOException, URISyntaxException{
        logger.atDebug().log("scan path: {}", path);
        //通过ClassLoader获取URL列表：
        Enumeration<URL> en = getContextClassLoader().getResources(path);
        while (en.hasMoreElements()) {
            URL url = en.nextElement();
            URI uri = url.toURI();
            //将uri转换为String并去除末尾的斜杠
            String uriStr = removeTrailingSlash(uriToString(uri));
            //获取basePackage所在父目录
            String uriBaseStr = uriStr.substring(0, uriStr.length() - basePackagePath.length());
            if (uriBaseStr.startsWith("file:")) {
                //去掉父目录开头的file:
                uriBaseStr = uriBaseStr.substring(5);
            }
            if (uriStr.startsWith("jar:")) {
                //在jar包中搜索
                scanFile(true, uriBaseStr, jarUriToPath(basePackagePath, uri), collector, mapper);
            } else {
                //在目录中搜索
                scanFile(false, uriBaseStr, Paths.get(uri), collector, mapper);
            }
        }
    }

    ClassLoader getContextClassLoader() {
        ClassLoader cl = null;
        /*因为Web应用的ClassLoader不是JVM提供的基于ClassPath的ClassLoader，而是Servlet容器提供的，从Thread中可以获取到
         Servlet容器专属的ClassLoader*/
        //先从Thread中获取
        cl = Thread.currentThread().getContextClassLoader();
        //若获取不到，再从当前Class获取
        if (cl == null) {
            cl = getClass().getClassLoader();
        }
        return cl;
    }

    /**
     * 将压缩文件的URI转成对应的Path
     * @param basePackagePath
     * @param jarUri
     * @return
     * @throws IOException
     */
    Path jarUriToPath(String basePackagePath, URI jarUri) throws IOException {
        return FileSystems.newFileSystem(jarUri, Map.of()).getPath(basePackagePath);
    }

    <R> void scanFile(boolean isJar, String base, Path root, List<R> collector, Function<Resource, R> mapper) throws IOException {
        String baseDir = removeTrailingSlash(base);
        //遍历目录下所有文件
        Files.walk(root).filter(Files::isRegularFile).forEach(file -> {
            Resource res = null;
            if (isJar) {
                res = new Resource(baseDir, removeLeadingSlash(file.toString()));
            } else {
                String path = file.toString();
                String name = removeLeadingSlash(path.substring(baseDir.length()));
                res = new Resource("file:" + path, name);
            }
            logger.atDebug().log("found resource: {}", res);
            R r = mapper.apply(res);
            if (r != null) {
                collector.add(r);
            }
        });
    }

    /**
     * 将uri转换为UTF-8格式的String
     * @param uri
     * @return
     */
    String uriToString(URI uri) {
        return URLDecoder.decode(uri.toString(), StandardCharsets.UTF_8);
    }

    /**
     * 去掉字符串开头的斜杠
     * @param s
     * @return
     */
    String removeLeadingSlash(String s) {
        if (s.startsWith("/") || s.startsWith("\\")) {
            s = s.substring(1);
        }
        return s;
    }

    /**
     * 去掉字符串末尾的斜杠
     * @param s
     * @return
     */
    String removeTrailingSlash(String s) {
        if (s.endsWith("/") || s.endsWith("\\")) {
            s = s.substring(0, s.length() - 1);
        }
        return s;
    }
}
