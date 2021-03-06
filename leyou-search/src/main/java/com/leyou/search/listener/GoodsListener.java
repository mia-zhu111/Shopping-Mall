package com.leyou.search.listener;

import com.leyou.search.service.SearchService;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class GoodsListener {
    @Autowired
    private SearchService searchService;

    @RabbitListener(bindings = @QueueBinding(
            //value就是队列
            value = @Queue(value = "leyou.search.save.queue", durable = "true"),
            //交换机的名称应该与item-service中配置的template.exchange名称一致
            exchange = @Exchange(value = "leyou.item.exchange", ignoreDeclarationExceptions = "true", type = ExchangeTypes.TOPIC),
            //RoutingKey
            key = {"item.insert", "item.update"}
    ))
    public void save(Long id) throws IOException {
        if(id == null) {
            return;
        }
        this.searchService.save(id);
    }

    @RabbitListener(bindings = @QueueBinding(
            //value就是队列
            value = @Queue(value = "leyou.search.delete.queue", durable = "true"),
            //交换机的名称应该与item-service中配置的template.exchange名称一致
            exchange = @Exchange(value = "leyou.item.exchange", ignoreDeclarationExceptions = "true", type = ExchangeTypes.TOPIC),
            //RoutingKey
            key = {"item.delete"}
    ))
    public void delete(Long id) throws IOException {
        if(id == null) {
            return;
        }
        this.searchService.delete(id);

    }
}
