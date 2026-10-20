package com.shopeasy.Foundation.repositories;

import com.shopeasy.Foundation.entities.PaymentInfo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentInfoRepository extends JpaRepository<PaymentInfo, Long> {}
