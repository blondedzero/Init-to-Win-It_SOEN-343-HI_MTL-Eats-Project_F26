package com.shopeasy.Domain.DLV;

import com.shopeasy.Foundation.entities.Courier;
import com.shopeasy.Foundation.entities.CourierStatus;
import com.shopeasy.Foundation.entities.Order;
import com.shopeasy.Foundation.entities.OrderStatus;
import com.shopeasy.Foundation.repositories.OrderRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

// Concrete observer (R-DLV-04): holds ready orders that no courier could take, and retries
// assignment whenever DLVModule reports that a courier became idle.
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
        // Orders still waiting from before a restart: ready, no courier, oldest first
        waitingOrders.addAll(orderRepository.findByStatusAndCourierIsNullOrderByReadyAtAsc(OrderStatus.READY));
    }

    public void enqueue(Order o) {
        waitingOrders.add(o);
    }

    @Override
    public void onCourierIdle(Courier c) {
        if (c.getStatus() != CourierStatus.IDLE) {
            return;
        }
        // Offer the oldest waiting order. The queue only fills while no courier is idle, so the
        // courier that just became idle is the nearest idle one (R-DLV-02).
        Order next;
        while ((next = waitingOrders.poll()) != null) {
            // Reload: the order may have been canceled or manually assigned while it waited
            Order order = orderRepository.findById(next.getOrderId()).orElse(null);
            if (order != null && order.getStatus() == OrderStatus.READY && order.getCourier() == null) {
                dlvModule.assignWaitingOrder(order, c);
                return;
            }
        }
    }
}
