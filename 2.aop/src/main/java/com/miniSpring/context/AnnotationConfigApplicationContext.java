package com.miniSpring.context;

import com.miniSpring.annotation.*;

import com.miniSpring.exception.*;
import com.miniSpring.io.PropertyResolver;
import com.miniSpring.io.ResourceResolver;
import com.miniSpring.utils.ClassUtils;

import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ClassName: AnnotationConfigApplicationContext
 * Description:
 *
 * @Author Jeffer Chen
 * @Create 2024/4/18 15:13
 * @Version 1.0
 */
public class AnnotationConfigApplicationContext implements ConfigurableApplicationContext{
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    //用来存储className及其对应的BeanDefinition
    protected final Map<String, BeanDefinition> beans;
    //用来解析Properties文件里的key-Value值
    protected final PropertyResolver propertyResolver;

    //存储BeanPostProcessor
    private List<BeanPostProcessor> beanPostProcessors = new ArrayList<>();
    //记录当前正在创建的所有BeanName
    private Set<String> creatingBeanNames;


    /**
     * 构造器
     * @param configClass 该类ComponentScan注解会标明需要扫描哪些包
     * @param propertyResolver Properties文件解析器
     */
    public AnnotationConfigApplicationContext(Class<?> configClass, PropertyResolver propertyResolver) {
        ApplicationContextUtils.setApplicationContext(this);

        this.propertyResolver = propertyResolver;

        //扫描获取所有Bean的Class类型
        final Set<String> beanClassNames = scanForClassNames(configClass);

        //对所有ClassName创建bean的定义
        this.beans = createBeanDefinitions(beanClassNames);

        //创建BeanName检测循环依赖
        this.creatingBeanNames = new HashSet<>();

        //优先创建@Configuration类型的Bean（工厂模式）
        this.beans.values().stream()
                //过滤出@Configuration
                .filter(this::isConfigurationDefinition)
                .sorted().map(def -> {
                    createBeanAsEarlySingleton(def);
                    return def.getName();
                }).collect(Collectors.toList());//TODO 这里的集合结果似乎并未被接收

        //创建BeanPostProcessor类型的Bean
        List<BeanPostProcessor> processors = this.beans.values().stream()
                //过滤出BeanPostProcessor
                .filter(this::isBeanPostProcessorDefinition)
                .sorted()
                //将生成的Instance存到processors List中
                .map(def -> {
                    return (BeanPostProcessor) createBeanAsEarlySingleton(def);
                }).collect(Collectors.toList());
        this.beanPostProcessors.addAll(processors);//TODO 两步能否简化为一步？

        //创建其他普通Bean：
        createNormalBeans();

        // 通过字段和set方法注入依赖:
        this.beans.values().forEach(def -> {
            injectBean(def);
        });

        // 调用init方法:
        this.beans.values().forEach(def -> {
            initBean(def);
        });

        if (logger.isDebugEnabled()) {
            this.beans.values().stream().sorted().forEach(def -> {
                logger.debug("bean initialized: {}", def);
            });
        }
    }

    /**
     * 调用所有Bean的初始化方法
     * @param def
     */
    void initBean(BeanDefinition def) {
        //调用init方法
        callMethod(def.getInstance(), def.getInitMethod(), def.getInitMethodName());
    }

    /**
     * 调用指定Bean的init/destroy方法
     * @param beanInstance
     * @param method
     * @param namedMethod
     */
    private void callMethod(Object beanInstance, Method method, String namedMethod) {
        if(method != null) {
            try {
                method.invoke(beanInstance);
            } catch (ReflectiveOperationException e) {
                throw new BeanCreationException(e);
            }
        } else if (namedMethod != null) {
            //method为空但name不为空，此时对应的是工厂模式，需要在实际类型中查找 initMethod/destroyMethod="xyz"
            Method named = ClassUtils.getNamedMethod(beanInstance.getClass(), namedMethod);
            named.setAccessible(true);
            try{
                named.invoke(beanInstance);
            }catch (ReflectiveOperationException e) {
                throw new BeanCreationException(e);
            }
        }
    }

    /**
     * 注入弱依赖但不调用init方法
     * @param def
     */
    void injectBean(BeanDefinition def) {
        //获取Bean实例，或被代理的原始实例
        Object beanInstance = getProxiedInstance(def);
        try {
            injectProperties(def, def.getBeanClass(), beanInstance);
        }catch (ReflectiveOperationException e) {
            throw new BeanCreationException(e);
        }
    }

    /**
     * 用于获取代理前的原始Bean
     * @param def
     * @return
     */
    Object getProxiedInstance(BeanDefinition def) {
        Object beanInstance = def.getInstance();
        //如果Proxy改变了原始Bean，又希望注入到原始Bean，则由BeanPostProcessor指定原始Bean
        List<BeanPostProcessor> reversedBeanProcessors = new ArrayList<>(this.beanPostProcessors);
        //因为创建bean Instance时是正向遍历代理，因此还原时需要反向走一遍
        Collections.reverse(reversedBeanProcessors);
        for(BeanPostProcessor beanPostProcessor : reversedBeanProcessors) {
            Object restoredInstance = beanPostProcessor.postProcessOnSetProperty(beanInstance, def.getName());
            if(restoredInstance != beanInstance) {
                beanInstance = restoredInstance;
            }
        }
        return beanInstance;
    }

    /**
     * 根据Setter方法和字段完成弱依赖注入
     * @param def
     * @param clazz
     * @param bean
     */
    void injectProperties(BeanDefinition def, Class<?> clazz, Object bean) throws ReflectiveOperationException {
        //在当前类查找field和Method并注入：
        for(Field f : clazz.getDeclaredFields()) {
            tryInjectProperties(def, clazz, bean, f);
        }
        for (Method m : clazz.getDeclaredMethods()) {
            tryInjectProperties(def, clazz, bean, m);
        }
        //在父类查找Field和Method并注入，因为有些@Autowired写在父类，递归调用至没有父类
        Class<?> superclass = clazz.getSuperclass();
        if(superclass != null) {
            injectProperties(def, superclass, bean);
        }
    }

    /**
     * 注入单个属性
     * @param def
     * @param clazz
     * @param bean
     * @param acc Field和Method都继承了AccessibleObject
     */
    void tryInjectProperties(BeanDefinition def, Class<?> clazz, Object bean, AccessibleObject acc) throws ReflectiveOperationException {
        //获取value和Autowired注解，两者弱同时为空则返回
        Value value = acc.getAnnotation(Value.class);
        Autowired autowired = acc.getAnnotation(Autowired.class);
        if (value == null && autowired == null) {
            return;
        }

        Field field = null;
        Method method = null;
        if(acc instanceof Field f){
            checkFieldOrMethod(f);//对属性修饰符进行校验
            f.setAccessible(true);
            field = f;
        }
        if (acc instanceof Method m) {
            checkFieldOrMethod(m);//对方法修饰符进行校验
            //setter方法超过一个参数则报错
            if (m.getParameters().length != 1) {
                throw new BeanDefinitionException(
                        String.format("Cannot inject a non-setter method %s for bean '%s': %s",
                                m.getName(), def.getName(), def.getBeanClass().getName()));
            }
            m.setAccessible(true);
            method = m;
        }

        String accessibleName = field != null ? field.getName() : method.getName();
        //获取需要注入属性的class
        Class<?> accessibleType = field != null ? field.getType() : method.getParameterTypes()[0];
        //value和Autowired注解同时存在则报错
        if (value != null && autowired != null) {
            throw new BeanCreationException(String.format("Cannot specify both @Autowired and @Value when inject %s.%s for bean '%s': %s",
                    clazz.getSimpleName(), accessibleName, def.getName(), def.getBeanClass().getName()));
        }

        //Value注入
        if(value != null) {
            Object propValue = this.propertyResolver.getRequiredProperty(value.value(), accessibleType);
            if(field != null){
                logger.atDebug().log("Field injection: {}.{} = {}", def.getBeanClass().getName(), accessibleName, propValue);
                field.set(bean, propValue);
            }
            if(method != null){
                logger.atDebug().log("Method injection: {}.{} ({})", def.getBeanClass().getName(), accessibleName, propValue);
                method.invoke(bean, propValue);
            }
        }

        //Autowired注入
        if(autowired != null) {
            String name = autowired.name();
            boolean required = autowired.value(); //default true
            Object depends = name.isEmpty() ? findBean(accessibleType) : findBean(name, accessibleType);
            if(required && depends == null) {
                throw new UnsatisfiedDependencyException(String.format("Dependency bean not found when inject %s.%s for " +
                                "bean '%s': %s", clazz.getSimpleName(),  accessibleName, def.getName(), 
                                def.getBeanClass().getName()));
            }
            if(depends != null){
                if(field != null){
                    logger.atDebug().log("Field injection: {}.{} = {}", def.getBeanClass().getName(), 
                            accessibleName, depends);
                    field.set(bean, depends);
                }
                if(method != null){
                    logger.atDebug().log("Method injection: {}.{} ({})", def.getBeanClass().getName(), 
                            accessibleName, depends);
                    method.invoke(bean, depends);
                }
            }
        }
    }

    /**
     * 校验Method和Field（都实现了Member接口）的修饰符是否正确：不能为static、属性不能为final、方法为final要warn
     * @param m
     */
    void checkFieldOrMethod(Member m) {
        int mod = m.getModifiers();
        if(Modifier.isStatic(mod)) {
            throw new BeanDefinitionException("Cannot inject static field: " + m);
        }
        if (Modifier.isFinal(mod)) {
            if (m instanceof Field field) {
                throw new BeanDefinitionException("Cannot inject final field: " + field);
            }
            //TODO final方法不能被子类重写，代理类的运行可能会受影响
            if (m instanceof Method method) {
                logger.warn(
                        "Inject final method should be careful because it is not called on target bean when bean is " +
                                "proxied and may cause NullPointerException.");
            }
        }
    }

    /**
     * 通过type获取指定类型的Bean实例
     * @param requiredType
     * @return
     * @param <T>
     */
    @Nullable
    @SuppressWarnings("unchecked")
    protected <T> T findBean(Class<?> requiredType) {
        BeanDefinition def = findBeanDefinition(requiredType);
        if(def == null){
            return null;
        }
        return (T) def.getRequiredInstance();
    }

    /**
     * 通过type和name获取指定类型的Bean实例
     * @param name
     * @param requiredType
     * @return
     * @param <T>
     */
    @Nullable
    @SuppressWarnings("unchecked")
    protected <T> T findBean(String name, Class<T> requiredType) {
        BeanDefinition def = findBeanDefinition(name, requiredType);
        if (def == null) {
            return null;
        }
        return (T) def.getRequiredInstance();
    }

    /**
     * 创建普通的Bean
     */
    void createNormalBeans() {
        //获取BeanDefinition列表
        List<BeanDefinition> defs = this.beans.values().stream()
                //过滤出还没有生成实例的BeanDefinition
                .filter(def -> def.getInstance() == null).sorted().collect(Collectors.toList());

        defs.forEach(def ->{
            //如果Bean未被创建(有些在list中的Bean可能已经在其他Bean创建时被注入了，所以需要再次过滤）
            if(def.getInstance() == null){
                //创建Bean
                createBeanAsEarlySingleton(def);
            }
        });
    }

    /**
     * 创建一个Bean，但不进行字段和方法级别的注入。如果创建的Bean不是Configuration，则在构造方法中注入的依赖Bean会自动创建
     * @param def
     * @return
     */
    public Object createBeanAsEarlySingleton(BeanDefinition def) {
        logger.atDebug().log("Try create bean '{}' as early singleton: {}", def.getName(), def.getBeanClass().getName());
        //若add操作返回false，代表产生了无法解决的循环依赖问题，报错
        // 可以解决的循环依赖如何处理？ 这一步的循环依赖都是强依赖，无法解决
        if(!this.creatingBeanNames.add(def.getName())){
            throw new UnsatisfiedDependencyException(String.format("Circular dependency detected when create bean '%s'",
                    def.getName()));
        }

        //创建方式：构造方法或工厂方法（根据有无FactoryMethod来判断
        //Executable是构造器和方法的公共接口
        Executable createFn = def.getFactoryName() == null ? def.getConstructor() : def.getFactoryMethod();

        //创建参数：
        final Parameter[] parameters = createFn.getParameters();
        final Annotation[][] parametersAnnos = createFn.getParameterAnnotations();
        Object[] args = new Object[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            final Parameter param = parameters[i];
            final Annotation[] paramAnnos = parametersAnnos[i];
            final Value value = ClassUtils.getAnnotation(paramAnnos, Value.class);
            final Autowired autowired = ClassUtils.getAnnotation(paramAnnos, Autowired.class);

            //@Configuration类型的Bean是工厂，需要优先创建，因此其构造器中不允许使用@Autowired
            final boolean isConfiguration = isConfigurationDefinition(def);
            if(isConfiguration && autowired != null) {
                throw new BeanCreationException(
                        String.format("Cannot specify @Autowired when create @Configuration bean '%s': %s.",
                                def.getName(), def.getBeanClass().getName()));
            }

            // 参数需要@Value或@Autowired两者之一，只能同时有其一:
            if (value != null && autowired != null) {
                throw new BeanCreationException(
                        String.format("Cannot specify both @Autowired and @Value when create bean '%s': %s.",
                                def.getName(), def.getBeanClass().getName()));
            }
            if (value == null && autowired == null) {
                throw new BeanCreationException(
                        String.format("Must specify @Autowired or @Value when create bean '%s': %s.",
                                def.getName(), def.getBeanClass().getName()));
            }

            //参数类型
            Class<?> type = param.getType();
            if(value != null){
                //@Value注解，就从解析文件中获取key对应的value并注入
                args[i] = this.propertyResolver.getRequiredProperty(value.value(), type);
            }else{
                //@Autowired注解
                String name = autowired.name();
                boolean required = autowired.value();
                //依赖的BeanDefinition，若有name则按name和type寻找，若无name则按type找（多个def且无Primary标识会报错）
                BeanDefinition dependsOnDef = name.isEmpty() ? findBeanDefinition(type) : findBeanDefinition(name, type);
                //检测如果是必需（默认必需）但是beans中没有该def，则报错
                if(required && dependsOnDef == null) {
                    throw new BeanCreationException(String.format("Missing autowired bean with type '%s' when create bean '%s': %s.", type.getName(),
                            def.getName(), def.getBeanClass().getName()));
                }
                if(dependsOnDef != null) {
                    //获取依赖Bean：
                    Object autowiredBeanInstance = dependsOnDef.getInstance();
                    //如果当前autowireBean未实例化 TODO 能走到这里的必然不是@Configuration啊？
                    if(autowiredBeanInstance == null && !isConfiguration) {
                        //当前依赖Bean尚未初始化，递归调用初始化该依赖Bean
                        autowiredBeanInstance = createBeanAsEarlySingleton(dependsOnDef);
                    }
                    args[i] = autowiredBeanInstance;
                }else {
                    args[i] = null;
                }
            }
        }

        //创建Bean实例：
        Object instance = null;
        if(def.getFactoryName() == null){
            //用构造方法创建
            try {
                instance = def.getConstructor().newInstance(args);
            } catch (Exception e) {
                throw new BeanCreationException(String.format("Exception when create bean '%s': %s",
                        def.getName(), def.getBeanClass().getName()), e);
            }
        }else {
            //用@Bean工厂方法创建，此时Configuration类已经创建完毕
            Object configInstance = getBean(def.getFactoryName());
            try {
                //invoke的第一个参数是对象实例，即在哪个实例上调用该方法，后面的可变参数要与方法参数一致，否则将报错
                instance = def.getFactoryMethod().invoke(configInstance, args);
            }catch (Exception e) {
                throw new BeanCreationException(String.format("Exception when create bean '%s': %s",
                        def.getName(), def.getBeanClass().getName()), e);
            }
        }
        def.setInstance(instance);

        //调用BeanPostProcessor处理Bean；每个processor都要过一遍，方法内部会进行类型判断，如果processed类型不同就代表这个processor
        // 对原Bean进行了处理，因此需要将Instance替换
        for(BeanPostProcessor processor : beanPostProcessors) {
            Object processed = processor.postProcessBeforeInitialization(def.getInstance(), def.getName());
            if (processed == null) {
                throw new BeanCreationException(String.format("PostBeanProcessor returns null when process " +
                        "bean '%s' by %s", def.getName(), processor));
            }
            //用processed替代原始Bean Instance
            if(def.getInstance() != processed){
                def.setInstance(processed);
            }
        }

        return def.getInstance();
    }

    /**
     * 判断beans map中有没有name对应的key
     * @param name
     * @return
     */
    @Override
    public boolean containsBean(String name) {
        return this.beans.containsKey(name);
    }

    /**
     * 通过BeanName获取bean的Instance
     * @param name
     * @return
     * @param <T>
     */
    public <T> T getBean(String name) {
        BeanDefinition def = this.beans.get(name);
        if(def == null){
            throw new NoSuchBeanDefinitionException(String.format("No bean defined with name '%s'.", name));
        }

        return (T) def.getRequiredInstance();
    }

    /**
     * 通过Name和Type查找Bean，不存在抛出NoSuchBeanDefinitionException，存在但与Type不匹配抛出BeanNotOfRequiredTypeException
     * @param name
     * @param requiredType
     * @return
     * @param <T>
     */
    @Override
    public <T> T getBean(String name, Class<T> requiredType) {
        T t = findBean(name, requiredType);
        if(t == null) {
            throw new NoSuchBeanDefinitionException(String.format("No bean defined with name '%s' and type '%s'.",
                    name, requiredType));
        }
        return t;
    }

    /**
     * 通过Type查找Bean，不存在抛出NoSuchBeanDefinitionException，存在多个但缺少唯一@Primary标注抛出NoUniqueBeanDefinitionException
     * @param requiredType
     * @return
     * @param <T>
     */
    public <T> T getBean(Class<T> requiredType) {
        BeanDefinition def = findBeanDefinition(requiredType);
        if(def == null) {
            throw new NoSuchBeanDefinitionException(String.format("No bean defined with type '%s'.", requiredType));
        }
        return (T) def.getRequiredInstance();
    }

    /**
     * 通过type查找Beans
     * @param requiredType
     * @return
     * @param <T>
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> List<T> getBeans(Class<T> requiredType) {
        List<BeanDefinition> defs = findBeanDefinitions(requiredType);
        if (defs.isEmpty()) {
            return List.of();
        }
        List<T> list = new ArrayList<>(defs.size());
        for (var def : defs) {
            list.add((T) def.getRequiredInstance());
        }
        return list;
    }

    /**
     * 执行beanInstance的destroy方法、清空this.beans Map、将ApplicationContextUtil中的成员变量设为null
     */
    @Override
    public void close() {
        logger.info("Closing {}...", this.getClass().getName());
        this.beans.values().forEach(def -> {
            final Object beanInstance = getProxiedInstance(def);
            callMethod(beanInstance, def.getDestroyMethod(), def.getDestroyMethodName());
        });
        this.beans.clear();
        logger.info("{} closed.", this.getClass().getName());
        ApplicationContextUtils.setApplicationContext(null);
    }

    /**
     * 为所有ClassName创建BeanDefinition并存入Map，对于@Configuration注解类还要为其内部@Bean创建BeanDefinition
     * @param beanClassNames 所有需要存入map的ClassName
     * @return 返回Map<ClassName, BeanDefinition>
     */
    Map<String, BeanDefinition> createBeanDefinitions(Set<String> beanClassNames) {
        HashMap<String, BeanDefinition> defs = new HashMap<>();
        for(String className : beanClassNames) {
            //获取Class
            Class<?> clazz = null;
            try {
                clazz = Class.forName(className);
            } catch (ClassNotFoundException e) {
                throw new BeanCreationException(e);
            }
            if(clazz.isAnnotation() || clazz.isEnum() || clazz.isInterface() || clazz.isRecord()) {
                continue; //跳过注解、枚举、接口、record
            }
            //是否标注@Component？
            Component component = ClassUtils.findAnnotation(clazz, Component.class);
            if(component != null) {
                logger.atDebug().log("found component: {}", clazz.getName());
                int mod = clazz.getModifiers(); //获取类的修饰符（public之类）
                if(Modifier.isAbstract(mod)) {
                    //@Component类不能是抽象类
                    throw new BeanDefinitionException("@Component class " + clazz.getName() + " must not be abstract.");
                }

                String beanName = ClassUtils.getBeanName(clazz);
                /*为何传入的方法名都是null？ 在@Component声明的Bean中，我们可以根据@PostConstruct和@PreDestroy直接拿到Method本身，
                 而在@Bean声明的Bean中，我们拿不到Method，只能从@Bean注解提取出字符串格式的方法名称，因此，存储在BeanDefinition的
                 方法名称与方法，其中总有一个为null*/
                BeanDefinition def = new BeanDefinition(beanName, clazz, getSuitableConstructor(clazz), getOrder(clazz),
                        clazz.isAnnotationPresent(Primary.class), null, null,
                        ClassUtils.findAnnotationMethod(clazz, PostConstruct.class),
                        ClassUtils.findAnnotationMethod(clazz, PreDestroy.class));
                addBeanDefinition(defs, def);
                logger.atDebug().log("define bean: ", def);

                //查找Configuration注解（该注解包含Component注解），表示该类中有生成bean的方法
                Configuration configuration = ClassUtils.findAnnotation(clazz, Configuration.class);
                if(configuration != null) {
                    scanFactoryMethods(beanName, clazz, defs);
                }
            }
        }
        return defs;
    }

    /**
     * 扫描Configuration，将Bean及其Definition存入Map
     * @param factoryBeanName 包含@Configuration注解的类名
     * @param clazz 包含@Configuration注解的类
     * @param defs Map<BeanName, BeanDefinition>
     */
    void scanFactoryMethods(String factoryBeanName, Class<?> clazz, HashMap<String, BeanDefinition> defs) {
        for(Method method : clazz.getDeclaredMethods()){
            Bean bean = method.getAnnotation(Bean.class);
            if(bean != null) {
                //抽象方法、final方法、私有方法通通不行
                int mod = method.getModifiers();
                if(Modifier.isAbstract(mod)){
                    throw new BeanDefinitionException("@Bean method " + clazz.getName() + "." + method.getName() +
                            " must not be abstract.");
                }
                if (Modifier.isFinal(mod)) {
                    throw new BeanDefinitionException("@Bean method " + clazz.getName() + "." + method.getName() +
                            " must not be final.");
                }
                if (Modifier.isPrivate(mod)) {
                    throw new BeanDefinitionException("@Bean method " + clazz.getName() + "." + method.getName() +
                            " must not be private.");
                }
                //@Bean方法的返回值是beanClass
                Class<?> beanClass = method.getReturnType();
                //不能是基本数据类型
                if(beanClass.isPrimitive()){
                    throw new BeanDefinitionException("@Bean method " + clazz.getName() + "." + method.getName() +
                            " must not return primitive type.");
                }
                //返回值不能为void
                if (beanClass == void.class || beanClass == Void.class) {
                    throw new BeanDefinitionException("@Bean method " + clazz.getName() + "." + method.getName() +
                            " must not return void.");
                }
                BeanDefinition def = new BeanDefinition(ClassUtils.getBeanName(method), beanClass, factoryBeanName,
                        method, getOrder(method), method.isAnnotationPresent(Primary.class),
                        bean.initMethod().isEmpty() ? null : bean.initMethod(),
                        bean.destroyMethod().isEmpty() ? null : bean.destroyMethod(),
                        null, null);
                addBeanDefinition(defs, def);
                logger.atDebug().log("define bean: ", def);
            }
        }
    }

    /**
     * 将BeanDefinition对象存到Map中，若有重复则报错
     * @param defs
     * @param def
     */
    void addBeanDefinition(HashMap<String, BeanDefinition> defs, BeanDefinition def) {
        //不为null代表defs中已经有了相同beanName的数据，报错
        if(defs.put(def.getName(), def) != null) {
            throw new BeanDefinitionException("Duplicate bean name: " + def.getName());
        }
    }

    /**
     * 获取目标类的Order注解，并返回注解Value值
     * @param clazz
     * @return 若无该注解，返回MAX_VALUE
     */
    int getOrder(Class<?> clazz) {
        Order order = clazz.getAnnotation(Order.class);
        //没有注解Order则默认优先级最低
        return order == null ? Integer.MAX_VALUE : order.value();
    }

    /**
     * 获取目标方法的Order注解，并返回注解Value值
     * @param method
     * @return 若无该注解，返回MAX_VALUE
     */
    int getOrder(Method method) {
        Order order = method.getAnnotation(Order.class);
        //没有注解Order则默认优先级最低
        return order == null ? Integer.MAX_VALUE : order.value();
    }

    /**
     * 获取bean类的唯一构造器，若超过1个就报错
     * @param clazz 需要获取构造器的类
     * @return 唯一构造器
     */
    Constructor<?> getSuitableConstructor(Class<?> clazz) {
        Constructor<?>[] cons = clazz.getConstructors();
        if(cons.length == 0){
            //如果没有public构造器，则获取私有构造器
            cons = clazz.getDeclaredConstructors();
            //TODO 为何构造器数量超过1个会报错？
            if(cons.length != 1) {
                throw new BeanDefinitionException("More than one constructor found in class " + clazz.getName() + ".");
            }
        }
        if(cons.length != 1) {
            throw new BeanDefinitionException("More than one public constructor found in class " + clazz.getName() + ".");
        }
        return cons[0];
    }


    /**
     * 通过启动类的ComponentScan注解获取需要扫描的包、通过Import注解获取需要实例化的Class，扫描得到所有的ClassName；
     * @param configClass 启动类
     * @return 所有需要实例化的Set<BeanName>
     */
    protected Set<String> scanForClassNames(Class<?> configClass) {
        //获取@ComponentScan注解，读取注解中的packageName信息，若无则默认当前类所在包
        ComponentScan scan = ClassUtils.findAnnotation(configClass, ComponentScan.class);
        final String[] scanPackages = scan == null || scan.value().length == 0 ?
                new String[] {configClass.getPackage().getName()} : scan.value();
        logger.atInfo().log("component scan in packages: {}", Arrays.toString(scanPackages));

        Set<String> classNameSet = new HashSet<>();
        for(String pkg : scanPackages){
            //扫描package，将所有class名加入Set
            logger.atDebug().log("scan package: {}", pkg);
            ResourceResolver rr = new ResourceResolver(pkg);
            List<String> classList = rr.scan(res -> {
                String name = res.name();
                if (name.endsWith(".class")) {
                    return name.substring(0, name.length() - 6).replace("/", ".")
                            .replace("\\", ".");
                }
                return null;
            });

            if(logger.isDebugEnabled()){
               classList.forEach(className -> {
                   logger.debug("class found by component scan: {}", className);
               });
            }
            classNameSet.addAll(classList);
        }

        //查找@Import（xyz.class），该注解标在启动类上，value即为需要实例化的Bean Class
        Import anImport = ClassUtils.findAnnotation(configClass, Import.class);
        if(anImport != null){
            for (Class<?> importConfigClass : anImport.value()) {
                String importClassName = importConfigClass.getName();
                if(classNameSet.contains(importClassName)){ //import中的Class已经被扫描过了
                    logger.warn("ignore import: " + importClassName + " for it is already been scanned.");
                }else {
                    logger.debug("class found by import: {}", importClassName);
                    classNameSet.add(importClassName);
                }
            }
        }
        return classNameSet;
    }

    /**
     * 根据Name查找BeanDefinition，若不存在返回null
     * @param name
     * @return
     */
    @Nullable
    public BeanDefinition findBeanDefinition(String name) {
        return this.beans.get(name);
    }

    /**
     * 根据Name查找指定类型的BeanDefinition，若不存在返回null，若不一致则报错
     * @param name
     * @param requiredType
     * @return
     */
    @Nullable
    public BeanDefinition findBeanDefinition(String name, Class<?> requiredType) {
        BeanDefinition def = findBeanDefinition(name);
        if (def == null) {
            return null;
        }
        if (!requiredType.isAssignableFrom(def.getBeanClass())) {
            throw new BeanNotOfRequiredTypeException(String.format("Autowire required type '%s' but bean '%s' " +
                            "has actual type '%s'.", requiredType.getName(),
                    name, def.getBeanClass().getName()));
        }
        return def;
    }

    /**
     * 根据Type查找某个BeanDefinition，如果不存在返回null，如果存在多个返回@Primary标注的一个，如果有多个@Primary标注，
     * 或没有@Primary标注但找到多个，均抛出NoUniqueBeanDefinitionException
     * @param type
     * @return
     */
    @Nullable
    public BeanDefinition findBeanDefinition(Class<?> type) {
        List<BeanDefinition> defs = findBeanDefinitions(type);
        if(defs.isEmpty())
            return null;
        if(defs.size() == 1)
            return defs.get(0);
        //超过1个时，查找@Primary
        List<BeanDefinition> primaryDefs = defs.stream().filter(def -> def.isPrimary()).collect(Collectors.toList());
        if (primaryDefs.size() == 1)
            return defs.get(0);

        if(primaryDefs.isEmpty()){//不存在@Primary注解的bean
            throw new NoUniqueBeanDefinitionException(String.format("Multiple bean with type '%s' found, but " +
                    "no @Primary specified.", type.getName()));
        } else { //存在不止一个@Primary注解的bean
            throw new NoUniqueBeanDefinitionException(String.format("Multiple bean with type '%s' found, " +
                    "and multiple @Primary specified.", type.getName()));
        }
    }

    /**
     * 判断传入的BeanDefinition是不是配置类（看是否包含Configuration注解）
     * @param def
     * @return
     */
    boolean isConfigurationDefinition(BeanDefinition def) {
        return ClassUtils.findAnnotation(def.getBeanClass(), Configuration.class) != null;
    }

    /**
     * 判断传入的BeanDefinition是不是BeanPostProcessor（查看def的BeanClass即可）
     * @param def
     * @return
     */
    boolean isBeanPostProcessorDefinition(BeanDefinition def) {
        return BeanPostProcessor.class.isAssignableFrom(def.getBeanClass());
    }

    /**
     * 根据Type查找若干个BeanDefinition，返回0个或多个。
     * @param type
     * @return
     */
    public List<BeanDefinition> findBeanDefinitions(Class<?> type) {
        return this.beans.values().stream()
                //按类型过滤
                .filter(def -> type.isAssignableFrom(def.getBeanClass()))
                //排序
                .sorted().collect(Collectors.toList());
    }
}
