package com.shopeasy.foundation.repositories;

import com.shopeasy.foundation.entities.PaymentInfo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentInfoRepository extends JpaRepository<PaymentInfo, Long> {}
