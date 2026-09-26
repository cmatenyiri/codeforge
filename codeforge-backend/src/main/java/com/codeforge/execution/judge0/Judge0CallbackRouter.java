package com.codeforge.execution.judge0;

import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Gets a Judge0 callback to the instance that is waiting for it.
 *
 * <p>Judge0 calls whichever instance its callback URL reaches — behind a load
 * balancer, any of them — but the batch is waited on by exactly one: the one
 * holding the request that sent it. The callback URL therefore names that
 * instance, and whichever instance receives the call forwards it over RabbitMQ
 * with that name as the routing key. Each instance consumes only its own queue,
 * so a callback crosses the broker once and reaches one instance, however many
 * are running.
 *
 * <p>A callback that reaches the instance it names is delivered directly,
 * without the round trip — which on a single instance is every callback.
 */
@Component
class Judge0CallbackRouter {

    private static final Logger log = LoggerFactory.getLogger(Judge0CallbackRouter.class);

    static final String EXCHANGE = "codeforge.judge0.callbacks";

    /** Carries the batch key alongside the untouched Judge0 body. */
    private static final String BATCH_HEADER = "judge0-batch";

    private final Judge0Batches batches;
    private final Instance instance;
    private final RabbitTemplate rabbitTemplate;
    private final JsonMapper jsonMapper;

    Judge0CallbackRouter(
            Judge0Batches batches, Instance instance, RabbitTemplate rabbitTemplate, JsonMapper jsonMapper) {
        this.batches = batches;
        this.instance = instance;
        this.rabbitTemplate = rabbitTemplate;
        this.jsonMapper = jsonMapper;
    }

    /** The path segment a callback URL uses to name this instance. */
    String instanceId() {
        return instance.id();
    }

    /**
     * Delivers a callback here, or forwards it to the instance it names.
     *
     * <p>Forwarding does not wait for the other side. If that instance has gone,
     * its queue went with it and the message is simply unroutable — the request
     * that wanted the result went with the instance too.
     */
    void route(String instanceId, String batchKey, byte[] body) {
        if (instance.id().equals(instanceId)) {
            deliver(batchKey, body);
            return;
        }
        Message message = MessageBuilder.withBody(body)
                .setContentType("application/json")
                .setHeader(BATCH_HEADER, batchKey)
                .build();
        rabbitTemplate.send(EXCHANGE, instanceId, message);
    }

    /** A callback another instance received on this one's behalf. */
    @RabbitListener(queues = "#{judge0Instance.queue()}")
    void onForwarded(Message message) {
        Object batchKey = message.getMessageProperties().getHeader(BATCH_HEADER);
        if (batchKey != null) {
            deliver(batchKey.toString(), message.getBody());
        }
    }

    private void deliver(String batchKey, byte[] body) {
        Judge0Api.Result result;
        try {
            result = jsonMapper.readValue(body, Judge0Api.Result.class);
        } catch (JacksonException e) {
            log.warn("Dropped a Judge0 callback for batch {} that was not a submission", batchKey);
            return;
        }
        batches.deliver(batchKey, result);
    }

    /**
     * This running instance, as the callbacks address it.
     *
     * <p>Random per start rather than configured, so two instances can never be
     * given the same name by a copied config file — and a restarted instance,
     * which has lost every batch it was waiting on, is correctly a different one.
     */
    public record Instance(String id) {

        public String queue() {
            return EXCHANGE + "." + id;
        }
    }

    @Configuration
    static class Topology {

        @Bean
        Instance judge0Instance() {
            return new Instance(UUID.randomUUID().toString());
        }

        /**
         * One direct exchange for everybody, and a queue per instance bound under
         * its own id.
         *
         * <p>The queue is exclusive and auto-deleted: it exists exactly as long
         * as the instance's connection does, so a stopped instance leaves nothing
         * behind for callbacks to pile up in.
         */
        @Bean
        Declarables judge0CallbackTopology(Instance judge0Instance) {
            DirectExchange exchange = new DirectExchange(EXCHANGE, true, false);
            Queue queue = new Queue(judge0Instance.queue(), false, true, true);
            Binding binding = BindingBuilder.bind(queue).to(exchange).with(judge0Instance.id());
            return new Declarables(exchange, queue, binding);
        }
    }
}
