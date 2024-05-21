package com.miniSpring.web;

import com.miniSpring.annotation.*;
import com.miniSpring.context.ApplicationContext;
import com.miniSpring.context.BeanDefinition;
import com.miniSpring.context.ConfigurableApplicationContext;
import com.miniSpring.exception.ErrorResponseException;
import com.miniSpring.exception.NestedRuntimeException;
import com.miniSpring.exception.ServerErrorException;
import com.miniSpring.exception.ServerWebInputException;
import com.miniSpring.io.PropertyResolver;
import com.miniSpring.utils.ClassUtils;
import com.miniSpring.web.utils.JsonUtils;
import com.miniSpring.web.utils.PathUtils;
import com.miniSpring.web.utils.WebUtils;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * ClassName: DispatcherServlet
 * Description:
 * 入口是父类的service()方法（Servlet容器调用），根据GET或POST调用对应的doGet()和doPost()方法
 * @Author Jeffer Chen
 * @Create 2024/5/7 11:26
 * @Version 1.0
 */
public class DispatcherServlet extends HttpServlet {
    final Logger logger = LoggerFactory.getLogger(getClass());
    //IoC容器
    ApplicationContext applicationContext;
    ViewResolver viewResolver;
    //静态目录
    String resourcePath;
    //网站图标
    String faviconPath;

    //这里不能用Map<String, Dispatcher>的原因在于我们要处理类似/hello/{name}这样的URL，没法使用精确查找，只能使用正则匹配
    List<Dispatcher> getDispatchers = new ArrayList<>();
    List<Dispatcher> postDispatchers = new ArrayList<>();

    /**
     * DispatcherServlet的有参构造
     * @param applicationContext IoC容器
     * @param propertyResolver 存储Properties数据
     */
    public DispatcherServlet(ApplicationContext applicationContext, PropertyResolver propertyResolver) {
        this.applicationContext = applicationContext;
        this.viewResolver = applicationContext.getBean(ViewResolver.class);
        //从PropertyResolver中读取静态目录和网站图标位置
        this.resourcePath = propertyResolver.getProperty("${miniSpring.web.static-path:/static/}");
        this.faviconPath = propertyResolver.getProperty("${miniSpring.web.favicon-path:/favicon.ico}");
        //若配置的ResourcePath不是以"/"结尾，则补上
        if(!this.resourcePath.endsWith("/")) {
            this.resourcePath = this.resourcePath + "/";
        }
    }

    /**
     * 扫描IoC容器，获取具有@Controller和@RestController的Bean Instance
     * @throws ServletException
     */
    @Override
    public void init() throws ServletException{
        logger.info("init {}.", getClass().getName());
        //在IoC容器中扫描 @Controller 和@RestController，这里对容器的强转是为了使用findBeanDefinitions()方法获取所有BeanDefinition
        for(BeanDefinition def : ((ConfigurableApplicationContext)this.applicationContext).findBeanDefinitions(Object.class)) {
            Class<?> beanClass = def.getBeanClass();
            Object bean = def.getRequiredInstance();
            Controller controller = beanClass.getAnnotation(Controller.class);
            RestController restController = beanClass.getAnnotation(RestController.class);
            //两个注解只能有其一
            if (controller != null && restController != null) {
                throw new ServletException("Found @Controller and @RestController on class: " + beanClass.getName());
            }
            //传入isRest、BeanName和beanInstance，将Controller bean中的方法加到两个List<Dispatchers>中
            if (controller != null) {
                addController(false, def.getName(), bean);
            }
            if (restController != null) {
                addController(true, def.getName(), bean);
            }
        }
    }

    /**
     * destroy方法，销毁IoC容器
     */
    @Override
    public void destroy() {
        this.applicationContext.close();
    }

    /**
     * 记录日志，并调用addMethods方法
     * @param isRest
     * @param name
     * @param instance
     */
    void addController(boolean isRest, String name, Object instance) throws ServletException {
        logger.info("add {} controller '{}': {}", isRest ? "REST" : "MVC", name, instance.getClass().getName());
        //TODO 这里的name似乎没有必要传递？
        addMethods(isRest, name, instance, instance.getClass());
    }

    /**
     * 遍历Controller及其父类的@GetMapping和@PostMapping方法，将其添加到List<Dispatcher>中
     * @param isRest 是否为RestController
     * @param name Controller BeanName
     * @param instance Controller beanInstance
     * @param type Controller类名
     * @throws ServletException
     */
    void addMethods(boolean isRest, String name, Object instance, Class<?> type) throws ServletException {
        for(Method m : type.getDeclaredMethods()) {
            //根据@GetMapping或@PostMapping将Method添加到不同List<Dispather>中
            GetMapping get = m.getAnnotation(GetMapping.class);
            if(get != null) {
                //这里是检查该方法是否为static  （静态方法无法使用注入Bean）
                checkMethod(m);
                this.getDispatchers.add(new Dispatcher("GET", isRest, get.value(), instance, m));
            }
            PostMapping post = m.getAnnotation(PostMapping.class);
            if(post != null) {
                checkMethod(m);
                this.postDispatchers.add(new Dispatcher("POST", isRest, post.value(), instance, m));
            }
        }
        //递归将父类的方法也add到List,因为当前Controller可以调用父类方法（故传输的实例和BeanName依然为当前Controller）
        Class<?> superClass = type.getSuperclass();
        if (superClass != null) {
            addMethods(isRest, name, instance, superClass);
        }
    }

    /**
     * 检查是否为静态方法，若是则抛错
     * @param m
     * @throws ServletException
     */
    private void checkMethod(Method m) throws ServletException {
        int mod = m.getModifiers();
        //有GetMapping和PostMapping注解的方法不能为静态（静态方法不能使用Spring注入的bean）
        if(Modifier.isStatic(mod)) {
            throw new ServletException("Cannot do URL mapping to static method: " + m);
        }
        m.setAccessible(true);
    }

    /*写入完毕后调用flush()是必须的，因为大部分Web服务器都基于HTTP/1.1协议，会复用TCP连接。如果没有调用flush()，将导致缓冲区的内容
      无法及时发送到客户端。此外，写入完毕后千万不要调用close()，原因同样是因为会复用TCP连接，如果关闭写入流，将关闭TCP连接，使得Web服
      务器无法复用此TCP连接。*/

    /**
     * 该方法由父类的service()调用，寻找与HttpServletRequest的url对应的Dispatcher（Controller中的Method）来处理GET请求
     * @param req
     * @param resp
     * @throws IOException
     * @throws ServletException
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        String url = req.getRequestURI();
        //静态资源 网站图标（读取.ico文件，写入文件内容） || 静态目录（读取文件，写入文件内容）
        if(url.equals(this.faviconPath) || url.startsWith(this.resourcePath)){
            doResource(url, req, resp);
        } else {
            doService(req, resp, this.getDispatchers);
        }
    }

    /**
     * 该方法由父类的service()调用，寻找与HttpServletRequest的url对应的Dispatcher（Controller中的Method）来处理POST请求
     * @param req
     * @param resp
     * @throws IOException
     * @throws ServletException
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        //与doGet的区别在于传入的Dispatchers不同
        doService(req, resp, this.postDispatchers);
    }

    /**
     * 从Request中获取请求URL，调用doService(url, req, resp, dispatchers)方法并集中处理抛出的错误
     * @param req
     * @param resp
     * @param dispatchers
     * @throws IOException
     * @throws ServletException
     */
    void doService(HttpServletRequest req, HttpServletResponse resp, List<Dispatcher> dispatchers)
            throws IOException, ServletException {
        String url = req.getRequestURI();
        try {
            doService(url, req, resp, dispatchers);
        } catch (ErrorResponseException e) {
            // ErrorResponseException是ServerWebInputException（Status 400）和ServerErrorException（Status 500）的父类
            logger.warn("process request failed with status " + e.statusCode + " : " + url, e);
            // iscommited()方法用于检查HttpservletResponse对象是否已经提交HTTP Header，可有效防止重复输出问题
            if(!resp.isCommitted()) {
                //清除Response缓冲区内容
                resp.resetBuffer();
                //发送一个错误响应到客户端
                resp.sendError(e.statusCode);
            }
        } catch (RuntimeException | ServletException | IOException e) {
            logger.warn("process request failed: " + url, e);
            throw e;
        } catch (Exception e) {
            logger.warn("process request failed: " + url, e);
            throw new NestedRuntimeException(e);
        }
    }

    /**
     * 从List<Dispatcher>中寻找匹配的方法来处理url请求
     * @param url
     * @param req
     * @param resp
     * @param dispatchers
     * @throws Exception
     */
    void doService(String url, HttpServletRequest req, HttpServletResponse resp, List<Dispatcher> dispatchers) throws Exception {
        for(Dispatcher dispatcher : dispatchers) {
            Result result = dispatcher.process(url, req, resp);
            //如果匹配到了对应的Dispatcher
            if(result.processed()) {
                Object r = result.returnObject();
                //如果是@RestController注解，则发送rest风格Response
                if(dispatcher.isRest) {
                    //惯例先确认resp是否已提交HTTP Header
                    if(!resp.isCommitted()) {
                        resp.setContentType("application/json");
                    }
                    //TODO （已提Issue）这里对ResponseBody注解仅考虑字符流和字节流，似乎是因为廖老师默认Controller方法返回值会先将Java对象转为JSON格式
                    if(dispatcher.isResponseBody) {
                        if(r instanceof String s) {
                            //以Response body形式发送字符流
                            PrintWriter pw = resp.getWriter();
                            pw.write(s);
                            pw.flush();
                        }else if(r instanceof byte[] data) {
                            //以Response body形式发送字节流
                            ServletOutputStream output = resp.getOutputStream();
                            output.write(data);
                            output.flush();
                        } else {
                            throw new ServletException("Unable to process REST result when handle url: " + url);
                        }
                    } else if (!dispatcher.isVoid) {
                        PrintWriter pw = resp.getWriter();
                        JsonUtils.writeJson(pw, r);
                        pw.flush();
                    }
                } else {
                    //发送MVC风格
                    if(!resp.isCommitted()) {
                        resp.setContentType("text/html");
                    }
                    //String类型只有@ResponseBody和重定向两种情况
                    if(r instanceof String s) {
                        if(dispatcher.isResponseBody) {
                            //以Response body形式发送
                            PrintWriter pw = resp.getWriter();
                            pw.write(s);
                            pw.flush();
                        } else if (s.startsWith("redirect:")) {
                            //重定向
                            resp.sendRedirect(s.substring(9));
                        }else {
                            throw new ServletException("Unable to process String result when handle url: " + url);
                        }
                    } else if(r instanceof byte[] data) {
                        //byte[]类型只能是@ResponseBody
                        if(dispatcher.isResponseBody) {
                            //以Response body形式发送
                            ServletOutputStream output = resp.getOutputStream();
                            output.write(data);
                            output.flush();
                        } else {
                            throw new ServletException("Unable to process byte[] result when handle url: " + url);
                        }
                    } else if(r instanceof ModelAndView mv) {
                        String view = mv.getViewName();
                        if(view.startsWith("redirect:")) {
                            //重定向
                            resp.sendRedirect(view.substring(9));
                        } else {
                            //用FreeMarkerViewResolver渲染至resp
                            this.viewResolver.render(view, mv.getModel(), req, resp);
                        }
                    } else if (!dispatcher.isVoid && r != null) {
                        throw new ServletException("Unable to process " + r.getClass().getName() + " result when handle url: " + url);
                    }
                }
                //如果dispatcher处理过了，这里就退出
                return;
            }
        }
        //未找到对应Dispatcher
        resp.sendError(404, "Not Found");
    }

    /**
     * 将Resource目录下的静态文件直接读取到Response中
     * @param url
     * @param req
     * @param resp
     * @throws IOException
     */
    void doResource(String url, HttpServletRequest req, HttpServletResponse resp) throws IOException {
        ServletContext ctx = req.getServletContext();
        //ServletContext. getResourceAsStream(String path)默认从WebAPP根目录下获取资源
        try (InputStream input = ctx.getResourceAsStream(url)) {
            if(input == null) {
                resp.sendError(404, "Not Found");
            } else {
                //取url最后一段/部分为file名
                String file = url;
                int n = url.lastIndexOf('/');
                if(n >= 0) {
                    file = url.substring(n+1);
                }
                //通过文件名（主要是后缀）获取文件的mimeType（[type]/[subtype] 形式）
                String mime = ctx.getMimeType(file);
                if(mime == null) {
                    mime = "application/octet-stream";  //代表任意的二进制数据
                }
                //将mimeType写入Response
                resp.setContentType(mime);
                ServletOutputStream output = resp.getOutputStream();
                //将输入流读取为输出流
                input.transferTo(output);
                output.flush();
            }
        }
    }

    static class Dispatcher {
        final static Result NOT_PROCESSED = new Result(false, null);
        final Logger logger = LoggerFactory.getLogger(getClass());

        boolean isRest; //是否返回rest
        boolean isResponseBody; //是否有@ResponseBody
        boolean isVoid; //是否返回void
        Pattern urlPattern; //URL正则匹配
        Object controller; //Bean实例
        Method handlerMethod; //处理方法
        Param[] methodParameters; //方法参数

        public Dispatcher(String httpMethod, boolean isRest, String urlPattern, Object controller, Method method) throws ServletException {
            this.isRest = isRest;
            this.isResponseBody = method.getAnnotation(ResponseBody.class) != null;
            this.isVoid = method.getReturnType() == void.class;
            this.urlPattern = PathUtils.compile(urlPattern);
            this.controller = controller;
            this.handlerMethod = method;
            //获取方法的参数及其注解
            Parameter[] params = method.getParameters();
            Annotation[][] paramsAnnos = method.getParameterAnnotations();
            this.methodParameters = new Param[params.length];
            for (int i = 0; i < params.length; i++) {
                //httpMethod为"POST"或"GET"，将parameter的信息打包封装至Param对象中
                this.methodParameters[i] = new Param(httpMethod, method, params[i], paramsAnnos[i]);
            }
            logger.atDebug().log("mapping {} to handler {}.{}", urlPattern, controller.getClass().getSimpleName(),
                    method.getName());
            if(logger.isDebugEnabled()) {
                for(var p : this.methodParameters) {
                    logger.debug("> parameter: {}", p);
                }
            }
        }

        /**
         * 对传入的URL进行正则匹配，如果match则传入Request中的参数、调用对应方法，并返回方法处理结果
         * @param url
         * @param request
         * @param response
         * @return
         * @throws Exception
         */
        Result process(String url, HttpServletRequest request, HttpServletResponse response) throws Exception {
            //将当前Dispatcher的urlPatter与传入的url进行正则匹配（尤其是.../{id} 之类的路径
            Matcher matcher = urlPattern.matcher(url);
            //如果匹配上了，则利用反射调用方法
            if(matcher.matches()) {
                Object[] arguments = new Object[this.methodParameters.length];
                for (int i = 0; i < arguments.length; i++) {
                    Param param = methodParameters[i];
                    //switch-case 新写法，通过参数注解来确定获取参数值的方式
                    arguments[i] = switch (param.paramType){
                        //@PathVariable类型({id}之类），需要从正则匹配结果中（PathUtils.compile()过程中保留了name信息）获取对应name的值
                        case PATH_VARIABLE -> {
                            try {
                                String s = matcher.group(param.name);
                                //将字符串转换为对应类型
                                yield convertToType(param.classType, s);
                            } catch (IllegalArgumentException e) {
                                throw new ServerWebInputException("Path variable '" + param.name + "' not found.");
                            }
                        }
                        //@RequestBody类型，将请求体中的数据（JSON格式）转换为Java对象（如User类等）
                        case REQUEST_BODY -> {
                            BufferedReader reader = request.getReader();
                            yield JsonUtils.readJson(reader, param.classType);
                        }
                        //@RequestParam类型，从URL Query即 ? 后面的键值对）或Form表单（application/x-www-form-urlencoded）提取参数
                        case REQUEST_PARAM -> {
                            String s = getOrDefault(request, param.name, param.defaultValue);
                            yield convertToType(param.classType, s);
                        }
                        //HttpServletRequest、HttpSession等Servlet API提供的参数，直接从DispatcherServlet的方法参数获得
                        case SERVLET_VARIABLE -> {
                            Class<?> classType = param.classType;
                            if(classType == HttpServletRequest.class) {
                                yield request;
                            } else if (classType == HttpServletResponse.class) {
                                yield response;
                            } else if (classType == HttpSession.class) {
                                yield request.getSession();
                            } else if (classType == ServletContext.class) {
                                yield request.getServletContext();
                            } else {
                                throw new ServerErrorException("Could not determine argument type: " + classType);
                            }
                        }
                    };
                }
                //通过反射将参数传入对应的handlerMethod运行，返回Result
                Object result = null;
                try {
                    result = this.handlerMethod.invoke(this.controller, arguments);
                } catch (InvocationTargetException e) {
                    Throwable t = e.getCause();
                    if(t instanceof Exception ex) {
                        throw ex;
                    }
                    throw e;
                } catch (ReflectiveOperationException e) {
                    throw new ServerErrorException(e);
                }
                return new Result(true, result);
            }
            //未匹配上则不执行
            return NOT_PROCESSED;
        }

        /**
         * 将字符串s转换为指定类型
         * @param classType
         * @param s
         * @return 若目标类不是String、boolean/int/long/byte/short/float/double及其包装类，则抛错
         */
        Object convertToType(Class<?> classType, String s) {
            if(classType == String.class) {
                return s;
            } else if (classType == boolean.class || classType == Boolean.class) {
                return Boolean.valueOf(s);
            } else if (classType == int.class || classType == Integer.class) {
                return Integer.valueOf(s);
            } else if (classType == long.class || classType == Long.class) {
                return Long.valueOf(s);
            } else if (classType == byte.class || classType == Byte.class) {
                return Byte.valueOf(s);
            } else if (classType == short.class || classType == Short.class) {
                return Short.valueOf(s);
            } else if (classType == float.class || classType == Float.class) {
                return Float.valueOf(s);
            } else if (classType == double.class || classType == Double.class) {
                return Double.valueOf(s);
            } else {
                throw new ServerErrorException("Could not determine argument type: " + classType);
            }
        }

        /**
         * 用于@RequestParam注解，从URL的查询参数（即 ? 后面的键值对），或者是在 POST 请求的请求体中通过 application/x-www-form-urlencoded
         * mimeType发送的数据，若未查询到则返回该注解的defaultValue
         * @param request
         * @param name
         * @param defaultValue
         * @return
         */
        private String getOrDefault(HttpServletRequest request, String name, String defaultValue) {
            //从Request中获取相应参数值
            String s = request.getParameter(name);
            //如果Request查询为Null，则使用默认值
            if(s == null) {
                //因为@RequestParam注解defaultValue默认值即为DEFAULT_PARAM_VALUE
                if (WebUtils.DEFAULT_PARAM_VALUE.equals(defaultValue)) {
                    throw new ServerWebInputException("Request parameter '" + name + "' not found.");
                }
                return defaultValue;
            }
            return s;
        }
    }

    static enum ParamType {
        /*
        PATH_VARIABLE：路径参数，从URL中提取；
        REQUEST_PARAM：URL参数，从URL Query或Form表单提取；
        REQUEST_BODY：REST请求参数，从Post传递的JSON提取；
        SERVLET_VARIABLE：HttpServletRequest等Servlet API提供的参数，直接从DispatcherServlet的方法参数获得
        */
        PATH_VARIABLE, REQUEST_PARAM, REQUEST_BODY, SERVLET_VARIABLE;
    }

    static class Param {
        String name; //参数名
        ParamType paramType; //参数类型（4种）
        Class<?> classType; //参数Class类型
        String defaultValue; //参数默认值

        public Param(String httpMethod, Method method, Parameter parameter, Annotation[] annotations) throws ServletException {
            PathVariable pv = ClassUtils.getAnnotation(annotations, PathVariable.class);
            RequestParam rp = ClassUtils.getAnnotation(annotations, RequestParam.class);
            RequestBody rb = ClassUtils.getAnnotation(annotations, RequestBody.class);
            //上述3个注解最多只能有一个
            int total = (pv == null ? 0 : 1) + (rp == null ? 0 : 1) + (rb == null ? 0 : 1);
            if(total > 1) {
                throw new ServletException("Annatation @PathVariable, @RequestParam and @RequestBody cannot be " +
                        "combined at method: " + method);
            }
            this.classType = parameter.getType();
            //解析注解信息
            if(pv != null) {
                this.name = pv.value();
                this.paramType = ParamType.PATH_VARIABLE;
            } else if(rp != null) {
                this.name = rp.value();
                this.defaultValue = rp.defaultValue();
                this.paramType = ParamType.REQUEST_PARAM;
            } else if (rb != null) {
                this.paramType = ParamType.REQUEST_BODY;
            } else {
                this.paramType = ParamType.SERVLET_VARIABLE;
                //检查Servlet variable是否合规
                if(this.classType != HttpServletRequest.class && this.classType != HttpServletResponse.class &&
                this.classType != HttpSession.class && this.classType != ServletContext.class) {
                    throw new ServerErrorException("(Missing annotation?) Unsupported argument type: " + classType +
                            " at method: " + method);
                }
            }
        }
        @Override
        public String toString() {
            return "Param [name=" + name + ", paramType=" + paramType + ", classType=" + classType + ", " +
                    "defaultValue=" + defaultValue + "]";
        }
    }

    /**
     * 用于封装Dispatcher process结果的record类
     * @param processed
     * @param returnObject
     */
    static record Result(boolean processed, Object returnObject){
    }
}
