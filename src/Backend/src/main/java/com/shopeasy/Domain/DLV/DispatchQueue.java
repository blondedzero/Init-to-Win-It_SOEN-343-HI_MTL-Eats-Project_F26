package com.shopeasy.Domain.DLV;

import com.shopeasy.Foundation.entities.Courier;
import com.shopeasy.Foundation.entities.CourierStatus;
import com.shopeasy.Foundation.entities.Order;
import com.shopeasy.Foundation.entities.OrderStatus;
import com.shopeasy.Foundation.repositories.OrderRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

//holds READY orders with no courier and offers the oldest one to a courier when they become idle (R-DLV-04)
@Component
public class DispatchQueue implements CourierStatusObserver {

    private final DLVModule dlvModule;
    private final OrderRepository orderRepository;

    private final Queue<Order> waitingOrders = new ConcurrentLinkedQueue<>();

    public DispatchQueue(DLVModule dlvModule, OrderRepository orderRepository) {
        this.dlvModule = dlvModule;
        this.orderRepository = orderRepository;
    }

    @PostConstruct
    void init() {
        dlvModule.subscribe(this);
        refresh();
    }

    public void refresh() {
        waitingOrders.clear();
        waitingOrders.addAll(orderRepository.findByStatusAndCourierIsNullOrderByReadyAtAsc(OrderStatus.READY));
    }

    public void enqueue(Order o) {
        waitingOrders.add(o);
    }

    @Override
    public void onCourierIdle(Courier c) {
        if (c.getStatus() != CourierStatus.IDLE || dlvModule.hasPendingOffer(c)) {
            return;
        }
        for (Order waiting : orderRepository.findByStatusAndCourierIsNullOrderByReadyAtAsc(OrderStatus.READY)) {
            if (waitingOrders.stream().noneMatch(o -> o.getOrderId().equals(waiting.getOrderId()))) {
                waitingOrders.add(waiting);
            }
        }
        Iterator<Order> it = waitingOrders.iterator();
        while (it.hasNext()) {
            Order next = it.next();
            Order order = orderRepository.findById(next.getOrderId()).orElse(null);
            if (order == null || order.getStatus() != OrderStatus.READY || order.getCourier() != null) {
                it.remove();
                continue;
            }
            if (dlvModule.hasDeclined(order.getOrderId(), c.getAccountId())) {
                continue;
            }
            it.remove();
            dlvModule.offerWaitingOrder(order, c);
            return;
        }
    }
}
