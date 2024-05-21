package com.miniSpring.web.utils;

import jakarta.servlet.ServletException;

import java.util.regex.Pattern;

/**
 * ClassName: PathUtils
 * Description:
 *
 * @Author Jeffer Chen
 * @Create 2024/5/7 22:01
 * @Version 1.0
 */
public class PathUtils {

    /**
     * 将path路径转换为正则表达式并compile为Pattern返回
     * @param path
     * @return
     * @throws ServletException
     */
    public static Pattern compile(String path) throws ServletException {
        //例如 {user} 会被替换为 (?<user>[^/]*)  ?代表任意个字符（可以为0个），[^/]表示不能有“/"
        String regPath = path.replaceAll("\\{([a-zA-Z][a-zA-Z0-9]*)\\}", "(?<$1>[^/]*)");
        //正常情况下此时所有的{}应该被替换掉了
        if(regPath.indexOf('{') >= 0 || regPath.indexOf('}') >= 0) {
            throw new ServletException("Invalid path:" + path);
        }
        //^ 和 $ 表示开头和结尾，将字符串compile为pattern，方便后续与URL匹配
        return Pattern.compile("^" + regPath + "$");
    }
}
