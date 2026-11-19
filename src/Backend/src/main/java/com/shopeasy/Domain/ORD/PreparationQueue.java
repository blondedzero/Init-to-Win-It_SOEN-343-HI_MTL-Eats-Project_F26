package com.shopeasy.Domain.ORD;

import com.shopeasy.Domain.ORD.exceptions.OrderRuleViolationException;
import com.shopeasy.Foundation.entities.Order;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class PreparationQueue {

    private record Entry(Long orderId, Instant placedAt) {}

    private static final Comparator<Entry> ARRIVAL_ORDER =
            Comparator.comparing(Entry::placedAt).thenComparing(Entry::orderId);

    private final List<Entry> entries = new ArrayList<>();

    public synchronized void enqueue(Order order) {
        if (positionOf(order.getOrderId()) != -1) return;
        Entry entry = new Entry(order.getOrderId(), order.getPlacedAt());
        int index = 0;
        while (index < entries.size() && ARRIVAL_ORDER.compare(entries.get(index), entry) < 0) {
            index++;
        }
        entries.add(index, entry);
    }

    public synchronized Optional<Long> head() {
        return entries.isEmpty() ? Optional.empty() : Optional.of(entries.getFirst().orderId());
    }

    public synchronized void dequeue(Long orderId) {
        Optional<Long> head = head();
        if (head.isEmpty() || !head.get().equals(orderId)) {
            throw new OrderRuleViolationException("Order " + orderId + " is not next in the preparation queue"
                    + head.map(id -> " (order " + id + " is)").orElse(""));
        }
        entries.removeFirst();
    }

    public synchronized void remove(Long orderId) {
        entries.removeIf(entry -> entry.orderId().equals(orderId));
    }

    public synchronized int positionOf(Long orderId) {
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).orderId().equals(orderId)) return i + 1;
        }
        return -1;
    }

    public synchronized int size() {
        return entries.size();
    }

    public synchronized List<Long> orderIds() {
        return entries.stream().map(Entry::orderId).toList();
    }
}
