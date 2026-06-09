package com.jnet.biz.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 消息队列配置
 * 
 * @author JNet Team
 * @since 2024-05-11
 */
@Configuration
public class RabbitMQConfig {
    
    // Exchange名称
    public static final String TASK_EXCHANGE = "algorithm.task.exchange";
    
    // Queue名称
    public static final String DATASET_BUILD_QUEUE = "dataset.build.queue";
    public static final String TRAINING_QUEUE = "training.queue";
    public static final String PREDICTION_QUEUE = "prediction.queue";
    
    // Routing Key
    public static final String DATASET_BUILD_ROUTING_KEY = "dataset.build";
    public static final String DATASET_BUILD_RETRY_ROUTING_KEY = "dataset.build.retry";
    public static final String TRAINING_ROUTING_KEY = "training.execute";
    public static final String PREDICTION_ROUTING_KEY = "prediction.execute";
    
    /**
     * 创建Direct Exchange
     */
    @Bean
    public DirectExchange taskExchange() {
        return new DirectExchange(TASK_EXCHANGE, true, false);
    }
    
    /**
     * 创建数据集构建队列（带死信队列）
     */
    @Bean
    public Queue datasetBuildQueue() {
        return QueueBuilder.durable(DATASET_BUILD_QUEUE)
            .withArgument("x-message-ttl", 3600000) // 1小时超时
            .withArgument("x-dead-letter-exchange", TASK_EXCHANGE)
            .withArgument("x-dead-letter-routing-key", DATASET_BUILD_RETRY_ROUTING_KEY)
            .build();
    }
    
    /**
     * 创建训练任务队列
     */
    @Bean
    public Queue trainingQueue() {
        return QueueBuilder.durable(TRAINING_QUEUE)
            .withArgument("x-message-ttl", 7200000) // 2小时超时
            .build();
    }
    
    /**
     * 创建预测任务队列
     */
    @Bean
    public Queue predictionQueue() {
        return QueueBuilder.durable(PREDICTION_QUEUE)
            .withArgument("x-message-ttl", 3600000) // 1小时超时
            .build();
    }
    
    /**
     * 绑定数据集构建队列
     */
    @Bean
    public Binding datasetBinding() {
        return BindingBuilder.bind(datasetBuildQueue())
            .to(taskExchange())
            .with(DATASET_BUILD_ROUTING_KEY);
    }
    
    /**
     * 绑定训练任务队列
     */
    @Bean
    public Binding trainingBinding() {
        return BindingBuilder.bind(trainingQueue())
            .to(taskExchange())
            .with(TRAINING_ROUTING_KEY);
    }
    
    /**
     * 绑定预测任务队列
     */
    @Bean
    public Binding predictionBinding() {
        return BindingBuilder.bind(predictionQueue())
            .to(taskExchange())
            .with(PREDICTION_ROUTING_KEY);
    }
    
    /**
     * 配置 RabbitMQ 监听器容器工厂
     * 用于控制消息消费的流量和并发
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        
        // 预取数量：每个消费者预先获取的消息数
        // 对于耗时较长的任务，设置为 1 确保负载均衡
        factory.setPrefetchCount(1);
        
        // 并发消费者数量范围
        factory.setConcurrentConsumers(2);      // 最小并发数
        factory.setMaxConcurrentConsumers(5);   // 最大并发数
        
        // 消费者启动时是否自动启动
        factory.setAutoStartup(true);
        
        // 签收模式：手动签收（确保消息处理完成后才确认）
        factory.setAcknowledgeMode(AcknowledgeMode.AUTO);
        
        // 重试配置
        factory.setDefaultRequeueRejected(false); // 失败后不重新入队（由死信队列处理）
        
        return factory;
    }
}
