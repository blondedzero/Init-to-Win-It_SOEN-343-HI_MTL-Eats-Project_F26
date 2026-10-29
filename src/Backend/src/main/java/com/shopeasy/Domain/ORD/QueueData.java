package com.shopeasy.Domain.ORD;

import java.util.List;

public record QueueData(Long storeId, String storeName, int size, List<OrderData> orders) {}
