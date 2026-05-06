package com.jnet.anno.utils;

import cn.hutool.extra.spring.SpringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;

import java.util.Locale;

/**
 * 国际化消息工具类
 * <p>
 * 提供基于 Spring MessageSource 的国际化消息获取功能
 * 支持多语言切换，自动根据当前 Locale 获取对应语言的消息
 * </p>
 *
 * <p><b>使用示例：</b></p>
 * <pre>{@code
 * // 获取默认语言消息
 * String msg = MessageSourceUtil.getMessage("user.not.found");
 * 
 * // 获取带参数的消息
 * String msg = MessageSourceUtil.getMessage("user.welcome", new Object[]{"张三"});
 * 
 * // 获取指定语言的消息
 * String msg = MessageSourceUtil.getMessage("user.welcome", Locale.ENGLISH);
 * }</pre>
 *
 * @author JNet Team
 * @see org.springframework.context.MessageSource
 * @since 1.0.0
 */
@Slf4j
public class MessageSourceUtil {

    /**
     * Spring 国际化消息源
     * <p>
     * 通过 Hutool 的 SpringUtil 从 Spring 容器中获取 MessageSource Bean
     * 在类加载时自动初始化，确保线程安全
     * </p>
     */
    private static final MessageSource MESSAGE_SOURCE = SpringUtil.getBean(MessageSource.class);

    /**
     * 获取国际化消息（使用当前 Locale）
     * <p>
     * 根据消息码从资源文件中获取对应语言的文本
     * 如果找不到对应的消息码，返回消息码本身
     * </p>
     *
     * @param code 消息码，对应 messages.properties 中的 key
     * @return 国际化消息文本，如果未找到则返回 code 本身
     * @throws IllegalArgumentException 如果 code 为 null 或空字符串
     * @see #getMessage(String, Object[])
     * @see #getMessage(String, Locale)
     */
    public static String getMessage(String code) {
        if (code == null || code.trim().isEmpty()) {
            log.warn("消息码为空，返回空字符串");
            return "";
        }
        
        Locale locale = LocaleContextHolder.getLocale();
        log.debug("获取国际化消息 - code: {}, locale: {}", code, locale);
        
        try {
            return MESSAGE_SOURCE.getMessage(code, null, locale);
        } catch (Exception e) {
            log.warn("未找到消息码 [{}] 对应的消息，返回原始码", code);
            return code;
        }
    }

    /**
     * 获取带参数的国际化消息（使用当前 Locale）
     * <p>
     * 支持消息模板中的占位符替换，例如：
     * <pre>
     * messages.properties: user.welcome=欢迎, {0}!
     * 调用: getMessage("user.welcome", new Object[]{"张三"})
     * 结果: "欢迎, 张三!"
     * </pre>
     * </p>
     *
     * @param code 消息码，对应 messages.properties 中的 key
     * @param args 参数数组，用于替换消息模板中的占位符 {0}, {1}, ...
     * @return 格式化后的国际化消息文本
     * @throws IllegalArgumentException 如果 code 为 null 或空字符串
     * @see java.text.MessageFormat
     */
    public static String getMessage(String code, Object[] args) {
        if (code == null || code.trim().isEmpty()) {
            log.warn("消息码为空，返回空字符串");
            return "";
        }
        
        Locale locale = LocaleContextHolder.getLocale();
        log.debug("获取带参数的国际化消息 - code: {}, args: {}, locale: {}", code, args, locale);
        
        try {
            return MESSAGE_SOURCE.getMessage(code, args, locale);
        } catch (Exception e) {
            log.warn("未找到消息码 [{}] 对应的消息，返回原始码", code, e);
            return code;
        }
    }

    /**
     * 获取指定语言的国际化消息
     * <p>
     * 忽略当前上下文 Locale，直接使用指定的 Locale 获取消息
     * 适用于需要强制使用某种语言的场景
     * </p>
     *
     * @param code   消息码，对应 messages.properties 中的 key
     * @param locale 目标语言环境，如 Locale.CHINESE、Locale.ENGLISH
     * @return 指定语言的国际化消息文本
     * @throws IllegalArgumentException 如果 code 为 null 或空字符串
     */
    public static String getMessage(String code, Locale locale) {
        if (code == null || code.trim().isEmpty()) {
            log.warn("消息码为空，返回空字符串");
            return "";
        }
        
        if (locale == null) {
            locale = LocaleContextHolder.getLocale();
            log.debug("Locale 为空，使用默认 Locale: {}", locale);
        }
        
        log.debug("获取指定语言的国际化消息 - code: {}, locale: {}", code, locale);
        
        try {
            return MESSAGE_SOURCE.getMessage(code, null, locale);
        } catch (Exception e) {
            log.warn("未找到消息码 [{}] 对应的消息，返回原始码", code, e);
            return code;
        }
    }

    /**
     * 获取国际化消息（带默认值）
     * <p>
     * 如果找不到对应的消息码，返回指定的默认值而不是消息码本身
     * 适用于需要提供友好提示的场景
     * </p>
     *
     * @param code           消息码
     * @param defaultMessage 默认消息，当找不到 code 对应的消息时返回
     * @return 国际化消息文本，如果未找到则返回 defaultMessage
     */
    public static String getMessage(String code, String defaultMessage) {
        if (code == null || code.trim().isEmpty()) {
            log.debug("消息码为空，返回默认消息");
            return defaultMessage;
        }
        
        Locale locale = LocaleContextHolder.getLocale();
        
        try {
            return MESSAGE_SOURCE.getMessage(code, null, defaultMessage, locale);
        } catch (Exception e) {
            log.debug("未找到消息码 [{}]，返回默认消息", code);
            return defaultMessage;
        }
    }

    /**
     * 获取带参数和默认值的国际化消息
     *
     * @param code           消息码
     * @param args           参数数组
     * @param defaultMessage 默认消息
     * @return 格式化后的国际化消息文本
     */
    public static String getMessage(String code, Object[] args, String defaultMessage) {
        if (code == null || code.trim().isEmpty()) {
            log.debug("消息码为空，返回默认消息");
            return defaultMessage;
        }
        
        Locale locale = LocaleContextHolder.getLocale();
        
        try {
            return MESSAGE_SOURCE.getMessage(code, args, defaultMessage, locale);
        } catch (Exception e) {
            log.debug("未找到消息码 [{}]，返回默认消息", code);
            return defaultMessage;
        }
    }
}
